package com.meowflow.infra.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.DisplayName;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIProvider Enum Tests")
class AIProviderTest {

    @Test
    @DisplayName("testGetByCode_OpenAI: Should find OPENAI by code")
    void testGetByCode_OpenAI() {
        AIProvider result = AIProvider.getByCode("openai");
        assertEquals(AIProvider.OPENAI, result);
    }

    @Test
    @DisplayName("testGetByCode_Anthropic: Should find ANTHROPIC by code")
    void testGetByCode_Anthropic() {
        AIProvider result = AIProvider.getByCode("anthropic");
        assertEquals(AIProvider.ANTHROPIC, result);
    }

    @Test
    @DisplayName("testGetByModel_GPT4o: Should find provider by model name")
    void testGetByModel_GPT4o() {
        AIProvider result = AIProvider.getByModel("gpt-4o");
        assertEquals(AIProvider.OPENAI, result);
    }

    @Test
    @DisplayName("testGetByModel_Claude: Should find provider for claude models")
    void testGetByModel_Claude() {
        AIProvider result = AIProvider.getByModel("claude-3-5-sonnet");
        assertEquals(AIProvider.ANTHROPIC, result);
    }

    @Test
    @DisplayName("testSupports_True: Should return true for supported model")
    void testSupports_True() {
        boolean result = AIProvider.OPENAI.supports("gpt-4o-mini");
        assertTrue(result);
    }

    @Test
    @DisplayName("testSupports_False: Should return false for unsupported model")
    void testSupports_False() {
        boolean result = AIProvider.OPENAI.supports("claude-3-5-sonnet");
        assertFalse(result);
    }

    @Test
    @DisplayName("testUnknown_Code: Should return UNKNOWN for invalid code")
    void testUnknown_Code() {
        AIProvider result = AIProvider.getByCode("invalid_provider");
        assertEquals(AIProvider.UNKNOWN, result);
    }

    @Test
    @DisplayName("testUnknown_Model: Should return UNKNOWN for invalid model")
    void testUnknown_Model() {
        AIProvider result = AIProvider.getByModel("unknown-model");
        assertEquals(AIProvider.UNKNOWN, result);
    }
}
