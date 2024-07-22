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
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;
import org.jetbrains.annotations.NotNull;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.PlayServiceImpl;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityMainBinding;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Word;
import org.leo.dictionary.entity.WordCriteria;
import org.leo.dictionary.word.provider.WordExporter;
import org.leo.dictionary.word.provider.WordImporter;
import org.leo.dictionary.word.provider.WordProvider;

import java.io.*;
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
    private final ActivityResultLauncher<Intent> filterWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    WordCriteriaProvider criteriaProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider();
                    updateWordsAndUi(criteriaProvider.getWordCriteria());
                }
                else {
                    revertDbUsage();
                }
            });
    private final ActivityResultLauncher<Intent> parseWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    updateWordsAndUi(null);
                }
            });
    private final ActivityResultLauncher<Intent> importWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    readWordsFromFile(result.getData().getData());
                }
            });
    private final ActivityResultLauncher<Intent> exportWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    writeWordsToFile(result.getData().getData());
                }
            });
    private final ActivityResultLauncher<Intent> editWordActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Word updatedWord = (Word) ((ApplicationWithDI) getApplicationContext()).data.get(UPDATED_WORD);
                    addUpdateOrDeleteWordInDbAndUi(updatedWord);
                }
                ((ApplicationWithDI) getApplicationContext()).data.clear();
            });
    private ActivityMainBinding binding;

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
                    AlertDialog.Builder builder = getConfirmDbCleanupOnImport(language, wordProvider, words);
                    builder.show();
                }
            }
        } catch (IOException e) {
            logUnhandledException(e);
            showMessage("Error happened. Please check logs");
        }
    }

    @NotNull
    private AlertDialog.Builder getConfirmDbCleanupOnImport(String language, DBWordProvider wordProvider, List<Word> words) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Language already present");
        builder.setMessage("In database already present some data for language " + language + ".Do you want to delete this data before import?");
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    wordProvider.deleteWords(language);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
            importWords(words);
        };
        builder.setPositiveButton("Yes", dialogClickListener);
        builder.setNegativeButton("No", dialogClickListener);
        return builder;
    }

    private void writeWordsToFile(Uri uri) {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            FilterWordsActivity.LanguageViewModel model = new ViewModelProvider(this).get(FilterWordsActivity.LanguageViewModel.class);
            model.getSelected().getValue();
            DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
            new WordExporter() {
                @Override
                protected BufferedWriter getBufferedWriter() {
                    return new BufferedWriter(new OutputStreamWriter(outputStream));
                }
            }.writeWords(wordProvider.getWordsForLanguage(model.getSelected().getValue()));

        } catch (IOException e) {
            logUnhandledException(e);
            showMessage("Error happened. Please check logs");
        }
    }

    private void addUpdateOrDeleteWordInDbAndUi(Word updatedWord) {
        DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        wordProvider.updateWordFully(updatedWord);

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

    private boolean shouldBeDisplayed(Word updatedWord) {
        WordCriteria wordCriteria = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider().getWordCriteria();
        return Objects.equals(updatedWord.getLanguage(), wordCriteria.getLanguageFrom())
                && (wordCriteria.getTopicsOr() == null || wordCriteria.getTopicsOr().isEmpty() ||
                containsAnyOfTopic(wordCriteria.getTopicsOr(), updatedWord.getTopics()));
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
            DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
            AlertDialog.Builder builder = getLanguageChooserBuilder(this, R.string.languages_to_delete, wordProvider::deleteWords);
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
        } else if (id == R.id.action_export_words_to_file) {
            AlertDialog.Builder builder = getLanguageChooserBuilder(this, R.string.languages_to_export, this::exportWordsToFile);
            builder.show();
            return true;
        } else if (id == R.id.action_import_words_from_file) {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("text/plain");
            chooseFile = Intent.createChooser(chooseFile, "Choose a file");
            importWordsActivityResultLauncher.launch(chooseFile);
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportWordsToFile(String language) {
        FilterWordsActivity.LanguageViewModel model = new ViewModelProvider(this).get(FilterWordsActivity.LanguageViewModel.class);
        model.select(language);
        Intent createFile = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        createFile.setType("text/plain");
        createFile.addCategory(Intent.CATEGORY_OPENABLE);
        createFile = Intent.createChooser(createFile, "Choose a file");
        createFile.putExtra(Intent.EXTRA_TITLE, "fileName_de.txt");
        exportWordsActivityResultLauncher.launch(createFile);
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

    private AlertDialog.Builder getLanguageChooserBuilder(Context context, int title, Consumer<String> consumer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        String[] items = wordProvider.languageFrom().toArray(new String[0]);
        builder.setCancelable(true);
        DialogInterface.OnClickListener onClickListener = (dialog, position) -> consumer.accept(items[position]);
        builder.setItems(items, onClickListener);
        return builder;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean dbSource = isDbSource();
        menu.findItem(R.id.action_use_db).setVisible(!dbSource);
        menu.findItem(R.id.action_import_words).setVisible(!dbSource);
        menu.findItem(R.id.action_clean_db).setVisible(dbSource);
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