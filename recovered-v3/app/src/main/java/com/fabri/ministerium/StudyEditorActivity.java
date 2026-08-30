package com.fabri.ministerium;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** Editor de notas/meditaciones con autosave real, ancla portable y etiquetas. */
public class StudyEditorActivity extends ThemedActivity {
    public static final String EXTRA_TYPE = "study_type";
    public static final String EXTRA_CATEGORY = "study_category";
    public static final String EXTRA_SOURCE = "study_source";
    public static final String EXTRA_SOURCE_KEY = "study_source_key";
    public static final String EXTRA_CONTENT_ID = "study_content_id";
    public static final String EXTRA_REFERENCE = "study_reference";
    public static final String EXTRA_QUOTE = "study_quote";
    public static final String EXTRA_ANCHOR_TEXT = "study_anchor_text";
    public static final String EXTRA_SEMANTIC_UNIT_ID = "study_semantic_unit_id";
    public static final String EXTRA_START_OFFSET = "study_start_offset";
    public static final String EXTRA_END_OFFSET = "study_end_offset";
    public static final String EXTRA_PREFIX = "study_prefix";
    public static final String EXTRA_SUFFIX = "study_suffix";
    private static final String DRAFTS = "study_editor_drafts";
    private static final long AUTOSAVE_DELAY_MS = 500L;

    private EditText title;
    private EditText body;
    private EditText tags;
    private StudyEntry entry;
    private String draftKey;
    private final Handler autosaveHandler = new Handler(Looper.getMainLooper());
    private final Runnable autosaveTask = () -> persist(false, false);
    private boolean finishingExplicitly;

    @Override protected void onCreate(Bundle state) {
        ThemeUtils.apply(this);
        super.onCreate(state);
        setContentView(R.layout.activity_study_editor);
        title = findViewById(R.id.inputStudyTitle);
        body = findViewById(R.id.inputStudyBody);
        tags = findViewById(R.id.inputStudyTags);
        entry = new StudyEntry();
        entry.type = value(getIntent().getStringExtra(EXTRA_TYPE), StudyEntry.MEDITATION);
        entry.category = value(getIntent().getStringExtra(EXTRA_CATEGORY), "Libres");
        entry.source = value(getIntent().getStringExtra(EXTRA_SOURCE), "");
        entry.sourceKey = value(getIntent().getStringExtra(EXTRA_SOURCE_KEY), "");
        entry.contentId = value(getIntent().getStringExtra(EXTRA_CONTENT_ID),
                ContentReference.infer(entry.sourceKey, getIntent().getStringExtra(EXTRA_REFERENCE)));
        entry.reference = value(getIntent().getStringExtra(EXTRA_REFERENCE), "");
        entry.quote = value(getIntent().getStringExtra(EXTRA_QUOTE), "");
        entry.anchorText = value(getIntent().getStringExtra(EXTRA_ANCHOR_TEXT), entry.quote);
        entry.semanticUnitId = value(getIntent().getStringExtra(EXTRA_SEMANTIC_UNIT_ID), "");
        entry.startOffset = getIntent().getIntExtra(EXTRA_START_OFFSET, -1);
        entry.endOffset = getIntent().getIntExtra(EXTRA_END_OFFSET, -1);
        entry.prefix = value(getIntent().getStringExtra(EXTRA_PREFIX), "");
        entry.suffix = value(getIntent().getStringExtra(EXTRA_SUFFIX), "");
        entry.anchorVersion = StudyEntry.CURRENT_ANCHOR_VERSION;
        draftKey = entry.type + ":" + entry.sourceKey + ":" + entry.semanticUnitId
                + ":" + entry.startOffset + ":" + entry.endOffset + ":"
                + Integer.toHexString(entry.quote.hashCode());

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
                scheduleAutosave();
            }

            @Override public void afterTextChanged(Editable s) {}
        };
        title.addTextChangedListener(autosave);
        body.addTextChangedListener(autosave);
        tags.addTextChangedListener(autosave);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            autosaveHandler.removeCallbacks(autosaveTask);
            persist(false, false);
            finish();
        });
        findViewById(R.id.btnSaveStudy).setOnClickListener(v -> persist(true, true));
    }

    @Override protected void onPause() {
        autosaveHandler.removeCallbacks(autosaveTask);
        if (!finishingExplicitly) persist(false, false);
        super.onPause();
    }

    @Override protected void onDestroy() {
        autosaveHandler.removeCallbacks(autosaveTask);
        super.onDestroy();
    }

    private void restoreDraft() {
        String raw = drafts().getString(draftKey, "");
        if (raw.isEmpty()) return;
        try {
            JSONObject draft = new JSONObject(raw);
            String draftId = draft.optString("entryId");
            if (!draftId.isEmpty()) entry.id = draftId;
            title.setText(draft.optString("title"));
            body.setText(draft.optString("body"));
            tags.setText(draft.optString("tags"));
            Toast.makeText(this, "Se recuperó el borrador guardado automáticamente.",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception ignored) {}
    }

    private void saveDraft() {
        if (draftKey == null || finishingExplicitly) return;
        try {
            drafts().edit().putString(draftKey, new JSONObject()
                    .put("entryId", entry.id == null ? "" : entry.id)
                    .put("title", title.getText().toString())
                    .put("body", body.getText().toString())
                    .put("tags", tags.getText().toString()).toString()).apply();
        } catch (Exception ignored) {}
    }

    private void scheduleAutosave() {
        autosaveHandler.removeCallbacks(autosaveTask);
        autosaveHandler.postDelayed(autosaveTask, AUTOSAVE_DELAY_MS);
    }

    private void persist(boolean requireContent, boolean finishAfter) {
        if (title == null || body == null || tags == null || entry == null) return;
        String content = body.getText().toString().trim();
        if (content.isEmpty()) {
            if (requireContent) body.setError("Escribe el contenido");
            return;
        }

        entry.title = title.getText().toString().trim();
        if (entry.title.isEmpty()) entry.title = StudyEntry.NOTE.equals(entry.type)
                ? "Nota" : "Meditación";
        entry.body = content;
        entry.tags.clear();
        for (String raw : tags.getText().toString().split("[,;]")) {
            String tag = raw.trim();
            if (!tag.isEmpty() && !entry.tags.contains(tag)) entry.tags.add(tag);
        }
        StudyStore.save(this, entry);
        drafts().edit().remove(draftKey).apply();

        if (finishAfter) {
            finishingExplicitly = true;
            autosaveHandler.removeCallbacks(autosaveTask);
            Toast.makeText(this, "Guardado en Mi estudio.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private SharedPreferences drafts() {
        return getSharedPreferences(DRAFTS, Context.MODE_PRIVATE);
    }

    private static String value(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }
}
