package org.leo.dictionary.apk.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;
import org.leo.dictionary.apk.ApplicationWithDI;
import org.leo.dictionary.apk.R;
import org.leo.dictionary.apk.audio.AndroidAudioService;
import org.leo.dictionary.apk.databinding.ActivityWordMatcherBinding;
import org.leo.dictionary.audio.AudioService;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

import java.util.*;
import java.util.stream.Collectors;

public class WordMatcherActivity extends AppCompatActivity {

    public static final int WORDS_ARRAY = 0;
    public static final int TRANSLATIONS_ARRAY = 1;
    public static final String DEFAULT_LIMIT = "10";
    public static final int NOT_SET = -1;
    private final int[] selected = new int[2];
    private int limit;
    private ActivityWordMatcherBinding binding;

    private static String getText(Element element) {
        return element.type == WORDS_ARRAY ? ((Word) element.value).getFullWord() : ((Translation) element.value).getTranslation();
    }

    private static Translation getTranslation(Word word, Set<String> languageTo, Random random) {
        List<Translation> translations = word.getTranslations().stream().filter(t -> languageTo.isEmpty() || languageTo.contains(t.getLanguage())).collect(Collectors.toList());
        if (translations.isEmpty()) {
            return new Translation();
        }
        return translations.get(random.nextInt(translations.size()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((ApplicationWithDI) getApplicationContext()).appComponent.playService().pause();
        binding = ActivityWordMatcherBinding.inflate(getLayoutInflater());
        binding.actionNext.setOnClickListener(v -> clearAndFillWords());
        clearAndFillWords();
        setContentView(binding.getRoot());
    }

    private void clearAndFillWords() {
        removeOldViews();
        clearSelection();
        List<Word> unknownWords = ((ApplicationWithDI) getApplicationContext()).appComponent.playService().getUnknownWords();
        AudioService audioService = ((ApplicationWithDI) getApplicationContext()).appComponent.audioService();
        int limitFromConfiguration = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("org.leo.dictionary.apk.config.entity.MatchWords.limit", DEFAULT_LIMIT));
        limit = Math.min(limitFromConfiguration, unknownWords.size());
        Element[][] words = new Element[2][limit];
        List<Translation> translationsToMatch = new ArrayList<>(limit);
        List<Word> wordsToMatch = new ArrayList<>(limit);
        fillWordsToMatch(unknownWords, wordsToMatch, translationsToMatch, words);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.0f
        );
        for (Element element : words[WORDS_ARRAY]) {
            Button button = createButton(element, audioService, layoutParams);
            binding.wordContainer.addView(button);
        }
        for (Element element : words[TRANSLATIONS_ARRAY]) {
            Button button = createButton(element, audioService, layoutParams);
            binding.translationContainer.addView(button);
        }
    }

    private Button createButton(Element element, AudioService audioService, LinearLayout.LayoutParams layoutParams) {
        Button button = new Button(this);
        button.setText(getText(element));
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setBackground(AppCompatResources.getDrawable(this, R.drawable.word_background));
        button.setAllCaps(false);
        button.setOnClickListener(v -> onClickListener(element, audioService));
        button.setLayoutParams(layoutParams);
        return button;
    }

    private int differentType(int type) {
        return type == TRANSLATIONS_ARRAY ? WORDS_ARRAY : TRANSLATIONS_ARRAY;
    }

    private void onClickListener(Element element, AudioService audioService) {
        if (selected[differentType(element.type)] == NOT_SET) {
            selected[element.type] = element.current;
        } else if (element.matches == selected[differentType(element.type)]) {
            selected[element.type] = element.current;
            binding.wordContainer.getChildAt(selected[WORDS_ARRAY]).setVisibility(View.INVISIBLE);
            binding.translationContainer.getChildAt(selected[TRANSLATIONS_ARRAY]).setVisibility(View.INVISIBLE);
            clearSelection();
        } else {
            clearSelection();
        }
        playAudio(audioService, element);
    }

    private void clearSelection() {
        Arrays.fill(selected, NOT_SET);
    }

    private void playAudio(AudioService audioService, Element element) {
        try {
            if (element.type == WORDS_ARRAY) {
                Word word = (Word) element.value;
                if (audioService instanceof AndroidAudioService) {
                    ((AndroidAudioService) audioService).playAsynchronous(word.getLanguage(), word.getFullWord());
                } else {
                    audioService.play(word.getLanguage(), word.getFullWord());
                }
            } else {
//                Translation translation = (Translation) element.value;
//                audioService.play(translation.getLanguage(), translation.getTranslation());
            }
        } catch (InterruptedException e) {
            //ignore
        }
    }

    private void fillWordsToMatch(List<Word> unknownWords, List<Word> wordsToMatch, List<Translation> translationsToMatch, Element[][] words) {
        Set<String> languageTo = ((ApplicationWithDI) getApplicationContext()).appComponent.wordCriteriaProvider().getWordCriteria().getLanguageTo();
        Random random = new Random();
        for (int index = 0; index < limit; index++) {
            Word word = unknownWords.get(random.nextInt(unknownWords.size()));
            if (wordsToMatch.contains(word)) {
                index--;
            } else {
                wordsToMatch.add(word);
                Translation translation = getTranslation(word, languageTo, random);
                translationsToMatch.add(translation);
                findFreeSpaces(words, word, translation, random);
            }
        }
    }

    private void findFreeSpaces(Element[][] wordsMap, Word word, Translation translation, Random random) {
        int wordIndex = findFreeSpace(random, wordsMap[WORDS_ARRAY]);
        int translationIndex = findFreeSpace(random, wordsMap[TRANSLATIONS_ARRAY]);
        wordsMap[WORDS_ARRAY][wordIndex] = new Element(WORDS_ARRAY, wordIndex, translationIndex, word);
        wordsMap[TRANSLATIONS_ARRAY][translationIndex] = new Element(TRANSLATIONS_ARRAY, translationIndex, wordIndex, translation);
    }

    private int findFreeSpace(Random random, Element[] toSearch) {
        do {
            int index = random.nextInt(limit);
            if (toSearch[index] == null) {
                return index;
            }
        }
        while (true);
    }

    private void removeOldViews() {
        binding.wordContainer.removeAllViews();
        binding.translationContainer.removeAllViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private static class Element {
        private final int type;
        private final int current;
        private final int matches;
        private final Object value;

        public Element(int type, int current, int matches, Object value) {
            this.type = type;
            this.current = current;
            this.matches = matches;
            this.value = value;
        }
    }
}