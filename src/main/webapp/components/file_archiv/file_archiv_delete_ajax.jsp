<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit"%><%@
page import="sk.iway.iwcm.users.UsersDB"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%
int fabId = Tools.getIntValue(Tools.getRequestParameter(request, "fab_id"),-1);
if(fabId > 0)
{
	out.print(FileArchivatorKit.deleteFile(fabId,UsersDB.getCurrentUser(request)));
	return;
}
%>false
