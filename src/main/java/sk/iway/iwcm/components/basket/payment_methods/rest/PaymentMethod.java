package sk.iway.iwcm.components.basket.payment_methods.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import sk.iway.iwcm.components.basket.support.FieldMapAttr;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PaymentMethod {
    public String nameKey() default "";
    public FieldMapAttr[] fieldMap() default {};
}