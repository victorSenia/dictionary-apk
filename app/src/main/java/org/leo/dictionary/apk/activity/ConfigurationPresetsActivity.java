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
import org.leo.dictionary.apk.activity.fragment.FilteredRecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewmodel.LanguageViewModel;
import org.leo.dictionary.apk.databinding.ActivityConfigurationPresetsBinding;
import org.leo.dictionary.word.provider.DBWordProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        binding.actionCreate.setOnClickListener(v -> savePresets(nameViewModel.getValue()));
        binding.actionEdit.setOnClickListener(v -> updatePresets(nameViewModel.getValue()));
        binding.actionDelete.setOnClickListener(v -> deletePresets(nameViewModel.getValue()));
        binding.actionApply.setOnClickListener(v -> applyPresets(nameViewModel.getValue()));
        binding.setLifecycleOwner(this);
        PresetsFragment presetsFragment = (PresetsFragment) getSupportFragmentManager().findFragmentById(R.id.presets_names);
        nameViewModel.getData().observe(this, v -> presetsFragment.setFilterValue(v));

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
            presetsFragment.getAllValues().remove(selected);
            clearName();
        }
    }

    private void clearName() {
        new ViewModelProvider(this).get(LanguageViewModel.class).setValue("");
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
            presetsFragment.getAllValues().add(selected);
            presetsFragment.getAllValues().sort(String::compareTo);
            clearName();
        }
    }

    public static class PresetsFragment extends FilteredRecyclerViewFragment<StringRecyclerViewAdapter<String>, String> {
        private boolean presetSelected = false;

        List<String> getAllValues() {
            return allValues;
        }

        @Override
        protected void setSelectedValues(StringRecyclerViewAdapter<String> adapter) {
            adapter.setSelected(adapter.values.indexOf(filter.getText().toString()));
            presetSelected = adapter.getSelected() != RecyclerView.NO_POSITION;
            updateButtonsVisibility();
        }

        @Override
        protected boolean stateChanged() {
            return false;
        }

        @Override
        protected void setFilterVisibility() {
            updateButtonsVisibility();
        }

        private void updateButtonsVisibility() {
            requireActivity().findViewById(R.id.action_create).setVisibility(!presetSelected && !filter.getText().toString().isEmpty() ? View.VISIBLE : View.GONE);
            requireActivity().findViewById(R.id.action_edit).setVisibility(presetSelected ? View.VISIBLE : View.GONE);
            requireActivity().findViewById(R.id.action_delete).setVisibility(presetSelected ? View.VISIBLE : View.GONE);
            requireActivity().findViewById(R.id.action_apply).setVisibility(presetSelected ? View.VISIBLE : View.GONE);
        }

        protected List<String> findValues() {
            DBWordProvider wordProvider = getDbWordProvider(requireActivity().getApplicationContext());
            return wordProvider.getConfigurationPresetNames();
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter(List<String> values) {
            recyclerView.setNestedScrollingEnabled(false);
            return new StringRecyclerViewAdapter<>(values, this,
                    new StringRecyclerViewAdapter.RememberSelectionOnClickListener<>((oldSelected, viewHolder) -> getNameViewModel().setValue(viewHolder.item)));
        }

        private LanguageViewModel getNameViewModel() {
            return new ViewModelProvider(requireActivity()).get(LanguageViewModel.class);
        }

        public void setFilterValue(String v) {
            filter.setText(v);
        }
    }
}