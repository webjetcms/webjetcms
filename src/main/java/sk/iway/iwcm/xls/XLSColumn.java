package sk.iway.iwcm.xls;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;

/**
 * Oznacuje atribut, ktory sa ma nacitavat z XLS
 *  XLSColumn.java
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2015
 *@author 		mbocko
 *@version      7
 *@created      15.6.2015 7:48:10
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface XLSColumn
{
	public String name() default "";
	public ColumnType type() default ColumnType.STRING;
	public Class<?> columnResolver() default ColumnResolver.class;
	public Class<?> columnType() default Object.class;


	/**
	 * Pattern pre import datoveho typu Date ak je v textovom poli (umoznuje napriklad importovat cas zadany ako text nastavenim patternu HH:mm)
	 * @return
	 */
	public String datePattern() default "";

	@SuppressWarnings("rawtypes")
	public static enum ColumnType
	{
		STRING(String.class),
		INT(Integer.class),
		DOUBLE(Double.class),
		DATE(Date.class),
		BOOLEAN(Boolean.class),
		CUSTOM(Object.class);

		private ColumnType(Class type)
		{
			this.type = type;
		}

		private Class type;
		public Class getType()
		{
			return type;
		}
	}
}
