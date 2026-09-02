package com.fabri.ministerium;

import android.content.Context;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small process cache for resolved daily liturgical data. Network-backed
 * repositories keep their own disk caches; this class prevents repeated parsing
 * and calendar resolution while moving between screens.
 */
public final class LiturgicalDayCache {
    private static final int MAX_ENTRIES = 10;
    private static final long MAX_AGE_MS = 30L * 60L * 1000L;
    private static final Map<String, LiturgicalDayPackage> CACHE =
            new LinkedHashMap<String, LiturgicalDayPackage>(16, 0.75f, true) {
                @Override protected boolean removeEldestEntry(Map.Entry<String, LiturgicalDayPackage> eldest) {
                    return size() > MAX_ENTRIES;
                }
            };

    private LiturgicalDayCache() {}

    public static LiturgicalDayPackage prepare(Context context, Calendar selected,
                                                boolean allowNetwork) throws Exception {
        Calendar date = normalized(selected);
        String key = key(date);
        synchronized (CACHE) {
            LiturgicalDayPackage cached = CACHE.get(key);
            if (cached != null && System.currentTimeMillis() - cached.preparedAt < MAX_AGE_MS) {
                return cached;
            }
        }

        LiturgicalDay day = LiturgicalSourceResolver.day(context, date);
        if (allowNetwork && MassReadingsRepository.isCurrentMonth(date)) {
            if (!MassReadingsRepository.has(context, date)) {
                try { MassReadingsRepository.syncDay(context.getApplicationContext(), date); }
                catch (Exception ignored) {}
            }
            try { DailyMassProperRepository.getOrSync(context.getApplicationContext(), date); }
            catch (Exception ignored) {}
        }

        boolean readings = false;
        try { readings = MassReadingsRepository.has(context, date); }
        catch (Exception ignored) {}
        DailyMassProperRepository.ProperDay proper = DailyMassProperRepository.cached(context, date);
        LiturgicalDayPackage result = new LiturgicalDayPackage(
                date, day, readings, proper, System.currentTimeMillis());
        synchronized (CACHE) { CACHE.put(key, result); }
        return result;
    }

    /** Best-effort preparation of the next few days; never blocks the caller. */
    public static void prefetch(Context context, Calendar from, int days) {
        final Context app = context.getApplicationContext();
        final Calendar start = normalized(from);
        final int count = Math.max(0, Math.min(days, 7));
        new Thread(() -> {
            for (int i = 1; i <= count; i++) {
                Calendar date = (Calendar) start.clone();
                date.add(Calendar.DATE, i);
                try { prepare(app, date, true); }
                catch (Exception ignored) {}
            }
        }, "ministerium-liturgical-prefetch").start();
    }

    public static void invalidate(Calendar selected) {
        synchronized (CACHE) { CACHE.remove(key(normalized(selected))); }
    }

    public static void clearMemory() {
        synchronized (CACHE) { CACHE.clear(); }
    }

    private static Calendar normalized(Calendar source) {
        Calendar date = Calendar.getInstance();
        date.clear();
        date.set(source.get(Calendar.YEAR), source.get(Calendar.MONTH),
                source.get(Calendar.DAY_OF_MONTH), 12, 0, 0);
        return date;
    }

    private static String key(Calendar date) {
        return date.get(Calendar.YEAR) + "-" + (date.get(Calendar.MONTH) + 1)
                + "-" + date.get(Calendar.DAY_OF_MONTH);
    }
}
