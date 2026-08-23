package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FavoritesActivity extends ThemedActivity {
    private final List<String> keys = new ArrayList<>();
    private final List<String> itemKeys = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        Set<String> saved = FavoritesStore.all(this);
        keys.clear();
        keys.addAll(saved);
        keys.sort(Comparator.naturalOrder());
        itemKeys.clear();
        itemKeys.addAll(FavoritesStore.allItems(this));
        itemKeys.sort(Comparator.naturalOrder());

        List<Map<String, String>> rows = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        for (String itemKey : itemKeys) {
            if (itemKey.startsWith("prayer:")) {
                PrayerEntry prayer = PrayerRepository.find(itemKey.substring("prayer:".length()));
                if (prayer != null) {
                    rows.add(Rows.row(prayer.title, "Oraciones básicas · " + prayer.category));
                    actions.add(() -> {
                        Intent intent = new Intent(this, PrayerReaderActivity.class);
                        intent.putExtra(PrayerReaderActivity.EXTRA_PRAYER_ID, prayer.id);
                        startActivity(intent);
                    });
                }
            } else if (itemKey.startsWith("section:")) {
                String[] parts = itemKey.split(":", 3);
                DocumentInfo sectionDocument = parts.length > 1
                        ? ContentRepository.document(parts[1]) : null;
                int sectionPosition = parts.length > 2 ? parsePage(parts[2]) : 0;
                if (sectionDocument != null) {
                    List<CatalogEntry> sectionEntries = ContentRepository.catalog(sectionDocument.id);
                    if (sectionPosition >= 0 && sectionPosition < sectionEntries.size()) {
                        CatalogEntry section = sectionEntries.get(sectionPosition);
                        rows.add(Rows.row(section.title,
                                sectionDocument.title + " · " + section.section));
                        actions.add(() -> {
                            Intent intent = new Intent(this, DocumentSectionReaderActivity.class);
                            intent.putExtra(DocumentSectionReaderActivity.EXTRA_DOCUMENT,
                                    sectionDocument.id);
                            intent.putExtra(DocumentSectionReaderActivity.EXTRA_ENTRY_INDEX,
                                    sectionPosition);
                            startActivity(intent);
                        });
                    }
                }
            } else if (itemKey.startsWith("hourdirect:")) {
                String[] parts = itemKey.split(":", 5);
                HoursVolume volume = parts.length > 1 ? HoursRepository.find(parts[1]) : null;
                String filePath = parts.length > 2 ? parts[2] : "";
                String fragment = parts.length > 3 ? parts[3] : "";
                String title = parts.length > 4 ? parts[4] : "Liturgia de las Horas";
                if (volume != null && !filePath.isEmpty()) {
                    rows.add(Rows.row(title, "Liturgia de las Horas · " + volume.title));
                    actions.add(() -> {
                        Intent intent = new Intent(this, HoursReaderActivity.class);
                        intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, volume.id);
                        intent.putExtra(HoursReaderActivity.EXTRA_FILE_PATH, filePath);
                        intent.putExtra(HoursReaderActivity.EXTRA_FRAGMENT, fragment);
                        intent.putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE, title);
                        if ("Tercia".equals(title) || "Sexta".equals(title)
                                || "Nona".equals(title)) {
                            intent.putExtra(HoursReaderActivity.EXTRA_SCROLL_TEXT, title);
                        }
                        intent.putExtra(HoursReaderActivity.EXTRA_SHOW_INTENTIONS,
                                "Laudes".equals(title) || "Vísperas".equals(title));
                        startActivity(intent);
                    });
                }
            } else if (itemKey.startsWith("hours:")) {
                String[] parts = itemKey.split(":", 3);
                HoursVolume volume = parts.length > 1 ? HoursRepository.find(parts[1]) : null;
                int tocPosition = parts.length > 2 ? parsePage(parts[2]) : 0;
                if (volume != null) {
                    try {
                        List<EpubTocEntry> toc = EpubUtils.tableOfContents(this, volume);
                        if (tocPosition >= 0 && tocPosition < toc.size()) {
                            EpubTocEntry selected = toc.get(tocPosition);
                            rows.add(Rows.row(selected.title,
                                    HoursRepository.isDevotional(volume)
                                            ? "Devocionario · Opus Dei"
                                            : "Liturgia de las Horas · " + volume.title));
                            actions.add(() -> {
                                Intent intent = new Intent(this, HoursReaderActivity.class);
                                intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, volume.id);
                                intent.putExtra(HoursReaderActivity.EXTRA_TOC_INDEX, tocPosition);
                                startActivity(intent);
                            });
                        }
                    } catch (Exception ignored) {
                    }
                }
            } else if (itemKey.startsWith("ritual:")) {
                String[] parts = itemKey.split(":", 3);
                RitualDocument ritualDocument = parts.length > 1
                        ? RitualRepository.find(parts[1]) : null;
                int ritualPosition = parts.length > 2 ? parsePage(parts[2]) : 0;
                if (ritualDocument != null && ritualPosition >= 0
                        && ritualPosition < ritualDocument.entries.size()) {
                    RitualEntry ritualEntry = ritualDocument.entries.get(ritualPosition);
                    rows.add(Rows.row(ritualEntry.title,
                            ritualDocument.title + " · " + ritualEntry.category));
                    actions.add(() -> {
                        Intent intent = new Intent(this, RitualReaderActivity.class);
                        intent.putExtra(RitualReaderActivity.EXTRA_DOCUMENT_ID,
                                ritualDocument.id);
                        intent.putExtra(RitualReaderActivity.EXTRA_ENTRY_INDEX,
                                ritualPosition);
                        startActivity(intent);
                    });
                }
            }
        }
        for (String key : keys) {
            String[] parts = key.split(":", 2);
            DocumentInfo document = ContentRepository.document(parts[0]);
            int page = parts.length == 2 ? parsePage(parts[1]) : 0;
            if (document != null) {
                rows.add(Rows.row(document.title,
                        "Página PDF " + (page + 1) + " · Toca para abrir"));
                actions.add(() -> {
                    Intent intent = new Intent(this, TextReaderActivity.class);
                    intent.putExtra(TextReaderActivity.EXTRA_DOCUMENT, document.id);
                    intent.putExtra(TextReaderActivity.EXTRA_PAGE, page);
                    startActivity(intent);
                });
            }
        }

        TextView empty = findViewById(R.id.txtEmpty);
        empty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        ListView list = findViewById(R.id.listFavorites);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> actions.get(position).run());
    }

    private static int parsePage(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
