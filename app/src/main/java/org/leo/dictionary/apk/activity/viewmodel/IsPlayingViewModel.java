package org.leo.dictionary.apk.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class IsPlayingViewModel extends ViewModel {
    private final MutableLiveData<Boolean> data = new MutableLiveData<>(Boolean.FALSE);

    public LiveData<Boolean> getData() {
        return data;
    }

    public void setPlaying() {
        data.postValue(true);
    }

    public void setPaused() {
        data.postValue(false);
    }
}
