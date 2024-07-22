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
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;

import java.util.Collections;
import java.util.List;

public abstract class RecyclerViewFragment<T> extends Fragment {
    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    protected RecyclerView recyclerView;
    // TODO: Customize parameters
    private int mColumnCount = 1;

    public StringRecyclerViewAdapter<T> getRecyclerViewAdapter() {
        return (StringRecyclerViewAdapter<T>) recyclerView.getAdapter();
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

            recyclerView.setAdapter(createRecyclerViewAdapter());
        }
        return view;
    }

    protected StringRecyclerViewAdapter<T> createRecyclerViewAdapter() {
        return new StringRecyclerViewAdapter<>(getStrings(), this, null);
    }

    protected List<T> getStrings() {
        return Collections.emptyList();
    }
}
