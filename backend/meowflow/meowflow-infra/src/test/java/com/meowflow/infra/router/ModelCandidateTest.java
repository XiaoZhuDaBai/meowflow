package com.meowflow.infra.router;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelCandidateTest {

    @Test
    void candidate_shouldHaveDefaultValues() {
        ModelCandidate candidate = new ModelCandidate("gpt-4", "openai", null);

        assertEquals("gpt-4", candidate.getName());
        assertEquals("openai", candidate.getProvider());
        assertTrue(candidate.isHealthy());
    }

    @Test
    void candidate_shouldTrackHealth() {
        ModelCandidate candidate = new ModelCandidate("gpt-4", "openai", null);

        assertTrue(candidate.isHealthy());

        candidate.markHealthy(false);

        assertFalse(candidate.isHealthy());
    }

    @Test
    void candidate_shouldSupportConstructor() {
        ModelCandidate candidate = new ModelCandidate("claude-3", "anthropic", null, 90, 0.8);

        assertEquals("claude-3", candidate.getName());
        assertEquals("anthropic", candidate.getProvider());
        assertEquals(90, candidate.getPriority());
        assertEquals(0.8, candidate.getWeight());
    }

    @Test
    void candidate_shouldSupportNullClient() {
        ModelCandidate candidate = new ModelCandidate("test-model", "test", null);

        assertNull(candidate.getClient());
        assertEquals("test-model", candidate.getName());
    }

    @Test
    void candidate_shouldCalculateScore() {
        ModelCandidate candidate = new ModelCandidate("gpt-4", "openai", null, 100, 1.0);
        candidate.updateLatency(100);

        double score = candidate.getScore();

        assertTrue(score > 0);
    }

    @Test
    void candidate_shouldReturnZeroScoreWhenUnhealthy() {
        ModelCandidate candidate = new ModelCandidate("gpt-4", "openai", null, 100, 1.0);
        candidate.markHealthy(false);

        double score = candidate.getScore();

        assertEquals(0, score);
    }

    @Test
    void candidate_shouldUpdateLastCheckTime() {
        ModelCandidate candidate = new ModelCandidate("gpt-4", "openai", null);
        long before = candidate.getLastCheckTime();

        candidate.markHealthy(true);

        assertTrue(candidate.getLastCheckTime() >= before);
    }
}
