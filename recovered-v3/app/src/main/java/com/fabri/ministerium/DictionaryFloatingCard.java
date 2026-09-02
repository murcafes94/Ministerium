package com.fabri.ministerium;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** Compact dictionary surface anchored close to the selected word. */
public final class DictionaryFloatingCard {
    private static PopupWindow active;

    private DictionaryFloatingCard() {}

    public static void show(Activity activity, WebView reader, String selection,
                            float rectTop, float rectBottom, float viewportHeight) {
        dismiss();
        final String query = normalize(selection);
        if (query.isEmpty()) return;

        Card card = createCard(activity, query);
        PopupWindow popup = new PopupWindow(card.root, card.width,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setElevation(dp(activity, 10));
        popup.setOnDismissListener(() -> { if (active == popup) active = null; });
        active = popup;

        int y = anchorY(activity, reader, rectTop, rectBottom, viewportHeight);
        popup.showAtLocation(reader.getRootView(), Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, y);

        card.close.setOnClickListener(v -> popup.dismiss());
        card.copy.setOnClickListener(v -> copy(activity, query));
        card.definition.setText("Buscando en los diccionarios locales…");
        card.definition.setMaxLines(7);
        card.definition.setEllipsize(TextUtils.TruncateAt.END);

        if (query.contains(" ")) {
            showCatalogChoices(activity, card, query);
            return;
        }

        new Thread(() -> {
            try {
                List<BibleDictionaryRepository.QuickResult> results =
                        BibleDictionaryRepository.quickLookup(activity.getApplicationContext(), query);
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || active != popup) return;
                    if (results.isEmpty()) showCatalogChoices(activity, card, query);
                    else showResults(activity, card, query, results);
                });
            } catch (Exception error) {
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || active != popup) return;
                    showCatalogChoices(activity, card, query);
                });
            }
        }, "ministerium-dictionary-card").start();
    }

    public static void dismiss() {
        PopupWindow value = active;
        active = null;
        if (value != null && value.isShowing()) value.dismiss();
    }

    private static void showResults(Activity activity, Card card, String query,
                                    List<BibleDictionaryRepository.QuickResult> results) {
        card.tabs.removeAllViews();
        card.tabsScroller.setVisibility(results.size() > 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < results.size(); i++) {
            BibleDictionaryRepository.QuickResult result = results.get(i);
            Button tab = smallButton(activity, shortSource(result.source));
            card.tabs.addView(tab);
            tab.setOnClickListener(v -> bindResult(activity, card, query, result));
        }
        bindResult(activity, card, query, results.get(0));
    }

    private static void bindResult(Activity activity, Card card, String query,
                                   BibleDictionaryRepository.QuickResult result) {
        card.source.setText(result.source.title + " · sin conexión");
        card.definition.setText(fromHtml(result.html));
        card.definition.setMaxLines(7);
        card.definition.setEllipsize(TextUtils.TruncateAt.END);
        card.more.setText("Ver más");
        card.more.setVisibility(View.VISIBLE);
        card.more.setOnClickListener(v -> activity.startActivity(
                new Intent(activity, BibleDictionaryActivity.class)
                        .putExtra(BibleDictionaryActivity.EXTRA_SOURCE_ID, result.source.id)
                        .putExtra(BibleDictionaryActivity.EXTRA_QUERY, query)));
    }

    private static void showCatalogChoices(Activity activity, Card card, String query) {
        card.source.setText("Diccionarios locales");
        card.definition.setText(query.contains(" ")
                ? "La selección contiene varias palabras. Elige un diccionario para buscar la expresión completa."
                : "No hubo una coincidencia exacta. Puedes buscar la palabra en el catálogo completo.");
        card.definition.setMaxLines(5);
        card.definition.setEllipsize(TextUtils.TruncateAt.END);
        card.tabsScroller.setVisibility(View.VISIBLE);
        card.tabs.removeAllViews();
        for (BibleDictionaryRepository.Source source : BibleDictionaryRepository.sources()) {
            if (!("biblical_pdf".equals(source.id) || "biblical_san_pablo".equals(source.id)
                    || "rae_15".equals(source.id))) continue;
            Button tab = smallButton(activity, shortSource(source));
            card.tabs.addView(tab);
            tab.setOnClickListener(v -> activity.startActivity(
                    new Intent(activity, BibleDictionaryActivity.class)
                            .putExtra(BibleDictionaryActivity.EXTRA_SOURCE_ID, source.id)
                            .putExtra(BibleDictionaryActivity.EXTRA_QUERY, query)));
        }
        card.more.setVisibility(View.GONE);
    }

    private static Card createCard(Activity activity, String query) {
        boolean dark = ThemeUtils.isDark(activity);
        int panel = Color.parseColor(dark ? "#302925" : "#FFFDF7");
        int ink = Color.parseColor(dark ? "#F3EDE4" : "#2A2521");
        int muted = Color.parseColor(dark ? "#C9BEB5" : "#75685E");
        int accent = Color.parseColor(dark ? "#E1C57A" : "#6E1D2A");
        int border = Color.parseColor(dark ? "#65564D" : "#D8C9B5");

        int width = Math.min(dp(activity, 340),
                activity.getResources().getDisplayMetrics().widthPixels - dp(activity, 28));
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 16), dp(activity, 13), dp(activity, 16), dp(activity, 12));
        GradientDrawable background = new GradientDrawable();
        background.setColor(panel);
        background.setCornerRadius(dp(activity, 14));
        background.setStroke(dp(activity, 1), border);
        root.setBackground(background);

        LinearLayout heading = new LinearLayout(activity);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView word = new TextView(activity);
        word.setText(query);
        word.setTextColor(accent);
        word.setTextSize(18);
        word.setTypeface(word.getTypeface(), android.graphics.Typeface.BOLD);
        heading.addView(word, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button close = smallButton(activity, "×");
        close.setContentDescription("Cerrar diccionario");
        heading.addView(close, new LinearLayout.LayoutParams(dp(activity, 42), dp(activity, 38)));
        root.addView(heading);

        TextView source = new TextView(activity);
        source.setTextColor(muted);
        source.setTextSize(12);
        source.setPadding(0, dp(activity, 2), 0, dp(activity, 7));
        root.addView(source);

        HorizontalScrollView tabsScroller = new HorizontalScrollView(activity);
        tabsScroller.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(activity);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabsScroller.addView(tabs, new HorizontalScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(tabsScroller);

        ScrollView scroll = new ScrollView(activity);
        TextView definition = new TextView(activity);
        definition.setTextColor(ink);
        definition.setTextSize(15);
        definition.setLineSpacing(0, 1.14f);
        definition.setPadding(0, dp(activity, 7), 0, dp(activity, 5));
        scroll.addView(definition, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        Button copy = smallButton(activity, "Copiar");
        Button more = smallButton(activity, "Ver más");
        actions.addView(copy);
        actions.addView(more);
        root.addView(actions);

        return new Card(root, source, definition, tabsScroller, tabs, close, copy, more, width);
    }

    private static Button smallButton(Activity activity, String text) {
        boolean dark = ThemeUtils.isDark(activity);
        Button button = new Button(activity);
        button.setText(text);
        button.setTextSize(12);
        button.setAllCaps(false);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setPadding(dp(activity, 10), dp(activity, 5), dp(activity, 10), dp(activity, 5));
        button.setTextColor(Color.parseColor(dark ? "#E1C57A" : "#6E1D2A"));
        button.setBackgroundColor(Color.TRANSPARENT);
        return button;
    }

    private static int anchorY(Activity activity, WebView reader, float top, float bottom,
                               float viewportHeight) {
        int[] location = new int[2];
        reader.getLocationOnScreen(location);
        int screenHeight = activity.getResources().getDisplayMetrics().heightPixels;
        float ratio = viewportHeight > 1 ? reader.getHeight() / viewportHeight : 1f;
        int below = location[1] + Math.round(bottom * ratio) + dp(activity, 8);
        int estimatedHeight = dp(activity, 260);
        if (below + estimatedHeight <= screenHeight - dp(activity, 16)) return Math.max(dp(activity, 12), below);
        int above = location[1] + Math.round(top * ratio) - estimatedHeight - dp(activity, 8);
        return Math.max(dp(activity, 12), above);
    }

    private static String shortSource(BibleDictionaryRepository.Source source) {
        if ("biblical_pdf".equals(source.id)) return "Bíblico I";
        if ("biblical_san_pablo".equals(source.id)) return "Bíblico II";
        if ("rae_15".equals(source.id)) return "RAE";
        return source.title;
    }

    private static Spanned fromHtml(String html) {
        if (Build.VERSION.SDK_INT >= 24) return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        return Html.fromHtml(html);
    }

    private static void copy(Activity activity, String text) {
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) clipboard.setPrimaryClip(ClipData.newPlainText("Diccionario", text));
        Toast.makeText(activity, "Palabra copiada.", Toast.LENGTH_SHORT).show();
    }

    private static String normalize(String value) {
        String query = value == null ? "" : value.replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ").trim();
        return query.length() <= 80 ? query : query.substring(0, 80).trim();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Card {
        final LinearLayout root;
        final TextView source;
        final TextView definition;
        final HorizontalScrollView tabsScroller;
        final LinearLayout tabs;
        final Button close;
        final Button copy;
        final Button more;
        final int width;

        Card(LinearLayout root, TextView source, TextView definition,
             HorizontalScrollView tabsScroller, LinearLayout tabs,
             Button close, Button copy, Button more, int width) {
            this.root = root;
            this.source = source;
            this.definition = definition;
            this.tabsScroller = tabsScroller;
            this.tabs = tabs;
            this.close = close;
            this.copy = copy;
            this.more = more;
            this.width = width;
        }
    }
}
