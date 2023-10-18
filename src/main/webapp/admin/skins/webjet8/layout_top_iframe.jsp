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

Prop prop = Prop.getInstance(Constants.getServletContext(), request);
%><!DOCTYPE html>

<!--
Template Name: Metronic - Responsive Admin Dashboard Template build with Twitter Bootstrap 3.1.1
Version: 2.0.2
Author: KeenThemes
Website: http://www.keenthemes.com/
Contact: support@keenthemes.com
Purchase: http://themeforest.net/item/metronic-responsive-admin-dashboard-template/4021469?ref=keenthemes
License: You must have a valid license purchased only from themeforest(the above link) in order to legally use the theme for your project.
-->
<!--[if IE 8]> <html lang="en" class="ie8 no-js"> <![endif]-->
<!--[if IE 9]> <html lang="en" class="ie9 no-js"> <![endif]-->
<!--[if !IE]><!-->
<html lang="en" class="no-js">
<!--<![endif]-->
<!-- BEGIN HEAD -->
<head>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" >
<title>Dashboard | Webjet</title>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta content="width=device-width, initial-scale=1.0" name="viewport"/>
<meta content="" name="description"/>
<meta content="" name="author"/>

<iwcm:combine type="css" set="adminStandardCss" />


<!-- END THEME STYLES -->
<link rel="shortcut icon" href="favicon.ico"/>

<iwcm:combine type="js" set="adminJqueryJs" />

</head>
<!-- END HEAD -->
<!-- BEGIN BODY -->
<body class="" style="background-image: none; background-color: transparent;">

