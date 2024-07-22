package org.leo.dictionary.apk.activity;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.databinding.FragmentStringsBinding;

import java.util.List;

public class StringRecyclerViewAdapter extends RecyclerView.Adapter<StringRecyclerViewAdapter.ViewHolder> {
    protected final List<String> mValues;
    protected final Fragment fragment;
    protected final OnClickListener onClickListener;

    public StringRecyclerViewAdapter(List<String> items, Fragment fragment, OnClickListener onClickListener) {
        mValues = items;
        this.fragment = fragment;
        this.onClickListener = onClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentStringsBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mContentView.setText(holder.mItem);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public interface OnClickListener {
        default void onClick(ViewHolder viewHolder) {
        }

        default boolean onLongClick(ViewHolder viewHolder) {
            return false;
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mContentView;
        public String mItem;

        public ViewHolder(FragmentStringsBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            if (onClickListener != null) {
                binding.getRoot().setOnLongClickListener(v -> {
                    if (onClickListener != null) {
                        return onClickListener.onLongClick(ViewHolder.this);
                    }
                    return false;
                });
                binding.getRoot().setOnClickListener(v -> {
                    if (onClickListener != null) {
                        onClickListener.onClick(ViewHolder.this);
                    }
                });
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
