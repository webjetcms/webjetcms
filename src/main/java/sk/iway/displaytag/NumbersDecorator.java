package sk.iway.displaytag;

import java.text.DecimalFormat;

import javax.servlet.jsp.PageContext;

import org.displaytag.decorator.DisplaytagColumnDecorator;
import org.displaytag.properties.MediaTypeEnum;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;

/**
 *  NumbersDecorator.java - formatuje dlhe cisla do citatelej podoby, napr. 12345678 -> 12 345 678
 *  formatovanie sa pouzije iba pre HTML vystup
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2010
 *@author       $Author: jeeff jeeff $
 *@version      $Revision: 1.3 $
 *@created      Date: 17.9.2010 14:02:46
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class NumbersDecorator implements DisplaytagColumnDecorator
{
	private static final DecimalFormat nf;
	static
	{
		  nf = new DecimalFormat(Constants.getString("0"));
		  nf.setGroupingSize(3);
		  nf.setGroupingUsed(true);
	}
	
	@Override
	public final String decorate(Object columnValue, PageContext pageContext, MediaTypeEnum media)
   {
		if(columnValue != null)
		{			
			if (media.equals(MediaTypeEnum.HTML))
			{
				long value = Tools.getLongValue(columnValue.toString(), 0);
				return nf.format(value);
			}
			return columnValue.toString();
		}
		return "&nbsp;";
   }

}
