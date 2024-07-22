package org.leo.dictionary.apk.helper;

import android.content.SharedPreferences;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.entity.WordCriteria;

import java.io.*;
import java.util.Base64;

public class WordCriteriaProvider {
    private SharedPreferences lastState;
    private WordCriteria wordCriteria;

    public static String serialize(final Object obj) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            out.flush();
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Object deserialize(String string) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(string)); ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
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
