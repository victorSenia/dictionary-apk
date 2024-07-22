package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.ExternalVoiceService;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityVoiceSelectorBinding;

import java.util.ArrayList;
import java.util.List;

public class VoiceSelectorActivity extends AppCompatActivity {
    private ActivityVoiceSelectorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FilterWordsActivity.LanguageViewModel languageViewModel = new ViewModelProvider(this).get(FilterWordsActivity.LanguageViewModel.class);
        languageViewModel.setSelected(getString(R.string.default_language));
        binding = ActivityVoiceSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        languageViewModel.getData().observe(this, this::updateUiWithWords);
        binding.setViewmodel(languageViewModel);
        binding.setLifecycleOwner(this);
        binding.defaultVoice.setOnClickListener(v -> {
            clearSelection();
            String language = languageViewModel.getSelected();
            ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().edit()
                    .remove(ApkModule.LAST_STATE_VOICE + language).apply();
            Toast.makeText(getBaseContext(), "Default voice used for " + language, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void updateUiWithWords(String language) {
        if (language.length() == 2) {
            ExternalVoiceService voiceService = ((ApplicationWithDI) getApplicationContext()).appComponent.externalVoiceService();
            voiceService.getVoicesNames(language);
            updateUiWithNewData(language, voiceService.getVoicesNames(language));
            binding.defaultVoice.setVisibility(View.VISIBLE);
        } else {
            StringRecyclerViewAdapter adapter = getStringRecyclerViewAdapter();
            clearAdapter(adapter);
            adapter.notifyDataSetChanged();
            binding.defaultVoice.setVisibility(View.GONE);
        }
    }

    private static void clearAdapter(StringRecyclerViewAdapter adapter) {
        adapter.clearSelection();
        adapter.mValues.clear();
    }

    private void updateUiWithNewData(String language, List<String> voices) {
        StringRecyclerViewAdapter adapter = getStringRecyclerViewAdapter();
        clearAdapter(adapter);
        adapter.mValues.addAll(voices);
        String selectedVoice = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().getString(ApkModule.LAST_STATE_VOICE + language, null);
        if (selectedVoice != null) {
            int index = voices.indexOf(selectedVoice);
            if (index != -1) {
                adapter.setSelected(index);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void clearSelection() {
        getStringRecyclerViewAdapter().clearSelection();
    }

    private StringRecyclerViewAdapter getStringRecyclerViewAdapter() {
        VoicesFragment fragment = (VoicesFragment) getSupportFragmentManager().findFragmentById(R.id.voices);
        return (StringRecyclerViewAdapter) fragment.recyclerView.getAdapter();
    }

    public static class VoicesFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            return new ArrayList<>();
        }

        @Override
        protected StringRecyclerViewAdapter createRecyclerViewAdapter() {
            return new StringRecyclerViewAdapter(getStrings(), this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener(viewHolder ->
                    {
                        FilterWordsActivity.LanguageViewModel languageViewModel = new ViewModelProvider(requireActivity()).get(FilterWordsActivity.LanguageViewModel.class);
                        String language = languageViewModel.getSelected();
                        ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.lastState().edit()
                                .putString(ApkModule.LAST_STATE_VOICE + language, viewHolder.mItem).apply();
                        Toast.makeText(requireActivity().getBaseContext(), viewHolder.mItem + " used for " + language, Toast.LENGTH_SHORT).show();
                    }));
        }
    }
}