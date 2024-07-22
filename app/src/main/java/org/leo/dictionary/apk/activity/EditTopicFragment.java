package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Word;

import java.util.List;

public class EditTopicFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private RecyclerView recyclerView;

    public void replaceData(List<Topic> topics) {
        ((TopicRecyclerViewAdapter) recyclerView.getAdapter()).replaceData(topics);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_topic_list, container, false);
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            MutableLiveData<Word> word = new ViewModelProvider(requireActivity()).get(EditWordViewModel.class).getUiState();
            recyclerView.setAdapter(createRecyclerViewAdapter(word));
        }
        return view;
    }

    private TopicRecyclerViewAdapter createRecyclerViewAdapter(MutableLiveData<Word> word) {
        return new TopicRecyclerViewAdapter(word.getValue().getTopics()) {
            @Override
            public void deleteItem(DeleteViewHolder viewHolder) {
                word.getValue().getTopics().remove(viewHolder.mItem);
                ((EditWordActivity) requireActivity()).filterTopics();
                super.deleteItem(viewHolder);
            }
        };
    }
}