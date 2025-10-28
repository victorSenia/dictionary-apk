package org.leo.dictionary.apk.helper;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.leo.dictionary.db.DatabaseConstants;

public class DatabaseHelper extends SQLiteOpenHelper implements DatabaseConstants<SQLiteDatabase> {
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    @Override
    public void execSQL(SQLiteDatabase db, String sql) {
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DatabaseConstants.super.onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        DatabaseConstants.super.onCreate(db);
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME_TOPIC);
    }
}