<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
page import="java.util.List"%><%@
page import="sk.iway.iwcm.system.msg.MessageDB"%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);

if (user == null) return;

if (session!=null)
{
   session.setAttribute("refreshed", "true");
}

List newMessages = MessageDB.getInstance(false).getUnreadedMessages(user.getUserId());
if (newMessages!=null && newMessages.size()>0)
{
	System.out.println("============= MAM MESSAGES, size="+newMessages.size()+", uid="+user.getUserId()+", rnd="+Tools.getRequestParameter(request, "rnd")+" ==========================");
	request.setAttribute("newMessages", newMessages);
}
out.println(Tools.getRequestParameter(request, "rnd"));
%><iwcm:present name="newMessages">
   <%="<script type=\"text/javascript\">"%>
	<iwcm:iterate id="msg" name="newMessages" type="sk.iway.iwcm.system.msg.AdminMessageBean">
		<%
		//zamedzenie duplicitneho otvarania okien
		if(session.getAttribute("recipient"+msg.getCreateByUserId())==null && request.getAttribute("recipient"+msg.getCreateByUserId())==null)
		{
			request.setAttribute("recipient"+msg.getCreateByUserId(), "");
		%>
			popupMessage(<iwcm:strutsWrite name="msg" property="adminMessageId"/>);
		<%}%>
	</iwcm:iterate>
	<%="</script>"%>
</iwcm:present>
