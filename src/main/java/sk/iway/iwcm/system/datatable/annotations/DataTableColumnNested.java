package sk.iway.iwcm.system.datatable.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Oznacenie vnoreneho atributu, ktoreho atributy sa tiez maju scanovat (inner object)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DataTableColumnNested {
    String prefix() default "auto";
}