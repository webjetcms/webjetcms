<%@page import="sk.iway.iwcm.components.reservation.ReservationManager"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationObjectBean"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationAjaxAction"%>
<%@page import="java.util.Date"%>
<%@page import="sk.iway.iwcm.components.reservation.ReservationBean"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
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

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageLng.setUserLng(request, response, lng);
	
	PageParams pageParams = new PageParams(request);

	request.setAttribute("cmpName", "reservation");
	request.setAttribute("titleKey", "components.reservation.add.title");
	request.setAttribute("descKey", "components.reservation.add.desc");
	
	request.setAttribute("dialogHasTabs", "true");
%>

<jsp:include page="/components/top-public.jsp"/>
<link type="text/css" rel="stylesheet" href="/admin/FCKeditor/editor/skins/webjet/fck_dialog.css" />
<link href="/admin/css/tabpane-luna.css" media="screen" rel="stylesheet" type="text/css" />
<style type="text/css">
.main { padding: 0px; }
</style>

<%=Tools.insertJQuery(request) %>
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
<script type="text/javascript" src="/admin/scripts/modalDialog.js"></script>
<script type="text/javascript" src="/components/form/check_form.js"></script>
<script type="text/javascript" src="/admin/scripts/dateTime.jsp"></script>

<script type="text/javascript">


	function loadReservation() {
		$.ajax({
			type : "POST",
			url : "/components/reservation/reservation-ajax_utf-8.jsp",
			data : "check_object=true&for_all_day=true&res_id=" + $("#reservationObjectId1").val(),
		   dataType : "json",					
			success : function(data) {				

				// disablujem startTime a finishTime, 
				// ak ma parameter reservation_for_all_day z ResevationObject ma hodnotu true 
				// a nastavim na hodnotu startTime = 14:00 a finishTime = 10:00
				if ($.trim(data.for_all_day) == 'true') {					
					$('label[for="starTimeId1"]').hide();					
					$("#starTimeId1").hide();
					$('label[for="finishTimeId1"]').hide();					
					$("#finishTimeId1").hide();	
					$("#starTimeId1").val("14:00");
					$("#finishTimeId1").val("10:00");
				} 
				else {					
					$('label[for="starTimeId1"]').show();					
					$("#starTimeId1").show();
					$('label[for="finishTimeId1"]').show();					
					$("#finishTimeId1").show();					
				} 

				//ak mozeme zadat pocet prekrivajucich rezervacii > 1, tak disablujem datum do
				if ($.trim(data.check_object) == 'true') {
					$("#dateTo").val($("#dateFrom").val());
					$("#dateTo").attr("readonly", true);
					$(".dateToCal").hide();
				} else {
					$("#dateTo").removeAttr("readonly");
					$(".dateToCal").show();
				}

			},
			async : false
		});

		$('#reservationTimes').load('/components/reservation/reservation-ajax_utf-8.jsp?load_reservation_times=true&res_id='+$("#reservationObjectId1").val());
		
		<%if(Tools.isEmpty(request.getParameter("showObjectIds")))
		{%>
			$('#reservationList').load('/components/reservation/reservation-ajax_utf-8.jsp?load_reservation_list=true&res_id='+$("#reservationObjectId1").val());
		<%}
		else
		{%>
			$('#reservationList').load('/components/reservation/reservation-ajax_utf-8.jsp?load_reservation_list=true&res_id='+$("#reservationObjectId1").val()+'&showObjectIds=<%=request.getParameter("showObjectIds")%>');
		<%}%>
	}

	$(document).ready(function() {
		loadReservation();		
		reservationObjectChange();
		$("#reservationObjectId1").change(reservationObjectChange);
		//$("#dateFrom").change(reservationObjectChange);
		hideObjectSelectBox();
	});
	
	function reservationObjectChange()
	{
		var val = $("#reservationObjectId1").val();
		  
		if (val != "") {
			$.ajax({
				'url': '/components/reservation/reservation-ajax_utf-8.jsp',
				data : "date="+$("#dateFrom").val()+"&check_checkTimeUnit=true&res_id=" + $("#reservationObjectId1").val(),
				dataType: 'json',
				success: function(data) {
					
					var valuesStart = getOptionValues(data);
					valuesStart.pop();
					var valuesEnd = getOptionValues(data);
					valuesEnd.shift();
					
					$('#startTimeId1, #finishTimeId1').empty();
					$.each(valuesStart, function(i, v){
						$('#startTimeId1').append('<option>' + v + '</option>')
					});
					$.each(valuesEnd, function(i, v){
						$('#finishTimeId1').append('<option>' + v + '</option>')
					});
				}
			})
		}
	}
	
	function getOptionValues(data)
	{
		var date = new Date();
		//date.setHours(0, 0, 0, 0);
		date.setHours(Number(data.startHour), Number(data.startMin), 0, 0);
		var endDate = new Date();
		endDate.setHours(Number(data.endHour), Number(data.endMin), 0, 0);
		var result = [];
		var day = date.getDay();
		
		while(true) {
			var hour = date.getHours() > 9 ? date.getHours() : "0" +date.getHours();
			var minute = date.getMinutes() > 9 ? date.getMinutes() : "0" + date.getMinutes();
			var d = hour + ":" + minute;
			result.push(d);
			
			date.setMinutes(date.getMinutes() + Number(data.timeUnit));
			if(date>endDate)
				break;
		}
		
		return result;
	}
	
	function timeChange()
	{
		var index =	$("#startTimeId1 option:selected").index()+1;
		$("#finishTimeId1 :nth-child("+index+")").prop('selected', true);
	}
	
	function hideObjectSelectBox()
	{
		if($("#reservationObjectId1").children().length == 2)
		{
			$("#reservationObjectId1 :nth-child("+2+")").prop('selected', true);
			loadReservation();
			reservationObjectChange();
			$("#selectObjectTrId").hide();
		}
	}
	
	function loadCalendarContent(fromHp) {
		var ok = true;
		resId = $("#reservationObjectId1").val();
		if (resId == '' || resId == undefined || parseInt(resId) < 1) {
			alert('<iwcm:text key="components.reservation.admin_addObject.vyberte_objekt"/>');
			ok = false;
		}
		dateFrom = fromHp ? $("#dateFrom").val() : $('#dateId').val();
		if (ok && dateFrom == '') {
			alert('<iwcm:text key="components.reservation.admin_addObject.vyberte_datum_od"/>');
			ok = false;
		}
		startTime = fromHp ? $("#starTimeId1").val() : $("#startTimeId").val();
		if (ok && startTime == '') {
			alert('<iwcm:text key="components.reservation.admin_addObject.vyberte_cas_zaciatku"/>');
			ok = false;
		}
		finishTime = fromHp ? $("#finishTimeId1").val() : $("#endTimeId").val();
		if (ok && finishTime == '') {
			alert('<iwcm:text key="components.reservation.admin_addObject.vyberte_cas_konca"/>');
			ok = false;
		}
		if (ok) {
			if (fromHp) {
				$('#dateId').val($("#dateFrom").val());
				$('#startTimeId').val($("#starTimeId1").val());
				$('#endTimeId').val($("#finishTimeId1").val());
			} else {
				$('#dateFrom').val($("#dateId").val());
				$('#starTimeId1').val($("#startTimeId").val());
				$('#finishTimeId1').val($("#endTimeId").val());
				if ($('.dateToCal').is(':hidden'))
					$("#dateTo").val($("#dateId").val());
			}

			$.ajax({
				type : "POST",
				url : "/components/reservation/reservation-ajax_utf-8.jsp",
				data : "load_calendar_content=true&date_from=" + dateFrom
						+ "&time_from=" + startTime + "&time_to=" + finishTime
						+ "&res_id=" + parseInt(resId),
				success : function(data) {
					$("#pristupnost1").html($.trim(data));
				},
				async : false
			});

			showHideTab('2');
		} else {
			window.setTimeout(showFirstTab, 50);
		}
	}

	function showFirstTab() {
		showHideTab('1');
	}

	function doOK() {
		document.reservationForm.bSaveReservation.click();
	}
	window.resizeTo(700, 590);

	$(document).ready(function() {
		showHideTab('1');
	});
</script>

<%
if (request.getParameter("bSaveReservation")!=null)
	pageContext.include("/sk/iway/iwcm/components/reservation/ReservationAjax.action");
%>

<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction"/>

<%
Identity user = UsersDB.getCurrentUser(request);
if (actionBean.getReservation()==null || actionBean.getReservation().getReservationId()<1)
{
	if (actionBean.getReservation()==null) actionBean.setReservation(new ReservationBean());
	
	if (user != null)
	{
		if (Tools.isEmpty(actionBean.getReservation().getName())) actionBean.getReservation().setName(user.getFirstName());
		if (Tools.isEmpty(actionBean.getReservation().getSurname()))  actionBean.getReservation().setSurname(user.getLastName());
		if (Tools.isEmpty(actionBean.getReservation().getEmail()))  actionBean.getReservation().setEmail(user.getEmail());
	}
	
	if (actionBean.getReservation().getDateFrom()==null) actionBean.getReservation().setDateFrom(new Date());
	if (actionBean.getReservation().getDateTo()==null) actionBean.getReservation().setDateTo(new Date());
}

List<ReservationObjectBean> selectedObjects = new ArrayList<ReservationObjectBean>();
if(Tools.isNotEmpty(request.getParameter("showObjectIds")))
{
	String[] selectedObjectsIds = request.getParameter("showObjectIds").split(",");
	for(String s : selectedObjectsIds)
	{
		int objectId = Tools.getIntValue(s, -1);
		selectedObjects.add(ReservationManager.getReservationObject(objectId));
	}
}
else
	selectedObjects = actionBean.getAllReservationObjects();
%>
<div class="mainTab">
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst">
				<a href="javascript:void(0)" onclick="showHideTab('1');" id="tabLink1">
					<iwcm:text key="components.docman.zakladneUdaje"/>
				</a>
			</li>
			<li class="last">
				<a href="javascript:void(0)" onclick="loadCalendarContent(true);" id="tabLink2">
					<iwcm:text key="components.docman.pristupnost"/>
				</a>
			</li>
		</ul>
	</div>
	<div class="tab-pane toggle_content" id="tab-pane-1">
		<div class="tab-page" id="tabMenu1" style="display: block;">	
		<iwcm:stripForm name="reservationForm" action="<%=PathFilter.getOrigPath(request)%>" method="post" beanclass="sk.iway.iwcm.components.reservation.ReservationAjaxAction" id="reservationForm">
			<stripes:errors />
		
			<table border="0">		
				<tr id="selectObjectTrId">
					<th><label for="reservationObjectId1"><iwcm:text key="components.reservation.reservation_list.object"/></label></th>
					<td>
						<stripes:select name="reservation.reservationObjectId" class="required" id="reservationObjectId1" onchange="loadReservation();">
							<stripes:option value=""><iwcm:text key="components.reservation.addReservation.chooseObject"/></stripes:option>
							<stripes:options-collection collection="<%=selectedObjects%>" label="name" value="reservationObjectId" title="Objekty"/>
						</stripes:select>
					</td>
				</tr>
				<tr>
					<th><label for="dateFrom"><iwcm:text key="components.reservation.addReservation.date_from"/></label></th>
					<td>
						<stripes:text class="date required" id="dateFrom" maxlength="10" size="11" name="reservation.dateFrom" onblur="checkDate(document.getElementById('reservationForm').dateFrom); if (document.getElementById('reservationForm').dateTo.value=='' || $('.dateToCal').is(':hidden')) document.getElementById('reservationForm').dateTo.value=document.getElementById('reservationForm').dateFrom.value; checkForm.checkField(document.getElementById('reservationForm').dateTo); reservationObjectChange(); return false;" />
						<img alt="" src="/components/calendar/calendar.gif" class="calendarIcon" onclick='showCalendar(this, document.getElementById("dateFrom"), "dd.mm.yyyy", null, 1)' />
					</td>		
					<th align="right"><label for="startTimeId1"><iwcm:text key="components.reservation.addReservation.time_from"/></label></th>
					<td>
						<stripes:select onchange="timeChange()" name="reservation.startTime" id="startTimeId1">
						</stripes:select>						
					</td>	
				</tr>
				
				<tr>
					<th><label for="dateTo"><iwcm:text key="components.reservation.addReservation.date_to"/></label></th>
					<%System.out.println(">>> max: "+actionBean.getReservationObject().getMaxReservations());%>
					<td>
						<stripes:text class="date required" id="dateTo" maxlength="10" size="11" name="reservation.dateTo" onblur="checkDate(document.getElementById('reservationForm').dateTo);return false;"/>
						<img alt="" src="/components/calendar/calendar.gif" class="calendarIcon dateToCal" onclick='showCalendar(this, document.getElementById("dateTo"), "dd.mm.yyyy", null, 1)' />
					</td>
					<th><label for="finishTimeId1"><iwcm:text key="components.reservation.addReservation.time_to"/></label></th>
					<td id="trFinishTime">
						<stripes:select name="reservation.finishTime" id="finishTimeId1">
						</stripes:select>
					</td>	
				</tr>
				<tr>
					<th><label for="reservationNameId1"><iwcm:text key="components.reservation.addReservation.name"/></label></th>
					<td colspan="3"><stripes:text id="reservationNameId1" name="reservation.name" class="required" size="26"/></td>
				</tr>
				
				<tr>
					<th><label for="surnameId1"><iwcm:text key="components.reservation.addReservation.surname"/></label></th>
					<td colspan="3"><stripes:text name="reservation.surname" id="surnameId1" class="required" size="26" /></td>
				</tr>
				<tr>
					<th><label for="emailId1"><iwcm:text key="components.reservation.addReservation.email"/></label></th>
					<td colspan="3"><stripes:text name="reservation.email" id="emailId1" class="required" size="26" /></td>
				</tr>
				<tr>
					<th><label for="phoneNumber"><iwcm:text key="components.reservation.addReservation.phoneNumber"/></label></th>
					<td colspan="3"><stripes:text name="reservation.phoneNumber" id="phoneNumber" size="26" /></td>
				</tr>
				<tr>
					<th><label for="purposeId1"><iwcm:text key="components.reservation.addReservation.purpose"/></label></th>
					<td colspan="3"><stripes:textarea name="reservation.purpose" id="purposeId1" class="required" rows="3" cols="40" /></td>
				</tr>				
				<tr>
					<td><stripes:submit name="bSaveReservation" value="Save" style="display: none;"/></td>
				</tr>
				
			</table>
		</iwcm:stripForm>
		
		<div id="reservationTimes"></div>
		<p style="margin-bottom: 0px;"><iwcm:text key="components.reservation.reservation_list"/>:</p>
		<style>
			table.displaytagTable td { font-size: 11px;}
		</style>
		<div style="height: 100px; overflow: auto;" id="reservationList"></div>
		
		</div>
		<div class="tab-page" id="tabMenu2">	
			<table id="pristupnost1"></table>
			<table border="0" cellpadding="0" cellspacing="0" width="600">
			   <tr>
			      <td valign="top" align="left">&nbsp;</td>
			      <td valign="top" align="left">
			         <strong><iwcm:text key="schodzky_edit_time.day"/>:</strong>
			         <input name="date" maxlength="10" size="8" id="dateId" onblur="checkDate(document.getElementById('dateId')); return false" class="input" type="text">&nbsp;<img class="calendarIcon" src="/components/calendar/calendar.gif" style="cursor: pointer; vertical-align: middle;" onclick='showCalendar(this, document.getElementById("dateId"), "dd.mm.yyyy")' alt="">
			        
			         <label for="startTimeId"><strong><app:text key="schodzky_edit_time.from"/></strong></label>
			        	<select id="startTimeId" class="form-control">
							<option value="00:30">00:30</option>
							<option value="01:00">01:00</option>
							<option value="01:30">01:30</option>
							<option value="02:00">02:00</option>
							<option value="02:30">02:30</option>
						
							<option value="03:00">03:00</option>
							<option value="03:30">03:30</option>
							<option value="04:00">04:00</option>
							<option value="04:30">04:30</option>
							<option value="05:00">05:00</option>
							<option value="05:30">05:30</option>
						
							<option value="06:00">06:00</option>
							<option value="06:30">06:30</option>
							<option value="07:00">07:00</option>
							<option value="07:30">07:30</option>
							<option value="08:00">08:00</option>
							<option value="08:30">08:30</option>
						
							<option value="09:00">09:00</option>
							<option value="09:30">09:30</option>
							<option value="10:00">10:00</option>
							<option value="10:30">10:30</option>
							<option value="11:00">11:00</option>
							<option value="11:30">11:30</option>
						
							<option value="12:00">12:00</option>
							<option value="12:30">12:30</option>
							<option value="13:00">13:00</option>
							<option value="13:30">13:30</option>
							<option value="14:00">14:00</option>
							<option value="14:30">14:30</option>
						
							<option value="15:00">15:00</option>
							<option value="15:30">15:30</option>
							<option value="16:00">16:00</option>
							<option value="16:30">16:30</option>
							<option value="17:00">17:00</option>
							<option value="17:30">17:30</option>
						
							<option value="18:00">18:00</option>
							<option value="18:30">18:30</option>
							<option value="19:00">19:00</option>
							<option value="19:30">19:30</option>
							<option value="20:00">20:00</option>
							<option value="20:30">20:30</option>
						
							<option value="21:00">21:00</option>
							<option value="21:30">21:30</option>
							<option value="22:00">22:00</option>
							<option value="22:30">22:30</option>
							<option value="23:00">23:00</option>
							<option value="23:30">23:30</option>				         	
			         </select>
			         
			         <label for="endTimeId"><strong><app:text key="schodzky_edit_time.till"/></strong></label>
			        	<select id="endTimeId" class="form-control">
							<option value="00:30">00:30</option>
							<option value="01:00">01:00</option>
							<option value="01:30">01:30</option>
							<option value="02:00">02:00</option>
							<option value="02:30">02:30</option>
						
							<option value="03:00">03:00</option>
							<option value="03:30">03:30</option>
							<option value="04:00">04:00</option>
							<option value="04:30">04:30</option>
							<option value="05:00">05:00</option>
							<option value="05:30">05:30</option>
						
							<option value="06:00">06:00</option>
							<option value="06:30">06:30</option>
							<option value="07:00">07:00</option>
							<option value="07:30">07:30</option>
							<option value="08:00">08:00</option>
							<option value="08:30">08:30</option>
						
							<option value="09:00">09:00</option>
							<option value="09:30">09:30</option>
							<option value="10:00">10:00</option>
							<option value="10:30">10:30</option>
							<option value="11:00">11:00</option>
							<option value="11:30">11:30</option>
						
							<option value="12:00">12:00</option>
							<option value="12:30">12:30</option>
							<option value="13:00">13:00</option>
							<option value="13:30">13:30</option>
							<option value="14:00">14:00</option>
							<option value="14:30">14:30</option>
						
							<option value="15:00">15:00</option>
							<option value="15:30">15:30</option>
							<option value="16:00">16:00</option>
							<option value="16:30">16:30</option>
							<option value="17:00">17:00</option>
							<option value="17:30">17:30</option>
						
							<option value="18:00">18:00</option>
							<option value="18:30">18:30</option>
							<option value="19:00">19:00</option>
							<option value="19:30">19:30</option>
							<option value="20:00">20:00</option>							
							<option value="20:30">20:30</option>
						
							<option value="21:00">21:00</option>
							<option value="21:30">21:30</option>
							<option value="22:00">22:00</option>
							<option value="22:30">22:30</option>
							<option value="23:00">23:00</option>
							<option value="23:30">23:30</option>				         	
			         </select>
			         
			         <input type="submit" name="submit" value="<iwcm:text key="schodzky_edit_time.set"/>" styleClass="button" onclick="loadCalendarContent(false);"/>
			      </td>
			   </tr>
			</table>
		</div>
	</div>
</div>
<jsp:include page="/components/bottom-public.jsp"/>