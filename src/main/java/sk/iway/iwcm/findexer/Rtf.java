package sk.iway.iwcm.findexer;

import java.io.InputStream;

import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

import sk.iway.iwcm.io.IwcmInputStream;

/**
 *  Vytiahne z RTF vsetky texty
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Streda, 2004, január 21
 *@modified     $Date: 2010/01/20 08:45:42 $
 */
public class Rtf
{

   /**
    *  Gets the text attribute of the Word class
    *
    *@param  fileName  Description of the Parameter
    *@return           The text value
    */
	public static String getText(String fileName)
	{
		try
      {
			IwcmInputStream is = new IwcmInputStream(fileName);
			return getText(is);
      }
	   catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
		return null;
	}


	/**
	 * Gets the text attribute of the Word class
	 *
	 * @param is - IwcmInputStream
	 * @return
	 */
   public static String getText(InputStream is)
   {
   	StringBuilder sb = new StringBuilder();
      try
      {
      	if (is != null)
      	{
      		DefaultStyledDocument sd = new DefaultStyledDocument();
      		RTFEditorKit rtfKit = new RTFEditorKit();
      		rtfKit.read(is, sd, 0);

            sb.append(sd.getText(0, sd.getLength()));

            is.close();
         }
         return(sb.toString());
      }
      catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }

      return (null);
   }
}
