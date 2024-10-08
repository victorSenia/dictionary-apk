package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.fragment.RecyclerViewFragment;
import org.leo.dictionary.apk.activity.viewadapter.ReturnSelectedStringRecyclerViewAdapter;
import org.leo.dictionary.apk.activity.viewadapter.StringRecyclerViewAdapter;
import org.leo.dictionary.apk.word.provider.AssetsWordProvider;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AssetsActivity extends AppCompatActivity {

    public static final String FOLDER_NAME = "FOLDER_NAME";

    private String getFolderName() {
        Bundle b = getIntent().getExtras();
        if (b != null) {
            return b.getString(FOLDER_NAME, AssetsWordProvider.ASSETS_WORDS);
        }
        return AssetsWordProvider.ASSETS_WORDS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.assets_activity);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class AssetsFragment extends RecyclerViewFragment<StringRecyclerViewAdapter<String>, String> {
        @Override
        protected List<String> getValues() {
            try {
                return Arrays.asList(requireActivity().getAssets().list(((AssetsActivity) requireActivity()).getFolderName()));
            } catch (IOException e) {
                MainActivity.logUnhandledException(e);
                return Collections.emptyList();
            }
        }

        @Override
        protected StringRecyclerViewAdapter<String> createRecyclerViewAdapter(List<String> values) {
            return new ReturnSelectedStringRecyclerViewAdapter(values, this);
        }
    }
}