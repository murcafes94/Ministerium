package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.text.Normalizer;
import java.util.Locale;

public class HoursTocActivity extends ThemedActivity {
    public static final String EXTRA_VOLUME_ID = "volume_id";
    private final List<Integer> displayedIndices = new ArrayList<>();
    private List<EpubTocEntry> entries;
    private HoursVolume volume;
    private boolean devotional;
    private boolean latin;
    private boolean missal;
    private boolean wayOfCross;
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        volume = HoursRepository.find(getIntent().getStringExtra(EXTRA_VOLUME_ID));
        if (volume == null) {
            finish();
            return;
        }
        ((TextView) findViewById(R.id.txtTitle)).setText(volume.title);
        devotional = HoursRepository.isDevotional(volume);
        latin = HoursRepository.isLatin2026(volume);
        missal = HoursRepository.isRomanMissal(volume);
        wayOfCross = HoursRepository.isRatzingerWayOfCross(volume);
        boolean reference = HoursRepository.isReference(volume);
        ((TextView) findViewById(R.id.txtSubtitle)).setText(
                devotional ? "Oración y devoción"
                        : latin ? "Liturgia Horarum · latine"
                        : missal ? "Ordinario y propios de la Misa"
                        : wayOfCross ? "Meditaciones y oraciones · 2005"
                        : reference ? "Biblioteca del Magisterio"
                        : "Liturgia de las Horas");
        ((TextView) findViewById(R.id.txtIntro)).setText(devotional
                ? "Selección de oraciones de Opus Dei en español y latín, disponible sin conexión."
                : latin
                ? "Edición latina completa para 2026 de Juraj Videky · elige un mes y luego la fecha y la Hora. Disponible sin conexión."
                : missal
                ? "Misal completo disponible sin conexión · incluye el Ordinario en latín y español, tiempos litúrgicos, propios, comunes y lecturas."
                : wayOfCross
                ? "Viacrucis del Coliseo de 2005 con las meditaciones del cardenal Joseph Ratzinger, futuro Benedicto XVI. Disponible sin conexión."
                : reference
                ? "Documento completo sin conexión · usa el buscador para localizar títulos, capítulos o numerales del índice."
                : "Índice del volumen · toca un día, semana, celebración o texto común para abrirlo.");
        findViewById(R.id.btnBack).setOnClickListener(v -> back());

        try {
            entries = EpubUtils.tableOfContents(this, volume);
            list = findViewById(R.id.listItems);
            showEntries("");
            list.setOnItemClickListener((parent, view, position, id) -> {
                Intent intent = new Intent(this, HoursReaderActivity.class);
                intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, volume.id);
                intent.putExtra(HoursReaderActivity.EXTRA_TOC_INDEX,
                        displayedIndices.get(position));
                startActivity(intent);
            });
            EditText filter = findViewById(R.id.inputListFilter);
            filter.setVisibility(View.VISIBLE);
            filter.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    showEntries(s == null ? "" : s.toString());
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo leer el índice del EPUB.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void showEntries(String query) {
        String wanted = normalize(query);
        List<Map<String, String>> rows = new ArrayList<>();
        displayedIndices.clear();
        for (int i = 0; i < entries.size(); i++) {
            EpubTocEntry entry = entries.get(i);
            if (devotional && HoursRepository.shouldHideDevotionalEntry(entry.title)) continue;
            if (latin && (entry.filePath.contains("..")
                    || entry.title.trim().startsWith("↑"))) continue;
            if (!wanted.isEmpty() && !normalize(entry.title).contains(wanted)) continue;
            rows.add(Rows.row(indent(entry.depth) + entry.title,
                    devotional ? (entry.depth == 0 ? "Sección" : "Oración o fórmula")
                            : latin ? "Mensis · 2026"
                            : missal ? volume.subtitle
                            : wayOfCross ? "Estación del Viacrucis · sin conexión"
                            : HoursRepository.isReference(volume) ? volume.subtitle
                            : (entry.depth <= 1 ? volume.subtitle : "Texto litúrgico")));
            displayedIndices.add(i);
        }
        list.setAdapter(Rows.adapter(this, rows));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }

    private static String indent(int depth) {
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < depth; i++) result.append("   ");
        return result.toString();
    }

    private void back() {
        finish();
    }

    @Override public void onBackPressed() { back(); }
}
