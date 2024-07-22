package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import org.leo.dictionary.ExternalVoiceService;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityVoiceSelectorBinding;

import java.util.ArrayList;
import java.util.List;

public class VoiceSelectorActivity extends AppCompatActivity {

    private static String getLanguage(View view) {
        return ((EditText) view.findViewById(R.id.language)).getText().toString().trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityVoiceSelectorBinding binding = ActivityVoiceSelectorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.findVoices.setOnClickListener(v -> updateUiWithWords(getLanguage(binding.getRoot())));
        binding.defaultVoice.setOnClickListener(v -> {
            clearSelection();
            String language = getLanguage(binding.getRoot());
            ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().edit()
                    .remove(ApkModule.LAST_STATE_VOICE + language).apply();
            Toast.makeText(getBaseContext(), "default voice used for " + language, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUiWithWords(String language) {
        if (language.length() == 2) {
            ExternalVoiceService voiceService = ((ApplicationWithDI) getApplicationContext()).appComponent.externalVoiceService();
            voiceService.getVoicesNames(language);
            updateUiWithNewData(language, voiceService.getVoicesNames(language));
        }
    }

    private void updateUiWithNewData(String language, List<String> voices) {
        StringRecyclerViewAdapter adapter = getStringRecyclerViewAdapter();
        adapter.clearSelection();
        adapter.mValues.clear();
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
        StringRecyclerViewAdapter adapter = getStringRecyclerViewAdapter();
        adapter.clearSelection();
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
            return new StringRecyclerViewAdapter(getStrings(), this, new VoiceSelectionOnClickListener(this));
        }
    }

    public static class VoiceSelectionOnClickListener extends StringRecyclerViewAdapter.RememberSelectionOnClickListener {

        private final Fragment fragment;

        public VoiceSelectionOnClickListener(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void onClick(StringRecyclerViewAdapter.StringViewHolder viewHolder) {
            super.onClick(viewHolder);
            String language = getLanguage(fragment.getView().getRootView());
            ((ApplicationWithDI) fragment.requireActivity().getApplicationContext()).appComponent.lastState().edit()
                    .putString(ApkModule.LAST_STATE_VOICE + language, viewHolder.mItem).apply();
            Toast.makeText(fragment.requireActivity().getBaseContext(), viewHolder.mItem + " used for " + language, Toast.LENGTH_SHORT).show();
        }
    }
}