package com.fabri.ministerium;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class FeedbackActivity extends ThemedActivity {
    private Spinner type;
    private Spinner module;
    private EditText message;
    private EditText steps;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);
        type = findViewById(R.id.spinnerFeedbackType);
        module = findViewById(R.id.spinnerFeedbackModule);
        message = findViewById(R.id.inputFeedbackMessage);
        steps = findViewById(R.id.inputFeedbackSteps);
        type.setAdapter(adapter(new String[]{"Error", "Opinión", "Calificación", "Sugerencia"}));
        module.setAdapter(adapter(new String[]{"General", "Biblia", "Liturgia", "Misal",
                "Lecturas", "Estudio", "Planes", "Actualizaciones"}));
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSendFeedback).setOnClickListener(v -> send());
    }

    private ArrayAdapter<String> adapter(String[] values) {
        return new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, values);
    }

    private void send() {
        String text = message.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Escribe primero tu comentario.", Toast.LENGTH_SHORT).show();
            return;
        }
        String kind = type.getSelectedItem().toString();
        String area = module.getSelectedItem().toString();
        StringBuilder body = new StringBuilder().append(kind).append(" · ").append(area)
                .append("\n\n").append(text);
        String detail = steps.getText().toString().trim();
        if (!detail.isEmpty()) body.append("\n\nPasos o contexto:\n").append(detail);
        body.append("\n\n---\nMinisterium ").append(BuildConfig.VERSION_NAME)
                .append(" · Android ").append(Build.VERSION.RELEASE)
                .append(" · ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL)
                .append("\nchannel: stable\nlabel: type:")
                .append(kind.toLowerCase(java.util.Locale.ROOT))
                .append("\nlabel: module:").append(area.toLowerCase(java.util.Locale.ROOT));
        Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Ministerium · " + kind + " · " + area)
                .putExtra(Intent.EXTRA_TEXT, body.toString());
        startActivity(Intent.createChooser(intent, "Enviar comentario de forma segura"));
    }
}
