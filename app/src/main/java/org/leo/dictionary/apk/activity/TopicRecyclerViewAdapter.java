package org.leo.dictionary.apk.activity;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.databinding.FragmentEditTopicBinding;
import org.leo.dictionary.apk.databinding.FragmentStringBinding;
import org.leo.dictionary.entity.Topic;

import java.util.List;

public class TopicRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected final List<Topic> mValues;

    public TopicRecyclerViewAdapter(List<Topic> items) {
        mValues = items;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return createDeleteViewHolder(parent);
    }

    private RecyclerView.ViewHolder createDeleteViewHolder(ViewGroup parent) {
        return new DeleteViewHolder(FragmentEditTopicBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    public RecyclerView.ViewHolder createStringViewHolder(ViewGroup parent) {
        return new StringViewHolder(FragmentStringBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DeleteViewHolder) {
            onBindDeleteViewHolder((DeleteViewHolder) holder, position);
        } else if (holder instanceof StringViewHolder) {
            onBindStringViewHolder((StringViewHolder) holder, position);
        }
    }

    protected void onBindDeleteViewHolder(final DeleteViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mTextView.setText(holder.mItem.getName());
    }

    protected void onBindStringViewHolder(final StringViewHolder holder, int position) {
        holder.mItem = mValues.get(position);
        holder.mTextView.setText(holder.mItem.getName());
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public void deleteItem(DeleteViewHolder viewHolder) {
        int indexToRemove = mValues.indexOf(viewHolder.mItem);
        mValues.remove(indexToRemove);
        notifyItemRemoved(indexToRemove);
    }

    public void replaceData(List<Topic> filteredList) {
        mValues.clear();
        mValues.addAll(filteredList);
        notifyDataSetChanged();
    }

    protected void onClickListener(StringViewHolder viewHolder) {
    }

    public class DeleteViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public Topic mItem;

        public DeleteViewHolder(FragmentEditTopicBinding binding) {
            super(binding.getRoot());
            mTextView = binding.content;
            binding.actionDelete.setOnClickListener(v -> deleteItem(DeleteViewHolder.this));
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }
    }

    public class StringViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public Topic mItem;

        public StringViewHolder(FragmentStringBinding binding) {
            super(binding.getRoot());
            mTextView = binding.content;
            binding.getRoot().setOnClickListener(v -> onClickListener(StringViewHolder.this));
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mTextView.getText() + "'";
        }
    }
}