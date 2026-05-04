package sk.iway.iwcm.xls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@SuppressWarnings("rawtypes")
@Retention(RetentionPolicy.RUNTIME)
public @interface XLSImport
{
	public Class importer() default DefaultEntityImporter.class;

}
