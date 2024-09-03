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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.slider.RangeSlider;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.FilteredRecyclerViewFragment;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.MultiSelectionStringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.activity.viewmodel.TopicViewModel;
import org.leo.dictionary.apk.databinding.FilterWordsActivityBinding;
import org.leo.dictionary.apk.helper.KnowledgeToRatingConverter;
import org.leo.dictionary.apk.helper.WordCriteriaProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.WordCriteria;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FilterWordsActivity extends AppCompatActivity {

    public static final int RANGE_FROM_INDEX = 0;
    public static final int RANGE_TO_INDEX = 1;

    private static <T> List<T> getSelected(RecyclerViewFragment<StringRecyclerViewAdapter<T>, T> strings) {
        if (strings != null && strings.getRecyclerViewAdapter() instanceof MultiSelectionStringRecyclerViewAdapter) {
            MultiSelectionStringRecyclerViewAdapter<T> adapter = (MultiSelectionStringRecyclerViewAdapter<T>) strings.getRecyclerViewAdapter();
            if (!adapter.getSelectedList().isEmpty()) {
                return adapter.getSelectedList();
            }
        } else if (strings != null) {
            StringRecyclerViewAdapter<T> adapter = strings.getRecyclerViewAdapter();
            if (adapter.getSelected() != RecyclerView.NO_POSITION) {
                return Collections.singletonList(adapter.values.get(adapter.getSelected()));
            }
        }
        return null;
    }

    private static WordCriteria getWordCriteria(Context context) {
        WordCriteriaProvider wordCriteriaProvider = ((ApplicationWithDI) context.getApplicationContext()).appComponent.wordCriteriaProvider();
        return wordCriteriaProvider.getObject();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageViewModel languageViewModel = new ViewModelProvider(this).get(LanguageViewModel.class);
        TopicViewModel rootTopicViewModel = new ViewModelProvider(this).get(TopicViewModel.class);
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
            criteriaProvider.setObject(criteria);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        binding.allTopics.setOnClickListener(v -> {
            TopicsFragment topics = (TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
            if (topics != null) {
                topics.getRecyclerViewAdapter().clearSelection();
            }
        });
        binding.allRootTopics.setOnClickListener(v -> {
            RootTopicsFragment topics = (RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
            if (topics != null) {
                topics.getRecyclerViewAdapter().clearSelection();
                rootTopicViewModel.postValue(null);
            }
        });
        WordCriteria wordCriteria = getWordCriteria(this);
        if (wordCriteria.getShuffleRandom() != -1) {
            binding.shuffle.setChecked(true);
        }
        List<Float> knowledgeRange = new ArrayList<>(2);
        knowledgeRange.add(wordCriteria.getKnowledgeFrom() != null ? KnowledgeToRatingConverter.knowledgeToRating(wordCriteria.getKnowledgeFrom()) : 0.0F);
        knowledgeRange.add(wordCriteria.getKnowledgeTo() != null ? KnowledgeToRatingConverter.knowledgeToRating(wordCriteria.getKnowledgeTo()) : KnowledgeToRatingConverter.starsCount);
        binding.knowledgeRangeSlider.setValues(knowledgeRange);
        ApkAppComponent appComponent = ((ApplicationWithDI) getApplicationContext()).appComponent;
        List<String> languagesFrom = appComponent.externalWordProvider().languageFrom();
        if (!ApkModule.isDBSource(appComponent.lastState()) || languagesFrom.isEmpty()) {
            binding.languageFromContainer.setVisibility(View.GONE);
        } else if (languagesFrom.size() == 1) {
            languageViewModel.setValue(languagesFrom.get(0));
            binding.languageFromContainer.setVisibility(View.GONE);
        }
        TextView text = binding.languagesToLabel;
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
        RangeSlider knowledgeRange = findViewById(R.id.knowledge_range_slider);
        if (shuffle.isChecked()) {
            criteria.setShuffleRandom(System.currentTimeMillis());
        }
        List<Topic> selectedTopics = getSelected(topics);
        if (selectedTopics != null) {
            criteria.setTopicsOr(selectedTopics.stream().map(Topic::getName).collect(Collectors.toList()));
        }
        List<String> selectedLanguageTo = getSelected(languageTo);
        if (selectedLanguageTo != null) {
            criteria.setLanguageTo(new HashSet<>(selectedLanguageTo));
        }
        List<String> selectedLanguageFrom = getSelected(languageFrom);
        if (selectedLanguageFrom != null) {
            criteria.setLanguageFrom(selectedLanguageFrom.get(0));
        }
        List<Topic> selectedRootTopic = getSelected(rootTopics);
        if (selectedRootTopic != null) {
            criteria.setRootTopic(selectedRootTopic.get(0).getName());
        }
        List<Float> range = knowledgeRange.getValues();
        double knowledgeFrom = KnowledgeToRatingConverter.ratingToKnowledge(range.get(RANGE_FROM_INDEX));
        if (knowledgeFrom > 0.00001) {
            criteria.setKnowledgeFrom(knowledgeFrom);
        }
        double knowledgeTo = KnowledgeToRatingConverter.ratingToKnowledge(range.get(RANGE_TO_INDEX));
        if (knowledgeTo < 0.99999) {
            criteria.setKnowledgeTo(knowledgeTo);
        }
        return criteria;
    }

    public static class RootTopicsFragment extends FilteredRecyclerViewFragment<StringRecyclerViewAdapter<Topic>, Topic> {
        protected String language;

        protected LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }

        protected String getStateLanguage() {
            return getLanguageViewModel().getValue();
        }

        protected void setTopicValue(Topic rootTopic) {
            getTopicViewModel().postValue(rootTopic);
        }

        @Override
        protected StringRecyclerViewAdapter<Topic> createRecyclerViewAdapter(List<Topic> values) {
            recyclerView.setNestedScrollingEnabled(false);
            StringRecyclerViewAdapter<Topic> adapter = new StringRecyclerViewAdapter<>(values, this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>(
                            (oldSelected, viewHolder) -> setTopicValue(viewHolder.item)),
                    getFormatter());
            if (adapter.values.size() == 1) {
                adapter.setSelected(0);
                setTopicValue(adapter.values.get(0));
            } else {
                String rootTopicName = getWordCriteria(requireActivity()).getRootTopic();
                if (rootTopicName != null && !rootTopicName.isEmpty()) {
                    Topic rootTopic = adapter.values.stream().filter(t -> rootTopicName.equals(t.getName())).findAny().orElse(null);
                    if (rootTopic != null) {
                        adapter.setSelected(adapter.values.indexOf(rootTopic));
                        setTopicValue(rootTopic);
                    }
                }
            }
            return adapter;
        }

        protected TopicViewModel getTopicViewModel() {
            return new ViewModelProvider(requireActivity()).get(TopicViewModel.class);
        }

        @Override
        protected List<Topic> findValues() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            language = getStateLanguage();
            List<Topic> result = wordProvider.findTopics(language, 1);
            setContainerVisibility(R.id.root_topics_container, result);
            setTopicValue(null);
            return result;
        }

        @Override
        protected void addObservers() {
            getLanguageViewModel().getData().observe(requireActivity(), this::updateListData);
        }

        @Override
        protected Function<Topic, String> getFormatter() {
            return Topic::getName;
        }

        @Override
        protected boolean stateChanged() {
            return !Objects.equals(language, getStateLanguage());
        }
    }

    public static class TopicsFragment extends RootTopicsFragment {
        protected Topic rootTopic;

        @Override
        protected void addObservers() {
            super.addObservers();
            getTopicViewModel().getData().observe(requireActivity(), this::updateListData);
        }

        @Override
        protected boolean stateChanged() {
            return super.stateChanged() || !Objects.equals(rootTopic, getStateRootTopic());
        }

        @Override
        protected List<Topic> findValues() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            language = getStateLanguage();
            rootTopic = getStateRootTopic();
            List<Topic> result = wordProvider.findTopicsWithRoot(language, rootTopic != null ? rootTopic.getName() : null, 2);
            setContainerVisibility(R.id.topics_container, result);
            return result;
        }

        @Override
        protected void setSelectedValues(StringRecyclerViewAdapter<Topic> adapter) {
            ((MultiSelectionStringRecyclerViewAdapter<Topic>) adapter).setSelected(getTopicsFromCriteria(adapter.values));
        }

        protected Topic getStateRootTopic() {
            return getTopicViewModel().getValue();
        }

        @Override
        protected StringRecyclerViewAdapter<Topic> createRecyclerViewAdapter(List<Topic> values) {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter<Topic> adapter = new MultiSelectionStringRecyclerViewAdapter<>(values, this, getOnClickUpdater(), getFormatter());
            List<Topic> selected = getTopicsFromCriteria(values);
            adapter.setSelected(selected);
            return adapter;
        }

        protected Consumer<Collection<Topic>> getOnClickUpdater() {
            return null;
        }

        protected List<Topic> getTopicsFromCriteria(List<Topic> topicList) {
            Set<String> topicsOr = getWordCriteria(requireActivity()).getTopicsOr();
            return topicsOr != null ? topicList.stream().filter(topic -> topicsOr.contains(topic.getName())).collect(Collectors.toList()) : Collections.emptyList();
        }
    }

    public static class LanguageFromFragment extends RecyclerViewFragment<StringRecyclerViewAdapter<String>, String> {
        @Override
        protected List<String> getValues() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            return wordProvider.languageFrom();
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter(List<String> values) {
            recyclerView.setNestedScrollingEnabled(false);
            StringRecyclerViewAdapter<String> adapter = new StringRecyclerViewAdapter<>(values, this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>(
                            (oldSelected, viewHolder) -> setLanguage(viewHolder.valueToString())
                    ));
            if (adapter.values.size() == 1) {
                adapter.setSelected(0);
                setLanguage(adapter.values.get(0));
            } else {
                String languageFrom = getLanguage();
                adapter.setSelected(adapter.values.indexOf(languageFrom));
                setLanguage(languageFrom);
            }
            return adapter;
        }

        protected String getLanguage() {
            return getWordCriteria(requireActivity()).getLanguageFrom();
        }

        protected void setLanguage(String viewHolder) {
            new ViewModelProvider(requireActivity()).get(LanguageViewModel.class).setValue(viewHolder);
        }
    }

    public static class LanguageToFragment extends RecyclerViewFragment<StringRecyclerViewAdapter<String>, String> {
        protected String language;

        @Override
        protected List<String> getValues() {
            language = getStateLanguage();
            return new ArrayList<>(getLanguageTo(language));
        }

        protected String getStateLanguage() {
            return getLanguageViewModel().getValue();
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
        protected void addObservers() {
            getLanguageViewModel().getData().observe(requireActivity(), this::updateListData);
        }

        protected void updateListData(String language) {
            StringRecyclerViewAdapter<String> adapter = getRecyclerViewAdapter();
            if (adapter != null && stateChanged()) {
                adapter.clearAdapter();
                adapter.values.addAll(getLanguageTo(language));
                adapter.notifyDataSetChanged();
            }
        }

        protected boolean stateChanged() {
            return !Objects.equals(language, getStateLanguage());
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter(List<String> values) {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter<String> adapter = new MultiSelectionStringRecyclerViewAdapter<>(values, this, null);
            adapter.setSelected(getWordCriteria(requireActivity()).getLanguageTo());
            return adapter;
        }
    }

}