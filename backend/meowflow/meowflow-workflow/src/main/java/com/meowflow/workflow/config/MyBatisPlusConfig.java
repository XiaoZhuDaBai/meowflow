package com.meowflow.workflow.config;

/**
 * Workflow 模块不再单独注册 MetaObjectHandler，统一复用
 * {@code com.meowflow.common.config.MyMetaObjectHandler}，避免重复 Bean。
 */
public final class MyBatisPlusConfig {
    private MyBatisPlusConfig() {
    }
}
