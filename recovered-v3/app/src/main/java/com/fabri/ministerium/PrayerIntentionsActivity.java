package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class PrayerIntentionsActivity extends ThemedActivity {
    private final List<String> intentions = new ArrayList<>();
    private IntentionsAdapter adapter;
    private TextView empty;
    private Button prayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_intentions);

        empty = findViewById(R.id.txtIntentionsEmpty);
        prayButton = findViewById(R.id.btnPrayIntentions);
        adapter = new IntentionsAdapter();
        ListView list = findViewById(R.id.listIntentions);
        list.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddIntention).setOnClickListener(v -> edit(-1));
        prayButton.setOnClickListener(v -> prayNow());
        reload();
    }

    private void reload() {
        intentions.clear();
        intentions.addAll(IntentionsStore.get(this));
        adapter.notifyDataSetChanged();
        empty.setVisibility(intentions.isEmpty() ? View.VISIBLE : View.GONE);
        prayButton.setEnabled(!intentions.isEmpty());
        prayButton.setAlpha(intentions.isEmpty() ? .45f : 1f);
        prayButton.setText(intentions.isEmpty()
                ? "Presentar mis intenciones"
                : "Presentar mis intenciones · " + intentions.size());
    }

    private void prayNow() {
        if (intentions.isEmpty()) return;
        StringBuilder text = new StringBuilder();
        for (String intention : intentions) {
            if (text.length() > 0) text.append("\n\n");
            text.append("• ").append(intention);
        }
        startActivity(new Intent(this, PrayerReaderActivity.class)
                .putExtra(PrayerReaderActivity.EXTRA_DIRECT_TITLE, "Mis intenciones")
                .putExtra(PrayerReaderActivity.EXTRA_DIRECT_SUBTITLE, "Oración privada")
                .putExtra(PrayerReaderActivity.EXTRA_DIRECT_TEXT, text.toString()));
    }

    private void edit(int position) {
        EditText input = new EditText(this);
        input.setSingleLine(false);
        input.setMinLines(2);
        input.setMaxLines(5);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setPadding(40, 20, 40, 20);
        if (position >= 0) {
            input.setText(intentions.get(position));
            input.setSelection(input.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(position >= 0 ? "Editar intención" : "Añadir intención")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) {
                        Toast.makeText(this, "Escribe una intención.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (position >= 0) intentions.set(position, value);
                    else intentions.add(value);
                    IntentionsStore.save(this, intentions);
                    dialog.dismiss();
                    reload();
                }));
        dialog.show();
    }

    private void delete(int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar intención")
                .setMessage("¿Deseas eliminar esta intención de oración?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    intentions.remove(position);
                    IntentionsStore.save(this, intentions);
                    reload();
                })
                .show();
    }

    private final class IntentionsAdapter extends BaseAdapter {
        @Override public int getCount() { return intentions.size(); }
        @Override public String getItem(int position) { return intentions.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                row = LayoutInflater.from(PrayerIntentionsActivity.this)
                        .inflate(R.layout.row_intention, parent, false);
            }
            TextView text = row.findViewById(R.id.txtIntention);
            Button editButton = row.findViewById(R.id.btnEditIntention);
            Button deleteButton = row.findViewById(R.id.btnDeleteIntention);
            text.setText(getItem(position));
            text.setOnClickListener(v -> edit(position));
            editButton.setOnClickListener(v -> edit(position));
            deleteButton.setOnClickListener(v -> delete(position));
            return row;
        }
    }
}
