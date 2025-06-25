<%@page import="sk.iway.iwcm.users.SettingsAdminWebpagesTable"%>
<%@ page import="sk.iway.iwcm.Tools" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><iwcm:checkLogon admin="true" perms="menuWebpages"/><%

SettingsAdminWebpagesTable.saveProperty(session, Tools.getRequestParameter(request, "property"), Tools.getRequestParameter(request, "value"));

%>
