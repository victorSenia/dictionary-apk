package org.leo.dictionary.apk.helper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KnowledgeToRatingConverterTest {

    @Test
    public void knowledgeToRating_usesFloorForPrecision() {
        assertEquals(0.0f, KnowledgeToRatingConverter.knowledgeToRating(0.19d), 0.0001f);
        assertEquals(1.0f, KnowledgeToRatingConverter.knowledgeToRating(0.21d), 0.0001f);
        assertEquals(4.0f, KnowledgeToRatingConverter.knowledgeToRating(0.99d), 0.0001f);
    }

    @Test
    public void knowledgeToRating_clampsOutOfRangeValues() {
        assertEquals(0.0f, KnowledgeToRatingConverter.knowledgeToRating(-0.5d), 0.0001f);
        assertEquals(0.0f, KnowledgeToRatingConverter.knowledgeToRating(Double.NaN), 0.0001f);
        assertEquals(5.0f, KnowledgeToRatingConverter.knowledgeToRating(1.0d), 0.0001f);
        assertEquals(5.0f, KnowledgeToRatingConverter.knowledgeToRating(1.5d), 0.0001f);
    }

    @Test
    public void ratingToKnowledge_returnsZeroForNegativeValues() {
        assertEquals(0.0d, KnowledgeToRatingConverter.ratingToKnowledge(-1.0f), 0.0001d);
        assertEquals(0.0d, KnowledgeToRatingConverter.ratingToKnowledge(Float.NaN), 0.0001d);
    }

    @Test
    public void ratingToKnowledge_dividesByStarsCountWithinRange() {
        assertEquals(0.5d, KnowledgeToRatingConverter.ratingToKnowledge(2.5f), 0.0001d);
        assertEquals(1.0d, KnowledgeToRatingConverter.ratingToKnowledge(5.0f), 0.0001d);
    }

    @Test
    public void ratingToKnowledge_clampsValuesAboveStarsCountToOne() {
        assertEquals(1.0d, KnowledgeToRatingConverter.ratingToKnowledge(6.0f), 0.0001d);
    }
}
