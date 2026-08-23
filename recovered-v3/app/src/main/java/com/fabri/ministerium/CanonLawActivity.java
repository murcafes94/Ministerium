package com.fabri.ministerium;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

public class CanonLawActivity extends ThemedActivity {
    public static final String EXTRA_CANON = "canon_number";
    private static final String OFFICIAL =
            "https://www.vatican.va/archive/cod-iuris-canonici/cic_index_sp.html";

    private WebView webView;
    private EditText input;
    private TextView status;
    private Button comments;
    private int currentCanon = 1;
    private String currentSource = OFFICIAL;

    @Override protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.apply(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_canon_law);
        webView = findViewById(R.id.canonWebView);
        input = findViewById(R.id.inputCanon);
        status = findViewById(R.id.txtCanonStatus);
        comments = findViewById(R.id.btnCanonComments);
        currentCanon = Math.max(1, Math.min(1752,
                getIntent().getIntExtra(EXTRA_CANON, 1)));

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view,
                                                               WebResourceRequest request) {
                return handleUrl(request.getUrl().toString());
            }
            @Override public void onPageFinished(WebView view, String url) {
                applyReaderStyle();
                ReaderPreferences.apply(CanonLawActivity.this, webView, false);
                UniversalSelectionMenu.restoreHighlights(CanonLawActivity.this,
                        webView, sourceKey());
                updateHeader();
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> back());
        findViewById(R.id.btnFindCanon).setOnClickListener(v -> findCanon());
        findViewById(R.id.btnCanonSearch).setOnClickListener(v -> {
            View panel = findViewById(R.id.canonSearchPanel);
            panel.setVisibility(panel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            if (panel.getVisibility() == View.VISIBLE) input.requestFocus();
        });
        comments.setOnClickListener(v -> showComment(currentCanon));
        status.setOnClickListener(v -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(currentSource))));
        ReaderChrome.bindTheme(this, findViewById(R.id.btnReaderTheme));
        ReaderChrome.bindGlobalMenu(this, findViewById(R.id.btnGlobalMenu));
        ReaderChrome.attach(this, webView, findViewById(R.id.readerHeader), context(),
                new ReaderChrome.Navigator() {
                    @Override public boolean canPrevious() { return currentCanon > 1; }
                    @Override public boolean canNext() { return currentCanon < 1752; }
                    @Override public void previous() { move(-1); }
                    @Override public void next() { move(1); }
                }, false);
        showCanon(currentCanon);
    }

    private void findCanon() {
        int canon;
        try {
            canon = Integer.parseInt(input.getText().toString().trim());
        } catch (NumberFormatException error) {
            canon = 0;
        }
        if (canon < 1 || canon > 1752) {
            Toast.makeText(this, "Escribe un canon entre 1 y 1752.",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(input.getWindowToken(), 0);
        showCanon(canon);
    }

    private void move(int amount) {
        int canon = currentCanon + amount;
        if (canon < 1 || canon > 1752) return;
        showCanon(canon);
    }

    private void showCanon(int canon) {
        currentCanon = canon;
        input.setText(String.valueOf(canon));
        int chunk = (canon - 1) / 100 + 1;
        String file = String.format(java.util.Locale.US,
                "file:///android_asset/canon-text/canons-%02d.html#canon-%d", chunk, canon);
        webView.loadUrl(file);
        UniversalSelectionMenu.attach(this, webView, context());
    }

    private boolean handleUrl(String url) {
        if (url == null || !url.startsWith("ministerium://canon-comment/")) return false;
        try {
            showComment(Integer.parseInt(url.substring(url.lastIndexOf('/') + 1)));
        } catch (NumberFormatException ignored) {}
        return true;
    }

    private void updateHeader() {
        CanonTextRepository.Entry amendment = CanonTextRepository.find(this, currentCanon);
        currentSource = amendment == null || amendment.source.isEmpty()
                ? OFFICIAL : amendment.source;
        status.setText(amendment == null
                ? "Canon " + currentCanon
                    + " · español y latín del archivo del Vaticano · toca para abrir la fuente"
                : "Canon " + currentCanon + " · texto vigente consolidado desde "
                    + amendment.date + " · toca para abrir la reforma oficial");
        try {
            CanonCommentaryRepository.Entry entry =
                    CanonCommentaryRepository.find(this, currentCanon);
            comments.setEnabled(entry != null);
            comments.setVisibility(entry == null ? View.GONE : View.VISIBLE);
        } catch (Exception error) {
            comments.setEnabled(false);
            comments.setVisibility(View.GONE);
        }
    }

    private void showComment(int canon) {
        try {
            CanonCommentaryRepository.Entry entry =
                    CanonCommentaryRepository.find(this, canon);
            if (entry == null) {
                Toast.makeText(this, "Esta edición no incluye un comentario específico para el canon "
                        + canon + ".", Toast.LENGTH_LONG).show();
                return;
            }
            String title = entry.commentedCanons.equals(String.valueOf(canon))
                    ? "Comentario al canon " + canon
                    : "Comentario a los cánones " + entry.commentedCanons;
            String article = CanonCommentaryRepository.article(this, entry);
            CanonTextRepository.Entry amendment = CanonTextRepository.find(this, canon);
            if (amendment != null) {
                article = "<aside style=\"padding:12px 14px;margin:0 0 16px;"
                        + "border-left:4px solid #9a742b;background:#f1e4c9;"
                        + "font-family:sans-serif;line-height:1.45\"><strong>Advertencia histórica</strong><br>"
                        + "Este comentario pertenece a la edición de estudio de 2001. El canon fue "
                        + "modificado posteriormente por " + amendment.reform + " y el texto vigente "
                        + "es el mostrado en la pantalla principal.</aside>" + article;
            }
            ReaderOverlayDialog.show(this, title, article);
        } catch (Exception error) {
            Toast.makeText(this, "No se pudo abrir el comentario.", Toast.LENGTH_LONG).show();
        }
    }

    private void applyReaderStyle() {
        boolean dark = ThemeUtils.isDark(this);
        String background = dark ? "#26211E" : "#FFF5DF";
        String ink = dark ? "#F3EDE4" : "#4F4132";
        String accent = dark ? "#E1C57A" : "#6E1D2A";
        String panel = dark ? "#302925" : "rgba(255,255,255,.32)";
        String border = dark ? "#65564D" : "#D8C9B5";
        String style = "html,body{background:" + background + "!important;color:" + ink
                + "!important}body{width:100%;max-width:none;margin:0;padding:24px!important;box-sizing:border-box}"
                + "body *{color:" + ink + "!important;-webkit-text-fill-color:" + ink
                + "!important}.comment-link{background:" + accent
                + "!important;color:#fff!important;-webkit-text-fill-color:#fff!important}"
                + "h1,h2{color:" + accent + "!important;-webkit-text-fill-color:" + accent
                + "!important}.canon-language{background:" + panel + "!important;border-color:"
                + border + "!important}h2{border-color:" + border + "!important}"
                + "@media(min-width:700px){body{padding-left:48px!important;"
                + "padding-right:48px!important}}"
                + "@media(min-width:1100px){body{padding-left:64px!important;"
                + "padding-right:64px!important}}";
        String script = "(function(){var all=document.querySelectorAll('.canon');"
                + "for(var i=0;i<all.length;i++)all[i].style.display='none';"
                + "var current=document.getElementById('canon-" + currentCanon + "');"
                + "if(current)current.style.display='block';"
                + "var s=document.createElement('style');s.innerHTML="
                + org.json.JSONObject.quote(style)
                + ";document.head.appendChild(s);window.scrollTo(0,0);})()";
        webView.evaluateJavascript(script, null);
    }

    private void back() {
        finish();
    }

    @Override public void onBackPressed() { back(); }

    private String sourceKey() { return "canon:" + currentCanon; }

    private ReaderContext context() {
        return new ReaderContext("Código de Derecho Canónico", sourceKey(),
                "Canon " + currentCanon, "CIC " + currentCanon,
                "Documentos/libros", false);
    }

    @Override protected void onPause() {
        try {
            ContinueReadingStore.save(this, "Código de Derecho Canónico",
                    "Canon " + currentCanon, CanonLawActivity.class,
                    new JSONObject().put(EXTRA_CANON, currentCanon), webView.getScrollY());
        } catch (Exception ignored) {}
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
