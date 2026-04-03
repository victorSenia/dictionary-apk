package org.leo.dictionary.apk.activity.fragment;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.activity.viewmodel.DetailsViewModel;
import org.leo.dictionary.apk.databinding.FragmentDetailsBinding;
import org.leo.dictionary.apk.helper.KnowledgeToRatingConverter;
import org.leo.dictionary.entity.Word;

public class DetailsFragment extends Fragment implements SharedPreferences.OnSharedPreferenceChangeListener, RatingBar.OnRatingBarChangeListener {
    private static final String DETAILS_TEXT_SIZE_PORTRAIT = "org.leo.dictionary.apk.config.entity.Appearance.detailsTextSizePortrait";
    private static final String DETAILS_TEXT_SIZE_LANDSCAPE = "org.leo.dictionary.apk.config.entity.Appearance.detailsTextSizeLandscape";

    private RatingBar knowledgeBar;
    private FragmentDetailsBinding binding;
    private SharedPreferences appearancePreferences;

    private int isVisible(SharedPreferences sharedPreferences) {
        return ApkModule.isDBSource(sharedPreferences) ? View.VISIBLE : View.GONE;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDetailsBinding.inflate(inflater, container, false);
        DetailsViewModel mViewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        ApkAppComponent appComponent = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent;
        knowledgeBar = binding.knowledgeBar;
        knowledgeBar.setVisibility(isVisible(appComponent.lastState()));
        knowledgeBar.setOnRatingBarChangeListener(this);
        appComponent.lastState().registerOnSharedPreferenceChangeListener(this);
        appearancePreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        appearancePreferences.registerOnSharedPreferenceChangeListener(this);
        applyTextSizes();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ApkAppComponent appComponent = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent;
        appComponent.lastState().unregisterOnSharedPreferenceChangeListener(this);
        if (appearancePreferences != null) {
            appearancePreferences.unregisterOnSharedPreferenceChangeListener(this);
            appearancePreferences = null;
        }
        binding = null;
        knowledgeBar = null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if (ApkModule.LAST_STATE_SOURCE.equals(key)) {
            knowledgeBar.setVisibility(isVisible(sharedPreferences));
        } else if (DETAILS_TEXT_SIZE_PORTRAIT.equals(key) || DETAILS_TEXT_SIZE_LANDSCAPE.equals(key)) {
            applyTextSizes();
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

    private void applyTextSizes() {
        if (binding == null || appearancePreferences == null) {
            return;
        }
        String value = getCurrentTextSizePreference();
        if (value == null) {
            return;
        }
        float[] sizes = parseTextSizes(value);
        if (sizes == null) {
            return;
        }
        applyTextSize(binding.detailsWordText, sizes[0]);
        applyTextSize(binding.detailsAdditionalInformationText, sizes[1]);
        applyTextSize(binding.detailsTranslationsText, sizes[2]);
    }

    @Nullable
    private String getCurrentTextSizePreference() {
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        String key = isLandscape ? DETAILS_TEXT_SIZE_LANDSCAPE : DETAILS_TEXT_SIZE_PORTRAIT;
        return appearancePreferences.contains(key) ? appearancePreferences.getString(key, null) : null;
    }

    @Nullable
    private float[] parseTextSizes(@Nullable String value) {
        String[] chunks = (value == null ? "" : value).split(",");
        if (chunks.length != 3) {
            return null;
        }
        float[] sizes = new float[3];
        try {
            for (int i = 0; i < chunks.length; i++) {
                sizes[i] = Float.parseFloat(chunks[i].trim());
            }
            return sizes;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void applyTextSize(TextView textView, float sizeSp) {
        textView.setTextSize(sizeSp);
    }
}
