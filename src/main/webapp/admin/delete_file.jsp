
<%@page import="sk.iway.iwcm.filebrowser.BrowseAction"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
if ("keep".equals(Constants.getInstallName())) request.setAttribute("dontCheckAdmin", "true");
%>
<%@ include file="/admin/layout_top.jsp" %>
<%
String file = Tools.getRequestParameter(request, "file");
Identity user = UsersDB.getCurrentUser(request);
if (Tools.isNotEmpty(file))
{
	try
	{
		if (BrowseAction.hasForbiddenSymbol(file)==false && (file.startsWith("/images") || file.startsWith("/files")) && user.isFolderWritable(file.substring(0, file.lastIndexOf("/"))) )
		{
			IwcmFile f = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(file));
			if (f.exists())
			{
				System.out.println("DELETING FILE: "+file);
				Adminlog.add(Adminlog.TYPE_FILE_DELETE, "Delete file "+file, -1, -1);
				f.delete();
			}
		}
	}
	catch (Exception ex)
	{

	}
}
%>
<%@ include file="/admin/layout_bottom.jsp" %>
