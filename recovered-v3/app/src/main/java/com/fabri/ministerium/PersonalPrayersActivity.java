package com.fabri.ministerium;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PersonalPrayersActivity extends ThemedActivity {
    private List<PersonalPrayer> prayers;
    private ListView list;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_prayers);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddPrayer).setOnClickListener(v -> edit(null));
        list = findViewById(R.id.listItems);
        list.setOnItemClickListener((parent, view, position, id) ->
                open(prayers.get(position)));
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            options(prayers.get(position));
            return true;
        });
        reload();
    }

    @Override protected void onResume() {
        super.onResume();
        if (list != null) reload();
    }

    private void reload() {
        prayers = PersonalPrayerStore.all(this);
        List<Map<String, String>> rows = new ArrayList<>();
        for (PersonalPrayer prayer : prayers) {
            String preview = prayer.text.replaceAll("\\s+", " ").trim();
            if (preview.length() > 150) preview = preview.substring(0, 149) + "…";
            rows.add(Rows.row(prayer.title, preview));
        }
        ((TextView) findViewById(R.id.txtIntro)).setText(prayers.isEmpty()
                ? "Crea tus propias oraciones. Permanecen privadas en este dispositivo."
                : "Toca para entrar en modo de oración. Mantén pulsado para editar o eliminar.");
        list.setAdapter(Rows.adapter(this, rows));
    }

    private void open(PersonalPrayer prayer) {
        startActivity(new Intent(this, PrayerReaderActivity.class)
                .putExtra(PrayerReaderActivity.EXTRA_PERSONAL_PRAYER_ID, prayer.id));
    }

    private void options(PersonalPrayer prayer) {
        new AlertDialog.Builder(this)
                .setTitle(prayer.title)
                .setItems(new String[]{"Editar", "Eliminar"}, (dialog, which) -> {
                    if (which == 0) edit(prayer);
                    else delete(prayer);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void edit(PersonalPrayer existing) {
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, 0, pad, 0);
        EditText title = new EditText(this);
        title.setHint("Título");
        title.setSingleLine(true);
        EditText text = new EditText(this);
        text.setHint("Escribe la oración…");
        text.setMinLines(8);
        text.setGravity(Gravity.TOP);
        text.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (existing != null) {
            title.setText(existing.title);
            text.setText(existing.text);
        }
        box.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        box.addView(text, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nueva oración" : "Editar oración")
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String prayerTitle = title.getText().toString().trim();
                    String prayerText = text.getText().toString().trim();
                    if (prayerTitle.isEmpty()) {
                        title.setError("Escribe un título");
                        return;
                    }
                    if (prayerText.isEmpty()) {
                        text.setError("Escribe la oración");
                        return;
                    }
                    PersonalPrayer prayer = existing == null ? new PersonalPrayer() : existing;
                    if (existing == null) prayer.id = UUID.randomUUID().toString();
                    prayer.title = prayerTitle;
                    prayer.text = prayerText;
                    PersonalPrayerStore.save(this, prayer);
                    dialog.dismiss();
                    reload();
                }));
        dialog.show();
    }

    private void delete(PersonalPrayer prayer) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar oración")
                .setMessage("¿Deseas eliminar «" + prayer.title + "» de este dispositivo?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    PersonalPrayerStore.delete(this, prayer.id);
                    reload();
                })
                .show();
    }
}
