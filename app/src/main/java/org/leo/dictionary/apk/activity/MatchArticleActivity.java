package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import org.leo.dictionary.apk.ApkModule;
import org.leo.dictionary.apk.activity.viewmodel.EditWordViewModel;
import org.leo.dictionary.apk.databinding.ActivityMatchArticleBinding;
import org.leo.dictionary.entity.Word;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

public class MatchArticleActivity extends AppCompatActivity {
    public static final int CORRECT_DELAY_MILLIS = 1500;
    private final Random random = new Random();
    private ActivityMatchArticleBinding binding;
    private List<Word> words;
    private List<String> articles;

    private int getCorrectDelayMillis() {
        return CORRECT_DELAY_MILLIS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMatchArticleBinding.inflate(getLayoutInflater());
        View root = binding.getRoot();
        setContentView(root);
        ActivityUtils.setFullScreen(this, root);
        EditWordViewModel model = new ViewModelProvider(this).get(EditWordViewModel.class);
        if (words == null || articles == null) {
            List<Word> unknownWords = ApkModule.getWords(this);
            unknownWords = unknownWords.stream().filter(w -> w.getArticle() != null && !w.getArticle().isEmpty()).collect(Collectors.toList());
            Collections.shuffle(unknownWords);
            words = unknownWords;
            articles = unknownWords.stream().map(Word::getArticle).distinct().sorted().collect(Collectors.toList());
            setNext(model);
        } else {
            if (model.getValue() == null) {
                setNext(model);
            }
        }
        model.getData().observe(this, w -> binding.textWord.setText(w.getWord()));
        binding.buttonNext.setOnClickListener(e -> setNext(model));

        binding.articlesContainer.removeAllViews();
        for (String variant : articles) {
            Button button = createButton(variant);
            binding.articlesContainer.addView(button);
        }
    }

    private Button createButton(String variant) {
        Button button = new Button(this);
        button.setText(variant);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setAllCaps(false);
        button.setOnClickListener(v -> checkArticle(variant));
        return button;
    }

    protected void checkArticle(String article) {
        EditWordViewModel model = new ViewModelProvider(this).get(EditWordViewModel.class);
        if (Objects.equals(model.getValue().getArticle(), article)) {
            binding.textWord.setText(model.getValue().getFullWord());
            this.binding.imageOk.setVisibility(View.VISIBLE);
            if (getCorrectDelayMillis() > -1) {
                final Handler handler = new Handler(Looper.getMainLooper());
                handler.postDelayed(() -> setNext(model), getCorrectDelayMillis());
            }
        }
    }

    private void setNext(EditWordViewModel model) {
        binding.imageOk.setVisibility(View.INVISIBLE);
        model.setValue(words.get(random.nextInt(words.size())));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}