package com.meowflow.common.test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * 通用集成测试基类 — 需要启动 Spring 容器的测试都继承它。
 *
 * <p>注意：<b>默认不激活任何特定 profile</b>，由调用方决定。集成测试应当在子类加
 * <code>@ActiveProfiles("isolation")</code> 或 <code>@ActiveProfiles("integration")</code>。
 * 子类可使用 <code>@SpringBootTest(classes = {...})</code> 指定目标 Application。
 *
 * <p>辅助工具：
 * <ul>
 *   <li>{@link SaTokenMockHelper} — 模拟登录态（设置 / 清除 {@code UserContextHolder}）</li>
 * </ul>
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
}
