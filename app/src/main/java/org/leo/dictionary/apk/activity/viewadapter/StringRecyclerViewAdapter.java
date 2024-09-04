package org.leo.dictionary.apk.activity.viewadapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.FragmentStringBinding;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class StringRecyclerViewAdapter<T> extends RecyclerView.Adapter<StringRecyclerViewAdapter.StringViewHolder<T>> {

    public final List<T> values;
    protected final Fragment fragment;
    protected final OnClickListener<T> onClickListener;
    protected Function<T, String> formatter = Object::toString;

    public StringRecyclerViewAdapter(List<T> items, Fragment fragment, OnClickListener<T> onClickListener) {
        values = items;
        this.fragment = fragment;
        this.onClickListener = onClickListener;
    }

    public StringRecyclerViewAdapter(List<T> items, Fragment fragment, OnClickListener<T> onClickListener, Function<T, String> formatter) {
        this(items, fragment, onClickListener);
        this.formatter = formatter;
    }

    public int getSelected() {
        return onClickListener instanceof RememberSelectionOnClickListener ? ((RememberSelectionOnClickListener<T>) onClickListener).selected : RecyclerView.NO_POSITION;
    }

    public void setSelected(int selected) {
        if (onClickListener instanceof RememberSelectionOnClickListener) {
            ((RememberSelectionOnClickListener<T>) onClickListener).setSelected(selected);
        }
    }

    @Override
    @NonNull
    public StringViewHolder<T> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StringViewHolder<>(FragmentStringBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false), onClickListener, formatter);
    }

    @Override
    public void onBindViewHolder(final StringViewHolder<T> holder, int position) {
        holder.item = values.get(position);
        holder.textView.setText(holder.valueToString());
        if (isBackgroundColorNeeded(holder)) {
            holder.itemView.setBackgroundColor(fragment.requireActivity().getColor(R.color.selected_background));
        } else {
            holder.itemView.setBackground(null);
        }
    }

    protected boolean isBackgroundColorNeeded(StringViewHolder<T> holder) {
        if (onClickListener instanceof RememberSelectionOnClickListener) {
            return ((RememberSelectionOnClickListener<T>) onClickListener).selected == holder.getAbsoluteAdapterPosition();
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public void clearSelection() {
        if (onClickListener instanceof RememberSelectionOnClickListener) {
            int selected = ((RememberSelectionOnClickListener<T>) onClickListener).selected;
            ((RememberSelectionOnClickListener<T>) onClickListener).clearSelection();
            notifyItemChanged(selected);
        }
    }

    public void clearAdapter() {
        if (onClickListener instanceof RememberSelectionOnClickListener) {
            ((RememberSelectionOnClickListener<T>) onClickListener).clearSelection();
        }
        values.clear();
    }

    public interface OnClickListener<T> {
        default void onClick(StringViewHolder<T> viewHolder) {
        }

        default boolean onLongClick(StringViewHolder<T> viewHolder) {
            return false;
        }
    }

    public static class RememberSelectionOnClickListener<T> implements StringRecyclerViewAdapter.OnClickListener<T> {
        protected int selected = RecyclerView.NO_POSITION;
        private final BiConsumer<Integer, StringViewHolder<T>> additionalOnClickHandling;

        public RememberSelectionOnClickListener(BiConsumer<Integer, StringViewHolder<T>> additionalOnClickHandling) {
            this.additionalOnClickHandling = additionalOnClickHandling;
        }


        @Override
        public void onClick(StringViewHolder<T> viewHolder) {
            int oldSelected = selected;
            selected = viewHolder.getAbsoluteAdapterPosition();
            if (additionalOnClickHandling != null) {
                additionalOnClickHandling.accept(oldSelected, viewHolder);
            }
            if (viewHolder.getBindingAdapter() != null) {
                viewHolder.getBindingAdapter().notifyItemChanged(oldSelected);
                viewHolder.getBindingAdapter().notifyItemChanged(selected);
            }
        }

        public void clearSelection() {
            selected = RecyclerView.NO_POSITION;
        }

        public void setSelected(int selected) {
            this.selected = selected;
        }

    }

    public static class StringViewHolder<T> extends RecyclerView.ViewHolder {
        public T item;
        protected TextView textView;
        protected Function<T, String> formatter;

        public StringViewHolder(FragmentStringBinding binding, OnClickListener<T> onClickListener, Function<T, String> formatter) {
            super(binding.getRoot());
            textView = binding.content;
            this.formatter = formatter;
            if (onClickListener != null) {
                binding.getRoot().setOnLongClickListener(v -> onClickListener.onLongClick(this));
                binding.getRoot().setOnClickListener(v -> onClickListener.onClick(this));
            }
        }

        public String valueToString() {
            if (formatter != null) {
                return formatter.apply(item);
            }
            return item.toString();
        }

        @Override
        public String toString() {
            return super.toString() + " '" + textView.getText() + "'";
        }
    }
}
