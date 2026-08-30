package com.fabri.ministerium;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
    private View lectionaryButton;
    private View calendarButton;
    private volatile boolean syncingLectionary;
    private volatile boolean syncingCalendar;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_center);
        status = findViewById(R.id.txtUpdateStatus);
        lectionaryButton = findViewById(R.id.btnUpdateLectionary);
        calendarButton = findViewById(R.id.btnUpdateCalendar);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckAll).setOnClickListener(v -> verifyAll());
        findViewById(R.id.btnUpdateApp).setOnClickListener(v -> showChangelog());
        calendarButton.setOnClickListener(v -> syncCalendar());
        findViewById(R.id.btnUpdateBreviary).setOnClickListener(v ->
                startActivity(new Intent(this, LatinHoursActivity.class)));
        lectionaryButton.setOnClickListener(v -> syncLectionary());
        findViewById(R.id.btnUpdateRituals).setOnClickListener(v ->
                startActivity(new Intent(this, PastoralActivity.class)));
        showVersions();
    }

    private void syncCalendar() {
        if (syncingCalendar) return;
        syncingCalendar = true;
        calendarButton.setEnabled(false);
        calendarButton.setAlpha(0.55f);
        final int year = Calendar.getInstance().get(Calendar.YEAR);
        final boolean hadLocal = LiturgicalCalendarRepository.hasCalendar(this, year);
        status.setText("Buscando actualización del Calendario litúrgico de Ecuador " + year + "…");
        LiturgicalCalendarRepository.updateYear(getApplicationContext(), year, updated ->
                runOnUiThread(() -> {
                    syncingCalendar = false;
                    calendarButton.setEnabled(true);
                    calendarButton.setAlpha(1f);
                    if (updated) {
                        status.setText("Calendario " + year + " actualizado y guardado en el dispositivo.");
                        Toast.makeText(this, "Calendario litúrgico actualizado.", Toast.LENGTH_LONG).show();
                    } else {
                        status.setText(hadLocal
                                ? "No se pudo buscar una revisión nueva. El calendario local de " + year + " sigue disponible sin conexión."
                                : "No se pudo descargar el calendario de " + year + ". Inténtalo de nuevo cuando tengas conexión.");
                    }
                }));
    }

    private void syncLectionary() {
        if (syncingLectionary) return;
        syncingLectionary = true;
        lectionaryButton.setEnabled(false);
        lectionaryButton.setAlpha(0.55f);

        final Calendar requested = Calendar.getInstance();
        final int alreadySaved = MassReadingsRepository.cachedDays(this, requested);
        status.setText("Sincronizando Leccionario del mes actual… " + alreadySaved
                + " días ya estaban guardados.");

        new Thread(() -> {
            try {
                MassReadingsRepository.SyncResult result =
                        MassReadingsRepository.syncCurrentMonth(getApplicationContext(), requested,
                                (completed, total) -> runOnUiThread(() ->
                                        status.setText("Sincronizando Leccionario: " + completed
                                                + " de " + total + " días…")));
                runOnUiThread(() -> {
                    syncingLectionary = false;
                    lectionaryButton.setEnabled(true);
                    lectionaryButton.setAlpha(1f);
                    status.setText("Leccionario sincronizado: " + result.saved + " de "
                            + result.total + " días disponibles sin conexión.");
                    Toast.makeText(this, "Sincronización del Leccionario terminada.",
                            Toast.LENGTH_LONG).show();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    syncingLectionary = false;
                    lectionaryButton.setEnabled(true);
                    lectionaryButton.setAlpha(1f);
                    int saved = MassReadingsRepository.cachedDays(this, requested);
                    status.setText("La sincronización no terminó. " + saved
                            + " días ya guardados siguen disponibles sin conexión.");
                    Toast.makeText(this,
                            "No se pudo completar la sincronización del Leccionario.",
                            Toast.LENGTH_LONG).show();
                });
            }
        }, "ministerium-lectionary-sync").start();
    }

    private void showVersions() {
        try {
            JSONObject manifest = new JSONObject(readAsset("package-manifest.json"));
            JSONArray packages = manifest.getJSONArray("packages");
            int year = Calendar.getInstance().get(Calendar.YEAR);
            boolean calendarAvailable = LiturgicalCalendarRepository.hasCalendar(this, year);
            int readings = MassReadingsRepository.cachedDays(this, Calendar.getInstance());
            StringBuilder value = new StringBuilder("App ")
                    .append(BuildConfig.VERSION_NAME).append(" · canal ")
                    .append(manifest.optString("channel", "testing"))
                    .append("\nCalendario ").append(year).append(calendarAvailable
                            ? " · instalado y disponible" : " · no encontrado localmente")
                    .append("\nLeccionario del mes · ").append(readings)
                    .append(" días sincronizados");
            for (int i = 0; i < packages.length(); i++) {
                JSONObject item = packages.getJSONObject(i);
                if ("calendar-ec".equals(item.optString("id"))) continue;
                String delivery = item.optString("delivery", "bundled");
                value.append("\n").append(item.getString("title")).append(" · ")
                        .append(item.optString("version", "incluido"))
                        .append(" · ").append("bundled".equals(delivery)
                                ? "incluido en la APK" : "actualizable");
            }
            JSONArray optional = manifest.optJSONArray("optionalUpdates");
            if (optional != null && optional.length() > 0) {
                value.append("\n\nOpcional:");
                for (int i = 0; i < optional.length(); i++) {
                    JSONObject item = optional.getJSONObject(i);
                    value.append("\n• ").append(item.optString("title", item.optString("id")))
                            .append(" · se descarga solo cuando lo solicites");
                }
            }
            status.setText(value.toString());
        } catch (Exception error) {
            status.setText("No se pudo leer el manifiesto local de paquetes.");
        }
    }

    private void verifyAll() {
        status.setText("Verificando el contenido incluido en esta APK…");
        new Thread(() -> {
            try {
                JSONObject manifest = new JSONObject(readAsset("package-manifest.json"));
                JSONArray packages = manifest.getJSONArray("packages");
                int verified = 0;
                for (int i = 0; i < packages.length(); i++) {
                    JSONObject item = packages.getJSONObject(i);
                    if (!"bundled".equals(item.optString("delivery", "bundled"))) continue;
                    String asset = item.getString("asset");
                    byte[] bytes = readAssetBytes(asset);
                    String mode = item.optString("verification", "sha256");
                    if ("generated-build".equals(mode)) {
                        if (bytes.length < 20) {
                            throw new IllegalStateException("El contenido incluido «"
                                    + item.getString("title") + "» está incompleto.");
                        }
                    } else {
                        String expected = item.optString("sha256", "");
                        if (expected.isEmpty() || !expected.equalsIgnoreCase(sha256(bytes))) {
                            throw new IllegalStateException("Falló la verificación local de "
                                    + item.getString("title") + ".");
                        }
                    }
                    verified++;
                }
                final int count = verified;
                final int year = Calendar.getInstance().get(Calendar.YEAR);
                final boolean calendarAvailable = LiturgicalCalendarRepository.hasCalendar(this, year);
                runOnUiThread(() -> status.setText(count
                        + " componentes incluidos verificados correctamente.\n"
                        + (calendarAvailable
                        ? "Calendario " + year + " instalado y disponible. "
                        : "No se encontró el calendario local de " + year + ". ")
                        + "Un fallo de Internet al buscar una revisión futura no significa que el calendario o los libros instalados hayan dejado de funcionar."));
            } catch (Exception error) {
                runOnUiThread(() -> status.setText(error.getMessage() == null
                        ? "No se pudo completar la verificación local." : error.getMessage()));
            }
        }).start();
    }

    private void showChangelog() {
        try {
            new android.app.AlertDialog.Builder(this).setTitle("Ministerium 4.1.0")
                    .setMessage(readAsset("changelog-4.1.0.txt"))
                    .setPositiveButton("Cerrar", null).show();
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir el historial de cambios.", Toast.LENGTH_SHORT).show();
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
