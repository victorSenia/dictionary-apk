package org.leo.dictionary.apk.helper;

import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.entity.GrammarCriteria;

public class GrammarCriteriaProvider extends ObjectInStateProvider<GrammarCriteria> {
    @Override
    protected GrammarCriteria newObject() {
        return new GrammarCriteria();
    }

    @Override
    protected String lastStateName() {
        return ApkModule.LAST_STATE_GRAMMAR_CRITERIA;
    }
}
