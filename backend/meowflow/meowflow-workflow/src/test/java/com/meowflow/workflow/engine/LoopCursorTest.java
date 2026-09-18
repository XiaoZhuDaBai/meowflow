package com.meowflow.workflow.engine;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LoopCursor 单元测试
 */
class LoopCursorTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("counter", 5);

        // When
        LoopCursor cursor = LoopCursor.builder()
                .iteration(3)
                .snapshot(snapshot)
                .build();

        // Then
        assertThat(cursor.getIteration()).isEqualTo(3);
        assertThat(cursor.getSnapshot()).containsEntry("counter", 5);
    }

    @Test
    void noArgsConstructor_shouldCreateWithDefaultIteration() {
        // When
        LoopCursor cursor = new LoopCursor();

        // Then
        assertThat(cursor.getIteration()).isEqualTo(0);
        assertThat(cursor.getSnapshot()).isNull();
    }

    @Test
    void incrementIteration_shouldIncrementBy1() {
        // Given
        LoopCursor cursor = LoopCursor.builder().iteration(0).build();

        // When
        cursor.incrementIteration();

        // Then
        assertThat(cursor.getIteration()).isEqualTo(1);

        // When - increment again
        cursor.incrementIteration();

        // Then
        assertThat(cursor.getIteration()).isEqualTo(2);
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        LoopCursor cursor = new LoopCursor();

        // When
        cursor.setIteration(10);
        Map<String, Object> snap = Map.of("key", "value");
        cursor.setSnapshot(snap);

        // Then
        assertThat(cursor.getIteration()).isEqualTo(10);
        assertThat(cursor.getSnapshot()).isSameAs(snap);
    }

    @Test
    void shouldBeSerializable() throws Exception {
        // Given
        LoopCursor cursor = LoopCursor.builder()
                .iteration(2)
                .snapshot(Map.of("x", 1, "y", "two"))
                .build();

        // When - serialize and deserialize
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos)) {
            oos.writeObject(cursor);
        }
        try (java.io.ObjectInputStream ois =
                     new java.io.ObjectInputStream(
                             new java.io.ByteArrayInputStream(baos.toByteArray()))) {
            LoopCursor deserialized = (LoopCursor) ois.readObject();

            // Then
            assertThat(deserialized.getIteration()).isEqualTo(2);
            assertThat(deserialized.getSnapshot()).containsEntry("x", 1);
        }
    }

    @Test
    void equalsAndHashCode_byAllArgsConstructor() {
        // Given
        LoopCursor c1 = LoopCursor.builder().iteration(1).build();
        LoopCursor c2 = LoopCursor.builder().iteration(1).build();

        // Then
        assertThat(c1).isEqualTo(c2);
        assertThat(c1.hashCode()).isEqualTo(c2.hashCode());
    }
}
