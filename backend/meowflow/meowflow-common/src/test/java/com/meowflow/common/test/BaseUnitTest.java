package com.meowflow.common.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.MockedStatic;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.context.TraceContextHolder;

/**
 * 通用单元测试基类，遵循 project 单元测试规范（UNIT_TEST_GUIDE.md）：
 *
 * <ul>
 *     <li>JUnit 5 + Mockito 5.8 + AssertJ</li>
 *     <li>Given-When-Then 三段式</li>
 *     <li>方法命名：methodName_scenario_expectedResult</li>
 * </ul>
 *
 * 该基类提供：
 * <ol>
 *     <li>Mockito 严格度配置：LENIENT，避免 strict stubbing 报错</li>
 *     <li>线程上下文清理：在每个测试结束后清理 UserContextHolder、TraceContextHolder，
 *         避免跨测试的数据污染</li>
 *     <li>{@link MockedStatic} 工具方法（子类在 try-with-resources 内调用）</li>
 * </ol>
 *
 * <p>
 * 注：DataScopeContext 属于 meowflow-user 模块，如需清理由各模块特定的基类扩展。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseUnitTest {

    @AfterEach
    void cleanupThreadLocals() {
        UserContextHolder.remove();
        TraceContextHolder.remove();
    }

    /**
     * 安全地创建 {@link MockedStatic}，确保子类正确释放资源。
     * 推荐使用 try-with-resources：
     * <pre>{@code
     * try (MockedStatic<XXX> mocked = mockStatic(XXX.class)) {
     *     mocked.when(XXX::method).thenReturn(...);
     *     // ...
     * }
     * }</pre>
     */
    protected <T> MockedStatic<T> mockStatic(Class<T> classToMock) {
        return org.mockito.Mockito.mockStatic(classToMock);
    }
}
