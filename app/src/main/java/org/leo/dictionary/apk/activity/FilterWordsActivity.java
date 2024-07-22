package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.WordCriteria;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class FilterWordsActivity extends AppCompatActivity {

    private static List<String> getSelected(StringsFragment strings) {
        if (strings != null && strings.recyclerView.getAdapter() instanceof MultiSelectionStringRecyclerViewAdapter) {
            MultiSelectionStringRecyclerViewAdapter adapter = (MultiSelectionStringRecyclerViewAdapter) strings.recyclerView.getAdapter();
            if (!adapter.getSelectedList().isEmpty()) {
                return adapter.getSelectedList();
            }
        } else if (strings != null && strings.recyclerView.getAdapter() instanceof StringRecyclerViewAdapter) {
            StringRecyclerViewAdapter adapter = (StringRecyclerViewAdapter) strings.recyclerView.getAdapter();
            if (adapter.getSelected() != RecyclerView.NO_POSITION) {
                return Collections.singletonList(adapter.mValues.get(adapter.getSelected()));
            }
        }
        return null;
    }

    private static WordCriteria getWordCriteria(Context context) {
        WordCriteriaProvider wordCriteriaProvider = ((ApplicationWithDI) context.getApplicationContext()).appComponent.wordCriteriaProvider();
        return wordCriteriaProvider.getWordCriteria();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageViewModel languageViewModel = new ViewModelProvider(this).get(LanguageViewModel.class);
        RootTopicViewModel rootTopicViewModel = new ViewModelProvider(this).get(RootTopicViewModel.class);
        setContentView(R.layout.filter_words_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        findViewById(R.id.find_words).setOnClickListener(v -> {
            Intent intent = new Intent();
            WordCriteria criteria = createCriteria();
            WordCriteriaProvider criteriaProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider();
            criteriaProvider.setWordCriteria(criteria);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        findViewById(R.id.all_topics).setOnClickListener(v -> {
            TopicsFragment topics = (TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
            if (topics != null) {
                ((MultiSelectionStringRecyclerViewAdapter) topics.recyclerView.getAdapter()).clearSelection();
            }
        });
        findViewById(R.id.all_root_topics).setOnClickListener(v -> {
            RootTopicsFragment topics = (RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
            if (topics != null) {
                ((StringRecyclerViewAdapter) topics.recyclerView.getAdapter()).clearSelection();
                rootTopicViewModel.select(null);
            }
        });
        if (getWordCriteria(this).getShuffleRandom() != -1) {
            SwitchCompat shuffle = findViewById(R.id.shuffle);
            shuffle.setChecked(true);
        }
        ApkAppComponent appComponent = ((ApplicationWithDI) getApplicationContext()).appComponent;
        List<String> languagesFrom = appComponent.externalWordProvider().languageFrom();
        if (!ApkModule.isDBSource(appComponent.lastState()) || languagesFrom.isEmpty()) {
            findViewById(R.id.language_from_container).setVisibility(View.GONE);
        } else if (languagesFrom.size() == 1) {
            languageViewModel.select(languagesFrom.get(0));
            findViewById(R.id.language_from_container).setVisibility(View.GONE);
        }
        TextView text = findViewById(R.id.languages_to_label);
        if (!ApkModule.isDBSource(appComponent.lastState())) {
            text.setText(R.string.languages_to_speak);
        } else {
            text.setText(R.string.languages_to);
        }
    }

    private WordCriteria createCriteria() {
        TopicsFragment topics = (TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
        RootTopicsFragment rootTopics = (RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
        LanguageFromFragment languageFrom = (LanguageFromFragment) getSupportFragmentManager().findFragmentById(R.id.language_from);
        LanguageToFragment languageTo = (LanguageToFragment) getSupportFragmentManager().findFragmentById(R.id.languages_to);
        WordCriteria criteria = new WordCriteria();
        SwitchCompat shuffle = findViewById(R.id.shuffle);
        if (shuffle.isChecked()) {
            criteria.setShuffleRandom(System.currentTimeMillis());
        }
        List<String> selectedTopics = getSelected(topics);
        if (selectedTopics != null) {
            criteria.setTopicsOr(selectedTopics);
        }
        List<String> selectedLanguageTo = getSelected(languageTo);
        if (selectedLanguageTo != null) {
            criteria.setLanguageTo(new HashSet<>(selectedLanguageTo));
        }
        List<String> selectedLanguageFrom = getSelected(languageFrom);
        if (selectedLanguageFrom != null) {
            criteria.setLanguageFrom(selectedLanguageFrom.get(0));
        }
        List<String> selectedRootTopic = getSelected(rootTopics);
        if (selectedRootTopic != null) {
            criteria.setRootTopic(selectedRootTopic.get(0));
        }
        return criteria;
    }

    public static class RootTopicsFragment extends StringsFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).getSelected().observe(requireActivity(), this::updateListData);
        }

        protected void updateListData(String language) {
            StringRecyclerViewAdapter adapter = (StringRecyclerViewAdapter) this.recyclerView.getAdapter();
            if (adapter != null) {
                adapter.mValues.clear();
                adapter.clearSelection();
                adapter.mValues.addAll(findTopics(language));
                adapter.notifyDataSetChanged();
            }
        }

        protected List<String> getStrings() {
            String language = new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).getSelected().getValue();
            return findTopics(language);
        }

        protected List<String> findTopics(String language) {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            List<String> result = wordProvider.findTopics(new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).getSelected().getValue(), 1).stream().map(Topic::getName).sorted().collect(Collectors.toList());
            requireActivity().findViewById(R.id.root_topics_container).setVisibility(result.size() > 1 ? View.VISIBLE : View.GONE);
            new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).select(null);
            return result;
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            StringRecyclerViewAdapter adapter = new StringRecyclerViewAdapter(getStrings(), this, new StringRecyclerViewAdapter.RememberSelectionOnClickListener() {
                @Override
                public void onClick(StringRecyclerViewAdapter.StringViewHolder viewHolder) {
                    super.onClick(viewHolder);
                    new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).select(viewHolder.mItem);
                }
            });
            if (adapter.mValues.size() == 1) {
                adapter.setSelected(0);
                new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).select(adapter.mValues.get(0));
            } else {
                String rootTopic = getWordCriteria(requireActivity()).getRootTopic();
                adapter.setSelected(adapter.mValues.indexOf(rootTopic));
                new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).select(rootTopic);
            }
            return adapter;
        }
    }

    public static class TopicsFragment extends RootTopicsFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).getSelected().observe(requireActivity(), this::updateListData);
        }

        protected List<String> getStrings() {
            String language = new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).getSelected().getValue();
            return findTopics(language);
        }

        protected List<String> findTopics(String language) {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            List<String> result = wordProvider.findTopicsWithRoot(
                    new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).getSelected().getValue(),
                    new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).getSelected().getValue(),2).
                    stream().map(Topic::getName).sorted().collect(Collectors.toList());
            requireActivity().findViewById(R.id.topics_container).setVisibility(result.size() > 1 ? View.VISIBLE : View.GONE);
            return result;
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter adapter = new MultiSelectionStringRecyclerViewAdapter(getStrings(), this);
            adapter.setSelected(getWordCriteria(requireActivity()).getTopicsOr());
            return adapter;
        }
    }

    public static class LanguageFromFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            return wordProvider.languageFrom();
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            StringRecyclerViewAdapter adapter = new StringRecyclerViewAdapter(getStrings(), this, new StringRecyclerViewAdapter.RememberSelectionOnClickListener() {
                @Override
                public void onClick(StringRecyclerViewAdapter.StringViewHolder viewHolder) {
                    super.onClick(viewHolder);
                    getLanguageViewModel().select(viewHolder.mItem);
                }
            });
            if (adapter.mValues.size() == 1) {
                adapter.setSelected(0);
                getLanguageViewModel().select(adapter.mValues.get(0));
            } else {
                String languageFrom = getWordCriteria(requireActivity()).getLanguageFrom();
                adapter.setSelected(adapter.mValues.indexOf(languageFrom));
                getLanguageViewModel().select(languageFrom);
            }
            return adapter;
        }

        private LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }
    }

    public static class LanguageToFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            String language = getLanguageViewModel().getSelected().getValue();
            return new ArrayList<>(getLanguageTo(language));
        }

        @NotNull
        private LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }

        private List<String> getLanguageTo(String language) {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            List<String> languagesTo = wordProvider.languageTo(language);
            requireActivity().findViewById(R.id.languages_to_container).setVisibility(languagesTo.size() > 1 ? View.VISIBLE : View.GONE);
            return languagesTo;
        }


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getLanguageViewModel().getSelected().observe(requireActivity(), this::updateListData);
        }

        private void updateListData(String language) {
            StringRecyclerViewAdapter adapter = (StringRecyclerViewAdapter) this.recyclerView.getAdapter();
            adapter.mValues.clear();
            adapter.clearSelection();
            adapter.mValues.addAll(getLanguageTo(language));
            adapter.notifyDataSetChanged();
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter adapter = new MultiSelectionStringRecyclerViewAdapter(getStrings(), this);
            adapter.setSelected(getWordCriteria(requireActivity()).getLanguageTo());
            return adapter;
        }
    }

    public static class LanguageViewModel extends ViewModel {
        private final MutableLiveData<String> selected = new MutableLiveData<>();

        public void select(String item) {
            selected.setValue(item);
        }

        public LiveData<String> getSelected() {
            return selected;
        }
    }

    public static class RootTopicViewModel extends LanguageViewModel {
    }
}