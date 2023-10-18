<%@page import="java.util.Collections"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.List"
%><%@page import="sk.iway.iwcm.components.reservation.ReservationObjectTimesDB"
%><%@page import="sk.iway.iwcm.components.reservation.ReservationObjectTimesBean"
%><%
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
<%@page import="sk.iway.iwcm.components.reservation.ReservationManager"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationBean"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationObjectBean"%><%

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

if(request.getParameter("load_calendar_content") != null && "true".equals(request.getParameter("load_calendar_content")))
{
	String dateFrom = request.getParameter("date_from");
	String timeFrom = request.getParameter("time_from");
	String timeTo = request.getParameter("time_to");
	int resId = Tools.getIntValue(request.getParameter("res_id"),0);
	if(resId > 0 && Tools.isNotEmpty(dateFrom) && Tools.isNotEmpty(timeFrom) && Tools.isNotEmpty(timeTo))
	{
		List<ReservationBean> reservations = ReservationManager.getReservations(new Date(DB.getTimestamp(dateFrom, "00:00")),new Date(DB.getTimestamp(dateFrom, "23:59")),resId);
		if(reservations != null && reservations.size() > 0)
		{
			%>
			<tr>
				<td>&nbsp;</td>
				<td valign="middle" align="left"><strong><iwcm:text key="components.reservation.admin_addObject.datum"/>:</strong>&nbsp;</td>
				<td valign="bottom" align="center"><div id="todaydiv"><strong><%=dateFrom%></strong></div></td>
				<td>&nbsp;</td>
		   </tr>
		   <tr>
				<td>&nbsp;</td>
				<td valign="middle" align="left">&nbsp;</td>
				<td><input type="image" src="/components/mail/images/hourmeter.jpg" ismap="ismap"/>&nbsp;&nbsp;&nbsp;&nbsp; </td>
				<td>&nbsp;</td>
			</tr>
			<%
			for(ReservationBean rb : reservations){
			%>
		  	<tr>
				<td>&nbsp;</td>
				<td><strong><%=rb.getName()+" "+rb.getSurname()%>:</strong>&nbsp;</td>
				<td><input src="/components/reservation/reservation_getimage.jsp?datum=<%=dateFrom%>&time=<%=timeFrom%>&stoptime=<%=timeTo%>&resId=<%=rb.getReservationId()%>" ismap="ismap" type="image"></td>
				<td>&nbsp;</td>
			</tr>
			<%
			}
		}
		else
		{
			%>

			<tr>
				<td>&nbsp;</td>
				<td colspan="2"><strong><iwcm:text key="components.reservation.admin_addObject.nie_su_rezervacie"/>&nbsp;<%=Tools.formatDate(DB.getTimestamp(dateFrom, "00:00"))%></strong></td>
				<td>&nbsp;</td>
		   </tr>
			<%
		}
	}
}
else if(request.getParameter("load_reservation_list") != null && "true".equals(request.getParameter("load_reservation_list")))
{
	int resId = Tools.getIntValue(request.getParameter("res_id"),0);
	List<ReservationBean> reservations = new ArrayList<ReservationBean>();
	if(resId>0 || Tools.isEmpty(request.getParameter("showObjectIds")))
		reservations = ReservationManager.getReservations(null, null, resId);
	else
	{
		String[] selectedObjectsIds = request.getParameter("showObjectIds").split(",");

		for(String s : selectedObjectsIds)
		{
			int objectId = Tools.getIntValue(s, -1);
			reservations.addAll(ReservationManager.getReservations(null, null, objectId));
		}
	}
	request.setAttribute("reservations",reservations);

	%>
	<display:table name="${reservations}" id="tableReservations" class="tabulkaStandard" requestURI="<%=PathFilter.getOrigPath(request)%>" cellpadding="0" cellspacing="0" length="10">
		<%ReservationBean reservationRow = (ReservationBean)tableReservations;%>

		<display:column property="reservationObjectName" titleKey="components.reservation.reservation_list.object2" sortable="true" />
		<display:column property="dateFrom"  titleKey="components.reservation.reservation_list.date_from2" sortable="true" decorator="sk.iway.displaytag.DateTimeDecorator"/>
		<display:column property="dateTo" titleKey="components.reservation.reservation_list.date_to2" decorator="sk.iway.displaytag.DateTimeDecorator"/>
		<display:column  titleKey="components.reservation.reservation_list.name" sortable="true" ><a href="mailto:${tableReservations.email}">${tableReservations.name} ${tableReservations.surname}</a></display:column>
		<display:column property="purpose" titleKey="components.reservation.reservation_list.purpose"/>
		<display:column titleKey="components.reservation.reservation_list.accept" style="text-align:center;" >
			<%if (((ReservationBean)tableReservations).isAccepted()) { %>
			<i class="fas fa-check" title='<iwcm:text key="components.reservation.admin_list.accepted.yes"/>'></i>
			<%} else { %>
			<i class="fas fa-lock" title='<iwcm:text key="components.reservation.admin_list.accepted.no"/>'></i>
			<%} %>
		</display:column>
	</display:table>
	<%
}
else if(request.getParameter("check_object") != null && request.getParameter("for_all_day") != null)
{
	int resId = Tools.getIntValue(request.getParameter("res_id"),0);
	ReservationObjectBean rob = ReservationManager.getReservationObjectById(resId);
	List<ReservationObjectTimesBean> rotb = ReservationObjectTimesDB.getInstance().getByReservationObjectId(resId);

	out.print("{");
	out.print("\"check_object\":\""+(rob != null && "true".equals(request.getParameter("check_object")) && (rob.getMaxReservations() > 1 || rotb.size()>0) ? "true" : "false")+"");
	out.print("\",");
	out.print("\"for_all_day\":\""+(rob != null && "true".equals(request.getParameter("for_all_day")) && rob.getReservationForAllDay() ? "true" : "false")+"");
	out.print("\"}");
}
else if(request.getParameter("check_checkTimeUnit") != null && request.getParameter("date")!=null)
{
	int resId = Tools.getIntValue(request.getParameter("res_id"),0);
	ReservationObjectBean rob = ReservationManager.getReservationObjectById(resId);
	List<ReservationObjectTimesBean> reservationTimes = ReservationObjectTimesDB.getInstance().getByReservationObjectId(resId);

	//default ak nie su zadane specificke dni
	String startHour = rob.getReservationTimeFrom().substring(0, rob.getReservationTimeFrom().indexOf(':'));
	String startMin = rob.getReservationTimeFrom().substring(rob.getReservationTimeFrom().indexOf(':')+1);
	String endHour = rob.getReservationTimeTo().substring(0, rob.getReservationTimeTo().indexOf(':'));
	String endMin = rob.getReservationTimeTo().substring(rob.getReservationTimeTo().indexOf(':')+1);

   	//ak je zadany den s vlastnym casom
   	if(reservationTimes!=null && reservationTimes.size()>0)
   	{
   		Calendar calendar = Calendar.getInstance();
      	calendar.setTime(Tools.getDateFromString(request.getParameter("date"), "dd.MM.yyyy"));
      	int day = calendar.get(Calendar.DAY_OF_WEEK)-1;
      	if(day==0)
      		day=7;
      	boolean emptyDay = true;

	   	for(ReservationObjectTimesBean rotb : reservationTimes)
	   	{
	   		if(day==rotb.getDen())
	   		{
	   			startHour = rotb.getCasOd().substring(0, rotb.getCasOd().indexOf(':'));
	      		startMin = rotb.getCasOd().substring(rotb.getCasOd().indexOf(':')+1);
	      		endHour = rotb.getCasDo().substring(0, rotb.getCasDo().indexOf(':'));
	      		endMin = rotb.getCasDo().substring(rotb.getCasDo().indexOf(':')+1);
	   			emptyDay=false;
	   		}
	   	}

	   	//ak bol vybrany den na ktory nie je zadany cas
	   	if(emptyDay)
	   	{
	   		startHour = "0";
	   		startMin = "0";
	   		endHour = "0";
	   		endMin = "0";
	   	}
   	}

	String timeUnit = rob.getTimeUnit();

	if(Tools.isEmpty(timeUnit))
		timeUnit = "30";

	out.print("{");
	out.print("\"timeUnit\":\""+timeUnit+"");
	out.print("\",");
	out.print("\"startHour\":\""+startHour+"");
	out.print("\",");
	out.print("\"startMin\":\""+startMin+"");
	out.print("\",");
	out.print("\"endHour\":\""+endHour+"");
	out.print("\",");
	out.print("\"endMin\":\""+endMin+"");
	out.print("\"}");
}
else if(request.getParameter("load_reservation_times") != null && "true".equals(request.getParameter("load_reservation_times")))
{
	int resId = Tools.getIntValue(request.getParameter("res_id"),-1);
	List<ReservationObjectTimesBean> reservationTimes = ReservationObjectTimesDB.getInstance().getByReservationObjectId(resId);
	request.setAttribute("reservationTimes",reservationTimes);

	if (reservationTimes!=null && reservationTimes.size()>0)
	{
	%>
	<p style="margin-top: 6px; margin-bottom: 0px;">Povolené časy rezervácie:</p>
	<display:table name="${reservationTimes}" id="tableReservationTimes" class="table table-striped" style="width: 100%; min-width: 50%;" requestURI="<%=PathFilter.getOrigPath(request)%>" cellpadding="0" cellspacing="0" length="10">
		<display:column titleKey="stat_doc.days">
			<%int den = ((ReservationObjectTimesBean)tableReservationTimes).getDen();
			if (den==1) { %>
				<iwcm:text key="dayfull.mo"/>
			<%} else if(den==2) { %>
				<iwcm:text key="dayfull.tu"/>
			<%} else if(den==3) { %>
				<iwcm:text key="dayfull.we"/>
			<%} else if(den==4) { %>
				<iwcm:text key="dayfull.th"/>
			<%} else if(den==5) { %>
				<iwcm:text key="dayfull.fr"/>
			<%} else if(den==6) { %>
				<iwcm:text key="dayfull.sa"/>
			<%} else if(den==7) { %>
				<iwcm:text key="dayfull.su"/>
			<%}%>
		</display:column>
		<display:column property="casOd" titleKey="components.basket.invoice.stats.time_from"/>
		<display:column property="casDo" titleKey="components.basket.invoice.stats.time_to"/>
	</display:table>
	<%
	}
}
%>