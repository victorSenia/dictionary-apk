package org.leo.dictionary.apk.activity;

import android.util.SparseBooleanArray;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectionStringRecyclerViewAdapter extends StringRecyclerViewAdapter {

    public MultiSelectionStringRecyclerViewAdapter(List<String> items, Fragment fragment) {
        super(items, fragment, new MultiSelectionOnClickListener());
    }

    protected boolean isBackgroundColorNeeded(StringViewHolder holder) {
        return ((MultiSelectionOnClickListener) onClickListener).selected.get(holder.getAbsoluteAdapterPosition(), false);
    }

    public List<String> getSelected() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < mValues.size(); i++) {
            if (((MultiSelectionOnClickListener) onClickListener).selected.get(i, false)) {
                result.add(mValues.get(i));
            }
        }
        return result;
    }

    public void clearSelection() {
        ((MultiSelectionOnClickListener) onClickListener).selected.clear();
        ((MultiSelectionOnClickListener) onClickListener).isMultiSelect = false;
        notifyDataSetChanged();
    }

    public static class MultiSelectionOnClickListener implements StringRecyclerViewAdapter.OnClickListener {
        private final SparseBooleanArray selected = new SparseBooleanArray();
        private boolean isMultiSelect = false;

        @Override
        public void onClick(StringViewHolder viewHolder) {
            if (isMultiSelect) {
                if (selected.get(viewHolder.getAbsoluteAdapterPosition(), false)) {
                    selected.delete(viewHolder.getAbsoluteAdapterPosition());
                } else {
                    selected.put(viewHolder.getAbsoluteAdapterPosition(), true);
                }
                viewHolder.getBindingAdapter().notifyItemChanged(viewHolder.getAbsoluteAdapterPosition());
            } else {
                selected.clear();
                selected.put(viewHolder.getAbsoluteAdapterPosition(), true);
                viewHolder.getBindingAdapter().notifyDataSetChanged();
            }
        }

        @Override
        public boolean onLongClick(StringViewHolder viewHolder) {
            if (!isMultiSelect) {
                isMultiSelect = true;
            }
            if (!selected.get(viewHolder.getAbsoluteAdapterPosition(), false)) {
                selected.put(viewHolder.getAbsoluteAdapterPosition(), true);
                viewHolder.getBindingAdapter().notifyItemChanged(viewHolder.getAbsoluteAdapterPosition());
            }
            return true;
        }
    }
}