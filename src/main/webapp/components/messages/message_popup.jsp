<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.system.msg.*" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.system.msg.AdminMessageBean" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<%
	if(Tools.getRequestParameter(request, "remove")!=null)
	{
		int mesId = Tools.getIntValue(Tools.getRequestParameter(request, "remove"),-1);
		System.out.println("============ REMOVE mesId="+mesId+" ====================");
		if(mesId>0)
		{
			session.removeAttribute("recipient"+mesId);
			return;
		}
	}
%>
<%
int messageId = Tools.getIntValue(Tools.getRequestParameter(request, "messageId"), -1);
if(messageId>0)
{
	AdminMessageBean mes = MessageDB.getInstance(false).getMessage(messageId);
	System.out.println(mes);

	session.setAttribute("recipient"+mes.getCreateByUserId(),"");

	String recipient = String.valueOf(mes.getCreateByUserId());
	session.setAttribute("equal","true");
%>

<script type="text/javascript">
function removeMessage()
{
	$.get("/components/messages/message_popup.jsp?remove=<%=recipient%>");
}
</script>


<frameset FRAMESPACING="0" FRAMEBORDER="0" rows="7,63,30" onunload="removeMessage();">
	<frame src="/components/messages/message_popup_top_most.jsp?recipient=<%=recipient%>" frameborder="0" noresize="true" marginheight="0" marginwidth="0" >
	<frame name="topFrame" src="/components/messages/message_popup_top.jsp?messageId=<%=messageId%>&recipient=<%=recipient%>" frameborder="0" noresize="true" marginheight="0" marginwidth="0" >
	<frame src="/components/messages/message_popup_bottom.jsp?messageId=<%=messageId%>&recipient=<%=recipient%>" frameborder="0" noresize="true" marginheight="0" marginwidth="0">
</frameset>

<%}%>
