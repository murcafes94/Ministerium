package com.fabri.ministerium;

import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ConscienceReaderActivity extends ThemedActivity {
    public static final String EXTRA_TITLE = "conscience_title";
    public static final String EXTRA_ASSET = "conscience_asset";
    private TextView content;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry_reader);
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String asset = getIntent().getStringExtra(EXTRA_ASSET);
        ((TextView) findViewById(R.id.txtReaderTitle)).setText(title);
        ((TextView) findViewById(R.id.txtReaderSubtitle)).setText("Examen de conciencia");
        ((TextView) findViewById(R.id.txtSource)).setText(
                "Texto extraído del documento proporcionado · sin conexión");
        content = findViewById(R.id.txtContent);
        try { content.setText(readAsset(asset)); }
        catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir el examen.", Toast.LENGTH_LONG).show();
            finish(); return;
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnFavorite).setVisibility(android.view.View.GONE);
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderContext context = new ReaderContext("Examen de conciencia", "conscience:" + asset,
                title, title, "Oraciones", false);
        TextViewReaderChrome.attach(this, content, findViewById(R.id.readerScroll),
                findViewById(R.id.readerHeader), context, null);
        TextViewReaderChrome.bindMore(this, findViewById(R.id.btnReaderMore), content, context);
    }

    private String readAsset(String asset) throws Exception {
        try (InputStream input = getAssets().open(asset);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toString(StandardCharsets.UTF_8.name()).trim();
        }
    }

}
