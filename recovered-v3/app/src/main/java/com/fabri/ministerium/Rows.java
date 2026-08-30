package com.fabri.ministerium;

import android.content.Context;
import android.widget.SimpleAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Rows {
    private Rows() {}

    public static Map<String, String> row(String title, String subtitle) {
        Map<String, String> row = new HashMap<>();
        row.put("title", title);
        row.put("subtitle", subtitle);
        return row;
    }

    public static SimpleAdapter adapter(Context context, List<Map<String, String>> rows) {
        return new SimpleAdapter(context, rows, R.layout.row_two_line,
                new String[]{"title", "subtitle"},
                new int[]{R.id.rowTitle, R.id.rowSubtitle});
    }

    public static List<Map<String, String>> emptyList() {
        return new ArrayList<>();
    }
}
