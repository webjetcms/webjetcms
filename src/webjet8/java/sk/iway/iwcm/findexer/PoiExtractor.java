package sk.iway.iwcm.findexer;

import java.io.InputStream;

import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.extractor.POITextExtractor;

import sk.iway.iwcm.io.IwcmInputStream;

/**
 *  Vytiahne z Wordu vsetky texty
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.3 $
 *@created      Streda, 2004, január 21
 *@modified     $Date: 2010/01/20 08:45:42 $
 */
public class PoiExtractor
{

   /**
    *  Gets the text attribute of the Word class
    *
    *@param  fileName  Description of the Parameter
    *@return           The text value
    */
	public static String getText(String fileName)
	{
		IwcmInputStream is = null;
		String result = null;
		try
      {
			is = new IwcmInputStream(fileName);
      }
	   catch (Exception ex)
      {
         sk.iway.iwcm.Logger.error(ex);
      }
	   if (is != null)
		{
			result = getText(is);
			try { if (is!=null) is.close(); } catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }
		}
	   //ked sa nepodari ziskat data z doc, tak skus ci sa nedaju ziskat data cez rtf
	   if(result == null)
	   {
	   	try
	      {
				is = new IwcmInputStream(fileName);
				result = Rtf.getText(is);
				is.close();
	      }
		   catch (Exception ex)
	      {
	         sk.iway.iwcm.Logger.error(ex);
	      }
	   }
		return result;
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
      		POITextExtractor extractor = ExtractorFactory.createExtractor(is);
      		sb.append(extractor.getText());
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
