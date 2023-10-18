<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
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

<div class="<%=classes %>">
	
	<div class="container">
		<% request.setAttribute("contentBlock", content); %><iwcm:write name="contentBlock"/>
	</div>
	
	
</div>