<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%

PageParams pageParams = new PageParams(request);

String type = pageParams.getValue("type", "1");
String title = pageParams.getValue("title", "");
String image1 = pageParams.getValue("image1", "");
String image2 = pageParams.getValue("image2", "");
String color = pageParams.getValue("color", "");
String classes = pageParams.getValue("classes", "");
String attrName = pageParams.getValue("attrName", "");

String includeStr = "!INCLUDE(/components/content-block/type-" + type + ".jsp, attrName=" + attrName + ", title=\"" + title + "\", image1=\"" + image1 + "\", image2=\"" + image2 + "\", color=\"" + color + "\", classes=\"" + classes + "\")!";
request.setAttribute("includeStr", includeStr);
%>
<iwcm:write name="includeStr"/>