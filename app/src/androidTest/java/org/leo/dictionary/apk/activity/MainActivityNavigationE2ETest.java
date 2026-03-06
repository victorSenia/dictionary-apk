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
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainActivityNavigationE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

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
    public void openSettingsFromOverflow_navigatesToSettingsScreen() {
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(R.string.action_settings)).perform(click());

        intended(hasComponent(new Intent(context, SettingsActivity.class).getComponent()));
        onView(withId(R.id.pager)).check(matches(isDisplayed()));
    }

    @Test
    public void openSentenceFromOverflow_navigatesToSentenceScreen() {
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(R.string.order_words_in_sentence)).perform(click());

        intended(hasComponent(new Intent(context, SentenceActivity.class).getComponent()));
        onView(withId(R.id.button_next)).check(matches(isDisplayed()));
    }

    @Test
    public void openArticleMatcherFromOverflow_navigatesToMatchArticleScreen() {
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(R.string.match_word_article)).perform(click());

        intended(hasComponent(new Intent(context, MatchArticleActivity.class).getComponent()));
    }

    @Test
    public void openGrammarFilterFromOverflow_navigatesToGrammarFilterScreen() {
        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(R.string.grammar_filter)).perform(click());

        intended(hasComponent(new Intent(context, GrammarFilterActivity.class).getComponent()));
        onView(withId(R.id.button_practice)).check(matches(isDisplayed()));
    }
}
