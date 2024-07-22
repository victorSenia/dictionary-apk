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
        return dbManager.getTopics(language, Integer.toString(level));
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

    public void updateWordFully(Word updatedWord) {
        if (updatedWord.getId() == 0) {
            dbManager.insertFully(updatedWord);
        } else {
            Word oldWord = dbManager.findWord(updatedWord.getId());
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
        }
    }

    public void importWords(List<Word> words) {
        int i = 0;
        long start = System.currentTimeMillis();
        long startPart = start;
        int partSize = 100;
        for (Word word : words) {
            i++;
            dbManager.insertFully(word);
            if (i % partSize == 0) {
                LOGGER.info("Save " + partSize + " words took " + (System.currentTimeMillis() - startPart) + " ms");
                startPart = System.currentTimeMillis();
            }
        }
        LOGGER.info("Save " + words.size() + " words took " + (System.currentTimeMillis() - start) + " ms");
    }

    public Word findWord(long id) {
        return dbManager.findWord(id);
    }

    public List<Word> getWordsForLanguage(String language) {
        return dbManager.getWordsForLanguage(language);
    }

    public void deleteWords(String language) {
        long start = System.currentTimeMillis();
        int deleted = dbManager.deleteForLanguage(language);
        LOGGER.info("Delete " + deleted + " words took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void deleteWord(long id) {
        dbManager.deleteWord(id);
    }

    public void setDbManager(DBManager dbManager) {
        this.dbManager = dbManager;
    }

}
