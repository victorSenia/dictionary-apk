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
import org.leo.dictionary.apk.helper.ValidationUtils;
import org.leo.dictionary.entity.Word;

public class EditWordFragment extends Fragment {
    private FragmentEditWordBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentEditWordBinding.inflate(inflater, container, false);
        EditWordViewModel viewmodel = new ViewModelProvider(requireActivity()).get(EditWordViewModel.class);
        binding.setViewmodel(viewmodel);
        binding.playWord.setOnClickListener(v -> playWord(viewmodel.getUiState()));
        return binding.getRoot();
    }

    public boolean isValid() {
        return ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textLanguage) & ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textWord);
    }

    private void playWord(MutableLiveData<Word> uiState) {
        try {
            ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.audioService().play(uiState.getValue().getLanguage(), uiState.getValue().getFullWord());
        } catch (InterruptedException e) {
            //ignore
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}