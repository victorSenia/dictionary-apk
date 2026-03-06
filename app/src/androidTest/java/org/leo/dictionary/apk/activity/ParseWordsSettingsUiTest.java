package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.leo.dictionary.apk.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ParseWordsSettingsUiTest {

    @Rule
    public ActivityScenarioRule<ParseWordsSettingsActivity> activityRule = new ActivityScenarioRule<>(ParseWordsSettingsActivity.class);

    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void assetButton_opensAssetsActivity() {
        onView(withId(R.id.asset)).check(matches(isDisplayed())).perform(click());
        intended(hasComponent(new Intent(context, AssetsActivity.class).getComponent()));
    }

    @Test
    public void fileButton_opensDocumentPickerChooser() {
        onView(withId(R.id.file)).check(matches(isDisplayed())).perform(click());
        intended(hasAction(Intent.ACTION_CHOOSER));
    }

    @Test
    public void parseWords_withoutSelectingSource_staysOnScreen() {
        onView(withId(R.id.parse_words)).perform(click());
        onView(withId(R.id.parse_words)).check(matches(isDisplayed()));
    }
}
