package org.leo.dictionary.apk.helper;

import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.entity.SentenceCriteria;

public class GrammarCriteriaProvider extends ObjectInStateProvider<SentenceCriteria> {
    @Override
    protected SentenceCriteria newObject() {
        return new SentenceCriteria();
    }

    @Override
    protected String lastStateName() {
        return ApkModule.LAST_STATE_GRAMMAR_CRITERIA;
    }
}
