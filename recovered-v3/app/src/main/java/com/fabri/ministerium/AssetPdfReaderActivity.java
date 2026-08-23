package com.fabri.ministerium;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AssetPdfReaderActivity extends ThemedActivity {
    public static final String EXTRA_TITLE = "asset_pdf_title";
    public static final String EXTRA_ASSET = "asset_pdf_path";
    private int pageIndex;
    private ParcelFileDescriptor descriptor;
    private PdfRenderer renderer;
    private PdfRenderer.Page openPage;
    private Bitmap bitmap;
    private ZoomImageView imageView;
    private TextView pageLabel;
    private EditText pageInput;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_pdf_reader);
        String title = value(getIntent().getStringExtra(EXTRA_TITLE));
        String asset = value(getIntent().getStringExtra(EXTRA_ASSET));
        if (asset.isEmpty()) { finish(); return; }
        ((TextView) findViewById(R.id.txtPdfTitle)).setText(title);
        imageView = findViewById(R.id.pdfImage);
        pageLabel = findViewById(R.id.txtPdfPage);
        pageInput = findViewById(R.id.inputPdfPage);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevious).setOnClickListener(v -> move(-1));
        findViewById(R.id.btnNext).setOnClickListener(v -> move(1));
        findViewById(R.id.btnResetZoom).setOnClickListener(v -> imageView.resetZoom());
        findViewById(R.id.btnGoPage).setOnClickListener(v -> goToPage());
        try {
            File file = copyAsset(asset);
            descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            imageView.post(this::renderPage);
        } catch (IOException error) {
            Toast.makeText(this, "No se pudo abrir el PDF.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private File copyAsset(String asset) throws IOException {
        File dir = new File(getCacheDir(), "reference-pdfs");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Directorio no disponible");
        File target = new File(dir, new File(asset).getName());
        if (target.isFile() && target.length() > 0) return target;
        try (InputStream input = getAssets().open(asset);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[32768]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
        return target;
    }

    private void goToPage() {
        int page;
        try { page = Integer.parseInt(pageInput.getText().toString().trim()); }
        catch (NumberFormatException error) { page = 0; }
        if (renderer == null || page < 1 || page > renderer.getPageCount()) {
            Toast.makeText(this, "Escribe una página válida.", Toast.LENGTH_SHORT).show();
            return;
        }
        pageIndex = page - 1; renderPage();
    }

    private void renderPage() {
        if (renderer == null || imageView.getWidth() == 0) return;
        closePage();
        openPage = renderer.openPage(pageIndex);
        int width = Math.max(1200, imageView.getWidth() * 2);
        int height = Math.max(1, Math.round(width * openPage.getHeight()
                / (float) openPage.getWidth()));
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();
        matrix.postScale(width / (float) openPage.getWidth(),
                height / (float) openPage.getHeight());
        openPage.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        imageView.setImageBitmap(bitmap);
        pageLabel.setText("Página " + (pageIndex + 1) + " de " + renderer.getPageCount()
                + " · edición de estudio 2001");
        findViewById(R.id.btnPrevious).setEnabled(pageIndex > 0);
        findViewById(R.id.btnNext).setEnabled(pageIndex < renderer.getPageCount() - 1);
    }

    private void move(int amount) {
        if (renderer == null) return;
        int next = Math.max(0, Math.min(renderer.getPageCount() - 1, pageIndex + amount));
        if (next != pageIndex) { pageIndex = next; renderPage(); }
    }

    private void closePage() {
        if (openPage != null) { openPage.close(); openPage = null; }
        if (bitmap != null) { bitmap.recycle(); bitmap = null; }
    }

    @Override protected void onDestroy() {
        closePage();
        if (renderer != null) renderer.close();
        if (descriptor != null) try { descriptor.close(); } catch (IOException ignored) {}
        super.onDestroy();
    }

    private static String value(String value) { return value == null ? "" : value; }
}
