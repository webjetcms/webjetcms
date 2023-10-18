<%@page import="sk.iway.iwcm.helpers.RequestDump"%>
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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<%@ include file="/admin/layout_top.jsp" %>
<script language="JavaScript">
var helpLink = "";
</script>

request.getServerName=<%=request.getServerName() %><br/>
Tools.getServerName=<%=Tools.getServerName(request) %><br/>
request.getScheme=<%=request.getScheme() %><br/>
request.isSecure=<%=request.isSecure() %><br/>
Tools.isSecure=<%=Tools.isSecure(request) %><br/>
request.getServerPort=<%=request.getServerPort() %><br/>
request.getRemoteAddr=<%=request.getRemoteAddr() %><br/>
Tools.getRemoteIP=<%=Tools.getRemoteIP(request) %><br/>
session.getId()=<%=session.getId() %><br/>
request.getUserPrincipal()=<%=request.getUserPrincipal() %><br/>
<br/>
<br/>
<%

RequestDump rd = new RequestDump(request);
out.println( rd.completeRequestReportAsHtml() );

%>


<%@ include file="/admin/layout_bottom.jsp" %>