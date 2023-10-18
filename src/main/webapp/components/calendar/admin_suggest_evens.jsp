<%@page import="sk.iway.iwcm.users.SettingsAdminDB"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.calendar.*,java.util.*" %>

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

<%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>

<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.calendar.CalendarActionBean"/>

<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(lng);
	Identity user = UsersDB.getCurrentUser(request);
	if (user == null) return;
	if (Tools.isNotEmpty(request.getParameter("odporucujem")))
	{
		pageContext.include("/sk/iway/iwcm/components/calendar/Calendar.action");

		if (request.getAttribute("saveOk") != null)
		{
		}
	}
	Calendar dnes = Calendar.getInstance();
	request.setAttribute("showApprove", "1");
	request.removeAttribute("suggest");
	request.setAttribute("calStart", Tools.formatDate(dnes.getTimeInMillis()));
	List<CalendarDetails> events = CalendarDB.getEvents(request);
	events = SettingsAdminDB.filterBeansByUserAllowedCategories(events,"type",user,"cmp_calendar");
	request.setAttribute("events", events);
	String inputCheckBox = new String("<input class=\"group-checkable\" type=\"checkbox\" id=\"checkAllId\" name=\"checkAllId\" value=\"true\" onclick=\"checkAll();\" />");
%>

<script type="text/javascript">

	helpLink = "components/calendar.jsp&book=components";

	function getAllChecked()
	{
		var odporuceneIds = [];

		$("input[name=checkOperacie]:checked").each(function(){
			odporuceneIds.push($(this).val());
		});

		$("#odporuceneId").val(odporuceneIds.join(","));
	}

	function checkAll(){
		if($('#checkAllId:checked').val() != null){
			$("input[name=checkOperacie]").prop('checked', true).change();
		}else{
			$("input[name=checkOperacie]").prop('checked', false).change();
		}
	}


	$(function(){
        $("input[name=checkOperacie]").on("change", function(){
            if($(this).is(":checked")){
                if($(this).parent().parent().hasClass("checker")){
                    $(this).parent().addClass("checked");
                }
			} else{
                if($(this).parent().parent().hasClass("checker")){
                    $(this).parent().removeClass("checked");
                }
			}
        });
	})
</script>
<div class="row title">
    <h1 class="page-title"><i class="fa icon-calendar"></i><iwcm:text key="menu.calendar"/><i class="fa fa-angle-right"></i><iwcm:text key="calendar.suggest_evens"/></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width full-height-content-ignore-block">

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">
			<iwcm:stripForm id="new" name="odporucanieForm" method="POST" action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.components.calendar.CalendarActionBean">
				<stripes:errors/>
				<div class="col-md-3 col-sm-6">
					<div class="form-group">
						<stripes:label for="searchCislo"><iwcm:text key="calendar.oznacene_udalosti"/></stripes:label>

						<stripes:select name="odporucitAkcia" id="odporucitAkcia" class="form-control">
							<stripes:option value="1" selected="true"><iwcm:text key="calendar.odporucit"/></stripes:option>
							<stripes:option value="0"><iwcm:text key="calendar.zrusit_odporucenie"/></stripes:option>
						</stripes:select>
					</div>
				</div>
				<div class="col-md-1 col-sm-6">
					<div class="form-group">
						<label class="control-label display-block">&nbsp;</label>
						<input type="hidden" name="odporuceneId" id="odporuceneId" />
						<stripes:submit class="button50" name="odporucujem" onclick="getAllChecked();"><iwcm:text key="calendar.vykonaj"/></stripes:submit>
					</div>
				</div>
			</iwcm:stripForm>
		</div>
	</div>
</div>

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
                    sortable: false
                },
                {
                    targets: [0],
                    title: '<iwcm:text key="calendar.name"/>',
                    className: "not-allow-hide row-controller-and-settings text-left",
                    searchType: "checkAll"
                },
                {
                    targets: [1],
                    title: '<iwcm:text key="calendar.begin"/>',
                    className: "not-allow-hide",
                    searchType: "input",
					orderData: [6]
                },
                {
                    targets: [2],
                    title: '<iwcm:text key="calendar.end"/>',
                    className: "not-allow-hide",
                    searchType: "input",
                    orderData: [7]
                },
                {
                    targets: [3],
                    title: '<iwcm:text key="calendar.type"/>',
                    className: "not-allow-hide",
                    searchType: "input"
                },
                {
                    targets: [4],
                    title: '<iwcm:text key="calendar.creator"/>',
                    className: "not-allow-hide",
                    searchType: "input"
                },
                {
                    targets: [5],
                    title: '<iwcm:text key="calendar.odporucana"/>',
                    className: "not-allow-hide",
                    searchType: "input"
                },
                {
                    targets: [6],
                    title: '<iwcm:text key="calendar.begin"/>',
                    className: "not-allow-hide",
                    visible: false
                },
                {
                    targets: [7],
                    title: '<iwcm:text key="calendar.end"/>',
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

<%@ include file="/admin/datatables_show_columns_from_settings.jsp" %>

<div class="table-ninja-style-wrapper">
	<display:table uid="event" name="events" class="table table-striped table-bordered nowrap table-ninja-style" style="width: 100%;" pagesize="999999999">
		<display:column titleKey="calendar.name">
			<div class="row-two-block not-checkbox">
				<div class="row-two-block-numbers">
					<a class="doc-title-link-edit" href="javascript:openPopupDialogFromLeftMenu('/admin/editevent.do?event=<jsp:getProperty name="event" property="calendarId"/>');">
						<i class="fa fa-pencil"></i>
						<jsp:getProperty name="event" property="title"/>
					</a>
				</div>
				<div class="row-two-block-tools" style="width:14px;">
					<input type="checkbox" class="checkOperacie" name="checkOperacie" value="<%=((CalendarDetails)event).getCalendarId()%>">
				</div>
			</div>
		</display:column>

		<display:column titleKey="calendar.begin" sortable="true" sortProperty="from" property="fromString"/>
		<display:column titleKey="calendar.end" sortable="true" sortProperty="to" property="toString"/>
		<display:column titleKey="calendar.type" sortable="true" property="type"/>

		<display:column titleKey="calendar.creator" sortable="true">
			<%
				UserDetails creator = UsersDB.getUser(((CalendarDetails)event).getCreatorId());
				String name = prop.getText("calendar_edit.configType.neznamy");
				if(creator != null)
					name = creator.getFullName();
				out.print(name);
			%>
		</display:column>

		<display:column titleKey="calendar.odporucana" sortable="true" style="text-align: center;">
			<%
				if(((CalendarDetails)event).isSuggest())
				{
			%><img src="accept.png" alt="<iwcm:text key="calendar.odporucana"/>" title="<iwcm:text key="calendar.odporucana"/>" /><%
			}
		%>
		</display:column>

		<display:column titleKey="calendar.begin" sortable="true" property="from.time"/>
		<display:column titleKey="calendar.end" sortable="true" property="to.time"/>

	</display:table>
</div>

<div>
	<iwcm:text key="editor.groupslist.check_all_checboxes"/>
</div>

<%@ include file="/admin/layout_bottom.jsp" %>
