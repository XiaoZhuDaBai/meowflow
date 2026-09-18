package com.meowflow.common.trace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TraceNode {

    /** 节点名称 */
    String value() default "";

    /** 节点分类：ai / flow / tool / notify / trigger */
    String category() default "default";

    /** 是否记录输入 */
    boolean recordInput() default true;

    /** 是否记录输出 */
    boolean recordOutput() default true;

    /** 输入脱敏字段 */
    String[] inputMaskFields() default {};

    /** 输出脱敏字段 */
    String[] outputMaskFields() default {};
}