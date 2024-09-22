package org.leo.dictionary.apk.word.provider;

import org.leo.dictionary.apk.helper.DatabaseManager;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.word.provider.WordProvider;

import java.util.*;
import java.util.logging.Logger;

public class DBWordProvider implements WordProvider {

    private final static Logger LOGGER = Logger.getLogger(DBWordProvider.class.getName());
    private DatabaseManager databaseManager;

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
        return databaseManager.getWords(wordCriteria);
    }

    @Override
    public List<Topic> findTopics(String language, int level) {
        return databaseManager.getTopics(language, null, Integer.toString(level));
    }

    @Override
    public List<Topic> findTopicsWithRoot(String language, String rootName, int level) {
        return databaseManager.getTopics(language, rootName, Integer.toString(level));
    }

    public List<Topic> findRootTopics(String language) {
        return databaseManager.findRootTopics(language);
    }

    @Override
    public List<String> languageFrom() {
        return databaseManager.languageFrom();
    }

    @Override
    public List<String> languageTo(String language) {
        return databaseManager.languageTo(language);
    }

    @Override
    public List<Word> findKnownWords() {
        return new ArrayList<>();
    }

    @Override
    public void updateWord(Word updatedWord) {
        updateWord(Collections.singletonList(updatedWord));
    }

    public void updateWord(Collection<Word> words) {
        databaseManager.executeInTransaction(() -> {
                    for (Word updatedWord : words) {
                        databaseManager.updateWord(updatedWord);
                    }
                    return words.size();
                }
        );
    }

    public void updateTopic(Topic topic) {
        databaseManager.updateTopic(topic);
    }

    public void updateWordFully(Word updatedWord) {
        if (updatedWord.getId() == 0) {
            databaseManager.executeInTransaction(() -> databaseManager.insertFully(updatedWord));
        } else {
            Word oldWord = databaseManager.findWord(updatedWord.getId());
            if (oldWord != null) {
                databaseManager.executeInTransaction(() -> updateWord(updatedWord, oldWord));
            } else {
                databaseManager.executeInTransaction(() -> databaseManager.insertFully(updatedWord));
            }
        }
    }

    private Word updateWord(Word updatedWord, Word oldWord) {
        if (!oldWord.equals(updatedWord)) {
            databaseManager.updateWord(updatedWord);
        }
        for (Translation translation : updatedWord.getTranslations()) {
            if (translation.getId() == 0) {
                databaseManager.insertTranslation(translation, updatedWord.getId());
            } else if (!translation.equals(findTranslationById(oldWord, translation.getId()))) {
                databaseManager.updateTranslation(translation);
            }
        }
        for (Translation translation : oldWord.getTranslations()) {
            if (findTranslationById(updatedWord, translation.getId()) == null) {
                databaseManager.deleteTranslation(translation.getId());
            }
        }
        for (Topic topic : updatedWord.getTopics()) {
            if (topic.getId() == 0) {
                databaseManager.insertWordTopicLink(updatedWord.getId(), databaseManager.insertTopic(topic));
            } else if (findTranslationById(oldWord, topic.getId()) == null) {
                databaseManager.insertWordTopicLink(updatedWord.getId(), topic.getId());
            }
        }
        for (Topic topic : oldWord.getTopics()) {
            if (findTopicById(updatedWord, topic.getId()) == null) {
                databaseManager.deleteWordTopicLink(updatedWord.getId(), topic.getId());
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
            databaseManager.executeInTransaction(() -> importWordsIntoDatabase(words.subList(start, end)));
            LOGGER.info("Save " + end + " words took " + (System.currentTimeMillis() - startPart) + " ms");
            startPart = System.currentTimeMillis();
        }
    }

    private List<Word> importWordsIntoDatabase(List<Word> words) {
        for (Word word : words) {
            databaseManager.insertFully(word);
        }
        return words;
    }

    public Word findWord(long id) {
        return databaseManager.findWord(id);
    }

    @Override
    public int countWords(WordCriteria wordCriteria) {
        return databaseManager.countWords(wordCriteria);
    }

    public List<Word> getWordsForLanguage(String language, String rootTopic) {
        return databaseManager.getWordsForLanguage(language, rootTopic);
    }

    public void deleteWords(String language) {
        long start = System.currentTimeMillis();
        int deleted = databaseManager.executeInTransaction(() -> databaseManager.deleteForLanguage(language));
        databaseManager.vacuum();
        LOGGER.info("Delete " + deleted + " words took " + (System.currentTimeMillis() - start) + " ms");
    }

    public void deleteWord(long id) {
        databaseManager.executeInTransaction(() -> databaseManager.deleteWord(id));
    }

    public void insertConfigurationPreset(String name, Map<String, ?> data) {
        databaseManager.executeInTransaction(() -> databaseManager.insertConfigurationPreset(name, data));
    }

    public void updateConfigurationPreset(String name, Map<String, ?> data) {
        databaseManager.executeInTransaction(() -> databaseManager.updateConfigurationPreset(name, data));
    }

    public void deleteConfigurationPreset(String name) {
        databaseManager.executeInTransaction(() -> databaseManager.deleteConfigurationPreset(name));
    }

    public List<String> getConfigurationPresetNames() {
        return databaseManager.getConfigurationPresetNames();
    }

    public Map<String, ?> getConfigurationPreset(String name) {
        return databaseManager.getConfigurationPreset(name);
    }

    public void setDbManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

}
