package sk.iway.iwcm.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *  DataSource.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2017
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 11. 1. 2017 11:47:34
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataSource 
{

	String name() default "iwcm";

}
