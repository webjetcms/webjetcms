<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_server_monitoring"/>
<%@page import="sk.iway.iwcm.system.monitoring.*"%>

<%@page import="sk.iway.iwcm.system.cluster.ClusterDB"%>
<%@page import="sk.iway.iwcm.database.SimpleQuery"%>
<c:catch var="exc">
<%@ include file="/admin/layout_top.jsp" %>

<script type="text/javascript" src="/components/form/check_form.js"></script>
<script type="text/javascript">helpLink = "components/server_monitoring.jsp&book=admin";</script>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-equalizer"></i><iwcm:text key="components.monitor.monitoring"/><i class="fa fa-angle-right"></i><iwcm:text key="components.monitor.sql_monitoring"/></h1>
</div>

<c:if test='<%=!Constants.getBoolean("serverMonitoringEnablePerformance")%>'>
	<div class="info_box"><iwcm:text key="components.monitor.disabled.performance"/></div>
</c:if>
<c:if test='<%=Constants.getBoolean("serverMonitoringEnablePerformance")%>'>
<%
	if ("true".equals(Tools.getRequestParameter(request, "reset")))
	{
		ExecutionTimeMonitor.resetSqlMeasurements();
		request.setAttribute("resetSuccessful", true);
	}

	List<ExecutionEntry> stats = ExecutionTimeMonitor.statsForSqls();

	boolean requestsStatsOnAnotherNode = Tools.getRequestParameter(request, "node") != null && !Constants.getString("clusterMyNodeName").equals(Tools.getRequestParameter(request, "node"));
	if (requestsStatsOnAnotherNode)
		stats = MonitoringDB.getSqlStatsFor(Tools.getRequestParameter(request, "node"));

	if (Tools.getRequestParameter(request, "nameFilter") != null)
	{
		final String searchedName = Tools.getRequestParameter(request, "nameFilter");
		SelectionFilter<ExecutionEntry> filter = new SelectionFilter<ExecutionEntry>(){

			public boolean fullfilsConditions(ExecutionEntry candidate)
			{
				return candidate.getName().toLowerCase().contains(searchedName.toLowerCase());
			}
		};
		stats = Tools.filter(stats, filter);
	}
	if(Tools.getRequestParameter(request, "countExecFrom") != null)
	{
		final int countFrom = Tools.getIntValue(Tools.getRequestParameter(request, "countExecFrom"), 1);
		final int countTo = Tools.getIntValue(Tools.getRequestParameter(request, "countExecTo"), -1);
		SelectionFilter<ExecutionEntry> filter = new SelectionFilter<ExecutionEntry>(){

			public boolean fullfilsConditions(ExecutionEntry candidate)
			{
				if(countTo == -1)
					return candidate.getNumberOfExecutions() >= countFrom;
				else
					return candidate.getNumberOfExecutions() >= countFrom && candidate.getNumberOfExecutions() <= countTo;
			}
		};
		stats = Tools.filter(stats, filter);
	}
	if(Tools.getRequestParameter(request, "timeExecFrom") != null)
	{
		final int timeFrom = Tools.getIntValue(Tools.getRequestParameter(request, "timeExecFrom"), 1);
		final int timeTo = Tools.getIntValue(Tools.getRequestParameter(request, "timeExecTo"), -1);
		SelectionFilter<ExecutionEntry> filter = new SelectionFilter<ExecutionEntry>(){

			public boolean fullfilsConditions(ExecutionEntry candidate)
			{
				if(timeTo == -1)
					return candidate.getAverageExecutionTime() >= timeFrom;
				else
					return candidate.getAverageExecutionTime() >= timeFrom && candidate.getAverageExecutionTime() <= timeTo;
			}
		};
		stats = Tools.filter(stats, filter);
	}
	if(Tools.getRequestParameter(request, "timeSlowestFrom") != null)
	{
		final int timeFrom = Tools.getIntValue(Tools.getRequestParameter(request, "timeSlowestFrom"), 1);
		final int timeTo = Tools.getIntValue(Tools.getRequestParameter(request, "timeSlowestTo"), -1);
		SelectionFilter<ExecutionEntry> filter = new SelectionFilter<ExecutionEntry>(){

			public boolean fullfilsConditions(ExecutionEntry candidate)
			{
				if(timeTo == -1)
					return candidate.getMaximumExecutionTime() >= timeFrom;
				else
					return candidate.getMaximumExecutionTime() >= timeFrom && candidate.getMaximumExecutionTime() <= timeTo;
			}
		};
		stats = Tools.filter(stats, filter);
	}

	request.setAttribute("stats", stats);

%>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.monitor.components_monitoring_filter"/>
			</a>
		</li>
		<li>
			<a href="#tabMenu2" data-toggle="tab">
				Reset
			</a>
		</li>
	</ul>

	<div class="tab-content">

	    <div id="tabMenu1" class="tab-pane active">

			<form action="<%=PathFilter.getOrigPath(request)%>" method="get">

					<c:if test="<%=ClusterDB.isServerRunningInClusterMode()%>">
					<div class="col-lg-3 col-sm-6">
						<div class="form-group">
							<iwcm:text key="components.monitoring.nodeName"/>:<br>
							<%@include file="/components/server_monitoring/_node_select.jsp" %>
						</div>
					</div>
					</c:if>

					<div class="col-lg-3 col-sm-6">
						<div class="form-group">
							<label class="form-label" for="nameFilter"><iwcm:text key="components.monitor.sql_part"/>:</label>
							<input type="text" value="${param.nameFilter}" name="nameFilter" class="minLen3 form-control" id="nameFilter"/>
						</div>
					</div>
					<div class="col-lg-3 col-sm-6">
						<div class="form-group">
							<label class="form-label"><iwcm:text key="components.monitor.count_of_exec"/> - </label>
							<label class="form-label" for="countExecFrom"><iwcm:text key="components.banner.from_date"/>:</label>
							<input type="text" value="${param.countExecFrom}" name="countExecFrom" class="form-control" id="countExecFrom"/>
							<label class="form-label" for="countExecTo"><iwcm:text key="components.banner.to_date"/>:</label>
							<input type="text" value="${param.countExecTo}" name="countExecTo" class="form-control" id="countExecTo"/>
						</div>
					</div>
					<div class="col-lg-3 col-sm-6">
						<div class="form-group">
							<label class="form-label"><iwcm:text key="components.monitor.time_of_exec"/> - </label>
							<label class="form-label" for="timeExecFrom"><iwcm:text key="components.banner.from_date"/>:</label>
							<input type="text" value="${param.timeExecFrom}" name="timeExecFrom" class="form-control" id="timeExecFrom"/>
							<label class="form-label" for="timeExecTo"><iwcm:text key="components.banner.to_date"/>:</label>
							<input type="text" value="${param.timeExecTo}" name="timeExecTo" class="form-control" id="timeExecTo"/>
						</div>
					</div>
					<div class="col-lg-3 col-sm-6">
						<div class="form-group">
							<label class="form-label"><iwcm:text key="components.monitoring.component_slowest_one"/> - </label>
							<label class="form-label" for="timeSlowestFrom"><iwcm:text key="components.banner.from_date"/>:</label>
							<input type="text" value="${param.timeSlowestFrom}" name="timeSlowestFrom" class="form-control" id="timeSlowestFrom"/>
							<label class="form-label" for="timeSlowestTo"><iwcm:text key="components.banner.to_date"/>:</label>
							<input type="text" value="${param.timeSlowestTo}" name="timeSlowestTo" class="form-control" id="timeSlowestTo" />
						</div>
					</div>
					<div class="col-lg-12">
						<div class="form-group">
							<input type="submit" class="btn green" value="<iwcm:text key="user.prop_search.search"/>" />
						</div>
					</div>

			</form>

		</div>

	    <div id="tabMenu2" class="tab-pane">

			<form action="<%=PathFilter.getOrigPath(request)%>" method="get">

				<div class="col-lg-12">
					<div class="form-group">
						<input type="hidden" value="true" name="reset" />
						<input type="submit" value="Reset" class="btn green" />
					</div>
				</div>

			</form>

		</div>

	</div>
</div>

<c:if test="${resetSuccessful}">
	<p><iwcm:text key="components.monitor.reset_ok"/></p>
</c:if>
<c:if test="<%=requestsStatsOnAnotherNode%>">
	<% Date when = new SimpleQuery().forDate(
		"SELECT MAX(created_at) FROM cluster_monitoring WHERE node = ? AND type = ? ",
		Constants.getString("clusterMyNodeName"), MonitoringDB.TYPE_SQL);  %>
	<p>
		<iwcm:text key="components.monitoring.date_when" param1="${param.node}" param2="<%=Tools.formatDateTime(when)%>" />
	</p>
	<p id="nodeRefresh">
		<input type="button" value="<iwcm:text key="components.monitoring.request_new_data"/>"
		onclick="$('#nodeRefresh').load('/components/server_monitoring/_demand_node_refresh.jsp?node=${param.node}')" class="button150x20"/>
	</p>
</c:if>

<logic:notEmpty name="stats">
<style type="text/css">
table.sort_table tr td { text-align: right; }
</style>
<display:table uid="stat" name="stats" export="true" pagesize="20" requestURI="<%=PathFilter.getOrigPath(request)%>" excludedParams="docid" class="sort_table">
	<display:setProperty name="export.excel.filename" value="export.xls" />
	<display:setProperty name="export.csv.filename" value="export.csv" />
	<display:setProperty name="export.xml.filename" value="export.xml" />
	<display:setProperty name="export.pdf.filename" value="export.pdf" />

	<display:column titleKey="components.monitoring.component_executions" sortable="true" property="numberOfExecutions" decorator="sk.iway.displaytag.NumbersDecorator" />
	<display:column titleKey="components.monitoring.component_execution_time" sortable="true" property="averageExecutionTime" decorator="sk.iway.displaytag.NumbersDecorator" />
	<display:column titleKey="components.monitoring.component_slowest_one" sortable="true" property="maximumExecutionTime" decorator="sk.iway.displaytag.NumbersDecorator" />
	<display:column titleKey="components.monitoring.component_fastest_one" sortable="true" property="minimumExecutionTime" decorator="sk.iway.displaytag.NumbersDecorator" />
	<display:column titleKey="components.monitoring.component_total_time" sortable="true" property="totalTimeSpentOnServingThisComponent" decorator="sk.iway.displaytag.NumbersDecorator" />
	<display:column titleKey="SQL" sortable="true" property="name" style="text-align: left;" />
</display:table>
</logic:notEmpty>
<logic:empty name="stats">
	<iwcm:text key="components.monitoring.no_results"/>
</logic:empty>

<div class="content-wrapper">
<c:if test='<%=!Constants.getBoolean("serverMonitoringEnableJPA")%>'>
	<iwcm:text key="components.monitor.disabledJPAwarn"/>
</c:if>
</div>

</c:if>
</c:catch>
<c:if test="${exc != null}">
	<iwcm:text key="${exc.message}" />
	<% ((Exception)pageContext.getAttribute("exc")).printStackTrace();%>
</c:if>
<%@ include file="/admin/layout_bottom.jsp" %>
