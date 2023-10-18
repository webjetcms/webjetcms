package sk.iway.displaytag;

import java.util.Date;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.properties.MediaTypeEnum;

import sk.iway.iwcm.Tools;

/**
 *  DateDecorator.java - decorator pre displaytag pre vypis datumu
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2005
 *@author       $Author: jeeff $
 *@version      $Revision: 1.6 $
 *@created      Date: 3.12.2005 17:03:48
 *@modified     $Date: 2009/04/01 09:06:02 $
 */
public class DateDecorator implements DisplaytagColumnDecorator
{
	@Override
	public final String decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media)
   {
		if(columnValue != null) {
			if (columnValue instanceof Date)
			{
				Date date = (Date) columnValue;
				return(Tools.formatDate(date.getTime()));
			}
			
			//mozno je to long, skus sparsovat
			try
			{
				long date = Long.parseLong(columnValue.toString());
				return(Tools.formatDate(date));
			}
			catch (Exception e)
			{
				
			}
			
			return(columnValue.toString());
		}
		return "&nbsp;";
   }
}
