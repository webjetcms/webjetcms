<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon /><%@page import="sk.iway.iwcm.tags.WriteTag"%><%
//otestuj ci existuje nahrada za tuto stranku
String forwardbottom = WriteTag.getCustomPageAdmin("/admin/layout_bottom.jsp", request);
if (forwardbottom!=null)
{
	pageContext.include(forwardbottom);
}
else
{
	pageContext.include("/admin/layout_bottom_default.jsp");
} 
%>