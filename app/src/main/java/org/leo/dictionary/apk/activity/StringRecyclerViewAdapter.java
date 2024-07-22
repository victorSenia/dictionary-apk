package org.leo.dictionary.apk.activity;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.databinding.FragmentStringBinding;

import java.util.List;

public class StringRecyclerViewAdapter extends RecyclerView.Adapter<StringRecyclerViewAdapter.StringViewHolder> {
    protected final List<String> mValues;
    protected final Fragment fragment;
    protected final OnClickListener onClickListener;

    public StringRecyclerViewAdapter(List<String> items, Fragment fragment, OnClickListener onClickListener) {
        mValues = items;
        this.fragment = fragment;
        this.onClickListener = onClickListener;
    }

    @Override
    public StringViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new StringViewHolder(FragmentStringBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final StringViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(holder.mItem);
        if (isBackgroundColorNeeded(holder)) {
            holder.itemView.setBackgroundColor(Color.DKGRAY);
        } else {
            holder.itemView.setBackground(null);
        }
    }

    protected boolean isBackgroundColorNeeded(StringViewHolder holder) {
        if (onClickListener instanceof RememberSelectionOnClickListener) {
            return ((RememberSelectionOnClickListener) onClickListener).selected == holder.getAbsoluteAdapterPosition();
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public interface OnClickListener {
        default void onClick(StringViewHolder viewHolder) {
        }

        default boolean onLongClick(StringViewHolder viewHolder) {
            return false;
        }
    }

    public static class RememberSelectionOnClickListener implements StringRecyclerViewAdapter.OnClickListener {
        protected int selected = RecyclerView.NO_POSITION;


        @Override
        public void onClick(StringRecyclerViewAdapter.StringViewHolder viewHolder) {
            selected = viewHolder.getAbsoluteAdapterPosition();
            viewHolder.getBindingAdapter().notifyDataSetChanged();
        }

        public void clearSelection() {
            selected = RecyclerView.NO_POSITION;
        }

        public void setSelected(int selected) {
            this.selected = selected;
        }

    }

    public class StringViewHolder extends RecyclerView.ViewHolder {
        public final TextView mContentView;
        public String mItem;

        public StringViewHolder(FragmentStringBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            if (onClickListener != null) {
                binding.getRoot().setOnLongClickListener(v -> onClickListener.onLongClick(StringViewHolder.this));
                binding.getRoot().setOnClickListener(v -> onClickListener.onClick(StringViewHolder.this));
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
