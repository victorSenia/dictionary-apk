package org.leo.dictionary.apk.activity;

import android.app.Activity;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.databinding.ActivityEditWordBinding;
import org.leo.dictionary.apk.word.provider.DBWordProvider;
import org.leo.dictionary.entity.Translation;

import java.util.List;

public class EditWordActivity extends AppCompatActivity {

    public static final String WORD_ID_TO_EDIT = "WORD_ID_TO_EDIT";
    public static final String TRANSLATION_INDEX_TO_EDIT = "TRANSLATION_INDEX_TO_EDIT";
    public static final long DEFAULT_VALUE_OF_WORD_ID = -1L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long id = getIntent().getExtras().getLong(WORD_ID_TO_EDIT, DEFAULT_VALUE_OF_WORD_ID);

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
            ((ApplicationWithDI) getApplicationContext()).data.put(MainActivity.UPDATED_WORD, model.getUiState().getValue());
            setResult(Activity.RESULT_OK);
            finish();
        });
        List<Translation> translations = model.getUiState().getValue().getTranslations();
        binding.buttonAddTranslation.setOnClickListener(v -> {
            translations.add(new Translation());
            addTranslationUi(translations.size() - 1);
        });
        for (int i = 0; i < translations.size(); i++) {
            addTranslationUi(i);
        }
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