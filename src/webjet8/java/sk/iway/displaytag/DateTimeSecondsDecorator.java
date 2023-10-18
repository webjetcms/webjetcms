package sk.iway.displaytag;

import java.util.Date;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.properties.MediaTypeEnum;

import sk.iway.iwcm.Tools;

/**
 *  DateTimeSecondsDecorator.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 28.10.2006 19:36:45
 *@modified     $Date: 2007/09/07 07:53:36 $
 */
public class DateTimeSecondsDecorator implements DisplaytagColumnDecorator
{
	@Override
	public final String decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media)
   {
		
		if(columnValue != null) {
			if ( columnValue instanceof Date)
			{
				Date date = (Date) columnValue;
				return(Tools.formatDateTimeSeconds(date));
			}
			//mozno je to long, skus sparsovat
			try
			{
				long date = Long.parseLong(columnValue.toString());
				if (date < 1000) return "";
				return(Tools.formatDateTimeSeconds(date));
			}
			catch (Exception e)
			{
				
			}
			
			//Logger.println(DateTimeDecorator.class, columnValue.getClass().toString());
			return(columnValue.toString());
		}
		return "&nbsp;";
   }
}
