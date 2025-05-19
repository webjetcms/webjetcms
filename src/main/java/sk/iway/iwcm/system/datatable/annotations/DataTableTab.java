package sk.iway.iwcm.system.datatable.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DataTableTab {
    String id() default "";
    String title() default "";
    boolean selected() default false;
    boolean hideOnCreate() default false;
    String content() default "null";
}
