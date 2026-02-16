package sk.iway.iwcm;


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;

import sk.iway.iwcm.common.PdfTools;
import sk.iway.iwcm.io.IwcmFile;

/**
 * <p>Description: Vytvori pdfko z HTML kodu, pouzivajuc kniznicu pd4ml.
 * 	Na nastavenie Truetype fontov nastavit konstantu pdfFontDirectory. Defaultne sa berie C:/Windows/Fonts</p>
 * <p>Copyright: Interway s.r.o. (www.interway.sk) 2008</p>
 * <p>Company: Interway s.r.o. (www.interway.sk)</p>
 * @author murbanec
 * @version 1.0
 */
public class HtmlToPdfAction
{

   @Deprecated
	public static void renderHtmlCode(String data, String path) throws IOException
   {
   	PdfTools.renderHtmlCode(data, new FileOutputStream(path), null);
   	Logger.println(HtmlToPdfAction.class,"Exporting pdf to: "+new IwcmFile(path).getAbsolutePath() );


   }

   	@Deprecated
	public static void renderHtmlCode(String data, OutputStream output, HttpServletRequest request) throws IOException
	{
		renderHtmlCode(data, output, request, null);
	}

	@Deprecated
	public static void renderHtmlCode(String data, OutputStream output, HttpServletRequest request, Pd4mlOptions options) throws IOException
	{
		renderHtmlCode(data, output, request, options, true);
	}

	@Deprecated
	//ing formulare sa generuju 2x zasebou v case mensom ako 1s
	public static void renderHtmlCode(String data, OutputStream output, HttpServletRequest request, Pd4mlOptions options, boolean useSpamProtection) throws IOException
	{
		PdfTools.renderHtmlCode(data, output, request, options, useSpamProtection);
	}

	@Deprecated
	public static boolean getPdfVersion(int docId, HttpServletRequest request, OutputStream output){
   		return PdfTools.getPdfVersion(docId,request,output);
	}


}