package sk.iway.iwcm.system.datatable.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataTableColumnEditor {
    String type() default "";
    String tab() default "";
    String format() default "";
    String wireFormat() default "";
    String label() default "";
    String message() default "";
    DataTableColumnEditorAttr[] options() default {};
    DataTableColumnEditorAttr[] attr() default {};
    DataTableColumnEditorAttr[] opts() default {};
    String separator() default "";
    DataTableOptionMethod[] optionMethods() default {};
}
