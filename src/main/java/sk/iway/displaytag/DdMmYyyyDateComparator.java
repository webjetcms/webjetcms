package sk.iway.displaytag;

import java.io.Serializable;

import org.displaytag.model.DefaultComparator;

import sk.iway.iwcm.DB;

/**
 *  DdMmYyyyDateComparator.java
 *  Comparator datumov vo formate dd.mm.yyyy
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: Branislav Hric $
 *@version      $Revision: 1.3 $
 *@created      Date: 13.6.2013 13:40:47
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class DdMmYyyyDateComparator extends DefaultComparator implements Serializable
{

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -6179352410099072154L;
 
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

	@Override
	public int compare(Object object1, Object object2)
	{
		long date1 = object1 == null ? 0 : DB.getTimestamp((String)object1);
      long date2 = object2 == null ? 0 : DB.getTimestamp((String)object2);

      if(date1 == date2)
	   	 return 0;
	   if(date1 > date2)
	   	 return ascending ? -1 : 1;
	   if(date1 < date2)
	   	 return ascending ? 1 : -1;
	   return 0 ;
	}
	
}
