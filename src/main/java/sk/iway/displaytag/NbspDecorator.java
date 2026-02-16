package sk.iway.displaytag;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.properties.MediaTypeEnum;

/**
 *  NbspDecorator.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Date: 4.7.2006 10:45:34
 *@modified     $Date: 2007/09/07 07:53:36 $
 */
public class NbspDecorator implements DisplaytagColumnDecorator
{
	 @Override
	public final String decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media)
   {
		if(columnValue==null || columnValue.toString().equals(""))
			return "&nbsp;";
		return columnValue.toString();
   }
}
