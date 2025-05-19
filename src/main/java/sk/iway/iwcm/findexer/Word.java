package sk.iway.iwcm.findexer;

import java.io.InputStream;

import org.apache.poi.hwpf.extractor.WordExtractor;

import sk.iway.iwcm.io.IwcmInputStream;

/**
 *  Vytiahne z Wordu vsetky texty
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.1 $
 *@created      Streda, 2004, január 21
 *@modified     $Date: 2004/01/22 12:30:58 $
 */
public class Word
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
      		//parse Word document
      		WordExtractor we = new WordExtractor(is);
      		sb.append(we.getText());
            is.close();
            we.close();
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
