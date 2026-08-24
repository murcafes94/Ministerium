package com.fabri.ministerium.bible.semantic;

import android.content.Context;

import java.io.File;

/** Locates independently versioned semantic Bible packages in private app storage. */
public final class SemanticBiblePackages {
    public static final String DEFAULT_EDITION_ID = "bj-es";
    private static final String DIRECTORY = "content/bible";
    private static final String EXTENSION = ".ministerium-bible.sqlite";

    private SemanticBiblePackages() {}

    public static File directory(Context context) {
        return new File(context.getFilesDir(), DIRECTORY);
    }

    public static File packageFile(Context context, String editionId) {
        if (editionId == null || editionId.trim().isEmpty()) {
            throw new IllegalArgumentException("editionId is required");
        }
        String safe = editionId.trim().toLowerCase().replaceAll("[^a-z0-9._-]", "_");
        return new File(directory(context), safe + EXTENSION);
    }

    public static boolean isInstalled(Context context, String editionId) {
        File file = packageFile(context, editionId);
        return file.isFile() && file.length() > 0;
    }

    public static SqliteBibleRepository openIfInstalled(Context context, String editionId) {
        File file = packageFile(context, editionId);
        if (!file.isFile() || file.length() == 0) return null;
        return SqliteBibleRepository.openReadOnly(file);
    }
}
