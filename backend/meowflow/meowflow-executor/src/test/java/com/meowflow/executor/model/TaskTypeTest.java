package com.meowflow.executor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskTypeTest {

    @Test
    void enumValues_shouldExist() {
        assertNotNull(TaskType.WORKFLOW_NODE);
        assertNotNull(TaskType.CONDITION);
        assertNotNull(TaskType.SCRIPT);
        assertNotNull(TaskType.HTTP_REQUEST);
        assertNotNull(TaskType.AI_CHAT);
        assertNotNull(TaskType.KNOWLEDGE_SEARCH);
        assertNotNull(TaskType.NOTIFICATION);
        assertNotNull(TaskType.SCHEDULED);
        assertNotNull(TaskType.BATCH);
        assertNotNull(TaskType.CUSTOM);
    }

    @Test
    void enumNames_shouldMatchExpectedValues() {
        assertEquals("WORKFLOW_NODE", TaskType.WORKFLOW_NODE.name());
        assertEquals("CONDITION", TaskType.CONDITION.name());
        assertEquals("SCRIPT", TaskType.SCRIPT.name());
        assertEquals("HTTP_REQUEST", TaskType.HTTP_REQUEST.name());
        assertEquals("AI_CHAT", TaskType.AI_CHAT.name());
        assertEquals("KNOWLEDGE_SEARCH", TaskType.KNOWLEDGE_SEARCH.name());
        assertEquals("NOTIFICATION", TaskType.NOTIFICATION.name());
        assertEquals("SCHEDULED", TaskType.SCHEDULED.name());
        assertEquals("BATCH", TaskType.BATCH.name());
        assertEquals("CUSTOM", TaskType.CUSTOM.name());
    }

    @Test
    void enumValueOf_shouldReturnCorrectEnum() {
        assertEquals(TaskType.WORKFLOW_NODE, TaskType.valueOf("WORKFLOW_NODE"));
        assertEquals(TaskType.AI_CHAT, TaskType.valueOf("AI_CHAT"));
    }

    @Test
    void enumValues_shouldHaveExpectedCount() {
        assertEquals(10, TaskType.values().length);
    }
}
