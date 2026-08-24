package com.fabri.ministerium;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Esquema compacto de contenido semántico.
 *
 * Esta base no contiene reglas de precedencia del calendario: almacena bloques,
 * relaciones y asignaciones que el motor litúrgico resuelve después de conocer
 * la celebración correspondiente a una fecha.
 */
public final class SemanticContentDbHelper extends SQLiteOpenHelper {
    public static final int SCHEMA_VERSION = 1;

    public static final String TABLE_META = "package_meta";
    public static final String TABLE_BLOCK = "content_block";
    public static final String TABLE_RELATION = "content_relation";
    public static final String TABLE_ASSIGNMENT = "liturgical_assignment";

    public SemanticContentDbHelper(Context context, String databasePath) {
        super(context, databasePath, null, SCHEMA_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_META + " ("
                + "key TEXT PRIMARY KEY NOT NULL,"
                + "value TEXT NOT NULL DEFAULT ''"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_BLOCK + " ("
                + "block_id TEXT PRIMARY KEY NOT NULL,"
                + "kind TEXT NOT NULL,"
                + "title TEXT NOT NULL DEFAULT '',"
                + "body TEXT NOT NULL DEFAULT '',"
                + "reference_text TEXT NOT NULL DEFAULT '',"
                + "language TEXT NOT NULL DEFAULT '',"
                + "source_key TEXT NOT NULL DEFAULT ''"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_RELATION + " ("
                + "relation_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "owner_id TEXT NOT NULL,"
                + "role TEXT NOT NULL,"
                + "target_id TEXT NOT NULL,"
                + "position INTEGER NOT NULL DEFAULT 0,"
                + "choice_group TEXT NOT NULL DEFAULT '',"
                + "condition_key TEXT NOT NULL DEFAULT ''"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_ASSIGNMENT + " ("
                + "assignment_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "celebration_key TEXT NOT NULL DEFAULT '',"
                + "season TEXT NOT NULL DEFAULT '',"
                + "rank_key TEXT NOT NULL DEFAULT '',"
                + "hour TEXT NOT NULL DEFAULT '',"
                + "weekday INTEGER NOT NULL DEFAULT -1,"
                + "cycle_key TEXT NOT NULL DEFAULT '',"
                + "role TEXT NOT NULL,"
                + "target_id TEXT NOT NULL,"
                + "priority INTEGER NOT NULL DEFAULT 0"
                + ")");

        db.execSQL("CREATE INDEX idx_relation_owner_role ON " + TABLE_RELATION
                + " (owner_id, role, position)");
        db.execSQL("CREATE INDEX idx_assignment_resolve ON " + TABLE_ASSIGNMENT
                + " (celebration_key, season, rank_key, hour, weekday, cycle_key, role, priority)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // La versión 1 es la primera base semántica. Las migraciones futuras deben
        // ser incrementales; nunca destruir silenciosamente un paquete instalado.
        if (oldVersion < 1) {
            onCreate(db);
        }
    }
}
