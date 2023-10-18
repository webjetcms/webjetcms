<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

//controlJS sposobi zacyklenie Firefoxu
request.setAttribute("packagerEnableControljs", Boolean.FALSE);

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

out.println(Tools.insertJQuery(request));

int rootGroupId = pageParams.getIntValue("rootGroupId", 1);
int maxLevel = pageParams.getIntValue("maxLevel", 4);
String classes = pageParams.getValue("classes", "none");
int startOffset = pageParams.getIntValue("startOffset", -1);

//ID root UL elementu
String sitemapId = null;
if (request.getAttribute("lastSitemapId") == null)
	sitemapId = "sitemap";
else
	sitemapId = request.getAttribute("lastSitemapId").toString()+"_1";
request.setAttribute("lastSitemapId",sitemapId);

request.setAttribute("ulLiOpenAllItems", "true");
%>

<%@page import="sk.iway.iwcm.components.menu.MenuULLI"%>
<link rel="stylesheet" href="/components/sitemap/jquery/jquery.treeview.css" />
<script src="/components/sitemap/jquery/jquery.treeview.min.js" type="text/javascript"></script>
<script type="text/javascript">
$(document).ready(function(){
	$("#<%=sitemapId%>").treeview({
		collapsed: true,
		animated: "medium"
	});
});
</script>
<div class="treeview">
<%
String vypis = MenuULLI.doTree(rootGroupId, maxLevel, startOffset, classes, sitemapId, -1, true, request);
out.println(vypis);
%>
</div>