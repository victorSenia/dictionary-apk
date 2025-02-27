package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.FilteredRecyclerViewFragment;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.MultiSelectionStringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.WordCriteriaViewModel;
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


    private static WordCriteria getWordCriteria(Context context) {
        WordCriteriaProvider wordCriteriaProvider = ((ApplicationWithDI) context.getApplicationContext()).appComponent.wordCriteriaProvider();
        return wordCriteriaProvider.getObject();
    }

    private void updateCount(WordCriteriaViewModel.WordCriteria criteria) {
        ExternalWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.externalWordProvider();
        int count = wordProvider.countWords(createCriteria(criteria));
        runOnUiThread(() -> {
            Button findWords = findViewById(R.id.find_words);
            findWords.setText(getString(R.string.find_words_count, count));
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WordCriteriaViewModel wordCriteriaViewModel = new ViewModelProvider(this).get(WordCriteriaViewModel.class);
        wordCriteriaViewModel.getData().observe(this, this::updateCount);
        FilterWordsActivityBinding binding = FilterWordsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        binding.findWords.setOnClickListener(v -> {
            Intent intent = new Intent();
            WordCriteria criteria = createCriteria(wordCriteriaViewModel.getValue());
            WordCriteriaProvider criteriaProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider();
            criteriaProvider.setObject(criteria);
            setResult(Activity.RESULT_OK, intent);
            finish();
        });
        binding.allTopics.setOnClickListener(v -> {
            TopicsFragment topics = (TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
            if (topics != null) {
                topics.getRecyclerViewAdapter().clearSelection();
                wordCriteriaViewModel.getValue().setTopicsOr(null);
                wordCriteriaViewModel.triggerUpdate();
            }
        });
        binding.allRootTopics.setOnClickListener(v -> {
            RootTopicsFragment topics = (RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
            if (topics != null) {
                topics.getRecyclerViewAdapter().clearSelection();
                wordCriteriaViewModel.getValue().setRootTopic(null);
                wordCriteriaViewModel.getValue().setTopicsOr(null);
                wordCriteriaViewModel.triggerUpdate();
            }
        });
        WordCriteria wordCriteria = getWordCriteria(this);
        if (wordCriteria.getShuffleRandom() != -1) {
            binding.shuffle.setChecked(true);
        }
        wordCriteriaViewModel.setValue(createCriteriaViewModel(wordCriteria));
        binding.knowledgeRangeSlider.setValues(wordCriteriaViewModel.getValue().getKnowledge());
        binding.knowledgeRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                wordCriteriaViewModel.getValue().setKnowledge(slider.getValues());
                wordCriteriaViewModel.triggerUpdate();
            }
        });
        ApkAppComponent appComponent = ((ApplicationWithDI) getApplicationContext()).appComponent;
        List<String> languagesFrom = appComponent.externalWordProvider().languageFrom();
        if (!ApkModule.isDBSource(appComponent.lastState()) || languagesFrom.isEmpty()) {
            binding.languageFromContainer.setVisibility(View.GONE);
        } else if (languagesFrom.size() == 1) {
            if (!languagesFrom.get(0).equals(wordCriteriaViewModel.getValue().getLanguageFrom())) {
                wordCriteriaViewModel.getValue().setLanguageFrom(languagesFrom.get(0));
                wordCriteriaViewModel.triggerUpdate();
            }
            binding.languageFromContainer.setVisibility(View.GONE);
        }
        TextView text = binding.languagesToLabel;
        if (!ApkModule.isDBSource(appComponent.lastState())) {
            text.setText(R.string.languages_to_speak);
        } else {
            text.setText(R.string.languages_to);
        }
    }

    private WordCriteriaViewModel.WordCriteria createCriteriaViewModel(WordCriteria wordCriteria) {
        WordCriteriaViewModel.WordCriteria criteriaViewModel = new WordCriteriaViewModel.WordCriteria();
        criteriaViewModel.setLanguageFrom(wordCriteria.getLanguageFrom());
        criteriaViewModel.setLanguageTo(wordCriteria.getLanguageTo());
        List<Float> knowledgeRange = new ArrayList<>(2);
        knowledgeRange.add(wordCriteria.getKnowledgeFrom() != null ? KnowledgeToRatingConverter.knowledgeToRating(wordCriteria.getKnowledgeFrom()) : 0.0F);
        knowledgeRange.add(wordCriteria.getKnowledgeTo() != null ? KnowledgeToRatingConverter.knowledgeToRating(wordCriteria.getKnowledgeTo()) : KnowledgeToRatingConverter.starsCount);
        criteriaViewModel.setKnowledge(knowledgeRange);
        return criteriaViewModel;
    }

    private WordCriteria createCriteria(WordCriteriaViewModel.WordCriteria criteriaModel) {
        WordCriteria criteria = new WordCriteria();
        SwitchCompat shuffle = findViewById(R.id.shuffle);
        if (shuffle.isChecked()) {
            criteria.setShuffleRandom(System.currentTimeMillis());
        }
        Set<Topic> selectedTopics = criteriaModel.getTopicsOr();
        if (selectedTopics != null) {
            criteria.setTopicsOr(selectedTopics);
        }
        Set<String> selectedLanguageTo = criteriaModel.getLanguageTo();
        if (selectedLanguageTo != null) {
            criteria.setLanguageTo(selectedLanguageTo);
        }
        String selectedLanguageFrom = criteriaModel.getLanguageFrom();
        if (selectedLanguageFrom != null) {
            criteria.setLanguageFrom(selectedLanguageFrom);
        }
        Set<Topic> selectedRootTopic = criteriaModel.getRootTopic();
        if (selectedRootTopic != null) {
            if (criteria.getLanguageFrom() == null && selectedRootTopic.size() == 1) {
                criteria.setLanguageFrom(selectedRootTopic.iterator().next().getLanguage());
            }
            criteria.setRootTopics(selectedRootTopic);
        }
        List<Float> range = criteriaModel.getKnowledge();
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

        protected String getStateLanguage() {
            return getWordCriteriaViewModel().getValue().getLanguageFrom();
        }

        protected WordCriteriaViewModel getWordCriteriaViewModel() {
            return new ViewModelProvider(requireActivity()).get(WordCriteriaViewModel.class);
        }

        protected void setTopicValue(Topic rootTopic) {
            if (rootTopic != null) {
                getWordCriteriaViewModel().getValue().setRootTopic(Collections.singleton(rootTopic));
                getWordCriteriaViewModel().getValue().setTopicsOr(null);
                getWordCriteriaViewModel().triggerUpdate();
            }
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
            return topics -> {
                getWordCriteriaViewModel().getValue().setRootTopic(new HashSet<>(topics));
                getWordCriteriaViewModel().getValue().setTopicsOr(null);
                getWordCriteriaViewModel().triggerUpdate();
            };
        }

        protected List<Topic> getTopicsFromCriteria(List<Topic> topicList) {
            Set<Long> topicIds = MainActivity.getTopicIds(getWordCriteria(requireActivity()).getRootTopics());
            return topicIds != null ? topicList.stream().filter(topic -> topicIds.contains(topic.getId())).collect(Collectors.toList()) : Collections.emptyList();
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
            getWordCriteriaViewModel().getData().observe(requireActivity(), this::updateListData);
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
        protected Set<Topic> rootTopic;

        @Override
        protected boolean stateChanged() {
            return super.stateChanged() || !Objects.equals(rootTopic, getStateRootTopic());
        }

        @Override
        protected List<Topic> findValues() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            language = getStateLanguage();
            rootTopic = getStateRootTopic();
            if (language == null && rootTopic != null && rootTopic.size() == 1) {
                language = rootTopic.iterator().next().getLanguage();
            }
            List<Topic> result = wordProvider.findTopicsWithRoot(language, rootTopic, 2);
            setContainerVisibility(R.id.topics_container, result);
            return result;
        }

        @Override
        protected void setSelectedValues(StringRecyclerViewAdapter<Topic> adapter) {
            ((MultiSelectionStringRecyclerViewAdapter<Topic>) adapter).setSelected(getTopicsFromCriteria(adapter.values));
        }

        protected Set<Topic> getStateRootTopic() {
            return getWordCriteriaViewModel().getValue().getRootTopic();
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
            return topics -> {
                getWordCriteriaViewModel().getValue().setTopicsOr(new HashSet<>(topics));
                getWordCriteriaViewModel().triggerUpdate();
            };
        }


        protected List<Topic> getTopicsFromCriteria(List<Topic> topicList) {
            Set<Long> topicsOr = MainActivity.getTopicIds(getWordCriteria(requireActivity()).getTopicsOr());
            return topicsOr != null ? topicList.stream().filter(topic -> topicsOr.contains(topic.getId())).collect(Collectors.toList()) : Collections.emptyList();
        }
    }

    public static class LanguageFromFragment extends RecyclerViewFragment<StringRecyclerViewAdapter<String>, String> {
        @Override
        protected List<String> getValues() {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            return wordProvider.languageFrom();
        }

        private WordCriteriaViewModel getWordCriteriaViewModel() {
            return new ViewModelProvider(requireActivity()).get(WordCriteriaViewModel.class);
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
            return getWordCriteriaViewModel().getValue().getLanguageFrom();
        }

        protected void setLanguage(String language) {
            if (!Objects.equals(language, getWordCriteriaViewModel().getValue().getLanguageFrom())) {
                getWordCriteriaViewModel().getValue().setLanguageFrom(language);
                getWordCriteriaViewModel().getValue().setRootTopic(null);
                getWordCriteriaViewModel().getValue().setTopicsOr(null);
                getWordCriteriaViewModel().triggerUpdate();
            }
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
            return getWordCriteriaViewModel().getValue().getLanguageFrom();
        }

        private WordCriteriaViewModel getWordCriteriaViewModel() {
            return new ViewModelProvider(requireActivity()).get(WordCriteriaViewModel.class);
        }

        private List<String> getLanguageTo(String language) {
            ExternalWordProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalWordProvider();
            List<String> languagesTo = wordProvider.languageTo(language);
            requireActivity().findViewById(R.id.languages_to_container).setVisibility(languagesTo.size() > 1 ? View.VISIBLE : View.GONE);
            return languagesTo;
        }

        @Override
        protected void addObservers() {
            getWordCriteriaViewModel().getData().observe(requireActivity(), this::updateListData);
        }

        protected void updateListData(Object criteria) {
            StringRecyclerViewAdapter<String> adapter = getRecyclerViewAdapter();
            if (adapter != null && stateChanged()) {
                adapter.clearAdapter();
                getWordCriteriaViewModel().getValue().setLanguageTo(null);
                getWordCriteriaViewModel().triggerUpdate();
                language = getStateLanguage();
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
            MultiSelectionStringRecyclerViewAdapter<String> adapter = new MultiSelectionStringRecyclerViewAdapter<>(values, this, getOnClickUpdater());
            adapter.setSelected(getWordCriteriaViewModel().getValue().getLanguageTo());
            return adapter;
        }

        protected Consumer<Collection<String>> getOnClickUpdater() {
            return languages -> {
                getWordCriteriaViewModel().getValue().setLanguageTo(new HashSet<>(languages));
                getWordCriteriaViewModel().triggerUpdate();
            };
        }
    }

}