package org.leo.dictionary.apk.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LanguageViewModel extends ViewModel {
    private final MutableLiveData<String> data = new MutableLiveData<>();

    public LiveData<String> getData() {
        return data;
    }

    public String getSelected() {
        return data.getValue();
    }

    public void setSelected(String item) {
        data.setValue(item);
    }

    public void setSelected(CharSequence s) {
        data.setValue(s.toString());
    }
}
