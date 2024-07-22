package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityEditWordBinding;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

import java.util.ArrayList;
import java.util.List;

public class EditWordActivity extends AppCompatActivity {

    public static final String WORD_ID_TO_EDIT = "WORD_ID_TO_EDIT";
    public static final String TRANSLATION_INDEX_TO_EDIT = "TRANSLATION_INDEX_TO_EDIT";
    public static final long DEFAULT_VALUE_OF_WORD_ID = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getExtras() != null && getIntent().getExtras().containsKey(WORD_ID_TO_EDIT) ? getIntent().getExtras().getLong(WORD_ID_TO_EDIT, DEFAULT_VALUE_OF_WORD_ID) : DEFAULT_VALUE_OF_WORD_ID;
        ActivityEditWordBinding binding = ActivityEditWordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EditWordViewModel model = new ViewModelProvider(this).get(EditWordViewModel.class);
        if (id != DEFAULT_VALUE_OF_WORD_ID) {
            DBWordProvider wordProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.dbWordProvider();
            model.setWord(wordProvider.findWord(id));
        } else {
            model.setNewWord();
        }
        binding.buttonSave.setOnClickListener(v -> {
            Word word = model.getUiState().getValue();
            if (isValidData()) {
                ((ApplicationWithDI) getApplicationContext()).data.put(MainActivity.UPDATED_WORD, word);
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
        if (model.getUiState().getValue().getTranslations() == null) {
            model.getUiState().getValue().setTranslations(new ArrayList<>());
        }
        List<Translation> translations = model.getUiState().getValue().getTranslations();
        binding.buttonAddTranslation.setOnClickListener(v -> {
            translations.add(new Translation());
            addTranslationUi(translations.size() - 1);
        });
        for (int i = 0; i < translations.size(); i++) {
            addTranslationUi(i);
        }
    }

    private boolean isValidData() {
        boolean isValid = true;
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof EditWordFragment) {
                isValid &= ((EditWordFragment) fragment).isValid();
            } else if (fragment instanceof EditTranslationFragment) {
                isValid &= ((EditTranslationFragment) fragment).isValid();
            }
        }
        return isValid;
    }

    private void addTranslationUi(int i) {
        Bundle bundle = new Bundle();
        bundle.putInt(TRANSLATION_INDEX_TO_EDIT, i);
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .add(R.id.edit_word_translations, EditTranslationFragment.class, bundle)
                .commit();
    }
}