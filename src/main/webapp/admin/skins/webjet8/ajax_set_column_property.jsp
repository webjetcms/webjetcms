<%@page import="sk.iway.iwcm.users.SettingsAdminWebpagesTable"%>
<%@ page import="sk.iway.iwcm.Tools" %>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"
%><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"
%><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"
%><iwcm:checkLogon admin="true" perms="menuWebpages"/><%

SettingsAdminWebpagesTable.saveProperty(session, Tools.getRequestParameter(request, "property"), Tools.getRequestParameter(request, "value"));

%>
