package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.viewadapter.ReturnSelectedStringRecyclerViewAdapter;
import org.leo.dictionary.apk.databinding.ParseWordsSettingsActivityBinding;
import org.leo.dictionary.apk.word.provider.AssetsWordProvider;
import org.leo.dictionary.config.entity.ParseWords;
import org.leo.dictionary.word.provider.FileWordProvider;
import org.leo.dictionary.word.provider.WordProvider;
import org.leo.dictionary.word.provider.WordProviderDelegate;

import java.io.FileNotFoundException;

public class ParseWordsSettingsActivity extends AppCompatActivity {
    private ParseWordsSettingsActivityBinding binding;
    private Object uri;
    private String type;
    private WordProvider selectedWordProvider;
    private final ActivityResultLauncher<Intent> filesActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null || data.getData() == null) {
                        return;
                    }
                    Uri selectedUri = data.getData();
                    persistReadPermissionIfPossible(data, selectedUri);
                    uri = selectedUri;
                    binding.file.setText(getFileName(selectedUri));
                    binding.asset.setText(R.string.asset);
                    type = ApkModule.FILE;
                    selectedWordProvider = ApkModule.createInputStreamWordProvider(getApplicationContext(), selectedUri);
                    updateSettingsVisibility(selectedWordProvider);
                }
            });
    private final ActivityResultLauncher<Intent> assetsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data == null) {
                        return;
                    }
                    String string = data.getStringExtra(ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA);
                    binding.asset.setText(string);
                    binding.file.setText(R.string.file);
                    uri = string;
                    type = ApkModule.ASSET;
                    ParseWords configuration = ApkModule.provideParseWordsConfiguration(getApplicationContext());
                    configuration.setPath(string);
                    selectedWordProvider = ApkModule.createAssetsWordProvider(configuration, getApplicationContext());
                    updateSettingsVisibility(selectedWordProvider);
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
        View root = binding.getRoot();
        setContentView(root);
        ActivityUtils.setFullScreen(this, root);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding.file.setOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            chooseFile.setType("text/plain");
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
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
                    updateWordProvider();

                    Intent intent = new Intent();
                    intent.putExtra(ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA, ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA);
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                } catch (FileNotFoundException e) {
                    ActivityUtils.logUnhandledException(e);
                    type = null;
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        selectedWordProvider = null;
        uri = null;
        type = null;
        binding = null;
    }

    private String getFileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (columnIndex >= 0) {
                    return cursor.getString(columnIndex);
                }
            }
        } catch (Exception e) {
            ActivityUtils.logUnhandledException(e);
            return "";
        }
        return "";
    }

    private void updateWordProvider() throws FileNotFoundException {
        if (selectedWordProvider == null) {
            throw new FileNotFoundException("Word provider is not selected");
        }
        WordProviderDelegate wordProviderDelegate = (WordProviderDelegate) ((ApplicationWithDI) getApplicationContext()).appComponent.externalWordProvider();
        wordProviderDelegate.setWordProvider(selectedWordProvider);
    }

    private void updateSettingsVisibility(WordProvider wordProvider) {
        int visibility = View.VISIBLE;
        if (wordProvider instanceof FileWordProvider && ((FileWordProvider) wordProvider).isConfigurationParsed()) {
            visibility = View.GONE;
        }
        binding.settings.setVisibility(visibility);
    }

    private void persistReadPermissionIfPossible(Intent data, Uri selectedUri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int grantedFlags = data.getFlags();
            if ((grantedFlags & Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) {
                return;
            }
            try {
                getContentResolver().takePersistableUriPermission(selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                ActivityUtils.logUnhandledException(e);
            }
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.parse_words_preferences, rootKey);
        }
    }
}
