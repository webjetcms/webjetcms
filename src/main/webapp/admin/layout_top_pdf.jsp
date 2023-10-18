<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page  %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true"/>

<html>
<head>
<title>:: Web JET admin ::</title>
<%
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");
	
	request.setAttribute("pdfHeaderHtml", "<img src='/admin/skins/webjet6/images/wjlogo.gif'>");
	request.setAttribute("pdfHeaderHeight", "30");
	request.setAttribute("pdfFooterHtml", "<table border='0' width='100%'><tr><td align='right'>${page} / ${total}</td></tr></table>");
	request.setAttribute("pdfFooterHeight", "-1");
%>
<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">

	<LINK rel="stylesheet" href="<%=request.getContextPath()%>/admin/css/style.css" media="screen">
	<LINK rel="stylesheet" href="<%=request.getContextPath()%>/admin/css/style.css" media="print">
	<LINK rel="stylesheet" href="<%=request.getContextPath()%>/admin/css/webjet5.css" media="screen">
	<LINK rel="stylesheet" href="<%=request.getContextPath()%>/admin/css/webjet5.css" media="print">
	<LINK rel="stylesheet" href="<%=request.getContextPath()%>/admin/css/print.css" media="screen">
	<LINK rel="stylesheet" href="<%=request.getContextPath()%>/admin/css/print.css" media="print">
	
	<style>
		<jsp:include page="/admin/css/perms-css.jsp"/>
	</style>

	<script language="JavaScript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
	<script LANGUAGE="JavaScript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>

</head>

<body bgcolor="#ffffff" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
