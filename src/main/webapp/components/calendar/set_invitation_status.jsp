<%@page import="sk.iway.iwcm.calendar.CalendarInvitationDetails"%>
<%@page import="java.io.File"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
page import="sk.iway.iwcm.calendar.CalendarDB"%><%

//nastavenie stavu akceptacie schodzky

/* parameter status je char(1) a mal by mat hodnoty:
	 * -=nenastavene
	 * A=accepted
	 * D=declined
	 * T=tentative 
*/	

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);

PageParams pageParams = new PageParams(request);
String status = pageParams.getValue("status", null);

int calendarInvitationId = Tools.getIntValue(request.getParameter("ciid"), -1);
CalendarInvitationDetails invDetails = null;
if (calendarInvitationId>0)
{
	invDetails = CalendarDB.getInvitation(calendarInvitationId);
}
if (invDetails!=null)
{
	boolean saveOK = CalendarDB.setCalendarInvitationStatus(calendarInvitationId, status);
	if (saveOK)
	{
		out.println(prop.getText("calendar.invitation.saveok-"+status));
		
		if ("A".equals(status))
		{
			//test na ICAL subor
			String icalURL = Constants.getString("calendarIcalDir")+"ical-"+invDetails.getCalendarId()+".ics";
			File f = new File(sk.iway.iwcm.Tools.getRealPath(icalURL));
			if (f.exists())
			{
				out.println(prop.getText("calendar.invitation.ical", icalURL));
			}
		}
	}
	else
	{
		out.println(prop.getText("calendar.invitation.savefail-"+status));
	}
}
else
{
	out.println(prop.getText("calendar.invitation.unknownid"));
}
%>