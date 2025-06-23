<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%

PageParams pageParams = new PageParams(request);

String title = pageParams.getValue("title", "");
String image1 = pageParams.getValue("image1", "");
String image2 = pageParams.getValue("image2", "");
String color = pageParams.getValue("color", "");
String classes = pageParams.getValue("classes", "");
String attrName = pageParams.getValue("attrName", "");
String content = Tools.getStringValue((String) request.getAttribute(attrName), "");

%>

<div class="blueBox <%=classes %>">
	<div class="container">
		<div class="col-lg-4 col-sm-12">
			<img src="<%=image1 %>" alt="<%=title %>" />
		</div>	
		<div class="col-lg-7 col-sm-12">
			<h2><%=title %></h2>
			<% request.setAttribute("contentBlock", content); %><iwcm:write name="contentBlock"/>
		</div>	
		<div class="col-lg-1 col-sm-12"></div>
	</div>
	<div class="img" style="background-image: url(<%=image2 %>);"></div>
</div>