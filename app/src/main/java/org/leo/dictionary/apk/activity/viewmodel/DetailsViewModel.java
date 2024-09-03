package org.leo.dictionary.apk.activity.viewmodel;

import org.leo.dictionary.entity.Word;

public class DetailsViewModel extends ObjectViewModel<Word> {

    public void updateWord(Word word, int index) {
        if (word != null) {
            postValue(word);
        }
    }

    public void clearWord() {
        postValue(null);
    }

}