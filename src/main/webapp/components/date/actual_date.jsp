<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,java.util.*"%><%
PageParams pageParams = new PageParams(request);

String format = pageParams.getValue("format", Constants.getString("dateFormat"));

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat(format);
Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), lng, false);

java.util.GregorianCalendar cal = new java.util.GregorianCalendar();
String actualDate = formatter.format(cal.getTime());
if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY)
{
	actualDate = prop.getText("dayfull.mo") +  " " + actualDate;
}
else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY)
{
	actualDate = prop.getText("dayfull.tu") +  " " + actualDate;
}
else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY)
{
	actualDate = prop.getText("dayfull.we") +  " " + actualDate;
}
else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY)
{
	actualDate = prop.getText("dayfull.th") +  " " + actualDate;
}
else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
{
	actualDate = prop.getText("dayfull.fr") +  " " + actualDate;
}
else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
{
	actualDate = prop.getText("dayfull.sa") +  " " + actualDate;
}
else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
{
	actualDate = prop.getText("dayfull.su") +  " " + actualDate;
}

out.println(actualDate);
%>