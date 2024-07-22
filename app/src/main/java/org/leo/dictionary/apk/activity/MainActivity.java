package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityMainBinding;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final ActivityResultLauncher<Intent> topicsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    WordCriteria criteria = (WordCriteria) data.getSerializableExtra(TopicsActivity.WORDS_CRITERIA);
                    updateTopicAndUi(criteria);
                }
            });
    private final ActivityResultLauncher<Intent> parseWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateTopicAndUi(null);
                }
            });
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNightMode(false);
        setOrientation(false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        binding.changeOrientation.getBehavior().
        binding.changeOrientation.setOnClickListener(v -> setOrientation(true));
//        binding.changeOrientation.postDelayed(() -> {
//            binding.changeOrientation.setVisibility(View.GONE);
//        }, 5000);
    }

    protected void setOrientation(boolean change) {
        SharedPreferences preferences = ApkModule.provideLastState(getApplicationContext());
        boolean isPortrait = preferences.getBoolean(ApkModule.LAST_STATE_IS_PORTRAIT, true);
        if (change ^ !isPortrait) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getWindow().getDecorView().getWindowInsetsController().
                    hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars()
            );
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            getWindow().getDecorView().getWindowInsetsController().
                    show(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars()
            );
        }
        if (change) {
            preferences.edit().putBoolean(ApkModule.LAST_STATE_IS_PORTRAIT, !isPortrait).apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_select_for_topic) {
            Intent intent = new Intent(this, TopicsActivity.class);
            topicsActivityResultLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_change_mode) {
            setNightMode(true);
            return true;
        } else if (id == R.id.action_parse_words) {
            Intent i = new Intent(this, ParseWordsSettingsActivity.class);
            parseWordsActivityResultLauncher.launch(i);
            return true;
        } else if (id == R.id.action_select_voices) {
            Intent i = new Intent(this, VoiceSelectorActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void setNightMode(boolean change) {
        SharedPreferences preferences = ApkModule.provideLastState(getApplicationContext());
        boolean isNightMode = preferences.getBoolean(ApkModule.LAST_STATE_IS_NIGHT_MODE, false);
        if (change ^ isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        if (change) {
            preferences.edit().putBoolean(ApkModule.LAST_STATE_IS_NIGHT_MODE, !isNightMode).apply();
        }
    }

    private void updateTopicAndUi(WordCriteria wordCriteria) {
        if (wordCriteria == null) {
            wordCriteria = new WordCriteria();
        }
        updateUiWithWords(wordCriteria);
    }

    private void updateUiWithWords(WordCriteria wordCriteria) {
        PlayService playService = ((ApplicationWithDI) getApplicationContext()).appComponent.playService();
        playService.findWords(wordCriteria);
        updateUiWithNewData(playService.getUnknownWords());
    }

    private void updateUiWithNewData(List<Word> unknownWords) {
        WordsFragment wordsFragment = (WordsFragment) getSupportFragmentManager().findFragmentById(R.id.words_fragment);
        wordsFragment.replaceData(unknownWords);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}