package sk.iway.displaytag;

import org.displaytag.export.excel.ExcelHssfView;

import sk.iway.iwcm.common.SearchTools;

/**
 *  ExcelExport.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Date: 1.6.2006 13:55:31
 *@modified     $Date: 2007/09/07 07:53:36 $
 */
public class ExcelExport extends ExcelHssfView
{
	 @Override
	protected java.lang.String escapeColumnValue(java.lang.Object value) 
	{
		String text="";
		if(value != null) {
			text=value.toString().trim();
			if(text.endsWith("&nbsp;"))
   			text=text.substring(0,text.length()-6);
			text = SearchTools.htmlToPlain(text);
		}
		return super.escapeColumnValue(text);
	}
}
