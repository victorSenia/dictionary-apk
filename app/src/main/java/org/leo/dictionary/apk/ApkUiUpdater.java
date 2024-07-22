package org.leo.dictionary.apk;

import android.content.SharedPreferences;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.entity.Word;

import java.util.WeakHashMap;

public class ApkUiUpdater implements UiUpdater {
    private final WeakHashMap<UiUpdater, Object> updaterWeakReference = new WeakHashMap<>();
    private SharedPreferences lastState;

    @Override
    public void updateState(Word word, int index) {
        lastState.edit().putInt(ApkModule.LAST_STATE_CURRENT_INDEX, index).apply();
        updaterWeakReference.keySet().forEach(uiUpdater -> uiUpdater.updateState(word, index));
    }

    public void addUiUpdater(UiUpdater uiUpdater) {
        updaterWeakReference.put(uiUpdater, new Object());
    }

    public void removeUiUpdater(UiUpdater uiUpdater) {
        updaterWeakReference.remove(uiUpdater);
    }

    public void setLastState(SharedPreferences lastState) {
        this.lastState = lastState;
    }
}
