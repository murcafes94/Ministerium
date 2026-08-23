package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CatalogActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT = "document_id";
    public static final String EXTRA_SCREEN_TITLE = "screen_title";

    private DocumentInfo document;
    private List<CatalogEntry> entries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        String documentId = getIntent().getStringExtra(EXTRA_DOCUMENT);
        document = ContentRepository.document(documentId);
        if (document == null) {
            finish();
            return;
        }

        String screenTitle = getIntent().getStringExtra(EXTRA_SCREEN_TITLE);
        ((TextView) findViewById(R.id.txtTitle)).setText(
                screenTitle == null ? document.title : screenTitle);
        ((TextView) findViewById(R.id.txtSubtitle)).setText(document.subtitle);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        entries = ContentRepository.catalog(document.id);
        List<Map<String, String>> rows = new ArrayList<>();
        for (CatalogEntry entry : entries) {
            rows.add(Rows.row(entry.title, entry.section));
        }

        ListView list = findViewById(R.id.listCatalog);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> openSection(position));

        findViewById(R.id.btnSearchDocument).setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra(SearchActivity.EXTRA_DOCUMENT, document.id);
            startActivity(intent);
        });
        findViewById(R.id.btnOpenOriginal).setOnClickListener(v -> openPdf(0));
    }

    private void openSection(int position) {
        Intent intent = new Intent(this, DocumentSectionReaderActivity.class);
        intent.putExtra(DocumentSectionReaderActivity.EXTRA_DOCUMENT, document.id);
        intent.putExtra(DocumentSectionReaderActivity.EXTRA_ENTRY_INDEX, position);
        startActivity(intent);
    }

    private void openPdf(int pageIndex) {
        Intent intent = new Intent(this, PdfReaderActivity.class);
        intent.putExtra(PdfReaderActivity.EXTRA_DOCUMENT, document.id);
        intent.putExtra(PdfReaderActivity.EXTRA_PAGE, pageIndex);
        startActivity(intent);
    }
}
