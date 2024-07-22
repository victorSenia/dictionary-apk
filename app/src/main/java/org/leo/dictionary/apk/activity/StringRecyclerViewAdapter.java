package org.leo.dictionary.apk.activity;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.FragmentStringBinding;

import java.util.List;
import java.util.function.Consumer;

public class StringRecyclerViewAdapter extends RecyclerView.Adapter<StringRecyclerViewAdapter.StringViewHolder> {
    protected final List<String> mValues;
    protected final Fragment fragment;
    protected final OnClickListener onClickListener;

    public StringRecyclerViewAdapter(List<String> items, Fragment fragment, OnClickListener onClickListener) {
        mValues = items;
        this.fragment = fragment;
        this.onClickListener = onClickListener;
    }

    public int getSelected() {
        return onClickListener instanceof RememberSelectionOnClickListener ? ((RememberSelectionOnClickListener) onClickListener).selected : RecyclerView.NO_POSITION;
    }

    public void setSelected(int selected) {
        if (onClickListener instanceof RememberSelectionOnClickListener) {
            ((RememberSelectionOnClickListener) onClickListener).setSelected(selected);
        }
    }

    @Override
    @NonNull
    public StringViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StringViewHolder(FragmentStringBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final StringViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(holder.mItem);
        if (isBackgroundColorNeeded(holder)) {
            holder.itemView.setBackgroundColor(fragment.requireActivity().getColor(R.color.selected_background));
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

    public void clearSelection() {
        if (onClickListener instanceof RememberSelectionOnClickListener) {
            int selected = ((RememberSelectionOnClickListener) onClickListener).selected;
            ((RememberSelectionOnClickListener) onClickListener).clearSelection();
            notifyItemChanged(selected);
        }
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
        private Consumer<StringRecyclerViewAdapter.StringViewHolder> additionalOnClickHandling;

        public RememberSelectionOnClickListener(Consumer<StringViewHolder> additionalOnClickHandling) {
            this.additionalOnClickHandling = additionalOnClickHandling;
        }


        @Override
        public void onClick(StringRecyclerViewAdapter.StringViewHolder viewHolder) {
            selected = viewHolder.getAbsoluteAdapterPosition();
            viewHolder.getBindingAdapter().notifyItemChanged(selected);
            if (additionalOnClickHandling != null) {
                additionalOnClickHandling.accept(viewHolder);
            }
        }

        public void clearSelection() {
            selected = RecyclerView.NO_POSITION;
        }

        public void setSelected(int selected) {
            this.selected = selected;
        }

    }

    public class StringViewHolder extends RecyclerView.ViewHolder {
        public TextView mContentView;
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
