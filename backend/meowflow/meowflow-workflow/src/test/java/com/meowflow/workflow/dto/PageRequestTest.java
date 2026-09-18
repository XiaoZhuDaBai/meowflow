package com.meowflow.workflow.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PageRequest 单元测试
 */
class PageRequestTest {

    @Test
    void defaultValues_shouldBeApplied() {
        // When
        PageRequest request = new PageRequest();

        // Then
        assertThat(request.getCurrent()).isEqualTo(1);
        assertThat(request.getSize()).isEqualTo(10);
        assertThat(request.getSort()).isNull();
        assertThat(request.getOrder()).isNull();
    }

    @Test
    void settersAndGetters_shouldWork() {
        // Given
        PageRequest request = new PageRequest();

        // When
        request.setCurrent(3);
        request.setSize(20);
        request.setSort("name");
        request.setOrder("asc");

        // Then
        assertThat(request.getCurrent()).isEqualTo(3);
        assertThat(request.getSize()).isEqualTo(20);
        assertThat(request.getSort()).isEqualTo("name");
        assertThat(request.getOrder()).isEqualTo("asc");
    }

    @Test
    void getOffset_firstPage_returnsZero() {
        // Given
        PageRequest request = new PageRequest();
        request.setCurrent(1);
        request.setSize(10);

        // When
        int offset = request.getOffset();

        // Then
        assertThat(offset).isEqualTo(0);
    }

    @Test
    void getOffset_middlePage_returnsCorrectOffset() {
        // Given - page 3, size 10 = offset 20
        PageRequest request = new PageRequest();
        request.setCurrent(3);
        request.setSize(10);

        // When
        int offset = request.getOffset();

        // Then
        assertThat(offset).isEqualTo(20);
    }

    @Test
    void getOffset_customSize_returnsCorrectOffset() {
        // Given - page 2, size 25 = offset 25
        PageRequest request = new PageRequest();
        request.setCurrent(2);
        request.setSize(25);

        // When
        int offset = request.getOffset();

        // Then
        assertThat(offset).isEqualTo(25);
    }

    @Test
    void getOffset_largePageNumber_returnsCorrectOffset() {
        // Given - page 100, size 50 = offset 4950
        PageRequest request = new PageRequest();
        request.setCurrent(100);
        request.setSize(50);

        // When
        int offset = request.getOffset();

        // Then
        assertThat(offset).isEqualTo(4950);
    }

    @Test
    void getOffset_pageSize_one_returnsCurrentMinusOne() {
        // Given
        PageRequest request = new PageRequest();
        request.setCurrent(5);
        request.setSize(1);

        // When
        int offset = request.getOffset();

        // Then
        assertThat(offset).isEqualTo(4);
    }

    @Test
    void defaultValues_canBeOverridden() {
        // Given
        PageRequest request = new PageRequest();
        // Default is current=1, size=10

        // When
        request.setCurrent(0);
        request.setSize(0);

        // Then - getOffset should produce -10 (current-1)*size
        assertThat(request.getOffset()).isEqualTo(0); // (0)*0 = 0
    }
}
