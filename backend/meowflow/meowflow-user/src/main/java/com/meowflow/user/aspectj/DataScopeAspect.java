package com.meowflow.user.aspectj;

import com.meowflow.common.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 数据权限切面，支持 ALL / DEPT / DEPT_AND_SUB / SELF / CUSTOM 五种范围。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DataScopeAspect {

    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_DEPT = "DEPT";
    public static final String SCOPE_DEPT_AND_SUB = "DEPT_AND_SUB";
    public static final String SCOPE_SELF = "SELF";
    public static final String SCOPE_CUSTOM = "CUSTOM";

    @Around("@annotation(com.meowflow.user.aspectj.DataScope)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        DataScope dataScope = getDataScope(joinPoint);
        if (dataScope == null) {
            return joinPoint.proceed();
        }

        String dataScopeType = dataScope.dataScopeType();
        String orgIdColumn = dataScope.deptAlias();
        String userIdColumn = dataScope.userAlias();

        DataScopeContext.set(dataScopeType, orgIdColumn, userIdColumn, UserContextHolder.getOrgId(),
                UserContextHolder.getUserId(), dataScope.customDeptIds());
        try {
            log.debug("DataScope active: type={}, orgIdColumn={}, userIdColumn={}",
                    dataScopeType, orgIdColumn, userIdColumn);
            return joinPoint.proceed();
        } finally {
            DataScopeContext.clear();
        }
    }

    private DataScope getDataScope(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.getTarget().getClass()
                    .getMethod(joinPoint.getSignature().getName(), getParameterTypes(joinPoint))
                    .getAnnotation(DataScope.class);
        } catch (NoSuchMethodException e) {
            try {
                return joinPoint.getTarget().getClass()
                        .getMethod(joinPoint.getSignature().getName())
                        .getAnnotation(DataScope.class);
            } catch (Exception ignored) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Class<?>[] getParameterTypes(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null) {
            return new Class<?>[0];
        }
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return types;
    }

    public String getDataScopeCondition(String dataScopeType, String orgIdColumn, String userIdColumn) {
        if (dataScopeType == null || SCOPE_ALL.equals(dataScopeType)) {
            return "";
        } else if (SCOPE_DEPT.equals(dataScopeType)) {
            Long orgId = UserContextHolder.getOrgId();
            if (orgId == null) {
                return " AND 1=0 ";
            }
            return " AND " + orgIdColumn + " = " + orgId + " ";
        } else if (SCOPE_SELF.equals(dataScopeType)) {
            Long userId = UserContextHolder.getUserId();
            if (userId == null) {
                return " AND 1=0 ";
            }
            return " AND " + userIdColumn + " = " + userId + " ";
        } else if (SCOPE_DEPT_AND_SUB.equals(dataScopeType)) {
            Long orgId = UserContextHolder.getOrgId();
            if (orgId == null) {
                return " AND 1=0 ";
            }
            return " AND (" + orgIdColumn + " = " + orgId + " OR " + orgIdColumn + " IN ("
                    + "SELECT id FROM sys_org WHERE path LIKE CONCAT('" + orgId + "/%'))) ";
        } else if (SCOPE_CUSTOM.equals(dataScopeType)) {
            return "";
        }
        return "";
    }
}
