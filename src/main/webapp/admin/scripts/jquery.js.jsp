<% 
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript"); 
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/common.js", null, request, response);
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%

String htmlCode = FileTools.readFileContent("/admin/scripts/jquery.js", "windows-1250");
out.println(htmlCode);

%>

