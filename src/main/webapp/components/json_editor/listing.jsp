<%@page import="java.util.List"%><%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String json = pageParams.getValue("json", "[]");
JSONArray itemsList = new JSONArray(json);
%>
<h1>JSON ITEMS LIST</h1>

<div id="jsonList">
<% for(int i = 0; i < itemsList.length(); i++) { %>
	<% JSONObject item = itemsList.getJSONObject(i); %>
	<div class="item">
		<h2><%=item.get("title") %></h2>
		<p class="description"><%=item.get("description") %></p>
		<img src="<%=item.get("image") %>">
	</div>
<% } %>
</div>
