package sk.iway.iwcm.system.datatable.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DataTableTabs {
    DataTableTab[] tabs() default {};
}
