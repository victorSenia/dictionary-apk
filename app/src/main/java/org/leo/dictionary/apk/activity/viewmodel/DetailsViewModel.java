package org.leo.dictionary.apk.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.leo.dictionary.entity.Word;

public class DetailsViewModel extends ViewModel {
    private final MutableLiveData<Word> data = new MutableLiveData<>();

    public LiveData<Word> getData() {
        return data;
    }

    public void updateWord(Word word, int index) {
        if (word != null) {
            data.postValue(word);
        }
    }

    public void clearWord() {
        data.postValue(null);
    }

    public Word getWord() {
        return data.getValue();
    }

}