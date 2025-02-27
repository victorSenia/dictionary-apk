package org.leo.dictionary.apk.helper;

import org.leo.dictionary.entity.GrammarCriteria;
import org.leo.dictionary.entity.GrammarSentence;
import org.leo.dictionary.entity.Hint;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.grammar.provider.GrammarProvider;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GrammarProviderHolder implements GrammarProvider {
    private GrammarProvider grammarProvider;

    public void setGrammarProvider(GrammarProvider grammarProvider) {
        this.grammarProvider = grammarProvider;
    }

    @Override
    public List<GrammarSentence> findSentences(GrammarCriteria criteria) {
        return grammarProvider.findSentences(criteria);
    }

    @Override
    public List<Hint> findHints(String language, Topic rootTopic, Set<Topic> topics) {
        return grammarProvider.findHints(language, rootTopic, topics);
    }

    @Override
    public List<Hint> findHints(String language, Set<Topic> rootTopic, Set<Topic> topics) {
        return grammarProvider.findHints(language, rootTopic, topics);
    }

    @Override
    public List<Topic> findTopics(String language, Topic rootTopic, int level) {
        return grammarProvider.findTopics(language, rootTopic, level);
    }

    @Override
    public List<Topic> findTopics(String language, Set<Topic> rootTopic, int level) {
        return grammarProvider.findTopics(language, rootTopic, level);
    }

    @Override
    public List<String> languages() {
        return grammarProvider.languages();
    }
}
