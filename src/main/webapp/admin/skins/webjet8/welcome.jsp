<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@ 
page pageEncoding="utf-8" import="sk.iway.iwcm.components.welcome.*" %><%@ 
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/><%
response.sendRedirect("/admin/");
%>