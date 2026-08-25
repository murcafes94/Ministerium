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
            int year = Calendar.getInstance().get(Calendar.YEAR);
            boolean calendarAvailable = LiturgicalCalendarRepository.hasCalendar(this, year);
            StringBuilder value = new StringBuilder("App ")
                    .append(BuildConfig.VERSION_NAME).append(" · canal ")
                    .append(manifest.optString("channel", "testing"))
                    .append("\nCalendario ").append(year).append(calendarAvailable
                            ? " · disponible localmente" : " · pendiente");
            for (int i = 0; i < packages.length(); i++) {
                JSONObject item = packages.getJSONObject(i);
                if ("calendar-ec".equals(item.optString("id"))) continue;
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
        status.setText("Verificando los paquetes instalados…");
        new Thread(() -> {
            try {
                JSONObject manifest = new JSONObject(readAsset("package-manifest.json"));
                JSONArray packages = manifest.getJSONArray("packages");
                int verified = 0;
                for (int i = 0; i < packages.length(); i++) {
                    JSONObject item = packages.getJSONObject(i);
                    String asset = item.getString("asset");
                    byte[] bytes = readAssetBytes(asset);
                    String mode = item.optString("verification", "sha256");
                    if ("generated-build".equals(mode)) {
                        if (bytes.length < 20) {
                            throw new IllegalStateException("El paquete generado «"
                                    + item.getString("title") + "» está incompleto.");
                        }
                    } else {
                        String expected = item.optString("sha256", "");
                        if (expected.isEmpty() || !expected.equalsIgnoreCase(sha256(bytes))) {
                            throw new IllegalStateException("Falló la verificación de "
                                    + item.getString("title") + ".");
                        }
                    }
                    verified++;
                }
                final int count = verified;
                final int year = Calendar.getInstance().get(Calendar.YEAR);
                final boolean calendarAvailable = LiturgicalCalendarRepository.hasCalendar(this, year);
                runOnUiThread(() -> status.setText(count
                        + " paquetes locales verificados correctamente.\n"
                        + (calendarAvailable
                        ? "Calendario " + year + " disponible en el dispositivo. "
                        : "Calendario " + year + " no encontrado. ")
                        + "La comprobación de una actualización por Internet es independiente de que el calendario local funcione."));
            } catch (Exception error) {
                runOnUiThread(() -> status.setText(error.getMessage() == null
                        ? "No se pudo completar la verificación local." : error.getMessage()));
            }
        }).start();
    }

    private void showChangelog() {
        try {
            new android.app.AlertDialog.Builder(this).setTitle("Ministerium 3.1.1")
                    .setMessage(readAsset("changelog-3.1.1.txt"))
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
