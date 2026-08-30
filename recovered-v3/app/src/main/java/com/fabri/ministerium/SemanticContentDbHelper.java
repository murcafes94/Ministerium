package com.fabri.ministerium;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Esquema compacto de contenido semántico.
 *
 * La identidad litúrgica es independiente del idioma. Un mismo unit_id puede
 * tener texto español y latino en paquetes distintos o en una misma base.
 * Los formularios contienen relaciones a esas unidades y pueden heredar de
 * otros formularios (propio -> temporal/feria/común) sin usar HTML ni EPUB.
 */
public final class SemanticContentDbHelper extends SQLiteOpenHelper {
    public static final int SCHEMA_VERSION = 2;

    public static final String TABLE_META = "package_meta";
    public static final String TABLE_UNIT = "semantic_unit";
    public static final String TABLE_TEXT = "localized_text";
    public static final String TABLE_FORM = "content_form";
    public static final String TABLE_INHERITANCE = "form_inheritance";
    public static final String TABLE_RELATION = "form_relation";
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
        createV2Tables(db);
    }

    private static void createV2Tables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_UNIT + " ("
                + "unit_id TEXT PRIMARY KEY NOT NULL,"
                + "kind TEXT NOT NULL"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_TEXT + " ("
                + "unit_id TEXT NOT NULL,"
                + "language TEXT NOT NULL,"
                + "title TEXT NOT NULL DEFAULT '',"
                + "body TEXT NOT NULL DEFAULT '',"
                + "reference_text TEXT NOT NULL DEFAULT '',"
                + "source_key TEXT NOT NULL DEFAULT '',"
                + "PRIMARY KEY (unit_id, language),"
                + "FOREIGN KEY (unit_id) REFERENCES " + TABLE_UNIT
                + "(unit_id) ON DELETE CASCADE"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_FORM + " ("
                + "form_id TEXT PRIMARY KEY NOT NULL,"
                + "form_type TEXT NOT NULL,"
                + "source_key TEXT NOT NULL DEFAULT ''"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_INHERITANCE + " ("
                + "child_form_id TEXT NOT NULL,"
                + "parent_form_id TEXT NOT NULL,"
                + "priority INTEGER NOT NULL DEFAULT 0,"
                + "PRIMARY KEY (child_form_id, parent_form_id),"
                + "FOREIGN KEY (child_form_id) REFERENCES " + TABLE_FORM
                + "(form_id) ON DELETE CASCADE,"
                + "FOREIGN KEY (parent_form_id) REFERENCES " + TABLE_FORM
                + "(form_id) ON DELETE CASCADE"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_RELATION + " ("
                + "relation_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "form_id TEXT NOT NULL,"
                + "role TEXT NOT NULL,"
                + "target_unit_id TEXT NOT NULL,"
                + "position INTEGER NOT NULL DEFAULT 0,"
                + "choice_group TEXT NOT NULL DEFAULT '',"
                + "condition_key TEXT NOT NULL DEFAULT '',"
                + "is_default INTEGER NOT NULL DEFAULT 0,"
                + "FOREIGN KEY (form_id) REFERENCES " + TABLE_FORM
                + "(form_id) ON DELETE CASCADE,"
                + "FOREIGN KEY (target_unit_id) REFERENCES " + TABLE_UNIT
                + "(unit_id) ON DELETE RESTRICT"
                + ")");

        db.execSQL("CREATE TABLE " + TABLE_ASSIGNMENT + " ("
                + "assignment_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "celebration_key TEXT NOT NULL DEFAULT '',"
                + "season TEXT NOT NULL DEFAULT '',"
                + "rank_key TEXT NOT NULL DEFAULT '',"
                + "hour TEXT NOT NULL DEFAULT '',"
                + "weekday INTEGER NOT NULL DEFAULT -1,"
                + "cycle_key TEXT NOT NULL DEFAULT '',"
                + "form_id TEXT NOT NULL,"
                + "priority INTEGER NOT NULL DEFAULT 0,"
                + "FOREIGN KEY (form_id) REFERENCES " + TABLE_FORM
                + "(form_id) ON DELETE CASCADE"
                + ")");

        db.execSQL("CREATE INDEX idx_text_language ON " + TABLE_TEXT
                + " (language, unit_id)");
        db.execSQL("CREATE INDEX idx_inheritance_child ON " + TABLE_INHERITANCE
                + " (child_form_id, priority)");
        db.execSQL("CREATE INDEX idx_relation_form_role ON " + TABLE_RELATION
                + " (form_id, role, position)");
        db.execSQL("CREATE INDEX idx_assignment_resolve ON " + TABLE_ASSIGNMENT
                + " (celebration_key, season, rank_key, hour, weekday, cycle_key, priority)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // La v1 todavía no fue usada como paquete de producción. Aun así se
            // conserva cualquier contenido experimental en tablas legacy para no
            // destruirlo silenciosamente durante el cambio de contrato.
            db.execSQL("ALTER TABLE content_block RENAME TO legacy_content_block_v1");
            db.execSQL("ALTER TABLE content_relation RENAME TO legacy_content_relation_v1");
            db.execSQL("ALTER TABLE liturgical_assignment RENAME TO legacy_liturgical_assignment_v1");
            createV2Tables(db);
        }
    }
}
