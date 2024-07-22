package org.leo.dictionary.apk;

import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.entity.Word;

import java.util.WeakHashMap;

public class ApkUiUpdater implements UiUpdater {
    private final WeakHashMap<UiUpdater, Object> updaterWeakReference = new WeakHashMap<>();

    @Override
    public void updateState(Word word) {
        updaterWeakReference.keySet().forEach(uiUpdater -> uiUpdater.updateState(word));
    }

    public void addUiUpdater(UiUpdater uiUpdater) {
        updaterWeakReference.put(uiUpdater, new Object());
    }

    public void removeUiUpdater(UiUpdater uiUpdater) {
        updaterWeakReference.remove(uiUpdater);
    }
}
