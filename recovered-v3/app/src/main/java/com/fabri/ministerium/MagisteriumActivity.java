package com.fabri.ministerium;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MagisteriumActivity extends ThemedActivity {
    private static final String OGLH_URL =
            "https://liturgiapapal.org/attachments/article/602/OGLH.pdf";

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_list);

        ((TextView) findViewById(R.id.txtTitle)).setText("Magisterio");
        ((TextView) findViewById(R.id.txtSubtitle)).setText(
                "Documentos, doctrina y derecho de la Iglesia");
        ((TextView) findViewById(R.id.txtIntro)).setText(
                "Los documentos EPUB y el Código bilingüe están disponibles sin conexión. "
                + "La flecha Atrás vuelve siempre al índice inmediatamente anterior.");
        findViewById(R.id.btnBack).setOnClickListener(v -> exitToHome());

        List<Map<String, String>> rows = new ArrayList<>();
        for (HoursVolume volume : HoursRepository.references()) {
            rows.add(Rows.row(volume.title, volume.subtitle + " · sin conexión"));
        }
        rows.add(Rows.row("Código de Derecho Canónico",
                "1.752 cánones · español y latín · texto local consolidado"));
        rows.add(Rows.row("Comentarios al Código de Derecho Canónico",
                "Comentarios enlazados por canon · texto sin conexión"));
        rows.add(Rows.row("Ordenación General de la Liturgia de las Horas",
                "Instrucción general · documento web en PDF"));

        ListView list = findViewById(R.id.listItems);
        list.setAdapter(Rows.adapter(this, rows));
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (position < HoursRepository.references().size()) {
                openEpub(HoursRepository.references().get(position));
            } else if (position == HoursRepository.references().size()) {
                startActivity(new Intent(this, CanonLawActivity.class));
            } else if (position == HoursRepository.references().size() + 1) {
                startActivity(new Intent(this, CanonLawActivity.class));
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(OGLH_URL)));
            }
        });
    }

    private void openEpub(HoursVolume volume) {
        Intent intent = new Intent(this, HoursTocActivity.class);
        intent.putExtra(HoursTocActivity.EXTRA_VOLUME_ID, volume.id);
        startActivity(intent);
    }

    private void exitToHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override public void onBackPressed() { exitToHome(); }
}
