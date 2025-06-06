<%@page import="java.util.List"%><%//otestuj ci existuje nahrada za tuto stranku
String forward = "/components/"+Constants.getInstallName()+"/reservation/room_list.jsp";
java.io.File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
if (fForward.exists())
{
	pageContext.forward(forward);
	return;
} %>

<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationBean"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationManager"%>
<%@page
	import="sk.iway.iwcm.components.reservation.ReservationObjectBean"%>
<%@page import="java.util.*,java.text.*"%>
<%@page
	import="sk.iway.iwcm.components.reservation.ReservationAjaxAction"%>
<%=Tools.insertJQuery(request)%><%
	if(sk.iway.iwcm.common.CloudToolsForCore.isBootstrap(request)){
		%>
		<link rel="stylesheet" href="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/themes/smoothness/jquery-ui.css">
<script src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.11.4/jquery-ui.min.js"></script>
		<%
	}else{

		%>
		<%=Tools.insertJQueryUI(pageContext, "datepicker") %>
		<%
	}

%>
<%
	boolean datepickerCSSInserted = Tools.isNotEmpty((String) (request
			.getAttribute("datepicker-css-Inserted")));
	request.setAttribute("datepicker-css-Inserted", "true");
	if (!datepickerCSSInserted) {
%>
<link rel="stylesheet" type="text/css" href="/components/_common/css/datepicker.css" />
<%
	}
%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);
	Prop prop = Prop.getInstance(lng);

	int rezobjid = Tools.getIntValue(request.getParameter("rezobjid"), -1);
	DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	java.util.Date date = Calendar.getInstance().getTime();
	String todayDatumFrom = dateFormat.format(date);
%>

<script type="text/javascript">
	//<![CDATA[
	function clickReservation(idecko) {
		window.location.href = '<iwcm:cp/><%=PathFilter.getOrigPath(request)%>?rezobjid=' + idecko;
	}

	function changeOccupancy(dateFrom)
	{
		//window.alert("changeOccupancy, date="+dateFrom);
		var rezobjid = '<%=rezobjid%>';
		//alert("rezobjid; " + rezobjid);
		var paramsIn = {
			"dateFrom" : dateFrom,
			"rezobjid" : rezobjid,
		};
		//alert("dateFrom: " + dateFrom);
		var params = $.param(paramsIn);
		$.ajax({
			type : "POST",
			dataType : "html",
			data : params,
			url : '/components/reservation/get_reservation_objects_ajax.jsp',
			success : function(html) {
				$("#occupancy_div").html(html);
			}
		});
	}

	$(document).ready(function() {

		var myDate = new Date();
		//datepicker
		if ($(".datepicker").length > 0) {
			$(".datepicker").datepicker({
				showOn : 'both',
				firstDay : 1,
				dateFormat: "dd.mm.yy"
			//minDate : new Date(),
			}, $.datepicker.regional['webjet']);
			$('.datepicker').datepicker('setDate', myDate);
		}
		//var nextDate = new Date(myDate.getTime()+86400000);
		<%	if (rezobjid > 0) {	%>changeOccupancy("<%=ResponseUtils.filter(todayDatumFrom) %>");<% } %>
	});
	//]]>
</script>

<script type="text/javascript" src="/components/form/check_form.js"></script>
<stripes:useActionBean var="reservationBean"
	beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction" />

<%
	List<ReservationObjectBean> reservationObjectList = ReservationManager.getReservationObjectsForAllDay(true);
	List<ReservationObjectBean> reservationObjectListOut = new ArrayList<ReservationObjectBean>();
	if (rezobjid > 0)
	{
		for (int i = 0; i < reservationObjectList.size(); i++)
		{
			ReservationObjectBean reservations = reservationObjectList.get(i);
			if (Integer.valueOf(rezobjid) == reservations.getReservationObjectId())
			{
				reservationObjectListOut.add(reservations);
			}
		}
	}
	else
	{
		reservationObjectListOut = reservationObjectList;
	}
	request.setAttribute("reservationObjectListOut", reservationObjectListOut);

	int imageWidth = pageParams.getIntValue("imageWidth", 200);
	int imageHeight = pageParams.getIntValue("imageHeight", 150);
%>

<style type="text/css">
	table.occupancyTable { width:200px; background-color: <%=pageParams.getValue("backgroundColor", "#eee") %>; <%-- background-color: rgba(33, 33, 33, 0.5);--%> padding: 16px; border-collapse: collapse; width:300px}
	table.occupancyTable td { <%--background-color: #333333;--%> margin: 1px;}

	div#ui-datepicker-div { width: auto; }
	.dayNameWrapper{
	background-color:<%=pageParams.getValue("dayOfWeekBackgroundColor", "#d7d7d7") %>;
	}

	table.occupancyTable td{
		color: <%=pageParams.getValue("dayColor", "#000") %>;
	}

	.dayNameWrapper span{
		font-weight:700;
		color: <%=pageParams.getValue("dayOfWeekColor", "#000") %>;
	}

</style>

<table>
	<tr>
		<td colspan="2">

			<form>
			<table class="tabulkaStandard" style="width: 100%">
				<tr>
					<th>
						<%
						if (rezobjid > 0) {	%><iwcm:text key="components.reservation.reservation_objects.room_list" /><%	}
						else { %><iwcm:text key="components.reservation.room_list.reservation_for_all_day" /><% }
						%>
					</th>
					<th><stripes:label
							for="components.reservation.room_list.price_for_day" /></th>
				</tr>
				<c:forEach var="reservationObject"
					items="${reservationObjectListOut}">
					<tr>
						<td>
							<h3><c:out value="${reservationObject.name}" /></h3>

							<p>
								<% if (imageWidth > 0 && imageHeight > 0) { %>
									<img src="/thumb${reservationObject.photoLink}?w=<%=imageWidth%>&h=<%=imageHeight%>&ip=5" style="float: left; padding-right: 10px; padding-bottom: 10px;" />
								<% } %>
								<c:out value="${reservationObject.description}" escapeXml="false" />
							</p>
						</td>
						<td width="120" style="text-align:right;">
						<p class="reservationPrice">
							<c:out value="${reservationObject.priceForDayString}  €" />
							<% if (rezobjid <= 1) { %>
							</p><%
if(pageParams.getBooleanValue("allowReservations", true)){
%>
							<br/>
							<input
							type="button" class="btn" name="reserveButton"
							value='<%=prop.getText("components.reservation.room_list.reservation_button")%>'
							onclick="clickReservation(${reservationObject.reservationObjectId});">
							<% } %><% } %>
						</td>
					</tr>
					<tr></tr>
				</c:forEach>
				<tr>
			</table>
			</form>
		</td>
	</tr>
	<% if (rezobjid > 0) { %>
	<tr>
		<td style="width: 50%">
			<%
				pageContext.include("reservation_object_reservation_form.jsp");
			%>
		</td>
		<td valign="top" style="vertical-align: top; padding-top: 42px;">
			<strong><iwcm:text key="components.reservation.actual_occupancy"/></strong>
			<div id="occupancy_div">
			</div>

			<table border="0">
				<tr>
					<td>
						<iwcm:text key="components.reservation.actual_occupancy.free"/>
						<div style="width: 30px; height: 3px; background-color: red;">
							<div style="width: 30px; height: 3px; background-color: green"></div>
						</div>
					</td>
					<td>
						<iwcm:text key="components.reservation.actual_occupancy.partial"/>
						<div style="width: 30px; height: 3px; background-color: red;">
							<div style="width: 15px; height: 3px; background-color: green"></div>
						</div>
					</td>
					<td>
						<iwcm:text key="components.reservation.actual_occupancy.full"/>
						<div style="width: 30px; height: 3px; background-color: red;">

						</div>
					</td>
				</tr>
			</table>

		</td>
	</tr>
	<% } %>
</table>
