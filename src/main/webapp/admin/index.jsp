<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%
//otestuj ci existuje nahrada za tuto stranku
String forward = WriteTag.getCustomPageAdmin("/admin/index.jsp", request);
if (forward!=null)
{
	pageContext.forward(forward);
	return;
}
%>
<iwcm:checkLogon admin="true"/>
<jsp:useBean id="iwcm_useriwcm" scope="session" type="sk.iway.iwcm.Identity"/>

<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<html>
<head>
<title>:: Web JET admin ::</title>
<%
   response.setHeader("Pragma","No-Cache");
   response.setDateHeader("Expires",0);
   response.setHeader("Cache-Control","no-Cache");

   String mainLink = "welcome.jsp";
   if (Tools.getRequestParameter(request, "mainLink")!=null)
   {
      mainLink = Tools.getRequestParameter(request, "mainLink");
   }

   //System.out.println("INDEX sessionID: " + session.getId()+" is new="+session.isNew());
%>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
</head>
<frameset rows="52,*" frameborder="no" border="0" framespacing="0">
	<frame name="topFrame" scrolling="no" noresize src="top.jsp" >
	<frameset cols="154,*" frameborder="no" border="0" framespacing="0" rows="*">
		<frame name="leftFrame" id="leftFrame" scrolling="no" noresize src="left.jsp">
		<frame name="mainFrame" src="<%=mainLink%>">
	</frameset>
</frameset>
<noframes>
	<body></body>
</noframes>
</html>
