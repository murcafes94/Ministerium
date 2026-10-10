package com.fabri.ministerium;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, qualifiers = "w360dp-h640dp-mdpi")
public class StaticTopBarControllerTest {
    private Activity activity;
    private ScrollView scroll;
    private LinearLayout body;
    private TextView header;
    private TextView paragraph;

    private void screen(int columnWidth, int headerHeight, boolean tagged) {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.BLACK);
        body = new LinearLayout(activity);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(22, 18, 22, 34);
        scroll.addView(body, new FrameLayout.LayoutParams(columnWidth, -2, Gravity.CENTER_HORIZONTAL));
        header = new TextView(activity);
        header.setText("Invitatorio");
        header.setOnClickListener(v -> {});
        if (tagged) header.setTag(StaticTopBarController.HEADER_TAG);
        body.addView(header, new LinearLayout.LayoutParams(-1, headerHeight));
        paragraph = new TextView(activity);
        paragraph.setText("Señor, abre mis labios");
        body.addView(paragraph, new LinearLayout.LayoutParams(-1, 2400));
        activity.setContentView(scroll);
    }

    private View page() {
        return ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
    }

    private void layout(int width, int height) {
        View page = page();
        page.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        page.layout(0, 0, width, height);
    }

    @Test public void scrollingCannotMoveHeaderOrExposeTextAboveIt() {
        screen(360, 64, true);
        StaticTopBarController.attach(activity);
        layout(360, 640);
        int[] before = new int[2];
        header.getLocationInWindow(before);
        assertEquals(82, scroll.getTop());
        assertEquals(558, scroll.getHeight());
        scroll.scrollTo(0, 500);
        assertEquals(500, scroll.getScrollY());
        int[] after = new int[2];
        header.getLocationInWindow(after);
        assertArrayEquals(before, after);
        assertEquals(0f, header.getTranslationY(), 0f);
        assertTrue(scroll.getClipChildren());
        assertTrue(scroll.getClipToPadding());
        assertFalse(header.getParent() == body);
        assertEquals(0, body.getPaddingTop());
        assertEquals(34, body.getPaddingBottom());
    }

    @Test public void tabletHeaderAndTextKeepSameCenteredMargins() {
        screen(820, 64, true);
        StaticTopBarController.attach(activity);
        layout(1200, 800);
        int[] barPosition = new int[2];
        int[] textPosition = new int[2];
        header.getLocationInWindow(barPosition);
        paragraph.getLocationInWindow(textPosition);
        assertEquals(212, barPosition[0]);
        assertEquals(barPosition[0], textPosition[0]);
        assertEquals(776, header.getWidth());
        assertEquals(header.getWidth(), paragraph.getWidth());
    }

    @Test public void largeTextHeadersReserveTheirEntireHeight() {
        screen(360, 220, true);
        StaticTopBarController.attach(activity);
        layout(360, 640);
        assertEquals(238, scroll.getTop());
        assertEquals(402, scroll.getHeight());
        assertEquals(220, header.getHeight());
    }

    @Test public void dynamicHymnInsertionKeepsExistingChildIndexes() {
        screen(360, 64, true);
        int insertion = body.getChildCount();
        TextView hymn = new TextView(activity);
        body.addView(hymn, insertion);
        StaticTopBarController.attach(activity);
        assertEquals(insertion, body.indexOfChild(hymn));
        body.removeView(hymn);
        body.addView(hymn, insertion);
        assertSame(hymn, body.getChildAt(insertion));
        View firstPage = page();
        StaticTopBarController.attach(activity);
        assertSame(firstPage, page());
    }

    @Test public void readerPreferencesRepaintFixedBarBackground() {
        screen(360, 64, true);
        NativeUi.readerPage(scroll);
        StaticTopBarController.attach(activity);
        page().setBackgroundColor(Color.MAGENTA);
        NativeUi.refreshReader(page());
        assertEquals(((ColorDrawable) scroll.getBackground()).getColor(),
                ((ColorDrawable) page().getBackground()).getColor());
    }
}
