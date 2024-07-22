package org.leo.dictionary.apk.activity.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.leo.dictionary.entity.Topic;

public class TopicViewModel extends ViewModel {
    private final MutableLiveData<Topic> data = new MutableLiveData<>();

    public Topic getTopic() {
        return data.getValue();
    }

    public void setTopic(Topic item) {
        data.postValue(item);
    }

    public LiveData<Topic> getData() {
        return data;
    }
}
