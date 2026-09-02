package com.fabri.ministerium;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Small process cache for repeated local searches. Static content is immutable per APK. */
public final class SearchResultCache {
    private static final int MAX_QUERIES = 12;
    private static final Map<String, List<SearchResult>> CACHE =
            new LinkedHashMap<String, List<SearchResult>>(16, .75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, List<SearchResult>> eldest) {
                    return size() > MAX_QUERIES;
                }
            };

    private SearchResultCache() {}

    public static String key(String scope, String documentId, String query) {
        return safe(scope) + "|" + safe(documentId) + "|"
                + safe(query).trim().toLowerCase(Locale.ROOT);
    }

    public static List<SearchResult> get(String key) {
        synchronized (CACHE) {
            List<SearchResult> value = CACHE.get(key);
            return value == null ? null : new ArrayList<>(value);
        }
    }

    public static void put(String key, List<SearchResult> results) {
        if (results == null) return;
        synchronized (CACHE) { CACHE.put(key, new ArrayList<>(results)); }
    }

    public static void clear() {
        synchronized (CACHE) { CACHE.clear(); }
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
