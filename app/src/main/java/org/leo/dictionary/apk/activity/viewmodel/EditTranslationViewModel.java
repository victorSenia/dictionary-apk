package org.leo.dictionary.apk.activity.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.leo.dictionary.entity.Translation;

public class EditTranslationViewModel extends ViewModel {
    private final MutableLiveData<Translation> data = new MutableLiveData<>(new Translation());

    public MutableLiveData<Translation> getData() {
        return data;
    }

    public Translation getTranslation() {
        return data.getValue();
    }

    public void setTranslation(Translation translation) {
        data.setValue(translation);
    }
}
