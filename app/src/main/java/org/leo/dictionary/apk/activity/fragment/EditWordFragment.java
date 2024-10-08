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
import org.leo.dictionary.apk.activity.viewmodel.EditWordViewModel;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.FragmentEditWordBinding;
import org.leo.dictionary.apk.helper.ValidationUtils;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.entity.Word;

public class EditWordFragment extends Fragment {
    private FragmentEditWordBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditWordBinding.inflate(inflater, container, false);
        LanguageViewModel languageViewModel = new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        binding.textLanguage.addTextChangedListener(new EditWordActivity.AbstractTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                languageViewModel.setSelected(s);
            }
        });
        EditWordViewModel wordViewModel = new ViewModelProvider(requireActivity()).get(EditWordViewModel.class);
        binding.setViewModel(wordViewModel);
        binding.playWord.setOnClickListener(v -> playWord(wordViewModel.getValue()));
        return binding.getRoot();
    }

    public boolean isValid() {
        return ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textLanguage) & ValidationUtils.isEmptySetEmptyErrorIfNot(binding.textWord);
    }

    private void playWord(Word word) {
        AudioService audioService = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.audioService();
        audioService.play(word.getLanguage(), word.getFullWord());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}