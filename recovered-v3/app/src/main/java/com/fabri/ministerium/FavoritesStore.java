package com.fabri.ministerium;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

public final class FavoritesStore {
    private static final String PREFS = "ministerium_favorites";
    private static final String KEY = "pages";
    private static final String ITEMS_KEY = "items";

    private FavoritesStore() {}

    public static String key(String documentId, int pageIndex) {
        return documentId + ":" + pageIndex;
    }

    public static Set<String> all(Context context) {
        return new HashSet<>(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY, new HashSet<>()));
    }

    public static boolean contains(Context context, String documentId, int pageIndex) {
        return all(context).contains(key(documentId, pageIndex));
    }

    public static void toggle(Context context, String documentId, int pageIndex) {
        Set<String> values = all(context);
        String key = key(documentId, pageIndex);
        if (!values.add(key)) values.remove(key);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(KEY, values).apply();
    }

    public static Set<String> allItems(Context context) {
        return new HashSet<>(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(ITEMS_KEY, new HashSet<>()));
    }

    public static boolean containsItem(Context context, String itemKey) {
        return allItems(context).contains(itemKey);
    }

    public static void toggleItem(Context context, String itemKey) {
        Set<String> values = allItems(context);
        if (!values.add(itemKey)) values.remove(itemKey);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putStringSet(ITEMS_KEY, values).apply();
    }
}
