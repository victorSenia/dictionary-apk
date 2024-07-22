package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.entity.WordCriteria;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class FilterWordsActivity extends AppCompatActivity {

    public static final String WORDS_CRITERIA = "wordsCriteria";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.filter_words_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        findViewById(R.id.find_words).setOnClickListener(v -> {
            Intent intent = new Intent();
            WordCriteria criteria = createCriteria();
            intent.putExtra(WORDS_CRITERIA, criteria);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        findViewById(R.id.all_topics).setOnClickListener(v -> {
            TopicsFragment topics = (TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
            ((MultiSelectionStringRecyclerViewAdapter) topics.recyclerView.getAdapter()).clearSelection();
            topics.recyclerView.getAdapter().notifyDataSetChanged();
        });
    }

    private WordCriteria createCriteria() {
        TopicsFragment topics = (TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
        LanguageFromFragment languageFrom = (LanguageFromFragment) getSupportFragmentManager().findFragmentById(R.id.language_from);
        LanguageToFragment languageTo = (LanguageToFragment) getSupportFragmentManager().findFragmentById(R.id.languages_to);
        LanguageToFragment languageToSpeak = (LanguageToFragment) getSupportFragmentManager().findFragmentById(R.id.languages_to_speak);
        WordCriteria criteria = new WordCriteria();
        List<String> selectedTopics = ((MultiSelectionStringRecyclerViewAdapter) topics.recyclerView.getAdapter()).getSelected();
        if (selectedTopics != null && !selectedTopics.isEmpty()) {
            criteria.setTopicsOr(selectedTopics);
        }
        List<String> selectedLanguageTo = ((MultiSelectionStringRecyclerViewAdapter) languageTo.recyclerView.getAdapter()).getSelected();
        if (selectedLanguageTo != null && !selectedLanguageTo.isEmpty()) {
            criteria.setLanguageTo(new HashSet<>(selectedLanguageTo));
        }
        List<String> selectedLanguageFrom = ((MultiSelectionStringRecyclerViewAdapter) languageFrom.recyclerView.getAdapter()).getSelected();
        if (selectedLanguageFrom != null && !selectedLanguageFrom.isEmpty()) {
            criteria.setLanguageFrom(selectedLanguageFrom.get(0));
        }
        List<String> selectedLanguageToSpeak = ((MultiSelectionStringRecyclerViewAdapter) languageToSpeak.recyclerView.getAdapter()).getSelected();
        if (selectedLanguageToSpeak != null && !selectedLanguageToSpeak.isEmpty()) {
            criteria.setLanguageTo(new HashSet<>(selectedLanguageToSpeak));
        }
        return criteria;
    }

    public static class TopicsFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.externalWordProvider();
            return wordProvider.findTopics();
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter adapter = new MultiSelectionStringRecyclerViewAdapter(getStrings(), this);
            WordCriteriaProvider wordCriteriaProvider = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.wordCriteriaProvider();
            adapter.setSelected(wordCriteriaProvider.getWordCriteria().getTopicsOr());
            return adapter;
        }
    }

    public static class LanguageFromFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.externalWordProvider();
            return wordProvider.languageFrom();
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter adapter = new MultiSelectionStringRecyclerViewAdapter(getStrings(), this);
            WordCriteriaProvider wordCriteriaProvider = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.wordCriteriaProvider();
            adapter.setSelected(Collections.singleton(wordCriteriaProvider.getWordCriteria().getLanguageFrom()));
            return adapter;
        }
    }

    public static class LanguageToFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.externalWordProvider();
            return wordProvider.languageTo();
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter adapter = new MultiSelectionStringRecyclerViewAdapter(getStrings(), this);
            WordCriteriaProvider wordCriteriaProvider = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.wordCriteriaProvider();
            adapter.setSelected(wordCriteriaProvider.getWordCriteria().getLanguageTo());
            return adapter;
        }
    }
}