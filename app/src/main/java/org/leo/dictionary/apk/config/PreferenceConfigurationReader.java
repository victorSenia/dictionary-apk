package org.leo.dictionary.apk.config;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import org.leo.dictionary.config.FileConfigurationReader;
import org.leo.dictionary.config.entity.Configuration;

import java.util.HashMap;
import java.util.Map;

public class PreferenceConfigurationReader extends FileConfigurationReader {
    private Context context;
    private SharedPreferences.OnSharedPreferenceChangeListener changeListener;

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void updateConfiguration(Configuration configuration) {
    }

    @Override
    public Map<Object, Object> readConfig(String path) {
        HashMap<Object, Object> properties = new HashMap<>(PreferenceManager.getDefaultSharedPreferences(context).getAll());
        changeListener = new SharedPreferencesProperties(properties);
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(changeListener);
        return properties;
    }

    @Override
    public boolean isExists(String path) {
        return true;
    }

    public static class SharedPreferencesProperties implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final HashMap<Object, Object> properties;

        public SharedPreferencesProperties(HashMap<Object, Object> properties) {
            this.properties = properties;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
            properties.put(key, sharedPreferences.getAll().get(key));
        }
    }
}
