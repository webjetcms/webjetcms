<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:notPresent name="dontCheckAdmin"><iwcm:checkLogon admin="true"/></iwcm:notPresent><iwcm:present name="dontCheckAdmin"><iwcm:checkLogon /></iwcm:present><%
if ("true".equals(Tools.getRequestParameter(request, SetCharacterEncodingFilter.PDF_PRINT_PARAM)))
{
	pageContext.include("/admin/layout_top_pdf.jsp");
	return;
}
%><%@page import="sk.iway.iwcm.SetCharacterEncodingFilter"%>
<%@ page import="sk.iway.iwcm.Tools" %>
<%
if(request.getAttribute("viewDoctype") == null){ %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<% }else{ %>
<html>
<% } %>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />

<title>:: Web JET admin ::</title>
<%
	response.setHeader("Pragma","No-Cache");
	response.setDateHeader("Expires",0);
	response.setHeader("Cache-Control","no-Cache");
%>

<meta http-equiv="Content-Type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>"/>

<script type="text/javascript">
if (window.name && window.name=="componentIframe")
{
	document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/components/iframe.css'>");
}
else
{
	document.write("<link rel='stylesheet' href='<%=request.getContextPath()%>/admin/css/style.css'>");
}

if (window.name && window.name=="componentIframeWindowTab")
{
	document.write("<style type='text/css'>div.box_tab { background-position: -5px !important; } div.box_toggle { background-position: -5px bottom!important; }  </style>");
}

var helpLink = "";
</script>

<% if(request.getAttribute("setOverflow") != null){ %>
<!--[if IE]>
	<style type="text/css">
		body { overflow: auto;}
	</style>
<![endif]-->
<% } %>

<!--[if IE]>
	<style type="text/css">
		html { overflow: auto;}
	</style>
<![endif]-->

<link rel='stylesheet' href='<%=request.getContextPath()%>/admin/skins/webjet6/css/webjet6.css' />
<link rel="stylesheet" type="text/css" href="/admin/css/datepicker.css" />

<!--[if lt IE 7]>
    <link rel='stylesheet' href='<%=request.getContextPath()%>/admin/skins/webjet6/css/webjet6_ie6.css' />
<![endif]-->

<style>
	<jsp:include page="/admin/css/perms-css.jsp"/>
</style>

<script type='text/javascript' src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
<script type='text/javascript' src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>
<script type='text/javascript' src='/components/calendar/popcalendar.jsp'></script><%request.setAttribute("sk.iway.iwcm.tags.CalendarTag.isJsIncluded", "true");%>

</head>
<body id="mainframeBody">
