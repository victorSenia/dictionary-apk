package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.databinding.ActivitySentenceBinding;
import org.leo.dictionary.entity.Sentence;
import org.leo.dictionary.entity.SentenceCriteria;
import org.leo.dictionary.entity.Translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class SentenceActivity extends AppCompatActivity {
    public static final int CORRECT_DELAY_MILLIS = 1500;
    private final List<Sentence.Part> result = new ArrayList<>();
    private final Random random = new Random();
    private ActivitySentenceBinding binding;
    private List<Sentence> sentences;
    private Sentence sentence;

    private int getCorrectDelayMillis() {
        return CORRECT_DELAY_MILLIS;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySentenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (sentences == null) {
            sentences = ((ApplicationWithDI) getApplicationContext()).appComponent.externalSentenceProvider().findSentences(new SentenceCriteria());
        }
        setNext();
        binding.buttonNext.setOnClickListener(e -> setNext());
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
            int partNUmber = 0;
            for (String part : sentence.getSentence().split(" ")) {
                parts.add(new Sentence.Part(partNUmber++, part));
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
                    this.binding.imageOk.setVisibility(View.VISIBLE);
                    if (getCorrectDelayMillis() > -1) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(this::setNext, getCorrectDelayMillis());
                    }
                }
            } else result.remove(part);
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