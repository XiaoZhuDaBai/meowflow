package com.meowflow.user.aspectj;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.user.test.BaseUserUnitTest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DataScopeAspect 单元测试。
 *
 * <p>主要覆盖范围：
 * <ul>
 *     <li>ALL / DEPT / SELF / DEPT_AND_SUB / CUSTOM 条件的 SQL 拼接</li>
 *     <li>DataScopeContext 上下文正确设置和清理</li>
 * </ul>
 */
@DisplayName("DataScopeAspect Tests")
class DataScopeAspectTest extends BaseUserUnitTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private org.aopalliance.aop.Advice advice;

    private final DataScopeAspect aspect = new DataScopeAspect();

    @Nested
    @DisplayName("SQL Condition Generation")
    class SqlConditionTests {

        @Test
        @DisplayName("ALL scope returns empty condition")
        void getDataScopeCondition_All_returnsEmpty() {
            // Given
            UserContextHolder.set(com.meowflow.common.context.UserContext.builder()
                    .userId(1L).orgId(1L).build());

            // When
            String cond = aspect.getDataScopeCondition(DataScopeAspect.SCOPE_ALL, "dept_id", "user_id");

            // Then
            assertThat(cond).isEmpty();
        }

        @Test
        @DisplayName("DEPT scope returns dept id filter")
        void getDataScopeCondition_Dept_returnsOrgFilter() {
            // Given
            UserContextHolder.set(com.meowflow.common.context.UserContext.builder()
                    .userId(1L).orgId(1L).build());

            // When
            String cond = aspect.getDataScopeCondition(DataScopeAspect.SCOPE_DEPT, "dept_id", "user_id");

            // Then
            assertThat(cond).contains("AND dept_id = 1");
        }

        @Test
        @DisplayName("SELF scope returns user id filter")
        void getDataScopeCondition_Self_returnsUserFilter() {
            // Given
            UserContextHolder.set(com.meowflow.common.context.UserContext.builder()
                    .userId(1L).username("alice").orgId(1L).build());

            // When
            String cond = aspect.getDataScopeCondition(DataScopeAspect.SCOPE_SELF, "dept_id", "user_id");

            // Then
            assertThat(cond).contains("AND user_id = 1");
        }

        @Test
        @DisplayName("DEPT scope without orgId returns 1=0 safety guard")
        void getDataScopeCondition_Dept_noOrgId_returnsFalseClause() {
            // Given - no org id
            UserContextHolder.set(com.meowflow.common.context.UserContext.builder()
                    .userId(1L).build());

            // When
            String cond = aspect.getDataScopeCondition(DataScopeAspect.SCOPE_DEPT, "dept_id", "user_id");

            // Then
            assertThat(cond).contains("1=0");
        }

        @Test
        @DisplayName("SELF scope without userId returns 1=0 safety guard")
        void getDataScopeCondition_Self_noUserId_returnsFalseClause() {
            // Given
            UserContextHolder.set(com.meowflow.common.context.UserContext.builder().build());

            // When
            String cond = aspect.getDataScopeCondition(DataScopeAspect.SCOPE_SELF, "dept_id", "user_id");

            // Then
            assertThat(cond).contains("1=0");
        }

        @Test
        @DisplayName("DEPT_AND_SUB scope generates IN subquery")
        void getDataScopeCondition_DeptAndSub_returnsInSubquery() {
            // Given
            UserContextHolder.set(com.meowflow.common.context.UserContext.builder()
                    .userId(1L).orgId(1L).build());

            // When
            String cond = aspect.getDataScopeCondition(DataScopeAspect.SCOPE_DEPT_AND_SUB, "dept_id", "user_id");

            // Then
            assertThat(cond).contains("1");
            assertThat(cond).contains("LIKE");
        }

        @Test
        @DisplayName("CUSTOM scope returns empty (caller fills in)")
        void getDataScopeCondition_Custom_returnsEmpty() {
            // When
            String cond = aspect.getDataScopeCondition(DataScopeAspect.SCOPE_CUSTOM, "dept_id", "user_id");

            // Then
            assertThat(cond).isEmpty();
        }

        @Test
        @DisplayName("Unknown scope returns empty")
        void getDataScopeCondition_Unknown_returnsEmpty() {
            // When
            String cond = aspect.getDataScopeCondition("UNKNOWN", "dept_id", "user_id");

            // Then
            assertThat(cond).isEmpty();
        }

        @Test
        @DisplayName("Null scope returns empty")
        void getDataScopeCondition_Null_returnsEmpty() {
            // When
            String cond = aspect.getDataScopeCondition(null, "dept_id", "user_id");

            // Then
            assertThat(cond).isEmpty();
        }
    }

    @Nested
    @DisplayName("DataScopeContext")
    class DataScopeContextTests {

        @Test
        @DisplayName("set and get return same info")
        void setAndGet_returnsSameInfo() {
            // Given
            DataScopeContext.set("DEPT", "dept_id", "user_id", 1L, 1L, "");

            try {
                // When
                DataScopeContext.DataScopeInfo info = DataScopeContext.get();

                // Then
                assertThat(info).isNotNull();
                assertThat(info.scopeType()).isEqualTo("DEPT");
                assertThat(info.deptColumn()).isEqualTo("dept_id");
                assertThat(info.userColumn()).isEqualTo("user_id");
                assertThat(info.currentOrgId()).isEqualTo(1L);
                assertThat(info.currentUserId()).isEqualTo(1L);
                assertThat(info.isDept()).isTrue();
                assertThat(info.isSelf()).isFalse();
                assertThat(info.isAll()).isFalse();
            } finally {
                DataScopeContext.clear();
            }
        }

        @Test
        @DisplayName("isAll returns true for null/ALL scope")
        void isAll_nullOrAll_returnsTrue() {
            DataScopeContext.set(null, "", "", null, null, "");
            try {
                assertThat(DataScopeContext.get().isAll()).isTrue();
            } finally {
                DataScopeContext.clear();
            }

            DataScopeContext.set("ALL", "", "", null, null, "");
            try {
                assertThat(DataScopeContext.get().isAll()).isTrue();
            } finally {
                DataScopeContext.clear();
            }
        }

        @Test
        @DisplayName("isSelf returns true for SELF scope")
        void isSelf_returnsTrue() {
            DataScopeContext.set("SELF", "", "", null, 1L, "");
            try {
                assertThat(DataScopeContext.get().isSelf()).isTrue();
            } finally {
                DataScopeContext.clear();
            }
        }

        @Test
        @DisplayName("clear removes context")
        void clear_removesContext() {
            // Given
            DataScopeContext.set("DEPT", "dept_id", "user_id", 1L, 1L, "");

            // When
            DataScopeContext.clear();

            // Then
            assertThat(DataScopeContext.get()).isNull();
        }
    }
}
