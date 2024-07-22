package org.leo.dictionary.apk.activity;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.leo.dictionary.entity.Word;

public class EditWordViewModel extends ViewModel {
    private final MutableLiveData<Word> uiState = new MutableLiveData<>(new Word());

    public MutableLiveData<Word> getUiState() {
        return uiState;
    }

    public void setWord(Word word) {
        uiState.setValue(word);
    }

    public void setNewWord() {
        uiState.setValue(new Word());
    }
}