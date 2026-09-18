package com.meowflow.workflow.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageResponse 单元测试
 */
class PageResponseTest {

    @Test
    void builder_shouldSetAllFields() {
        // Given
        List<String> records = List.of("a", "b", "c");

        // When
        PageResponse<String> page = PageResponse.<String>builder()
                .total(100L)
                .current(1)
                .size(10)
                .pages(10)
                .records(records)
                .build();

        // Then
        assertThat(page.getTotal()).isEqualTo(100L);
        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getPages()).isEqualTo(10);
        assertThat(page.getRecords()).containsExactly("a", "b", "c");
    }

    @Test
    void noArgsConstructor_shouldCreateEmpty() {
        // When
        PageResponse<String> page = new PageResponse<>();

        // Then
        assertThat(page.getTotal()).isNull();
        assertThat(page.getRecords()).isNull();
    }

    @Test
    void of_shouldCalculatePagesAutomatically() {
        // Given
        List<String> records = List.of("a", "b", "c");

        // When - 100 records, 10 per page = 10 pages
        PageResponse<String> page = PageResponse.of(records, 100L, 1, 10);

        // Then
        assertThat(page.getTotal()).isEqualTo(100L);
        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getPages()).isEqualTo(10);
        assertThat(page.getRecords()).isEqualTo(records);
    }

    @Test
    void of_withNonPerfectDivision_calculatesCorrectPages() {
        // Given
        List<String> records = List.of("a");

        // When - 95 records, 10 per page = 10 pages (ceil)
        PageResponse<String> page = PageResponse.of(records, 95L, 1, 10);

        // Then
        assertThat(page.getPages()).isEqualTo(10); // ceil(95/10) = 10
    }

    @Test
    void of_withExactDivision_calculatesCorrectPages() {
        // Given
        List<String> records = List.of();

        // When - 100 records, 10 per page = 10 pages exactly
        PageResponse<String> page = PageResponse.of(records, 100L, 1, 10);

        // Then
        assertThat(page.getPages()).isEqualTo(10);
    }

    @Test
    void of_withEmptyRecords_stillCalculatesPages() {
        // Given
        List<String> records = List.of();

        // When - 0 records
        PageResponse<String> page = PageResponse.of(records, 0L, 1, 10);

        // Then - ceil(0/10) = 0
        assertThat(page.getPages()).isEqualTo(0);
        assertThat(page.getRecords()).isEmpty();
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        PageResponse<String> page = new PageResponse<>();

        // When
        page.setTotal(50L);
        page.setCurrent(2);
        page.setSize(20);
        List<String> records = List.of("x");
        page.setRecords(records);

        // Then
        assertThat(page.getTotal()).isEqualTo(50L);
        assertThat(page.getCurrent()).isEqualTo(2);
        assertThat(page.getSize()).isEqualTo(20);
        assertThat(page.getRecords()).isSameAs(records);
    }

    @Test
    void of_preservesGenericType() {
        // Given - test with Integer type
        List<Integer> records = List.of(1, 2, 3, 4, 5);

        // When
        PageResponse<Integer> page = PageResponse.of(records, 50L, 1, 5);

        // Then
        assertThat(page.getRecords()).hasSize(5);
        assertThat(page.getRecords().get(0)).isEqualTo(1);
    }
}
