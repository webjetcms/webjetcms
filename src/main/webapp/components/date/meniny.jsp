<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.calendar.*"%><%

PageParams pageParams = new PageParams(request);

int offset = pageParams.getIntValue("offset", 0);

//v DB mame len SK a CZ meniny, ak to nie je CZ, pouzijeme default SK
String lng = PageLng.getUserLng(request);
if ("cz".equalsIgnoreCase(lng)==false) lng="sk";

String name = CalendarDB.getMeniny(lng, offset);

if (name != null)
{
   out.print(name);
}
%>