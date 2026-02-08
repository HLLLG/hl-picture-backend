package com.hl.hlpicture.infrastructure.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) // 作用范围
@Retention(RetentionPolicy.RUNTIME) // 生效状态
public @interface AuthCheck {

    /**
     * 必需的权限
     *
     * @return
     */
    String mustRole() default "";
}
