package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class StudyEditorActivity extends ThemedActivity {
    public static final String EXTRA_TYPE = "study_type";
    public static final String EXTRA_CATEGORY = "study_category";
    public static final String EXTRA_SOURCE = "study_source";
    public static final String EXTRA_SOURCE_KEY = "study_source_key";
    public static final String EXTRA_REFERENCE = "study_reference";
    public static final String EXTRA_QUOTE = "study_quote";
    public static final String EXTRA_SEMANTIC_UNIT_ID = "study_semantic_unit_id";
    public static final String EXTRA_START_OFFSET = "study_start_offset";
    public static final String EXTRA_END_OFFSET = "study_end_offset";
    private static final String DRAFTS = "study_editor_drafts";
    private EditText title;
    private EditText body;
    private StudyEntry entry;
    private String draftKey;
    private boolean saved;

    @Override protected void onCreate(Bundle state) {
        ThemeUtils.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_study_editor);
        title = findViewById(R.id.inputStudyTitle);
        body = findViewById(R.id.inputStudyBody);
        entry = new StudyEntry();
        entry.type = value(getIntent().getStringExtra(EXTRA_TYPE), StudyEntry.MEDITATION);
        entry.category = value(getIntent().getStringExtra(EXTRA_CATEGORY), "Libres");
        entry.source = value(getIntent().getStringExtra(EXTRA_SOURCE), "");
        entry.sourceKey = value(getIntent().getStringExtra(EXTRA_SOURCE_KEY), "");
        entry.reference = value(getIntent().getStringExtra(EXTRA_REFERENCE), "");
        entry.quote = value(getIntent().getStringExtra(EXTRA_QUOTE), "");
        entry.semanticUnitId = value(getIntent().getStringExtra(EXTRA_SEMANTIC_UNIT_ID), "");
        entry.startOffset = getIntent().getIntExtra(EXTRA_START_OFFSET, -1);
        entry.endOffset = getIntent().getIntExtra(EXTRA_END_OFFSET, -1);
        draftKey = entry.type + ":" + entry.sourceKey;

        ((TextView) findViewById(R.id.txtStudyEditorTitle)).setText(
                StudyEntry.NOTE.equals(entry.type) ? "Nueva nota" : "Nueva meditación");
        TextView context = findViewById(R.id.txtStudyContext);
        String detail = entry.reference;
        if (!entry.quote.isEmpty()) detail += (detail.isEmpty() ? "" : "\n")
                + "“" + entry.quote + "”";
        context.setText(detail);
        context.setVisibility(detail.isEmpty() ? View.GONE : View.VISIBLE);
        restoreDraft();

        TextWatcher autosave = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveDraft();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        title.addTextChangedListener(autosave);
        body.addTextChangedListener(autosave);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveStudy).setOnClickListener(v -> save());
    }

    private void restoreDraft() {
        String raw = drafts().getString(draftKey, "");
        if (raw.isEmpty()) return;
        try {
            JSONObject draft = new JSONObject(raw);
            title.setText(draft.optString("title"));
            body.setText(draft.optString("body"));
            Toast.makeText(this, "Se recuperó el borrador guardado automáticamente.",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private void saveDraft() {
        if (saved || draftKey == null) return;
        try {
            drafts().edit().putString(draftKey, new JSONObject()
                    .put("title", title.getText().toString())
                    .put("body", body.getText().toString()).toString()).apply();
        } catch (Exception ignored) {}
    }

    private void save() {
        String content = body.getText().toString().trim();
        if (content.isEmpty()) {
            body.setError("Escribe el contenido");
            return;
        }
        entry.title = title.getText().toString().trim();
        if (entry.title.isEmpty()) entry.title = StudyEntry.NOTE.equals(entry.type)
                ? "Nota" : "Meditación";
        entry.body = content;
        StudyStore.save(this, entry);
        saved = true;
        drafts().edit().remove(draftKey).apply();
        Toast.makeText(this, "Guardado en Mi estudio.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private SharedPreferences drafts() {
        return getSharedPreferences(DRAFTS, Context.MODE_PRIVATE);
    }

    private static String value(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
