package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import org.jetbrains.annotations.NotNull;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.FilteredRecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchWordsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_words);
    }

    public static class FilteredWordsFragment extends FilteredRecyclerViewFragment<StringRecyclerViewAdapter<Word>, Word> {

        @NotNull
        private static Function<String, Boolean> getFilterFunction(CharSequence filterString) {
            Function<String, Boolean> predicate;
            try {
                Pattern pattern = Pattern.compile(filterString.toString(), Pattern.CASE_INSENSITIVE);
                predicate = s -> pattern.matcher(s).find();
            } catch (PatternSyntaxException e) {
                String filter = filterString.toString();
                predicate = s -> s.contains(filter);
            }
            return predicate;
        }

        protected List<Word> findValues() {
            PlayService playService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.playService();
            return playService.getUnknownWords();
        }

        @Override
        protected StringRecyclerViewAdapter<Word> createRecyclerViewAdapter(List<Word> values) {
            return new StringRecyclerViewAdapter<>(values, this, new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>((oldSelected, viewHolder) -> setWordValue(viewHolder.item)), getFormatter());
        }

        private void setWordValue(Word item) {
        }

        @Override
        protected Predicate<Word> filterPredicate(CharSequence filterString) {
            Function<String, Boolean> filterFunction = getFilterFunction(filterString);
            return w -> {
                if (filterFunction.apply(w.getWord())) {
                    return true;
                }
                for (Translation t : w.getTranslations()) {
                    if (filterFunction.apply(t.getTranslation())) {
                        return true;
                    }
                }
                return false;
            };
        }

        @Override
        protected Function<Word, String> getFormatter() {
            return Word::formatWord;
        }

        @Override
        protected boolean stateChanged() {
            return false;
        }
    }
}