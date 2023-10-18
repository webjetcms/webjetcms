<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@ 
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@ 
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@ 
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@ 
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true"/><?xml version="1.0" encoding="<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" 
        "http://www.w3.org/TR/2000/REC-xhtml1-20000126/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="<%=PageLng.getUserLng(request)%>" lang="<%=PageLng.getUserLng(request)%>">
 <head>
  <title></title>
  <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />
  <meta http-equiv="Content-language" content="<%=PageLng.getUserLng(request)%>" />
  <meta name="description" content="WebJET Content Management web site" />
  <meta name="author" content="Interway, s.r.o." />
    <style type="text/css" title="InterWay, s.r.o." media="screen">
      	@import "<iwcm:write name="base_css_link"/>";
    </style>
    <style type="text/css" media="print">
      	@import "/css/print.css";
    </style>
    <style type="text/css" title="InterWay, s.r.o." media="screen">
		@import "/components/_common/css/default.css";
	</style>
    <!--[if gte IE 5]>  
    <style type="text/css" title="InterWay, s.r.o." media="screen">
      	@import "/css/page_ie.css";
    </style>
    <![endif]-->
    <script type="text/javascript" src="/jscripts/common.js"></script>
</head>
<body id="webjetEditorBody"><iwcm:write name="doc_data"/></body>
</html>
