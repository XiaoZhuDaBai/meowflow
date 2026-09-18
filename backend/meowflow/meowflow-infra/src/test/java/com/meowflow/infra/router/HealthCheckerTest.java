package com.meowflow.infra.router;

import com.meowflow.infra.client.ChatClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthCheckerTest {

    @AfterEach
    void tearDown() {
        // HealthChecker starts a scheduled executor in constructor
        // We can't easily test it without a full Spring context
    }

    @Test
    void candidate_shouldHaveNameAndProvider() {
        ChatClient mockClient = mock(ChatClient.class);
        ModelCandidate candidate = new ModelCandidate("gpt-4", "openai", mockClient);

        assertEquals("gpt-4", candidate.getName());
        assertEquals("openai", candidate.getProvider());
        assertEquals(mockClient, candidate.getClient());
    }

    @Test
    void candidate_shouldHaveDefaultHealthyStatus() {
        ModelCandidate candidate = new ModelCandidate("test", "test", null);

        assertTrue(candidate.isHealthy());
    }

    @Test
    void candidate_shouldSupportCapabilities() {
        ModelCandidate candidate = new ModelCandidate("test", "test", null);

        assertTrue(candidate.supports(java.util.List.of()));
    }
}
