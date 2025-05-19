package sk.iway.displaytag;

import org.displaytag.export.CsvView;

import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.SearchTools;

/**
 *  ExcelExport.java
 *
 *@Title        webjet4
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2006
 *@author       $Author: miros $
 *@version      $Revision: 1.2 $
 *@created      Date: 30.6.2006 14:33:11
 *@modified     $Date: 2006/09/22 15:18:46 $
 */
public class CsvExport extends CsvView
{
	@Override
	public String getMimeType()
   {
       return "text/csv; charset="+SetCharacterEncodingFilter.getEncoding(); //$NON-NLS-1$
		//return "text/csv";
   } 
	
	@Override
	protected java.lang.String escapeColumnValue(java.lang.Object value) {
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

