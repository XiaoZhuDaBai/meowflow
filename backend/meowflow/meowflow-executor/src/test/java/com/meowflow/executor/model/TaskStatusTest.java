package com.meowflow.executor.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskStatusTest {

    @Test
    void enumValues_shouldExist() {
        assertNotNull(TaskStatus.PENDING);
        assertNotNull(TaskStatus.RUNNING);
        assertNotNull(TaskStatus.SUCCESS);
        assertNotNull(TaskStatus.FAILED);
        assertNotNull(TaskStatus.CANCELLED);
        assertNotNull(TaskStatus.TIMEOUT);
        assertNotNull(TaskStatus.RETRYING);
    }

    @Test
    void enumNames_shouldMatchExpectedValues() {
        assertEquals("PENDING", TaskStatus.PENDING.name());
        assertEquals("RUNNING", TaskStatus.RUNNING.name());
        assertEquals("SUCCESS", TaskStatus.SUCCESS.name());
        assertEquals("FAILED", TaskStatus.FAILED.name());
        assertEquals("CANCELLED", TaskStatus.CANCELLED.name());
        assertEquals("TIMEOUT", TaskStatus.TIMEOUT.name());
        assertEquals("RETRYING", TaskStatus.RETRYING.name());
    }
}
