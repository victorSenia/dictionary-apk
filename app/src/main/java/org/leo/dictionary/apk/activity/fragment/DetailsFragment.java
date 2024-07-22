package org.leo.dictionary.apk.activity.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.activity.viewmodel.DetailsViewModel;
import org.leo.dictionary.apk.databinding.FragmentDetailsBinding;

public class DetailsFragment extends Fragment {


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentDetailsBinding binding = FragmentDetailsBinding.inflate(inflater, container, false);
        DetailsViewModel mViewModel = new ViewModelProvider(requireActivity()).get(DetailsViewModel.class);
        binding.setViewModel(mViewModel);
        binding.setLifecycleOwner(this);
        return binding.getRoot();
    }
}