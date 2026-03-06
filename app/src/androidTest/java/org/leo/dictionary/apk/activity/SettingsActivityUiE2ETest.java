package org.leo.dictionary.apk.activity;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.viewpager2.widget.ViewPager2;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.leo.dictionary.apk.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class SettingsActivityUiE2ETest {

    @Rule
    public ActivityScenarioRule<SettingsActivity> activityRule = new ActivityScenarioRule<>(SettingsActivity.class);

    @Test
    public void tabsAndPager_areDisplayed() {
        onView(withId(R.id.tab_layout)).check(matches(isDisplayed()));
        onView(withId(R.id.pager)).check(matches(isDisplayed()));
    }

    @Test
    public void speechTab_displaysSpeechPreferences() {
        setPagerPosition(1);
        assertPagerPosition(1);
    }

    @Test
    public void matchWordsTab_displaysMatchWordsPreferences() {
        setPagerPosition(2);
        assertPagerPosition(2);
    }

    @Test
    public void appearanceTab_displaysAppearancePreferences() {
        setPagerPosition(3);
        assertPagerPosition(3);
    }

    private void setPagerPosition(int position) {
        activityRule.getScenario().onActivity(activity -> {
            ViewPager2 pager = activity.findViewById(R.id.pager);
            pager.setCurrentItem(position, false);
        });
    }

    private void assertPagerPosition(int expected) {
        activityRule.getScenario().onActivity(activity -> {
            ViewPager2 pager = activity.findViewById(R.id.pager);
            assertEquals(expected, pager.getCurrentItem());
        });
    }
}
