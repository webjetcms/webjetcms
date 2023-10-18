<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/iframe_blank.js", null, request, response);
%><%@ page pageEncoding="utf-8" 
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" 
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" 
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/2000/REC-xhtml1-20000126/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="sk" lang="sk">
<head>
   <title>WebJET</title>
   <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" />
</head>
<body>

&nbsp;

</body>
</html>
