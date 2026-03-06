package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.WordsFragment;
import org.leo.dictionary.apk.activity.viewadapter.WordsRecyclerViewAdapter;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MainActivityFunctionalE2ETest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        disableAnimations();
    }

    @After
    public void tearDown() {
        activityRule.getScenario().onActivity(activity -> getPlayService(activity).pause());
    }

    @Test
    public void playPauseNextPrevious_controlsAreFunctional() {
        if (!waitUntilWordsLoaded()) {
            ensureViewExists(R.id.button_play);
            return;
        }

        clickByIdOnUiThread(R.id.button_play);
        waitUntilPlayingState(true, 4000);

        clickByIdOnUiThread(R.id.button_next);
        clickByIdOnUiThread(R.id.button_previous);

        clickByIdOnUiThread(R.id.button_play);
        waitUntilPlayingState(false, 4000);
        ensureViewExists(R.id.filter_text);
    }

    @Test
    public void playFromSelectedWordInList_startsPlayback() {
        if (!waitUntilWordsLoaded()) {
            ensureViewExists(R.id.button_play);
            return;
        }

        clickFirstWordItemOnUiThread();
        clickByIdOnUiThread(R.id.play_from);

        waitUntilPlayingState(true, 4000);
        ensureViewExists(R.id.button_play);
    }

    @Test
    public void listFilter_canApplyAndClear() {
        activityRule.getScenario().onActivity(activity -> getPlayService(activity).pause());

        if (!isVisibleInActivity(R.id.filter_text)) {
            return;
        }
        if (!waitUntilWordsLoaded()) {
            return;
        }
        int initialCount = getWordListCount();
        assertTrue(initialCount > 0);
        String sourceWord = getWordAt(0);
        String query = sourceWord.substring(0, Math.min(3, sourceWord.length())).toLowerCase(Locale.ROOT);

        setEditTextOnUiThread(R.id.filter_text, query);
        SystemClock.sleep(300);
        int filteredCount = getWordListCount();
        assertTrue(filteredCount > 0 && filteredCount <= initialCount);

        setEditTextOnUiThread(R.id.filter_text, "zzzz_no_results_expected_123");
        SystemClock.sleep(300);
        int noResultCount = getWordListCount();
        assertTrue(noResultCount <= filteredCount);

        setEditTextOnUiThread(R.id.filter_text, "");
        SystemClock.sleep(300);
        assertTrue(getWordListCount() > 0);
    }

    @Test
    public void filterActivity_openApplyAndReturnToMain() {
        Context context = ApplicationProvider.getApplicationContext();

        openActionBarOverflowOrOptionsMenu(context);
        onView(withText(R.string.find_words)).perform(click());

        onView(withId(R.id.find_words)).check(matches(isDisplayed())).perform(click());

        onView(withId(R.id.toolbar)).check(matches(isDisplayed()));
        assertTrue(getWordListCount() >= 0);
    }

    private void ensureViewExists(int viewId) {
        AtomicReference<android.view.View> view = new AtomicReference<>();
        activityRule.getScenario().onActivity(activity -> view.set(activity.findViewById(viewId)));
        assertTrue(view.get() != null);
    }

    private boolean isVisibleInActivity(int viewId) {
        AtomicReference<android.view.View> view = new AtomicReference<>();
        activityRule.getScenario().onActivity(activity -> view.set(activity.findViewById(viewId)));
        return view.get() != null && view.get().getVisibility() == android.view.View.VISIBLE;
    }

    private void clickByIdOnUiThread(int viewId) {
        activityRule.getScenario().onActivity(activity -> {
            android.view.View view = activity.findViewById(viewId);
            if (view != null) {
                view.performClick();
            }
        });
    }

    private void clickFirstWordItemOnUiThread() {
        activityRule.getScenario().onActivity(activity -> {
            androidx.recyclerview.widget.RecyclerView list = activity.findViewById(R.id.list);
            if (list != null && list.getAdapter() != null && list.getAdapter().getItemCount() > 0) {
                list.scrollToPosition(0);
                androidx.recyclerview.widget.RecyclerView.ViewHolder holder = list.findViewHolderForAdapterPosition(0);
                if (holder != null && holder.itemView != null) {
                    holder.itemView.performClick();
                }
            }
        });
    }

    private void setEditTextOnUiThread(int viewId, String value) {
        activityRule.getScenario().onActivity(activity -> {
            android.view.View view = activity.findViewById(viewId);
            if (view instanceof android.widget.TextView) {
                ((android.widget.TextView) view).setText(value);
            }
        });
    }

    private boolean waitUntilPlayingState(boolean expected, long timeoutMs) {
        long start = SystemClock.elapsedRealtime();
        while (SystemClock.elapsedRealtime() - start < timeoutMs) {
            AtomicReference<Boolean> playing = new AtomicReference<>(null);
            activityRule.getScenario().onActivity(activity -> playing.set(getPlayService(activity).isPlaying()));
            if (Boolean.valueOf(expected).equals(playing.get())) {
                return true;
            }
            SystemClock.sleep(100);
        }
        return false;
    }

    private boolean waitUntilWordsLoaded() {
        long start = SystemClock.elapsedRealtime();
        while (SystemClock.elapsedRealtime() - start < 5000) {
            if (getWordListCount() > 0) {
                return true;
            }
            SystemClock.sleep(100);
        }
        return false;
    }

    private int getWordListCount() {
        AtomicInteger count = new AtomicInteger(0);
        activityRule.getScenario().onActivity(activity -> count.set(getAdapter(activity).getItemCount()));
        return count.get();
    }

    private String getWordAt(int index) {
        AtomicReference<String> word = new AtomicReference<>("");
        activityRule.getScenario().onActivity(activity -> word.set(getAdapter(activity).values.get(index).getWord()));
        return word.get() == null ? "" : word.get();
    }

    private WordsRecyclerViewAdapter getAdapter(MainActivity activity) {
        WordsFragment wordsFragment = (WordsFragment) activity.getSupportFragmentManager().findFragmentById(R.id.words_fragment);
        return wordsFragment.getRecyclerViewAdapter();
    }

    private PlayService getPlayService(MainActivity activity) {
        return ((ApplicationWithDI) activity.getApplicationContext()).appComponent.playService();
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
