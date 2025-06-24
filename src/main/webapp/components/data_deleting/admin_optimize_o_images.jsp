<%@page import="sk.iway.iwcm.gallery.GalleryDB"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.File"%>
<%@page import="sk.iway.iwcm.tags.support.ResponseUtils"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/admin/layout_top.jsp" %>

<%!

public static long resizeOImagesInDir(String path, int maxWidth, int maxHeight, JspWriter out) throws IOException
{
	if (path.endsWith("/")==false) path = path + "/";
	out.println("<strong>Resizing dir: " + Tools.escapeHtml(path) + "</strong><br/>");

	File dir = new File(sk.iway.iwcm.Tools.getRealPath(path));
	if (dir.exists()==false || dir.isDirectory()==false) return 0;

	long savedSize = 0;
	long sacedSizeSubdirs = 0;

	File files[] = dir.listFiles();
	for (File f : files)
	{
		if (f.isDirectory()) sacedSizeSubdirs+= resizeOImagesInDir(path+f.getName()+"/", maxWidth, maxHeight, out);
	}
	for (File f : files)
	{

		if (f.isDirectory()) continue;
		if (f.getName().startsWith("o_")==false) continue;

		String realPath = sk.iway.iwcm.Tools.getRealPath(path + f.getName());
		String realPathSmall = sk.iway.iwcm.Tools.getRealPath(path+"m_"+f.getName());

		int ret = GalleryDB.resizePicture(realPath, realPathSmall, maxWidth, maxHeight);
		out.println("   resize: " + Tools.escapeHtml(path+f.getName()) + " ret="+ret);

		File origFile = new File(realPath);
		File smallFile = new File(realPathSmall);
		if (smallFile.exists())
		{
			if (smallFile.length()<origFile.length())
			{
				savedSize += (origFile.length() - smallFile.length());

				long lastModified = origFile.lastModified();
				//skopiruj to nazad na o_obrazok
				GalleryDB.copyFile(realPathSmall, realPath);

				origFile = new File(realPath);
				origFile.setLastModified(lastModified);
			}
			else
			{
				out.println(" FILE BIGGER, SKIPPING");
			}

			boolean deleted = smallFile.delete();
			out.println("   delete: " + Tools.escapeHtml(path+"m_"+f.getName()) + " deleted="+deleted+"<br/>");
		}
		else
		{
			out.println("   ERROR: small file doesn't exists<br/>");
		}
		out.flush();
	}
	out.println("<br/>Saved: "+savedSize+" bytes, "+Tools.formatFileSize(savedSize)+"<br/>");
	return savedSize+sacedSizeSubdirs;
}

%>

<script language="JavaScript">
var helpLink = "";
</script>
<%
String dir = Tools.getRequestParameter(request, "dir");
int width = Tools.getIntValue(Tools.getRequestParameter(request, "w"), 1600);
int height = Tools.getIntValue(Tools.getRequestParameter(request, "h"), 1600);

if (Tools.isNotEmpty(dir) && dir.startsWith("/images/"))
{
	long savedSize = resizeOImagesInDir(dir, width, height, out);
	out.println("<br/>Saved TOTAL: "+savedSize+" bytes, "+Tools.formatFileSize(savedSize)+"<br/>");
}

if (Tools.isEmpty(dir)) dir = "/images/gallery/";
%>
<h1>Optimize o_ images in gallery</h1>

<form action="admin_optimize_o_images.jsp" method="post">
	Dir: <input type="text" name="dir" value="<%=ResponseUtils.filter(dir) %>"/>
	<br/>
	Max width: <input type="text" name="w" value="<%=width%>"/>
	<br/>
	Max height: <input type="text" name="h" value="<%=height%>"/>
	<br/>
	<input type="submit" name="bSubmit" value="Optimize"/>
</form>


<%@ include file="/admin/layout_bottom.jsp" %>
