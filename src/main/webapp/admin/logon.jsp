<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" 
import="sk.iway.iwcm.*,
sk.iway.iwcm.stat.*,
sk.iway.iwcm.users.*" %>
<%@ page import="sk.iway.iwcm.tags.WriteTag" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/datetime.tld" prefix="dt" %>
<%
//otestuj ci existuje nahrada za tuto stranku
String forward = WriteTag.getCustomPageAdmin("/admin/logon.jsp", request);
if (forward!=null)
{
	pageContext.forward(forward);
	return;
}
%>