<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %><%@page import="sk.iway.iwcm.Tools"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%

String afterLogonRedirect = (String)request.getAttribute("afterLogonRedirect");
String docRedirect = "/";
//aby sa zmenilo URL a subor sa nacital znova zo servera
String afterLogonRedirectRefreshed = afterLogonRedirect;
if (afterLogonRedirect != null) afterLogonRedirectRefreshed = Tools.addParameterToUrl(afterLogonRedirect, "rnd", String.valueOf(Tools.getNow()));

try
{
   //skus ziskaj posledne docid
   int last_doc_id = ((Integer) request.getSession().getAttribute("last_doc_id")).intValue();
   docRedirect = "/showdoc.do?docid="+last_doc_id;
}
catch (Exception ex)
{

}

if (afterLogonRedirect==null || afterLogonRedirect.equals("/"))
{
   response.sendRedirect(docRedirect);
}

%>

<html>
	<head>
		<meta http-equiv='refresh' content='1; url=<%=afterLogonRedirectRefreshed%>'>
	</head>
	<body>
	   <iwcm:text key="components.urlredirects.downloadWillStartShortly"/> <a href="<%=afterLogonRedirectRefreshed%>"><%=afterLogonRedirect%></a>.
	   <br><br>
	   <a href="<%=docRedirect%>"><iwcm:text key="gallery.back"/></a>	
	</body>
</html>