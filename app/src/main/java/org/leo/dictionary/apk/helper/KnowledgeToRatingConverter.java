package org.leo.dictionary.apk.helper;

public class KnowledgeToRatingConverter {
    public static final int starsCount = 5;

    public static float knowledgeToRating(double value) {
        if (Double.isNaN(value) || value <= 0d) {
            return 0f;
        }
        if (value >= 1d) {
            return starsCount;
        }
        return Double.valueOf(Math.floor(value * starsCount)).floatValue();
    }

    public static double ratingToKnowledge(float value) {
        if (Float.isNaN(value) || value < 0) {
            return 0;
        }
        if (value > starsCount) {
            return 1.;
        }
        return value / starsCount;
    }
}
