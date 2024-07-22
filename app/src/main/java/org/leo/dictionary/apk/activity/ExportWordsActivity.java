package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.word.provider.WordExporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExportWordsActivity extends AppCompatActivity {

    private final ActivityResultLauncher<Intent> exportWordsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    MainActivity.runAtBackground(() -> writeWordsToFile(result.getData().getData()));
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FilterWordsActivity.LanguageViewModel languageViewModel = new ViewModelProvider(this).get(FilterWordsActivity.LanguageViewModel.class);
        FilterWordsActivity.RootTopicViewModel rootTopicViewModel = new ViewModelProvider(this).get(FilterWordsActivity.RootTopicViewModel.class);
        setContentView(R.layout.export_words_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        findViewById(R.id.find_words).setOnClickListener(v -> {
            exportWordsToFile();
        });
        findViewById(R.id.all_root_topics).setOnClickListener(v -> {
            FilterWordsActivity.RootTopicsFragment topics = (FilterWordsActivity.RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
            if (topics != null) {
                ((StringRecyclerViewAdapter) topics.recyclerView.getAdapter()).clearSelection();
                rootTopicViewModel.select(null);
            }
        });
        ApkAppComponent appComponent = ((ApplicationWithDI) getApplicationContext()).appComponent;
        List<String> languagesFrom = appComponent.externalWordProvider().languageFrom();
        if (!ApkModule.isDBSource(appComponent.lastState()) || languagesFrom.isEmpty()) {
            findViewById(R.id.language_from_container).setVisibility(View.GONE);
        } else if (languagesFrom.size() == 1) {
            languageViewModel.select(languagesFrom.get(0));
        }
    }

    private void exportWordsToFile() {
        Intent createFile = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        createFile.setType("text/plain");
        createFile.addCategory(Intent.CATEGORY_OPENABLE);
        createFile = Intent.createChooser(createFile, "Choose a file");
        createFile.putExtra(Intent.EXTRA_TITLE, "fileName_de.txt");//TODO not working
        exportWordsActivityResultLauncher.launch(createFile);
    }

    private void writeWordsToFile(Uri uri) {
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            FilterWordsActivity.LanguageViewModel languageViewModel = new ViewModelProvider(this).get(FilterWordsActivity.LanguageViewModel.class);
            FilterWordsActivity.RootTopicViewModel rootTopicViewModel = new ViewModelProvider(this).get(FilterWordsActivity.RootTopicViewModel.class);
            DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
            String language = languageViewModel.getSelected().getValue();
            String rootTopicName = rootTopicViewModel.getSelected().getValue();
            WordExporter wordExporter = new WordExporter() {
                @Override
                protected BufferedWriter getBufferedWriter() {
                    return new BufferedWriter(new OutputStreamWriter(outputStream));
                }
            };
            if (rootTopicName != null && !rootTopicName.isEmpty()) {
                wordExporter.writeWords(wordProvider.getWordsForLanguage(language, rootTopicName), false,
                        Collections.singletonList(rootTopicName));
            } else {
                wordExporter.writeWords(wordProvider.getWordsForLanguage(language, rootTopicName), true,
                        wordProvider.findRootTopics(language).stream().map(Topic::getName).collect(Collectors.toList()));
            }
        } catch (IOException e) {
            MainActivity.logUnhandledException(e);
            showMessage("Error happened. Please check logs");
        }
    }

    private void showMessage(String text) {
        runOnUiThread(() -> Toast.makeText(this, text, Toast.LENGTH_LONG).show());
    }
}