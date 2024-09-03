package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import org.leo.dictionary.PlayService;
import org.leo.dictionary.PlayServiceImpl;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.viewadapter.ReturnSelectedStringRecyclerViewAdapter;
import org.leo.dictionary.apk.databinding.ParseWordsSettingsActivityBinding;
import org.leo.dictionary.apk.word.provider.AssetsWordProvider;
import org.leo.dictionary.word.provider.WordProvider;

import java.io.FileNotFoundException;

public class ParseWordsSettingsActivity extends AppCompatActivity {
    private ParseWordsSettingsActivityBinding binding;
    private Object uri;
    private String type;
    private final ActivityResultLauncher<Intent> filesActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    uri = result.getData().getData();
                    binding.file.setText(getFileName(result.getData().getData()));
                    binding.asset.setText(R.string.asset);
                    type = ApkModule.FILE;
                }
            });
    private final ActivityResultLauncher<Intent> assetsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    String string = data.getStringExtra(ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA);
                    binding.asset.setText(string);
                    binding.file.setText(R.string.file);
                    uri = string;
                    type = ApkModule.ASSET;
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ParseWordsSettingsActivityBinding.inflate(getLayoutInflater());
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        setContentView(binding.getRoot());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding.file.setOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("text/plain");
            chooseFile = Intent.createChooser(chooseFile, getString(R.string.choose_file));
            filesActivityResultLauncher.launch(chooseFile);
        });
        binding.asset.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssetsActivity.class);
            Bundle b = new Bundle();
            b.putString(AssetsActivity.FOLDER_NAME, AssetsWordProvider.ASSETS_WORDS);
            intent.putExtras(b);
            assetsActivityResultLauncher.launch(intent);
        });
        binding.parseWords.setOnClickListener(v -> {
            if (type != null) {
                try {
                    if (ApkModule.ASSET.equals(type)) {
                        ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().edit()
                                .putString(ApkModule.LAST_STATE_SOURCE, type)
                                .putString(ApkModule.LAST_STATE_URI, uri.toString())
                                .apply();
                    } else if (ApkModule.FILE.equals(type)) {//TODO for files URI is not correct; and problems with rights
                        ((ApplicationWithDI) getApplicationContext()).appComponent.lastState().edit()
                                .putString(ApkModule.LAST_STATE_SOURCE, type)
                                .putString(ApkModule.LAST_STATE_URI, uri.toString())
                                .apply();
                    }
                    updateWordProvider(uri);

                    Intent intent = new Intent();
                    intent.putExtra(ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA, ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } catch (FileNotFoundException e) {
                    MainActivity.logUnhandledException(e);
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private String getFileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex > 0) {
                    return cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            MainActivity.logUnhandledException(e);
            return "";
        }
        return "";
    }

    private void updateWordProvider(Object data) throws FileNotFoundException {
        WordProvider wordProvider;
        if (data instanceof Uri) {
            wordProvider = ApkModule.createInputStreamWordProvider(getApplicationContext(), (Uri) data);
        } else {
            wordProvider = ApkModule.createWordProvider(getApplicationContext(), ((ApplicationWithDI) getApplicationContext()).appComponent.lastState(), ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider());
        }
        PlayService playService = ((ApplicationWithDI) getApplicationContext()).appComponent.playService();
        ((PlayServiceImpl) playService).setWordProvider(wordProvider);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.parse_words_preferences, rootKey);
        }
    }
}