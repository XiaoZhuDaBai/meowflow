package com.meowflow.workflow.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ExecutionSnapshot 单元测试
 */
class ExecutionSnapshotTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder()
                .id(1L)
                .executionId(100L)
                .workflowId(200L)
                .version("v1.0")
                .status("running")
                .completedNodes("[{\"nodeId\":\"n1\",\"status\":\"SUCCESS\"}]")
                .variables("{\"var1\":\"value1\"}")
                .loopCursors("{\"loop1\":1}")
                .waitingReason("waiting_for_join")
                .pendingEdgeId("edge-1")
                .lastEventId("event-123")
                .snapshotVersion(5)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertThat(snapshot.getId()).isEqualTo(1L);
        assertThat(snapshot.getExecutionId()).isEqualTo(100L);
        assertThat(snapshot.getWorkflowId()).isEqualTo(200L);
        assertThat(snapshot.getVersion()).isEqualTo("v1.0");
        assertThat(snapshot.getStatus()).isEqualTo("running");
        assertThat(snapshot.getCompletedNodes()).contains("SUCCESS");
        assertThat(snapshot.getVariables()).contains("var1");
        assertThat(snapshot.getLoopCursors()).contains("loop1");
        assertThat(snapshot.getWaitingReason()).isEqualTo("waiting_for_join");
        assertThat(snapshot.getPendingEdgeId()).isEqualTo("edge-1");
        assertThat(snapshot.getLastEventId()).isEqualTo("event-123");
        assertThat(snapshot.getSnapshotVersion()).isEqualTo(5);
    }

    @Test
    void noArgsConstructor_shouldCreateEmpty() {
        // When
        ExecutionSnapshot snapshot = new ExecutionSnapshot();

        // Then
        assertThat(snapshot.getId()).isNull();
        assertThat(snapshot.getExecutionId()).isNull();
        assertThat(snapshot.getStatus()).isNull();
        assertThat(snapshot.getSnapshotVersion()).isNull();
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        ExecutionSnapshot snapshot = new ExecutionSnapshot();

        // When
        snapshot.setId(42L);
        snapshot.setStatus("paused");
        snapshot.setWaitingReason("loop_iterating");
        snapshot.setLastEventId("evt-1");
        snapshot.setSnapshotVersion(10);

        // Then
        assertThat(snapshot.getId()).isEqualTo(42L);
        assertThat(snapshot.getStatus()).isEqualTo("paused");
        assertThat(snapshot.getWaitingReason()).isEqualTo("loop_iterating");
        assertThat(snapshot.getLastEventId()).isEqualTo("evt-1");
        assertThat(snapshot.getSnapshotVersion()).isEqualTo(10);
    }

    @Test
    void snapshotVersion_incrementsCorrectly() {
        // Given
        ExecutionSnapshot snapshot = ExecutionSnapshot.builder().snapshotVersion(0).build();

        // When - simulating updates
        snapshot.setSnapshotVersion(1);
        snapshot.setSnapshotVersion(2);
        snapshot.setSnapshotVersion(3);

        // Then
        assertThat(snapshot.getSnapshotVersion()).isEqualTo(3);
    }

    @Test
    void jsonFields_defaultToNull() {
        // When
        ExecutionSnapshot snapshot = new ExecutionSnapshot();

        // Then
        assertThat(snapshot.getCompletedNodes()).isNull();
        assertThat(snapshot.getVariables()).isNull();
        assertThat(snapshot.getLoopCursors()).isNull();
    }

    @Test
    void status_states() {
        // Given
        ExecutionSnapshot snapshot = new ExecutionSnapshot();

        // When/Then - all status values
        snapshot.setStatus("pending");
        assertThat(snapshot.getStatus()).isEqualTo("pending");

        snapshot.setStatus("running");
        assertThat(snapshot.getStatus()).isEqualTo("running");

        snapshot.setStatus("paused");
        assertThat(snapshot.getStatus()).isEqualTo("paused");

        snapshot.setStatus("success");
        assertThat(snapshot.getStatus()).isEqualTo("success");

        snapshot.setStatus("failed");
        assertThat(snapshot.getStatus()).isEqualTo("failed");
    }
}
