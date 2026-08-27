package com.fabri.ministerium;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;

public class PrayerReaderActivity extends ThemedActivity {
    public static final String EXTRA_PRAYER_ID = "prayer_id";
    public static final String EXTRA_COLLECTION = "prayer_collection";
    public static final String EXTRA_PERSONAL_PRAYER_ID = "personal_prayer_id";
    public static final String EXTRA_DIRECT_TITLE = "direct_prayer_title";
    public static final String EXTRA_DIRECT_TEXT = "direct_prayer_text";
    public static final String EXTRA_DIRECT_SUBTITLE = "direct_prayer_subtitle";

    private PrayerEntry entry;
    private PersonalPrayer personalPrayer;
    private String directText = "";
    private TextView content;
    private Button favorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_reader);

        entry = PrayerRepository.find(getIntent().getStringExtra(EXTRA_PRAYER_ID));
        personalPrayer = PersonalPrayerStore.find(
                this, getIntent().getStringExtra(EXTRA_PERSONAL_PRAYER_ID));
        directText = value(getIntent().getStringExtra(EXTRA_DIRECT_TEXT));
        if (entry == null && personalPrayer == null && directText.isEmpty()) {
            finish();
            return;
        }

        content = findViewById(R.id.txtContent);
        favorite = findViewById(R.id.btnFavorite);
        bindContent();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        favorite.setOnClickListener(v -> {
            if (entry == null) return;
            FavoritesStore.toggleItem(this, "prayer:" + entry.id);
            updateFavorite();
        });
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        updateFavorite();
        configureNavigation();
    }

    private void bindContent() {
        TextView title = findViewById(R.id.txtReaderTitle);
        TextView subtitle = findViewById(R.id.txtReaderSubtitle);
        TextView source = findViewById(R.id.txtSource);
        if (personalPrayer != null) {
            title.setText(personalPrayer.title);
            subtitle.setText("Mi oración");
            source.setText("Oración personal · guardada únicamente en este dispositivo");
            content.setText(personalPrayer.text);
            return;
        }
        if (!directText.isEmpty()) {
            title.setText(value(getIntent().getStringExtra(EXTRA_DIRECT_TITLE),
                    "Oración personal"));
            subtitle.setText(value(getIntent().getStringExtra(EXTRA_DIRECT_SUBTITLE),
                    "Oración privada"));
            source.setText("Contenido privado · guardado únicamente en este dispositivo");
            content.setText(directText);
            return;
        }
        title.setText(entry.title);
        subtitle.setText(entry.category);
        source.setText("Oración tradicional · disponible sin conexión");
        try {
            content.setText(PrayerRepository.read(this, entry));
        } catch (IOException error) {
            Toast.makeText(this, "No se pudo abrir la oración.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void configureNavigation() {
        if (personalPrayer != null) {
            List<PersonalPrayer> prayers = PersonalPrayerStore.all(this);
            int position = indexOf(prayers, personalPrayer.id);
            ReaderContext context = new ReaderContext("Mis oraciones",
                    "personal-prayer:" + personalPrayer.id, personalPrayer.title,
                    "Oración personal", "Mis oraciones", false);
            TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                    findViewById(R.id.readerHeader), context, new ReaderChrome.Navigator() {
                        @Override public boolean canPrevious() { return position > 0; }
                        @Override public boolean canNext() {
                            return position >= 0 && position < prayers.size() - 1;
                        }
                        @Override public void previous() {
                            showPersonal(prayers.get(position - 1));
                        }
                        @Override public void next() {
                            showPersonal(prayers.get(position + 1));
                        }
                    });
            TextViewReaderChrome.bindMore(
                    this, findViewById(R.id.btnReaderMore), content, context);
            return;
        }
        if (!directText.isEmpty()) {
            String title = value(getIntent().getStringExtra(EXTRA_DIRECT_TITLE),
                    "Oración personal");
            ReaderContext context = new ReaderContext("Oración", "private-prayer-session",
                    title, "Oración privada", title, false);
            TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                    findViewById(R.id.readerHeader), context, null);
            TextViewReaderChrome.bindMore(
                    this, findViewById(R.id.btnReaderMore), content, context);
            return;
        }

        List<PrayerEntry> entries = "additional".equals(
                getIntent().getStringExtra(EXTRA_COLLECTION))
                ? PrayerRepository.additional() : PrayerRepository.basic();
        int position = entries.indexOf(entry);
        ReaderContext context = new ReaderContext("Oraciones", "prayer:" + entry.id,
                entry.title, entry.category, "Oraciones", false);
        TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                findViewById(R.id.readerHeader), context, new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return position > 0; }
                    @Override public boolean canNext() {
                        return position >= 0 && position < entries.size() - 1;
                    }
                    @Override public void previous() { show(entries.get(position - 1)); }
                    @Override public void next() { show(entries.get(position + 1)); }
                });
        TextViewReaderChrome.bindMore(
                this, findViewById(R.id.btnReaderMore), content, context);
    }

    private int indexOf(List<PersonalPrayer> prayers, String id) {
        for (int index = 0; index < prayers.size(); index++) {
            if (id.equals(prayers.get(index).id)) return index;
        }
        return -1;
    }

    private void show(PrayerEntry next) {
        getIntent().putExtra(EXTRA_PRAYER_ID, next.id);
        recreate();
    }

    private void showPersonal(PersonalPrayer next) {
        getIntent().putExtra(EXTRA_PERSONAL_PRAYER_ID, next.id);
        recreate();
    }

    private void updateFavorite() {
        if (entry == null) {
            favorite.setVisibility(View.GONE);
            return;
        }
        favorite.setVisibility(View.VISIBLE);
        favorite.setText(FavoritesStore.containsItem(
                this, "prayer:" + entry.id) ? "★" : "☆");
    }

    private static String value(String candidate) {
        return candidate == null ? "" : candidate.trim();
    }

    private static String value(String candidate, String fallback) {
        String result = value(candidate);
        return result.isEmpty() ? fallback : result;
    }
}
