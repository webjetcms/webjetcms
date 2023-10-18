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
    //tento sa da zapnut na zobrazenie
    boolean[] visible() default {};
    //tento sa v DT uplne schova a neda sa ani v nastaveniach DT zobrazit
    boolean[] hidden() default {};
    //tento sa v DT EDITORE nezobrazi
    boolean[] hiddenEditor() default {};
    //vypnutie filtra pre stlpec
    boolean[] filter() default {};
    DataTableColumnEditor[] editor() default {};
    DataTableColumnType[] inputType() default {};
    String sortAfter() default "";
    String perms() default "";
    boolean[] orderable() default {};
    //skratka na nastavenie default hodnoty do editora
    String defaultValue() default "";
}
