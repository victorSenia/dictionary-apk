package org.leo.dictionary.apk.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.R;

import java.util.List;

public abstract class RecyclerViewFragment<V extends RecyclerView.Adapter<? extends RecyclerView.ViewHolder>, T> extends Fragment {
    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    protected RecyclerView recyclerView;
    protected EditText filter;
    // TODO: Customize parameters
    private int mColumnCount = 1;

    public V getRecyclerViewAdapter() {
        return (V) recyclerView.getAdapter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
        addObservers();
    }

    protected void addObservers() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_strings_list, container, false);
        RecyclerView listView = view.findViewById(R.id.list);

        // Set the adapter
        if (listView != null) {
            filter = view.findViewById(R.id.filter_text);
//            filter.setVisibility(View.GONE);
            Context context = listView.getContext();
            recyclerView = listView;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }

            recyclerView.setAdapter(createRecyclerViewAdapter(getValues()));
        }
        return view;
    }

    protected abstract V createRecyclerViewAdapter(List<T> values);

    protected abstract List<T> getValues();
}
