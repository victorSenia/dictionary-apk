package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.activity.viewadapter.ReturnSelectedStringRecyclerViewAdapter;
import org.leo.dictionary.apk.databinding.ActivitySentenceBinding;
import org.leo.dictionary.apk.grammar.provider.AssetsSentenceProvider;
import org.leo.dictionary.apk.helper.SentenceProviderHolder;
import org.leo.dictionary.entity.Sentence;
import org.leo.dictionary.entity.SentenceCriteria;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.grammar.provider.SentenceProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SentenceActivity extends AppCompatActivity {
    public static final String CONFIG_PREFIX = "org.leo.dictionary.apk.config.entity.SentenceOrder.";
    public static final int CORRECT_DELAY_MILLIS = 1500;
    private final ActivityResultLauncher<Intent> assetsActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String string = result.getData().getStringExtra(ReturnSelectedStringRecyclerViewAdapter.DATA_STRING_EXTRA);
                    if (string == null) {
                        return;
                    }
                    try {
                        SentenceProvider provider = ApkModule.createAssetsSentenceProvider(string, getApplicationContext());
                        SharedPreferences preferences = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState();
                        preferences.edit()
                                .putString(ApkModule.LAST_STATE_SENTENCE_SOURCE, ApkModule.ASSET)
                                .putString(ApkModule.LAST_STATE_SENTENCE_URI, string)
                                .apply();
                        ((SentenceProviderHolder) ((ApplicationWithDI) getApplicationContext()).appComponent.externalSentenceProvider()).setSentenceProvider(provider);
                        reloadSentences();
                    } catch (RuntimeException e) {
                        ActivityUtils.logUnhandledException(e);
                        Toast.makeText(this, R.string.file_cannot_be_accessed, Toast.LENGTH_LONG).show();
                    }
                }
            });
    private final ActivityResultLauncher<Intent> fileActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri == null) {
                        return;
                    }
                    try {
                        SentenceProvider provider = ApkModule.createInputStreamSentenceProvider(getApplicationContext(), uri);
                        SharedPreferences preferences = ((ApplicationWithDI) getApplicationContext()).appComponent.lastState();
                        preferences.edit()
                                .putString(ApkModule.LAST_STATE_SENTENCE_SOURCE, ApkModule.FILE)
                                .putString(ApkModule.LAST_STATE_SENTENCE_URI, uri.toString())
                                .apply();
                        ((SentenceProviderHolder) ((ApplicationWithDI) getApplicationContext()).appComponent.externalSentenceProvider()).setSentenceProvider(provider);
                        reloadSentences();
                    } catch (RuntimeException e) {
                        ActivityUtils.logUnhandledException(e);
                        Toast.makeText(this, R.string.file_cannot_be_accessed, Toast.LENGTH_LONG).show();
                    }
                }
            });
    private final List<Sentence.Part> result = new ArrayList<>();
    private final Random random = new Random();
    private ActivitySentenceBinding binding;
    private List<Sentence> sentences;
    private Sentence sentence;

    private int getCorrectDelayMillis() {
        return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(CONFIG_PREFIX + "correctDelayMillis", Integer.toString(CORRECT_DELAY_MILLIS)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySentenceBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);
        ActivityUtils.setFullScreen(this, root);
        binding.buttonAsset.setOnClickListener(v -> {
            Intent intent = new Intent(this, AssetsActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(AssetsActivity.FOLDER_NAME, AssetsSentenceProvider.ASSETS_SENTENCES);
            intent.putExtras(bundle);
            assetsActivityResultLauncher.launch(intent);
        });
        binding.buttonFile.setOnClickListener(v -> {
            Intent chooseFile = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
            chooseFile.setType("text/plain");
            fileActivityResultLauncher.launch(chooseFile);
        });
        reloadSentences();
        binding.buttonNext.setOnClickListener(e -> setNext());
    }

    private void reloadSentences() {
        List<Sentence> providerSentences = ((ApplicationWithDI) getApplicationContext()).appComponent.externalSentenceProvider().findSentences(new SentenceCriteria());
        sentences = providerSentences == null ? new ArrayList<>() : new ArrayList<>(providerSentences);
        setNext();
    }

    private void setNext() {
        binding.imageOk.setVisibility(View.INVISIBLE);
        if (!sentences.isEmpty()) {
            sentence = sentences.get(random.nextInt(sentences.size()));
            result.clear();
            parseParts();
            if (sentence.getParts().size() == 1) {
                sentences.remove(sentence);
                setNext();
                return;
            }

            updateUI();
        } else {
            binding.sentenceContainer.removeAllViews();
            binding.wordsContainer.removeAllViews();
            binding.textTranslation.setVisibility(View.VISIBLE);
            binding.textTranslation.setText(R.string.no_sentences);
        }
    }

    private void updateUI() {
        binding.sentenceContainer.removeAllViews();
        binding.wordsContainer.removeAllViews();
        List<Sentence.Part> parts = new ArrayList<>(sentence.getParts());
        Collections.shuffle(parts);
        for (Sentence.Part part : parts) {
            createPartButton(part, binding.wordsContainer, binding.sentenceContainer);
        }
        List<Translation> translations = sentence.getTranslations();
        if (translations != null && !translations.isEmpty()) {
            binding.textTranslation.setVisibility(View.VISIBLE);
            binding.textTranslation.setText(translations.get(0).getTranslation());
        } else {
            binding.textTranslation.setVisibility(View.GONE);
        }
    }

    private void parseParts() {
        if (sentence.getParts() == null) {
            List<Sentence.Part> parts = new ArrayList<>();
            sentence.setParts(parts);
            int partNumber = 0;
            for (String part : sentence.getSentence().split(" ")) {
                parts.add(new Sentence.Part(partNumber++, part));
            }
        }
    }

    private void createPartButton(Sentence.Part part, ViewGroup original, ViewGroup moveTo) {
        Button button = new Button(this);
        button.setText(part.getPart());
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setAllCaps(false);
        View.OnClickListener onClickListener = createOnClickListener(part, original, moveTo, button);
        button.setOnClickListener(onClickListener);
        original.addView(button);
    }

    private View.OnClickListener createOnClickListener(Sentence.Part part, ViewGroup original, ViewGroup moveTo, Button button) {
        return v -> {
            original.removeView(button);
            createPartButton(part, moveTo, original);
            if (original == binding.wordsContainer) {
                result.add(part);
                if (isCorrect()) {
                    binding.imageOk.setVisibility(View.VISIBLE);
                    if (getCorrectDelayMillis() > -1) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(this::setNext, getCorrectDelayMillis());
                    }
                }
            } else {
                result.remove(part);
            }
        };
    }

    private boolean isCorrect() {
        if (binding.wordsContainer.getChildCount() == 0) {
            StringBuilder builder = new StringBuilder(sentence.getSentence().length());
            result.forEach(p -> p.join(builder));
            return sentence.getSentence().equals(builder.toString());
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
