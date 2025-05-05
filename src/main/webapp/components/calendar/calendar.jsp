<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.calendar.*,
sk.iway.iwcm.*,
sk.iway.iwcm.i18n.*,
java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
//ak mame na stranke viac kalendarov, toto je nase ID:
Integer calendarID = (Integer)request.getAttribute("calendar.jsp.calendarID");
if (calendarID == null) calendarID = new Integer(1);

int myCalendarId = calendarID.intValue();

request.setAttribute("calendar.jsp.calendarID", new Integer(myCalendarId+1));
%>

<!-- KALENDAR ZACIATOK  -->

<script type="text/javascript">
	<!--
		var ie4 = (document.all) ? true : false;
		var ns4 = (document.layers) ? true : false;
		var ns6 = (document.getElementById && !document.all) ? true : false;

		function writeToLayer(lay,txt){

			if (ie4){

				document.all[lay].innerHTML = txt;
				}

			if (ns4){

				document[lay].document.write(txt);
				document[lay].document.close();
				}

			if (ns6){

				over = document.getElementById([lay]);
				range = document.createRange();
				range.setStartBefore(over);
				domfrag = range.createContextualFragment(txt);

				while (over.hasChildNodes()){

					over.removeChild(over.lastChild);
					}
				over.appendChild(domfrag);
				}
			}

		function showEvent(calendarId, text)
		{
			writeToLayer("eventLayer"+calendarId, text);
		}
	-->
</script>

	<%!
		/** The days in each month. */
		int dom[] = {
				31, 28, 31, 30,   /* jan feb mar apr */
				31, 30, 31, 31, /* may jun jul aug */
				30, 31, 30, 31 /* sep oct nov dec */
		};

		public List getEvents(int day, GregorianCalendar clickDate, List events)
		{
			if (events==null) return(new ArrayList());
			clickDate.set(Calendar.DAY_OF_MONTH, day);
			long l_date = clickDate.getTime().getTime();

			//skus najst zaznam
			Iterator iter = events.iterator();
			CalendarDetails event;
			List ret=new ArrayList();
			while (iter.hasNext())
			{
				event = (CalendarDetails)iter.next();
				if (l_date>=event.getFromLong() && l_date<=event.getToLong()) ret.add(event);
			}

			return(ret);
		}

	%>
	<%
		PageParams pageParams = new PageParams(request);
		String typyNazvy = pageParams.getValue("typyNazvy", null);
		request.removeAttribute("typyNazvy");
		if (Tools.isNotEmpty(typyNazvy)) request.setAttribute("typyNazvy", typyNazvy);

		String lng = PageLng.getUserLng(request);
		pageContext.setAttribute("lng", lng);

		Prop prop = Prop.getInstance(lng);

		/** The names of the months */
		String[] months = new String[12];
		months[0] = prop.getText("component.calendar.january");
		months[1] = prop.getText("component.calendar.february");
		months[2] = prop.getText("component.calendar.march");
		months[3] = prop.getText("component.calendar.april");
		months[4] = prop.getText("component.calendar.may");
		months[5] = prop.getText("component.calendar.june");
		months[6] = prop.getText("component.calendar.july");
		months[7] = prop.getText("component.calendar.august");
		months[8] = prop.getText("component.calendar.september");
		months[9] = prop.getText("component.calendar.october");
		months[10] = prop.getText("component.calendar.november");
		months[11] = prop.getText("component.calendar.december");

		long l_now = (new java.util.Date()).getTime();
		java.sql.Date dateNow = new java.sql.Date(l_now);

		request.setAttribute("calAllEvents", "true");
		request.setAttribute("showApprove", "1");
		List events = CalendarDB.getEvents(request); // (ArrayList)request.getAttribute("events");

		int totalRows = 1;
		int i;
		// First get the month and year from the form.
		boolean yyok = false;   // -1 is a valid year, use boolean
		int yy = 0, mm = 0, dd=0;

		Calendar c = Calendar.getInstance();

		boolean is_print=false;
		if (request.getParameter("print")!=null) is_print=true;

		String yyString = request.getParameter("year");
		yy = c.get(Calendar.YEAR);
		if (yyString != null && yyString.length() > 0)
		{
			try
			{
				yy = Integer.parseInt(yyString);
			}
			catch (NumberFormatException e) {}
		}

		String mmString = request.getParameter("month");
		//lebo v kalendari je mesiac indexovany od 0... choreeeee
		mm = c.get(Calendar.MONTH)+1;
		if (mmString != null && mmString.length() > 0)
		{
			try
			{
				mm = Integer.parseInt(mmString);
			}
			catch (NumberFormatException e) {}
		}

		String ddString = request.getParameter("day");
		dd = c.get(Calendar.DAY_OF_MONTH);
		if (ddString != null && ddString.length() > 0)
		{
			try
			{
				dd = Integer.parseInt(ddString);
			}
			catch (NumberFormatException e) {}
		}

		GregorianCalendar clickDate = new GregorianCalendar(yy, mm-1, dd);

		/** The number of days to leave blank at the start of this month */
		int leadGap = 0;

		GregorianCalendar calendar = new GregorianCalendar(yy, mm-1, 1);
		// Compute how much to leave before the first.
		// getDay() returns 0 for Sunday, which is just right.
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		int calDay = calendar.get(Calendar.DAY_OF_WEEK);
		if (calDay==Calendar.MONDAY) leadGap = 0;
		if (calDay==Calendar.TUESDAY) leadGap = 1;
		if (calDay==Calendar.WEDNESDAY) leadGap = 2;
		if (calDay==Calendar.THURSDAY) leadGap = 3;
		if (calDay==Calendar.FRIDAY) leadGap = 4;
		if (calDay==Calendar.SATURDAY) leadGap = 5;
		if (calDay==Calendar.SUNDAY) leadGap = 6;

		int daysInMonth = dom[mm-1];
		if (calendar.isLeapYear(calendar.get(Calendar.YEAR)) && mm == 2)
		{
			++daysInMonth;
		}

		int currentWeek = calendar.get(Calendar.WEEK_OF_YEAR);
		totalRows = 1;
		//compute total rows
		for (i = 1; i <= daysInMonth; i++)
		{
			if ((leadGap + i) % 7 == 0)
			{
				totalRows++;
			}
		}
	%>

<script type="text/javascript">
	<!--
		document.write('<style type="text/css" media="all">@import url("/components/calendar/calendar.css");</style>');

		var print=false;
		<iwcm:present parameter="print">
			print = true;
		</iwcm:present>
	//-->
</script>

<div class="calendar">
    <form method="get" action="<%=PathFilter.getOrigPath(request)%>" id="calendarf<%=myCalendarId%>">
    <%
    if (Constants.getInt("linkType")==Constants.LINK_TYPE_DOCID && request.getParameter("docid")!=null)
	 {
			out.println("<input type='hidden' name='docid' value='"+Tools.getDocId(request)+"'/>");
	 }
    %>
	<fieldset>

	<%
	//if we are going to print this, dont show select boxes
	if (!is_print)
	{
	%>
		<select name="month" class="cal_select" onchange="document.getElementById('calendarf<%=myCalendarId%>').submit();">
		<%
			for (i=1; i<=months.length; i++)
			{
				if (i==mm)
					out.print("<option value='"+i+"' class=\"cal_select\" selected=\"selected\">");
				else
					out.print("<option value='"+i+"' class=\"cal_select\">");
				out.print(months[i-1]);
				out.println("</option>");
			}
		%>
		</select>
		<select name="year" class="cal_select" onchange="document.getElementById('calendarf<%=myCalendarId%>').submit();">
		<%
			Calendar calNow = GregorianCalendar.getInstance();
			for (i=1990; i<=calNow.get(Calendar.YEAR); i++)
			{
				if (i==yy)
					out.print("<option class=\"cal_select\"  selected=\"selected\">");
				else
					out.print("<option class=\"cal_select\">");
				out.print(i);
				out.println("</option>");
			}
		%>
		</select>
	<%
	} else {
		out.println(months[mm-1]+" "+yy);
	}
	%>
	</fieldset>
	</form>
	<table cellspacing="0" cellpadding="0">
		<tr>
			<td>

			<!--zaciatok tabulky s kalendarom -->

			<table border="0" cellspacing="1" cellpadding="0">
				<tr>
					<td class="cal_dn"><iwcm:text key="calendar.mo"/></td>
					<td class="cal_dn"><iwcm:text key="calendar.tu"/></td>
					<td class="cal_dn"><iwcm:text key="calendar.we"/></td>
					<td class="cal_dn"><iwcm:text key="calendar.th"/></td>
					<td class="cal_dn"><iwcm:text key="calendar.fr"/></td>
					<td class="cal_dn"><iwcm:text key="calendar.sa"/></td>
					<td class="cal_dn"><iwcm:text key="calendar.su"/></td>
				</tr>
				<tr>
			<%
					String weekend = "";

					// Blank out the labels before 1st day of month
					for (i = 0; i < leadGap; i++)
					{
						weekend = "";
					   if (i > 4) weekend = "_weekend";
						out.print("<td class=\"cal_dayd"+weekend+"\"> </td>");
					}

					// Fill in numbers for the day of month.
					int tdInLine = 0;
					String day;
					String css_class;

					CalendarDetails event;
					List eventsDay;
					Iterator iter;
					String data;
					for (i = 1; i <= daysInMonth; i++)
					{
					   weekend = "";
					   if ((leadGap + i) % 7 == 6 || (leadGap + i) % 7 == 0) weekend = "_weekend";
						css_class = "class=\"cal_day"+weekend+"\"";

						out.print("<td "+css_class+">");

						//out.print("<font class=\"cal_day\">");
						if (i<10) day = "0"+i;
						else day = ""+i;

						out.print(day);


						//if (!is_print) out.print("</a>");
						//else out.print("</font>");

						eventsDay = getEvents(i, clickDate, events);
						if (eventsDay!=null && eventsDay.size()>0){

							out.println("<br />");
							iter = eventsDay.iterator();

							while (iter.hasNext()){

								event = (CalendarDetails)iter.next();

								data = "&lt;h3>"+prop.getText("component.calendar.event")+":&lt;/h3>&lt;b>"+event.getTitle()+"&lt;/b>&lt;br />"+prop.getText("component.calendar.date")+":&lt;br />"+event.getFromString()+"-"+event.getToString();
								data = data.replace('\'', ' ');
								data = data.replace('"', ' ');

								out.println("<a href='javascript:showEvent("+myCalendarId+", \""+data+"\")'>"+event.getType()+"</a>");
								}
							}
						out.println("</td>");
						tdInLine++;

						// wrap if end of line.
						if ((leadGap + i) % 7 == 0){

							out.println("</tr>");
							if (i<daysInMonth) out.print("<tr>");
							tdInLine = 0;
							}
						}
					// Blank out the labels after last day of month
						if (tdInLine>0){

							for (i = tdInLine; i < 7; i++){

								weekend = "";
								if (i>4) weekend = "_weekend";

								out.print("<td class=\"cal_dayd"+weekend+"\"> </td>");
								}
							}
							else{
								totalRows--;
							}

					if (tdInLine!=0) {
			%>
				</tr>
				<% } %>
			</table>

			<!--koniec tabulky s kalendarom -->

    		</td>
		</tr>
	</table>

	<div class="akcia" id="eventLayer<%=myCalendarId%>">
		<h3><iwcm:text key="component.calendar.event"/>:</h3>

		<iwcm:text key="component.calendar.helpText"/>
	</div>
</div>
	<!-- KALENDAR KONIEC  -->