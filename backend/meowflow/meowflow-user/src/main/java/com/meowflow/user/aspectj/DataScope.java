package com.meowflow.user.aspectj;

import java.lang.annotation.*;

/**
 * 数据权限注解。
 * <p>
 * 配合 {@link DataScopeAspect} 使用。提供五种数据权限范围：
 * <ul>
 *     <li>ALL - 全部数据</li>
 *     <li>DEPT - 本部门数据</li>
 *     <li>DEPT_AND_SUB - 本部门及下级部门数据</li>
 *     <li>SELF - 仅本人数据</li>
 *     <li>CUSTOM - 自定义部门数据，通过 {@link #customDeptIds()} 指定</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    String dataScopeType() default "ALL";

    String deptAlias() default "";

    String userAlias() default "";

    /**
     * 自定义部门 ID 列表，仅当 dataScopeType = CUSTOM 时生效。
     * 支持字符串形式，例如 "1,2,3"。
     */
    String customDeptIds() default "";
}
