package org.leo.dictionary.apk.activity.viewmodel;


import org.leo.dictionary.entity.Hint;
import org.leo.dictionary.entity.Topic;

import java.util.Set;

public class SentenceCriteriaViewModel extends ObjectViewModel<SentenceCriteriaViewModel.SentenceCriteria> {

    public static class SentenceCriteria {
        private String language;
        private Topic rootTopic;
        private Set<Hint> hints;
        private Set<Topic> topicsOr;

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public Topic getRootTopic() {
            return rootTopic;
        }

        public void setRootTopic(Topic rootTopic) {
            this.rootTopic = rootTopic;
        }

        public Set<Hint> getHints() {
            return hints;
        }

        public void setHints(Set<Hint> hints) {
            this.hints = hints;
        }

        public Set<Topic> getTopicsOr() {
            return topicsOr;
        }

        public void setTopicsOr(Set<Topic> topicsOr) {
            this.topicsOr = topicsOr;
        }
    }
}
