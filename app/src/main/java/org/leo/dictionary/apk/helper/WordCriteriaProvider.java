package org.leo.dictionary.apk.helper;

import android.content.SharedPreferences;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.activity.MainActivity;
import org.leo.dictionary.entity.WordCriteria;

import java.io.*;
import java.util.Base64;

public class WordCriteriaProvider {
    private SharedPreferences lastState;
    private WordCriteria wordCriteria;

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

    public WordCriteria getWordCriteria() {
        if (wordCriteria != null) {
            return wordCriteria;
        }
        return new WordCriteria();
    }

    public void setWordCriteria(WordCriteria criteria) {
        wordCriteria = criteria;
        if (lastState != null) {
            lastState.edit().remove(ApkModule.LAST_STATE_CURRENT_INDEX).apply();
            if (criteria == null) {
                lastState.edit().remove(ApkModule.LAST_STATE_WORD_CRITERIA).apply();
            } else {
                lastState.edit().putString(ApkModule.LAST_STATE_WORD_CRITERIA, serialize(criteria)).apply();
            }
        }
    }

    public WordCriteria getLastStateWordCriteria() {
        if (lastState != null && lastState.contains(ApkModule.LAST_STATE_WORD_CRITERIA)) {
            return (WordCriteria) deserialize(lastState.getString(ApkModule.LAST_STATE_WORD_CRITERIA, ""));
        }
        return null;
    }

    public void setLastState(SharedPreferences lastState) {
        this.lastState = lastState;
        wordCriteria = getLastStateWordCriteria();
    }
}
