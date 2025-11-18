package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.ActivityGrammarLearningBinding;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.entity.GrammarCriteria;
import org.leo.dictionary.entity.GrammarSentence;
import org.leo.dictionary.grammar.provider.GrammarProvider;

import java.util.List;
import java.util.function.BiConsumer;

public class GrammarLearningActivity extends AppCompatActivity {
    private ActivityGrammarLearningBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGrammarLearningBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);
        ActivityUtils.setFullScreen(this, root);
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
            builder.setCharAt(0, Character.toUpperCase(builder.charAt(0)));
            return builder.toString();
        }

        @Override
        protected StringRecyclerViewAdapter<GrammarSentence> createRecyclerViewAdapter(List<GrammarSentence> values) {
            recyclerView.setNestedScrollingEnabled(false);
            BiConsumer<Integer, StringRecyclerViewAdapter.StringViewHolder<GrammarSentence>> additionalOnClickHandling =
                    (oldSelected, viewHolder) -> {
                        String string = viewHolder.valueToString();
                        getLanguageViewModel().setValue(string);
                        String language = viewHolder.item.getLanguage();
                        AudioService audioService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.audioService();
                        ApkModule.playAsynchronousIfPossible(audioService, language, string);
                    };
            return new StringRecyclerViewAdapter<>(values, this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>(additionalOnClickHandling), this::sentenceToString);
        }

        private LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }
    }
}