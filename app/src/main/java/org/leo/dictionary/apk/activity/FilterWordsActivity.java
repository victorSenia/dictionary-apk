package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.FilterWordsActivityBinding;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.WordCriteria;

import java.util.*;
import java.util.stream.Collectors;

public class FilterWordsActivity extends AppCompatActivity {

    public static final int FILTER_AFTER_SIZE = 10;

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
        FilterWordsActivityBinding binding = FilterWordsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        binding.findWords.setOnClickListener(v -> {
            Intent intent = new Intent();
            WordCriteria criteria = createCriteria();
            WordCriteriaProvider criteriaProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider();
            criteriaProvider.setWordCriteria(criteria);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        binding.allTopics.setOnClickListener(v -> {
            TopicsFragment topics = (TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
            if (topics != null) {
                ((MultiSelectionStringRecyclerViewAdapter) topics.recyclerView.getAdapter()).clearSelection();
            }
        });
        binding.allRootTopics.setOnClickListener(v -> {
            RootTopicsFragment topics = (RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
            if (topics != null) {
                ((StringRecyclerViewAdapter) topics.recyclerView.getAdapter()).clearSelection();
                rootTopicViewModel.setSelected(null);
            }
        });
        if (getWordCriteria(this).getShuffleRandom() != -1) {
            SwitchCompat shuffle = findViewById(R.id.shuffle);
            shuffle.setChecked(true);
        }
        ApkAppComponent appComponent = ((ApplicationWithDI) getApplicationContext()).appComponent;
        List<String> languagesFrom = appComponent.externalWordProvider().languageFrom();
        if (!ApkModule.isDBSource(appComponent.lastState()) || languagesFrom.isEmpty()) {
            binding.languageFromContainer.setVisibility(View.GONE);
        } else if (languagesFrom.size() == 1) {
            languageViewModel.setSelected(languagesFrom.get(0));
            binding.languageFromContainer.setVisibility(View.GONE);
        }
        setFilterViewToRootTopicsFragment(R.id.topics, binding.textTopic);
        setFilterViewToRootTopicsFragment(R.id.root_topics, binding.textRootTopic);
        TextView text = binding.languagesToLabel;
        if (!ApkModule.isDBSource(appComponent.lastState())) {
            text.setText(R.string.languages_to_speak);
        } else {
            text.setText(R.string.languages_to);
        }
    }

    private void setFilterViewToRootTopicsFragment(int id, EditText filterView) {
        RootTopicsFragment fragment = (RootTopicsFragment) getSupportFragmentManager().findFragmentById(id);
        if (fragment != null) {
            fragment.setFilterView(filterView);
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
        protected EditText filter;
        protected String language;
        private List<String> topics;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).getData().observe(requireActivity(), this::updateListData);
        }

        protected void updateListData(String value) {
            StringRecyclerViewAdapter adapter = (StringRecyclerViewAdapter) this.recyclerView.getAdapter();
            if (adapter != null && stateChanged()) {
                findTopicsHideFilterIfNeeded(value);
                filterTopicsInAdapter();
            }
        }

        private void findTopicsHideFilterIfNeeded(String value) {
            topics = findTopics(value);
            if (filter != null) {
                filter.setVisibility(topics.size() < FILTER_AFTER_SIZE ? View.GONE : View.VISIBLE);
            }
        }

        protected void filterTopicsInAdapter() {
            StringRecyclerViewAdapter adapter = (StringRecyclerViewAdapter) this.recyclerView.getAdapter();
            if (adapter != null) {
                adapter.mValues.clear();
                adapter.mValues.addAll(filterTopics());
                adapter.clearSelection();
            }
        }

        protected void setFilterView(EditText filterView) {
            filter = filterView;
            filter.addTextChangedListener(new EditWordActivity.AbstractTextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterTopicsInAdapter();
                }
            });
        }

        protected List<String> filterTopics() {
            if (filter != null && !filter.getText().toString().isEmpty()) {
                CharSequence filterString = filter.getText().toString();
                return topics.stream().filter(t -> t.contains(filterString)).collect(Collectors.toList());
            }
            return topics;
        }

        protected boolean stateChanged() {
            return !Objects.equals(language, getStateLanguage());
        }

        protected List<String> getStrings() {
            findTopicsHideFilterIfNeeded(getStateLanguage());
            return new ArrayList<>(topics);
        }

        protected String getStateLanguage() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).getSelected();
        }

        protected List<String> findTopics(String value) {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            language = getStateLanguage();
            List<String> result = wordProvider.findTopics(language, 1).stream().map(Topic::getName).sorted().collect(Collectors.toList());
            requireActivity().findViewById(R.id.root_topics_container).setVisibility(result.size() > 1 ? View.VISIBLE : View.GONE);
            new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).setSelected(null);
            return result;
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            StringRecyclerViewAdapter adapter = new StringRecyclerViewAdapter(getStrings(), this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener(
                            viewHolder -> new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).setSelected(viewHolder.mItem)));
            if (adapter.mValues.size() == 1) {
                adapter.setSelected(0);
                new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).setSelected(adapter.mValues.get(0));
            } else {
                String rootTopic = getWordCriteria(requireActivity()).getRootTopic();
                adapter.setSelected(adapter.mValues.indexOf(rootTopic));
                new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).setSelected(rootTopic);
            }
            return adapter;
        }
    }

    public static class TopicsFragment extends RootTopicsFragment {
        protected String rootTopic;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).getData().observe(requireActivity(), this::updateListData);
        }

        @Override
        protected boolean stateChanged() {
            return super.stateChanged() || !Objects.equals(rootTopic, getStateRootTopic());
        }

        protected List<String> findTopics(String value) {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            language = getStateLanguage();
            rootTopic = getStateRootTopic();
            List<String> result = wordProvider.findTopicsWithRoot(language, rootTopic, 2).
                    stream().map(Topic::getName).sorted().collect(Collectors.toList());
            requireActivity().findViewById(R.id.topics_container).setVisibility(result.size() > 1 ? View.VISIBLE : View.GONE);
            return result;
        }

        private String getStateRootTopic() {
            return new ViewModelProvider(requireActivity()).get(RootTopicViewModel.class).getSelected();
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
            StringRecyclerViewAdapter adapter = new StringRecyclerViewAdapter(getStrings(), this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener(
                            viewHolder -> getLanguageViewModel().setSelected(viewHolder.mItem)
                    ));
            if (adapter.mValues.size() == 1) {
                adapter.setSelected(0);
                getLanguageViewModel().setSelected(adapter.mValues.get(0));
            } else {
                String languageFrom = getWordCriteria(requireActivity()).getLanguageFrom();
                adapter.setSelected(adapter.mValues.indexOf(languageFrom));
                getLanguageViewModel().setSelected(languageFrom);
            }
            return adapter;
        }

        private LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }
    }

    public static class LanguageToFragment extends StringsFragment {
        protected String language;

        @Override
        protected List<String> getStrings() {
            language = getStateLanguage();
            return new ArrayList<>(getLanguageTo(language));
        }

        protected String getStateLanguage() {
            return getLanguageViewModel().getSelected();
        }

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
            getLanguageViewModel().getData().observe(requireActivity(), this::updateListData);
        }

        protected void updateListData(String language) {
            StringRecyclerViewAdapter adapter = (StringRecyclerViewAdapter) this.recyclerView.getAdapter();
            if (adapter != null && stateChanged()) {
                adapter.mValues.clear();
                adapter.mValues.addAll(getLanguageTo(language));
                adapter.clearSelection();
            }
        }

        protected boolean stateChanged() {
            return !Objects.equals(language, getStateLanguage());
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
        private final MutableLiveData<String> data = new MutableLiveData<>();

        public LiveData<String> getData() {
            return data;
        }

        public String getSelected() {
            return data.getValue();
        }

        public void setSelected(String item) {
            data.setValue(item);
        }
    }

    public static class RootTopicViewModel extends LanguageViewModel {
    }
}