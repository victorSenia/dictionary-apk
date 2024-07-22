package org.leo.dictionary.apk.activity;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.databinding.FragmentWordBinding;
import org.leo.dictionary.entity.Word;

import java.util.List;

public class WordsRecyclerViewAdapter extends RecyclerView.Adapter<WordsRecyclerViewAdapter.ViewHolder> {

    private final List<Word> words;
    private final Fragment fragment;

    private int positionId;

    public WordsRecyclerViewAdapter(List<Word> words, Fragment fragment) {
        this.words = words;
        this.fragment = fragment;
    }

    public int getPositionId() {
        return positionId;
    }

    public void replaceData(List<Word> words) {
        this.words.clear();
        this.words.addAll(words);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(FragmentWordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = words.get(position);
        holder.mContentView.setText(Word.formatWord(words.get(position)));
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mContentView;
        public Word mItem;

        public ViewHolder(FragmentWordBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            binding.getRoot().setOnCreateContextMenuListener(fragment);
            binding.getRoot().setOnLongClickListener(v -> {
                positionId = getAbsoluteAdapterPosition();
                return false;
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}