package org.leo.dictionary.apk.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.ActivityConfigurationPresetsBinding;
import org.leo.dictionary.apk.word.provider.DBWordProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigurationPresetsActivity extends AppCompatActivity {
    private ActivityConfigurationPresetsBinding binding;

    private static DBWordProvider getDbWordProvider(Context context) {
        return ((ApplicationWithDI) context).appComponent.dbWordProvider();
    }

    private static boolean isKeyValidForSave(String key) {
        return (key.startsWith("org.leo.dictionary.config.entity.") && !key.startsWith("org.leo.dictionary.config.entity.ParseWords."))
                || key.startsWith("org.leo.dictionary.apk.config.entity.");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConfigurationPresetsBinding.inflate(getLayoutInflater());
        LanguageViewModel nameViewModel = new ViewModelProvider(this).get(LanguageViewModel.class);
        binding.setViewModel(nameViewModel);
        binding.actionCreate.setOnClickListener(v -> savePresets(nameViewModel.getSelected()));
        binding.actionEdit.setOnClickListener(v -> updatePresets(nameViewModel.getSelected()));
        binding.actionDelete.setOnClickListener(v -> deletePresets(nameViewModel.getSelected()));
        binding.actionApply.setOnClickListener(v -> applyPresets(nameViewModel.getSelected()));
        binding.setLifecycleOwner(this);
        setContentView(binding.getRoot());
    }

    private void applyPresets(String selected) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).edit();
        for (Map.Entry<String, ?> entry : getDbWordProvider().getConfigurationPreset(selected).entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                editor.putBoolean(entry.getKey(), (Boolean) entry.getValue());
            } else {
                editor.putString(entry.getKey(), entry.getValue().toString());
            }
        }
        editor.apply();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void deletePresets(String selected) {
        getDbWordProvider().deleteConfigurationPreset(selected);
        PresetsFragment presetsFragment = (PresetsFragment) getSupportFragmentManager().findFragmentById(R.id.presets_names);
        if (presetsFragment != null) {
            presetsFragment.presets.remove(selected);
            clearName();
            presetsFragment.filterPresetsInAdapter(selected);
        }
    }

    private void clearName() {
        new ViewModelProvider(this).get(LanguageViewModel.class).setSelected("");
    }

    private DBWordProvider getDbWordProvider() {
        return getDbWordProvider(this.getApplicationContext());
    }

    private void updatePresets(String selected) {
        getDbWordProvider().updateConfigurationPreset(selected, filterSavedData());
    }

    private Map<String, Object> filterSavedData() {
        final Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext()).getAll().entrySet()) {
            if (isKeyValidForSave(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void savePresets(String selected) {
        getDbWordProvider().insertConfigurationPreset(selected, filterSavedData());
        PresetsFragment presetsFragment = (PresetsFragment) getSupportFragmentManager().findFragmentById(R.id.presets_names);
        if (presetsFragment != null) {
            presetsFragment.presets.add(selected);
            presetsFragment.presets.sort(String::compareTo);
            clearName();
            presetsFragment.filterPresetsInAdapter(selected);
        }
    }

    public static class PresetsFragment extends RecyclerViewFragment<String> {
        private List<String> presets;
        private boolean presetSelected = false;

        protected void filterPresetsInAdapter(String filterString) {
            StringRecyclerViewAdapter<String> adapter = getRecyclerViewAdapter();
            if (adapter != null) {
                adapter.clearAdapter();
                adapter.values.addAll(filterPresets(filterString));
                adapter.setSelected(adapter.values.indexOf(filterString));
                presetSelected = adapter.getSelected() != RecyclerView.NO_POSITION;
                updateButtonsVisibility();
                adapter.notifyDataSetChanged();
            }
        }

        private void updateButtonsVisibility() {
            requireActivity().findViewById(R.id.action_create).setVisibility(!presetSelected && getNameViewModel().getSelected() != null && !getNameViewModel().getSelected().isEmpty() ? View.VISIBLE : View.GONE);
            requireActivity().findViewById(R.id.action_edit).setVisibility(presetSelected ? View.VISIBLE : View.GONE);
            requireActivity().findViewById(R.id.action_delete).setVisibility(presetSelected ? View.VISIBLE : View.GONE);
            requireActivity().findViewById(R.id.action_apply).setVisibility(presetSelected ? View.VISIBLE : View.GONE);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getNameViewModel().getData().observe(requireActivity(), this::filterPresetsInAdapter);
        }

        protected List<String> filterPresets(String filterString) {
            if (!filterString.isEmpty()) {
                return presets.stream().filter(t -> t.contains(filterString)).collect(Collectors.toList());
            }
            return presets;
        }

        protected List<String> getStrings() {
            presets = findPresets();
            updateButtonsVisibility();
            return new ArrayList<>(presets);
        }

        protected List<String> findPresets() {
            DBWordProvider wordProvider = getDbWordProvider(requireActivity().getApplicationContext());
            return wordProvider.getConfigurationPresetNames();
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter() {
            recyclerView.setNestedScrollingEnabled(false);
            return new StringRecyclerViewAdapter<>(getStrings(), this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>((oldSelected, viewHolder) -> getNameViewModel().setSelected(viewHolder.item)));
        }

        private LanguageViewModel getNameViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }
    }
}