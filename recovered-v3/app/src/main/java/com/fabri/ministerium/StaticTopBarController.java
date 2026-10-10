package com.fabri.ministerium;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

/** Keeps native top bars outside the scrolling viewport. Legacy WebView and
 * TextView readers continue to use ReaderChrome and TextViewReaderChrome. */
public final class StaticTopBarController {
    public static final String HEADER_TAG = "fixed-top-bar";

    private StaticTopBarController() {}

    public static void attach(Activity activity) {
        if (activity == null) return;
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) return;
        ViewGroup host = (ViewGroup) content;
        if (host.getChildCount() == 0 || !(host.getChildAt(0) instanceof ScrollView)) return;
        ScrollView scroll = (ScrollView) host.getChildAt(0);
        if (scroll.getChildCount() == 0 || !(scroll.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout body = (LinearLayout) scroll.getChildAt(0);
        if (body.getOrientation() != LinearLayout.VERTICAL || body.getChildCount() < 2) return;
        View header = body.getChildAt(0);
        // Explicit native headers are moved before the first layout and before
        // reader scroll restoration. Large accessibility text has no height cap.
        if (HEADER_TAG.equals(header.getTag())) {
            separate(activity, host, scroll, body, header);
        } else {
            // Retain the conservative detection for inherited XML screens.
            scroll.post(() -> {
                if (scroll.getParent() != host || body.getChildAt(0) != header) return;
                if (header.getHeight() > 0 && header.getHeight() <= dp(activity, 180)
                        && isInteractiveHeader(header)) {
                    separate(activity, host, scroll, body, header);
                }
            });
        }
    }

    private static void separate(Activity activity, ViewGroup host, ScrollView scroll,
                                 LinearLayout body, View header) {
        int index = host.indexOfChild(scroll);
        if (index < 0) return;
        ViewGroup.LayoutParams pageParams = scroll.getLayoutParams();
        ViewGroup.LayoutParams headerParams = header.getLayoutParams();
        int columnWidth = body.getLayoutParams().width;

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        copyBackground(activity, scroll, page);
        // Reader preferences must also repaint the area behind the fixed bar.
        if ("reader-page".equals(scroll.getTag())) NativeUi.readerPage(page);

        FrameLayout bar = new FrameLayout(activity);
        LinearLayout column = new LinearLayout(activity);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(body.getPaddingLeft(), body.getPaddingTop(), body.getPaddingRight(), 0);
        body.setPadding(body.getPaddingLeft(), 0, body.getPaddingRight(), body.getPaddingBottom());
        body.removeView(header);
        // Keep child indexes stable for dynamic content (e.g. Compline hymns).
        View slot = new View(activity);
        slot.setVisibility(View.GONE);
        body.addView(slot, 0, new LinearLayout.LayoutParams(0, 0));
        header.animate().cancel();
        header.setTranslationY(0);
        header.setTranslationZ(0);
        header.setElevation(0);
        column.addView(header, headerParams);
        bar.addView(column, new FrameLayout.LayoutParams(columnWidth,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL));

        host.removeView(scroll);
        page.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.setClipToPadding(true);
        scroll.setClipChildren(true);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        host.addView(page, index, pageParams);
    }

    private static void copyBackground(Activity activity, View source, View target) {
        Drawable background = source.getBackground();
        if (background == null) return;
        Drawable.ConstantState state = background.getConstantState();
        target.setBackground(state != null
                ? state.newDrawable(activity.getResources()).mutate() : background.mutate());
    }

    private static boolean isInteractiveHeader(View view) {
        if (view instanceof Button || view.isClickable()) return true;
        if (!(view instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) view;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (isInteractiveHeader(group.getChildAt(i))) return true;
        }
        return false;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
