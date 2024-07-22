package org.leo.dictionary.apk.activity.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.leo.dictionary.entity.Word;

public class EditWordViewModel extends ViewModel {
    private final MutableLiveData<Word> data = new MutableLiveData<>(new Word());

    public MutableLiveData<Word> getData() {
        return data;
    }

    public Word getValue() {
        return data.getValue();
    }

    public void setWord(Word word) {
        data.setValue(word);
    }

    public void setNewWord() {
        data.setValue(new Word());
    }
}