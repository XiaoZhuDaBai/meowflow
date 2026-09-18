package com.meowflow.workflow.definition;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeTypeTest {

    @Test
    void fromCode_resolvesFrontendAliases() {
        assertThat(EdgeType.fromCode("condition")).isEqualTo(EdgeType.CONDITION);
        assertThat(EdgeType.fromCode("condition-true")).isEqualTo(EdgeType.CONDITION_TRUE);
        assertThat(EdgeType.fromCode("condition-false")).isEqualTo(EdgeType.CONDITION_FALSE);
        assertThat(EdgeType.fromCode("loop")).isEqualTo(EdgeType.LOOP);
        assertThat(EdgeType.fromCode("error")).isEqualTo(EdgeType.ERROR);
        assertThat(EdgeType.fromCode("default")).isEqualTo(EdgeType.DEFAULT);
    }

    @Test
    void fromCode_unknownFallsBackToDefault() {
        assertThat(EdgeType.fromCode("unknown-edge")).isEqualTo(EdgeType.DEFAULT);
        assertThat(EdgeType.fromJson("condition")).isEqualTo(EdgeType.CONDITION);
    }

    @Test
    void fromCode_withNull_returnsDefault() {
        assertThat(EdgeType.fromCode(null)).isEqualTo(EdgeType.DEFAULT);
    }

    @Test
    void fromCode_withBlankString_returnsDefault() {
        assertThat(EdgeType.fromCode("")).isEqualTo(EdgeType.DEFAULT);
        assertThat(EdgeType.fromCode("   ")).isEqualTo(EdgeType.DEFAULT);
    }

    @Test
    void fromCode_isCaseInsensitive() {
        assertThat(EdgeType.fromCode("CONDITION")).isEqualTo(EdgeType.CONDITION);
        assertThat(EdgeType.fromCode("Loop")).isEqualTo(EdgeType.LOOP);
        assertThat(EdgeType.fromCode("ERROR")).isEqualTo(EdgeType.ERROR);
    }

    @Test
    void fromCode_resolvesByEnumName() {
        assertThat(EdgeType.fromCode("DEFAULT")).isEqualTo(EdgeType.DEFAULT);
        assertThat(EdgeType.fromCode("PARALLEL")).isEqualTo(EdgeType.PARALLEL);
        assertThat(EdgeType.fromCode("CONDITION_TRUE")).isEqualTo(EdgeType.CONDITION_TRUE);
    }

    @Test
    void fromJson_serializesCorrectly() {
        assertThat(EdgeType.fromJson("condition-true")).isEqualTo(EdgeType.CONDITION_TRUE);
        assertThat(EdgeType.fromJson("condition-false")).isEqualTo(EdgeType.CONDITION_FALSE);
        assertThat(EdgeType.fromJson("loop")).isEqualTo(EdgeType.LOOP);
    }

    @Test
    void fromJson_withNull_returnsDefault() {
        assertThat(EdgeType.fromJson(null)).isEqualTo(EdgeType.DEFAULT);
    }

    @Test
    void fromJson_withUnknownValue_returnsDefault() {
        assertThat(EdgeType.fromJson("invalid_code")).isEqualTo(EdgeType.DEFAULT);
    }

    @Test
    void getCode_returnsUniqueCode() {
        assertThat(EdgeType.DEFAULT.getCode()).isEqualTo("default");
        assertThat(EdgeType.CONDITION.getCode()).isEqualTo("condition");
        assertThat(EdgeType.CONDITION_TRUE.getCode()).isEqualTo("condition-true");
        assertThat(EdgeType.CONDITION_FALSE.getCode()).isEqualTo("condition-false");
        assertThat(EdgeType.LOOP.getCode()).isEqualTo("loop");
        assertThat(EdgeType.PARALLEL.getCode()).isEqualTo("parallel");
        assertThat(EdgeType.ERROR.getCode()).isEqualTo("error");
    }

    @Test
    void getDescription_returnsChineseDescription() {
        assertThat(EdgeType.DEFAULT.getDescription()).isEqualTo("普通边");
        assertThat(EdgeType.CONDITION.getDescription()).isEqualTo("条件");
        assertThat(EdgeType.CONDITION_TRUE.getDescription()).isEqualTo("条件为真");
        assertThat(EdgeType.CONDITION_FALSE.getDescription()).isEqualTo("条件为假");
        assertThat(EdgeType.LOOP.getDescription()).isEqualTo("循环");
        assertThat(EdgeType.PARALLEL.getDescription()).isEqualTo("并行分支");
        assertThat(EdgeType.ERROR.getDescription()).isEqualTo("错误");
    }

    @Test
    void values_includesAllEdgeTypes() {
        assertThat(EdgeType.values()).hasSize(7);
    }
}
