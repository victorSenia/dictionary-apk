package org.leo.dictionary.apk.word.provider;

import org.leo.dictionary.apk.helper.DBManager;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.word.provider.WordProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DBWordProvider implements WordProvider {

    private final static Logger LOGGER = Logger.getLogger(DBWordProvider.class.getName());
    private DBManager dbManager;

    private static Translation findTranslationById(Word word, long id) {
        for (Translation translation : word.getTranslations()) {
            if (translation.getId() == id) {
                return translation;
            }
        }
        return null;
    }

    private static Topic findTopicById(Word word, long id) {
        for (Topic topic : word.getTopics()) {
            if (topic.getId() == id) {
                return topic;
            }
        }
        return null;
    }

    @Override
    public List<Word> findWords(WordCriteria wordCriteria) {
        return dbManager.getWords(wordCriteria);
    }

    @Override
    public List<Topic> findTopics(String language, int level) {
        return dbManager.getTopics(language, null, Integer.toString(level));
    }

    @Override
    public List<Topic> findTopicsWithRoot(String language, String rootName, int level) {
        return dbManager.getTopics(language, rootName, Integer.toString(level));
    }

    public List<Topic> findRootTopics(String language) {
        return dbManager.findRootTopics(language);
    }

    @Override
    public List<String> languageFrom() {
        return dbManager.languageFrom();
    }

    @Override
    public List<String> languageTo(String language) {
        return dbManager.languageTo(language);
    }

    @Override
    public List<Word> findKnownWords() {
        return new ArrayList<>();
    }

    @Override
    public void updateWord(Word updatedWord) {
        dbManager.updateWord(updatedWord);
    }

    public void updateTopic(Topic topic) {
        dbManager.updateTopic(topic);
    }

    public void updateWordFully(Word updatedWord) {
        if (updatedWord.getId() == 0) {
            dbManager.executeInTransaction(() -> dbManager.insertFully(updatedWord));
        } else {
            Word oldWord = dbManager.findWord(updatedWord.getId());
            if (oldWord != null) {
                dbManager.executeInTransaction(() -> updateWord(updatedWord, oldWord));
            } else {
                dbManager.executeInTransaction(() -> dbManager.insertFully(updatedWord));
            }
        }
    }

    private Word updateWord(Word updatedWord, Word oldWord) {
        if (!oldWord.equals(updatedWord)) {
            dbManager.updateWord(updatedWord);
        }
        for (Translation translation : updatedWord.getTranslations()) {
            if (translation.getId() == 0) {
                dbManager.insertTranslation(translation, updatedWord.getId());
            } else if (!translation.equals(findTranslationById(oldWord, translation.getId()))) {
                dbManager.updateTranslation(translation);
            }
        }
        for (Translation translation : oldWord.getTranslations()) {
            if (findTranslationById(updatedWord, translation.getId()) == null) {
                dbManager.deleteTranslation(translation.getId());
            }
        }
        for (Topic topic : updatedWord.getTopics()) {
            if (topic.getId() == 0) {
                dbManager.insertWordTopicLink(updatedWord.getId(), dbManager.insertTopic(topic));
            } else if (findTranslationById(oldWord, topic.getId()) == null) {
                dbManager.insertWordTopicLink(updatedWord.getId(), topic.getId());
            }
        }
        for (Topic topic : oldWord.getTopics()) {
            if (findTopicById(updatedWord, topic.getId()) == null) {
                dbManager.deleteWordTopicLink(updatedWord.getId(), topic.getId());
            }
        }
        return updatedWord;
    }

    public void importWords(List<Word> words) {
        long start = System.currentTimeMillis();
        int chunkSize = 1000;
        importWordsInChunks(words, chunkSize);
        LOGGER.info("Save " + words.size() + " words took " + (System.currentTimeMillis() - start) + " ms");
    }

    private void importWordsInChunks(List<Word> words, int chunkSize) {
        long startPart = System.currentTimeMillis();
        for (int chunkStart = 0; chunkStart < words.size(); chunkStart += chunkSize) {
            int start = chunkStart;
            int end = Math.min(words.size(), start + chunkSize);
            dbManager.executeInTransaction(() -> importWordsIntoDatabase(words.subList(start, end)));
            LOGGER.info("Save " + end + " words took " + (System.currentTimeMillis() - startPart) + " ms");
            startPart = System.currentTimeMillis();
        }
    }

    private List<Word> importWordsIntoDatabase(List<Word> words) {
        for (Word word : words) {
            dbManager.insertFully(word);
        }
        return words;
    }

    public Word findWord(long id) {
        return dbManager.findWord(id);
    }

    public List<Word> getWordsForLanguage(String language, String rootTopic) {
        return dbManager.getWordsForLanguage(language, rootTopic);
    }

    public void deleteWords(String language) {
        long start = System.currentTimeMillis();
        int deleted = dbManager.executeInTransaction(() -> dbManager.deleteForLanguage(language));
        dbManager.vacuum();
        LOGGER.info("Delete " + deleted + " words took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void deleteWord(long id) {
        dbManager.executeInTransaction(() -> dbManager.deleteWord(id));
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

}
