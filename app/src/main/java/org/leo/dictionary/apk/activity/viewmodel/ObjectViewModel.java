package org.leo.dictionary.apk.activity.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public abstract class ObjectViewModel<T> extends ViewModel {
    private final MutableLiveData<T> data = new MutableLiveData<>();

    public MutableLiveData<T> getData() {
        return data;
    }

    public T getValue() {
        return data.getValue();
    }

    public void setValue(T object) {
        data.setValue(object);
    }

    public void postValue(T object) {
        data.postValue(object);
    }
}
