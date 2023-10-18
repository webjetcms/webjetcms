<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page language="java" pageEncoding="utf-8" import="sk.iway.iwcm.Tools" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
int messageId = Tools.getIntValue(Tools.getRequestParameter(request, "messageId"), -1);
session.setAttribute("messageId",new Integer(messageId));
String recipient = Tools.getRequestParameter(request, "recipient");
%>
<html>
	<head>
		<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
		<LINK rel="stylesheet" href="/admin/css/style.css">
		<link rel="stylesheet" href="/components/cmp.css">
	</head>

	<body style="background-color:white;margin:0">

		<span id="messages"></span>

		<iframe name="feederIframe" id="feederIframe" width="1" height="1" src="message_popup_refresher.jsp?recipient=<%=recipient%>" style="display:none;"></iframe>

		<script type="text/javascript">
			$(document).ready(function()
			{
				$(window).unload(function()
				{
					$.get("/components/messages/message_popup.jsp?remove=<%=recipient%>");
				});
			});
		</script>

	</body>

</html>
