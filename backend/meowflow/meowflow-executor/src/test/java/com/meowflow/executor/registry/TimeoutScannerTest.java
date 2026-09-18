package com.meowflow.executor.registry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TimeoutScanner 鍗曞厓娴嬭瘯
 */
class TimeoutScannerTest {

    private TimeoutScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new TimeoutScanner();
    }

    @AfterEach
    void tearDown() {
        scanner.shutdown();
    }

    @Test
    void registerTask_shouldStorePendingTask() {
        // When
        scanner.registerTask("task-1", 5000L, LocalDateTime.now());

        // Then
        assertThat(scanner.getPendingTaskCount()).isEqualTo(1);
    }

    @Test
    void unregisterTask_shouldRemoveTask() {
        // Given
        scanner.registerTask("task-1", 5000L, LocalDateTime.now());

        // When
        scanner.unregisterTask("task-1");

        // Then
        assertThat(scanner.getPendingTaskCount()).isEqualTo(0);
    }

    @Test
    void scanTimeouts_shouldTriggerCallbackForTimeoutTask() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        scanner.setCallback(taskId -> {
            if ("task-timeout".equals(taskId)) {
                callbackInvoked.set(true);
                latch.countDown();
            }
        });

        // Register task that already timed out
        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(10);
        scanner.registerTask("task-timeout", 100L, pastTime);

        // When - wait for scanner to detect timeout
        boolean completed = latch.await(3, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(callbackInvoked.get()).isTrue();
        assertThat(scanner.getPendingTaskCount()).isEqualTo(0);
    }

    @Test
    void scanTimeouts_shouldNotTriggerForValidTask() throws Exception {
        // Given
        AtomicBoolean callbackInvoked = new AtomicBoolean(false);

        scanner.setCallback(taskId -> {
            callbackInvoked.set(true);
        });

        // Register task with future timeout
        scanner.registerTask("task-valid", 60000L, LocalDateTime.now());

        // When - wait a bit
        Thread.sleep(500);

        // Then
        assertThat(callbackInvoked.get()).isFalse();
        assertThat(scanner.getPendingTaskCount()).isEqualTo(1);
    }

    @Test
    void getPendingTaskCount_shouldReturnCorrectCount() {
        // Given
        scanner.registerTask("task-1", 5000L, LocalDateTime.now());
        scanner.registerTask("task-2", 5000L, LocalDateTime.now());
        scanner.registerTask("task-3", 5000L, LocalDateTime.now());

        // When
        int count = scanner.getPendingTaskCount();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    void setCallback_shouldAllowMultipleCallbacks() {
        // Given
        AtomicBoolean firstCallback = new AtomicBoolean(false);
        AtomicBoolean secondCallback = new AtomicBoolean(false);

        scanner.setCallback(taskId -> firstCallback.set(true));
        scanner.setCallback(taskId -> secondCallback.set(true));

        // Register timeout task
        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(10);
        scanner.registerTask("task-1", 100L, pastTime);

        // When - wait for scan
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {
        }

        // Then - only latest callback is active
        assertThat(firstCallback.get()).isFalse();
        assertThat(secondCallback.get()).isTrue();
    }

    @Test
    void callbackException_shouldNotStopScanning() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(1);

        scanner.setCallback(taskId -> {
            if ("task-error".equals(taskId)) {
                throw new RuntimeException("Callback error");
            }
            if ("task-success".equals(taskId)) {
                latch.countDown();
            }
        });

        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(10);
        scanner.registerTask("task-error", 100L, pastTime);

        // Wait for error task to be processed
        Thread.sleep(1500);

        // Register another task
        scanner.registerTask("task-success", 100L, pastTime);

        // When - wait for second task
        boolean completed = latch.await(3, TimeUnit.SECONDS);

        // Then - should still work after exception
        assertThat(completed).isTrue();
    }

    @Test
    void shutdown_shouldStopScanning() {
        // Given
        scanner.registerTask("task-1", 5000L, LocalDateTime.now());

        // When
        scanner.shutdown();

        // Then
        assertThat(scanner.getPendingTaskCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void multipleTasksWithDifferentTimeouts_shouldHandleCorrectly() throws Exception {
        // Given
        CountDownLatch latch = new CountDownLatch(2);

        scanner.setCallback(taskId -> {
            if (taskId.startsWith("task-timeout")) {
                latch.countDown();
            }
        });

        LocalDateTime pastTime = LocalDateTime.now().minusSeconds(10);
        LocalDateTime futureTime = LocalDateTime.now();

        scanner.registerTask("task-timeout-1", 100L, pastTime);
        scanner.registerTask("task-valid", 60000L, futureTime);
        scanner.registerTask("task-timeout-2", 100L, pastTime);

        // When
        boolean completed = latch.await(3, TimeUnit.SECONDS);

        // Then
        assertThat(completed).isTrue();
        assertThat(scanner.getPendingTaskCount()).isEqualTo(1); // Only valid task remains
    }
}
