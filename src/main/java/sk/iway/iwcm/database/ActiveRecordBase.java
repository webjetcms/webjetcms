package sk.iway.iwcm.database;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;

import javax.persistence.Transient;

import org.apache.commons.beanutils.PropertyUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import sk.iway.iwcm.database.nestedsets.CommonNestedSetBean;

/**
 *  ActiveRecordBase.java
 *
 *		Basic subclass for easier and uniform work with
 *		JPA beans. Supposed to work together with JpaDB class,
 *		namely subclasses of JpaDB.
 *
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: marosurbanec $
 *@version      $Revision: 1.3 $
 *@created      Date: 28.2.2011 11:21:12
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public abstract class ActiveRecordBase
{
	//pouziva sa pri importe ako cislo riadku v XLSx subore
	@Transient
	@JsonInclude(Include.NON_NULL)
	private Integer __rowNum__;

	public boolean save()
	{
		String dbName = "iwcm";
		if (this.getClass().isAnnotationPresent(DataSource.class)) {

			Annotation annotation = this.getClass().getAnnotation(DataSource.class);
			DataSource dataSource = (DataSource) annotation;
			dbName = dataSource.name();
		}

		return new JpaDB<ActiveRecordBase>(ActiveRecordBase.class,dbName).save(this);
	}

	public boolean delete()
	{
		String dbName = "iwcm";
		if (this.getClass().isAnnotationPresent(DataSource.class)) {

			Annotation annotation = this.getClass().getAnnotation(DataSource.class);
			DataSource dataSource = (DataSource) annotation;
			dbName = dataSource.name();
		}

		return new JpaDB<ActiveRecordBase>(ActiveRecordBase.class,dbName).delete(this);
	}

	@Override
	public String toString()
	{
		StringBuilder toString = new StringBuilder();

		try
		{
			for (PropertyDescriptor descriptor : PropertyUtils.getPropertyDescriptors(this.getClass()))
			{

					String property = descriptor.getName();
					if (this instanceof CommonNestedSetBean && CommonNestedSetBean.TO_STRING_IGNORED_PROPERTIES.contains(property))
						continue;

					Object value = PropertyUtils.getProperty(this, property);
					if ( value!=null && value instanceof ActiveRecordBase)
					{
						//zabran rekurzivnej lavine
						toString.append(property).append(" = ");
						toString.append(" instance of ").append(value.getClass().getCanonicalName()).append(',');
					}
					else
					{
						//toString.append(property).append(" = kukni kod ");
						//jeeff:toto sposobovalo stack overflow pri importe struktury v intranete toString.append(String.valueOf(value)).append(',');
						//toString.append(String.valueOf(value)).append(',');
					}
			}
		}
		catch (StackOverflowError ex)
		{
		   sk.iway.iwcm.Logger.error(ActiveRecord.class, ex.getMessage(), ex);
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return toString.toString();
	}

	public Integer get__rowNum__() {
		return __rowNum__;
	}

	public void set__rowNum__(Integer __rowNum__) {
		this.__rowNum__ = __rowNum__;
	}


}