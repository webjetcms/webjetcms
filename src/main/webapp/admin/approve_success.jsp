<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/>

<html>
<head>
<title><iwcm:text key="approve.page.title"/></title> 
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
<%
   response.setHeader("Pragma","No-Cache");
   response.setDateHeader("Expires",0);
   response.setHeader("Cache-Control","no-Cache");
%>
<meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
<LINK rel="stylesheet" href="css/style.css">
</head>
<body bgcolor="#FFFFFF" text="#000000" style="margin: 10px; font-weight: bold;" leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">

<iwcm:present name="approved">
	<img src="/admin/images/warning.gif" align="absmiddle"/>
   <iwcm:text key="approve.page.approved"/>
</iwcm:present>

<iwcm:present name="approvedToNextLevel">
	<img src="/admin/images/warning.gif" align="absmiddle"/>
   <iway:request name="approvedToNextLevel"/>
</iwcm:present>

<iwcm:present name="pagePublishDate">
	<img src="/admin/images/warning.gif" align="absmiddle"/>
	<iwcm:text key="editor.publish.pagesaved"/>
	<iway:request name="pagePublishDate"/>
</iwcm:present>

<iwcm:present name="rejected">
	<img src="/admin/images/warning.gif" align="absmiddle"/>
   <iwcm:text key="approve.page.rejected"/>
</iwcm:present>

<%
session.removeAttribute("approveByUsers");
%>

</table>
</body>
</html>