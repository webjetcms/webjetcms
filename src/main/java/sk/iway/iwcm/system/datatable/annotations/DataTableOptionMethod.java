package sk.iway.iwcm.system.datatable.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataTableOptionMethod {
    String className() default "";
    String methodName() default "";
    String labelProperty() default "";
    String valueProperty() default "";
}