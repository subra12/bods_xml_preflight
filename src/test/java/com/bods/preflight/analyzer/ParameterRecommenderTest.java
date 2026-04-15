package com.bods.preflight.analyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParameterRecommenderTest {
    @Test
    void shouldCalculateBufferedMaxSize() {
        ParameterRecommender recommender = new ParameterRecommender();
        assertEquals(5000, recommender.calculateRecommendedMaxSize(3398));
        assertEquals(5000, recommender.calculateRecommendedMaxSize(4000));
        assertEquals(12000, recommender.calculateRecommendedMaxSize(9801));
    }
}
