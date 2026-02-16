package sk.iway.iwcm.system.jpa;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.persistence.Entity;

import org.apache.commons.beanutils.PropertyUtils;

/**
 *  JpaComparator.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: rusho $
 *@version      $Revision: 1.3 $
 *@created      Date: 26.4.2010 15:23:05
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class JpaComparator<T> implements Serializable, Comparator<T>
{
   /**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -7444047899157864246L;
	public static final boolean ASC = true;
   public static final boolean DESC = false;
   public static final boolean CASE_SENSITIVE = true;
   public static final boolean CASE_INSENSITIVE = false;

   protected boolean ascending;
   protected boolean caseSensitive;
   protected String property;

   public JpaComparator(Class<T> clazz, String propertyName)
	{
		this(clazz,propertyName,true);
	}

	public JpaComparator(Class<T> clazz, String propertyName, boolean isAscending)
	{
		this(clazz,propertyName,isAscending,true);
	}

	public JpaComparator(Class<T> clazz, String propertyName, boolean isAscending, boolean isCaseSensitive)
	{
		if(clazz.getAnnotation(Entity.class)==null)
		{
			throw new IllegalArgumentException("Class '"+clazz.getName()+"' doesn't have @javax.persistence.Entity annotation!");
		}
		ascending = isAscending;
		caseSensitive = isCaseSensitive;
		property = propertyName;
	}
	@Override
	@SuppressWarnings("unchecked")
	public int compare(T o1, T o2) throws ClassCastException
	{
		try
		{
			Object value1 = PropertyUtils.getProperty(o1, property);
	      Object value2 = PropertyUtils.getProperty(o2, property);

	      // nulls first policy...
	      if (value1 == null)
	      {
	          return (value2 == null) ? 0 : -1;
	      }
	      else if (value2 == null)
	      {
	          return 1;
	      }

	      if (this.caseSensitive == false)
	      {
	          value1 = JpaComparator.toUpperCase(value1);
	          value2 = JpaComparator.toUpperCase(value2);
	      }

	      int compareResult = JpaComparator.toComparable(value1).compareTo(JpaComparator.toComparable(value2)); //NOSONAR
	      return (ascending) ? compareResult : -compareResult;
		}
		catch (Exception e)
		{
			 throw new ClassCastException( e.toString() );
		}
	}

	public void orderList(List<T> listOfObjects)
	{
		Collections.sort(listOfObjects, this);
	}

   public static Object toUpperCase(Object object)
   {
      if ((object instanceof String) || (object instanceof StringBuffer))
      {
          return object.toString().toUpperCase();
      }
      else if (object instanceof char[])
      {
          return new String((char[]) object).toUpperCase();
      }
      else
      {
          return object;
      }
  }

  @SuppressWarnings("rawtypes")
   public static Comparable toComparable(Object object)
   {
		if (object == null)
		{
			return null;
		}
		else if (object instanceof Comparable)
		{
			return (Comparable) object;
		}
		else if (object instanceof StringBuffer)
		{
			return object.toString();
		}
		else if (object instanceof char[])
		{
			return new String((char[]) object);
		}
		else
		{
			/* TODO: na zvazenie ci sa spravi toString, alebo hodi nasledovna exception:
			 * throw new ClassCastException("Invalid Comparable class:" + object.getClass().getName());
			 */
			return object.toString();
		}
  }


	// Getters & Setters
	public boolean isAscending()
	{
		return ascending;
	}

	public void setAscending(boolean ascending)
	{
		this.ascending = ascending;
	}

	public boolean isCaseSensitive()
	{
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive)
	{
		this.caseSensitive = caseSensitive;
	}

	public String getProperty()
	{
		return property;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}



}
