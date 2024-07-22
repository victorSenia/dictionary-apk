package org.leo.dictionary.apk.activity.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.EditWordActivity;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewadapter.TopicRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.EditWordViewModel;
import org.leo.dictionary.apk.activity.viewmodel.TopicViewModel;
import org.leo.dictionary.apk.databinding.DialogEditTopicBinding;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Topic;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EditTopicFragment extends Fragment {

    // TODO: Customize parameter argument names
    private static final String ARG_COLUMN_COUNT = "column-count";
    // TODO: Customize parameters
    private int mColumnCount = 1;
    private RecyclerView recyclerView;

    public void replaceData(List<Topic> topics) {
        ((TopicRecyclerViewAdapter) recyclerView.getAdapter()).replaceData(topics);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_topic_list, container, false);
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            Word word = new ViewModelProvider(requireActivity()).get(EditWordViewModel.class).getValue();
            recyclerView.setAdapter(createRecyclerViewAdapter(word));
        }
        return view;
    }

    private TopicRecyclerViewAdapter createRecyclerViewAdapter(Word word) {
        return new TopicRecyclerViewAdapter(word.getTopics()) {
            @Override
            public void deleteItem(DeleteViewHolder viewHolder) {
                word.getTopics().remove(viewHolder.mItem);
                ((EditWordActivity) requireActivity()).filterTopics();
                super.deleteItem(viewHolder);
            }

            @Override
            public void editItem(DeleteViewHolder viewHolder) {
                new EditTopicDialogFragment(viewHolder.mItem,
                        () -> viewHolder.getBindingAdapter().notifyItemChanged(viewHolder.getAbsoluteAdapterPosition())).
                        show(requireActivity().getSupportFragmentManager(), "EditTopic");
            }
        };
    }

    public static class EditTopicDialogFragment extends DialogFragment {
        private final Runnable onSafeConsumer;
        private final Topic topicToEdit;
        private List<Topic> topics;
        private List<Topic> filteredTopics = new ArrayList<>();
        private DialogEditTopicBinding binding;

        public EditTopicDialogFragment(Topic topic, Runnable onSafeConsumer) {
            this.topicToEdit = topic;
            this.onSafeConsumer = onSafeConsumer;
        }

        private List<Topic> findTopics(String language) {
            DBWordProvider wordProvider = ((ApplicationWithDI) getContext().getApplicationContext()).appComponent.dbWordProvider();
            return wordProvider.findRootTopics(language);
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            topics = findTopics(topicToEdit.getLanguage());
            binding = DialogEditTopicBinding.inflate(inflater, container, false);
            TopicViewModel topicViewModel = new ViewModelProvider(this).get(TopicViewModel.class);
            topicViewModel.setTopic(topicToEdit);
            binding.setViewModel(topicViewModel);
            binding.setLifecycleOwner(this);
            binding.buttonSave.setOnClickListener(v -> {
                DBWordProvider wordProvider = ((ApplicationWithDI) getContext().getApplicationContext()).appComponent.dbWordProvider();
                wordProvider.updateTopic(topicViewModel.getTopic());
                if (onSafeConsumer != null) {
                    onSafeConsumer.run();
                }
                dismiss();
            });
            boolean isRootTopic = topicToEdit.getLevel() == 1;
            if (isRootTopic) {
                binding.rootTopicFullContainer.setVisibility(View.GONE);
            } else {
                rootTopicVisibility(topicToEdit.getRoot());
                binding.textTopic.addTextChangedListener(new EditWordActivity.AbstractTextWatcher() {
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        filterTopics(s);
                    }
                });
                binding.topicList.setLayoutManager(new LinearLayoutManager(binding.topicList.getContext()));
                binding.topicList.setAdapter(new TopicRecyclerViewAdapter(filteredTopics) {
                    @Override
                    @NonNull
                    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        return createStringViewHolder(parent);
                    }

                    @Override
                    protected void onClickListener(StringRecyclerViewAdapter.StringViewHolder<Topic> viewHolder) {
                        Topic newRoot = filteredTopics.get(viewHolder.getAbsoluteAdapterPosition());
                        selectNewRoot(newRoot);
                    }
                });
                binding.createTopic.setOnClickListener(v -> createAndAddRootTopic());
                binding.actionDelete.setOnClickListener(v -> selectNewRoot(null));
                binding.actionEdit.setOnClickListener(v -> editRoot());
            }
            return binding.getRoot();
        }

        @Override
        public void onStart() {
            super.onStart();
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        private void editRoot() {
            TopicViewModel rootTopicViewModel = new ViewModelProvider(this).get(TopicViewModel.class);
            new EditTopicDialogFragment(topicToEdit.getRoot(), () -> rootTopicViewModel.setTopic(rootTopicViewModel.getTopic())).show(requireActivity().getSupportFragmentManager(), "EditRootTopic");
        }

        private void selectNewRoot(Topic newRoot) {
            rootTopicVisibility(newRoot);
            TopicViewModel rootTopicViewModel = new ViewModelProvider(this).get(TopicViewModel.class);
            rootTopicViewModel.getTopic().setRoot(newRoot);
            filterTopics();
            rootTopicViewModel.setTopic(rootTopicViewModel.getTopic());
        }

        private void rootTopicVisibility(Topic newRoot) {
            binding.rootTopicContainer.setVisibility(newRoot == null ? View.GONE : View.VISIBLE);
        }

        private void filterTopics() {
            filterTopics(binding.textTopic.getText().toString());
        }

        private void createAndAddRootTopic() {
            TopicViewModel rootTopicViewModel = new ViewModelProvider(this).get(TopicViewModel.class);
            Topic topic = new Topic();
            topic.setName(binding.textTopic.getText().toString());
            topic.setLevel(1);
            topic.setLanguage(rootTopicViewModel.getTopic().getLanguage());
            topics.add(topic);
            selectNewRoot(topic);
        }

        private void filterTopics(CharSequence input) {
            if (input.length() > 0) {
                filteredTopics = topics.stream().
                        filter(topic -> topic.getName().contains(input)).
                        filter(topic -> !Objects.equals(topicToEdit.getRoot(), topic)).collect(Collectors.toList());
            } else {
                filteredTopics = new ArrayList<>();
            }
            ((TopicRecyclerViewAdapter) binding.topicList.getAdapter()).replaceData(filteredTopics);
        }
    }

}