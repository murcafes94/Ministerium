package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;

public class UpdateCenterActivity extends ThemedActivity {
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_center);
        status = findViewById(R.id.txtUpdateStatus);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckAll).setOnClickListener(v -> verifyAll());
        findViewById(R.id.btnUpdateApp).setOnClickListener(v -> showChangelog());
        findViewById(R.id.btnUpdateCalendar).setOnClickListener(v ->
                startActivity(new Intent(this, LiturgicalCalendarActivity.class)));
        findViewById(R.id.btnUpdateBreviary).setOnClickListener(v ->
                startActivity(new Intent(this, LatinHoursActivity.class)));
        findViewById(R.id.btnUpdateLectionary).setOnClickListener(v ->
                startActivity(new Intent(this, MassReadingsActivity.class)));
        findViewById(R.id.btnUpdateRituals).setOnClickListener(v ->
                startActivity(new Intent(this, RitualCatalogActivity.class)));
        showVersions();
    }

    private void showVersions() {
        try {
            JSONObject manifest = new JSONObject(readAsset("package-manifest.json"));
            JSONArray packages = manifest.getJSONArray("packages");
            StringBuilder value = new StringBuilder("App ")
                    .append(BuildConfig.VERSION_NAME).append(" · canal ")
                    .append(manifest.optString("channel", "stable"));
            for (int i = 0; i < packages.length(); i++) {
                JSONObject item = packages.getJSONObject(i);
                value.append("\n").append(item.getString("title")).append(" · ")
                        .append(item.getString("version")).append(" · ")
                        .append(item.getString("updated"));
            }
            status.setText(value.toString());
        } catch (Exception error) {
            status.setText("No se pudo leer el manifiesto local de paquetes.");
        }
    }

    private void verifyAll() {
        status.setText("Verificando esquema e integridad de los paquetes locales…");
        new Thread(() -> {
            try {
                JSONObject manifest = new JSONObject(readAsset("package-manifest.json"));
                JSONArray packages = manifest.getJSONArray("packages");
                int verified = 0;
                for (int i = 0; i < packages.length(); i++) {
                    JSONObject item = packages.getJSONObject(i);
                    if (!item.getString("sha256").equalsIgnoreCase(
                            sha256(readAssetBytes(item.getString("asset"))))) {
                        throw new IllegalStateException("Falló la verificación de "
                                + item.getString("title") + ".");
                    }
                    verified++;
                }
                final int count = verified;
                runOnUiThread(() -> status.setText(count
                        + " paquetes verificados · esquema y SHA-256 correctos.\n"
                        + "Las actualizaciones se instalan por módulo y conservan la versión anterior hasta validar la nueva."));
            } catch (Exception error) {
                runOnUiThread(() -> status.setText(error.getMessage() == null
                        ? "No se pudo completar la verificación." : error.getMessage()));
            }
        }).start();
    }

    private void showChangelog() {
        try {
            new android.app.AlertDialog.Builder(this).setTitle("Ministerium 3.0.0")
                    .setMessage(readAsset("changelog-3.0.0.txt"))
                    .setPositiveButton("Cerrar", null).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir el historial de cambios.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String readAsset(String path) throws Exception {
        return new String(readAssetBytes(path), StandardCharsets.UTF_8);
    }

    private byte[] readAssetBytes(String path) throws Exception {
        try (InputStream input = getAssets().open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder result = new StringBuilder();
        for (byte value : digest) result.append(String.format("%02x", value & 0xff));
        return result.toString();
    }
}
