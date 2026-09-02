package com.fabri.ministerium;

import android.content.Context;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Small, best-effort runtime warmup. It never performs network I/O and never
 * blocks the UI thread. Only data that is highly likely to be opened from the
 * home screen is prepared.
 */
public final class RuntimeWarmup {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private RuntimeWarmup() {}

    public static void start(Context context) {
        if (!STARTED.compareAndSet(false, true)) return;
        final Context app = context.getApplicationContext();
        new Thread(() -> {
            Calendar today = Calendar.getInstance();
            try { LiturgicalDayCache.prepare(app, today, false); }
            catch (Exception ignored) {}

            // Prepare tomorrow as a very small navigation cache, still offline.
            Calendar tomorrow = (Calendar) today.clone();
            tomorrow.add(Calendar.DATE, 1);
            try { LiturgicalDayCache.prepare(app, tomorrow, false); }
            catch (Exception ignored) {}

            trimReconstructibleCache(app);
        }, "ministerium-runtime-warmup").start();
    }

    /** Keep reconstructible caches bounded without touching user data. */
    private static void trimReconstructibleCache(Context context) {
        File root = context.getCacheDir();
        File[] files = root == null ? null : root.listFiles();
        if (files == null) return;
        long cutoff = System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L;
        for (File file : files) {
            if (file == null || file.isDirectory() || file.lastModified() >= cutoff) continue;
            String name = file.getName();
            if (name.startsWith("ministerium-") || name.startsWith("epub-")) {
                try { file.delete(); } catch (Exception ignored) {}
            }
        }
    }
}
