<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.Prop"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
//it's there just for backward compatibilityString

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String groupIds = pageParams.getValue("groupIds", "");

String code = Prop.getInstance(lng).getText("components.blog.atricles-code", groupIds);
request.setAttribute("blog-data", code);
%>
<iwcm:write name="blog-data"/>