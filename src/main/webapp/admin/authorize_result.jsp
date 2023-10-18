<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<% 
//otestuj ci existuje nahrada za tuto stranku
String forward = "/admin/authorize_result-"+Constants.getInstallName()+".jsp";
java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
if (fForward.exists())
{
	pageContext.forward(forward);
	return;
}
%>


<%@ include file="layout_top.jsp" %>

<h3><iwcm:text key="authorize_user.title"/>:</h3>

<logic:present name="emailSendFail">
	<b><iwcm:text key="authorize_user.email_send_fail"/>
</logic:present>

<logic:notPresent name="emailSendFail">
	<b><iwcm:text key="authorize_user.authorize_od"/>:</b><br><br>
	<hr>
	<iwcm:text key="authorize_user.sender"/>: <iway:request name="from"/><br>
	<iwcm:text key="authorize_user.for"/>: <iway:request name="to"/><br>
	<iwcm:text key="authorize_user.subject"/>: <iway:request name="subject"/><br>
	<hr>
	<iway:request name="body"/>
</logic:notPresent>


<%@ include file="layout_bottom.jsp" %>