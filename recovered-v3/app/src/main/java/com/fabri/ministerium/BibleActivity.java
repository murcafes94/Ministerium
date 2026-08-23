package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BibleActivity extends ThemedActivity {
    private static final int BOOKS = 0;
    private static final int RECENT = 1;
    private List<BibleRepository.Book> books;
    private final List<BibleRepository.Book> visible = new ArrayList<>();
    private List<BibleHistoryStore.Entry> recent = new ArrayList<>();
    private ListView list;
    private EditText filter;
    private TextView intro;
    private Button booksTab;
    private Button recentTab;
    private int mode = BOOKS;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bible_library);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        list = findViewById(R.id.listBibleItems);
        filter = findViewById(R.id.inputBibleFilter);
        intro = findViewById(R.id.txtBibleIntro);
        booksTab = findViewById(R.id.btnBibleBooks);
        recentTab = findViewById(R.id.btnBibleRecent);
        try {
            books = BibleRepository.books(this);
            visible.addAll(books);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo preparar la Biblia local.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        booksTab.setOnClickListener(v -> selectMode(BOOKS));
        recentTab.setOnClickListener(v -> selectMode(RECENT));
        findViewById(R.id.btnBibleMarkers).setOnClickListener(v ->
                startActivity(new Intent(this, MarkersActivity.class)));
        findViewById(R.id.btnBiblePlans).setOnClickListener(v ->
                startActivity(new Intent(this, BiblePlansActivity.class)));
        findViewById(R.id.btnBibleSearch).setOnClickListener(v ->
                startActivity(new Intent(this, BibleSearchActivity.class)));
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mode == BOOKS) filterBooks(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (mode == BOOKS) {
                BibleRepository.Book book = visible.get(position);
                Intent intent = new Intent(this, BibleChaptersActivity.class);
                intent.putExtra(BibleChaptersActivity.EXTRA_BOOK_INDEX, books.indexOf(book));
                startActivity(intent);
            } else if (position < recent.size()) {
                BibleHistoryStore.Entry entry = recent.get(position);
                Intent intent = new Intent(this, BibleReaderActivity.class);
                intent.putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, entry.bookIndex);
                intent.putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, entry.chapterIndex);
                startActivity(intent);
            }
        });
        selectMode(BOOKS);
    }

    @Override protected void onResume() {
        super.onResume();
        if (mode == RECENT) showRecent();
    }

    private void selectMode(int next) {
        mode = next;
        booksTab.setAlpha(mode == BOOKS ? 1f : .68f);
        recentTab.setAlpha(mode == RECENT ? 1f : .68f);
        filter.setVisibility(mode == BOOKS ? View.VISIBLE : View.GONE);
        if (mode == BOOKS) filterBooks(filter.getText().toString());
        else showRecent();
    }

    private void filterBooks(String query) {
        String wanted = normalize(query);
        visible.clear();
        for (BibleRepository.Book book : books) {
            if (normalize(book.title + " " + book.abbreviation + " "
                    + book.testament).contains(wanted)) visible.add(book);
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (BibleRepository.Book book : visible) {
            rows.add(Rows.row(book.title, BibleRepository.citationAbbreviation(book)
                    + " · " + book.chapters.size() + " capítulos · " + book.testament));
        }
        intro.setText("73 libros · elige un libro y un capítulo. El buscador reconoce frases y referencias; las notas se abren sin abandonar el pasaje.");
        list.setAdapter(Rows.adapter(this, rows));
    }

    private void showRecent() {
        recent = BibleHistoryStore.recent(this);
        List<Map<String, String>> rows = new ArrayList<>();
        for (BibleHistoryStore.Entry entry : recent) {
            rows.add(Rows.row(entry.citation, entry.title + " · última lectura guardada"));
        }
        intro.setText(recent.isEmpty()
                ? "Los libros y capítulos que leas aparecerán aquí para volver rápidamente."
                : "Tus 15 capítulos más recientes. Toca una cita para continuar leyendo.");
        list.setAdapter(Rows.adapter(this, rows));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).trim();
    }
}
