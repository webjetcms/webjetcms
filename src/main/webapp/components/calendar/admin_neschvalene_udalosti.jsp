<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.users.SettingsAdminDB"%>
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
<iwcm:checkLogon admin="true" perms="cmp_calendar"/>

<%@ include file="/admin/layout_top.jsp" %>

<%@page import="sk.iway.iwcm.calendar.CalendarDB"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.calendar.CalendarDetails"%>
<%@page import="sk.iway.iwcm.calendar.EventTypeDB"%>
<%@page import="sk.iway.iwcm.calendar.EventTypeDetails"%>

<script type="text/javascript">
<!--
	helpLink = "components/calendar.jsp&book=components";
//-->
</script>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-calendar"></i><iwcm:text key="menu.calendar"/><i class="fa fa-angle-right"></i><iwcm:text key="calendar.neschvalene_udalosti"/></h1>
</div>

<%
	Identity user = UsersDB.getCurrentUser(request);
	if (user == null) return;
	List<CalendarDetails> events = CalendarDB.getNotApprovedEvents(user.getUserId());
	events = SettingsAdminDB.filterBeansByUserAllowedCategories(events,"type",user,"cmp_calendar");
	request.setAttribute("events", events);
%>

<script type="text/javascript">

    $(document).ready(function() {

        datatable = $("#event");

        datatable.DataTable( {
            initComplete: function(oSettings, json) {
                datatables_globalConfig.fn_initComplete(this.api());
            },
            fnDrawCallback: function(oSettings) {
                datatables_globalConfig.fn_drawCallback(this.api());
            },
            columnDefs: [
                {
                    targets: "_all",
                    sortable: false,
					searchType: 'input'
                },
                {
                    targets: [0],
                    title: '<iwcm:text key="calendar.name"/>',
                    className: "not-allow-hide row-controller-and-settings text-left"
                },
                {
                    targets: [1],
                    title: '<iwcm:text key="calendar.begin"/>',
                    className: "not-allow-hide"
                },
                {
                    targets: [2],
                    title: '<iwcm:text key="calendar.end"/>',
                    className: "not-allow-hide"
                },
                {
                    targets: [3],
                    title: '<iwcm:text key="calendar.type"/>',
                    className: "not-allow-hide"
                },
                {
                    targets: [4],
                    title: '<iwcm:text key="components.table.column.tools"/>',
                    className: "not-allow-hide",
					visible: false
                }
            ],
            serverSide: false,
            bStateSave: false,
            info: true,
            searching: true,
            bFilter: true,
            paging: true,
            pageLength: 100,
            pagingType: datatables_globalConfig.pagingType,
            ordering: true,
            order: [[2, 'asc']],
            orderCellsTop: false,
            responsive: true,
            scrollX: "100%",
            scrollY: "100%",
            scrollCollapse:true,
            fixedColumns: {
                leftColumns: 2
            },
            language: datatables_globalConfig.language,
            dom: datatables_globalConfig.dom,
            lengthMenu: datatables_globalConfig.lengthMenu,
            buttons: datatables_globalConfig.buttons
        } );

    } );

</script>

<script>
    function showControllerRow(data) {
        var buttons = data[4];
        return datatables_globalConfig.fn_createButtons(buttons);
    }
</script>

<%@ include file="/admin/datatables_show_columns_from_settings.jsp" %>

<div class="table-ninja-style-wrapper">
	<display:table uid="event" name="events" class="table table-striped table-bordered nowrap table-ninja-style" style="width: 100%;" pagesize="999999999">
		<display:column titleKey="calendar.name">
			<div class="row-two-block not-checkbox">
				<div class="row-two-block-numbers">
					<a class="doc-title-link-edit" href="javascript:popup('/components/calendar/admin_event_to_approve.jsp?calendarId=<jsp:getProperty name="event" property="calendarId"/>', 550, 350);">
						<i class="fa fa-pencil"></i>
						<jsp:getProperty name="event" property="title"/>
					</a>
				</div>
				<div class="row-two-block-tools" style="width:14px;">
					<div class="btn-group">
						<a class="show-doc-controller" href="javascript:;"><i class="fa fa-cog"></i></a>
					</div>
				</div>
			</div>
		</display:column>

		<display:column titleKey="calendar.begin" sortable="true" sortProperty="from" property="fromString"></display:column>

		<display:column titleKey="calendar.end" sortable="true" sortProperty="to" property="toString"></display:column>

		<display:column titleKey="calendar.type" sortable="true">
			<%
				EventTypeDetails eventType = EventTypeDB.getTypeById(((CalendarDetails)event).getTypeId());
				out.print(eventType.getName());
			%>

		</display:column>

		<display:column titleKey="components.table.column.tools">

			<a class="btn btn-default"  href="javascript:popup('/components/calendar/admin_event_to_approve.jsp?calendarId=<jsp:getProperty name="event" property="calendarId"/>', 550, 350);">
				<i class="fa fa-exclamation-triangle"></i> <iwcm:text key="users.import.authorize"/>
			</a>

		</display:column>

	</display:table>
</div>

<%@ include file="/admin/layout_bottom.jsp" %>