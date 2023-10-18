<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>

<%
response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

String cmpName = (String)request.getAttribute("cmpName");
String titleKey = (String)request.getAttribute("titleKey");
String descKey = (String)request.getAttribute("descKey");
String iconLink = "/components/"+cmpName+"/editoricon.gif";
if (request.getAttribute("iconLink")!=null)
{
	iconLink = (String)request.getAttribute("iconLink");
}
if (titleKey == null) titleKey = "components."+cmpName+".title"; 
if (descKey == null) descKey = "components."+cmpName+".desc";

//otestuj ci existuje...
if ((new File(sk.iway.iwcm.Tools.getRealPath(iconLink))).isFile()==false)
{
   iconLink = "/components/editoricon.gif";
}

%>

<%=titleKey%>

