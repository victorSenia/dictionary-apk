package org.leo.dictionary.apk.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.NotNull;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApkUiUpdater;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.MainActivity;
import org.leo.dictionary.apk.activity.viewadapter.WordsRecyclerViewAdapter;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WordsFragment extends Fragment {

    public static final long INACTIVITY_TIMEOUT = 3000;
    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    private final AtomicBoolean scrollAllowed = new AtomicBoolean(true);
    private final Runnable scrollAllowedRunnable = () -> scrollAllowed.set(true);
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private RecyclerView recyclerView;
    private UiUpdater uiUpdater;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
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

    private WordsRecyclerViewAdapter getRecyclerViewAdapter() {
        return (WordsRecyclerViewAdapter) recyclerView.getAdapter();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_strings_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull @NotNull RecyclerView recyclerView, int dx, int dy) {
                    stopStartHandler();
                    super.onScrolled(recyclerView, dx, dy);
                }
            });
            recyclerView.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(@NonNull @NotNull RecyclerView rv, @NonNull @NotNull MotionEvent e) {
                    stopStartHandler();
                    return super.onInterceptTouchEvent(rv, e);
                }
            });
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            PlayService playService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.playService();

            ArrayList<Word> words = new ArrayList<>();
            int currentIndex = ApkModule.getLastStateCurrentIndex(((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.lastState());

            WordsRecyclerViewAdapter adapter = new WordsRecyclerViewAdapter(words, this, currentIndex);
            recyclerView.setAdapter(adapter);
            MainActivity.runAtBackground(() -> {
                adapter.words.addAll(playService.getUnknownWords());
                requireActivity().runOnUiThread(adapter::notifyDataSetChanged);
            });
            recyclerView.scrollToPosition(currentIndex);

            ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.uiUpdater();
            uiUpdater = (word, index) -> {
                if (scrollAllowed.get()) {
                    requireActivity().runOnUiThread(() -> recyclerView.scrollToPosition(index));
                }
            };
            apkUiUpdater.addUiUpdater(uiUpdater);
        }
        return view;
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