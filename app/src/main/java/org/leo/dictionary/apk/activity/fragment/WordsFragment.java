package org.leo.dictionary.apk.activity.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApkUiUpdater;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.viewadapter.WordsRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.IsPlayingViewModel;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class WordsFragment extends FilteredRecyclerViewFragment<WordsRecyclerViewAdapter, Word> {

    public static final long INACTIVITY_TIMEOUT = 3000;
    private final AtomicBoolean scrollAllowed = new AtomicBoolean(true);
    private final Runnable scrollAllowedRunnable = () -> scrollAllowed.set(true);
    private UiUpdater uiUpdater;
    private Handler mHandler;
    private boolean updatePlayer = true;

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

    @Override
    protected void addObservers() {
        super.addObservers();
        new ViewModelProvider(requireActivity()).get(IsPlayingViewModel.class).getData().
                observe(requireActivity(), b -> setFilterVisibility());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        mHandler = null;
        super.onDestroy();
    }

    public void replaceData() {
        updateListData(null);
        recyclerView.scrollToPosition(0);
    }

    public void wordUpdated(int index) {
        getRecyclerViewAdapter().notifyItemChanged(index);
    }

    public void wordAdded(int index, Word word) {
        getRecyclerViewAdapter().values.add(word);
        getRecyclerViewAdapter().notifyItemInserted(index);
    }

    public void wordDeleted(int index) {
        getRecyclerViewAdapter().values.remove(index);
        getRecyclerViewAdapter().notifyItemRemoved(index);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                stopStartHandler();
                super.onScrolled(recyclerView, dx, dy);
            }
        });
        recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                stopStartHandler();
                return super.onInterceptTouchEvent(rv, e);
            }
        });

        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.uiUpdater();
        uiUpdater = (word, index) -> {
            if (scrollAllowed.get()) {
                requireActivity().runOnUiThread(() -> recyclerView.scrollToPosition(index));
            }
        };
        apkUiUpdater.addUiUpdater(uiUpdater);
        if (savedInstanceState == null || !isPlayServiceSetUp()) {
            replaceData();
        } else {
            try {
                updatePlayer = false;
                replaceData();
            } finally {
                updatePlayer = true;
            }
        }
        recyclerView.scrollToPosition(getCurrentIndex());
        return view;
    }

    private boolean isPlayServiceSetUp() {
        PlayService playService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.playService();
        return playService != null && (playService.isPlaying() || playService.isReady());
    }

    @Override
    protected Predicate<Word> filterPredicate(CharSequence filterString) {
        String suffix = "\\t";
        int articleSuffixIndex = filterString.toString().indexOf(suffix);
        boolean article = articleSuffixIndex != -1;
        CharSequence articleSequence = !article ? "" : filterString.subSequence(0, articleSuffixIndex);
        CharSequence sequence = !article ? filterString : filterString.subSequence(articleSuffixIndex + suffix.length(), filterString.length());
        Function<String, Boolean> filterFunction = getFilterFunction(sequence);
        Function<String, Boolean> articleFilterFunction = getFilterFunction(articleSequence);
        return w -> {
            if (article) {
                if (articleSequence.length() == 0) {
                    return (w.getArticle() == null || w.getArticle().isEmpty()) && (sequence.length() == 0 || wordOrTranslationMatch(filterFunction, w));
                }
                String s = w.getArticle() == null ? "" : w.getArticle();
                return articleFilterFunction.apply(s) && (sequence.length() == 0 || wordOrTranslationMatch(filterFunction, w));
            }
            return wordOrTranslationMatch(filterFunction, w);
        };
    }

    private static boolean wordOrTranslationMatch(Function<String, Boolean> filterFunction, Word w) {
        if (filterFunction.apply(w.getWord())) {
            return true;
        }
        for (Translation t : w.getTranslations()) {
            if (filterFunction.apply(t.getTranslation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void filterValuesInAdapter() {
        List<Word> words = filterValues();
        if (updatePlayer) {
            ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.playService().setWords(words);
        }
        WordsRecyclerViewAdapter adapter = getRecyclerViewAdapter();
        adapter.values.clear();
        adapter.values.addAll(words);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected Function<Word, String> getFormatter() {
        return Word::formatWord;
    }

    @Override
    protected boolean stateChanged() {
        return true;
    }

    protected List<Word> findValues() {
        return ApkModule.getWords(requireActivity());
    }

    @Override
    protected WordsRecyclerViewAdapter createRecyclerViewAdapter(List<Word> values) {
        return new WordsRecyclerViewAdapter(values, this, getCurrentIndex());
    }

    @Override
    protected List<Word> getValues() {
        return new ArrayList<>();
    }

    private int getCurrentIndex() {
        return ApkModule.getLastStateCurrentIndex(((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.lastState());
    }

    private void stopStartHandler() {
        stopHandler();
        startHandler();
    }

    public void updateKnowledge(double knowledge) {
        allValues.forEach(word -> word.setKnowledge(knowledge));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.uiUpdater();
        apkUiUpdater.removeUiUpdater(uiUpdater);
    }

    @Override
    protected boolean isFilterVisible() {
        return !new ViewModelProvider(requireActivity()).get(IsPlayingViewModel.class).getValue()
                && super.isFilterVisible();
    }

    private void startHandler() {
        mHandler.postDelayed(scrollAllowedRunnable, INACTIVITY_TIMEOUT);
    }

    private void stopHandler() {
        scrollAllowed.set(false);
        mHandler.removeCallbacks(scrollAllowedRunnable);
    }
}