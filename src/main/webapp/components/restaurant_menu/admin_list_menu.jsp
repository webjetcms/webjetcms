<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MenuDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MenuBean"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>

<iwcm:menu notName="cmp_restaurant_menu">
	<%response.sendRedirect("/admin/403.jsp");
		if (1==1) return;%>
</iwcm:menu>
<%@ include file="/admin/layout_top.jsp" %>

<script type="text/javascript">
	function setWeek() {
		window.open("/components/restaurant_menu/admin_list_menu.jsp?week="+document.getElementById('weekId').value,"_self");
	}

	function confirmDelete(menuId)
	{
		if(window.confirm('<iwcm:text key="components.dictionary.delete_confirm"/>') == true)
		{
			document.actionForm.elements["menuToDelete"].value=menuId;
			document.actionForm.elements["week"].value=document.getElementById('weekId').value;
			document.actionForm.submit();
		}
	}

	function changePriority(menuId, priority)
	{
		document.actionForm2.elements["menuToUpdate"].value=menuId;
		document.actionForm2.elements["priority"].value=priority;
		document.actionForm2.elements["week"].value=document.getElementById('weekId').value;
		document.actionForm2.submit();
	}
</script>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);

if (request.getParameter("deleteFromMenu")!=null)
	pageContext.include("/sk/iway/iwcm/components/restaurant_menu/Restaurant.action");

if (request.getParameter("changePriority")!=null)
	pageContext.include("/sk/iway/iwcm/components/restaurant_menu/Restaurant.action");

List<MenuBean> ponList = new ArrayList<MenuBean>();
List<MenuBean> utoList = new ArrayList<MenuBean>();
List<MenuBean> strList = new ArrayList<MenuBean>();
List<MenuBean> stvList = new ArrayList<MenuBean>();
List<MenuBean> piaList = new ArrayList<MenuBean>();
List<MenuBean> sobList = new ArrayList<MenuBean>();
List<MenuBean> nedList = new ArrayList<MenuBean>();

String week;
if(request.getParameter("week")!=null && request.getParameter("week").matches("[0-9]+-[0-9]+")) week=request.getParameter("week");
else week=getWeek();
int w=Integer.parseInt(week.substring(0, week.indexOf("-")));
int y=Integer.parseInt(week.substring(week.indexOf("-")+1, week.length()));

Calendar cal = Calendar.getInstance();
cal.set(Calendar.YEAR, y);
cal.set(Calendar.WEEK_OF_YEAR, w);
cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
Date day = cal.getTime();

ponList = MenuDB.getInstance().getByDate(day);
cal.add(Calendar.DATE, 1);
day = cal.getTime();
utoList = MenuDB.getInstance().getByDate(day);
cal.add(Calendar.DATE, 1);
day = cal.getTime();
strList = MenuDB.getInstance().getByDate(day);
cal.add(Calendar.DATE, 1);
day = cal.getTime();
stvList = MenuDB.getInstance().getByDate(day);
cal.add(Calendar.DATE, 1);
day = cal.getTime();
piaList = MenuDB.getInstance().getByDate(day);
cal.add(Calendar.DATE, 1);
day = cal.getTime();
sobList = MenuDB.getInstance().getByDate(day);
cal.add(Calendar.DATE, 1);
day = cal.getTime();
nedList = MenuDB.getInstance().getByDate(day);

cal.add(Calendar.DATE, -6);
SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");

request.setAttribute("ponList", ponList);
request.setAttribute("utoList", utoList);
request.setAttribute("strList", strList);
request.setAttribute("stvList", stvList);
request.setAttribute("piaList", piaList);
request.setAttribute("sobList", sobList);
request.setAttribute("nedList", nedList);
%>
<%!
	public String getWeek()
	{
		String week = Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
		String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
    	return week+"-"+year;
 	}
%>

<style type="text/css">
    select {
        resize: vertical;
    }

    div#ui-datepicker-div{
    	width:220px;
    }

	.datepicker table tbody tr:hover {
		background: #dff0d8;
	}
</style>

<div class="row title">
	<h1 class="page-title"><i class="fa icon-book-open"></i><iwcm:text key="components.restaurant_menu.title"/><i class="fa fa-angle-right"></i><iwcm:text key="components.restaurant_menu.listMenu"/></h1>
</div>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active"><a id="tabLink1" href="#tabMenu1" data-toggle="tab"><iwcm:text key="components.filter"/></a></li>
	</ul>

	<div class="tab-content">
		<div id="tabMenu1" class="tab-pane active">
			<div class="col-md-2 col-sm-6">
				<div class="form-group">
					<label for="weekId"><iwcm:text key="components.restaurant_menu.vyberTyzden"/>:</label>
					<input type="text" name="week" id="weekId" class="form-control date-picker" value="<%=week%>"/>
				</div>
			</div>
			<div class="col-md-2 col-sm-6">
				<div class="form-group">
					<label class="control-label display-block">&nbsp;</label>
					<input type="button" class="btn green" value="<iwcm:text key="components.restaurant_menu.select"/>" onclick="setWeek()">
				</div>
			</div>
		</div>
	</div>







<form action="<%=PathFilter.getOrigPath(request)%>" name="actionForm" style="display: none;">
	<input type="hidden" name="deleteFromMenu" value="true" />
	<input type="hidden" name="menuToDelete" value="" />
	<input type="hidden" name="week" value="" />
</form>

<form action="<%=PathFilter.getOrigPath(request)%>" name="actionForm2" style="display: none;">
	<input type="hidden" name="changePriority" value="true" />
	<input type="hidden" name="menuToUpdate" value="" />
	<input type="hidden" name="priority" value="" />
	<input type="hidden" name="week" value="" />
</form>
<div class="col-sm-12"><h4><iwcm:text key="components.restaurant_menu.pondelok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h4></div>
<display:table class="sort_table table table-hover table-wj" name="ponList" uid="row" >

    <display:column titleKey="components.restaurant_menu.name" property="meal.name" style="width: 25%;" />
	<display:column titleKey="components.table.column.tools" style="width: 5%;">
	<a href="javascript:confirmDelete(${row.menuId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove"></i></a>
	</display:column>
<display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" style="width: 10%;" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description"  style="width: 20%;" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"></display:column>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
    <display:column titleKey="components.restaurant_menu.poradie">
 		<input type="text" value="${row.priority}" style="width: 40px;" onblur="changePriority(${row.menuId},this.value)" class="form-control" />
 	</display:column>
</display:table>

<div class="col-sm-12"><h4><iwcm:text key="components.restaurant_menu.utorok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h4></div>
<display:table class="sort_table table table-hover table-wjsort_table" name="utoList" uid="row" >

    <display:column titleKey="components.restaurant_menu.name" property="meal.name" style="width: 25%;" />
	<display:column titleKey="components.table.column.tools" style="width: 5%;">
	<a href="javascript:confirmDelete(${row.menuId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove"></i></a>
	</display:column>
<display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory"  style="width: 10%;" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" style="width: 20%;" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"></display:column>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
 	<display:column titleKey="components.restaurant_menu.poradie">
 		<input type="text" value="${row.priority}" style="width: 40px;" onblur="changePriority(${row.menuId},this.value)" class="form-control" />
 	</display:column>

</display:table>

<div class="col-sm-12"><h4><iwcm:text key="components.restaurant_menu.streda"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h4></div>
<display:table class="sort_table table table-hover table-wj" name="strList" uid="row" >

    <display:column titleKey="components.restaurant_menu.name" property="meal.name" style="width: 25%;" />
	<display:column titleKey="components.table.column.tools" style="width: 5%;">
	<a href="javascript:confirmDelete(${row.menuId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove"></i></a>
	</display:column>
<display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" style="width: 10%;" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" style="width: 20%;" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"></display:column>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
 	<display:column titleKey="components.restaurant_menu.poradie">
 		<input type="text" value="${row.priority}" style="width: 40px;" onblur="changePriority(${row.menuId},this.value)" class="form-control" />
 	</display:column>

</display:table>

<div class="col-sm-12"><h4><iwcm:text key="components.restaurant_menu.stvrtok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%></h4></div>
<display:table class="sort_table table table-hover table-wj" name="stvList" uid="row" >

    <display:column titleKey="components.restaurant_menu.name" property="meal.name" style="width: 25%;" />
	<display:column titleKey="components.table.column.tools" style="width: 5%;">
	<a href="javascript:confirmDelete(${row.menuId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove"></i></a>
	</display:column>
<display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory"  style="width: 10%;" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" style="width: 20%;" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"></display:column>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
 	<display:column titleKey="components.restaurant_menu.poradie">
 		<input type="text" value="${row.priority}" style="width: 40px;" onblur="changePriority(${row.menuId},this.value)" class="form-control" />
 	</display:column>

</display:table>

<div class="col-sm-12"><h4><iwcm:text key="components.restaurant_menu.piatok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%></h4></div>
<display:table class="sort_table table table-hover table-wj" name="piaList" uid="row" >

    <display:column titleKey="components.restaurant_menu.name" property="meal.name" style="width: 25%;" />
	<display:column titleKey="components.table.column.tools" style="width: 5%;">
	<a href="javascript:confirmDelete(${row.menuId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove"></i></a>
	</display:column>
<display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" style="width: 10%;" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" style="width: 205%;" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"></display:column>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
 	<display:column titleKey="components.restaurant_menu.poradie">
 		<input type="text" value="${row.priority}" style="width: 40px;" onblur="changePriority(${row.menuId},this.value)" class="form-control" />
 	</display:column>

</display:table>

<div class="col-sm-12"><h4><iwcm:text key="components.restaurant_menu.sobota"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h4></div>
<display:table class="sort_table table table-hover table-wj" name="sobList" uid="row" >

    <display:column titleKey="components.restaurant_menu.name" property="meal.name" style="width: 25%;" />
	<display:column titleKey="components.table.column.tools" style="width: 5%;">
	<a href="javascript:confirmDelete(${row.menuId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove"></i></a>
	</display:column>
<display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" style="width: 10%;" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" style="width: 20%;" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"></display:column>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
 	<display:column titleKey="components.restaurant_menu.poradie">
 		<input type="text" value="${row.priority}" style="width: 40px;" onblur="changePriority(${row.menuId},this.value)" class="form-control" />
 	</display:column>

</display:table>

<div class="col-sm-12"><h4><iwcm:text key="components.restaurant_menu.nedela"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h4></div>
<display:table class="sort_table table table-hover table-wj" name="nedList" uid="row" >

    <display:column titleKey="components.restaurant_menu.name" property="meal.name" style="width: 25%;" />
	<display:column titleKey="components.table.column.tools" style="width: 5%;">
	<a href="javascript:confirmDelete(${row.menuId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove"></i></a>
	</display:column>
<display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" style="width: 210%;" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" style="width: 20%;" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"></display:column>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />

 	<display:column titleKey="components.restaurant_menu.poradie">
 		<input type="text" value="${row.priority}" style="width: 40px;" onblur="changePriority(${row.menuId},this.value)" class="form-control" />
 	</display:column>

</display:table>

<script type="text/javascript">
$(document).ready(function(){
	$('#weekId').val('<%=week%>');

	$('.date-picker').datepicker({
		autoclose: true,
		format: "dd-mm-yyyy",
		language: "sk",
		calendarWeeks: true,
		daysOfWeekDisabled: [0,6],
		weekStart: 1

	}).on('hide', function(e) {
		var kk = $(".date-picker").val(),
				week = moment(kk, "DD/MM/YYYY").isoWeek(),
				year = moment(kk, "DD/MM/YYYY").year(),
				attachIt = week + "-" + year;

		if (moment(kk, "DD/MM/YYYY").week() == 1 && moment(kk, "DD/MM/YYYY").month() == 0) {
			$('#weekId').val(week + "-" + (year - 1));
		} else {
			$('#weekId').val(attachIt);
		}

	})
})
</script>


<%@ include file="/admin/layout_bottom.jsp" %>