package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.text.InputType;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import org.leo.dictionary.apk.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            setInputTypeNumber("org.leo.dictionary.config.entity.General.delayBefore");
            setInputTypeNumber("org.leo.dictionary.config.entity.General.delayAfter");
            setInputTypeNumber("org.leo.dictionary.config.entity.Repeat.delay");
            setInputTypeNumber("org.leo.dictionary.config.entity.Repeat.times");
            setInputTypeNumber("org.leo.dictionary.config.entity.Translation.delay");
            setInputTypeNumber("org.leo.dictionary.config.entity.Spelling.delay");
            setInputTypeNumber("org.leo.dictionary.config.entity.Spelling.letterDelay");
            setInputTypeNumber("org.leo.dictionary.config.entity.General.delayPerLetterAfter");
            setInputTypeNumber("org.leo.dictionary.config.entity.General.knowledgeIncrease");
            setInputTypeNumber("org.leo.dictionary.apk.config.entity.Speech.speed");
            setInputTypeNumber("org.leo.dictionary.apk.config.entity.Speech.pitch");
        }

        private void setInputTypeNumber(String key) {
            EditTextPreference editTextPreference = findPreference(key);
            if (editTextPreference != null) {
                editTextPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            }
        }
    }
}