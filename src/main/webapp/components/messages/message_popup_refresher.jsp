<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" language="java" import="java.net.*,java.util.*,sk.iway.iwcm.system.msg.*,sk.iway.iwcm.users.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%
Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
int messageId = Tools.getIntValue(session.getAttribute("messageId").toString(), -1);
List<AdminMessageBean> messages = null;
if("true".equals(session.getAttribute("equal"))) {
	messages = (List<AdminMessageBean>)MessageDB.getInstance(false).getMessages(session, messageId, true);
	session.removeAttribute("equal");
}
else
	messages = (List<AdminMessageBean>)MessageDB.getInstance(false).getMessages(session, messageId, false);
if (messages != null)
{
	request.setAttribute("messages", messages);
}
String recipient = Tools.getRequestParameter(request, "recipient");
UserDetails ud=UsersDB.getUser(Integer.parseInt(recipient));

String text = Tools.getStringValue(Tools.getRequestParameter(request, "text"),"");
%>
<iwcm:iterate name="messages" id="msg">
<%
ud=UsersDB.getUser(((AdminMessageBean)msg).getCreateByUserId().intValue());
if(ud.getUserId()!=user.getUserId())
	text += "<b style=\"color:red\">";
else
	text += "<b>";
text+=ud.getFullName()+" ("+Tools.formatTime(((AdminMessageBean)msg).getCreateDate())+"):</b><br>"+
		((AdminMessageBean)msg).getMessageText()+"<br>";
messageId=((AdminMessageBean)msg).getAdminMessageId();
%>
</iwcm:iterate>
<%
session.setAttribute("messageId",new Integer(messageId));
//String url = URLEncoder.encode(text,"windows-1250");
String content = "5;url=/components/messages/message_popup_refresher.jsp?recipient="+recipient;
%>


<body>
<script type="text/javascript">

	setTimeout('location.reload(true)',5000);
	window.parent.document.getElementById("messages").innerHTML += "<%=text.replaceAll("\r","<br>").replaceAll("\n","<br>").replaceAll("\"","\\\\\"")%>";
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
	window.parent.scrollBy(0,1000);
</script>
</body>
