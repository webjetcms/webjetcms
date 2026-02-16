package sk.iway.displaytag;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.properties.MediaTypeEnum;


/**
 *  BooleanDeacorator.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2011
 *@author       $Author: Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 18.2.2011 14:50:58
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class BooleanDecorator implements DisplaytagColumnDecorator
{
	@Override
	public String decorate(Object columnValue, PageContext arg1, MediaTypeEnum arg2)
	{
		if(columnValue != null) 
		{
			if ( columnValue instanceof Boolean)
			{
				boolean truefalse = (Boolean) columnValue;
				//Logger.println(BooleanDecorator.class, columnValue.getClass().toString());
				if(truefalse){
					return "<span class=\"glyphicon glyphicon-ok-circle\" aria-hidden=\"true\"></span>";
				}
				else{
					return "<span class=\"glyphicon glyphicon-ban-circle\" aria-hidden=\"true\"></span> ";
				}

			}
							
			//Logger.println(DateTimeDecorator.class, columnValue.getClass().toString());
			return(columnValue.toString());
		}
		return "&nbsp;";
	}
}
