package org.leo.dictionary.apk.helper;

public class KnowledgeToRatingConverter {
    public static final int starsCount = 5;

    public static float knowledgeToRating(double value) {
        return Double.valueOf(Math.floor(value * starsCount)).floatValue();
    }

    public static double ratingToKnowledge(float value) {
        if (value < 0) {
            return 0;
        }
        if (value > starsCount) {
            return starsCount;
        }
        return value / starsCount;
    }
}