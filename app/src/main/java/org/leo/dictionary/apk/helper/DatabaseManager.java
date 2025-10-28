package org.leo.dictionary.apk.helper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.leo.dictionary.db.DatabaseConstants;
import org.leo.dictionary.db.DatabaseManagerParent;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.helper.SerializeUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class DatabaseManager extends DatabaseManagerParent<Cursor> {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DatabaseManager(DatabaseHelper databaseHelper) {
        dbHelper = databaseHelper;
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    @Override
    protected int findColumn(Cursor cursor, String columnName) {
        return cursor.getColumnIndexOrThrow(columnName);
    }

    @Override
    protected boolean moveToNext(Cursor cursor) {
        if (!cursor.isAfterLast()) {
            cursor.moveToNext();
            return !cursor.isAfterLast();
        }
        return false;
    }

    @Override
    protected long getLong(Cursor cursor, int index) {
        return cursor.getLong(index);
    }
    @Override
    protected byte[] getBlob(Cursor cursor, int index) {
        return cursor.getBlob(index);
    }

    @Override
    protected double getDouble(Cursor cursor, int index) {
        return cursor.getDouble(index);
    }

    @Override
    protected String getString(Cursor cursor, int index) {
        return cursor.getString(index);
    }

    @Override
    protected int getInt(Cursor cursor, int index) {
        return cursor.getInt(index);
    }

    public synchronized <T> T executeInTransaction(Supplier<T> supplier) {
        getDatabase().beginTransaction();
        try {
            T result = supplier.get();
            getDatabase().setTransactionSuccessful();
            return result;
        } finally {
            getDatabase().endTransaction();
        }
    }

    protected long insertWord(Word word) {
        long id = getWordId(word);
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseConstants.COLUMN_LANGUAGE, word.getLanguage());
        contentValue.put(DatabaseConstants.WORD_COLUMN_WORD, word.getWord());
        contentValue.put(DatabaseConstants.WORD_COLUMN_ARTICLE, word.getArticle());
        contentValue.put(DatabaseConstants.WORD_COLUMN_ADDITIONAL_INFORMATION, word.getAdditionalInformation());
        contentValue.put(DatabaseConstants.WORD_COLUMN_KNOWLEDGE, word.getKnowledge());
        return getDatabase().insert(DatabaseConstants.TABLE_NAME_WORD, null, contentValue);
    }

    public long insertTopic(Topic topic) {
        if (topic.getId() > 0) {
            return topic.getId();
        }
        long id = getTopicId(topic);
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseConstants.COLUMN_LANGUAGE, topic.getLanguage());
        contentValue.put(DatabaseConstants.TOPIC_COLUMN_LEVEL, topic.getLevel());
        if (topic.getRoot() != null) {
            contentValue.put(DatabaseConstants.TOPIC_COLUMN_ROOT_ID, insertTopic(topic.getRoot()));
        }
        contentValue.put(DatabaseConstants.TOPIC_COLUMN_NAME, topic.getName());
        long insertedId = getDatabase().insert(DatabaseConstants.TABLE_NAME_TOPIC, null, contentValue);
        topic.setId(insertedId);
        return insertedId;
    }

    public long insertTranslation(Translation translation, long wordId) {
        long id = getTranslationId(translation, wordId);
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseConstants.TRANSLATION_COLUMN_WORD_ID, wordId);
        contentValue.put(DatabaseConstants.TRANSLATION_COLUMN_TRANSLATION, translation.getTranslation());
        contentValue.put(DatabaseConstants.COLUMN_LANGUAGE, translation.getLanguage());
        long insertedId = getDatabase().insert(DatabaseConstants.TABLE_NAME_TRANSLATION, null, contentValue);
        translation.setId(insertedId);
        return insertedId;
    }

    public long insertWordTopicLink(long wordId, long topicId) {
        long id = getId(DatabaseConstants.TABLE_NAME_WORD_TOPIC,
                DatabaseConstants.COLUMN_ID + " = ? AND " + DatabaseConstants.TRANSLATION_COLUMN_WORD_ID + " = ?",
                Long.toString(topicId), Long.toString(wordId));
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseConstants.COLUMN_ID, topicId);
        contentValue.put(DatabaseConstants.TRANSLATION_COLUMN_WORD_ID, wordId);
        return getDatabase().insert(DatabaseConstants.TABLE_NAME_WORD_TOPIC, null, contentValue);
    }

    public int deleteWordTopicLink(long wordId, long topicId) {
        return getDatabase().delete(DatabaseConstants.TABLE_NAME_WORD_TOPIC,
                DatabaseConstants.TRANSLATION_COLUMN_WORD_ID + " = ? AND " + DatabaseConstants.COLUMN_ID + " = ?",
                new String[]{Long.toString(wordId), Long.toString(topicId)});
    }

    @Override
    protected Cursor rawQuery(String fullSql, String... selectionArgs) {
        return getDatabase().rawQuery(fullSql, selectionArgs);
    }

    @Override
    protected Cursor query(boolean distinct, String table,
                           String[] columns, String selection,
                           String[] selectionArgs,
                           String orderBy, String limit) {
        return getDatabase().query(distinct, table, columns, selection, selectionArgs, null, null, orderBy, limit);
    }

    public int deleteForLanguage(String language) {
        List<String> wordIds = getWordIdsForLanguage(language);
        for (int fromIndex = 0; fromIndex < wordIds.size(); fromIndex += DatabaseConstants.PAGE_SIZE) {
            deleteWords(wordIds.subList(fromIndex, Math.min(wordIds.size(), fromIndex + DatabaseConstants.PAGE_SIZE)));
        }
        getDatabase().delete(DatabaseConstants.TABLE_NAME_TOPIC, DatabaseConstants.COLUMN_LANGUAGE + "= ?", new String[]{language});
        return wordIds.size();
    }

    public void vacuum() {
        getDatabase().execSQL("VACUUM");
    }

    protected int deleteWords(List<String> wordIds) {
        getDatabase().delete(DatabaseConstants.TABLE_NAME_WORD, DatabaseConstants.COLUMN_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        getDatabase().delete(DatabaseConstants.TABLE_NAME_TRANSLATION, DatabaseConstants.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        getDatabase().delete(DatabaseConstants.TABLE_NAME_WORD_TOPIC, DatabaseConstants.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        return wordIds.size();
    }

    public int updateWord(Word word) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseConstants.COLUMN_LANGUAGE, word.getLanguage());
        contentValues.put(DatabaseConstants.WORD_COLUMN_WORD, word.getWord());
        contentValues.put(DatabaseConstants.WORD_COLUMN_ADDITIONAL_INFORMATION, word.getAdditionalInformation());
        contentValues.put(DatabaseConstants.WORD_COLUMN_ARTICLE, word.getArticle());
        contentValues.put(DatabaseConstants.WORD_COLUMN_KNOWLEDGE, word.getKnowledge());
        return getDatabase().update(DatabaseConstants.TABLE_NAME_WORD, contentValues, DatabaseConstants.COLUMN_ID + " = ?", new String[]{Long.toString(word.getId())});
    }

    public int updateTopic(Topic topic) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseConstants.COLUMN_LANGUAGE, topic.getLanguage());
        contentValues.put(DatabaseConstants.TOPIC_COLUMN_LEVEL, topic.getLevel());
        if (topic.getRoot() != null) {
            contentValues.put(DatabaseConstants.TOPIC_COLUMN_ROOT_ID, insertTopic(topic.getRoot()));
        } else {
            contentValues.put(DatabaseConstants.TOPIC_COLUMN_ROOT_ID, (String) null);
        }
        contentValues.put(DatabaseConstants.TOPIC_COLUMN_NAME, topic.getName());
        return getDatabase().update(DatabaseConstants.TABLE_NAME_TOPIC, contentValues, DatabaseConstants.COLUMN_ID + " = ?", new String[]{Long.toString(topic.getId())});
    }

    public int updateTranslation(Translation translation) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseConstants.COLUMN_LANGUAGE, translation.getLanguage());
        contentValues.put(DatabaseConstants.TRANSLATION_COLUMN_TRANSLATION, translation.getTranslation());
        return getDatabase().update(DatabaseConstants.TABLE_NAME_TRANSLATION, contentValues, DatabaseConstants.COLUMN_ID + " = ?", new String[]{Long.toString(translation.getId())});
    }

    public void deleteTranslation(long id) {
        getDatabase().delete(DatabaseConstants.TABLE_NAME_TRANSLATION, DatabaseConstants.COLUMN_ID + " = ?", new String[]{Long.toString(id)});
    }

    private SQLiteDatabase getDatabase() {
        return database;
    }

    public long insertConfigurationPreset(String name, Map<String, ?> data) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseConstants.COLUMN_ID, name);
        contentValue.put(DatabaseConstants.CONFIGURATION_PRESET_DATA, SerializeUtils.serializeToBytes(data));
        return getDatabase().insert(DatabaseConstants.TABLE_NAME_CONFIGURATION_PRESET, null, contentValue);
    }

    public int updateConfigurationPreset(String name, Map<String, ?> data) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseConstants.COLUMN_ID, name);
        contentValue.put(DatabaseConstants.CONFIGURATION_PRESET_DATA, SerializeUtils.serializeToBytes(data));
        return getDatabase().update(DatabaseConstants.TABLE_NAME_CONFIGURATION_PRESET, contentValue, DatabaseConstants.COLUMN_ID + " = ?", new String[]{name});
    }

    public int deleteConfigurationPreset(String name) {
        return getDatabase().delete(DatabaseConstants.TABLE_NAME_CONFIGURATION_PRESET, DatabaseConstants.COLUMN_ID + " = ?", new String[]{name});
    }
}