package org.leo.dictionary.apk.activity.viewadapter;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.MainActivity;
import org.leo.dictionary.apk.databinding.FragmentStringBinding;
import org.leo.dictionary.apk.databinding.FragmentWordSelectedBinding;
import org.leo.dictionary.apk.databinding.FragmentWordSelectedDbBinding;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Word;

import java.util.List;

public class WordsRecyclerViewAdapter extends RecyclerView.Adapter<WordsRecyclerViewAdapter.WordViewHolder> {

    public static final int SELECTED_WORD_VIEW_TYPE = 1;
    public static final int SELECTED_WORD_DB_VIEW_TYPE = 2;
    public final List<Word> words;
    private final Fragment fragment;

    private int positionId;

    public WordsRecyclerViewAdapter(List<Word> words, Fragment fragment, int currentIndex) {
        this.words = words;
        this.fragment = fragment;
        this.positionId = currentIndex;
    }

    public void replaceData(List<Word> words) {
        this.words.clear();
        this.words.addAll(words);
        this.positionId = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == SELECTED_WORD_VIEW_TYPE) {
            return new WordViewHolder(FragmentWordSelectedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else if (viewType == SELECTED_WORD_DB_VIEW_TYPE) {
            return new WordViewHolder(FragmentWordSelectedDbBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        return new WordViewHolder(FragmentStringBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        if (positionId == position) {
            if (isDBSource()) {
                return SELECTED_WORD_DB_VIEW_TYPE;
            }
            return SELECTED_WORD_VIEW_TYPE;
        }
        return super.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(final WordViewHolder holder, int position) {
        holder.mItem = words.get(position);
        holder.mContentView.setText(Word.formatWord(holder.mItem));
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    private void deleteWord(Word word) {
        DBWordProvider wordProvider = ((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.dbWordProvider();
        wordProvider.deleteWord(word.getId());
        words.remove(positionId);
        notifyItemRemoved(positionId);

        PlayService playService = ((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.playService();
        playService.safeDelete(positionId);
    }

    private void playFromSelected() {
        PlayService playService = ((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.playService();
        playService.playFrom(positionId);
    }

    private void editWord(Word word) {
        MainActivity activity = (MainActivity) fragment.requireActivity();
        activity.editWord(positionId, word);
    }

    private boolean isDBSource() {
        return ApkModule.isDBSource(((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.lastState());
    }

    public void setSelectedPosition(int positionId) {
        int previousPosition = this.positionId;
        this.positionId = positionId;
        fragment.requireActivity().runOnUiThread(() -> {
                    notifyItemChanged(previousPosition);
                    notifyItemChanged(positionId);
                }
        );
    }

    private void noDBSourceError() {
        Toast.makeText(fragment.requireActivity(), R.string.not_database_error, Toast.LENGTH_SHORT).show();
    }

    public class WordViewHolder extends RecyclerView.ViewHolder {
        public TextView mContentView;
        public Word mItem;

        public WordViewHolder(FragmentStringBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            binding.getRoot().setOnCreateContextMenuListener(fragment);
            binding.getRoot().setOnClickListener(v -> setSelectedPosition(getAbsoluteAdapterPosition()));
        }

        public WordViewHolder(FragmentWordSelectedDbBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            binding.getRoot().setOnCreateContextMenuListener(fragment);
            binding.playFrom.setOnClickListener(view -> playFromSelected());
            binding.actionDelete.setOnClickListener(view -> {
                if (isDBSource()) {
                    getDeleteConfirmationBuilder(view, mItem).show();
                } else {
                    noDBSourceError();
                }
            });
            binding.actionEdit.setOnClickListener(view -> {
                if (isDBSource()) {
                    editWord(mItem);
                } else {
                    noDBSourceError();
                }
            });
        }

        public WordViewHolder(FragmentWordSelectedBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            binding.getRoot().setOnCreateContextMenuListener(fragment);
            binding.playFrom.setOnClickListener(view -> playFromSelected());
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }

        private AlertDialog.Builder getDeleteConfirmationBuilder(View view, Word word) {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle(R.string.delete_word_confirmation);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(fragment.getString(R.string.delete_word_confirmation_message, word.getFullWord()));
            builder.setPositiveButton(R.string.yes, (dialog, which) -> deleteWord(word));
            return builder;
        }
    }
}