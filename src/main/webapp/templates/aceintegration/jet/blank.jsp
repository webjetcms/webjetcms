<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.inquiry.*,sk.iway.iwcm.*,java.io.*" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%
String lng = PageLng.getUserLng(request);
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="sk" lang="${ninja.temp.lngIso}" data-browser-name="${ninja.userAgent.browserName}" data-browser-version="${ninja.userAgent.browserVersion}" data-device-type="${ninja.userAgent.deviceType}" data-device-os="${ninja.userAgent.deviceOS}">
<head>
	<meta http-equiv="X-UA-Compatible" content="IE=EmulateIE7" />
	<title><iwcm:write name="doc_title"/></title>
	<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />
    <meta http-equiv="Content-language" content="<%=PageLng.getUserLng(request)%>" />
    <logic:notEmpty name="perex_data"><meta name="Description" content="<iwcm:write name="perex_data"/>" /></logic:notEmpty>
    <logic:notEmpty name="field_a"><meta name="Keywords" content="<iwcm:write name="field_a"/>" /></logic:notEmpty>
	<meta name="author" content="InterWay, a. s. - www.interway.sk" />
	<meta name="generator" content="WebJET - redakèný systém - www.webjet.sk" />
    <iwcm:write name="group_htmlhead_recursive"/>
    <iwcm:write name="html_head"/>
</head>
<body style="padding: 0; margin: 0;">
	<iwcm:write name="doc_data"/>
</body>
</html>