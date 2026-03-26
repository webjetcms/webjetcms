package sk.iway.iwcm.system.stripes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotuje atribut v actionbeane, ktory sa setuje len z pageParams, 
 *  ak je obsiahnuty v povodnom requeste, ignoruje sa
 * @author mbocko
 *
 */
@Target(value=ElementType.FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface PageParamOnly  { }