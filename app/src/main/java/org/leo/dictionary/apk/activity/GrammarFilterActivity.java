package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.FilteredRecyclerViewFragment;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.MultiSelectionStringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewadapter.ReturnSelectedStringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.SentenceCriteriaViewModel;
import org.leo.dictionary.apk.databinding.ActivityGrammarFilterBinding;
import org.leo.dictionary.apk.grammar.provider.AssetsGrammarProvider;
import org.leo.dictionary.apk.helper.GrammarProviderHolder;
import org.leo.dictionary.entity.Hint;
import org.leo.dictionary.entity.SentenceCriteria;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.grammar.provider.GrammarProvider;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GrammarFilterActivity extends AppCompatActivity {
    public static final String NOT_CHANGED = "NOT_CHANGED";
    private final ActivityResultLauncher<Intent> assetsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    String string = data.getStringExtra(ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA);
                    SharedPreferences preferences = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState();
                    preferences.edit().putString(ApkModule.LAST_STATE_GRAMMAR_URI, string).apply();
                    GrammarProviderHolder grammarProviderHolder = (GrammarProviderHolder) ((ApplicationWithDI) getApplicationContext()).appComponent.externalGrammarProvider();
                    GrammarProvider grammarProvider = ApkModule.createAssetsGrammarProvider(string, getApplicationContext());
                    grammarProviderHolder.setGrammarProvider(grammarProvider);
                    grammarProvider.findSentences(new SentenceCriteria());
                }
            });
    private ActivityGrammarFilterBinding binding;

    private static void updateViewModelLanguage(ViewModelStoreOwner owner, String language) {
        SentenceCriteriaViewModel sentenceCriteriaViewModel = getSentenceCriteriaViewModel(owner);
        SentenceCriteriaViewModel.SentenceCriteria criteria = sentenceCriteriaViewModel.getValue();
        if (criteria == null) {
            criteria = new SentenceCriteriaViewModel.SentenceCriteria();
        }
        criteria.setLanguage(language);
        criteria.setRootTopic(null);
        criteria.setTopicsOr(null);
        criteria.setHints(null);
        sentenceCriteriaViewModel.setValue(criteria);
    }

    private static void updateViewModelRootTopic(ViewModelStoreOwner owner, Topic rootTopic) {
        SentenceCriteriaViewModel sentenceCriteriaViewModel = getSentenceCriteriaViewModel(owner);
        SentenceCriteriaViewModel.SentenceCriteria criteria = sentenceCriteriaViewModel.getValue();
        if (criteria == null) {
            criteria = new SentenceCriteriaViewModel.SentenceCriteria();
        }
        criteria.setRootTopic(rootTopic);
        criteria.setTopicsOr(null);
        criteria.setHints(null);
        sentenceCriteriaViewModel.setValue(criteria);
    }

    private static void updateViewModelTopics(ViewModelStoreOwner owner, Set<Topic> topicsOr) {
        SentenceCriteriaViewModel sentenceCriteriaViewModel = getSentenceCriteriaViewModel(owner);
        SentenceCriteriaViewModel.SentenceCriteria criteria = sentenceCriteriaViewModel.getValue();
        if (criteria == null) {
            criteria = new SentenceCriteriaViewModel.SentenceCriteria();
        }
        criteria.setTopicsOr(topicsOr);
        criteria.setHints(null);
        sentenceCriteriaViewModel.setValue(criteria);
    }

    private static void updateViewModelHints(ViewModelStoreOwner owner, Set<Hint> hints) {
        SentenceCriteriaViewModel sentenceCriteriaViewModel = getSentenceCriteriaViewModel(owner);
        SentenceCriteriaViewModel.SentenceCriteria criteria = sentenceCriteriaViewModel.getValue();
        if (criteria == null) {
            criteria = new SentenceCriteriaViewModel.SentenceCriteria();
        }
        criteria.setHints(hints);
        sentenceCriteriaViewModel.setValue(criteria);
    }


    private static SentenceCriteriaViewModel getSentenceCriteriaViewModel(ViewModelStoreOwner owner) {
        SentenceCriteriaViewModel sentenceCriteriaViewModel = new ViewModelProvider(owner).get(SentenceCriteriaViewModel.class);
        if (sentenceCriteriaViewModel.getValue() == null) {
            sentenceCriteriaViewModel.getData().setValue(new SentenceCriteriaViewModel.SentenceCriteria());
        }
        return sentenceCriteriaViewModel;
    }

    private static SentenceCriteria getGrammarCriteria(Context context) {
        return ((ApplicationWithDI) context.getApplicationContext()).appComponent.grammarCriteriaProvider().getObject();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GrammarProvider grammarProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.externalGrammarProvider();
        SentenceCriteria original = getGrammarCriteria(this);
        grammarProvider.findSentences(original);

        SentenceCriteriaViewModel viewModel = new ViewModelProvider(this).get(SentenceCriteriaViewModel.class);
        GrammarFilterActivity owner = this;
//        updateViewModel(owner, original.getLanguage(), null, null, null);//TODO fill topics and hints

        binding = ActivityGrammarFilterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.buttonAsset.setOnClickListener(e -> {
            Intent intent = new Intent(owner, AssetsActivity.class);
            Bundle b = new Bundle();
            b.putString(AssetsActivity.FOLDER_NAME, AssetsGrammarProvider.ASSETS_GRAMMAR);
            intent.putExtras(b);
            assetsActivityResultLauncher.launch(intent);
        });

        binding.allRootTopics.setOnClickListener(v -> {
            FilterWordsActivity.RootTopicsFragment topics = (FilterWordsActivity.RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
            if (topics != null) {
                topics.getRecyclerViewAdapter().clearSelection();
                updateViewModelRootTopic(owner, null);
            }
        });
        binding.allTopics.setOnClickListener(v -> {
            FilterWordsActivity.TopicsFragment topics = (FilterWordsActivity.TopicsFragment) getSupportFragmentManager().findFragmentById(R.id.topics);
            if (topics != null) {
                topics.getRecyclerViewAdapter().clearSelection();
                updateViewModelTopics(owner, new HashSet<>());
            }
        });
        binding.allHints.setOnClickListener(v -> {
            FilterWordsActivity.RootTopicsFragment topics = (FilterWordsActivity.RootTopicsFragment) getSupportFragmentManager().findFragmentById(R.id.root_topics);
            if (topics != null) {
                topics.getRecyclerViewAdapter().clearSelection();
                updateViewModelHints(owner, new HashSet<>());
            }
        });
        binding.buttonLearn.setOnClickListener(v -> {
            SentenceCriteria criteria = createCriteria();
            ((ApplicationWithDI) this.getApplicationContext()).appComponent.grammarCriteriaProvider().setObject(criteria);
            Intent intent = new Intent(owner, GrammarLearningActivity.class);
            startActivity(intent);
        });
        binding.buttonPractice.setOnClickListener(v -> {
            SentenceCriteria criteria = createCriteria();
            ((ApplicationWithDI) this.getApplicationContext()).appComponent.grammarCriteriaProvider().setObject(criteria);
            Intent intent = new Intent(this, GrammarCheckActivity.class);
            startActivity(intent);
        });
    }

    private SentenceCriteria createCriteria() {
        SentenceCriteria criteria = new SentenceCriteria();
        SentenceCriteriaViewModel sentenceCriteriaViewModel = getSentenceCriteriaViewModel(this);
        SentenceCriteriaViewModel.SentenceCriteria criteriaModel = sentenceCriteriaViewModel.getValue();
        if (criteriaModel != null) {
            if (criteriaModel.getLanguage() != null) {
                criteria.setLanguage(criteriaModel.getLanguage());
            }
            if (criteriaModel.getRootTopic() != null) {
                criteria.setRootTopic(criteriaModel.getRootTopic().getName());
            }
            if (criteriaModel.getTopicsOr() != null) {
                criteria.setTopicsOr(criteriaModel.getTopicsOr().stream().map(Topic::getName).collect(Collectors.toSet()));
            }
            if (criteriaModel.getHints() != null) {
                criteria.setHints(criteriaModel.getHints().stream().map(Hint::getHint).collect(Collectors.toSet()));
            }
        }
        return criteria;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public static class LanguageFragment extends RecyclerViewFragment<StringRecyclerViewAdapter<String>, String> {
        @Override
        protected List<String> getValues() {
            GrammarProvider grammarProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalGrammarProvider();
            return grammarProvider.languages();
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter(List<String> values) {
            recyclerView.setNestedScrollingEnabled(false);
            StringRecyclerViewAdapter<String> adapter = new StringRecyclerViewAdapter<>(values, this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>(
                            (oldSelected, viewHolder) -> setSelected(viewHolder.valueToString())
                    ));
            requireActivity().findViewById(R.id.language_container).setVisibility(adapter.values.size() > 1 ? View.VISIBLE : View.GONE);
            if (adapter.values.size() == 1) {
                adapter.setSelected(0);
                setSelected(adapter.values.get(0));
            } else {
                String languageFrom = getGrammarCriteria(requireActivity()).getLanguage();
                adapter.setSelected(adapter.values.indexOf(languageFrom));
                setSelected(languageFrom);
            }
            return adapter;
        }

        private void setSelected(String language) {
            updateViewModelLanguage(requireActivity(), language);
        }
    }

    public static class RootTopicsFragment extends FilterWordsActivity.RootTopicsFragment {
        @Override
        protected void addObservers() {
            getSentenceCriteriaViewModel(requireActivity()).getData().observe(requireActivity(), this::updateListData);
        }

        @Override
        protected List<Topic> findValues() {
            GrammarProvider grammarProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalGrammarProvider();
            language = getStateLanguage();
            List<Topic> result = grammarProvider.findTopics(language, null, 1);
            setContainerVisibility(R.id.root_topics_container, result);
            setTopicValue(null);
            return result;
        }

        @Override
        protected String getStateLanguage() {
            return getSentenceCriteriaViewModel(requireActivity()).getValue().getLanguage();
        }

        @Override
        protected void setTopicValue(Topic rootTopic) {
            updateViewModelRootTopic(requireActivity(), rootTopic);
        }
    }

    public static class TopicsFragment extends FilterWordsActivity.TopicsFragment {
        @Override
        protected void addObservers() {
            getSentenceCriteriaViewModel(requireActivity()).getData().observe(requireActivity(), this::updateListData);
        }

        @Override
        protected List<Topic> findValues() {
            GrammarProvider wordProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalGrammarProvider();
            language = getStateLanguage();
            rootTopic = getStateRootTopic();
            List<Topic> result = wordProvider.findTopics(language, rootTopic != null ? rootTopic.getName() : null, 2);
            setContainerVisibility(R.id.topics_container, result);
            return result;
        }

        @Override
        protected Topic getStateRootTopic() {
            return getSentenceCriteriaViewModel(requireActivity()).getValue().getRootTopic();
        }

        @Override
        protected void setSelectedValues(StringRecyclerViewAdapter<Topic> adapter) {
            super.setSelectedValues(adapter);
            updateViewModelTopics(requireActivity(), new HashSet<>(((MultiSelectionStringRecyclerViewAdapter<Topic>) adapter).getSelectedList()));
        }

        @Override
        protected List<Topic> getTopicsFromCriteria(List<Topic> topicList) {
            Set<String> topicsOr = getGrammarCriteria(requireActivity()).getTopicsOr();
            return topicsOr != null ? topicList.stream().filter(topic -> topicsOr.contains(topic.getName())).collect(Collectors.toList()) : Collections.emptyList();
        }

        @Override
        protected Consumer<Collection<Topic>> getOnClickUpdater() {
            return topics -> updateViewModelTopics(requireActivity(), new HashSet<>(topics));
        }
    }

    public static class HintsFragment extends FilteredRecyclerViewFragment<StringRecyclerViewAdapter<Hint>, Hint> {
        protected String language;
        protected Topic rootTopic;
        private Set<Topic> topics;

        @Override
        protected void addObservers() {
            getSentenceCriteriaViewModel(requireActivity()).getData().observe(requireActivity(), this::updateListData);
        }

        @Override
        protected Function<Hint, String> getFormatter() {
            return Hint::getHint;
        }

        @Override
        protected boolean stateChanged() {
            return !Objects.equals(language, getStateLanguage()) || !Objects.equals(rootTopic, getStateRootTopic()) || !Objects.equals(topics, getStateTopics());
        }

        @Override
        protected List<Hint> findValues() {
            GrammarProvider grammarProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalGrammarProvider();
            language = getStateLanguage();
            rootTopic = getStateRootTopic();
            topics = getStateTopics();
            List<Hint> result = grammarProvider.findHints(language, rootTopic == null ? null : rootTopic.getName(), topics == null ? null : topics.stream().map(Topic::getName).collect(Collectors.toSet()));
            setContainerVisibility(R.id.hints_container, result);
            return result;
        }

        @Override
        protected StringRecyclerViewAdapter<Hint> createRecyclerViewAdapter(List<Hint> values) {
            recyclerView.setNestedScrollingEnabled(false);
            MultiSelectionStringRecyclerViewAdapter<Hint> adapter = new MultiSelectionStringRecyclerViewAdapter<>(values, this, getOnClickUpdater(), Hint::getHint);
            List<Hint> selected = getHintsFromCriteria(values);
            adapter.setSelected(selected);
            return adapter;
        }

        private Consumer<Collection<Hint>> getOnClickUpdater() {
            return hints -> updateViewModelHints(requireActivity(), new HashSet<>(hints));
        }

        protected List<Hint> getHintsFromCriteria(List<Hint> hintList) {
            Set<String> hints = getGrammarCriteria(requireActivity()).getHints();
            return hints != null ? hintList.stream().filter(hint -> hints.contains(hint.getHint())).collect(Collectors.toList()) : Collections.emptyList();
        }


        protected String getStateLanguage() {
            return getSentenceCriteriaViewModel(requireActivity()).getValue().getLanguage();
        }

        protected Topic getStateRootTopic() {
            return getSentenceCriteriaViewModel(requireActivity()).getValue().getRootTopic();
        }

        protected Set<Topic> getStateTopics() {
            return getSentenceCriteriaViewModel(requireActivity()).getValue().getTopicsOr();
        }
    }
}