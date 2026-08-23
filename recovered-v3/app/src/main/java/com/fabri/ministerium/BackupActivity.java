package com.fabri.ministerium;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupActivity extends ThemedActivity {
    private static final int CREATE_BACKUP = 81;
    private static final int RESTORE_BACKUP = 82;
    private TextView status;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);
        status = findViewById(R.id.txtBackupStatus);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCreateBackup).setOnClickListener(v -> createBackup());
        findViewById(R.id.btnRestoreBackup).setOnClickListener(v -> chooseRestore());
        findViewById(R.id.btnDriveBackup).setOnClickListener(v -> createBackup());
        refreshStatus();
    }

    private void createBackup() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "ministerium-backup-current.json");
        startActivityForResult(intent, CREATE_BACKUP);
    }

    private void chooseRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, RESTORE_BACKUP);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (requestCode == CREATE_BACKUP) {
                byte[] backup = BackupManager.create(this);
                try (OutputStream output = getContentResolver().openOutputStream(uri, "w")) {
                    if (output == null) throw new IllegalStateException();
                    output.write(backup);
                    output.flush();
                }
                long now = System.currentTimeMillis();
                getSharedPreferences("backup_status_v3", MODE_PRIVATE).edit()
                        .putLong("last_backup", now).apply();
                refreshStatus();
                Toast.makeText(this, "Copia creada y verificada.", Toast.LENGTH_LONG).show();
            } else if (requestCode == RESTORE_BACKUP) {
                byte[] backup;
                try (InputStream input = getContentResolver().openInputStream(uri);
                     ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    if (input == null) throw new IllegalStateException();
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                    backup = output.toByteArray();
                }
                BackupManager.restore(this, backup);
                Toast.makeText(this, "Copia restaurada. Tus datos ya están disponibles.",
                        Toast.LENGTH_LONG).show();
                recreate();
            }
        } catch (Exception error) {
            Toast.makeText(this, error.getMessage() == null
                    ? "No se pudo completar la operación." : error.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void refreshStatus() {
        long value = getSharedPreferences("backup_status_v3", MODE_PRIVATE)
                .getLong("last_backup", 0);
        status.setText(value <= 0 ? "Aún no se ha creado una copia."
                : "Última copia: " + new SimpleDateFormat(
                "d MMM yyyy, HH:mm", new Locale("es", "EC")).format(new Date(value)));
    }
}
