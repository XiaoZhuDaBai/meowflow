package com.meowflow.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.meowflow.common.context.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 统一填充 MyBatis-Plus 实体时间和审计字段。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        fillAuditField(metaObject, "createBy");
        fillAuditField(metaObject, "updateBy");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        fillAuditField(metaObject, "updateBy");
    }

    private void fillAuditField(MetaObject metaObject, String fieldName) {
        if (!metaObject.hasSetter(fieldName) || metaObject.getValue(fieldName) != null) {
            return;
        }
        Long userId = UserContextHolder.getUserId();
        String username = UserContextHolder.getUsername();
        Class<?> setterType = metaObject.getSetterType(fieldName);
        if (setterType == Long.class && userId != null) {
            metaObject.setValue(fieldName, userId);
        } else if (setterType == String.class && username != null) {
            metaObject.setValue(fieldName, username);
        }
    }
}
