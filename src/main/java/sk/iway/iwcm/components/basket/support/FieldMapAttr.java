package sk.iway.iwcm.components.basket.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import sk.iway.iwcm.editor.FieldType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldMapAttr {
    public char fieldAlphabet();
    public FieldType fieldType() default FieldType.TEXT;
    public String fieldLabel() default "";
    public boolean isRequired() default false;
    public String defaultValue() default "";
}