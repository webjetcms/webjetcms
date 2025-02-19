<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.users.*,sk.iway.iwcm.i18n.Prop,java.util.*"%><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true"/><jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/><%
if ("true".equals(Tools.getRequestParameter(request, SetCharacterEncodingFilter.PDF_PRINT_PARAM)))
{
  pageContext.include("/admin/layout_top_pdf.jsp");
  return;
}

String lng = Prop.getLng(request, false);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(Constants.getServletContext(), request);

//nafejkujeme ze mame jquery aj ui
Tools.insertJQueryUI(pageContext, "all");
//fajkujeme ajax_support.js pre autocomplete tag
//request.setAttribute("ajaxSupportInserted", true);

String adminJqueryVersion = (String)request.getAttribute("adminJqueryVersion");
if (Tools.isEmpty(adminJqueryVersion)) adminJqueryVersion = "adminJqueryJs";

%><!DOCTYPE html>

<!--[if IE 8]> <html class="ie8 no-js"> <![endif]-->
<!--[if IE 9]> <html class="ie9 no-js"> <![endif]-->
<!--[if !IE]><!-->
<html class="no-js">
<!--<![endif]-->
<!-- BEGIN HEAD -->
<head>
	<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" >
	<title>WebJET CMS</title>
	<%
		String uaCompatible = (String)request.getAttribute("X-UA-Compatible");
		if (Tools.isEmpty(uaCompatible)) uaCompatible = Constants.getString("xUaCompatibleAdminValue");
	%>
	<meta http-equiv="X-UA-Compatible" content="<%=uaCompatible %>" />
	<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
	<meta content="www.interway.sk" name="author"/>

	<!-- BEGIN GLOBAL MANDATORY STYLES -->
	<%--<link href="http://fonts.googleapis.com/css?family=Open+Sans:400,300,600,700&subset=all" rel="stylesheet" type="text/css"/>--%>
	<iwcm:combine type="css" set="adminStandardCssWj9" />
	<link href="/admin/skins/webjet8/assets/global/css/webjet2021.css" rel="stylesheet" type="text/css"/>

	<!-- END THEME STYLES -->
	<link rel="shortcut icon" href="/admin/skins/webjet6/images/favicon.ico"/>

	<iwcm:combine type="js" set="<%=adminJqueryVersion%>" />

	<script type="text/javascript">
		$.ajaxSetup({
			headers: {
				'X-CSRF-Token': "<%=sk.iway.iwcm.system.stripes.CSRF.getCsrfToken(request.getSession(), true)%>"
			}
		});
		$(function () {
			setTimeout(()=> {
				//scrolll selected left menu item into view
				let $menuItem = $("div.md-main-menu__item--open");
				//console.log("$menuItem=", $menuItem);
				if ($menuItem.length>0) {
					let top = $menuItem.position().top;
					if (top > 150) {
						//console.log("Scrolling to:", top);
						$(".page-sidebar-menu").slimScroll({ scrollToY: top+'px' });
					}
				}
			}, 100);
		});
	</script>

</head>
<!-- END HEAD -->
<!-- BEGIN BODY -->
<%--<body class="page-header-fixed page-quick-sidebar-over-content">--%>
<body id="mainBodyElement" class="webjetcms-admin-body page-header-fixed page-quick-sidebar-over-content page-sidebar-fixed page-sidebar-closed-hide-logo SmallWindow-ShowHeader<%
	String wjPageRefere = request.getHeader("Referer");
	boolean wjIframed = false;
	if (wjPageRefere != null || "true".equalsIgnoreCase(Tools.getRequestParameter(request, "iframed")))
	{
		if ("true".equalsIgnoreCase(Tools.getRequestParameter(request, "iframed")) || wjPageRefere.indexOf("editor_component.jsp")!=-1)
		{
			out.print(" page-wjiframed");
			wjIframed = true;
		}
	}
	if (wjIframed==false && "true".equals(request.getParameter("iniframe"))) {
		out.print(" page-iniframe");
	}
	%>">

	<%
	if (wjIframed == false && (PathFilter.getOrigPath(request).startsWith("/admin")==false || PathFilter.getOrigPath(request).endsWith(".do")) ) { %>
		<script type="text/javascript">
		try
		{
			if (window.parent && window.parent != window.self)
			{
				var mainBody = document.getElementById("mainBodyElement");
				mainBody.className = mainBody.className + "  page-wjiframed";
			}
		}
		catch (e) {}
		</script>
	<% } %>

<!-- BEGIN HEADER -->
<%@ include file="layout_header.jsp" %>
<!-- END HEADER -->

<style type="text/css">
	<jsp:include page="/admin/css/perms-css.jsp"/>
</style>


<%--<div class="clearfix"></div>--%>

<!-- BEGIN CONTAINER -->
<div class="page-container">
    <!-- BEGIN SIDEBAR -->
  <%@ include file="layout_sidebar.jsp" %>
    <!-- END SIDEBAR -->

  <!-- BEGIN CONTENT -->
  <div class="page-content-wrapper">
    <div class="page-content">
