package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.databinding.FragmentEditWordBinding;
import org.leo.dictionary.entity.Word;

public class EditWordFragment extends Fragment {


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentEditWordBinding binding = FragmentEditWordBinding.inflate(inflater, container, false);
        EditWordViewModel viewmodel = new ViewModelProvider(requireActivity()).get(EditWordViewModel.class);
        binding.setViewmodel(viewmodel);
        binding.playWord.setOnClickListener(v -> playWord(viewmodel.getUiState()));
        return binding.getRoot();
    }


    private void playWord(MutableLiveData<Word> uiState) {
        try {
            ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.audioService().play(uiState.getValue().getLanguage(), uiState.getValue().getFullWord());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}