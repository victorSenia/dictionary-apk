package org.leo.dictionary.apk.activity;

import android.os.SystemClock;
import android.view.View;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.leo.dictionary.apk.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class VoiceSelectorUiTest {

    @Rule
    public ActivityScenarioRule<VoiceSelectorActivity> activityRule = new ActivityScenarioRule<>(VoiceSelectorActivity.class);

    @After
    public void tearDown() {
        activityRule.getScenario().onActivity(VoiceSelectorActivity::finish);
    }

    @Test
    public void languageInput_withTwoLettersShowsDefaultVoiceButton() {
        onView(withId(R.id.language)).perform(clearText(), replaceText("de"), closeSoftKeyboard());
        SystemClock.sleep(250);
        onView(withId(R.id.default_voice)).check(matches(isDisplayed()));
    }

    @Test
    public void languageInput_withMoreThanTwoLettersHidesDefaultVoiceButton() {
        onView(withId(R.id.language)).perform(clearText(), replaceText("deu"), closeSoftKeyboard());
        SystemClock.sleep(250);
        activityRule.getScenario().onActivity(activity -> {
            View defaultVoice = activity.findViewById(R.id.default_voice);
            assertEquals(View.GONE, defaultVoice.getVisibility());
        });
    }
}
