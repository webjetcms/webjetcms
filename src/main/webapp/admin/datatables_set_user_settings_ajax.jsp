<%@page import="sk.iway.iwcm.users.SettingsAdminDB"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminBean"%>
<%@page import="java.util.Map"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@ 
page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%
Identity currUser = UsersDB.getCurrentUser(request);

if(currUser != null) {
	String key = Tools.getParameter(request, "key");
	String value = Tools.getParameter(request, "value");
	
	SettingsAdminBean setting = new SettingsAdminBean(currUser.getUserId(), key, value);

	if("true".equals(value)) {
		SettingsAdminDB.deleteSetting(setting);
	} else if("false".equals(value)) {
		SettingsAdminDB.setSetting(setting);
	}
}
%>