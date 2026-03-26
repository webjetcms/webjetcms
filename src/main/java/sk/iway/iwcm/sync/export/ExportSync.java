package sk.iway.iwcm.sync.export;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;

/**
 *  ExportSync.java  servuje zip archiv exportu z groupId, vyzaduje admin credentials
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2013
 *@author       $Author: jeeff mhalas $
 *@version      $Revision: 1.3 $
 *@created      Date: 14.5.2013 14:12:57
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ExportSync extends HttpServlet
{

	/**
	 * Comment for <code>serialVersionUID</code>
	 */
	private static final long serialVersionUID = -5912813630916961279L;
	
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		String path = Tools.URLDecode(request.getPathInfo());
		getExportZip(path, request, response);
	}

	/**
	 * Na zaklade gorupId a admin logon v parametroch spravi Export a do response zapise zip file s obsahom exportu
	 * na export sa pouziva {@link ExportManager}
	 * @param path
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public static void getExportZip(String path, HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		int groupId = Tools.getIntValue(request.getParameter("remoteGroupId"), -1);
		Logger.debug(ExportSync.class, "Exporting data for synchronization, path: " + path + ", groupId="+groupId);
		ExportManager export = ExportManager.create(request, request.getSession());
		try
		{
			export.exportGroup(groupId);
			String zipFilePath =(String)request.getAttribute("zipfile");
			Logger.debug(ExportSync.class, "Zip file path: "+zipFilePath);
			zipFilePath =Tools.getRealPath(zipFilePath);
			if(Tools.isEmpty(zipFilePath))
			{
				sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
				response.setStatus(404);
				response.getWriter().print("<html><body>404 - not found</body></html>");
				return;
			}
			File file = new File(zipFilePath);
	      ServletOutputStream outStream = response.getOutputStream();
	      ServletContext context  = Constants.getServletContext();
	      String mimetype = context.getMimeType(zipFilePath);
	      if (mimetype == null) {
	          mimetype = "application/octet-stream";
	      }
	      response.setContentType(mimetype);
	      response.setContentLength((int)file.length());
	      String fileName = (new File(zipFilePath)).getName();
	      response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
	      DataInputStream in = new DataInputStream(new FileInputStream(file));
	      IOUtils.copy(in, outStream);
	      outStream.flush();
	      //TODO: delete file?
	      in.close();
	      //outStream.close();
		}
      catch (IOException e)
		{
			Logger.debug(ExportSync.class, "Failed to export... cause: " + e.getMessage());
      	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
			response.setStatus(404);
			response.getWriter().print("<html><body>404 - not found</body></html>");
			return;
		}
		
	}
}
