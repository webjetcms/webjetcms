package sk.iway.iwcm.system.stripes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * oznacuje ze actionBean triede budu injectnute pageparams ako parametre requestu
 * 
 * @author mbocko
 *
 */
@Target(value={ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface BindPageParams { }