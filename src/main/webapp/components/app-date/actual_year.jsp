<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="java.util.*"%><%
Calendar cal = GregorianCalendar.getInstance();
out.println(cal.get(Calendar.YEAR));
%>