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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BibleDictionaryActivity extends ThemedActivity {
    public static final String EXTRA_SOURCE_ID = "dictionary_source_id";
    private static final int MAXIMUM_RESULTS = 120;
    private final List<BibleDictionaryRepository.Entry> visible = new ArrayList<>();
    private List<BibleDictionaryRepository.Entry> entries = new ArrayList<>();
    private BibleDictionaryRepository.Source source;
    private ListView list;
    private EditText filter;
    private TextView intro;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        list = findViewById(R.id.listItems);
        filter = findViewById(R.id.inputListFilter);
        intro = findViewById(R.id.txtIntro);
        source = BibleDictionaryRepository.findSource(
                getIntent().getStringExtra(EXTRA_SOURCE_ID));
        if (source == null) showSources();
        else showDictionary();
    }

    private void showSources() {
        ((TextView) findViewById(R.id.txtTitle)).setText("Diccionarios");
        ((TextView) findViewById(R.id.txtSubtitle)).setText(
                "Biblia · Teología · Lengua española");
        intro.setText("Elige una fuente. Todos los diccionarios funcionan sin conexión.");
        filter.setVisibility(View.GONE);
        List<Map<String, String>> rows = new ArrayList<>();
        for (BibleDictionaryRepository.Source item : BibleDictionaryRepository.sources()) {
            rows.add(Rows.row(item.title, item.subtitle));
        }
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            BibleDictionaryRepository.Source selected =
                    BibleDictionaryRepository.sources().get(position);
            Intent intent = new Intent(this, BibleDictionaryActivity.class);
            intent.putExtra(EXTRA_SOURCE_ID, selected.id);
            startActivity(intent);
        });
    }

    private void showDictionary() {
        ((TextView) findViewById(R.id.txtTitle)).setText(source.title);
        ((TextView) findViewById(R.id.txtSubtitle)).setText(source.subtitle);
        try {
            entries = BibleDictionaryRepository.entries(this, source);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir este diccionario.",
                    Toast.LENGTH_LONG).show();
        }
        list.setOnItemClickListener((parent, view, position, id) -> {
            BibleDictionaryRepository.Entry entry = visible.get(position);
            Intent intent = new Intent(this, HoursReaderActivity.class);
            intent.putExtra(HoursReaderActivity.EXTRA_VOLUME_ID, source.volume.id);
            intent.putExtra(HoursReaderActivity.EXTRA_FILE_PATH, entry.filePath);
            intent.putExtra(HoursReaderActivity.EXTRA_FRAGMENT, entry.fragment);
            intent.putExtra(HoursReaderActivity.EXTRA_ENTRY_TITLE, entry.term);
            if ("rae_15".equals(source.id)) {
                intent.putExtra(HoursReaderActivity.EXTRA_DICTIONARY_TERM, entry.term);
            }
            startActivity(intent);
        });
        filter.setVisibility(entries.isEmpty() ? View.GONE : View.VISIBLE);
        filter.setHint("Buscar palabra o tema");
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                showResults(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        showResults("");
    }

    private void showResults(String query) {
        String wanted = normalize(query);
        visible.clear();
        if (!wanted.isEmpty()) {
            for (BibleDictionaryRepository.Entry entry : entries) {
                if (entry.normalizedTerm.startsWith(wanted)) {
                    visible.add(entry);
                    if (visible.size() >= MAXIMUM_RESULTS) break;
                }
            }
            if (visible.size() < MAXIMUM_RESULTS) {
                for (BibleDictionaryRepository.Entry entry : entries) {
                    if (!entry.normalizedTerm.startsWith(wanted)
                            && entry.normalizedTerm.contains(wanted)) {
                        visible.add(entry);
                        if (visible.size() >= MAXIMUM_RESULTS) break;
                    }
                }
            }
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (BibleDictionaryRepository.Entry entry : visible) {
            rows.add(Rows.row(entry.term, "Toca para abrir la entrada en un recuadro de lectura"));
        }
        String count = String.format(Locale.ROOT, "%,d", entries.size()).replace(',', '.');
        String message = entries.isEmpty()
                ? "El archivo del diccionario no está disponible."
                : wanted.isEmpty()
                ? count + " entradas disponibles. Escribe una palabra o tema para buscar."
                : visible.isEmpty()
                ? "No se encontraron entradas para «" + query.trim() + "»."
                : visible.size() >= MAXIMUM_RESULTS
                ? "Mostrando los primeros " + MAXIMUM_RESULTS + " resultados. Escribe más letras para precisar."
                : visible.size() + " resultados encontrados.";
        intro.setText(message);
        list.setAdapter(Rows.adapter(this, rows));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
}
