<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.users.*,java.util.*"%>
<%@ page import="org.apache.struts.util.ResponseUtils" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
if (Constants.getBoolean("editorEnableXHTML")) pageContext.setAttribute(org.apache.struts.Globals.XHTML_KEY, "true", PageContext.PAGE_SCOPE);
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
<logic:present name="passResultEmailFail">
	<br />
	<iwcm:text key="logon.name_email_not_exist"/>.
</logic:present>
<logic:present name="passResultEmail">
	<br />
	<%if(Constants.getBoolean("passwordUseHash")){%>
		<iwcm:text key="logon.hash.lost_password_send_success" param1='<%=(String)request.getAttribute("passResultEmail") %>'/>
	<%}else{%>
		<iwcm:text key="logon.lost_password_send_success" param1='<%=(String)request.getAttribute("passResultEmail") %>'/>
	<%}%>
	<br />
</logic:present>
<logic:notPresent name="passResultEmail">
    <logic:present name="error.logon.user.blocked">
        <p class='alert alert-danger'><iwcm:text key="checkform.fail_probablySpamBot"/></p>
    </logic:present>
</logic:notPresent>

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
	
	<input type=hidden name="docid" value="<%=org.apache.struts.util.ResponseUtils.filter(request.getParameter("docid"))%>" />
</form>