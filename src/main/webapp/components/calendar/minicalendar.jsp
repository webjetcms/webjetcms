<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*,
java.util.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
%>

<%!
   /** The names of the months */
   String[] months = {
      "Jan", "Feb", "Mar", "Apr",
      "Máj", "Jún", "Júl", "Aug",
      "Sep", "Okt", "Nov", "Dec"
   };

   /** The days in each month. */
   int dom[] = {
         31, 28, 31, 30,   /* jan feb mar apr */
         31, 30, 31, 31, /* may jun jul aug */
         30, 31, 30, 31 /* sep oct nov dec */
   };
%>
<%
   int totalRows = 1;
   int i;

   // First get the month and year from the form.
   int yy = 0, mm = 0, dd=0;

   Calendar c = Calendar.getInstance();

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



   /** The number of days to leave blank at the start of this month */
   int leadGap = 0;

   //month - the value used to set the MONTH time field in the calendar. Month value is 0-based. e.g., 0 for January
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
<table border=0 width="140" cellpadding=0 cellspacing=0 align="center">
<tr>
	<td colspan="8" align="center" class="CAL-header"><%=mm%>/<%=yy%></td>
</tr>
<tr>
	<td class=CAL-DN>&nbsp;</td>
	<td class=CAL-DN><iwcm:text key="calendar.mo"/></td>
	<td class=CAL-DN><iwcm:text key="calendar.tu"/></td>
	<td class=CAL-DN><iwcm:text key="calendar.we"/></td>
	<td class=CAL-DN><iwcm:text key="calendar.th"/></td>
	<td class=CAL-DN><iwcm:text key="calendar.fr"/></td>
	<td class=CAL-WDN><iwcm:text key="calendar.sa"/></td>
	<td class=CAL-WDN><iwcm:text key="calendar.su"/></td>
</tr>
<tr class=CAL-Seven>
	<td width=17% class=CAL-SEVEN align=center><%=currentWeek%>.</td>
	<td rowspan='<%=totalRows%>' colspan=7>
		<table class=CAL border=1 width="100%" height="100%" borderColorDark=#ffffff borderColorLight=#797B7B width=80% cellspacing=0 cellpadding=1>
		
		<tr>
		<%
		      // Blank out the labels before 1st day of month
		      int tdInLine = 0;
		      for (i = 0; i < leadGap; i++) 
		      {
		         out.println("<td class=\"CAL-DayD\">&nbsp;</td>");
		         tdInLine++;
		      }
		
		      // Fill in numbers for the day of month.
		      String day;
		      for (i = 1; i <= daysInMonth; i++)
		      {
		         if (i==dd)
		         {

		            out.print("<td class='CAL-DaySelected'>");
		         }
		         else
		         {
		            if (tdInLine > 4)
		            {
		            	out.print("<td class='CAL-DayWeekend'>");		               
		            }
		            else
		            {
		               out.print("<td>");
		            }
		         }
		            
		         if (i<10) day = "0"+i;
		         else day = ""+i;
		         
		         out.print(day);
		
		         out.println("</td>");
		         tdInLine++;
		
		         // wrap if end of line.
		         if ((leadGap + i) % 7 == 0)
		         {
		            out.println("</tr>");
		            out.print("<tr>");
		            tdInLine = 0;
		         }
		      }
		      // Blank out the labels after last day of month
		      for (i = tdInLine; i < 7; i++)
		      {
		         out.print("<td class=\"CAL-DayD\">&nbsp;</td>");
		      }
		%>
		</tr>
		</table>

	</td>
</tr>
<%
   for (i=1; i<totalRows; i++)
   {
      currentWeek++;
      out.println("<tr class=CAL-Seven><td width=17% class=CAL-SEVEN align=center>"+currentWeek+".</td></tr>");
   }
%>
</table>
