package sk.iway;

import net.htmlparser.jericho.Source;

/**
 *  Konvertuje HTML kod do cisteho textu
 *
 *@Title        magma-web
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.5 $
 *@created      Piatok, 2003, november 28
 *@modified     $Date: 2005/10/25 06:48:04 $
 */
public class Html2Text
{

	/**
	 *  Description of the Method
	 *
	 *@param  html_text  Description of the Parameter
	 *@return            Description of the Return Value
	 */
	public static String html2text(String html_text)
	{
		if (html_text == null) return("");
		return new Source(html_text).getTextExtractor().toString();
	}
}
