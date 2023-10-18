<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page language="java" pageEncoding="utf-8" import="java.net.*,java.util.*,sk.iway.iwcm.system.msg.*,sk.iway.iwcm.users.*,sk.iway.iwcm.Tools" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%

String recipient = Tools.getRequestParameter(request, "recipient");
UserDetails ud=UsersDB.getUser(Integer.parseInt(recipient));
%>
<html>
	<head>
		<LINK rel="stylesheet" href="/admin/css/style.css">
		<link rel="stylesheet" href="/components/cmp.css">
	</head>
	<body style="margin:0">
		<div style="margin-top:5px;">
			<b><iwcm:text key="admin.message_popup.recipient"/>:</b> <%=ud.getFullName()%>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
			<b><iwcm:text key="admin.message_popup.email"/>:</b> <a href="mailto:<%=ud.getEmail()%>" style="color:black;font-weight:normal"><%=ud.getEmail()%></a>
		</div>
	</body>
</html>
