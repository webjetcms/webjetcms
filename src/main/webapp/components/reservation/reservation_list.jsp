<%@page import="sk.iway.iwcm.components.reservation.ReservationObjectBean"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationAjaxAction"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_reservation"/>
<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);
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
boolean datepickerCSSInserted = Tools.isNotEmpty( (String)(request.getAttribute("datepicker-css-Inserted")) );
request.setAttribute("datepicker-css-Inserted", "true");
if(!datepickerCSSInserted)
{
%>
	<link rel="stylesheet" type="text/css" href="/admin/css/datepicker.css" />
<%
}
%>

<%@page import="sk.iway.iwcm.components.reservation.ReservationManager"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationBean"%>

<script type="text/javascript" src="/components/form/check_form.js"></script>
<script type="text/javascript" src="/components/calendar/popcalendar.jsp"></script>



<script type="text/javascript">
	//<![CDATA[
$(document).ready(function(){

	//datepicker a jeho priradena ikonka - spravanie na hover
	$(".ui-datepicker-trigger").on("mouseover", function(){
		$(this).addClass("hover");
	}).on("mouseout", function(){
		$(this).removeClass("hover");
	});

	//datepicker
	if ($(".datepicker").length > 0)
	{
		$(".datepicker").datepicker({
			showOn: 'both',
			firstDay: 1
		}, $.datepicker.regional['webjet']);
	}

	//timepicker
	$('.timepicker').focus(function() {
		$(this).timePicker();
	});
});

	function Ok()
	{
		document.filterForm.bFilter.click();
	}

	function passw(url, reservationId)
	{
		var newWindow;
		newWindow = window.open(url+"?reservation.reservationId="+reservationId,"","left=200, top=300, height=100,width=400,location=1");
		if (window.focus)
			newWindow.focus();
	}

	function confirmDelete(reservationId)
	{
		if(window.confirm('<iwcm:text key="components.dictionary.delete_confirm"/>') == true)
	   	{
	   		document.deleteForm.elements["reservation.reservationId"].value = reservationId;
	    	document.deleteForm.submit();
	   	}
	}
	//]]>
</script>
<%
	if (request.getParameter("bInputPasswd")!=null)
		pageContext.include("/sk/iway/iwcm/components/reservation/ReservationAjax.action");
	if (request.getParameter("bFilter")!=null)
	  pageContext.include("/sk/iway/iwcm/components/reservation/ReservationAjax.action");
	if (request.getParameter("bDeleteReservation")!=null)
	  pageContext.include("/sk/iway/iwcm/components/reservation/ReservationAjax.action");
	if (request.getParameter("bDeleteFilter")!=null)
		  pageContext.include("/sk/iway/iwcm/components/reservation/ReservationAjax.action");

	UserDetails currUser = UsersDB.getCurrentUser(session);
%>
<stripes:useActionBean var="reservationBean" beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction" />
	<%
		String acceptText = (String)request.getAttribute("acceptText");
		if (acceptText != null)
			out.print("<div class='error' style='color: red; font-weight: bold;'><p>"+acceptText+"</p></div>");

		String delText = (String)request.getAttribute("delText");

		if (delText != null)
			out.print("<div style='font-weight: bold;'><p>"+delText+"</p></div>");

		//rzapach: #19288, komentar #7
		String[] selectedObjectsIds = pageParams.getValue("showObjectIds", "").split(",");
		List<ReservationBean> selectedObjectsReservations = new ArrayList<ReservationBean>();
		List<ReservationObjectBean> selectedObjects = new ArrayList<ReservationObjectBean>();
		if(Tools.isNotEmpty( pageParams.getValue("showObjectIds", "")))
		{
			for(String s : selectedObjectsIds)
			{
				int objectId = Tools.getIntValue(s, -1);
				if(request.getParameter("bFilter")==null)
					selectedObjectsReservations.addAll(Collections.unmodifiableList(ReservationManager.getReservations(reservationBean.getFilterDateFrom(), reservationBean.getFilterDateTo(), objectId)));
				selectedObjects.add(ReservationManager.getReservationObject(objectId));
			}
		}
		else
		{
			selectedObjectsReservations = reservationBean.getAllReservations();
			selectedObjects = reservationBean.getAllReservationObjects();
		}

		if(request.getParameter("bFilter")!=null)
			selectedObjectsReservations = reservationBean.getAllReservations();

		request.setAttribute("selectedObjectsReservations", selectedObjectsReservations);
	%>



	<div class="reservationSearchForm">
		<iwcm:stripForm action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction" name="filterForm">
			<strong><iwcm:text key="components.reservation.reservation_list.search_reservation"/></strong>
			<br/>
			<iwcm:text key="components.reservation.reservation_list.date_from"/> <stripes:text class="datepicker" id="filterDateFromId" maxlength="10" size="11" name="filterDateFrom"  />
			<iwcm:text key="components.reservation.reservation_list.date_to"/> <stripes:text class="datepicker" id="filterDateToId" maxlength="10" size="11" name="filterDateTo"  />
			<iwcm:text key="components.reservation.reservation_list.object"/>
				<stripes:select name="filterObjectId" class="required" id="filterObjectId1">
						<stripes:option value="">Vyber objekt...</stripes:option>
						<stripes:options-collection collection="<%=selectedObjects%>" label="name" value="reservationObjectId" title="Objekty" />
					</stripes:select>
			<input type="submit" name="bFilter" class="submit" value="<iwcm:text key="searchall.search"/>" />
		</iwcm:stripForm>
	</div>

	<div class="addReservation">
		<a class="button" href="javascript:wjPopup('/components/reservation/addReservation.jsp?showObjectIds=<%=pageParams.getValue("showObjectIds", "")%>', 700, 450);"><iwcm:text key="components.reservation.reservation_list.addTitle"/></a>
	</div>



	<form action="<%=PathFilter.getOrigPath(request)%>" name="actionForm" style="display: none;">
		<input type="hidden" name="bInputPasswd" value="true" />
		<input type="hidden" name="reservation.reservationId" value="" />
	</form>

	<form action="<%=PathFilter.getOrigPath(request)%>" name="deleteForm" style="display: none;">
		<input type="hidden" name="bDeleteReservation" value="true" />
		<input type="hidden" name="reservation.reservationId" value="" />
	</form>

	<display:table name="selectedObjectsReservations" id="tableReservations" class="tabulkaStandard" pagesize="10" requestURI="<%=PathFilter.getOrigPath(request)%>">
		<%ReservationBean reservationRow = (ReservationBean)tableReservations;  %>

		<display:column title="#" sortable="false">
			<%=pageContext.getAttribute("tableReservations_rowNum")%>
		</display:column>
		<display:column property="reservationObjectName" titleKey="components.reservation.reservation_list.object2" sortable="false" />
		<display:column property="dateFrom"  titleKey="components.reservation.reservation_list.date_from2" sortable="false" decorator="sk.iway.displaytag.DateTimeDecorator"/>
		<display:column property="dateTo" titleKey="components.reservation.reservation_list.date_to2" decorator="sk.iway.displaytag.DateTimeDecorator"/>
		<display:column  titleKey="components.reservation.reservation_list.name" sortable="false" ><a href="mailto:${tableReservations.email}">${tableReservations.name} ${tableReservations.surname}</a></display:column>
		<display:column property="purpose" titleKey="components.reservation.reservation_list.purpose"/>
		<display:column titleKey="components.reservation.reservation_list.accept" style="text-align:center;" >
			<%if (((ReservationBean)tableReservations).isAccepted()) { %>
				<img src='/admin/images/icon-confirm.gif' border="0" alt='<iwcm:text key="components.reservation.admin_list.accepted.yes"/>' title='<iwcm:text key="components.reservation.admin_list.accepted.yes"/>' />
			<%} else { %>
				<img src='/admin/images/lock.gif' border="0" alt='<iwcm:text key="components.reservation.admin_list.accepted.no"/>' title='<iwcm:text key="components.reservation.admin_list.accepted.no"/>' />
			<%} %>
		</display:column>

		<display:column titleKey="components.table.column.tools" style="text-align:center;">
			<%if (ReservationManager.isDeletedWithoutPass(reservationRow.getReservationId()) || (currUser != null && currUser.getEmail().equals(reservationRow.getEmail()))){ %>
				<a href="javascript:confirmDelete(${tableReservations.reservationId});">
					<img src='/admin/images/icon_del.gif' border=0 alt='<iwcm:text key="button.delete"/>' />
				</a>
			<%} else { %>
				<a href="javascript:passw('/components/reservation/inputPasswd.jsp', ${tableReservations.reservationId});">
					<img src='/admin/images/icon_del.gif' border=0 alt='<iwcm:text key="button.delete" />' />
				</a>
			<%} %>
		</display:column>
	</display:table>