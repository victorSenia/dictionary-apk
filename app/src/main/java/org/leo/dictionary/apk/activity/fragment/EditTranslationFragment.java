package org.leo.dictionary.apk.activity.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.EditWordActivity;
import org.leo.dictionary.apk.activity.viewmodel.EditTranslationViewModel;
import org.leo.dictionary.apk.activity.viewmodel.EditWordViewModel;
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
        boolean requestFocus = getArguments().getBoolean(EditWordActivity.REQUEST_FOCUS, false);
        EditTranslationViewModel translationViewModel = new ViewModelProvider(this).get(EditTranslationViewModel.class);
        Word word = new ViewModelProvider(requireActivity()).get(EditWordViewModel.class).getValue();
        translationViewModel.setTranslation(word.getTranslations().get(index));
        binding.setViewModel(translationViewModel);
        binding.playTranslation.setOnClickListener(v -> playTranslation(translationViewModel.getTranslation()));
        binding.buttonDeleteTranslation.setOnClickListener(v -> removeTranslation(word, binding.getViewModel().getTranslation()));
        if (requestFocus) {
            binding.textLanguage.requestFocus();
        }
        return binding.getRoot();
    }

    private void playTranslation(Translation translation) {
        AudioService audioService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.audioService();
        audioService.play(translation.getLanguage(), translation.getTranslation());
    }

    public boolean isValid() {
        return ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textLanguage) & ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textTranslation);
    }

    private void removeTranslation(Word word, Translation translation) {
        word.getTranslations().remove(translation);
        requireActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
    }

}