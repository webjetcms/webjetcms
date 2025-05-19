package sk.iway.displaytag;


import java.io.Serializable;

import org.displaytag.exception.ObjectLookupException;
import org.displaytag.exception.RuntimeLookupException;
import org.displaytag.model.Cell;
import org.displaytag.model.DefaultComparator;
import org.displaytag.model.Row;
import org.displaytag.util.LookupUtil;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.SearchTools;

/**
 *  NumberComparator.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.4 $
 *@created      Date: 10.4.2006 10:56:56
 *@modified     $Date: 2006/11/30 09:31:20 $
 */
public class NumberComparator extends DefaultComparator implements Serializable
{
	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -6110437067864934090L;
	/**
	 * name of the property in bean.
	 */
	private String property;
	/**
	 * sort order ascending?
	 */
	private boolean ascending;

	/**
	 * @param ascending
	 *           The ascending to set.
	 */
	public void setAscending(boolean ascending)
	{
		this.ascending = ascending;
	}

	/**
	 * @param property
	 *           The property to set.
	 */
	public void setProperty(String property)
	{
		this.property = property;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Object object1, Object object2)
	{

		Object obj1 = null;
      Object obj2 = null;

      // if property is null compare using two static cell objects
      if (this.property == null)
      {
          return checkNullsAndCompare(object1, object2);
      }

      if (object1 instanceof Row)
      {
          obj1 = ((Row) object1).getObject();
      }
      if (object2 instanceof Row)
      {
          obj2 = ((Row) object2).getObject();
      }

      try
      {
          Object result1 = null;
          Object result2 = null;

          // If they have supplied a decorator, then make sure and use it for the sorting as well
          result1 = LookupUtil.getBeanProperty(obj1, this.property);
          result2 = LookupUtil.getBeanProperty(obj2, this.property);

          return checkNullsAndCompare(result1, result2);
      }
      catch (ObjectLookupException e)
      {
          throw new RuntimeLookupException(getClass(), this.property, e);
      }



	}

	/**
    * Compares two given objects handlig nulls and not comparable objects are handled. Not comparable objects are
    * compared using their string representation.
    * @param object1 first object to compare
    * @param object2 second object to compare
    * @return int result
    */
   private int checkNullsAndCompare(Object object1, Object object2)
   {
       int returnValue = 0;

       if (object1 != null && object2 != null)
       {
           // if object are not null and don't implement comparable, compare using string values

			try
			{
				String str1 = object1.toString().trim();
				String str2 = object2.toString().trim();

				if (object1 instanceof Cell)
				{
					str1 = (String)((Cell)object1).getStaticValue();
				}
				if (object2 instanceof Cell)
				{
					str2 = (String)((Cell)object2).getStaticValue();
				}

				str1 = SearchTools.htmlToPlain(str1);
				str2 = SearchTools.htmlToPlain(str2);

				str1 = str1.replace(',', '.');
				str2 = str2.replace(',', '.');

				str1 = Tools.replace(str1, "%", "");
				str1 = Tools.replace(str1, " ", "");

				str2 = Tools.replace(str2, "%", "");
				str2 = Tools.replace(str2, " ", "");

				if ("-".equals(str1)) str1="0";
				if ("-".equals(str2)) str2="0";

				str1 = getOnlyNumber(str1);
				str2 = getOnlyNumber(str2);

				//System.out.println("compare: "+str1+" vs "+str2);

				double number1 = Double.parseDouble(str1);
				double number2 = Double.parseDouble(str2);
				if (number1==number2)
				{
					return 0;
				}
				if (number1 < number2)
				{
					returnValue = 1;
				}
				else
				{
					returnValue = -1;
				}
			}
			catch (Exception ex)
			{
				sk.iway.iwcm.Logger.error(ex);
			}
       }
       else if (object1 == null && object2 != null)
       {
           returnValue = 1;
       }
       else if (object1 != null && object2 == null)
       {
           returnValue = -1;
       }
       else
       {
           // both null
           returnValue = 0;
       }

       int ascendingInt = this.ascending ? 1 : -1;
       return ascendingInt * returnValue;
   }

   /**
    * vrati napr z 10 123.22 EUR > 10123.22
    * @param src
    * @return
    */
   private String getOnlyNumber(String src)
   {
   	if(Tools.isEmpty(src)) return "0";
   	StringBuilder builder = new StringBuilder("");
      for (int i = 0; i < src.length(); i++) {
          char c = src.charAt(i);
          if (Character.isDigit(c) || '.' == c || ',' == c) {
              builder.append(c);
          }
      }
      return (builder.length() > 0 ? builder.toString() : "0");
   }
}