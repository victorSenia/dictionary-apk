package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.databinding.FragmentEditTranslationBinding;
import org.leo.dictionary.apk.helper.ValidationUtils;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

public class EditTranslationFragment extends Fragment {
    FragmentEditTranslationBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditTranslationBinding.inflate(inflater, container, false);
        int index = getArguments().getInt(EditWordActivity.TRANSLATION_INDEX_TO_EDIT);
        EditTranslationViewModel translationViewModel = new ViewModelProvider(this).get(EditTranslationViewModel.class);
        MutableLiveData<Word> word = new ViewModelProvider(requireActivity()).get(EditWordViewModel.class).getUiState();
        translationViewModel.setTranslation(word.getValue().getTranslations().get(index));
        binding.setViewmodel(translationViewModel);
        binding.playTranslation.setOnClickListener(v -> playTranslation(translationViewModel.getUiState()));
        binding.buttonDeleteTranslation.setOnClickListener(v -> removeTranslation(word, binding.getViewmodel().getUiState()));
        binding.textLanguage.requestFocus();
        return binding.getRoot();
    }

    private void playTranslation(MutableLiveData<Translation> uiState) {
        AudioService audioService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.audioService();
        audioService.play(uiState.getValue().getLanguage(), uiState.getValue().getTranslation());
    }

    public boolean isValid() {
        return ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textLanguage) & ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textTranslation);
    }

    private void removeTranslation(MutableLiveData<Word> word, MutableLiveData<Translation> uiState) {
        word.getValue().getTranslations().remove(uiState.getValue());
        requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

    public static class EditTranslationViewModel extends ViewModel {
        private final MutableLiveData<Translation> uiState = new MutableLiveData<>(new Translation());

        public MutableLiveData<Translation> getUiState() {
            return uiState;
        }

        public void setTranslation(Translation translation) {
            uiState.setValue(translation);
        }
    }
}