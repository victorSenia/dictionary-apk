package org.leo.dictionary.apk.helper;

import android.content.SharedPreferences;
import org.leo.dictionary.apk.activity.MainActivity;

import java.io.*;
import java.util.Base64;

public abstract class ObjectInStateProvider<T extends Serializable> {
    protected SharedPreferences lastState;
    private T object;

    public static String serialize(final Object obj) {
        byte[] bytes = serializeToBytes(obj);
        if (bytes != null) {
            return Base64.getEncoder().encodeToString(bytes);
        }
        return "";
    }

    public static byte[] serializeToBytes(final Object obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            MainActivity.logUnhandledException(e);
            return null;
        }
    }

    public static Object deserialize(String string) {
        return deserializeBytes(Base64.getDecoder().decode(string));
    }

    public static Object deserializeBytes(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            MainActivity.logUnhandledException(e);
        }
        return null;
    }

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
