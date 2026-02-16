package sk.iway.displaytag;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.properties.MediaTypeEnum;

import sk.iway.iwcm.Tools;
import sk.iway.tags.CurrencyTag;

/**
 *  PriceDecorator.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 9.3.2006 17:24:54
 *@modified     $Date: 2007/09/07 07:53:36 $
 */
public class PriceDecorator implements DisplaytagColumnDecorator
{
	@Override
	public final String decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media)
   {
		if(columnValue != null)
		{
			double price = Tools.getDoubleValue(columnValue.toString(), 0);
			return(CurrencyTag.formatNumber(price));
		}
		return "&nbsp;";
   }

}
