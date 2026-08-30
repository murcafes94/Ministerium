package com.fabri.ministerium;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PdfReaderActivity extends ThemedActivity {
    public static final String EXTRA_DOCUMENT = "document_id";
    public static final String EXTRA_PAGE = "page_index";

    private DocumentInfo document;
    private int pageIndex;
    private ParcelFileDescriptor descriptor;
    private PdfRenderer renderer;
    private PdfRenderer.Page openPage;
    private Bitmap bitmap;
    private ZoomImageView imageView;
    private TextView pageLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);

        document = ContentRepository.document(getIntent().getStringExtra(EXTRA_DOCUMENT));
        if (document == null) {
            finish();
            return;
        }
        pageIndex = Math.max(0, Math.min(document.pageCount - 1,
                getIntent().getIntExtra(EXTRA_PAGE, 0)));

        imageView = findViewById(R.id.pdfImage);
        pageLabel = findViewById(R.id.txtPdfPage);
        ((TextView) findViewById(R.id.txtPdfTitle)).setText(document.title);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnPrevious).setOnClickListener(v -> move(-1));
        findViewById(R.id.btnNext).setOnClickListener(v -> move(1));
        findViewById(R.id.btnResetZoom).setOnClickListener(v -> imageView.resetZoom());
        findViewById(R.id.btnText).setOnClickListener(v -> openText());

        try {
            File localPdf = copyAssetToCache();
            descriptor = ParcelFileDescriptor.open(localPdf, ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(descriptor);
            pageIndex = Math.min(pageIndex, renderer.getPageCount() - 1);
            imageView.post(this::renderPage);
        } catch (IOException error) {
            Toast.makeText(this, "No se pudo abrir el libro original.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private File copyAssetToCache() throws IOException {
        File directory = new File(getCacheDir(), "pdfs");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("No se pudo crear el directorio temporal.");
        }
        File target = new File(directory, document.id + ".pdf");
        if (target.exists() && target.length() > 0) return target;

        try (InputStream input = getAssets().open(document.pdfAsset);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        }
        return target;
    }

    private void renderPage() {
        if (renderer == null || imageView.getWidth() == 0) return;
        closePage();
        openPage = renderer.openPage(pageIndex);

        int targetWidth = Math.max(1200, imageView.getWidth() * 2);
        int targetHeight = Math.max(1,
                Math.round(targetWidth * (openPage.getHeight() / (float) openPage.getWidth())));
        bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Matrix matrix = new Matrix();
        matrix.postScale(targetWidth / (float) openPage.getWidth(),
                targetHeight / (float) openPage.getHeight());
        openPage.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
        imageView.setImageBitmap(bitmap);
        pageLabel.setText("Página PDF " + (pageIndex + 1) + " de " + renderer.getPageCount());
        findViewById(R.id.btnPrevious).setEnabled(pageIndex > 0);
        findViewById(R.id.btnNext).setEnabled(pageIndex < renderer.getPageCount() - 1);
    }

    private void move(int delta) {
        if (renderer == null) return;
        int next = Math.max(0, Math.min(renderer.getPageCount() - 1, pageIndex + delta));
        if (next != pageIndex) {
            pageIndex = next;
            renderPage();
        }
    }

    private void openText() {
        Intent intent = new Intent(this, TextReaderActivity.class);
        intent.putExtra(TextReaderActivity.EXTRA_DOCUMENT, document.id);
        intent.putExtra(TextReaderActivity.EXTRA_PAGE, pageIndex);
        startActivity(intent);
    }

    private void closePage() {
        if (openPage != null) {
            openPage.close();
            openPage = null;
        }
        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }
    }

    @Override
    protected void onDestroy() {
        closePage();
        if (renderer != null) renderer.close();
        if (descriptor != null) {
            try {
                descriptor.close();
            } catch (IOException ignored) {
            }
        }
        super.onDestroy();
    }
}
