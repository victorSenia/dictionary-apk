package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.UiUpdater;
import org.leo.dictionary.apk.ApkUiUpdater;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.databinding.FragmentDetailsBinding;

public class DetailsFragment extends Fragment {

    private UiUpdater uiUpdater;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentDetailsBinding binding = FragmentDetailsBinding.inflate(inflater, container, false);
        DetailsViewModel mViewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
        binding.setViewmodel(mViewModel);
        binding.setLifecycleOwner(this);
        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.uiUpdater();
        uiUpdater = mViewModel::updateWord;
        apkUiUpdater.addUiUpdater(uiUpdater);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ApkUiUpdater apkUiUpdater = (ApkUiUpdater) ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.uiUpdater();
        apkUiUpdater.removeUiUpdater(uiUpdater);
        uiUpdater = null;
    }
}