<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%
String domain = Tools.getParameter(request, "domain");
if(Tools.isNotEmpty(domain)) session.setAttribute("preview.editorDomainName", domain);
%>