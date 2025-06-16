<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%@ page contentType="text/html" import="java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>

<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@page import="sk.iway.iwcm.io.IwcmInputStream"%>
<%@page import="java.io.IOException"%>
<%@page import="sk.iway.iwcm.io.IwcmOutputStream"%>
<%@page import="java.io.File"%>
<%@ page import="sk.iway.iwcm.system.zip.ZipOutputStream" %>
<%@ page import="sk.iway.iwcm.system.zip.ZipEntry" %>
<%@ page import="sk.iway.iwcm.Constants" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<iwcm:checkLogon admin="true"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/><%!

private void getFilesFromDir(String rootDir, ServletContext servletContext, ZipOutputStream zipOut, JspWriter out, boolean saveFullPath) throws IOException
{
	System.out.println("Ziskavam adresar: " + rootDir + "<br>");
	out.flush();
   IwcmFile file;
   if (rootDir.indexOf(":")==-1)
   {
      file = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(rootDir));
   }
   else
   {
      file = new IwcmFile(rootDir);
   }
	int i;
	byte[] buf = new byte[64000];
	IwcmInputStream in;
	int len;

	System.out.println("Testing file: "+file.getAbsolutePath()+" isDir="+file.isDirectory());

	String rootDir2 = rootDir.replace('/', File.separatorChar);
   if (file.isDirectory())
   {
   	IwcmFile[] files = file.listFiles();
	if(files!=null) {
   		int size = files.length;
		for (i=0; i<size; i++)
		{
			if (files[i].isDirectory())
			{
				System.out.println("Adding DIRECTORY: "+file.getName()+"<br>");
				out.println("Adding DIRECTORY: "+Tools.escapeHtml(file.getName())+"<br>");
			   	getFilesFromDir(rootDir+"/"+files[i].getName(), servletContext, zipOut, out,saveFullPath);
			}
			else
			{
				try
				{
					System.out.println("Adding file: "+files[i].getName()+"<br>");
				   out.println("Adding file: "+Tools.escapeHtml(files[i].getName())+"<br>");

				   in = new IwcmInputStream(files[i].getAbsolutePath());
					//Add ZIP entry to output stream.

					ZipEntry entry;


					if (saveFullPath)
					{
						String entryPath = rootDir2 + java.io.File.separatorChar + files[i].getName();
						if (entryPath.indexOf(File.separatorChar)==0) entryPath = entryPath.substring(1);
						System.out.println("Adding entry 1: "+entryPath);
						entry = new ZipEntry(entryPath);
					}
					else
					{
						String entryPath = rootDir2.substring(rootDir2.lastIndexOf(File.separator)+1, rootDir2.length()) + java.io.File.separatorChar + files[i].getName();
						System.out.println("Adding entry 2: "+entryPath);
						entry = new ZipEntry(entryPath);
					}

					entry.setTime(files[i].lastModified());
					entry.setSize(files[i].length());
					zipOut.putNextEntry(entry);

	            // Transfer bytes from the file to the ZIP file
	            while ((len = in.read(buf)) > 0)
	            {
	            	zipOut.write(buf, 0, len);
	            }
				}
				catch (Exception ex)
				{
					sk.iway.iwcm.Logger.error(ex);
				}
			}
		}
	}
   }
   else
   {
   	out.println("Adding file: "+Tools.escapeHtml(file.getName())+"<br>");

	   in = new IwcmInputStream(file.getAbsolutePath());
		//Add ZIP entry to output stream.
		String entryPath = rootDir2 + java.io.File.separatorChar + file.getName();
		if (entryPath.indexOf(File.separatorChar)==0) entryPath = entryPath.substring(1);

		ZipEntry entry = new ZipEntry(entryPath);
		entry.setTime(file.lastModified());
		zipOut.putNextEntry(entry);

      // Transfer bytes from the file to the ZIP file
      while ((len = in.read(buf)) > 0)
      {
      	zipOut.write(buf, 0, len);
      }
   }
}
%><%
if (Tools.getRequestParameter(request, "dir")!=null && Tools.getRequestParameter(request, "file")!=null)
{
	String dir = Tools.getRequestParameter(request, "dir");
	if (dir.endsWith("/"))
	{
		dir =  dir.substring(0,dir.length()-1);
	}

	System.out.println("zip-dir.jsp: dir="+dir+" file="+Tools.getRequestParameter(request, "file"));

	ZipOutputStream zout = new ZipOutputStream(new IwcmOutputStream(sk.iway.iwcm.Tools.getRealPath(Tools.getRequestParameter(request, "file"))));
	if (Tools.getRequestParameter(request, "dontSaveFullPath")!=null)
	{
   		getFilesFromDir(dir, Constants.getServletContext(), zout, out,false);
	}
	else
	{
		getFilesFromDir(dir, Constants.getServletContext(), zout, out,true);
	}
   zout.close();

   System.out.println("<hr>Done. <a href='"+Tools.getRequestParameter(request, "file")+"'>"+Tools.getRequestParameter(request, "file")+"</a>");
	out.println("<hr>Done. <a href='"+Tools.getRequestParameter(request, "file")+"'>"+Tools.getRequestParameter(request, "file")+"</a>");
}%>
