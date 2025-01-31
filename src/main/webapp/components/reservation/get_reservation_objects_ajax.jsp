<%@page import="java.util.Map"%><%@page import="java.util.List"%>
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationBean"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationManager"%>
<%@page
	import="sk.iway.iwcm.components.reservation.ReservationRoomManager"%>
<%@page
	import="sk.iway.iwcm.components.reservation.ReservationObjectBean"%>
<%@page import="java.util.*,java.text.*,sk.iway.iwcm.i18n.*"%>
<%@page
	import="sk.iway.iwcm.components.reservation.ReservationAjaxAction"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(lng);
	PageParams pageParams = new PageParams(request);
	String filterDateFromString = Tools.getStringValue(request.getParameter("dateFrom"), "20.10.2014");
	String rezobjid = Tools.getStringValue(request.getParameter("rezobjid"), "");
	pageContext.setAttribute("rezobjid", rezobjid);
%>

<stripes:useActionBean var="reservationBean" beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction" />

<%
	ReservationRoomManager resMan = new ReservationRoomManager();
	DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

	//spravime kalendar od 1. v danom mesiaci po posledny den v danom mestiaci a zaroven od pondelka do nedele
	Calendar filterDateFromCal = DateTools.dateToCalendar(dateFormat.parse(filterDateFromString));
	filterDateFromCal.set(Calendar.DAY_OF_MONTH, 1);

	int month = filterDateFromCal.get(Calendar.MONTH) + 1;
	int year = filterDateFromCal.get(Calendar.YEAR);

	//out.println(Tools.formatDate(filterDateFromCal.getTime()));

	Calendar previousMonth = GregorianCalendar.getInstance();
	previousMonth.setTime(filterDateFromCal.getTime());
	previousMonth.add(Calendar.MONTH, -1);

	while (filterDateFromCal.get(Calendar.DAY_OF_WEEK)!=Calendar.MONDAY)
	{
		filterDateFromCal.add(Calendar.DAY_OF_YEAR, -1);
	}

	//filterDateFromCal.add(Calendar.DAY_OF_YEAR, -filterDateFromCal.get(Calendar.DAY_OF_WEEK));

	//out.println(Tools.formatDate(filterDateFromCal.getTime()));

	Calendar filterDateToCal = DateTools.dateToCalendar(dateFormat.parse(filterDateFromString));
	filterDateToCal.set(Calendar.DAY_OF_MONTH, 1);
	filterDateToCal.add(Calendar.MONTH, 1);
	while (filterDateToCal.get(Calendar.DAY_OF_WEEK)!=Calendar.SUNDAY)
	{
		filterDateToCal.add(Calendar.DAY_OF_YEAR, 1);
	}

	//out.println(Tools.formatDate(filterDateToCal.getTime()));


	Map<String, Integer> occupancyMap = new HashMap<String, Integer>();
	List<String> datumBetweenStringListForDay = DateTools.getDatesBetweenIncludeString(filterDateFromCal.getTime(), filterDateToCal.getTime());

	//doplnim poradie dna v tyzdni a poradie tyzdna
	List<ReservationBean> reservationList = null;
	List<ReservationObjectBean> reservationObjectList = new ArrayList<ReservationObjectBean>();
	if (Tools.isNotEmpty(rezobjid))
	{
		//reservation zoradene podla datum_from reservation_id in (...)
		reservationList = ReservationManager.getAllReservationsForAllDayObjectId(filterDateFromCal.getTime(), filterDateToCal.getTime(), true, rezobjid);
		//reservation_object id=42
		ReservationObjectBean reservationObject = ReservationManager.getReservationObjectForAllDayById(Integer.valueOf(rezobjid), true);
		if (reservationObject.getReservationObjectId() != 0)
		{
			reservationObjectList.add(reservationObject);
		}
		else
		{
			return;
		}
	}
	else
	{
		reservationList = ReservationManager.getAllReservationsForAllDay(filterDateFromCal.getTime(), filterDateToCal.getTime(), true);
		reservationObjectList = ReservationManager.getReservationObjectsForAllDay(true);
	}
	for (int i = 0; i < reservationList.size(); i++)
	{
		ReservationBean reservation = reservationList.get(i);
		//out.println(reservation.getDateFrom()+"-"+reservation.getDateTo());
		int reservationObjectId = reservation.getReservationObjectId();
		int maxReservations = ReservationManager.getReservationObjectById(reservationObjectId).getMaxReservations();
		Calendar dateToCal = DateTools.dateToCalendar(reservation.getDateTo());
		Date dateToMinusOneDay = DateTools.getDaysAfter(-1, dateToCal).getTime();
		List<String> reservDatumBetweenStringList = DateTools.getDatesBetweenIncludeString(reservation.getDateFrom(), dateToMinusOneDay);
		for (int j = 0; j < reservDatumBetweenStringList.size(); j++)
		{
			String datumReservation = reservDatumBetweenStringList.get(j);//Št.06.11.2014.okt
			//Št.06.11.2014.okt-42-4
			String key = datumReservation + "-" + reservationObjectId + "-" + maxReservations;
			//out.println(key);
			if (!occupancyMap.isEmpty() && occupancyMap.containsKey(key))
			{
				int value = occupancyMap.get(key);
				occupancyMap.put(key, value + 1);//Po.17.11.2014.okt-42-4=2 a neskor =3
			}
			else
			{
				occupancyMap.put(key, 1);//Št.06.11.2014.okt-42-4=1
			}
		}
	}

	//out.println(filterDateFromCal.getTime());

	DateTools dt = new DateTools();
	List<String> datumBetweenStringListOrderForDay = dt.getDatesBetweenIncludeOrderString(filterDateFromCal.getTime(), filterDateToCal.getTime());
	//out.println(datumBetweenStringListOrderForDay+"<br/>");
	List<Date> datumBetweenDateListForOccup = DateTools.getDatesBetweenInclude(filterDateFromCal.getTime(), filterDateToCal.getTime());
	//out.println(datumBetweenDateListForOccup+"<br/>");
	String[][] occupancyArray = resMan.getOccupancyArrayFromHash(occupancyMap, datumBetweenDateListForOccup,	reservationObjectList);
	String[][] dayArray = resMan.getDaysArrayFromList(datumBetweenStringListForDay);
	request.setAttribute("dayArray", dayArray);
	request.setAttribute("occupancyArray", occupancyArray);
	if (Tools.isNotEmpty(rezobjid))
	{
		//datumReservation-reservationObjectId-occupancy/maxReservations
		//List<String> occupancyListWithDatum = resMan.getListOccupancyWithDatum(occupancyMap,
		//			datumBetweenStringListOrderForDay);
		//request.setAttribute("occupancyListWithDatum", occupancyListWithDatum);
		String[][] occupancyArrayWithDatum = resMan.getArrayOccupancyWithDatum(occupancyMap, datumBetweenStringListOrderForDay, rezobjid);
		request.setAttribute("occupancyArrayWithDatum", occupancyArrayWithDatum);
	}

	int tdCounter = Tools.getIntValue(String.valueOf(year)+String.valueOf(month)+"00", 0);
%>

<c:if test="${not empty rezobjid}">
	<table class="reservationDataTable">
		<tr>
			<td class="calendarMonthStep" align="center" onclick="changeOccupancy('<%=Tools.formatDate(previousMonth.getTime())%>')">&lt;&lt;</td>
			<td colspan="5" align="center"> <iwcm:text key='<%="component.calendar.month."+month%>'/> <%=year%> </td>
			<% previousMonth.add(Calendar.MONTH, 2); %><td class="calendarMonthStep" align="center" onclick="changeOccupancy('<%=Tools.formatDate(previousMonth.getTime())%>')">&gt;&gt;</td>
		</tr>
		<tr>
			<td><iwcm:text key="calendar.mo"/></td>
			<td><iwcm:text key="calendar.tu"/></td>
			<td><iwcm:text key="calendar.we"/></td>
			<td><iwcm:text key="calendar.th"/></td>
			<td><iwcm:text key="calendar.fr"/></td>
			<td><iwcm:text key="calendar.sa"/></td>
			<td><iwcm:text key="calendar.su"/></td>
		</tr>
		<c:forEach var="occupA" items="${occupancyArrayWithDatum}">
			<tr>
				<%
				String[] occupA = (String[])pageContext.getAttribute("occupA");
				for (int i=1; i<=7; i++)
				{
					if (occupA[i].contains("-"))
					{
						String[] splited = occupA[i].split("-");

						Calendar cal = GregorianCalendar.getInstance();
						cal.setTime(Tools.getDateFromString(splited[3], Constants.getString("dateFormat")));

						String addClass = "";
						if (cal.get(Calendar.MONTH)+1 != month)
						{
							addClass += " otherMonth";
						}
					%>
						<td width="35"
							title="<%=prop.getText("components.reservation.get_reservation_objects_ajax.volnych")%>&nbsp;<%=splited[1]%>"
							class="calendarDay<%=addClass %>" id="calendarTd<%=tdCounter %>" onmouseover="calendarOver(<%=tdCounter %>)"
							onclick="calendarClick('<%=splited[3]%>', <%=tdCounter++ %>)"
							>
							<%=splited[0]%>
							<br/>
							<div style="width: 30px; height: 3px; background-color: red;">
								<div style="width: <%=splited[2]%>px; height: 3px; background-color: green"></div>
							</div>
						</td>
					<%
					}
					else
					{
						%>
						<td width="35"><%=occupA[i] %></td>
						<%
					}
				}
				%>
		</c:forEach>
	</table>
</c:if>

<c:if test="${empty rezobjid}">
	<table>
		<tr>
			<c:forEach var="day" items="${dayArray}" varStatus="status3">
				<c:if
					test="${not status3.first and dayArray[status3.index][3] eq dayArray[status3.index-1][3]}">
					<td></td>
				</c:if>
				<c:if
					test="${not status3.first and dayArray[status3.index][3] ne dayArray[status3.index-1][3]}">
					<td width="34">${day[3]}</td>
				</c:if>
				<c:if test="${status3.first}">
					<td width="34">${day[3]}</td>
				</c:if>
			</c:forEach>
		</tr>
		<tr>
			<c:forEach var="day" items="${dayArray}" varStatus="status4">
				<c:if
					test="${not status4.first and dayArray[status4.index][4] eq dayArray[status4.index-1][4]}">
					<td></td>
				</c:if>
				<c:if
					test="${not status4.first and dayArray[status4.index][4] ne dayArray[status4.index-1][4]}">
					<td width="34">${day[4]}</td>
				</c:if>
				<c:if test="${status4.first}">
					<td width="34">${day[4]}</td>
				</c:if>
			</c:forEach>
		</tr>
		<tr>
			<c:forEach var="day" items="${dayArray}">
				<td width="34"><c:out value="${day[1]}" /></td>
			</c:forEach>
		</tr>
		<tr>
			<c:forEach var="day" items="${dayArray}">
				<td width="34"><c:out value="${day[0]}" /></td>
			</c:forEach>
		</tr>
	</table>
	<table>
		<c:forEach var="occupancy" items="${occupancyArray}"
			varStatus="status">
			<tr>
				<c:if test="${status.count % 2 == 1}">
					<td colspan="35"><c:out value="${occupancy[0]}" /></td>
				</c:if>
				<c:if test="${status.count % 2 == 0}">
					<td width="34"><c:out value="${occupancy[0]}" /></td>
					<td width="34"><c:out value="${occupancy[1]}" /></td>
					<td width="34"><c:out value="${occupancy[2]}" /></td>
					<td width="34"><c:out value="${occupancy[3]}" /></td>
					<td width="34"><c:out value="${occupancy[4]}" /></td>
					<td width="34"><c:out value="${occupancy[5]}" /></td>
					<td width="34"><c:out value="${occupancy[6]}" /></td>
					<td width="34"><c:out value="${occupancy[7]}" /></td>
					<td width="34"><c:out value="${occupancy[8]}" /></td>
					<td width="34"><c:out value="${occupancy[9]}" /></td>
					<td width="34"><c:out value="${occupancy[10]}" /></td>
					<td width="34"><c:out value="${occupancy[11]}" /></td>
					<td width="34"><c:out value="${occupancy[12]}" /></td>
					<td width="34"><c:out value="${occupancy[13]}" /></td>
					<td width="34"><c:out value="${occupancy[14]}" /></td>
					<td width="34"><c:out value="${occupancy[15]}" /></td>
					<td width="34"><c:out value="${occupancy[16]}" /></td>
					<td width="34"><c:out value="${occupancy[17]}" /></td>
					<td width="34"><c:out value="${occupancy[18]}" /></td>
					<td width="34"><c:out value="${occupancy[19]}" /></td>
					<td width="34"><c:out value="${occupancy[20]}" /></td>
					<td width="34"><c:out value="${occupancy[21]}" /></td>
					<td width="34"><c:out value="${occupancy[22]}" /></td>
					<td width="34"><c:out value="${occupancy[23]}" /></td>
					<td width="34"><c:out value="${occupancy[24]}" /></td>
					<td width="34"><c:out value="${occupancy[25]}" /></td>
					<td width="34"><c:out value="${occupancy[26]}" /></td>
					<td width="34"><c:out value="${occupancy[27]}" /></td>
					<td width="34"><c:out value="${occupancy[28]}" /></td>
					<td width="34"><c:out value="${occupancy[29]}" /></td>
					<td width="34"><c:out value="${occupancy[30]}" /></td>
					<td width="34"><c:out value="${occupancy[31]}" /></td>
					<td width="34"><c:out value="${occupancy[32]}" /></td>
					<td width="34"><c:out value="${occupancy[33]}" /></td>
					<td width="34"><c:out value="${occupancy[34]}" /></td>
				</c:if>
			</tr>
		</c:forEach>
		<tr>
	</table>
</c:if>