package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.ActivityGrammarLearningBinding;
import org.leo.dictionary.entity.Sentence;
import org.leo.dictionary.entity.SentenceCriteria;
import org.leo.dictionary.grammar.provider.GrammarProvider;

import java.util.List;

public class GrammarLearningActivity extends AppCompatActivity {
    private ActivityGrammarLearningBinding binding;

    private static SentenceCriteria getGrammarCriteria(Context context) {
        return ((ApplicationWithDI) context.getApplicationContext()).appComponent.grammarCriteriaProvider().getObject();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGrammarLearningBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    public static class SentencesFragment extends RecyclerViewFragment<StringRecyclerViewAdapter<Sentence>, Sentence> {
        @Override
        protected List<Sentence> getValues() {
            GrammarProvider grammarProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalGrammarProvider();
            SentenceCriteria sentenceCriteria = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.grammarCriteriaProvider().getObject();
            return grammarProvider.findSentences(sentenceCriteria);
        }

        public String sentenceToString(Sentence s) {
            StringBuilder builder = new StringBuilder();
            if (!s.getSentencePrefix().isEmpty()) {
                builder.append(s.getSentencePrefix());
                builder.append(' ');
            }
            builder.append(s.getAnswer());
            if (!s.getSentenceSuffix().isEmpty()) {
                builder.append(' ');
                builder.append(s.getSentenceSuffix());
            }
            return builder.toString();
        }

        @Override
        protected StringRecyclerViewAdapter<Sentence> createRecyclerViewAdapter(List<Sentence> values) {
            recyclerView.setNestedScrollingEnabled(false);
            StringRecyclerViewAdapter<Sentence> adapter = new StringRecyclerViewAdapter<>(values, this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>(
                            (oldSelected, viewHolder) -> getLanguageViewModel().setValue(viewHolder.valueToString())
                    ), this::sentenceToString);
            return adapter;
        }

        private LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }
    }
}