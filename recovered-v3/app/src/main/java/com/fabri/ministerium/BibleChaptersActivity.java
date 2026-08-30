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

public class BibleChaptersActivity extends ThemedActivity {
    public static final String EXTRA_BOOK_INDEX = "book_index";
    private BibleRepository.Book book; private int bookIndex; private final List<Integer> visible = new ArrayList<>(); private ListView list;
    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this); super.onCreate(savedInstanceState); setContentView(R.layout.activity_simple_list);
        try { List<BibleRepository.Book> books = BibleRepository.books(this); bookIndex = Math.max(0, Math.min(books.size() - 1, getIntent().getIntExtra(EXTRA_BOOK_INDEX, 0))); book = books.get(bookIndex); }
        catch (Exception error) { Toast.makeText(this, "No se pudo abrir el índice bíblico.", Toast.LENGTH_LONG).show(); finish(); return; }
        ((TextView) findViewById(R.id.txtTitle)).setText(book.title); ((TextView) findViewById(R.id.txtSubtitle)).setText(book.abbreviation + " · " + book.testament);
        ((TextView) findViewById(R.id.txtIntro)).setText("Selecciona el capítulo que deseas leer."); findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        list = findViewById(R.id.listItems); for (int i = 0; i < book.chapters.size(); i++) visible.add(i); showRows();
        EditText filter = findViewById(R.id.inputListFilter); filter.setVisibility(View.VISIBLE); filter.setInputType(android.text.InputType.TYPE_CLASS_NUMBER); filter.setHint("Ir al capítulo");
        filter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { visible.clear(); String wanted = s.toString().trim(); for (int i = 0; i < book.chapters.size(); i++) if (wanted.isEmpty() || String.valueOf(book.chapters.get(i).number).startsWith(wanted)) visible.add(i); showRows(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        list.setOnItemClickListener((parent, view, position, id) -> { Intent intent = new Intent(this, BibleReaderActivity.class); intent.putExtra(BibleReaderActivity.EXTRA_BOOK_INDEX, bookIndex); intent.putExtra(BibleReaderActivity.EXTRA_CHAPTER_INDEX, visible.get(position)); startActivity(intent); });
    }
    private void showRows() { List<Map<String, String>> rows = new ArrayList<>(); for (int index : visible) rows.add(Rows.row("Capítulo " + book.chapters.get(index).number, book.title + " " + book.chapters.get(index).number)); list.setAdapter(Rows.adapter(this, rows)); }
}
