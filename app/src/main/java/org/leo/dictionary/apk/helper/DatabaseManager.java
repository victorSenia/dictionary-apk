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
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class DatabaseManager {

    public static final int PAGE_SIZE = 200;
    private final Context context;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DatabaseManager(Context c) {
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
        int knowledgeIndex = res.getColumnIndexOrThrow(DatabaseHelper.WORD_COLUMN_KNOWLEDGE);
        while (!res.isAfterLast()) {
            word = new Word();
            word.setId(res.getLong(idIndex));
            word.setWord(res.getString(wordIndex));
            word.setArticle(res.getString(articleIndex));
            word.setAdditionalInformation(res.getString(additionalInformationIndex));
            word.setLanguage(res.getString(languageIndex));
            word.setKnowledge(res.getDouble(knowledgeIndex));
            words.add(word);
            res.moveToNext();
        }
    }

    private static void mapTopicsFromCursor(String language, Cursor res, List<Topic> topics, Map<Long, Topic> loadedTopics) {
        int idIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
        int nameIndex = res.getColumnIndexOrThrow(DatabaseHelper.TOPIC_COLUMN_NAME);
        int levelIndex = res.getColumnIndexOrThrow(DatabaseHelper.TOPIC_COLUMN_LEVEL);
        int rootIndex = res.getColumnIndexOrThrow(DatabaseHelper.TOPIC_COLUMN_ROOT_ID);
        int languageIndex = res.getColumnIndexOrThrow(DatabaseHelper.COLUMN_LANGUAGE);
        Topic topic;
        while (!res.isAfterLast()) {
            long id = res.getLong(idIndex);
            topic = loadedTopics.get(id);
            if (topic != null) {
                topics.add(topic);
            } else {
                topic = new Topic();
                topic.setId(id);
                topic.setName(res.getString(nameIndex));
                topic.setLevel(res.getInt(levelIndex));
                topic.setLanguage(res.getString(languageIndex));
                long rootId = res.getLong(rootIndex);
                if (rootId > 0) {
                    Topic rootTopic = loadedTopics.get(rootId);
                    if (rootTopic != null) {
                        topic.setRoot(rootTopic);
                    }
                }
                topics.add(topic);
                loadedTopics.put(topic.getId(), topic);
            }
            res.moveToNext();
        }
    }

    protected void open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertFully(Word word) {
        long wordId = insertWord(word);
        if (word.getTranslations() != null) {
            for (Translation translation : word.getTranslations()) {
                insertTranslation(translation, wordId);
            }
        }
        if (word.getTopics() != null) {
            for (Topic topic : word.getTopics()) {
                insertWordTopicLink(wordId, insertTopic(topic));
            }
        }
        return wordId;
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
        contentValue.put(DatabaseHelper.COLUMN_LANGUAGE, word.getLanguage());
        contentValue.put(DatabaseHelper.WORD_COLUMN_WORD, word.getWord());
        contentValue.put(DatabaseHelper.WORD_COLUMN_ARTICLE, word.getArticle());
        contentValue.put(DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION, word.getAdditionalInformation());
        contentValue.put(DatabaseHelper.WORD_COLUMN_KNOWLEDGE, word.getKnowledge());
        return getDatabase().insert(DatabaseHelper.TABLE_NAME_WORD, null, contentValue);
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
        contentValue.put(DatabaseHelper.COLUMN_LANGUAGE, topic.getLanguage());
        contentValue.put(DatabaseHelper.TOPIC_COLUMN_LEVEL, topic.getLevel());
        if (topic.getRoot() != null) {
            contentValue.put(DatabaseHelper.TOPIC_COLUMN_ROOT_ID, insertTopic(topic.getRoot()));
        }
        contentValue.put(DatabaseHelper.TOPIC_COLUMN_NAME, topic.getName());
        long insertedId = getDatabase().insert(DatabaseHelper.TABLE_NAME_TOPIC, null, contentValue);
        topic.setId(insertedId);
        return insertedId;
    }

    protected long getTopicId(Topic topic) {
        List<String> arguments = new ArrayList<>();
        arguments.add(topic.getLanguage());
        arguments.add(topic.getName());
        arguments.add(Integer.toString(topic.getLevel()));
        return getId(DatabaseHelper.TABLE_NAME_TOPIC,
                DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.TOPIC_COLUMN_NAME + " = ? AND " + DatabaseHelper.TOPIC_COLUMN_LEVEL + " = ?" +
                        andNullOrEquals(DatabaseHelper.TOPIC_COLUMN_ROOT_ID, () -> topic.getRoot() != null ? Long.toString(insertTopic(topic.getRoot())) : null, arguments),
                arguments.toArray(new String[0]));
    }

    private String andNullOrEquals(String column, Supplier<String> supplier, List<String> arguments) {
        return " AND " + nullOrEquals(column, supplier, arguments);
    }

    private String nullOrEquals(String column, Supplier<String> supplier, List<String> arguments) {
        String value = supplier.get();
        if (value == null) {
            return column + " IS NULL ";
        }
        arguments.add(value);
        return column + " = ? ";
    }

    protected long getTranslationId(Translation translation, long wordId) {
        return getId(DatabaseHelper.TABLE_NAME_TRANSLATION,
                DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " = ? AND " + DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION + " = ?",
                translation.getLanguage(), Long.toString(wordId), translation.getTranslation());
    }

    protected long getWordId(Word word) {
        List<String> arguments = new ArrayList<>();
        arguments.add(word.getLanguage());
        arguments.add(word.getWord());
        return getId(DatabaseHelper.TABLE_NAME_WORD,
                DatabaseHelper.COLUMN_LANGUAGE + " = ? AND " + DatabaseHelper.WORD_COLUMN_WORD + " = ? " +
                        andNullOrEquals(DatabaseHelper.WORD_COLUMN_ARTICLE, word::getArticle, arguments) +
                        andNullOrEquals(DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION, word::getAdditionalInformation, arguments),
                arguments.toArray(new String[0]));
    }

    protected long getId(String table, String selection, String... selectionArg) {
        try (Cursor cursor = getDatabase().query(true, table,
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

    public long insertTranslation(Translation translation, long wordId) {
        long id = getTranslationId(translation, wordId);
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, wordId);
        contentValue.put(DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION, translation.getTranslation());
        contentValue.put(DatabaseHelper.COLUMN_LANGUAGE, translation.getLanguage());
        long insertedId = getDatabase().insert(DatabaseHelper.TABLE_NAME_TRANSLATION, null, contentValue);
        translation.setId(insertedId);
        return insertedId;
    }

    public long insertWordTopicLink(long wordId, long topicId) {
        long id = getId(DatabaseHelper.TABLE_NAME_WORD_TOPIC,
                DatabaseHelper.COLUMN_ID + " = ? AND " + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " = ?",
                Long.toString(topicId), Long.toString(wordId));
        if (id != -1) {
            return id;
        }
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.COLUMN_ID, topicId);
        contentValue.put(DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, wordId);
        return getDatabase().insert(DatabaseHelper.TABLE_NAME_WORD_TOPIC, null, contentValue);
    }

    public int deleteWordTopicLink(long wordId, long topicId) {
        return getDatabase().delete(DatabaseHelper.TABLE_NAME_WORD_TOPIC,
                DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " = ? AND " + DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{Long.toString(wordId), Long.toString(topicId)});
    }

    protected Cursor fetchWordsCursor(WordCriteria criteria, boolean countOnly) {
        String sql = "SELECT " + (countOnly ? "COUNT (DISTINCT w." + DatabaseHelper.COLUMN_ID + ")" : "DISTINCT w.*") + " FROM " + DatabaseHelper.TABLE_NAME_WORD + " w";
        List<String> selectionArgs = new ArrayList<>();
        String where = " WHERE 1=1";
        if ((criteria.getTopicsOr() != null && !criteria.getTopicsOr().isEmpty()) ||
                (criteria.getRootTopics() != null && !criteria.getRootTopics().isEmpty())) {
            Set<String> topicIds = getTopicIds(criteria.getLanguageFrom(), criteria.getRootTopics(), criteria.getTopicsOr());
            sql += " INNER JOIN " + DatabaseHelper.TABLE_NAME_WORD_TOPIC + " t ON w." + DatabaseHelper.COLUMN_ID + " = t." + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " AND t." + DatabaseHelper.COLUMN_ID + " IN (" + createPlaceholders(topicIds.size()) + ")";
            selectionArgs.addAll(topicIds);
        }
        if ((criteria.getLanguageTo() != null && !criteria.getLanguageTo().isEmpty())) {
            sql += " INNER JOIN " + DatabaseHelper.TABLE_NAME_TRANSLATION + " tr ON w." + DatabaseHelper.COLUMN_ID + " = tr." + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " AND tr." + DatabaseHelper.COLUMN_LANGUAGE + " IN (" + createPlaceholders(criteria.getLanguageTo().size()) + ")";
            selectionArgs.addAll(criteria.getLanguageTo());
        }
        if (criteria.getLanguageFrom() != null) {
            where += " AND w." + DatabaseHelper.COLUMN_LANGUAGE + " = ?";
            selectionArgs.add(criteria.getLanguageFrom());
        }
        if (criteria.getKnowledgeFrom() != null) {
            where += " AND w." + DatabaseHelper.WORD_COLUMN_KNOWLEDGE + " >= ?";
            selectionArgs.add(criteria.getKnowledgeFrom().toString());
        }
        if (criteria.getKnowledgeTo() != null) {
            where += " AND w." + DatabaseHelper.WORD_COLUMN_KNOWLEDGE + " <= ?";
            selectionArgs.add(criteria.getKnowledgeTo().toString());
        }
        String orderBy = criteria.getShuffleRandom() != -1 ? "w." + DatabaseHelper.COLUMN_ID : "w." + DatabaseHelper.COLUMN_LANGUAGE + ", w." + DatabaseHelper.WORD_COLUMN_WORD + " COLLATE NOCASE";
        String fullSql = sql + where + (countOnly ? "" : " ORDER BY " + orderBy);
        Cursor cursor = getDatabase().rawQuery(fullSql, !selectionArgs.isEmpty() ? selectionArgs.toArray(selectionArgs.toArray(new String[0])) : null);
        cursor.moveToFirst();
        return cursor;
    }

    protected Cursor fetchTranslationsCursor(Set<String> languages, List<String> wordIds) {
        String selection = DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")";
        List<String> selectionArgs = new ArrayList<>(wordIds);
        if (languages != null && !languages.isEmpty()) {
            selection += " AND " + DatabaseHelper.COLUMN_LANGUAGE + " IN (" + createPlaceholders(languages.size()) + ")";
            selectionArgs.addAll(languages);
        }
        String[] columns = new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_LANGUAGE, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION};
        Cursor cursor = getDatabase().query(DatabaseHelper.TABLE_NAME_TRANSLATION,
                columns, selection,
                selectionArgs.toArray(selectionArgs.toArray(new String[0])),
                null, null, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID, null);
        cursor.moveToFirst();
        return cursor;
    }

    protected Set<String> getTopicIds(String languageFrom, Set<Topic> rootTopic, Set<Topic> topicsOr) {
        if (topicsOr != null && !topicsOr.isEmpty()) {
            return topicsOr.stream().map(topic -> Long.toString(topic.getId())).collect(Collectors.toSet());
        }
        try (Cursor res = fetchTopics(languageFrom, null, true, rootTopic.stream().map(topic -> Long.toString(topic.getId())).collect(Collectors.toSet()))) {
            Set<String> ids = new HashSet<>();
            while (!res.isAfterLast()) {
                ids.add(res.getString(0));
                res.moveToNext();
            }
            return ids;
        }
    }

    protected Cursor fetchTopics(String language, String level, boolean idOnly, Set<String> rootId) {
        String selection = " 1 = 1";
        List<String> selectionArgs = new ArrayList<>();
        if (language != null) {
            selection += " AND " + DatabaseHelper.COLUMN_LANGUAGE + "= ?";
            selectionArgs.add(language);
        }
        if (level != null) {
            selection += " AND " + DatabaseHelper.TOPIC_COLUMN_LEVEL + "= ?";
            selectionArgs.add(level);
        }
        if (rootId != null && !rootId.isEmpty()) {
            selection += " AND " + DatabaseHelper.TOPIC_COLUMN_ROOT_ID + " IN (" + createPlaceholders(rootId.size()) + ")";
            selectionArgs.addAll(rootId);
        }
        String[] columns;
        if (idOnly) {
            columns = new String[]{DatabaseHelper.COLUMN_ID};
        } else {
            columns = new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_LANGUAGE, DatabaseHelper.TOPIC_COLUMN_NAME, DatabaseHelper.TOPIC_COLUMN_LEVEL, DatabaseHelper.TOPIC_COLUMN_ROOT_ID};
        }
        Cursor cursor = getDatabase().query(DatabaseHelper.TABLE_NAME_TOPIC,
                columns, selection,
                selectionArgs.toArray(selectionArgs.toArray(new String[0])),
                null, null, null, null);
        cursor.moveToFirst();
        return cursor;
    }

    public List<Topic> getTopics(String language, Set<Long> rootIds, String level) {
        HashMap<Long, Topic> loadedTopics = new HashMap<>();
        if (rootIds != null) {
            loadedTopics.putAll(findRootTopics(language).stream().collect(Collectors.toMap(Topic::getId, Function.identity())));
        }
        try (Cursor res = fetchTopics(language, level, false, rootIds != null ? rootIds.stream().map(Object::toString).collect(Collectors.toSet()) : null)) {
            List<Topic> topics = new ArrayList<>();
            if (!res.isAfterLast()) {
                mapTopicsFromCursor(language, res, topics, loadedTopics);
            }
            return topics;
        }
    }

    public List<Topic> findRootTopics(String language) {
        return getTopics(language, null, "1");
    }

    public List<Topic> getTopicsForWord(String wordId, String language, String level, Map<Long, Topic> loadedTopics) {
        try (Cursor res = getDatabase().rawQuery("SELECT t.* FROM " + DatabaseHelper.TABLE_NAME_TOPIC + " t " +
                        " INNER JOIN " + DatabaseHelper.TABLE_NAME_WORD_TOPIC + " tw " +
                        " ON t." + DatabaseHelper.COLUMN_ID + " = tw." + DatabaseHelper.COLUMN_ID +
                        " AND tw." + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + "= ?" +
                        " AND t." + DatabaseHelper.COLUMN_LANGUAGE + "= ?" +
                        " AND t." + DatabaseHelper.TOPIC_COLUMN_LEVEL + "= ?"
                , new String[]{wordId, language, level})) {
            res.moveToFirst();
            List<Topic> topics = new ArrayList<>();
            if (!res.isAfterLast()) {
                mapTopicsFromCursor(language, res, topics, loadedTopics);
            }
            return topics;
        }
    }

    public List<String> languageFrom() {
        try (Cursor res = getDatabase().query(true, DatabaseHelper.TABLE_NAME_WORD,
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

    public List<String> languageTo(String language) {
        try (Cursor res = languageToCursor(language)) {
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

    private Cursor languageToCursor(String language) {
        if (language != null) {
            return getDatabase().rawQuery("SELECT DISTINCT t." + DatabaseHelper.COLUMN_LANGUAGE +
                    " FROM " + DatabaseHelper.TABLE_NAME_TRANSLATION + " t" + " INNER JOIN " + DatabaseHelper.TABLE_NAME_WORD + " w " +
                    "ON w." + DatabaseHelper.COLUMN_ID + " = t." + DatabaseHelper.TRANSLATION_COLUMN_WORD_ID +
                    " AND w." + DatabaseHelper.COLUMN_LANGUAGE + "= ?", new String[]{language});
        } else {
            return getDatabase().query(true, DatabaseHelper.TABLE_NAME_TRANSLATION,
                    new String[]{DatabaseHelper.COLUMN_LANGUAGE}, null,
                    null,
                    null, null, null, null);
        }
    }

    public List<Word> getWords(WordCriteria criteria) {
        return getWords(() -> fetchWordsCursor(criteria, false), criteria.getLanguageTo(), false);
    }

    public int countWords(WordCriteria criteria) {
        try (Cursor res = fetchWordsCursor(criteria, true)) {
            if (!res.isAfterLast()) {
                return res.getInt(0);
            }
        }
        return 0;
    }

    public List<Word> getWordsForLanguage(String language, Set<Topic> rootTopics) {
        WordCriteria criteria = new WordCriteria();
        criteria.setLanguageFrom(language);
        criteria.setRootTopics(rootTopics);
        return getWords(() -> fetchWordsCursor(criteria, false), null, true);
    }


    private List<Word> getWords(CursorProvider cursorProvider, Set<String> languages, boolean includeTopics) {
        List<Word> words = new ArrayList<>();
        try (Cursor res = cursorProvider.getCursor()) {
            if (!res.isAfterLast()) {
                mapWordsFromCursor(res, words);
            }
        }
        Map<Long, Word> wordsMap = words.stream().collect(Collectors.toMap(Word::getId, Function.identity()));
        List<String> wordIds = words.stream().map(w -> Long.toString(w.getId())).collect(Collectors.toList());
        for (int fromIndex = 0; fromIndex < words.size(); fromIndex += PAGE_SIZE) {
            try (Cursor res = fetchTranslationsCursor(languages, wordIds.subList(fromIndex, Math.min(wordIds.size(), fromIndex + PAGE_SIZE)))) {
                if (!res.isAfterLast()) {
                    mapTranslationsFromCursor(res, wordsMap);
                }
            }
        }
        words = words.stream().filter(this::hasTranslations).collect(Collectors.toList());
        if (includeTopics && !words.isEmpty()) {
            Map<Long, Topic> topics = new HashMap<>(getTopics(null, (Set<Long>) null, "1").stream().collect(Collectors.toMap(Topic::getId, Function.identity())));
            for (Word word : words) {
                word.setTopics(getTopicsForWord(String.valueOf(word.getId()), word.getLanguage(), "2", topics));//TODO
            }
        }
        return words;
    }

    private boolean hasTranslations(Word w) {
        return w.getTranslations() != null && !w.getTranslations().isEmpty();
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
        getDatabase().delete(DatabaseHelper.TABLE_NAME_TOPIC, DatabaseHelper.COLUMN_LANGUAGE + "= ?", new String[]{language});
        return wordIds.size();
    }

    public void vacuum() {
        getDatabase().execSQL("VACUUM");
    }

    private int deleteWords(List<String> wordIds) {
        getDatabase().delete(DatabaseHelper.TABLE_NAME_WORD, DatabaseHelper.COLUMN_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        getDatabase().delete(DatabaseHelper.TABLE_NAME_TRANSLATION, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        getDatabase().delete(DatabaseHelper.TABLE_NAME_WORD_TOPIC, DatabaseHelper.TRANSLATION_COLUMN_WORD_ID + " IN (" + createPlaceholders(wordIds.size()) + ")", wordIds.toArray(new String[0]));
        return wordIds.size();
    }

    protected List<String> getWordIdsForLanguage(String language) {
        try (Cursor res = getDatabase().query(true, DatabaseHelper.TABLE_NAME_WORD,
                new String[]{DatabaseHelper.COLUMN_ID}, DatabaseHelper.COLUMN_LANGUAGE + "= ?",
                new String[]{language},
                null, null, null, null)) {
            res.moveToFirst();
            List<String> result = new ArrayList<>();
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
        List<Word> words = getWords(() -> getCursorForWordById(id), null, true);
        if (!words.isEmpty()) {
            return words.get(0);
        }
        return null;
    }

    private Cursor getCursorForWordById(long id) {
        Cursor cursor = getDatabase().query(true, DatabaseHelper.TABLE_NAME_WORD,
                new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.COLUMN_LANGUAGE,
                        DatabaseHelper.WORD_COLUMN_WORD, DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION,
                        DatabaseHelper.WORD_COLUMN_ARTICLE, DatabaseHelper.WORD_COLUMN_KNOWLEDGE},
                DatabaseHelper.COLUMN_ID + "= ?",
                new String[]{Long.toString(id)},
                null, null, null, null);
        cursor.moveToFirst();
        return cursor;

    }

    public int updateWord(Word word) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.COLUMN_LANGUAGE, word.getLanguage());
        contentValues.put(DatabaseHelper.WORD_COLUMN_WORD, word.getWord());
        contentValues.put(DatabaseHelper.WORD_COLUMN_ADDITIONAL_INFORMATION, word.getAdditionalInformation());
        contentValues.put(DatabaseHelper.WORD_COLUMN_ARTICLE, word.getArticle());
        contentValues.put(DatabaseHelper.WORD_COLUMN_KNOWLEDGE, word.getKnowledge());
        return getDatabase().update(DatabaseHelper.TABLE_NAME_WORD, contentValues, DatabaseHelper.COLUMN_ID + " = ?", new String[]{Long.toString(word.getId())});
    }

    public int updateTopic(Topic topic) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.COLUMN_LANGUAGE, topic.getLanguage());
        contentValues.put(DatabaseHelper.TOPIC_COLUMN_LEVEL, topic.getLevel());
        if (topic.getRoot() != null) {
            contentValues.put(DatabaseHelper.TOPIC_COLUMN_ROOT_ID, insertTopic(topic.getRoot()));
        } else {
            contentValues.put(DatabaseHelper.TOPIC_COLUMN_ROOT_ID, (String) null);
        }
        contentValues.put(DatabaseHelper.TOPIC_COLUMN_NAME, topic.getName());
        return getDatabase().update(DatabaseHelper.TABLE_NAME_TOPIC, contentValues, DatabaseHelper.COLUMN_ID + " = ?", new String[]{Long.toString(topic.getId())});
    }

    public int updateTranslation(Translation translation) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DatabaseHelper.COLUMN_LANGUAGE, translation.getLanguage());
        contentValues.put(DatabaseHelper.TRANSLATION_COLUMN_TRANSLATION, translation.getTranslation());
        return getDatabase().update(DatabaseHelper.TABLE_NAME_TRANSLATION, contentValues, DatabaseHelper.COLUMN_ID + " = ?", new String[]{Long.toString(translation.getId())});
    }

    public void deleteTranslation(long id) {
        getDatabase().delete(DatabaseHelper.TABLE_NAME_TRANSLATION, DatabaseHelper.COLUMN_ID + " = ?", new String[]{Long.toString(id)});
    }

    private SQLiteDatabase getDatabase() {
        return database;
    }

    public long insertConfigurationPreset(String name, Map<String, ?> data) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.COLUMN_ID, name);
        contentValue.put(DatabaseHelper.CONFIGURATION_PRESET_DATA, WordCriteriaProvider.serializeToBytes(data));
        return getDatabase().insert(DatabaseHelper.TABLE_NAME_CONFIGURATION_PRESET, null, contentValue);
    }

    public int updateConfigurationPreset(String name, Map<String, ?> data) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.COLUMN_ID, name);
        contentValue.put(DatabaseHelper.CONFIGURATION_PRESET_DATA, WordCriteriaProvider.serializeToBytes(data));
        return getDatabase().update(DatabaseHelper.TABLE_NAME_CONFIGURATION_PRESET, contentValue, DatabaseHelper.COLUMN_ID + " = ?", new String[]{name});
    }

    public int deleteConfigurationPreset(String name) {
        return getDatabase().delete(DatabaseHelper.TABLE_NAME_CONFIGURATION_PRESET, DatabaseHelper.COLUMN_ID + " = ?", new String[]{name});
    }

    public Map<String, ?> getConfigurationPreset(String name) {
        try (Cursor cursor = getDatabase().query(true, DatabaseHelper.TABLE_NAME_CONFIGURATION_PRESET,
                new String[]{DatabaseHelper.CONFIGURATION_PRESET_DATA},
                DatabaseHelper.COLUMN_ID + "= ?",
                new String[]{name},
                null, null, null, null)) {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                int dataIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.CONFIGURATION_PRESET_DATA);
                if (!cursor.isAfterLast()) {
                    return (Map<String, ?>) WordCriteriaProvider.deserializeBytes(cursor.getBlob(dataIndex));
                }
            }
        }
        return null;
    }

    public List<String> getConfigurationPresetNames() {
        try (Cursor cursor = getDatabase().query(true, DatabaseHelper.TABLE_NAME_CONFIGURATION_PRESET,
                new String[]{DatabaseHelper.COLUMN_ID, DatabaseHelper.CONFIGURATION_PRESET_DATA},
                null, null, null, null, null, null)) {
            List<String> result = new ArrayList<>();
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
                while (!cursor.isAfterLast()) {
                    result.add(cursor.getString(idIndex));
                    cursor.moveToNext();
                }
            }
            return result;
        }
    }

    public interface CursorProvider {
        Cursor getCursor();
    }
}