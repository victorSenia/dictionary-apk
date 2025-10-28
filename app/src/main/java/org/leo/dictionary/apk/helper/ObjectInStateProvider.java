package org.leo.dictionary.apk.helper;

import android.content.SharedPreferences;

import java.io.Serializable;

import static org.leo.dictionary.helper.SerializeUtils.deserialize;
import static org.leo.dictionary.helper.SerializeUtils.serialize;

public abstract class ObjectInStateProvider<T extends Serializable> {
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
                lastState.edit().putString(lastStateName(), serialize(object)).apply();
            }
        }
    }

    protected abstract T newObject();

    protected abstract String lastStateName();

    public T getLastStateObject() {
        if (lastState != null && lastState.contains(lastStateName())) {
            return (T) deserialize(lastState.getString(lastStateName(), ""));
        }
        return newObject();
    }

    public void setLastState(SharedPreferences lastState) {
        this.lastState = lastState;
        object = getLastStateObject();
    }
}
