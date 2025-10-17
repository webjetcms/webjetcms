package sk.iway.iwcm.components.basket.delivery_methods.jpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import sk.iway.iwcm.components.basket.supprot.FieldMapAttr;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DeliveryMethod {
    public String nameKey() default "";
    public FieldMapAttr[] fieldMap() default {};
}