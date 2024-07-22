package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.widget.AdapterView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.ApkUiUpdater;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
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

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable @org.jetbrains.annotations.Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (item.getItemId() == R.id.action_play_from) {
            PlayService playService = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.playService();

            playService.playFrom(((WordsRecyclerViewAdapter) recyclerView.getAdapter()).getPositionId());
            updatePlayerUi();
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    private void updatePlayerUi() {
        PlayerFragment player = (PlayerFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.player_fragment);
        if (player != null) {
            player.updateButtonUi();
        }
    }

    public void replaceData(List<Word> unknownWords) {
        ((WordsRecyclerViewAdapter) recyclerView.getAdapter()).replaceData(unknownWords);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_words_list, container, false);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            PlayService playService = ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.playService();

            ArrayList<Word> words = new ArrayList<>(playService.getUnknownWords());
            recyclerView.setAdapter(new WordsRecyclerViewAdapter(words, this));

            ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.uiUpdater();
            uiUpdater = word -> getActivity().runOnUiThread(() -> recyclerView.scrollToPosition(words.indexOf(word)));
            apkUiUpdater.addUiUpdater(uiUpdater);
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) getActivity().getApplicationContext()).appComponent.uiUpdater();
        apkUiUpdater.removeUiUpdater(uiUpdater);
    }
}