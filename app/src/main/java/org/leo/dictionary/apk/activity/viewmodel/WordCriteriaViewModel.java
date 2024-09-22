package org.leo.dictionary.apk.activity.viewmodel;


import org.leo.dictionary.entity.Hint;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.WordCriteria;

import java.util.List;
import java.util.Set;

public class WordCriteriaViewModel extends ObjectViewModel<WordCriteriaViewModel.WordCriteria> {

    public void triggerUpdate() {
        postValue(getValue());
    }

    public static class WordCriteria {
        private List<Float> knowledge;
        private Set<Topic> topicsOr;
        private Topic rootTopic;
        private String languageFrom;
        private Set<String> languageTo;

        public List<Float> getKnowledge() {
            return knowledge;
        }

        public void setKnowledge(List<Float> knowledge) {
            this.knowledge = knowledge;
        }

        public Set<Topic> getTopicsOr() {
            return topicsOr;
        }

        public void setTopicsOr(Set<Topic> topicsOr) {
            this.topicsOr = topicsOr;
        }

        public Topic getRootTopic() {
            return rootTopic;
        }

        public void setRootTopic(Topic rootTopic) {
            this.rootTopic = rootTopic;
        }

        public String getLanguageFrom() {
            return languageFrom;
        }

        public void setLanguageFrom(String languageFrom) {
            this.languageFrom = languageFrom;
        }

        public Set<String> getLanguageTo() {
            return languageTo;
        }

        public void setLanguageTo(Set<String> languageTo) {
            this.languageTo = languageTo;
        }
    }
}
