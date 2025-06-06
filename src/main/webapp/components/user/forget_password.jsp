<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.users.*,java.util.*"%>
<%@ page import="sk.iway.iwcm.tags.support_logic.ResponseUtils" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(sk.iway.iwcm.tags.support_logic.CustomTagUtils.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
PageParams pageParams = new PageParams(request);
//TODO: joruz - pridat to do komponenty pre vkladanie
String emailLogon = pageParams.getValue("emailLogon", "false");

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

if(request.getParameter("loginName") != null)
{	
	//posli heslo emailom
	String loginName = request.getParameter("loginName");
	boolean ret = Tools.isNotEmpty(loginName) && loginName.contains("@") ? UsersDB.sendPassword(request, null, loginName) : UsersDB.sendPassword(request, loginName, null);
}
%>
<iwcm:present name="passResultEmailFail">
	<br />
	<iwcm:text key="logon.name_email_not_exist"/>.
</iwcm:present>
<iwcm:present name="passResultEmail">
	<br />
	<%if(Constants.getBoolean("passwordUseHash")){%>
		<iwcm:text key="logon.hash.lost_password_send_success" param1='<%=(String)request.getAttribute("passResultEmail") %>'/>
	<%}else{%>
		<iwcm:text key="logon.lost_password_send_success" param1='<%=(String)request.getAttribute("passResultEmail") %>'/>
	<%}%>
	<br />
</iwcm:present>
<iwcm:notPresent name="passResultEmail">
    <iwcm:present name="error.logon.user.blocked">
        <p class='alert alert-danger'><iwcm:text key="checkform.fail_probablySpamBot"/></p>
    </iwcm:present>
</iwcm:notPresent>

<form name="passwdSendForm" method="post" action="<%=PathFilter.getOrigPath(request)%>">
	<%if(Constants.getBoolean("passwordUseHash")){%>
		<iwcm:text key="components.user.hash.forgot_password"/>.
	<%}else{%>
		<iwcm:text key="components.user.forgot_password"/>.
	<%}%>
	<br />
	<iwcm:text key="components.user.forgot_password.name"/> / <iwcm:text key="components.user.email"/>: <input class="input" type="text" name="loginName" value='<%=ResponseUtils.filter(Tools.getStringValue(request.getParameter("loginName"),""))%>'/>
	<br />
	
	<a href="javascript:document.passwdSendForm.submit();"><iwcm:text key="components.user.send_login_info"/></a>.
	
	<input type=hidden name="docid" value="<%=sk.iway.iwcm.tags.support_logic.ResponseUtils.filter(request.getParameter("docid"))%>" />
</form>