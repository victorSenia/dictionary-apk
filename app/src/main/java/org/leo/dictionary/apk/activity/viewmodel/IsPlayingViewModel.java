package org.leo.dictionary.apk.activity.viewmodel;

public class IsPlayingViewModel extends ObjectViewModel<Boolean> {
    public void setPlaying() {
        postValue(Boolean.TRUE);
    }

    public void setPaused() {
        postValue(Boolean.FALSE);
    }
}
