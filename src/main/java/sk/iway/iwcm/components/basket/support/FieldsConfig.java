package sk.iway.iwcm.components.basket.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FieldsConfig {
    public String nameKey() default "";
    public FieldMapAttr[] fieldMap() default {};
}