<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.InitServlet"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.system.msg.*,java.util.*" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%
   if (session!=null)
   {
      session.setAttribute("refreshed", "true");
   }
   out.println(new java.util.Date());

   if (InitServlet.isTypeCloud()==false)
   {
	   List newMessages = MessageDB.getInstance(false).getUnreadedMessages(session);
	   if (newMessages!=null && newMessages.size()>0)
	   {
	   	request.setAttribute("newMessages", newMessages);
	   }
   }
%>

<iwcm:present name="newMessages">
	<%="<script type='text/javascript' language='JavaScript'>"%>
	function popupMessage(id)
	{
		var options = "toolbar=no,scrollbars=no,resizable=yes,width=500,height=400;"
		window.open("/components/messages/message_popup.jsp?messageId="+id,"msgpop"+id,options);
	}
	<iwcm:iterate id="msg" name="newMessages" type="sk.iway.iwcm.system.msg.AdminMessageBean">
		popupMessage(<iwcm:beanWrite name="msg" property="adminMessageId"/>);
	</iwcm:iterate>
	<%="</script>"%>
</iwcm:present>
&nbsp;