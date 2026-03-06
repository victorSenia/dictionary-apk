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
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class GrammarFilterUiE2ETest {

    @Rule
    public ActivityScenarioRule<GrammarFilterActivity> activityRule = new ActivityScenarioRule<>(GrammarFilterActivity.class);

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
    public void learnButton_opensGrammarLearningScreen() {
        onView(withId(R.id.button_learn)).check(matches(isDisplayed())).perform(click());

        intended(hasComponent(new Intent(context, GrammarLearningActivity.class).getComponent()));
        onView(withId(R.id.sentences)).check(matches(isDisplayed()));
    }

    @Test
    public void practiceButton_opensGrammarCheckScreen() {
        onView(withId(R.id.button_practice)).check(matches(isDisplayed())).perform(click());

        intended(hasComponent(new Intent(context, GrammarCheckActivity.class).getComponent()));
        onView(withId(R.id.next)).check(matches(isDisplayed()));
    }
}
