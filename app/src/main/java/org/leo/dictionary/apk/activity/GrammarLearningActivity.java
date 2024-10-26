package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.ActivityGrammarLearningBinding;
import org.leo.dictionary.entity.GrammarCriteria;
import org.leo.dictionary.entity.GrammarSentence;
import org.leo.dictionary.grammar.provider.GrammarProvider;

import java.util.List;

public class GrammarLearningActivity extends AppCompatActivity {
    private ActivityGrammarLearningBinding binding;

    private static GrammarCriteria getGrammarCriteria(Context context) {
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

    public static class SentencesFragment extends RecyclerViewFragment<StringRecyclerViewAdapter<GrammarSentence>, GrammarSentence> {
        @Override
        protected List<GrammarSentence> getValues() {
            GrammarProvider grammarProvider = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.externalGrammarProvider();
            GrammarCriteria grammarCriteria = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.grammarCriteriaProvider().getObject();
            return grammarProvider.findSentences(grammarCriteria);
        }

        public String sentenceToString(GrammarSentence s) {
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
        protected StringRecyclerViewAdapter<GrammarSentence> createRecyclerViewAdapter(List<GrammarSentence> values) {
            recyclerView.setNestedScrollingEnabled(false);
            return new StringRecyclerViewAdapter<>(values, this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>(
                            (oldSelected, viewHolder) -> getLanguageViewModel().setValue(viewHolder.valueToString())
                    ), this::sentenceToString);
        }

        private LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }
    }
}