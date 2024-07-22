package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.leo.dictionary.PlayService;
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
        binding.selectVoice.setOnClickListener(v -> {
            updateUiWithWords(getLanguage(binding.getRoot()));
        });
        binding.defaultVoice.setOnClickListener(v -> {
            String language = getLanguage(binding.getRoot());
            ApkModule.provideLastState(getApplicationContext()).edit()
                    .remove(ApkModule.LAST_STATE_VOICE + language).apply();
            Toast.makeText(getBaseContext(), "default voice used for " + language, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUiWithWords(String language) {
        if (language.length() == 2) {
            PlayService playService = ((ApplicationWithDI) getApplicationContext()).appComponent.playService();
            playService.getVoicesNames(language);
            updateUiWithNewData(playService.getVoicesNames(language));
        }
    }

    private void updateUiWithNewData(List<String> voices) {
        VoicesFragment fragment = (VoicesFragment) getSupportFragmentManager().findFragmentById(R.id.voices);
        StringRecyclerViewAdapter adapter = (StringRecyclerViewAdapter) fragment.recyclerView.getAdapter();
        adapter.mValues.clear();
        adapter.mValues.addAll(voices);
        adapter.notifyDataSetChanged();
    }

    public static class VoicesFragment extends StringsFragment {
        @Override
        protected List<String> getStrings() {
            return new ArrayList<>();
        }

        @Override
        protected StringRecyclerViewAdapter getRecyclerViewAdapter() {
            return new StringRecyclerViewAdapter(getStrings(), this) {
                @Override
                protected View.OnClickListener getOnClickListener(ViewHolder viewHolder) {
                    return v -> {
                        String language = getLanguage(getView().getRootView());
                        ApkModule.provideLastState(getActivity().getApplicationContext()).edit()
                                .putString(ApkModule.LAST_STATE_VOICE + language, viewHolder.mItem).apply();
                        Toast.makeText(getActivity().getBaseContext(), viewHolder.mItem + " used for " + language, Toast.LENGTH_SHORT).show();
                    };
                }
            };
        }
    }

}