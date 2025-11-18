package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import org.leo.dictionary.apk.R;

public class SettingsActivity extends AppCompatActivity {
    private final int[] tabNames = {R.string.general, R.string.speech, R.string.match_words};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        View root = findViewById(android.R.id.content);
        ActivityUtils.setFullScreen(this, root);
        ScreenSlidePagerAdapter screenSlidePagerAdapter = new ScreenSlidePagerAdapter(this);
        ViewPager2 viewPager = findViewById(R.id.pager);
        viewPager.setAdapter(screenSlidePagerAdapter);
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(getText(tabNames[position]))).attach();
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public @NonNull Fragment createFragment(int position) {
            if (position == 0) {
                return new GeneralCustomPreferencesFragment();
            }
            if (position == 1) {
                return new VoiceCustomPreferencesFragment();
            }
            return new MatchWordsCustomPreferencesFragment();
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    public static abstract class CustomPreferencesFragment extends PreferenceFragmentCompat {
        protected void setInputTypeNumber(String key) {
            EditTextPreference editTextPreference = findPreference(key);
            if (editTextPreference != null) {
                editTextPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));
            }
        }
    }

    public static class GeneralCustomPreferencesFragment extends CustomPreferencesFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.general_preferences, rootKey);
            setInputTypeNumber("org.leo.dictionary.config.entity.General.delayBefore");
            setInputTypeNumber("org.leo.dictionary.config.entity.General.delayAfter");
            setInputTypeNumber("org.leo.dictionary.config.entity.Repeat.delay");
            setInputTypeNumber("org.leo.dictionary.config.entity.Repeat.times");
            setInputTypeNumber("org.leo.dictionary.config.entity.Translation.delay");
            setInputTypeNumber("org.leo.dictionary.config.entity.Spelling.delay");
            setInputTypeNumber("org.leo.dictionary.config.entity.Spelling.letterDelay");
            setInputTypeNumber("org.leo.dictionary.config.entity.General.delayPerLetterAfter");
            setInputTypeNumber("org.leo.dictionary.config.entity.General.knowledgeIncrease");
        }
    }

    public static class VoiceCustomPreferencesFragment extends CustomPreferencesFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.voice_preferences, rootKey);
            setInputTypeNumber("org.leo.dictionary.apk.config.entity.Speech.speed");
            setInputTypeNumber("org.leo.dictionary.apk.config.entity.Speech.pitch");
        }
    }

    public static class MatchWordsCustomPreferencesFragment extends CustomPreferencesFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.match_words_preferences, rootKey);
            setInputTypeNumber("org.leo.dictionary.apk.config.entity.MatchWords.limit");
        }
    }
}