package sk.iway.iwcm.system.multidomain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an entity that uses the common domain ID (is domainId independent).
 * Entities marked with this annotation will always use the Constants.getInt("domainIdCommon") - typically 1, regardless of the current domain context.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DomainIdCommon {
}
