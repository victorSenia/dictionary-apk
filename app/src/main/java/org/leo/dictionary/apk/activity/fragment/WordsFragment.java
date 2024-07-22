package org.leo.dictionary.apk.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.jetbrains.annotations.Nullable;
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

public class WordsFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private RecyclerView recyclerView;
    private UiUpdater uiUpdater;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    public void replaceData(List<Word> unknownWords) {
        getRecyclerViewAdapter().replaceData(unknownWords);
    }

    @Nullable
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
            uiUpdater = (word, index) -> requireActivity().runOnUiThread(() -> recyclerView.scrollToPosition(index));
            apkUiUpdater.addUiUpdater(uiUpdater);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.uiUpdater();
        apkUiUpdater.removeUiUpdater(uiUpdater);
    }
}