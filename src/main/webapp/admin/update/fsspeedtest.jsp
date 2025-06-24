<%@page import="java.io.File"%>
<%@page import="sk.iway.iwcm.doc.DebugTimer"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>
<h1>File system speed test, call with URL parameter ?act=fix</h1>
<p><a href="?act=fix">RUN IT</a></p>
<%
if ("fix".equals(request.getParameter("act"))) {
	DebugTimer dt = new DebugTimer("fsspeedtest.jsp");


	{
		out.println("Testing mime speed, start="+dt.getLastDiff()+" ms<br/>");

		IwcmFile mimeDir = new IwcmFile(Tools.getRealPath("/components/_common/mime/"));

		out.println("has base file object, fullPath="+mimeDir.getAbsolutePath()+" diff="+dt.getLastDiff()+" ms<br/>");

		IwcmFile mimeDirFiles[] = mimeDir.listFiles();

		out.println("listFiles, size="+mimeDirFiles.length+", diff="+dt.getLastDiff()+" ms<br/>");

		for (IwcmFile file : mimeDirFiles)
		{
			long changed = file.lastModified();
			long size = file.length();
			boolean isFile = file.isFile();
			boolean exists = file.exists();
		}

		out.println("listing done, diff="+dt.getLastDiff()+" ms<br/>");
	}

	{
		out.println("<br/><br/>Testing modinfo speed, start="+dt.getLastDiff()+" ms<br/>");

		File components[] = (new File(Tools.getRealPath("/components"))).listFiles();

		out.println("modinfo list, size="+components.length+", diff="+dt.getLastDiff()+" ms<br/>");

		for (File f : components)
		{
			if (f.isDirectory())
			{
				for (int j=0; j<30; j++)
				{
					String fileName = "modinfo.properties";
					if (j>0) fileName = "modinfo"+j+".properties";

					File file = new File(Tools.getRealPath("/components/"+f.getName()+"/"+fileName));
					if (file.exists())
					{
						long changed = file.lastModified();
						long size = file.length();
						boolean isFile = file.isFile();
						boolean exists = file.exists();
					}
				}
			}
		}

		out.println("modinfo listing done, diff="+dt.getLastDiff()+" ms<br/>");
	}

	out.println("Total time="+dt.getDiff()+"ms <br/>");
}

%>


<%@ include file="/admin/layout_bottom.jsp" %>