<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" 
import="sk.iway.iwcm.*,
sk.iway.iwcm.stat.*,
sk.iway.iwcm.users.*" %>
<%@ page import="sk.iway.iwcm.tags.WriteTag" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>

<%
//otestuj ci existuje nahrada za tuto stranku
String forward = WriteTag.getCustomPageAdmin("/admin/logon.jsp", request);
if (forward!=null)
{
	pageContext.forward(forward);
	return;
}
%>