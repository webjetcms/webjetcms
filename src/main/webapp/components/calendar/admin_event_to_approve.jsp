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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_calendar"/>

<%@page import="sk.iway.iwcm.calendar.CalendarDB"%>
<%@page import="sk.iway.iwcm.calendar.CalendarDetails"%>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.components.calendar.CalendarActionBean"%>

<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.calendar.CalendarActionBean"/>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);
request.setAttribute("dialogTitleKey","calendar.schval_title");
request.setAttribute("dialogDescKey","calendar.schval_desc");
request.setAttribute("cmpName", "calendar");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<%

if (Tools.isNotEmpty(request.getParameter("schvalujem")))
{
	pageContext.include("/sk/iway/iwcm/components/calendar/Calendar.action");
}
int calendarId = -1;
CalendarDetails cal = null;
if(actionBean != null && actionBean.getApproveEventId() > 0)
{
	calendarId = actionBean.getApproveEventId();
	cal = CalendarDB.getEvent(calendarId,request);
}
else{
	calendarId = Tools.getIntValue(request.getParameter("calendarId"),-1);
	if(calendarId != -1)
		cal = CalendarDB.getEvent(calendarId,request);
}
if(calendarId == -1 || cal == null )
	return;
%>
<div class="padding10">
	<iwcm:stripForm class="searchAdvanced" name="schvalBtnForm" method="GET" action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.components.calendar.CalendarActionBean">
	<stripes:errors/>
	<table border="0" cellpadding="0" cellspacing="5">
	  <tr>
	    <td><b><iwcm:text key="calendar_edit.title"/>:</b></td>
	    <td><%=cal.getTitle()%></td>
	  </tr>
	  <tr>
	    <td><b><iwcm:text key="calendar_edit.description"/>:</b></td>
	    <td><%=cal.getDescription()%></td>
	  </tr>
	  <tr>
	    <td><b><iwcm:text key="calendar_edit.begin"/>:</b></td>
	    <td><%=cal.getFromString()%></b></td>
	  </tr>
	  <tr>
	    <td><b><iwcm:text key="calendar_edit.ende"/>:</b></td>
	    <td><%=cal.getToString()%></td>
	  </tr>
	  <tr>
	    <td><b><iwcm:text key="calendar_edit.time_range"/>:</b></td>
	    <td><%=cal.getTimeRange()%></td>
	  </tr>
	  <tr>
		<td><b><iwcm:text key="calendar_edit.area"/>:</b></td>
		<td><%=cal.getArea()%></td>
	  </tr>
	  <tr>
		<td><b><iwcm:text key="calendar_edit.city"/>:</b></td>
		<td><%=cal.getCity()%></td>
	  </tr>
	  <tr>
		<td><b><iwcm:text key="calendar.vytvoril"/>:</b></td>
		<td>
			<%
			UserDetails creator = UsersDB.getUser(cal.getCreatorId());
			String name = prop.getText("calendar.neznamy");
			if(creator != null)
				name = creator.getFullName();
			out.print(name);
			%>
		</td>
	  </tr>
	  <tr>
	    <input type="hidden" name="schvalujem" value="schvalujem" />
		<input type="hidden" name="approveEventId" id="approveEventId" value="<%=calendarId%>" />
		<input type="hidden" name="approveStatus" id="approveStatus" value="" />
		<td><input type="button" class="button50" name="schvalujem2" value="<%=prop.getText("calendar.schvalujem")%>" onclick="document.getElementById('approveStatus').value = 1;this.form.submit();"/></td>
		<td><input type="button" class="button50" name="schvalujem3" value="<%=prop.getText("calendar.neschvalujem")%>" onclick="document.getElementById('approveStatus').value = 0;this.form.submit();"/></td>
	  </tr>
	</table>
	</iwcm:stripForm>
</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>
<script type="text/javascript">
<!--
	var btnOk = document.getElementById("btnOk");
	btnOk.style.visibility="hidden";
// -->
</script>