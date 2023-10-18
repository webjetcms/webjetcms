<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
	boolean deleted = Boolean.parseBoolean(request.getParameter("deleted") + "");
%>
<logic:present parameter="updateOK">
	<% if(deleted) { %>
		<p><center><font color="green" ><b><iwcm:text key="components.forum.deleted"/>.</b></font></center></p>
	<% } else { %>
		<p><center><font color="green" ><b><iwcm:text key="components.forum.update_ok"/>.</b></font></center></p>
	<% } %>
</logic:present>

<logic:present parameter="updateError">
	<p><center><font color="red" ><b><iwcm:text key="components.forum.update_error"/>.</b></font></center></p>
</logic:present>
