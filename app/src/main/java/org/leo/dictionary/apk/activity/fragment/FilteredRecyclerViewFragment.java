package org.leo.dictionary.apk.activity.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.activity.EditWordActivity;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class FilteredRecyclerViewFragment<V extends RecyclerView.Adapter<? extends RecyclerView.ViewHolder>, T> extends RecyclerViewFragment<V, T> {
    public static final int FILTER_AFTER_SIZE = 10;
    protected List<T> allValues;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addObservers();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        filter.addTextChangedListener(new EditWordActivity.AbstractTextWatcher() {
            private String previous;

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if ((previous != null && !previous.equals(s.toString())) || (previous == null && s.length() != 0)) {
                    filterValuesInAdapter();
                }
                previous = s.toString();
            }
        });
        return view;
    }

    protected void updateListData(Object value) {
        if (stateChanged()) {
            findValuesHideFilterIfNeeded();
            filterValuesInAdapter();
        }
    }

    protected void findValuesHideFilterIfNeeded() {
        allValues = findValues();
        setFilterVisibility();
    }

    protected void setFilterVisibility() {
        if (filter != null) {
            filter.setVisibility(isFilterVisible() ? View.VISIBLE : View.GONE);
        }
    }

    protected boolean isFilterVisible() {
        return !(allValues.size() < FILTER_AFTER_SIZE);
    }

    protected void setContainerVisibility(int containerId, List<T> result) {
        requireActivity().findViewById(containerId).setVisibility(result.size() > 1 ? View.VISIBLE : View.GONE);
    }

    protected void filterValuesInAdapter() {
        V adapter = getRecyclerViewAdapter();
        if (adapter instanceof StringRecyclerViewAdapter) {
            ((StringRecyclerViewAdapter<T>) adapter).clearAdapter();
            ((StringRecyclerViewAdapter<T>) adapter).values.addAll(filterValues());
            setSelectedValues(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    protected List<T> filterValues() {
        if (filter != null && !filter.getText().toString().isEmpty()) {
            CharSequence filterString = filter.getText().toString();
            Predicate<T> predicate = filterPredicate(filterString);
            return allValues.stream().filter(predicate).collect(Collectors.toList());
        }
        return allValues;
    }

    protected List<T> getValues() {
        findValuesHideFilterIfNeeded();
        return new ArrayList<>(allValues);
    }

    protected Predicate<T> filterPredicate(CharSequence filterString) {
        return t -> getFormatter().apply(t).contains(filterString);
    }

    protected Function<T, String> getFormatter() {
        return T::toString;
    }

    protected abstract boolean stateChanged();

    protected abstract List<T> findValues();

    protected void setSelectedValues(V adapter) {
    }
}