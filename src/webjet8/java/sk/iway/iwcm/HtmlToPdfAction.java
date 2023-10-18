package sk.iway.iwcm;


import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import sk.iway.iwcm.common.PdfTools;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;

/** 
 * <p>Description: Vytvori pdfko z HTML kodu, pouzivajuc kniznicu pd4ml. 
 * 	Na nastavenie Truetype fontov nastavit konstantu pdfFontDirectory. Defaultne sa berie C:/Windows/Fonts</p>
 * <p>Copyright: Interway s.r.o. (www.interway.sk) 2008</p>
 * <p>Company: Interway s.r.o. (www.interway.sk)</p>
 * @author murbanec
 * @version 1.0
 */
public class HtmlToPdfAction extends Action
{
   @Override
	public ActionForward execute(ActionMapping mapping,
         ActionForm form,
         HttpServletRequest request,
         HttpServletResponse response)
          throws IOException, ServletException
   {
   	long timeBeforeGenerating = System.currentTimeMillis();
      HttpSession session = request.getSession();

      if (session == null)
      {
         return (mapping.findForward("logon"));
      }      
      
      int docId = Tools.getDocId(request);
      
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
		boolean ok = PdfTools.getPdfVersion(docId, request, baos);
		
		if (ok)
		{
			response.setContentType("application/pdf");
         
         //aby to islo aj v IE6: http://www.alagad.com/go/blog-entry/error-internet-explorer-cannot-download-filename-from-webserver
			response.setHeader("Pragma", "public");
			response.setHeader("Cache-Control", "max-age=0");
   		
			response.setContentLength(baos.size());
			response.getOutputStream().write(baos.toByteArray());
			response.getOutputStream().flush();
			response.getOutputStream().close();
		}
		else
		{
			response.getOutputStream().println(Prop.getInstance(request).getText("components.pdf.unable_to_generate"));
		}
      
		Logger.println(this,"Html "+PathFilter.getOrigPath(request)+"?"+request.getAttribute("javax.servlet.forward.query_string")+
					" exported to pdf in "+(System.currentTimeMillis() - timeBeforeGenerating)+" ms");
      return null;
   }

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