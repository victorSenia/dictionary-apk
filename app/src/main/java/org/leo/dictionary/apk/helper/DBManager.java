package org.leo.dictionary.apk.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DBManager {

    public static final int PAGE_SIZE = 200;
    private final Context context;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
        open();
    }

    private static void mapTranslationsFromCursor(Cursor res, Map<Long, Word> wordsMap) {
        Translation translation;
        Word word;
        int idIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
        int languageIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE);
        int translationIndex = res.getColumnIndexOrThrow(DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION);
        int wordIdIndex = res.getColumnIndexOrThrow(DatabaseHelper.TRANSLATION_COLUMN_WORD_ID);
        while (!res.isAfterLast()) {
            translation = new Translation();
            translation.setId(res.getLong(idIndex));
            translation.setLanguage(res.getString(languageIndex));
            translation.setTranslation(res.getString(translationIndex));
            word = wordsMap.get(res.getLong(wordIdIndex));
            if (word != null) {
                if (word.getTranslations() == null) {
                    word.setTranslations(new ArrayList<>());
                }
                word.getTranslations().add(translation);
            }
            res.moveToNext();
        }
    }

    private static void mapWordsFromCursor(Cursor res, List<Word> words) {
        Word word;
        int idIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
        int languageIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE);
        int wordIndex = res.getColumnIndexOrThrow(DatabaseHelper.WORD_COLUMN_WORD);
        int articleIndex = res.getColumnIndexOrThrow(DatabaseHelper.WORD_COLUMN_ARTICLE);
        int additionalInformationIndex = res.getColumnIndexOrThrow(DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION);
        while (!res.isAfterLast()) {
            word = new Word();
            word.setId(res.getLong(idIndex));
            word.setWord(res.getString(wordIndex));
            word.setArticle(res.getString(articleIndex));
            word.setAdditionalInformation(res.getString(additionalInformationIndex));
            word.setLanguage(res.getString(languageIndex));
            words.add(word);
            res.moveToNext();
        }
    }

    protected DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public long insertFully(Word word) {
        long wordId = insert(word);
        for (Translation translation : word.getTranslations()) {
            insert(translation, wordId);
        }
        for (Topic topic : word.getTopics()) {
            insert(wordId, insert(topic));
        }
        return wordId;
    }

    protected long insert(Word word) {
        long id = getId(word);
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.COLUMN_LANGUAGE, word.getLanguage());
        contentValue.put(DatabaseHelper.WORD_COLUMN_WORD, word.getWord());
        contentValue.put(DatabaseHelper.WORD_COLUMN_ARTICLE, word.getArticle());
        contentValue.put(DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION, word.getAdditionalInformation());
        contentValue.put(DatabaseHelper.WORD_COLUMN_KNOWLEDGE, word.getKnowledge());
        return database.insert(DatabaseHelper.TABLE_NAME_WORD, null, contentValue);
    }

    protected long insert(Topic topic) {
        if (topic.getId() > 0) {
            return topic.getId();
        }
        long id = getId(topic);
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.COLUMN_LANGUAGE, topic.getLanguage());
        contentValue.put(DatabaseHelper.TOPIC_COLUMN_LEVEL, topic.getLevel());
        contentValue.put(DatabaseHelper.TOPIC_COLUMN_NAME, topic.getName());
        long insertedId = database.insert(DatabaseHelper.TABLE_NAME_TOPIC, null, contentValue);
        topic.setId(insertedId);
        return insertedId;
    }

    protected long getId(Topic topic) {
        return getId(DatabaseHelper.TABLE_NAME_TOPIC,
                DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.TOPIC_COLUMN_NAME + " = ? AND " + DatabaseHelper.TOPIC_COLUMN_LEVEL + " = ?",
                topic.getLanguage(), topic.getName(), Integer.toString(topic.getLevel()));
    }

    protected long getId(Translation translation, long wordId) {
        return getId(DatabaseHelper.TABLE_NAME_TRANSLATION,
                DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " = ? AND " + DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION + " = ?",
                translation.getLanguage(), Long.toString(wordId), translation.getTranslation());
    }

    protected long getId(Word word) {
        if (word.getArticle() == null) {
            if (word.getAdditionalInformation() == null) {
                return getId(DatabaseHelper.TABLE_NAME_WORD,
                        DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.WORD_COLUMN_WORD + " = ? AND " + DatabaseHelper.WORD_COLUMN_ARTICLE + " IS NULL AND "
                                + DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION + " IS NULL",
                        word.getLanguage(), word.getWord());
            }
            return getId(DatabaseHelper.TABLE_NAME_WORD,
                    DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.WORD_COLUMN_WORD + " = ? AND " + DatabaseHelper.WORD_COLUMN_ARTICLE + " IS NULL AND "
                            + DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION + " = ?",
                    word.getLanguage(), word.getWord(), word.getAdditionalInformation());
        } else if (word.getAdditionalInformation() == null) {
            return getId(DatabaseHelper.TABLE_NAME_WORD,
                    DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.WORD_COLUMN_WORD + " = ? AND " + DatabaseHelper.WORD_COLUMN_ARTICLE + " = ? AND "
                            + DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION + " IS NULL",
                    word.getLanguage(), word.getWord(), word.getArticle());
        } else {
            return getId(DatabaseHelper.TABLE_NAME_WORD,
                    DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.WORD_COLUMN_WORD + " = ? AND " + DatabaseHelper.WORD_COLUMN_ARTICLE + " = ? AND "
                            + DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION + " = ?",
                    word.getLanguage(), word.getWord(), word.getArticle(), word.getAdditionalInformation());
        }
    }

    protected long getId(String table, String selection, String... selectionArg) {
        try (Cursor cursor = database.query(true, table,
                new String[]{DatabaseHelper.COLUMN_ID}, selection,
                selectionArg,
                null, null, null, null)) {
            cursor.moveToFirst();
            if (cursor.getCount() > 0) {
                return cursor.getLong(0);
            }
        }
        return -1;
    }

    public long insert(Translation translation, long wordId) {
        long id = getId(translation, wordId);
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, wordId);
        contentValue.put(DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION, translation.getTranslation());
        contentValue.put(DatabaseHelper.COLUMN_LANGUAGE, translation.getLanguage());
        long insertedId = database.insert(DatabaseHelper.TABLE_NAME_TRANSLATION, null, contentValue);
        translation.setId(insertedId);
        return insertedId;
    }

    protected long insert(long wordId, long topicId) {
        long id = getId(DatabaseHelper.TABLE_NAME_WORD_TOPIC,
                DatabaseHelper.COLUMN_ID + " = ? AND " + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " = ?",
                Long.toString(topicId), Long.toString(wordId));
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.COLUMN_ID, topicId);
        contentValue.put(DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, wordId);
        return database.insert(DatabaseHelper.TABLE_NAME_WORD_TOPIC, null, contentValue);
    }

    protected Cursor fetchWords(WordCriteria criteria) {
        String sql = "SELECT DISTINCT w.* FROM " + DatabaseHelper.TABLE_NAME_WORD + " w";
        List<String> selectionArgs = new ArrayList<>();
        String where = "";
        if (criteria.getTopicsOr() != null && !criteria.getTopicsOr().isEmpty()) {
            List<String> topicIds = getTopicIds(criteria.getLanguageFrom(), criteria.getTopicsOr());
            sql += " INNER JOIN " + DatabaseHelper.TABLE_NAME_WORD_TOPIC + " t ON w." + DatabaseHelper.COLUMN_ID + " = t." + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " AND t." + DatabaseHelper.COLUMN_ID + " IN (" + createPlaceholders(topicIds.size()) + ")";
            selectionArgs.addAll(topicIds);
        }
        if (criteria.getLanguageFrom() != null) {
            where += " w." + DatabaseHelper.COLUMN_LANGUAGE + " = ?";
            selectionArgs.add(criteria.getLanguageFrom());
        }
        Cursor cursor = database.rawQuery(sql + (!where.isEmpty() ? " WHERE " + where : "") + " ORDER BY " + DatabaseHelper.COLUMN_ID, !selectionArgs.isEmpty() ? selectionArgs.toArray(selectionArgs.toArray(new String[0])) : null);
        cursor.moveToFirst();
        return cursor;
    }

    protected Cursor fetchTranslations(Set<String> languages, List<String> wordIds) {
        String selection = DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")";
        List<String> selectionArgs = new ArrayList<>(wordIds);
        if (languages != null && !languages.isEmpty()) {
            selection += " AND " + DatabaseHelper.COLUMN_LANGUAGE + " IN (" + createPlaceholders(languages.size()) + ")";
            selectionArgs.addAll(languages);
        }
        String[] columns = new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_LANGUAGE, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION};
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME_TRANSLATION,
                columns, selection,
                selectionArgs.toArray(selectionArgs.toArray(new String[0])),
                null, null, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, null);
        cursor.moveToFirst();
        return cursor;
    }

    protected List<String> getTopicIds(String languageFrom, Set<String> topicsOr) {
        try (Cursor res = fetchTopics(languageFrom, null, true, topicsOr)) {
            List<String> ids = new ArrayList<>();
            while (!res.isAfterLast()) {
                ids.add(res.getString(0));
                res.moveToNext();
            }
            return ids;
        }
    }

    protected Cursor fetchTopics(String language, String level, boolean idOnly, Set<String> names) {
        String selection = " 1 = 1";
        List<String> selectionArgs = new ArrayList<>();
        if (language != null) {
            selection += " AND " + DatabaseHelper.COLUMN_LANGUAGE + "= ?";
            selectionArgs.add(language);
        }
        if (level != null) {
            selection += " AND " + DatabaseHelper.TOPIC_COLUMN_LEVEL + "<= ?";
            selectionArgs.add(level);
        }
        if (names != null && !names.isEmpty()) {
            selection += " AND " + DatabaseHelper.TOPIC_COLUMN_NAME + " IN (" + createPlaceholders(names.size()) + ")";
            selectionArgs.addAll(names);
        }
        String[] columns;
        if (idOnly) {
            columns = new String[]{DatabaseHelper.COLUMN_ID};
        } else {
            columns = new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.TOPIC_COLUMN_NAME, DatabaseHelper.TOPIC_COLUMN_LEVEL};
        }
        Cursor cursor = database.query(DatabaseHelper.TABLE_NAME_TOPIC,
                columns, selection,
                selectionArgs.toArray(selectionArgs.toArray(new String[0])),
                null, null, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    public List<Topic> getTopics(String language, String level) {
        try (Cursor res = fetchTopics(language, level, false, null)) {
            List<Topic> topics = new ArrayList<>();
            if (!res.isAfterLast()) {
                int idIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
                int nameIndex = res.getColumnIndexOrThrow(DatabaseHelper.TOPIC_COLUMN_NAME);
                int levelIndex = res.getColumnIndexOrThrow(DatabaseHelper.TOPIC_COLUMN_LEVEL);
                while (!res.isAfterLast()) {
                    Topic topic = new Topic();
                    topic.setId(res.getLong(idIndex));
                    topic.setName(res.getString(nameIndex));
                    topic.setLevel(res.getInt(levelIndex));
                    topic.setLanguage(language);
                    topics.add(topic);
                    res.moveToNext();
                }
            }
            return topics;
        }
    }

    public List<String> languageFrom() {
        try (Cursor res = database.query(true, DatabaseHelper.TABLE_NAME_WORD,
                new String[]{DatabaseHelper.COLUMN_LANGUAGE}, null,
                null,
                null, null, null, null)) {
            List<String> languages = new ArrayList<>();
            res.moveToFirst();
            if (!res.isAfterLast()) {
                int columnIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE);
                while (!res.isAfterLast()) {
                    languages.add(res.getString(columnIndex));
                    res.moveToNext();
                }
            }
            return languages;
        }
    }

    public List<String> languageTo(String language) {//TODO
        try (Cursor res = database.query(true, DatabaseHelper.TABLE_NAME_TRANSLATION,
                new String[]{DatabaseHelper.COLUMN_LANGUAGE}, null,
                null,
                null, null, null, null)) {
            List<String> languages = new ArrayList<>();
            res.moveToFirst();
            if (!res.isAfterLast()) {
                int columnIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE);
                while (!res.isAfterLast()) {
                    languages.add(res.getString(columnIndex));
                    res.moveToNext();
                }
            }
            return languages;
        }
    }

    public List<Word> getWords(WordCriteria criteria) {
        List<Word> words = new ArrayList<>();
        try (Cursor res = fetchWords(criteria)) {
            if (!res.isAfterLast()) {
                mapWordsFromCursor(res, words);
            }
        }
        Map<Long, Word> wordsMap = words.stream().collect(Collectors.toMap(Word::getId, Function.identity()));
        List<String> wordIds = words.stream().map(w -> Long.toString(w.getId())).collect(Collectors.toList());
        for (int fromIndex = 0; fromIndex < words.size(); fromIndex += PAGE_SIZE) {
            try (Cursor res = fetchTranslations(criteria.getLanguageTo(), wordIds.subList(fromIndex, Math.min(wordIds.size(), fromIndex + PAGE_SIZE)))) {
                if (!res.isAfterLast()) {
                    mapTranslationsFromCursor(res, wordsMap);
                }
            }
        }
        return words;
    }

    protected String createPlaceholders(int length) {
        return String.join(", ", Collections.nCopies(length, "?"));
    }

    public int deleteWord(long id) {
        return deleteWords(Collections.singletonList(Long.toString(id)));
    }

    public int deleteForLanguage(String language) {
        List<String> wordIds = getWordIdsForLanguage(language);
        for (int fromIndex = 0; fromIndex < wordIds.size(); fromIndex += PAGE_SIZE) {
            deleteWords(wordIds.subList(fromIndex, Math.min(wordIds.size(), fromIndex + PAGE_SIZE)));
        }
        database.delete(DatabaseHelper.TABLE_NAME_TOPIC, DatabaseHelper.COLUMN_LANGUAGE + "= ?", new String[]{language});
        database.execSQL("VACUUM");
        return wordIds.size();
    }

    private int deleteWords(List<String> wordIds) {
        database.delete(DatabaseHelper.TABLE_NAME_WORD, DatabaseHelper.COLUMN_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        database.delete(DatabaseHelper.TABLE_NAME_TRANSLATION, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        database.delete(DatabaseHelper.TABLE_NAME_WORD_TOPIC, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        return wordIds.size();
    }

    protected List<String> getWordIdsForLanguage(String language) {
        try (Cursor res = database.query(true, DatabaseHelper.TABLE_NAME_WORD,
                new String[]{DatabaseHelper.COLUMN_ID}, DatabaseHelper.COLUMN_LANGUAGE + "= ?",
                new String[]{language},
                null, null, null, null)) {
            List<String> result = new ArrayList<>();
            res.moveToFirst();
            if (!res.isAfterLast()) {
                int columnIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
                while (!res.isAfterLast()) {
                    result.add(res.getString(columnIndex));
                    res.moveToNext();
                }
            }
            return result;
        }
    }

    public Word findWord(long id) {
        List<Word> words = new ArrayList<>();
        try (Cursor res = database.query(true, DatabaseHelper.TABLE_NAME_WORD,
                new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_LANGUAGE,
                        DatabaseHelper.WORD_COLUMN_WORD, DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION,
                        DatabaseHelper.WORD_COLUMN_ARTICLE},
                DatabaseHelper.COLUMN_ID + "= ?",
                new String[]{Long.toString(id)},
                null, null, null, null)) {
            res.moveToFirst();
            if (!res.isAfterLast()) {
                mapWordsFromCursor(res, words);
            }
        }
        Map<Long, Word> wordsMap = words.stream().collect(Collectors.toMap(Word::getId, Function.identity()));
        for (int fromIndex = 0; fromIndex < words.size(); fromIndex += PAGE_SIZE) {
            try (Cursor res = fetchTranslations(null, Collections.singletonList(Long.toString(id)))) {
                if (!res.isAfterLast()) {
                    mapTranslationsFromCursor(res, wordsMap);
                }
            }
        }
        if (!words.isEmpty()) {
            return words.get(0);
        }
        return null;
    }

    public int updateWord(Word word) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.COLUMN_LANGUAGE, word.getLanguage());
        contentValues.put(DatabaseHelper.WORD_COLUMN_WORD, word.getWord());
        contentValues.put(DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION, word.getAdditionalInformation());
        contentValues.put(DatabaseHelper.WORD_COLUMN_ARTICLE, word.getArticle());
        contentValues.put(DatabaseHelper.WORD_COLUMN_KNOWLEDGE, word.getKnowledge());
        return database.update(DatabaseHelper.TABLE_NAME_WORD, contentValues, DatabaseHelper.COLUMN_ID + " = ?", new String[]{Long.toString(word.getId())});
    }

    public int updateTranslation(Translation translation) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.COLUMN_LANGUAGE, translation.getLanguage());
        contentValues.put(DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION, translation.getTranslation());
        return database.update(DatabaseHelper.TABLE_NAME_TRANSLATION, contentValues, DatabaseHelper.COLUMN_ID + " = ?", new String[]{Long.toString(translation.getId())});
    }

    public void deleteTranslation(long id) {
        database.delete(DatabaseHelper.TABLE_NAME_TRANSLATION, DatabaseHelper.COLUMN_ID + " = ?", new String[]{Long.toString(id)});

    }
}