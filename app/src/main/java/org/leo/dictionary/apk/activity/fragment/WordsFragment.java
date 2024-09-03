package org.leo.dictionary.apk.activity.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApkUiUpdater;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.MainActivity;
import org.leo.dictionary.apk.activity.viewadapter.WordsRecyclerViewAdapter;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WordsFragment extends RecyclerViewFragment<WordsRecyclerViewAdapter, Word> {

    public static final long INACTIVITY_TIMEOUT = 3000;
    private final AtomicBoolean scrollAllowed = new AtomicBoolean(true);
    private final Runnable scrollAllowedRunnable = () -> scrollAllowed.set(true);
    private UiUpdater uiUpdater;
    private Handler mHandler;

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

    public void replaceData(List<Word> unknownWords) {
        getRecyclerViewAdapter().replaceData(unknownWords);
        if (!unknownWords.isEmpty()) {
            recyclerView.scrollToPosition(0);
        }
    }

    public void wordUpdated(int index) {
        getRecyclerViewAdapter().notifyItemChanged(index);
    }

    public void wordAdded(int index, Word word) {
        getRecyclerViewAdapter().words.add(word);
        getRecyclerViewAdapter().notifyItemInserted(index);
    }

    public void wordDeleted(int index) {
        getRecyclerViewAdapter().words.remove(index);
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

        MainActivity.runAtBackground(() -> {
            getRecyclerViewAdapter().words.addAll(findValues());
            requireActivity().runOnUiThread(() -> {
                getRecyclerViewAdapter().notifyDataSetChanged();
                recyclerView.scrollToPosition(getCurrentIndex());
            });
        });
        return view;
    }

    protected List<Word> findValues() {
        PlayService playService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.playService();
        return playService.getUnknownWords();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.uiUpdater();
        apkUiUpdater.removeUiUpdater(uiUpdater);
    }


    private void startHandler() {
        mHandler.postDelayed(scrollAllowedRunnable, INACTIVITY_TIMEOUT);
    }

    private void stopHandler() {
        scrollAllowed.set(false);
        mHandler.removeCallbacks(scrollAllowedRunnable);
    }

}