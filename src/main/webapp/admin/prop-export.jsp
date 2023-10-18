<%@page import="com.fasterxml.jackson.databind.SerializationFeature"%><%@
page import="com.fasterxml.jackson.databind.ObjectMapper"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="edit_text"/><%
String lng = Tools.getRequestParameter(request, "lng");
String format = Tools.getRequestParameter(request, "format");
String prefix = Tools.getRequestParameter(request, "prefix");
if(Tools.isEmpty(lng) || Tools.isEmpty(format))
	return;
if(format.equals("properties"))
	response.setHeader( "Content-Disposition", "attachment; filename=" + "text_"+lng+"-"+Constants.getInstallName()+".properties" );
if(format.equals("json"))
	response.setHeader( "Content-Disposition", "attachment; filename=" + "text_"+lng+"-"+Constants.getInstallName()+".json" );
response.setContentType("application/x-download; charset=utf-8");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"
%><%@page import="sk.iway.iwcm.i18n.Prop"
%><%@page import="sk.iway.iwcm.i18n.IwayProperties"%><%
Identity user = (Identity) session.getAttribute(Constants.USER_KEY);
if (user != null && user.isAdmin())
{
	Logger.debug(IwayProperties.class, "Exporting custom properties for language: " + lng + " and format: " + format + " and prefix: " + prefix);
	IwayProperties iwprop = Prop.getChangedProperties(lng, prefix);

	if(format.equals("properties"))
		iwprop.storeUtf8(out,null);
	if(format.equals("json"))
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		out.print(mapper.writeValueAsString(iwprop));
	}
}
%>
