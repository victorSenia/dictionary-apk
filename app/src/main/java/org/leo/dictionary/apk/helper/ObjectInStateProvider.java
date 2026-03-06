package org.leo.dictionary.apk.helper;

import android.content.SharedPreferences;
import android.util.Log;

import java.io.Serializable;

import static org.leo.dictionary.helper.SerializeUtils.deserialize;
import static org.leo.dictionary.helper.SerializeUtils.serialize;

public abstract class ObjectInStateProvider<T extends Serializable> {
    private static final String TAG = ObjectInStateProvider.class.getSimpleName();
    protected SharedPreferences lastState;
    private T object;


    public T getObject() {
        if (object != null) {
            return object;
        }
        return newObject();
    }

    public void setObject(T object) {
        this.object = object;
        if (lastState != null) {
            if (object == null) {
                lastState.edit().remove(lastStateName()).apply();
            } else {
                try {
                    lastState.edit().putString(lastStateName(), serialize(object)).apply();
                } catch (Exception | LinkageError e) {
                    // Keep in-memory state even if persistent serialization is unavailable on runtime.
                    Log.w(TAG, "Failed to persist state " + lastStateName(), e);
                    lastState.edit().remove(lastStateName()).apply();
                }
            }
        }
    }

    protected abstract T newObject();

    protected abstract String lastStateName();

    public T getLastStateObject() {
        if (lastState != null && lastState.contains(lastStateName())) {
            try {
                return (T) deserialize(lastState.getString(lastStateName(), ""));
            } catch (Exception | LinkageError e) {
                Log.w(TAG, "Failed to restore state " + lastStateName(), e);
                lastState.edit().remove(lastStateName()).apply();
            }
        }
        return newObject();
    }

    public void setLastState(SharedPreferences lastState) {
        this.lastState = lastState;
        object = getLastStateObject();
    }
}
