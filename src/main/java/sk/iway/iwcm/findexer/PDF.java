package sk.iway.iwcm.findexer;

import java.util.StringTokenizer;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmInputStream;


/**
 *  Vytiahne z PDF vsetky texty
 *
 *@Title        WebJET
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2002
 *@author       $Author: jeeff $
 *@version      $Revision: 1.2 $
 *@created      Streda, 2004, január 21
 *@modified     $Date: 2004/01/27 17:48:00 $
 */
public class PDF
{

	/**
	 *  Gets the text attribute of the PDF class
	 *
	 *@param  fileName  Description of the Parameter
	 *@return           The text value
	 */
	public static String getText(String fileName)
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			IwcmInputStream is = new IwcmInputStream(fileName);

			PDDocument pdfDocument = Loader.loadPDF(new RandomAccessReadBuffer(is), "");
	        try {
	      	   /*
	            if (pdfDocument.isEncrypted()) {
	                try {
	                    pdfDocument.decrypt("");
	                } catch (Exception e) {
	                    // Ignore
	                }
	            }
	            */

	            PDFTextStripper stripper = new PDFTextStripper();

				   stripper.setSortByPosition(false);
				   stripper.setSuppressDuplicateOverlappingText(true);

				   String text = stripper.getText(pdfDocument);

				   //System.out.println(text);

              StringTokenizer st = new StringTokenizer(text, "\n");
              while (st.hasMoreTokens())
              {
                 String line = fixVerticalText(st.nextToken());
                 if (Tools.isEmpty(line)) continue;
                 sb.append(line);
                 sb.append('\n');
              }

               //sb.append(text);

	        } finally {
	            pdfDocument.close();
	        }

			try { if (is!=null) is.close(); } catch (Exception ex) { sk.iway.iwcm.Logger.error(ex); }

			return(sb.toString());
			//return (sw.getBuffer().toString());
		}
		catch (Exception ex)
		{
			sk.iway.iwcm.Logger.error(ex);
		}

		return (null);
	}

   /**
    * v TB sme narazili na problem parsovania horizontalneho textu, ani aktualna verzia PDFboxu to neriesila
    * vystupom boli bloky textu typu silna sucast:
    * ssiil
    * naa
    * sssuuuu
    * ccaaaaa
    * ssttt
    * @param line
    * @return
    */
	private static String fixVerticalText(String line)
   {
      line = line.trim();
      if (line.length()>25 || line.contains(" ") || line.contains(",") || line.contains(".")) return line;
      if (line.length()<3) return "";

      char lastChar = line.charAt(0);
      StringBuilder sb = new StringBuilder();
      sb.append(lastChar);
      for (int i = 1; i<line.length(); i++)
      {
         char ch = line.charAt(i);
         if (lastChar != ch)
         {
            sb.append(ch);
            lastChar = ch;
         }
      }

      Logger.debug(PDF.class, "fixVerticalText: line="+line+" sb="+sb.toString());

      if (sb.length()<3) return "";

      return sb.toString();
   }
}
