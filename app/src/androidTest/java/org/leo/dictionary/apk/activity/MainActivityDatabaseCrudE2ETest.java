package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.view.View;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.os.ParcelFileDescriptor;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.leo.dictionary.apk.R;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class MainActivityDatabaseCrudE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        disableAnimations();
    }

    @Test
    public void databaseCrud_createEditTopicsAndPlayFlows() {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toLowerCase(Locale.ROOT);
        String wordName = "uitest_word_" + suffix;
        String topicName = "uitest_topic_" + suffix;

        switchToDatabaseSource();
        cleanFiltersIfVisible();

        openOverflowAndClick(R.string.add_word);
        onView(withId(R.id.text_language)).perform(replaceText("de"), closeSoftKeyboard());
        onView(withId(R.id.text_article)).perform(replaceText("der"), closeSoftKeyboard());
        onView(withId(R.id.text_word)).perform(replaceText(wordName), closeSoftKeyboard());
        onView(withId(R.id.button_add_translation)).perform(click());
        onView(allOf(withId(R.id.text_language), isDescendantOfA(withId(R.id.edit_word_translations))))
                .perform(replaceText("en"), closeSoftKeyboard());
        onView(withId(R.id.text_translation)).perform(replaceText("translation_" + suffix), closeSoftKeyboard());

        onView(withId(R.id.text_topic)).perform(replaceText(topicName), closeSoftKeyboard());
        onView(withId(R.id.create_topic)).perform(click());

        onView(withId(R.id.play_word)).perform(click());
        onView(withId(R.id.play_translation)).perform(click());
        onView(withId(R.id.button_save)).perform(click());
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
    }

    private void switchToDatabaseSource() {
        Context context = ApplicationProvider.getApplicationContext();
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(R.string.database)).perform(click());
        onView(withText(R.string.use_db)).perform(click());
        onView(withId(R.id.find_words)).check(matches(isDisplayed())).perform(click());
    }

    private void cleanFiltersIfVisible() {
        openOverflowAndClick(R.string.find_words);
        clickIfVisible(R.id.all_root_topics);
        clickIfVisible(R.id.all_topics);
        onView(withId(R.id.find_words)).perform(click());
    }

    private void clickIfVisible(int viewId) {
        if (isVisible(viewId)) {
            onView(withId(viewId)).perform(click());
        }
    }

    private boolean isVisible(int viewId) {
        AtomicBoolean visible = new AtomicBoolean(false);
        activityRule.getScenario().onActivity(activity -> {
            View view = activity.findViewById(viewId);
            visible.set(view != null && view.getVisibility() == View.VISIBLE);
        });
        return visible.get();
    }

    private void openOverflowAndClick(int menuTextResId) {
        Context context = ApplicationProvider.getApplicationContext();
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(menuTextResId)).perform(click());
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
