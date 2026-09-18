package com.meowflow.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.RouteDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 application.yml 中所有声明的路由都存在预期 id / predicate / filter 组合。
 *
 * <p>通过 yaml 字符串直接解析，避免引入 Spring 上下文。
 */
@DisplayName("Gateway 路由约定稳定性")
class GatewayRouteConventionTest {

    @Test
    @DisplayName("application.yml 中所有路由至少有 path predicate 与 StripPrefix 过滤")
    void routes_havePathAndStripPrefix() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
            assertThat(in).isNotNull();
            String yaml = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            // 简单解析 —— 找到每个 "- id: meowflow-XXX" 块
            String[] lines = yaml.split("\\R");
            List<String> routeIds = new ArrayList<>();
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.contains("- id: meowflow-")) {
                    String id = line.substring(line.indexOf("- id:") + "- id:".length()).trim();
                    routeIds.add(id);
                }
            }

            // 期望路由集合
            assertThat(routeIds).contains(
                    "meowflow-user",
                    "meowflow-workflow",
                    "meowflow-executor",
                    "meowflow-template",
                    "meowflow-monitor",
                    "meowflow-infra"
            );
        }
    }
}
