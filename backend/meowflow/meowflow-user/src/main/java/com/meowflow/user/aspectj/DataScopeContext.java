package com.meowflow.user.aspectj;

/**
 * 数据权限线程上下文。
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeInfo> CONTEXT = new ThreadLocal<>();

    private DataScopeContext() {
    }

    public static void set(String scopeType, String deptColumn, String userColumn,
                           Long currentOrgId, Long currentUserId, String customDeptIds) {
        CONTEXT.set(new DataScopeInfo(scopeType, deptColumn, userColumn,
                currentOrgId, currentUserId, customDeptIds));
    }

    public static DataScopeInfo get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record DataScopeInfo(
            String scopeType,
            String deptColumn,
            String userColumn,
            Long currentOrgId,
            Long currentUserId,
            String customDeptIds
    ) {
        public boolean isAll() {
            return scopeType == null || "ALL".equalsIgnoreCase(scopeType);
        }

        public boolean isSelf() {
            return "SELF".equalsIgnoreCase(scopeType);
        }

        public boolean isDept() {
            return "DEPT".equalsIgnoreCase(scopeType);
        }

        public boolean isDeptAndSub() {
            return "DEPT_AND_SUB".equalsIgnoreCase(scopeType);
        }

        public boolean isCustom() {
            return "CUSTOM".equalsIgnoreCase(scopeType);
        }
    }
}
