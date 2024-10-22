package sk.iway.iwcm.system.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Anotacia pre zobrazenie Spring komponenty v zozname aplikacii (appstore)
 * /docs/custom-apps/appstore/
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface WebjetAppStore {
    String nameKey() default "";
    String descKey() default "";
    String imagePath() default "";
    String galleryImages() default "";
    String domainName() default "";
    boolean commonSettings() default true;
    boolean[] custom() default {};
    String itemKey() default "";
    String componentPath() default ""; //If this APP is for old JSP components enter path to JSP file, eg. /components/gallery/gallery.jsp
}