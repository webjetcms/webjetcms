<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>


<HTML>
<HEAD>
<TITLE><iway:request name="doc_title"/></TITLE>
<meta http-equiv="Content-type" content="text/html;charset=windows-1250">
<link rel="stylesheet" href="/css/page.css" type="text/css">

</HEAD>

<body>

   <iwcm:write name="doc_data"/>

</body>
</html>