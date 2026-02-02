package org.leo.dictionary.apk.helper;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

import java.util.List;
import java.util.stream.Collectors;

public final class WordUiFormatter {
    // Unicode bidirectional isolation marks: keep each dynamic chunk directionally isolated.
    private static final char FSI = '\u2068';
    private static final char PDI = '\u2069';

    private WordUiFormatter() {
    }

    public static String formatWord(Word word) {
        if (word == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(isolate(word.getFullWord()));
        if (word.getAdditionalInformation() != null) {
            builder.append(", ").append(isolate(word.getAdditionalInformation()));
        }
        builder.append(" - ").append(formatTranslations(word.getTranslations(), ", "));
        return builder.toString();
    }

    private static String formatTranslations(List<Translation> translations, String delimiter) {
        if (translations == null) {
            return "";
        }
        return translations.stream()
                .map(Translation::getTranslation)
                .map(WordUiFormatter::isolate)
                .collect(Collectors.joining(delimiter));
    }

    private static String isolate(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return FSI + value + PDI;
    }
}
