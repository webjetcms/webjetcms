package sk.iway.displaytag;

import java.text.DecimalFormat;

import jakarta.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.exception.DecoratorException;
import org.displaytag.properties.MediaTypeEnum;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 *  CurrencyDecorator.java
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author:  Marián Halaš $
 *@version      $Revision: 1.3 $
 *@created      Date: 12.7.2012 16:06:21
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class CurrencyDecorator implements DisplaytagColumnDecorator
{
	@Override
	public Object decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media) throws DecoratorException
	{
		if(columnValue != null)
		{
			double currency = Tools.getDoubleValue(columnValue.toString(), 0);
			DecimalFormat df1 = new DecimalFormat(Constants.getString("currencyDecoratorFormat"));		
			return Tools.replace(df1.format(currency), ".", ",");
		}
		return "&nbsp;";
	}
}
