package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.PlayServiceImpl;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityMainBinding;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.word.provider.WordProvider;

import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    public static final String POSITION_ID = "positionId";
    public static final String UPDATED_WORD = "updatedWord";
    private final ActivityResultLauncher<Intent> filterWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    WordCriteria criteria = (WordCriteria) data.getSerializableExtra(FilterWordsActivity.WORDS_CRITERIA);
                    updateWordsAndUi(criteria);
                }
            });
    private final ActivityResultLauncher<Intent> parseWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateWordsAndUi(null);
                }
            });
    private final ActivityResultLauncher<Intent> editWordActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
                    Word updatedWord = (Word) ((ApplicationWithDI) getApplicationContext()).data.get(UPDATED_WORD);
                    wordProvider.updateWord(updatedWord);

                    PlayService playService = ((ApplicationWithDI) getApplicationContext()).appComponent.playService();
                    Integer positionId = (Integer) ((ApplicationWithDI) getApplicationContext()).data.get(POSITION_ID);
                    WordsFragment wordsFragment = (WordsFragment) getSupportFragmentManager().findFragmentById(R.id.words_fragment);
                    if (positionId != null) {
                        if (shouldBeDisplayed(updatedWord)) {
                            playService.safeUpdate(positionId, updatedWord);
                            wordsFragment.wordUpdated(positionId);
                        } else {
                            playService.safeDelete(positionId);
                            wordsFragment.wordDeleted(positionId);
                        }
                    } else {
                        if (shouldBeDisplayed(updatedWord)) {
                            playService.safeAdd(updatedWord);
                            positionId = playService.getUnknownWords().size() - 1;
                            wordsFragment.wordAdded(positionId, updatedWord);
                        }
                    }
                }
                ((ApplicationWithDI) getApplicationContext()).data.clear();
            });
    private ActivityMainBinding binding;

    private boolean shouldBeDisplayed(Word updatedWord) {
        WordCriteria wordCriteria = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider().getWordCriteria();
        //TODO check topics
        return Objects.equals(updatedWord.getLanguage(), wordCriteria.getLanguageFrom());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setOrientation(false);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.changeOrientation.setOnClickListener(v -> setOrientation(true));
//        binding.changeOrientation.postDelayed(() -> {
//            binding.changeOrientation.setVisibility(View.GONE);
//        }, 5000);
    }

    protected void setOrientation(boolean change) {
        SharedPreferences preferences = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState();
        boolean isPortrait = preferences.getBoolean(ApkModule.LAST_STATE_IS_PORTRAIT, true);
        if (!change ^ !isPortrait) {
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
            Intent intent = new Intent(this, FilterWordsActivity.class);
            filterWordsActivityResultLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_change_mode) {
            changeNightMode();
            return true;
        } else if (id == R.id.action_parse_words) {
            Intent i = new Intent(this, ParseWordsSettingsActivity.class);
            parseWordsActivityResultLauncher.launch(i);
            return true;
        } else if (id == R.id.action_select_voices) {
            Intent i = new Intent(this, VoiceSelectorActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_import_words) {
            if (isDbSource()) {
                showMessage("Not possible. Already used source DB");
            } else {
                importWords(((ApplicationWithDI) getApplicationContext()).appComponent.playService().getUnknownWords());
            }
            return true;
        } else if (id == R.id.action_use_db) {
            prepareForDbUsage();

            Intent intent = new Intent(this, FilterWordsActivity.class);
            filterWordsActivityResultLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_clean_db) {
            AlertDialog.Builder builder = getOptionsBuilder(this);
            builder.show();
            return true;
        } else if (id == R.id.action_word_matcher) {
            Intent i = new Intent(this, WordMatcherActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_add_word) {
            Intent intent = new Intent(this, EditWordActivity.class);
            editWordActivityResultLauncher.launch(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareForDbUsage() {
        ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().edit().putString(ApkModule.LAST_STATE_SOURCE, ApkModule.DB).apply();

        PlayService playService = ((ApplicationWithDI) getApplicationContext()).appComponent.playService();
        WordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        ((PlayServiceImpl) playService).setWordProvider(wordProvider);
    }

    private AlertDialog.Builder getOptionsBuilder(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.languages_to_delete);
        DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        String[] items = wordProvider.languageFrom().toArray(new String[0]);
        builder.setCancelable(true);
        DialogInterface.OnClickListener onClickListener = (dialog, position) -> wordProvider.deleteWords(items[position]);
        builder.setItems(items, onClickListener);
        return builder;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_use_db).setVisible(!isDbSource());
        menu.findItem(R.id.action_import_words).setVisible(!isDbSource());
        menu.findItem(R.id.action_clean_db).setVisible(isDbSource());
        menu.findItem(R.id.action_add_word).setVisible(isDbSource());
        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isDbSource() {
        return ApkModule.isDBSource(((ApplicationWithDI) getApplicationContext()).appComponent.lastState());
    }

    protected void changeNightMode() {
        SharedPreferences preferences = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState();
        boolean isNightMode = preferences.getBoolean(ApkModule.LAST_STATE_IS_NIGHT_MODE, false);
        if (!isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        preferences.edit().putBoolean(ApkModule.LAST_STATE_IS_NIGHT_MODE, !isNightMode).apply();
    }

    private void updateWordsAndUi(WordCriteria wordCriteria) {
        WordCriteriaProvider wordCriteriaProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider();
        wordCriteriaProvider.setWordCriteria(wordCriteria);
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

    public void editWord(int positionId, Word word) {
        Intent intent = new Intent(this, EditWordActivity.class);
        intent.putExtra(EditWordActivity.WORD_ID_TO_EDIT, word.getId());
        ((ApplicationWithDI) getApplicationContext()).data.put(POSITION_ID, positionId);
        editWordActivityResultLauncher.launch(intent);
    }

    private void importWords(List<Word> words) {
        new Thread(() -> {
            showMessage("Import started. It could take few minutes. Please do not import next, until this import is finished");
            DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
            long time = System.currentTimeMillis();
            wordProvider.importWords(words);
            String text = "Import took " + (System.currentTimeMillis() - time) + " ms";
            showMessage(text);
        }).start();
    }

    private void showMessage(String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}