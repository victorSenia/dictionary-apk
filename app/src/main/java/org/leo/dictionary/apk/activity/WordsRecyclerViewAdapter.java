package org.leo.dictionary.apk.activity;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.FragmentWordBinding;
import org.leo.dictionary.apk.databinding.FragmentWordSelectedBinding;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Word;

import java.util.List;

public class WordsRecyclerViewAdapter extends RecyclerView.Adapter<WordsRecyclerViewAdapter.ViewHolder> {

    public static final int SELECTED_WORD_VIEW_TYPE = 1;
    protected final List<Word> words;
    private final Fragment fragment;

    private int positionId = -1;

    public WordsRecyclerViewAdapter(List<Word> words, Fragment fragment) {
        this.words = words;
        this.fragment = fragment;
    }

    public void replaceData(List<Word> words) {
        this.words.clear();
        this.words.addAll(words);
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == SELECTED_WORD_VIEW_TYPE) {
            return new ViewHolder(FragmentWordSelectedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        return new ViewHolder(FragmentWordBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        if (positionId == position) {
            return SELECTED_WORD_VIEW_TYPE;
        }
        return super.getItemViewType(position);
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

    private void deleteWord() {
        DBWordProvider wordProvider = ((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.dbWordProvider();
        wordProvider.deleteWord(words.get(positionId).getId());
        words.remove(positionId);
        notifyItemRemoved(positionId);

        PlayService playService = ((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.playService();
        playService.safeDelete(positionId);
    }

    private void playFromSelected() {
        PlayService playService = ((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.playService();
        playService.playFrom(positionId);
        PlayerFragment player = (PlayerFragment) fragment.requireActivity().getSupportFragmentManager().findFragmentById(R.id.player_fragment);
        if (player != null) {
            player.updateButtonUi();
        }
    }

    private void editWord() {
        MainActivity activity = (MainActivity) fragment.requireActivity();
        activity.editWord(positionId, words.get(positionId));
    }

    private boolean isDBSource() {
        return ApkModule.isDBSource(((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.lastState());
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mContentView;
        public Word mItem;

        public ViewHolder(FragmentWordBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            binding.getRoot().setOnCreateContextMenuListener(fragment);
            binding.getRoot().setOnClickListener(v -> {
                int previousPosition = positionId;
                positionId = getAbsoluteAdapterPosition();
                fragment.requireActivity().runOnUiThread(() -> {
                            notifyItemChanged(previousPosition);
                            notifyItemChanged(positionId);
                        }
                );
            });
        }

        public ViewHolder(FragmentWordSelectedBinding binding) {
            super(binding.getRoot());
            mContentView = binding.content;
            binding.getRoot().setOnCreateContextMenuListener(fragment);
            binding.playFrom.setOnClickListener(view -> playFromSelected());
            binding.actionDelete.setOnClickListener(view -> {
                if (isDBSource()) {
                    getDeleteConfirmationBuilder(view).show();
                }
            });
            binding.actionEdit.setOnClickListener(view -> {
                if (isDBSource()) {
                    editWord();
                }
            });
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }

        private AlertDialog.Builder getDeleteConfirmationBuilder(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle("Delete confirmation");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage("Do you really want to delete word '" + words.get(positionId).getFullWord() + "'?");
            builder.setPositiveButton("Yes", (dialog, which) -> deleteWord());
            return builder;
        }
    }
}