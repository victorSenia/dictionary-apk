package org.leo.dictionary.apk.activity;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.leo.dictionary.apk.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ManifestActivitiesUiSmokeTest {

    @Test
    public void mainActivity_launches() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void settingsActivity_launches() {
        try (ActivityScenario<SettingsActivity> ignored = ActivityScenario.launch(SettingsActivity.class)) {
            onView(withId(R.id.tab_layout)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void assetsActivity_launches() {
        try (ActivityScenario<AssetsActivity> ignored = ActivityScenario.launch(AssetsActivity.class)) {
            onView(withId(R.id.assets)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void exportWordsActivity_launches() {
        try (ActivityScenario<ExportWordsActivity> ignored = ActivityScenario.launch(ExportWordsActivity.class)) {
            onView(withId(R.id.find_words)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void filterWordsActivity_launches() {
        try (ActivityScenario<FilterWordsActivity> ignored = ActivityScenario.launch(FilterWordsActivity.class)) {
            onView(withId(R.id.find_words)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void parseWordsSettingsActivity_launches() {
        try (ActivityScenario<ParseWordsSettingsActivity> ignored = ActivityScenario.launch(ParseWordsSettingsActivity.class)) {
            onView(withId(R.id.parse_words)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void voiceSelectorActivity_launches() {
        try (ActivityScenario<VoiceSelectorActivity> ignored = ActivityScenario.launch(VoiceSelectorActivity.class)) {
            onView(withId(R.id.language)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void editWordActivity_launches() {
        try (ActivityScenario<EditWordActivity> ignored = ActivityScenario.launch(EditWordActivity.class)) {
            onView(withId(R.id.button_save)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void wordMatcherActivity_launches() {
        try (ActivityScenario<WordMatcherActivity> ignored = ActivityScenario.launch(WordMatcherActivity.class)) {
            onView(withId(R.id.action_next)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void configurationPresetsActivity_launches() {
        try (ActivityScenario<ConfigurationPresetsActivity> ignored = ActivityScenario.launch(ConfigurationPresetsActivity.class)) {
            onView(withId(R.id.text_preset_name)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void grammarFilterActivity_launches() {
        try (ActivityScenario<GrammarFilterActivity> ignored = ActivityScenario.launch(GrammarFilterActivity.class)) {
            onView(withId(R.id.button_practice)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void grammarLearningActivity_launches() {
        try (ActivityScenario<GrammarLearningActivity> ignored = ActivityScenario.launch(GrammarLearningActivity.class)) {
            onView(withId(R.id.sentences)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void grammarCheckActivity_launches() {
        try (ActivityScenario<GrammarCheckActivity> ignored = ActivityScenario.launch(GrammarCheckActivity.class)) {
            onView(withId(R.id.next)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void sentenceActivity_launches() {
        try (ActivityScenario<SentenceActivity> ignored = ActivityScenario.launch(SentenceActivity.class)) {
            onView(withId(R.id.button_next)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void matchArticleActivity_launches() {
        try (ActivityScenario<MatchArticleActivity> ignored = ActivityScenario.launch(MatchArticleActivity.class)) {
            onView(withId(R.id.button_next)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void speechRecognitionActivity_launches() {
        try (ActivityScenario<SpeechRecognitionActivity> ignored = ActivityScenario.launch(SpeechRecognitionActivity.class)) {
            onView(withId(R.id.button_speak)).check(matches(isDisplayed()));
        }
    }
}
