package org.leo.dictionary.apk.activity.viewmodel;

import org.leo.dictionary.entity.Word;

public class EditWordViewModel extends ObjectViewModel<Word> {

    public void setNewObject() {
        setValue(new Word());
    }
}