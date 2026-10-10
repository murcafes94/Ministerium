package com.fabri.ministerium;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Shared spacing and reading styles for the native V5 screens. */
public final class NativeUi {
    private NativeUi() {}

    public static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    public static void addCenteredRoot(ScrollView scroll, LinearLayout root, int maximumDp) {
        Context context = scroll.getContext();
        int width = Math.min(context.getResources().getDisplayMetrics().widthPixels, dp(context, maximumDp));
        scroll.addView(root, new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));
    }

    public static LinearLayout header(Activity activity, String title, boolean reader) {
        LinearLayout row = new LinearLayout(activity);
        row.setTag(StaticTopBarController.HEADER_TAG);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity, 4), 0, dp(activity, 12));
        ImageView back = new ImageView(activity);
        back.setImageResource(R.drawable.ic_nav_back);
        back.setColorFilter(activity.getResources().getColor(R.color.wine));
        back.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        back.setContentDescription("Volver");
        back.setBackgroundResource(R.drawable.bg_button_secondary);
        back.setFocusable(true);
        back.setOnClickListener(v -> activity.finish());
        row.addView(back, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        TextView label = new TextView(activity);
        label.setText(title);
        label.setTextSize(21);
        label.setTextColor(activity.getResources().getColor(R.color.ink));
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setPadding(dp(activity, 12), 0, dp(activity, 8), 0);
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
        if (reader) {
            TextView settings = new TextView(activity);
            settings.setText("Aa");
            settings.setTextSize(17);
            settings.setTextColor(activity.getResources().getColor(R.color.wine));
            settings.setGravity(Gravity.CENTER);
            settings.setBackgroundResource(R.drawable.bg_button_secondary);
            settings.setContentDescription("Preferencias de lectura");
            settings.setFocusable(true);
            settings.setOnClickListener(v -> activity.startActivity(new Intent(activity, ReaderSettingsActivity.class)));
            row.addView(settings, new LinearLayout.LayoutParams(dp(activity, 48), dp(activity, 48)));
        }
        return row;
    }

    public static void dateControl(TextView view, String description) {
        view.setContentDescription(description);
        view.setBackgroundResource(R.drawable.bg_button_secondary);
        view.setPadding(dp(view.getContext(), 6), dp(view.getContext(), 6), dp(view.getContext(), 6), dp(view.getContext(), 6));
        view.setMinHeight(dp(view.getContext(), 52));
        view.setFocusable(true);
    }

    public static void readerBody(Context context, TextView view) {
        view.setTag("reader-body");
        view.setTextSize(17f * ReaderPreferences.textZoom(context) / 110f);
        String family = ReaderPreferences.family(context);
        if (ReaderPreferences.PALATINO.equals(family)) family = ReaderPreferences.SERIF;
        view.setTypeface(Typeface.create(family, ReaderPreferences.weight(context) >= 600 ? Typeface.BOLD : Typeface.NORMAL));
        view.setLineSpacing(0, ReaderPreferences.lineHeight(context));
        view.setTextColor(Color.parseColor(ReaderVisualPalette.from(context).ink));
        view.setTextIsSelectable(true);
    }

    public static void readerPage(View view) {
        view.setTag("reader-page");
        view.setBackgroundColor(Color.parseColor(ReaderVisualPalette.from(view.getContext()).background));
    }

    public static void refreshReader(View view) {
        if ("reader-page".equals(view.getTag())) readerPage(view);
        if (view instanceof TextView && "reader-body".equals(view.getTag())) readerBody(view.getContext(), (TextView) view);
        if (view instanceof LinearLayout && "reader-panel".equals(view.getTag())) readerPanel((LinearLayout) view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) refreshReader(group.getChildAt(i));
        }
    }

    public static void readerPanel(LinearLayout panel) {
        panel.setTag("reader-panel");
        Context context = panel.getContext();
        // Paragraph containers have no visual frame; the whole reader is one page.
        panel.setBackgroundColor(Color.TRANSPARENT);
        int horizontal = dp(context, Math.min(40, ReaderPreferences.horizontalPaddingPx(context)));
        panel.setPadding(horizontal, dp(context, 6), horizontal, dp(context, 8));
    }
}
