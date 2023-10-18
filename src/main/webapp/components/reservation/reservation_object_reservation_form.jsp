<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
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
<%@ page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationManager"%>
<%@page
	import="sk.iway.iwcm.components.reservation.ReservationObjectBean"%>
<%=Tools.insertJQuery(request)%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
%>

<script type="text/javascript">
	//<![CDATA[          

	$(document).ready(function() {
		//
	});
	//]]>
</script>

<%
	if (request.getParameter("bSaveReservation") != null)
	{
		pageContext.include("/sk/iway/iwcm/components/reservation/ReservationAjax.action");
		if (request.getAttribute("saveOK") != null)
		{
			%>
			<span style="color: green; font-weight: bold;"><iwcm:text
					key="components.reservation.reservation_object_reservation_form.reservation_ok" /></span>
			<%
			return;
		}
	}
%>

<stripes:useActionBean var="reservationBean"
	beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction" />
<%
	String rezobjid = request.getParameter("rezobjid");
	ReservationObjectBean reservationObject = ReservationManager.getReservationObjectForAllDayById(Integer.valueOf(rezobjid), true);
	request.setAttribute("reservationObject", reservationObject);
%>

<c:if test="${reservationObject.getReservationObjectId() != 0}">

	<div id="saved_div"></div>

	<iwcm:stripForm name="reservationForm"
		action="<%=PathFilter.getOrigPath(request)%>" method="post"
		beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction"
		id="reservationForm">

		<table border="0">
			<tr>
				<th style="text-align: left;"><label for="dateFrom1"><iwcm:text
							key="components.reservation.reservation_object_reservation_form.date_from" /></label></th>
				<th style="text-align: left;"><label for="dateTo"><iwcm:text
							key="components.reservation.reservation_object_reservation_form.date_to" /></label></th>
			</tr>
			<tr>
				<td><stripes:text name="reservation.dateFrom" id="dateFrom1"
						class="input datepicker" maxlength="10" size="11"
						onchange="changeOccupancy(this.value)" /></td>
				<td><stripes:text name="reservation.dateTo" id="dateTo"
						class="input datepicker" maxlength="10" size="11" /></td>
			</tr>
			<tr>
				<th style="text-align: left;"><label for="reservationNameId"><iwcm:text
							key="components.reservation.reservation_object_reservation_form.name" /></label></th>
			</tr>
			<tr>
				<td colspan="2"><stripes:text id="reservationNameId"
						name="reservation.name" class="required" size="32" /></td>
			</tr>
			<tr>
				<th style="text-align: left;"><label for="surnameId"><iwcm:text
							key="components.reservation.reservation_object_reservation_form.surname" /></label></th>
			</tr>
			<tr>
				<td colspan="2"><stripes:text name="reservation.surname"
						id="surnameId" class="required" size="32" /></td>
			</tr>
			<tr>
				<th style="text-align: left;"><label for="emailId"><iwcm:text
							key="components.reservation.reservation_object_reservation_form.email" /></label></th>
			</tr>
			<tr>
				<td colspan="2"><stripes:text name="reservation.email"
						id="emailId" class="required" size="32" /></td>
			</tr>
			<tr>
				<th style="text-align: left;"><label for="phoneNumber"><iwcm:text
							key="components.reservation.reservation_object_reservation_form.phoneNumber" /></label></th>
			</tr>
			<tr>
				<td colspan="2"><stripes:text name="reservation.phoneNumber"
						id="phoneNumber" size="32" /></td>
			</tr>
			<tr>
				<th style="text-align: left;"><label for="purposeId"><iwcm:text
							key="components.reservation.reservation_object_reservation_form.note" /></label></th>
			</tr>
			<tr>
				<td colspan="2"><stripes:textarea name="reservation.purpose"
						id="purposeId" class="required" rows="3" cols="33" /></td>
			</tr>
			<tr>
				<td><stripes:submit name="bSaveReservation">
						<iwcm:text key="button.submit" />
					</stripes:submit> <input type="hidden" name="rezobjid" value="<%=rezobjid%>" /> <input
					type="hidden" name="reservation.reservationObjectId"
					value="<%=rezobjid%>" /> <input type="hidden"
					name="reservation.startTime" value="14:00" /> <input type="hidden"
					name="reservation.finishTime" value="10:00" /> <input
					type="hidden" name="reservation.reservationObjectId"
					value="${reservationObject.getReservationObjectId()}"></td>
			</tr>
		</table>
	</iwcm:stripForm>
</c:if>