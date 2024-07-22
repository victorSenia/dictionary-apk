package org.leo.dictionary.apk.helper;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "dictionary.db";
    public static final String TABLE_NAME_TOPIC = "topic";
    public static final String TABLE_NAME_TRANSLATION = "translation";
    public static final String TABLE_NAME_WORD = "word";
    public static final String TABLE_NAME_WORD_TOPIC = "word_topic";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_LANGUAGE = "language";
    public static final String WORD_COLUMN_WORD = "word";
    public static final String WORD_COLUMN_ADDITIONAL_INFORMATION = "additional_information";
    public static final String WORD_COLUMN_ARTICLE = "article";
    public static final String WORD_COLUMN_KNOWLEDGE = "knowledge";

    public static final String TOPIC_COLUMN_NAME = "name";
    public static final String TOPIC_COLUMN_LEVEL = "level";

    public static final String TRANSLATION_COLUMN_WORD_ID = "word_" + COLUMN_ID;
    public static final String TRANSLATION_COLUMN_TRANSLATION = "translation";

    // database version
    static final int DB_VERSION = 1;

    // Creating table query

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    public void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_WORD_TOPIC);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_TRANSLATION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_WORD);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_TOPIC);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTables(db);
        onCreate(db);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME_TOPIC + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_LANGUAGE + " TEXT NOT NULL, " +
                TOPIC_COLUMN_NAME + " TEXT NOT NULL, " + TOPIC_COLUMN_LEVEL + " INTEGER);");
        db.execSQL("CREATE TABLE " + TABLE_NAME_WORD + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_LANGUAGE + " TEXT NOT NULL, " + WORD_COLUMN_WORD + " TEXT NOT NULL, " +
                WORD_COLUMN_ADDITIONAL_INFORMATION + " TEXT, " + WORD_COLUMN_ARTICLE + " TEXT, " + WORD_COLUMN_KNOWLEDGE + " REAL);");
        db.execSQL("CREATE TABLE " + TABLE_NAME_TRANSLATION + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_LANGUAGE + " TEXT NOT NULL, " +
                TRANSLATION_COLUMN_TRANSLATION + " TEXT NOT NULL, " + TRANSLATION_COLUMN_WORD_ID + " INTEGER," +
                "CONSTRAINT fk_word FOREIGN KEY (" + TRANSLATION_COLUMN_WORD_ID + ") REFERENCES " + TABLE_NAME_WORD + "(" + COLUMN_ID + ")" +
                ");");
        db.execSQL("CREATE TABLE " + TABLE_NAME_WORD_TOPIC + " (" + TRANSLATION_COLUMN_WORD_ID + " INTEGER, " + COLUMN_ID + " INTEGER, " +
                "CONSTRAINT fk_topic FOREIGN KEY (" + TRANSLATION_COLUMN_WORD_ID + ") REFERENCES " + TABLE_NAME_TRANSLATION + "(" + COLUMN_ID + ")," +
                "CONSTRAINT fk_word FOREIGN KEY (" + COLUMN_ID + ") REFERENCES " + TABLE_NAME_WORD + "(" + COLUMN_ID + ")" +
                ");");

        db.execSQL("CREATE UNIQUE INDEX " + TABLE_NAME_TOPIC + "_unique1 " + " ON " + TABLE_NAME_TOPIC + " (" + COLUMN_LANGUAGE + ", " + TOPIC_COLUMN_LEVEL + ", " + TOPIC_COLUMN_NAME + ");");
        db.execSQL("CREATE UNIQUE INDEX " + TABLE_NAME_WORD + "_unique1 " + " ON " + TABLE_NAME_WORD + " (" + COLUMN_LANGUAGE + ", " + WORD_COLUMN_WORD + ", " + WORD_COLUMN_ARTICLE + ", " + WORD_COLUMN_ADDITIONAL_INFORMATION + ");");
        db.execSQL("CREATE UNIQUE INDEX " + TABLE_NAME_TRANSLATION + "_unique1 " + " ON " + TABLE_NAME_TRANSLATION + " (" + TRANSLATION_COLUMN_WORD_ID + ", " + COLUMN_LANGUAGE + ", " + TRANSLATION_COLUMN_TRANSLATION + ");");
        db.execSQL("CREATE UNIQUE INDEX " + TABLE_NAME_WORD_TOPIC + "_unique1 " + " ON " + TABLE_NAME_WORD_TOPIC + " (" + COLUMN_ID + ", " + TRANSLATION_COLUMN_WORD_ID + ");");
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME_TOPIC);
    }
}