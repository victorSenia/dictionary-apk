package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.PlayServiceImpl;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.*;
import org.leo.dictionary.apk.activity.fragment.WordsFragment;
import org.leo.dictionary.apk.activity.viewmodel.DetailsViewModel;
import org.leo.dictionary.apk.databinding.ActivityMainBinding;
import org.leo.dictionary.apk.helper.KnowledgeToRatingConverter;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.word.provider.WordImporter;
import org.leo.dictionary.word.provider.WordProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainActivity extends AppCompatActivity {
    public final static Logger LOGGER = Logger.getLogger(MainActivity.class.getName());
    public static final String POSITION_ID = "positionId";
    public static final String UPDATED_WORD = "updatedWord";
    public static final String WORD_PROVIDER = "wordProvider";
    private final ActivityResultLauncher<Intent> filterWordsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            WordCriteriaProvider criteriaProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider();
            runAtBackground(() -> updateWordsAndUi(criteriaProvider.getWordCriteria()));
        } else {
            revertDbUsage();
        }
    });
    private final ActivityResultLauncher<Intent> parseWordsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            runAtBackground(() -> updateWordsAndUi(null));
        }
    });
    private final ActivityResultLauncher<Intent> importWordsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            runAtBackground(() -> readWordsFromFile(result.getData().getData()));
        }
    });
    private final ActivityResultLauncher<Intent> editWordActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK) {
            Word updatedWord = (Word) ((ApplicationWithDI) getApplicationContext()).data.get(UPDATED_WORD);
            Integer positionId = (Integer) ((ApplicationWithDI) getApplicationContext()).data.get(POSITION_ID);
            runAtBackground(() -> addUpdateOrDeleteWordInDbAndUi(updatedWord, positionId));
        }
        ((ApplicationWithDI) getApplicationContext()).data.clear();
    });
    private UiUpdater uiUpdater;
    private ActivityMainBinding binding;

    public static void runAtBackground(Runnable runnable) {
        new Thread(runnable).start();
    }

    public static void logUnhandledException(Exception e) {
        LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }

    private void readWordsFromFile(Uri data) {
        try (InputStream inputStream = getContentResolver().openInputStream(data)) {
            List<Word> words = new WordImporter() {
                @Override
                protected BufferedReader getBufferedReader() {
                    return new BufferedReader(new InputStreamReader(inputStream));
                }
            }.readWords();
            if (!words.isEmpty()) {
                DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
                String language = words.get(0).getLanguage();
                if (wordProvider.languageFrom().contains(language)) {
                    runOnUiThread(() -> {
                        AlertDialog.Builder builder = getConfirmDbCleanupOnImport(language, wordProvider, words);
                        builder.show();
                    });
                }
            }
        } catch (IOException e) {
            logUnhandledException(e);
            showMessage(getString(R.string.unexpected_error));
        }
    }

    private AlertDialog.Builder getConfirmDbCleanupOnImport(String language, DBWordProvider wordProvider, List<Word> words) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.languages_already_present);
        builder.setMessage(getString(R.string.languages_already_present_question, language));
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> runAtBackground(() -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    deleteWordsAndUpdateUi(language, wordProvider);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
            importWords(words);
        });
        builder.setPositiveButton(R.string.yes, dialogClickListener);
        builder.setNegativeButton(R.string.no, dialogClickListener);
        return builder;
    }

    private void deleteWordsAndUpdateUi(String language, DBWordProvider wordProvider) {
        wordProvider.deleteWords(language);
        WordCriteria wordCriteria = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider().getWordCriteria();
        if (Objects.equals(language, wordCriteria.getLanguageFrom())) {
            updateWordsAndUi(wordCriteria);
        }
    }

    private void addUpdateOrDeleteWordInDbAndUi(Word updatedWord, Integer positionId) {
        DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        wordProvider.updateWordFully(updatedWord);

        PlayService playService = ((ApplicationWithDI) getApplicationContext()).appComponent.playService();
        WordsFragment wordsFragment = (WordsFragment) getSupportFragmentManager().findFragmentById(R.id.words_fragment);
        if (wordsFragment != null) {
            if (positionId != null) {
                if (shouldBeDisplayed(updatedWord)) {
                    playService.safeUpdate(positionId, updatedWord);
                    runOnUiThread(() -> wordsFragment.wordUpdated(positionId));
                } else {
                    playService.safeDelete(positionId);
                    runOnUiThread(() -> wordsFragment.wordDeleted(positionId));
                }
            } else {
                if (shouldBeDisplayed(updatedWord)) {
                    playService.safeAdd(updatedWord);
                    int newPositionId = playService.getUnknownWords().size() - 1;
                    runOnUiThread(() -> wordsFragment.wordAdded(newPositionId, updatedWord));
                }
            }
        }
    }

    private boolean shouldBeDisplayed(Word updatedWord) {
        WordCriteria wordCriteria = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider().getWordCriteria();
        return Objects.equals(updatedWord.getLanguage(), wordCriteria.getLanguageFrom()) && (wordCriteria.getTopicsOr() == null || wordCriteria.getTopicsOr().isEmpty() || containsAnyOfTopic(wordCriteria.getTopicsOr(), updatedWord.getTopics()));
    }

    private boolean containsAnyOfTopic(Set<String> topicsOr, List<Topic> topics) {
        return topics.stream().map(Topic::getName).anyMatch(topicsOr::contains);
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

        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) getApplicationContext()).appComponent.uiUpdater();
        uiUpdater = new ViewModelProvider(this).get(DetailsViewModel.class)::updateWord;
        apkUiUpdater.addUiUpdater(uiUpdater);
//        binding.changeOrientation.postDelayed(() -> {
//            binding.changeOrientation.setVisibility(View.GONE);
//        }, 5000);
    }

    protected void setOrientation(boolean change) {
        SharedPreferences preferences = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState();
        boolean isPortrait = preferences.getBoolean(ApkModule.LAST_STATE_IS_PORTRAIT, true);
        if (change ^ !isPortrait) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//            getWindow().getDecorView().getWindowInsetsController().
//                    hide(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars()
//                    );
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
//            getWindow().getDecorView().getWindowInsetsController().
//                    show(android.view.WindowInsets.Type.statusBars() | android.view.WindowInsets.Type.navigationBars()
//                    );
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
        } else if (id == R.id.action_filter_words) {
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
        } else if (id == R.id.action_configuration_presets) {
            Intent i = new Intent(this, ConfigurationPresetsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_import_words) {
            if (isDbSource()) {
                showMessage(getString(R.string.already_database_error));
            } else {
                runAtBackground(() -> importWords(((ApplicationWithDI) getApplicationContext()).appComponent.playService().getUnknownWords()));
            }
            return true;
        } else if (id == R.id.action_use_db) {
            prepareForDbUsage();
            Intent intent = new Intent(this, FilterWordsActivity.class);
            filterWordsActivityResultLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_clean_db) {
            DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
            AlertDialog.Builder builder = getLanguageChooserBuilder(this, language -> runAtBackground(() -> deleteWordsAndUpdateUi(language, wordProvider)));
            builder.show();
            return true;
        } else if (id == R.id.action_set_knowledge) {
            createSetKnowledgeDialog(this).show();
            return true;
        } else if (id == R.id.action_word_matcher) {
            stopPlayer();
            Intent i = new Intent(this, WordMatcherActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_word_recognition) {
            stopPlayer();
            Intent i = new Intent(this, SpeechRecognitionActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_add_word) {
            Intent intent = new Intent(this, EditWordActivity.class);
            editWordActivityResultLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_export_words_to_file) {
            Intent i = new Intent(this, ExportWordsActivity.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_import_words_from_file) {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("text/plain");
            chooseFile = Intent.createChooser(chooseFile, getString(R.string.choose_file));
            importWordsActivityResultLauncher.launch(chooseFile);
        }
        return super.onOptionsItemSelected(item);
    }

    private void stopPlayer() {
        ((ApplicationWithDI) getApplicationContext()).appComponent.playService().pause();
    }

    private void prepareForDbUsage() {
        ApplicationWithDI applicationContext = (ApplicationWithDI) getApplicationContext();
        ApkAppComponent appComponent = applicationContext.appComponent;
        SharedPreferences lastState = appComponent.lastState();
        applicationContext.data.put(ApkModule.LAST_STATE_SOURCE, ApkModule.getLastStateSource(lastState));

        lastState.edit().putString(ApkModule.LAST_STATE_SOURCE, ApkModule.DB).apply();

        PlayService playService = appComponent.playService();
        applicationContext.data.put(WORD_PROVIDER, ((PlayServiceImpl) playService).getWordProvider());
        WordProvider wordProvider = appComponent.dbWordProvider();
        ((PlayServiceImpl) playService).setWordProvider(wordProvider);
    }

    private void revertDbUsage() {
        ApplicationWithDI applicationContext = (ApplicationWithDI) getApplicationContext();
        if (applicationContext.data.containsKey(ApkModule.LAST_STATE_SOURCE)) {
            applicationContext.appComponent.lastState().edit().putString(ApkModule.LAST_STATE_SOURCE, (String) applicationContext.data.get(ApkModule.LAST_STATE_SOURCE)).apply();
            PlayService playService = applicationContext.appComponent.playService();
            WordProvider wordProvider = (WordProvider) applicationContext.data.get(WORD_PROVIDER);
            ((PlayServiceImpl) playService).setWordProvider(wordProvider);
        }
        applicationContext.data.clear();
    }

    private AlertDialog.Builder getLanguageChooserBuilder(Context context, Consumer<String> consumer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        String[] items = wordProvider.languageFrom().toArray(new String[0]);
        if (items.length > 0) {
            builder.setMessage(R.string.languages_to_delete);
            DialogInterface.OnClickListener onClickListener = (dialog, position) -> consumer.accept(items[position]);
            builder.setItems(items, onClickListener);
        } else {
            builder.setMessage(R.string.no_languages);
        }
        return builder;
    }

    private AlertDialog.Builder createSetKnowledgeDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.select_knowledge);
        RatingBar ratingBar = new RatingBar(context);
        ratingBar.setNumStars(KnowledgeToRatingConverter.starsCount);
        ratingBar.setMax(KnowledgeToRatingConverter.starsCount);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(ratingBar);
        builder.setView(layout);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> setKnowledgeInDatabase(KnowledgeToRatingConverter.ratingToKnowledge(ratingBar.getRating())));
        return builder;
    }

    private void setKnowledgeInDatabase(double knowledge) {
        ApplicationWithDI applicationContext = (ApplicationWithDI) getApplicationContext();
        PlayService playService = applicationContext.appComponent.playService();
        List<Word> words = playService.getUnknownWords();
        for (Word word : words) {
            word.setKnowledge(knowledge);
        }
        applicationContext.appComponent.dbWordProvider().updateWord(words);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean dbSource = isDbSource();
        menu.findItem(R.id.action_use_db).setVisible(!dbSource);
        menu.findItem(R.id.action_import_words).setVisible(!dbSource);
        menu.findItem(R.id.action_clean_db).setVisible(dbSource);
        menu.findItem(R.id.action_set_knowledge).setVisible(dbSource);
        menu.findItem(R.id.action_add_word).setVisible(dbSource);
        menu.findItem(R.id.action_import_words_from_file).setVisible(dbSource);
        menu.findItem(R.id.action_export_words_to_file).setVisible(dbSource);
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
        runOnUiThread(() -> {
            WordsFragment wordsFragment = (WordsFragment) getSupportFragmentManager().findFragmentById(R.id.words_fragment);
            if (wordsFragment != null) {
                wordsFragment.replaceData(unknownWords);
            }
            new ViewModelProvider(this).get(DetailsViewModel.class).clearWord();
        });
    }

    public void editWord(int positionId, Word word) {
        Intent intent = new Intent(this, EditWordActivity.class);
        intent.putExtra(EditWordActivity.WORD_ID_TO_EDIT, word.getId());
        ((ApplicationWithDI) getApplicationContext()).data.put(POSITION_ID, positionId);
        editWordActivityResultLauncher.launch(intent);
    }

    private void importWords(List<Word> words) {
        showMessage(getString(R.string.import_started));
        DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        long time = System.currentTimeMillis();
        wordProvider.importWords(words);
        showMessage(getString(R.string.import_finished, (System.currentTimeMillis() - time)));
    }

    private void showMessage(String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) getApplicationContext()).appComponent.uiUpdater();
        apkUiUpdater.removeUiUpdater(uiUpdater);
        uiUpdater = null;
        binding = null;
    }
}