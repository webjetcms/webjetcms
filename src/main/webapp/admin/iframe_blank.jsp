<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/>


<%@page import="sk.iway.iwcm.Tools"%><html>
<head>
   <title>WebJET</title>
   <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
   <link rel="stylesheet" href="/admin/css/style.css" />
   <link rel="stylesheet" href="/admin/skins/webjet6/css/webjet6.css" />
</head>
<body>

<% String key = Tools.getRequestParameter(request, "textKey");
String param1 = Tools.getRequestParameter(request, "param1");
String param2 = Tools.getRequestParameter(request, "param2");
String param3 = Tools.getRequestParameter(request, "param3");
if(param1 == null)
	param1 = "";
if(param2 == null)
	param2 = "";
if(param3 == null)
	param3 = "";

if (Tools.isNotEmpty(key)) {%><iwcm:text key="<%=key %>" param1="<%=param1 %>" param2="<%=param2 %>" param3="<%=param3 %>" /><%} %>
&nbsp;

</body>
</html>
