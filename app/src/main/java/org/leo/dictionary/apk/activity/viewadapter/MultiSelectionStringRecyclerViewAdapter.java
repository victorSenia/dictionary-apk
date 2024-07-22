package org.leo.dictionary.apk.activity.viewadapter;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.*;
import java.util.function.Function;

public class MultiSelectionStringRecyclerViewAdapter<T> extends StringRecyclerViewAdapter<T> {

    public MultiSelectionStringRecyclerViewAdapter(List<T> items, Fragment fragment) {
        super(items, fragment, new MultiSelectionOnClickListener<>());
    }

    public MultiSelectionStringRecyclerViewAdapter(List<T> items, Fragment fragment, Function<T, String> formatter) {
        super(items, fragment, new MultiSelectionOnClickListener<>(), formatter);
    }

    protected boolean isBackgroundColorNeeded(StringViewHolder<T> holder) {
        return ((MultiSelectionOnClickListener<T>) onClickListener).selected.containsKey(holder.getAbsoluteAdapterPosition());
    }

    public List<T> getSelectedList() {
        return new ArrayList<>(((MultiSelectionOnClickListener<T>) onClickListener).selected.values());
    }

    public void setSelected(Collection<T> items) {
        if (items != null && !items.isEmpty()) {
            MultiSelectionOnClickListener<T> onClickListener = (MultiSelectionOnClickListener<T>) this.onClickListener;
            for (T item : items) {
                int key = values.indexOf(item);
                if (key != -1) {
                    onClickListener.selected.put(key, item);
                    notifyItemChanged(key);
                }
            }
            if (onClickListener.selected.size() > 1) {
                onClickListener.isMultiSelect = true;
            }
        }
    }

    @Override
    public void clearAdapter() {
        ((MultiSelectionOnClickListener<T>) this.onClickListener).selected.clear();
        super.clearAdapter();
    }

    public void clearSelection() {
        MultiSelectionOnClickListener<T> onClickListener = (MultiSelectionOnClickListener<T>) this.onClickListener;
        onClickListener.clearAndUpdateUi(this);
        onClickListener.isMultiSelect = false;
    }

    public static class MultiSelectionOnClickListener<T> implements StringRecyclerViewAdapter.OnClickListener<T> {
        private final Map<Integer, T> selected = new HashMap<>();
        private boolean isMultiSelect = false;

        @Override
        public void onClick(StringViewHolder<T> viewHolder) {
            int adapterPosition = viewHolder.getAbsoluteAdapterPosition();
            if (isMultiSelect) {
                if (selected.containsKey(adapterPosition)) {
                    selected.remove(adapterPosition);
                } else {
                    selected.put(adapterPosition, viewHolder.item);
                }
                viewHolder.getBindingAdapter().notifyItemChanged(adapterPosition);
            } else {
                clearAndUpdateUi(viewHolder.getBindingAdapter());
                selected.put(adapterPosition, viewHolder.item);
                viewHolder.getBindingAdapter().notifyItemChanged(adapterPosition);
            }
        }

        private void clearAndUpdateUi(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
            Integer[] oldSelected = selected.keySet().toArray(new Integer[0]);
            selected.clear();
            for (int index : oldSelected) {
                adapter.notifyItemChanged(index);
            }
        }

        @Override
        public boolean onLongClick(StringViewHolder<T> viewHolder) {
            if (!isMultiSelect) {
                isMultiSelect = true;
            }
            int adapterPosition = viewHolder.getAbsoluteAdapterPosition();
            if (!selected.containsKey(adapterPosition)) {
                selected.put(adapterPosition, viewHolder.item);
                viewHolder.getBindingAdapter().notifyItemChanged(adapterPosition);
            }
            return true;
        }
    }
}