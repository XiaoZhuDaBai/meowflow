package com.meowflow.workflow.trigger;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ScheduleMetadata 单元测试
 */
class ScheduleMetadataTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        ScheduleMetadata meta = ScheduleMetadata.builder()
                .workflowId(1L)
                .nodeId("node-1")
                .type("cron")
                .cronExpression("0 0 * * * *")
                .triggerTime(now)
                .status("active")
                .createTime(System.currentTimeMillis())
                .updateTime(System.currentTimeMillis())
                .lastFireTime(System.currentTimeMillis())
                .lastExecutionId(42L)
                .lockOwner("instance-1")
                .lockExpiresAt(System.currentTimeMillis() + 30_000L)
                .build();

        // Then
        assertThat(meta.getWorkflowId()).isEqualTo(1L);
        assertThat(meta.getNodeId()).isEqualTo("node-1");
        assertThat(meta.getType()).isEqualTo("cron");
        assertThat(meta.getCronExpression()).isEqualTo("0 0 * * * *");
        assertThat(meta.getTriggerTime()).isEqualTo(now);
        assertThat(meta.getStatus()).isEqualTo("active");
        assertThat(meta.getCreateTime()).isNotNull();
        assertThat(meta.getUpdateTime()).isNotNull();
        assertThat(meta.getLastFireTime()).isNotNull();
        assertThat(meta.getLastExecutionId()).isEqualTo(42L);
        assertThat(meta.getLockOwner()).isEqualTo("instance-1");
        assertThat(meta.getLockExpiresAt()).isNotNull();
    }

    @Test
    void noArgsConstructor_shouldCreateEmpty() {
        // When
        ScheduleMetadata meta = new ScheduleMetadata();

        // Then
        assertThat(meta.getWorkflowId()).isNull();
        assertThat(meta.getNodeId()).isNull();
        assertThat(meta.getType()).isNull();
        assertThat(meta.getStatus()).isNull();
    }

    @Test
    void shouldBeSerializable() throws Exception {
        // Given
        ScheduleMetadata meta = ScheduleMetadata.builder()
                .workflowId(1L)
                .nodeId("node-1")
                .type("cron")
                .cronExpression("0 0 * * * *")
                .build();

        // When - serialize and deserialize
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(meta);
        }
        try (java.io.ObjectInputStream ois =
                     new java.io.ObjectInputStream(
                             new java.io.ByteArrayInputStream(baos.toByteArray()))) {
            ScheduleMetadata deserialized = (ScheduleMetadata) ois.readObject();

            // Then
            assertThat(deserialized.getWorkflowId()).isEqualTo(1L);
            assertThat(deserialized.getNodeId()).isEqualTo("node-1");
            assertThat(deserialized.getType()).isEqualTo("cron");
            assertThat(deserialized.getCronExpression()).isEqualTo("0 0 * * * *");
        }
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        ScheduleMetadata meta = new ScheduleMetadata();

        // When
        meta.setWorkflowId(100L);
        meta.setNodeId("trigger-1");
        meta.setStatus("paused");
        meta.setLastExecutionId(500L);
        meta.setLockOwner("pod-3");

        // Then
        assertThat(meta.getWorkflowId()).isEqualTo(100L);
        assertThat(meta.getNodeId()).isEqualTo("trigger-1");
        assertThat(meta.getStatus()).isEqualTo("paused");
        assertThat(meta.getLastExecutionId()).isEqualTo(500L);
        assertThat(meta.getLockOwner()).isEqualTo("pod-3");
    }

    @Test
    void type_cron_isValid() {
        ScheduleMetadata meta = ScheduleMetadata.builder()
                .type("cron")
                .cronExpression("0 * * * * *")
                .build();

        assertThat(meta.getType()).isEqualTo("cron");
    }

    @Test
    void type_once_isValid() {
        ScheduleMetadata meta = ScheduleMetadata.builder()
                .type("once")
                .triggerTime(LocalDateTime.now().plusHours(1))
                .build();

        assertThat(meta.getType()).isEqualTo("once");
    }

    @Test
    void status_states() {
        ScheduleMetadata meta = new ScheduleMetadata();
        meta.setStatus("active");
        assertThat(meta.getStatus()).isEqualTo("active");

        meta.setStatus("paused");
        assertThat(meta.getStatus()).isEqualTo("paused");

        meta.setStatus("cancelled");
        assertThat(meta.getStatus()).isEqualTo("cancelled");
    }
}
