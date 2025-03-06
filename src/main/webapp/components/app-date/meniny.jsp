<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.calendar.*"%>
<%@ page import="sk.iway.iwcm.doc.ShowDoc" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%

PageParams pageParams = new PageParams(request);
int offset = pageParams.getIntValue("offset", 0);

//v DB mame len SK a CZ meniny, ak to nie je CZ, pouzijeme default SK
String lng = PageLng.getUserLng(request);
if ("cz".equalsIgnoreCase(lng)==false) lng="sk";

String name = CalendarDB.getMeniny(lng, offset);

String format = pageParams.getValue("format", "long");

if ("long".equals(format))
{
	if ("cz".equals(lng)) out.print(ShowDoc.updateCodes(null, "!DEN_DATUM_CZ!", -1, request, sk.iway.iwcm.Constants.getServletContext()));
	else out.print(ShowDoc.updateCodes(null, "!DEN_DATUM!", -1, request, sk.iway.iwcm.Constants.getServletContext()));
%><iwcm:text key="components.app-date.dnes_ma_meniny"/>	
	<%
	if (name != null)
	{
	   out.print(name);
	}
	%>
<% } else { 
	if (name != null)
	{
		%><iwcm:text key="components.app-date.dnes_ma_meniny_bez_datumu"/>
		<%
	   out.print(name);
	}	
} %>