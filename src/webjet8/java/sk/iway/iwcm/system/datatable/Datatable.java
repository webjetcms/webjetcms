package sk.iway.iwcm.system.datatable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Title        webjet8
 * Company      Interway a. s. (www.interway.sk)
 * Copyright    Interway a. s. (c) 2001-2019
 * @author       tmarcinkova $
 * @created      2019/05/10 11:03
 *
 *  Anotacia '@Datatable'
 *  	- treba ju dat triedam, ktorych vynimky potrebujeme spracovat ako response pre DataTables Editor (postara sa o to
 *  	  trieda DatatableExceptionHandler)
 *  	- urcite ju treba dat @RestController-om ktore rozsiruju DatatableRestController
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Datatable
{
}
