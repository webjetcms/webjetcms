<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%>
<%@ page language="java" pageEncoding="utf-8" import="sk.iway.iwcm.Tools,sk.iway.iwcm.system.msg.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
int messageId = Tools.getIntValue(Tools.getRequestParameter(request, "messageId"), -1);
String recipient = Tools.getRequestParameter(request, "recipient");

if(Tools.getRequestParameter(request, "text")!=null) {

		AdminMessageBean am = new AdminMessageBean();
		am.setMessageText(Tools.getRequestParameter(request, "text"));
		am.setRecipientUserId(new Integer(recipient));
		if("true".equals(Tools.getRequestParameter(request, "onlyLogged")))
			am.setOnlyForLogged(Boolean.TRUE);
		else
			am.setOnlyForLogged(Boolean.FALSE);

		MessageDB.getInstance(false).saveMessage(session,am);
		messageId = am.getAdminMessageId();
}

AdminMessageBean mes = MessageDB.getInstance(false).getMessage(messageId);
%>
<logic:present parameter="text">
	<script type="text/javascript">
		top.window.frames['topFrame'].window.frames['feederIframe'].window.location.reload();
	</script>
</logic:present>
<logic:present parameter="removeAttribute" >
	<script type="text/javascript">
		top.window.close();
	</script>
</logic:present>
<LINK rel="stylesheet" href="/admin/css/style.css">
<link rel="stylesheet" href="/components/cmp.css">
<body>
<table>
<form action="message_popup_bottom.jsp">
<input type="hidden" name="recipient" value="<%=recipient%>">
<input type="hidden" name="messageId" value="<%=messageId%>">
<tr>
	<td><textarea style="width:494px;height:85px" name="text"></textarea>
</tr>
<tr>
	<td align="right"><input type="submit" value="<iwcm:text key="admin.message_popup.send"/>"></td>
</tr>

</form>

</table>
</body>
