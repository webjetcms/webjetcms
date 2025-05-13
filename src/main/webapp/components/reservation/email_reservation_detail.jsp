<%@page import="sk.iway.iwcm.components.reservation.ReservationObjectBean"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationBean"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%@page import="sk.iway.iwcm.i18n.Prop"%>

<%
	ReservationBean reservation = (ReservationBean) request.getAttribute("reservation");
	ReservationObjectBean reservationObject = (ReservationObjectBean) request.getAttribute("reservationObject");

	boolean approve = true;
	if("true".equals(Tools.getParameter(request, "cancel")))
		approve = false;

	String lng = PageLng.getUserLng(request);
	Prop prop = Prop.getInstance(lng);
%>

<table class="invoiceDetailWrappingTable" border="0" cellspacing="0" cellpadding="20" align="center">
	<tr>
		<td>
			<table class="invoiceDetailTable" border="0" cellspacing="0" cellpadding="0" align="center">
				<tr>
					<td class="invoiceHeader">
						<%if(approve){%>
							<iwcm:text key="components.reservation.approved"/>
						<%}else{%>
							<iwcm:text key="components.reservation.canceled"/>
						<%}%>
					</td>
					<%if(Tools.isNotEmpty(reservationObject.getPhotoLink())){%>
						<td class="invoiceHeader alignRight"><img src="<%=reservationObject.getPhotoLink()%>" style="width:100px;" alt=""></td>
					<%}%>
				</tr>
				<tr>
					<td class="noRightBorder"><strong><iwcm:text key="components.reservation.number"/>:</strong> <iwcm:beanWrite name="reservation" property="reservationId"/></td>
					<td class="alignRight">
						<%if(approve){%>
							<iwcm:text key="components.reservation.approved.date"/>: <%=Tools.formatDateTime(new Date())%>
						<%}else{%>
							<iwcm:text key="components.reservation.canceled.date"/>: <%=Tools.formatDateTime(new Date())%>
						<%}%>
					</td>
				</tr>
				<tr>
					<td colspan="2" class="oslovenie" >
						<%if(approve){%>
							<iwcm:text key="components.reservation.approved.text" param1='<%=reservation.getName()+" "+reservation.getSurname()%>' param2="<%=reservationObject.getName()%>"/>
						<%}else{%>
							<iwcm:text key="components.reservation.canceled.text" param1='<%=reservation.getName()+" "+reservation.getSurname()%>' param2="<%=reservationObject.getName()%>"/>
						<%}%>
					</td>
				</tr>
				<tr>
					<td>
						<strong><iwcm:text key="components.reservation.add_object.title"/></strong>
						<br/>
						<table class="invoiceInnerTable">
					      <tr>
					         <td><iwcm:text key="components.reservation.admin_addObject.name"/></td>
					         <td><iwcm:beanWrite name="reservationObject" property="name"/></td>
					      </tr>
					      <tr>
					         <td><iwcm:text key="components.reservation.admin_addObject.description"/></td>
					         <td><iwcm:beanWrite name="reservationObject" property="description"/></td>
					      </tr>
					     </table><br/>
				     	<strong><iwcm:text key="components.reservation"/></strong>
						<br/>
						<table class="invoiceInnerTable">
						     <tr>
						        <td><iwcm:text key="components.reservation.addReservation.date_from"/></td>
						        <td><%=Tools.formatDateTime(reservation.getDateFrom())%></td>
						     </tr>
						     <tr>
						        <td><iwcm:text key="components.reservation.addReservation.date_to"/></td>
						        <td><%=Tools.formatDateTime(reservation.getDateTo())%></td>
						     </tr>
						     <tr>
						        <td><iwcm:text key="components.reservation.addReservation.purpose"/></td>
						        <td><iwcm:beanWrite name="reservation" property="purpose"/></td>
						     </tr>
						     <%if(reservationObject.getReservationForAllDay() && Tools.isNotEmpty(reservationObject.getPriceForDayString()))
						     {%>
							     <tr>
							        <td><iwcm:text key="components.reservation.admin_addObject.price_for_day"/>:</td>
							        <td><iwcm:beanWrite name="reservationObject" property="priceForDayString"/> €</td>
							     </tr>
							 <%}else if (Tools.isNotEmpty(reservationObject.getPriceForHourString())){%>
							 	<tr>
							        <td><iwcm:text key="components.reservation.admin_addObject.price_for_hour"/>:</td>
							        <td><iwcm:beanWrite name="reservationObject" property="priceForHourString"/> €</td>
							     </tr>
							 <%}%>
						  </table>
					</td>
					<td>
						<strong><iwcm:text key="components.basket.invoice_email.contact"/></strong>
						<br/>
						<table class="invoiceInnerTable">
					      <tr>
					         <td><iwcm:text key="components.reservation.addReservation.name"/></td>
					         <td><iwcm:beanWrite name="reservation" property="name"/></td>
					      </tr>
					      <tr>
					         <td><iwcm:text key="components.reservation.addReservation.surname"/></td>
					         <td><iwcm:beanWrite name="reservation" property="surname"/></td>
					      </tr>
					      <tr>
					         <td><iwcm:text key="components.reservation.addReservation.email"/></td>
					         <td><iwcm:beanWrite name="reservation" property="email"/></td>
					      </tr>
					      <tr>
					         <td><iwcm:text key="components.reservation.addReservation.phoneNumber"/></td>
					         <td><iwcm:beanWrite name="reservation" property="phoneNumber"/></td>
					      </tr>
					   </table>
					</td>
				</tr>
			</table>
		</td>
	</tr>
	<tr>
		<td class="footer">
			<iwcm:text key="components.basket.orderConfirmationFooter" param1="<%=Tools.getBaseHref(request) %>"/>
		</td>
	</tr>
</table>