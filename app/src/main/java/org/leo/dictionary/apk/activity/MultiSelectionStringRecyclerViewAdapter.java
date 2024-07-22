package org.leo.dictionary.apk.activity;

import android.widget.Toast;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class MultiSelectionStringRecyclerViewAdapter extends StringRecyclerViewAdapter {
    public MultiSelectionStringRecyclerViewAdapter(List<String> items, Fragment fragment) {
        super(items, fragment, new MultiSelectionOnClickListener());
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        if (((MultiSelectionOnClickListener) onClickListener).selected.contains(holder.mItem)) {
//            holder.itemView.setBackground(fragment.getActivity().getResources().getDrawable(android.R.drawable.alert_light_frame));
        }
    }

    public List<String> getSelected() {
        return ((MultiSelectionOnClickListener) onClickListener).selected;

//        return ((MultiSelectionOnClickListener) onClickListener).selected.stream().sorted().map(position -> mValues.get(position)).collect(Collectors.toList());
    }

    public static class MultiSelectionOnClickListener implements StringRecyclerViewAdapter.OnClickListener {
        private final List<String> selected = new ArrayList<>();
        private boolean isMultiSelect = false;

        @Override
        public void onClick(ViewHolder viewHolder) {
            if (isMultiSelect) {
                if (selected.contains(viewHolder.mItem)) {
                    selected.remove(viewHolder.mItem);
                } else {
                    selected.add(viewHolder.mItem);
                }
                viewHolder.getBindingAdapter().notifyDataSetChanged();
            } else {
                selected.clear();
                selected.add(viewHolder.mItem);
                viewHolder.getBindingAdapter().notifyDataSetChanged();
            }
            Toast.makeText(viewHolder.itemView.getContext(), selected.toString(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onLongClick(ViewHolder viewHolder) {
            if (!isMultiSelect) {
                isMultiSelect = true;
            }
            if (!selected.contains(viewHolder.mItem)) {
                selected.add(viewHolder.mItem);
                viewHolder.getBindingAdapter().notifyDataSetChanged();
            }
            Toast.makeText(viewHolder.itemView.getContext(), selected.toString(), Toast.LENGTH_SHORT).show();
            return true;
        }
    }

}