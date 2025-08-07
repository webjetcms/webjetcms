package sk.iway.iwcm.system.datatable.annotations;

import sk.iway.iwcm.system.datatable.DataTableColumnType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataTableColumn {
    String data() default "";
    String tab() default "";
    String name() default "";
    String title() default "";
    String[] defaultContent() default {};
    String className() default "";
    String renderFormat() default "";
    String renderFormatLinkTemplate() default "";
    String renderFormatPrefix() default "";
    //column default visibility in table, if false, user can set column to visible
    boolean[] visible() default {};
    //set to true to hide column in table
    boolean[] hidden() default {};
    //set to true to hide column in editor
    boolean[] hiddenEditor() default {};
    //set to false to disable column filter
    boolean[] filter() default {};

    DataTableColumnEditor[] editor() default {};
    DataTableColumnType[] inputType() default {};
    String sortAfter() default "";
    String perms() default "";
    boolean[] orderable() default {};

    /**
     * set default value for column in editor for new entity.
     * Can not be used with fetchOnCreate because it will be overridden by returned entity.
     */
    String defaultValue() default "";

    //if true property is always copied from old object to new object on edit
    boolean[] alwaysCopyProperties() default {};

    /**
     * specify JPA column for order, can have multiple values, eg.:
     * orderProperty = "contactFirstName,deliveryName"
     */
    String orderProperty() default "";


    //
    DataTableAi[] ai() default {};
}
