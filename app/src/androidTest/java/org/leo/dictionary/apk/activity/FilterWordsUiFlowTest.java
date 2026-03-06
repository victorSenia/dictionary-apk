package org.leo.dictionary.apk.activity;

import android.os.ParcelFileDescriptor;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.leo.dictionary.apk.R;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class FilterWordsUiFlowTest {

    @Rule
    public ActivityScenarioRule<FilterWordsActivity> activityRule = new ActivityScenarioRule<>(FilterWordsActivity.class);

    @Before
    public void setUp() {
        disableAnimations();
    }

    @Test
    public void wordsOrderMode_canSwitchBetweenImportShuffleAndSorted() {
        clickByIdOnUiThread(R.id.import_order);
        assertCheckedRadio(R.id.import_order);
        clickByIdOnUiThread(R.id.shuffle);
        assertCheckedRadio(R.id.shuffle);
        clickByIdOnUiThread(R.id.sorted);
        assertCheckedRadio(R.id.sorted);
    }

    @Test
    public void clearTopicButtonsAndFindWords_submitFlowWorks() {
        clickIfDisplayed(R.id.all_root_topics);
        clickIfDisplayed(R.id.all_topics);
        onView(withId(R.id.find_words)).check(matches(isDisplayed()));
        clickByIdOnUiThread(R.id.find_words);
    }

    private void clickIfDisplayed(int viewId) {
        if (isDisplayedInActivity(viewId)) {
            clickByIdOnUiThread(viewId);
        }
    }

    private boolean isDisplayedInActivity(int viewId) {
        AtomicBoolean displayed = new AtomicBoolean(false);
        activityRule.getScenario().onActivity(activity -> {
            android.view.View v = activity.findViewById(viewId);
            displayed.set(v != null && v.getVisibility() == android.view.View.VISIBLE);
        });
        return displayed.get();
    }

    private void clickByIdOnUiThread(int viewId) {
        activityRule.getScenario().onActivity(activity -> {
            android.view.View v = activity.findViewById(viewId);
            if (v != null) {
                v.performClick();
            }
        });
    }

    private void assertCheckedRadio(int expectedCheckedId) {
        AtomicInteger checkedId = new AtomicInteger(-1);
        activityRule.getScenario().onActivity(activity -> {
            android.widget.RadioGroup group = activity.findViewById(R.id.words_order_group);
            checkedId.set(group.getCheckedRadioButtonId());
        });
        assertEquals(expectedCheckedId, checkedId.get());
    }

    private void disableAnimations() {
        executeShellSetting("settings put global window_animation_scale 0");
        executeShellSetting("settings put global transition_animation_scale 0");
        executeShellSetting("settings put global animator_duration_scale 0");
    }

    private void executeShellSetting(String command) {
        try (ParcelFileDescriptor ignored = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command)) {
            // no-op
        } catch (IOException ignored) {
            // Best effort: if shell command is blocked, continue without failing test setup.
        }
    }
}
