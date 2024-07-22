package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.ExternalVoiceService;
import org.leo.dictionary.ExternalWordProvider;
import org.leo.dictionary.apk.ApkAppComponent;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.ActivityVoiceSelectorBinding;
import org.leo.dictionary.audio.AudioService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VoiceSelectorActivity extends AppCompatActivity {
    private ActivityVoiceSelectorBinding binding;

    private static void playTestString(Context activity, String language) {
        AudioService audioService = ((ApplicationWithDI) activity.getApplicationContext()).appComponent.audioService();
        String string = activity.getString(R.string.default_text);
        ApkModule.playAsynchronousIfPossible(audioService, language, string);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageViewModel languageViewModel = new ViewModelProvider(this).get(LanguageViewModel.class);
        languageViewModel.setSelected(getString(R.string.default_language));
        binding = ActivityVoiceSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        languageViewModel.getData().observe(this, this::updateUiWithWords);
        binding.setViewModel(languageViewModel);
        binding.setLifecycleOwner(this);
        binding.defaultVoice.setOnClickListener(v -> {
            getStringRecyclerViewAdapter().clearSelection();
            String language = languageViewModel.getSelected();
            ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().edit()
                    .remove(ApkModule.LAST_STATE_VOICE + language).apply();
            playTestString(this, language);
            Toast.makeText(this, "Default voice used for " + language, Toast.LENGTH_SHORT).show();
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
            StringRecyclerViewAdapter<String> adapter = getStringRecyclerViewAdapter();
            adapter.clearAdapter();
            adapter.notifyDataSetChanged();
            binding.defaultVoice.setVisibility(View.GONE);
        }
    }

    private void updateUiWithNewData(String language, List<String> voices) {
        StringRecyclerViewAdapter<String> adapter = getStringRecyclerViewAdapter();
        adapter.clearAdapter();
        adapter.values.addAll(voices);
        String selectedVoice = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().getString(ApkModule.LAST_STATE_VOICE + language, null);
        if (selectedVoice != null) {
            int index = voices.indexOf(selectedVoice);
            if (index != -1) {
                adapter.setSelected(index);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private StringRecyclerViewAdapter<String> getStringRecyclerViewAdapter() {
        VoicesFragment fragment = (VoicesFragment) getSupportFragmentManager().findFragmentById(R.id.voices);
        return fragment.getRecyclerViewAdapter();
    }

    public static class VoicesFragment extends RecyclerViewFragment<String> {
        @Override
        protected List<String> getStrings() {
            return new ArrayList<>();
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            return new StringRecyclerViewAdapter<>(getStrings(), this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>((oldSelected, viewHolder) ->
                    {
                        LanguageViewModel languageViewModel = new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
                        String language = languageViewModel.getSelected();
                        ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent.lastState().edit()
                                .putString(ApkModule.LAST_STATE_VOICE + language, viewHolder.valueToString()).apply();
                        playTestString(requireActivity(), language);
                        Toast.makeText(requireActivity().getBaseContext(), viewHolder.valueToString() + " used for " + language, Toast.LENGTH_SHORT).show();
                    }));
        }

    }

    public static class LanguagesFragment extends RecyclerViewFragment<String> {
        @Override
        protected List<String> getStrings() {
            ApkAppComponent appComponent = ((ApplicationWithDI) requireActivity().getApplicationContext()).appComponent;
            ExternalWordProvider wordProvider = appComponent.externalWordProvider();
            Set<String> result = new HashSet<>();
            result.addAll(wordProvider.languageFrom());
            result.addAll(wordProvider.languageTo(null));
            result.addAll(appComponent.dbWordProvider().languageFrom());
            result.addAll(appComponent.dbWordProvider().languageTo(null));
            return result.stream().sorted().collect(Collectors.toList());
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            return new StringRecyclerViewAdapter<>(getStrings(), this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>(
                            (oldSelected, viewHolder) -> getLanguageViewModel().setSelected(viewHolder.valueToString())
                    ));
        }

        private LanguageViewModel getLanguageViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }
    }

}