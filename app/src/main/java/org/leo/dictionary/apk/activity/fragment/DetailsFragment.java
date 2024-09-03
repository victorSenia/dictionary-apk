package org.leo.dictionary.apk.activity.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.viewmodel.DetailsViewModel;
import org.leo.dictionary.apk.databinding.FragmentDetailsBinding;
import org.leo.dictionary.apk.helper.KnowledgeToRatingConverter;
import org.leo.dictionary.entity.Word;

public class DetailsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, RatingBar.OnRatingBarChangeListener {

    private RatingBar knowledgeBar;

    private static int isVisible(SharedPreferences sharedPreferences) {
        return ApkModule.isDBSource(sharedPreferences) ? View.VISIBLE : View.GONE;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentDetailsBinding binding = FragmentDetailsBinding.inflate(inflater, container, false);
        DetailsViewModel mViewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        ApkAppComponent appComponent = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent;
        knowledgeBar = binding.knowledgeBar;
        knowledgeBar.setVisibility(isVisible(appComponent.lastState()));
        knowledgeBar.setOnRatingBarChangeListener(this);
        appComponent.lastState().registerOnSharedPreferenceChangeListener(this);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ApkAppComponent appComponent = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent;
        appComponent.lastState().unregisterOnSharedPreferenceChangeListener(this);
        knowledgeBar = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (ApkModule.LAST_STATE_SOURCE.equals(key)) {
            knowledgeBar.setVisibility(isVisible(sharedPreferences));
        }
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        if (fromUser) {
            double knowledge = KnowledgeToRatingConverter.ratingToKnowledge(rating);
            Word word = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class).getValue();
            word.setKnowledge(knowledge);
            ApkAppComponent appComponent = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent;
            appComponent.dbWordProvider().updateWord(word);
        }
    }
}