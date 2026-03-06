package org.leo.dictionary.apk.helper;

import org.junit.Test;
import org.leo.dictionary.entity.Translation;
import org.leo.dictionary.entity.Word;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WordUiFormatterTest {

    private static final String FSI = "\u2068";
    private static final String PDI = "\u2069";

    @Test
    public void formatWord_returnsEmptyStringForNullWord() {
        assertEquals("", WordUiFormatter.formatWord(null));
    }

    @Test
    public void formatWord_formatsWordAdditionalInfoAndTranslations() {
        Translation first = new Translation() {
            @Override
            public String getTranslation() {
                return "hello";
            }
        };
        Translation second = new Translation() {
            @Override
            public String getTranslation() {
                return "hi";
            }
        };
        List<Translation> translations = Arrays.asList(first, second);

        Word word = new Word() {
            @Override
            public String getFullWord() {
                return "hallo";
            }

            @Override
            public String getAdditionalInformation() {
                return "n.";
            }

            @Override
            public List<Translation> getTranslations() {
                return translations;
            }
        };

        assertEquals(
                FSI + "hallo" + PDI + ", " + FSI + "n." + PDI + " - " + FSI + "hello" + PDI + ", " + FSI + "hi" + PDI,
                WordUiFormatter.formatWord(word)
        );
    }

    @Test
    public void formatWord_skipsAdditionalInfoAndTranslationsWhenMissing() {
        Word word = new Word() {
            @Override
            public String getFullWord() {
                return "hallo";
            }

            @Override
            public String getAdditionalInformation() {
                return null;
            }

            @Override
            public List<Translation> getTranslations() {
                return Collections.emptyList();
            }
        };

        assertEquals(FSI + "hallo" + PDI, WordUiFormatter.formatWord(word));
    }

    @Test
    public void formatWord_keepsSeparatorWhenAdditionalInfoIsEmptyString() {
        Word word = new Word() {
            @Override
            public String getFullWord() {
                return "hallo";
            }

            @Override
            public String getAdditionalInformation() {
                return "";
            }

            @Override
            public List<Translation> getTranslations() {
                return Collections.emptyList();
            }
        };

        assertEquals(FSI + "hallo" + PDI + ", ", WordUiFormatter.formatWord(word));
    }

    @Test
    public void formatWord_skipsTranslationsWhenListIsNull() {
        Word word = new Word() {
            @Override
            public String getFullWord() {
                return "hallo";
            }

            @Override
            public String getAdditionalInformation() {
                return null;
            }

            @Override
            public List<Translation> getTranslations() {
                return null;
            }
        };

        assertEquals(FSI + "hallo" + PDI, WordUiFormatter.formatWord(word));
    }
}
