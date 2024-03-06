package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityMainBinding;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private final ActivityResultLauncher<Intent> topicsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    String topic = data.getStringExtra(ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA);
                    updateTopicAndUi(topic);
                }
            });
    private final ActivityResultLauncher<Intent> parseWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateTopicAndUi(null);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
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
        } else if (id == R.id.action_parse_words) {
            Intent i = new Intent(this, ParseWordsSettingsActivity.class);
            parseWordsActivityResultLauncher.launch(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateTopicAndUi(String topic) {
        WordCriteria wordCriteria = new WordCriteria();
        if (topic != null && !topic.isEmpty()) {
            wordCriteria.setTopicsOr(Collections.singleton(topic));
        }
        ApkModule.provideLastState(getApplicationContext()).edit().putString(ApkModule.LAST_STATE_TOPIC, topic).apply();
        updateUiWithWords(wordCriteria);
    }

    private void updateUiWithWords(WordCriteria wordCriteria) {
        PlayService playService = ((ApplicationWithDI) getApplicationContext()).appComponent.playService();
        playService.findWords(wordCriteria);
        updateUiWithNewData(playService.getUnknownWords());
    }

    private void updateUiWithNewData(List<Word> unknownWords) {
        WordsFragment wordsFragment = (WordsFragment) getSupportFragmentManager().findFragmentById(R.id.wordsFragment);
        wordsFragment.replaceData(unknownWords);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}