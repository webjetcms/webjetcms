package sk.iway.displaytag;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.exception.DecoratorException;
import org.displaytag.properties.MediaTypeEnum;

/**
 *  MinutesToHoursDecorator.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 24.8.2012 16:41:08
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class HoursMinutesDecorator implements DisplaytagColumnDecorator
{
	private static int MINUTES_PER_HOUR = 60;
	
	@Override
	public Object decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media) throws DecoratorException
	{
		if(columnValue != null) 
		{
			if(columnValue instanceof Integer)
			{
				int minutes = (Integer)columnValue;
				String hoursPart = Integer.toString(minutes / MINUTES_PER_HOUR);
				String minutesPart = Integer.toString(minutes % MINUTES_PER_HOUR);
				if(hoursPart.length()==1)
					hoursPart="0"+hoursPart;		
				if(minutesPart.length()==1)
					minutesPart="0"+minutesPart;		
				return hoursPart+":"+minutesPart;
				
			}
			if(columnValue instanceof Long)
			{
				long minutes = (Long)columnValue;
				String hoursPart = Long.toString(minutes / MINUTES_PER_HOUR);
				String minutesPart = Long.toString(minutes % MINUTES_PER_HOUR);
				if(hoursPart.length()==1)
					hoursPart="0"+hoursPart;		
				if(minutesPart.length()==1)
					minutesPart="0"+minutesPart;		
				return hoursPart+":"+minutesPart;
			}
			return(columnValue.toString());
		}
		return "&nbsp;";
	}
	
}
