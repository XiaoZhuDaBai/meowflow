package com.meowflow.user.test;

import com.meowflow.common.context.UserContextHolder;
import com.meowflow.common.context.TraceContextHolder;
import com.meowflow.user.aspectj.DataScopeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * meowflow-user 模块专用单元测试基类，继承自通用 {@code BaseUnitTest}，
 * 在 {@code @AfterEach} 钩子里额外清理 DataScopeContext，避免线程复用导致数据污染。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseUserUnitTest {

    @AfterEach
    void cleanupThreadLocals() {
        UserContextHolder.remove();
        TraceContextHolder.remove();
        DataScopeContext.clear();
    }
}
