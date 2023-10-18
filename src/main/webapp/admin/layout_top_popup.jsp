<%@page import="sk.iway.iwcm.Tools"%><!DOCTYPE html>
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

    String adminJqueryVersion = (String)request.getAttribute("adminJqueryVersion");
    if (Tools.isEmpty(adminJqueryVersion)) adminJqueryVersion = "adminJqueryJs";
%>
<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />

<iwcm:combine type="css" set="adminStandardCss" />
<style>
	<jsp:include page="/admin/css/perms-css.jsp"/>

    /* Set default body */
    body {
        color: #333333;
        font-family: "Open Sans", sans-serif;
        padding: 0px !important;
        margin: 0px !important;
        font-size: 13px;
        direction: ltr;
    }
    html, body { height: 100% }
	body { background-image: none; background-color: white; }
	* {
    box-sizing: border-box;
	}
	*::before, *::after {
	    box-sizing: border-box;
	}
</style>

<iwcm:combine type="js" set="<%=adminJqueryVersion%>" />
<iwcm:combine type="js" set="adminStandardJs" />
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
<%
//nafejkujeme ze mame jquery aj ui
Tools.insertJQueryUI(pageContext, "autocomplete");
//fajkujeme ajax_support.js pre autocomplete tag
request.setAttribute("ajaxSupportInserted", true);
%>
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>

</head>

<body bgcolor="#ffffff" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
