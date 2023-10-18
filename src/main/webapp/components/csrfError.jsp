<%@page import="sk.iway.iwcm.i18n.Prop"%><%@page import="sk.iway.iwcm.PageLng"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"%><%
String error = (String)request.getAttribute("errorText");
if (error == null)
{
	String lng = PageLng.getUserLng(request);
	Prop prop = Prop.getInstance(lng);
	error = prop.getText("components.csrfError");	
}
if (error != null)
{
	request.setAttribute("errorText", error);
%>
<div class='error' style='color: red; font-weight: bold;'><p><%=error %></p></div>
<script type="text/javascript">
	window.alert("<%=error %>");
</script>
<% } %>
