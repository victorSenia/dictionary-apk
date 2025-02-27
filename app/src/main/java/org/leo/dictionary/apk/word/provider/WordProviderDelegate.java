package org.leo.dictionary.apk.word.provider;

import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.word.provider.WordProvider;

import java.util.List;
import java.util.Set;

public class WordProviderDelegate implements WordProvider {
    private WordProvider delegate;

    @Override
    public List<Word> findKnownWords() {
        return delegate.findKnownWords();
    }

    @Override
    public void updateWord(Word word) {
        delegate.updateWord(word);
    }

    @Override
    public void updateWords(List<Word> words) {
        delegate.updateWords(words);
    }

    @Override
    public List<Word> findWords(WordCriteria wordCriteria) {
        return delegate.findWords(wordCriteria);
    }

    @Override
    public int countWords(WordCriteria wordCriteria) {
        return delegate.countWords(wordCriteria);
    }

    @Override
    public List<String> findTopics(String language) {
        return delegate.findTopics(language);
    }

    @Override
    public List<Topic> findTopicsWithRoot(String language, Topic rootTopic, int upToLevel) {
        return delegate.findTopicsWithRoot(language, rootTopic, upToLevel);
    }

    @Override
    public List<Topic> findTopicsWithRoot(String language, Set<Topic> rootTopics, int upToLevel) {
        return delegate.findTopicsWithRoot(language, rootTopics, upToLevel);
    }

    @Override
    public List<Topic> findTopics(String language, int level) {
        return delegate.findTopics(language, level);
    }

    @Override
    public List<String> languageFrom() {
        return delegate.languageFrom();
    }

    @Override
    public List<String> languageTo(String language) {
        return delegate.languageTo(language);
    }

    public void setWordProvider(WordProvider delegate) {
        this.delegate = delegate;
    }

    public WordProvider getDelegate() {
        return delegate;
    }
}
