package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.databinding.ActivityGrammarCheckBinding;
import org.leo.dictionary.entity.Sentence;
import org.leo.dictionary.grammar.provider.GrammarProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GrammarCheckActivity extends AppCompatActivity {
    public static final int CORRECT_DELAY_MILLIS = 1500;
    public static final int VARIANTS_LIMIT = 5;
    public static final boolean SHOW_VARIANTS = true;
    private List<Sentence> sentences;
    private Sentence sentence;
    private ActivityGrammarCheckBinding binding;

    private static String createPlaceholder(int length) {
        return Stream.generate(() -> " ").limit(length).collect(Collectors.joining());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sentences == null) {
            GrammarProvider grammarProvider = ((ApplicationWithDI) getApplicationContext()).appComponent.externalGrammarProvider();
            sentences = grammarProvider.findSentences(((ApplicationWithDI) getApplicationContext()).appComponent.grammarCriteriaProvider().getObject());
        }
        binding = ActivityGrammarCheckBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.next.setOnClickListener(e -> updateUIWithNextSentence());
        binding.sentenceAnswer.addTextChangedListener(new EditWordActivity.AbstractTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.sentence.setText(sentenceToString(sentence));
                if (sentence.getAnswer().equals(String.valueOf(s))) {
                    binding.imageOk.setVisibility(View.VISIBLE);
                    if (getCorrectDelayMillis() > -1) {
                        final Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(() -> updateUIWithNextSentence(), getCorrectDelayMillis());
                    }
                }
            }
        });
        updateUIWithNextSentence();
    }

    private static int getCorrectDelayMillis() {
        return CORRECT_DELAY_MILLIS;
    }

    private void updateUIWithNextSentence() {
        updateUI(nextSentence());
    }

    private void updateUI(Sentence sentence) {
        binding.imageOk.setVisibility(View.INVISIBLE);
        binding.sentenceHint.setText(sentence.getHint().getHint());
        binding.sentenceAnswer.setText("");
        binding.sentencePrefix.setVisibility(sentence.getSentencePrefix().isEmpty() ? View.GONE : View.VISIBLE);
        binding.sentencePrefix.setText(sentence.getSentencePrefix());
        binding.sentenceAnswer.setText("");
        binding.sentenceSuffix.setVisibility(sentence.getSentenceSuffix().isEmpty() ? View.GONE : View.VISIBLE);
        binding.sentenceSuffix.setText(sentence.getSentenceSuffix());
        if (SHOW_VARIANTS) {
            List<String> variants = fillWrongVariants(sentence.getAnswer(), sentence.getHint().getVariants());
            binding.variantsContainer.removeAllViews();
            for (String variant : variants) {
                Button button = createButton(variant);
                binding.variantsContainer.addView(button);
            }
            binding.sentence.setVisibility(View.VISIBLE);
            binding.sentence.setText(sentenceToString(sentence));
            binding.sentenceAnswer.setVisibility(View.GONE);
            binding.sentencePrefix.setVisibility(View.GONE);
            binding.sentenceSuffix.setVisibility(View.GONE);
        }
    }

    public String sentenceToString(Sentence s) {
        StringBuilder builder = new StringBuilder();
        if (!s.getSentencePrefix().isEmpty()) {
            builder.append(s.getSentencePrefix());
            builder.append(' ');
        }
        builder.append(binding.sentenceAnswer.getText().toString().isEmpty() ? createPlaceholder(s.getAnswer().length()) : binding.sentenceAnswer.getText().toString());
        if (!s.getSentenceSuffix().isEmpty()) {
            builder.append(' ');
            builder.append(s.getSentenceSuffix());
        }
        return builder.toString();
    }

    private List<String> fillWrongVariants(String answer, List<String> allVariants) {
        List<String> shuffled = new ArrayList<>(allVariants);
        Collections.shuffle(shuffled);
        List<String> variants = new ArrayList<>();
        variants.add(answer);
        for (String variant : shuffled) {
            if (variants.size() == VARIANTS_LIMIT) {
                break;
            }
            if (!variant.equals(answer)) {
                variants.add(variant);
            }
        }
        Collections.shuffle(variants);
        return variants;
    }

    private Button createButton(String variant) {
        Button button = new Button(this);
        button.setText(variant);
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setAllCaps(false);
        button.setOnClickListener(v -> binding.sentenceAnswer.setText(variant));
        return button;
    }

    private Sentence nextSentence() {
        sentence = sentences.get(new Random().nextInt(sentences.size()));
        return sentence;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}