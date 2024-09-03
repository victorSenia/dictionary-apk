package org.leo.dictionary.apk.helper;

import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.entity.WordCriteria;

public class WordCriteriaProvider extends ObjectInStateProvider<WordCriteria> {
    @Override
    protected WordCriteria newObject() {
        return new WordCriteria();
    }

    @Override
    protected String lastStateName() {
        return ApkModule.LAST_STATE_WORD_CRITERIA;
    }

    @Override
    public void setObject(WordCriteria object) {
        super.setObject(object);
        if (lastState != null) {
            lastState.edit().remove(ApkModule.LAST_STATE_CURRENT_INDEX).apply();
        }
    }
}
