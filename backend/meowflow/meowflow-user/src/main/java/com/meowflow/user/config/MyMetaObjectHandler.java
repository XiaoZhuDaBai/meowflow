package com.meowflow.user.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.meowflow.common.context.UserContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/**
 * 旧类名保留兼容；统一 Handler 已迁移至 common 模块，本类不再注册为 Bean。
 */
@Slf4j
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "createBy", String.class, getCurrentUsername());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "updateBy", String.class, getCurrentUsername());
    }

    private String getCurrentUsername() {
        try {
            return UserContextHolder.getUsername();
        } catch (Exception e) {
            return "system";
        }
    }
}
