package com.meowflow.user.aspectj;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    String module() default "";

    String method() default "";

    String description() default "";

    boolean saveParams() default true;

    boolean saveResult() default false;
}
