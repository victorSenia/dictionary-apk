package org.leo.dictionary.apk.activity;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.leo.dictionary.entity.Word;

public class DetailsViewModel extends ViewModel {
    private final MutableLiveData<Word> uiState = new MutableLiveData<>();

    public LiveData<Word> getUiState() {
        return uiState;
    }

    public void updateWord(Word word, int index) {
        if (word != null) {
            uiState.postValue(word);
        }
    }

}