package org.leo.dictionary.apk.helper;

import org.leo.dictionary.entity.Sentence;
import org.leo.dictionary.entity.SentenceCriteria;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.grammar.provider.SentenceProvider;

import java.util.List;

public class SentenceProviderHolder implements SentenceProvider {
    private SentenceProvider sentenceProvider;

    @Override
    public List<Sentence> findSentences(SentenceCriteria sentenceCriteria) {
        return sentenceProvider.findSentences(sentenceCriteria);
    }

    @Override
    public List<Topic> findTopics(String language, String rootTopic, int level) {
        return sentenceProvider.findTopics(language, rootTopic, level);
    }

    @Override
    public List<String> languages() {
        return sentenceProvider.languages();
    }

    public void setSentenceProvider(SentenceProvider provider) {
        sentenceProvider = provider;
    }
}
