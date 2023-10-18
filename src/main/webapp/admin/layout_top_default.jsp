<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@page import="sk.iway.iwcm.tags.WriteTag"%><%@ 
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/>
<html>
<head>
<title>:: Web JET admin ::</title>
<%
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");
	
	//String lng = PageLng.getUserLng(request);
	//pageContext.setAttribute("lng", lng);
%>
<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />

<script type="text/javascript">
if (window.name && window.name=="componentIframe")
{
	document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/components/iframe.css' />");
}
else
{
	document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/admin/css/style.css' />");
}

var helpLink = "";
</script>
<link rel='stylesheet' href='<%=request.getContextPath()%>/admin/css/webjet5.css' />
<style>
	<jsp:include page="/admin/css/perms-css.jsp"/>
</style>

<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></SCRIPT>

</head>

<body bgcolor="#ffffff">

<%
if (request.getAttribute("layout_closeMainTable")==null)
{
	out.println("<table border='0' cellpadding='0' cellspacing='0' width='100%' height='100%'>");
	out.println("<tr>");
	out.println("<td align='left' valign='top' width='100%' height='99%' class='mainarea'>");
}
%>
