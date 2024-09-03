package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.EditTopicFragment;
import org.leo.dictionary.apk.activity.fragment.EditTranslationFragment;
import org.leo.dictionary.apk.activity.fragment.EditWordFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewadapter.TopicRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.EditWordViewModel;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.ActivityEditWordBinding;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EditWordActivity extends AppCompatActivity {

    public static final String WORD_ID_TO_EDIT = "WORD_ID_TO_EDIT";
    public static final String TRANSLATION_INDEX_TO_EDIT = "TRANSLATION_INDEX_TO_EDIT";
    public static final String REQUEST_FOCUS = "REQUEST_FOCUS";
    public static final long DEFAULT_VALUE_OF_WORD_ID = -1L;
    private ActivityEditWordBinding binding;
    private List<Topic> filteredTopics = new ArrayList<>();
    private List<Topic> topics = new ArrayList<>();
    private EditWordViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        model = new ViewModelProvider(this).get(EditWordViewModel.class);
        LanguageViewModel languageViewModel = new ViewModelProvider(this).get(LanguageViewModel.class);
        long id = extrasContainsKey(WORD_ID_TO_EDIT) ? getIntent().getExtras().getLong(WORD_ID_TO_EDIT) : DEFAULT_VALUE_OF_WORD_ID;
        binding = ActivityEditWordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        languageViewModel.getData().observe(this, this::updateTopicListData);
        if (id != DEFAULT_VALUE_OF_WORD_ID) {
            DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
            model.setValue(wordProvider.findWord(id));
        } else {
            model.setNewObject();
        }
        binding.buttonSave.setOnClickListener(v -> {
            if (isValidData()) {
                Word word = model.getValue();
                ((ApplicationWithDI) getApplicationContext()).data.put(MainActivity.UPDATED_WORD, word);
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
        Word word = model.getValue();
        if (word.getTranslations() == null) {
            word.setTranslations(new ArrayList<>());
        }
        if (word.getTopics() == null) {
            word.setTopics(new ArrayList<>());
        }
        List<Translation> translations = word.getTranslations();
        binding.buttonAddTranslation.setOnClickListener(v -> {
            translations.add(new Translation());
            addTranslationUi(translations.size() - 1, true);
        });
        for (int index = 0; index < translations.size(); index++) {
            addTranslationUi(index, false);
        }
        binding.textTopic.addTextChangedListener(new AbstractTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterTopics(s);
            }
        });
        binding.topicList.setLayoutManager(new LinearLayoutManager(binding.topicList.getContext()));
        binding.topicList.setAdapter(new TopicRecyclerViewAdapter(filteredTopics) {
            @Override
            @NonNull
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return createStringViewHolder(parent);
            }

            @Override
            protected void onClickListener(StringRecyclerViewAdapter.StringViewHolder<Topic> viewHolder) {
                addTopicToWord(viewHolder.item);
            }
        });
        binding.createTopic.setOnClickListener(v -> createAndAddTopicToWord());
    }

    private boolean extrasContainsKey(String key) {
        return getIntent().getExtras() != null && getIntent().getExtras().containsKey(key);
    }

    private void addTopicToWord(Topic topic) {
        getWord().getTopics().add(topic);
        updateTopicsFragment();
        filterTopics();
    }

    public void filterTopics() {
        filterTopics(binding.textTopic.getText().toString());
    }

    private void updateTopicsFragment() {
        EditTopicFragment topicFragment = (EditTopicFragment) getSupportFragmentManager().findFragmentById(R.id.edit_word_topics);
        if (topicFragment != null) {
            topicFragment.replaceData(getWord().getTopics());
        }
    }

    private void createAndAddTopicToWord() {
        Topic topic = new Topic();
        topic.setName(binding.textTopic.getText().toString());
        topic.setLevel(2);
        topic.setLanguage(getWord().getLanguage());
        topics.add(topic);
        addTopicToWord(topic);
    }

    private Word getWord() {
        return model.getValue();
    }

    private void filterTopics(CharSequence input) {
        if (input.length() > 0) {
            List<Topic> wordTopics = getWord().getTopics();
            filteredTopics = topics.stream().filter(topic -> topic.getName().contains(input)).filter(topic -> !wordTopics.contains(topic)).collect(Collectors.toList());
        } else {
            filteredTopics = new ArrayList<>();
        }
        ((TopicRecyclerViewAdapter) binding.topicList.getAdapter()).replaceData(filteredTopics);
    }


    private void updateTopicListData(String language) {
        if (topics != null && !topics.isEmpty() && Objects.equals(topics.get(0).getLanguage(), language)) {
            //the same language, do nothing
        } else if (language != null && !language.isEmpty()) {
            topics = findTopics(language);
            filterTopics();
            filterWordTopics(language);
            updateTopicsFragment();
        } else {
            filterWordTopics(language);
            updateTopicsFragment();
        }
    }

    private void filterWordTopics(String language) {
        Word word = getWord();
        word.setTopics(word.getTopics().stream().filter(t -> Objects.equals(t.getLanguage(), language)).collect(Collectors.toList()));
    }

    private List<Topic> findTopics(String language) {
        DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
        return wordProvider.findTopics(language, 2);
    }

    private boolean isValidData() {
        boolean isValid = true;
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof EditWordFragment) {
                isValid &= ((EditWordFragment) fragment).isValid();
            } else if (fragment instanceof EditTranslationFragment) {
                isValid &= ((EditTranslationFragment) fragment).isValid();
            }
        }
        return isValid;
    }

    private void addTranslationUi(int i, boolean requestFocus) {
        Bundle bundle = new Bundle();
        bundle.putInt(TRANSLATION_INDEX_TO_EDIT, i);
        bundle.putBoolean(REQUEST_FOCUS, requestFocus);
        getSupportFragmentManager().beginTransaction().setReorderingAllowed(true).add(R.id.edit_word_translations, EditTranslationFragment.class, bundle).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public static class AbstractTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}