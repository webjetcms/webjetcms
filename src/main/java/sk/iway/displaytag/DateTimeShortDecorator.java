package sk.iway.displaytag;

import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.properties.MediaTypeEnum;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 *  DateTimeShortDecorator.java - decorator datumu a casu v skratenom formate DD.MM. hh:mm
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 17.7.2006 15:37:55
 *@modified     $Date: 2007/09/07 07:53:36 $
 */
public class DateTimeShortDecorator implements DisplaytagColumnDecorator
{
	private static SimpleDateFormat sdf = null;
	@Override
	public final String decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media)
   {
		if (sdf == null)
		{
			String dateTimeShortFormat = Constants.getString("dateTimeShortFormat");
			if (Tools.isEmpty(dateTimeShortFormat))
			{
				dateTimeShortFormat = "dd.MM H:mm";
			}
			sdf = new SimpleDateFormat(dateTimeShortFormat);
		}
		
		if(columnValue != null) {
			if ( columnValue instanceof Date)
			{
				Date date = (Date) columnValue;
				return(sdf.format(date));
			}
			//Logger.println(DateTimeShortDecorator.class, columnValue.getClass().toString());
			return(columnValue.toString());
		}
		return "&nbsp;";
   }
}
