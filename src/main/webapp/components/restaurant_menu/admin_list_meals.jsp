<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<iwcm:menu notName="cmp_restaurant_menu">
	<%response.sendRedirect("/admin/403.jsp");
		if (1==1) return;%>
</iwcm:menu>

<%@ include file="/admin/layout_top.jsp" %>

<%@page import="sk.iway.iwcm.components.restaurant_menu.MealDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MealBean"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>

<script type="text/javascript">
	function edit(mealId)
	{
		openPopupDialogFromLeftMenu("/components/restaurant_menu/admin_new_meal.jsp?meal.mealId="+mealId);
	}

	function confirmDelete(mealId)
	{
		if(window.confirm('<iwcm:text key="components.dictionary.delete_confirm"/>') == true)
		{
			document.actionForm.elements["meal.mealId"].value=mealId;
			document.actionForm.submit();
		}
	}

	function newMeal() {
		openPopupDialogFromLeftMenu('/components/restaurant_menu/admin_new_meal.jsp');
	}
</script>

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);

String filterNameMeal = Tools.getStringValue(request.getParameter("filterNameMeal"), "");
List<MealBean> mealList = new ArrayList<MealBean>();

if (request.getParameter("bDelete")!=null)
	pageContext.include("/sk/iway/iwcm/components/restaurant_menu/Restaurant.action");

if (request.getParameter("filterForm") != null)
	mealList = MealDB.getInstance().findByName(filterNameMeal);
else
	mealList = MealDB.getInstance().findAll();

request.setAttribute("mealList", mealList);
%>

<div class="row title">
	<h1 class="page-title"><i class="fa icon-book-open"></i><iwcm:text key="components.restaurant_menu.title"/><i class="fa fa-angle-right"></i><iwcm:text key="components.restaurant_menu.mealsList"/></h1>
</div>

<div class="webjet5hidden">
	<div class="box_tab box_tab_thin">
		<ul class="tab_menu">
			<li class="open">
				<div class="first">&nbsp;</div>
				<a class="activeTab" href="#" id="tabLink1" onclick="showHideTab('1');">
					<iwcm:text key="components.filter"/>
				</a>
			</li>
		</ul>
	</div>

	<div class="box_toggle">
		<div class="toggle_content">
			<div id="tabMenu1">

				<form name="mealFilterForm" action="/components/restaurant_menu/admin_list_meals.jsp" class="zobrazenie">
					<fieldset>
						<p>
							<label>
								<iwcm:text key="components.restaurant_menu.name"/>:
								<input type="text" class="poleKratke" value="<%=filterNameMeal %>" name="filterNameMeal" />
							</label>

							<input type="submit" class="button50" value="<iwcm:text key="components.tips.view"/>" />
							<input type="hidden" name="filterForm" value="" />
							<input type="button" class="button50" value="<iwcm:text key="components.restaurant_menu.addNewMeal"/>" onclick="newMeal()">
						</p>
					</fieldset>
				</form>

			</div>
		</div>
	</div>
</div>


<form action="<%=PathFilter.getOrigPath(request)%>" name="actionForm" style="display: none;">
	<input type="hidden" name="bDelete" value="true" />
	<input type="hidden" name="meal.mealId" value="" />
</form>

<display:table class="sort_table table table-hover table-wj" name="mealList" uid="row" >

    <display:column sortable="true" titleKey="components.restaurant_menu.name" sortProperty="name">
    	<a href="javascript:edit(${row.mealId})">${row.name}</a>
    </display:column>

    <display:column titleKey="components.table.column.tools" style="text-align:center;">
    	<a href="javascript:edit(${row.mealId})" title='<iwcm:text key="components.banner.edit"/>'><i class="glyphicon glyphicon-pencil" aria-hidden="true"></i></a>
 		<a href="javascript:confirmDelete(${row.mealId});" title='<iwcm:text key="button.delete"/>'><i class="glyphicon glyphicon-remove" aria-hidden="true"></i></a>
 	 </display:column>

    <display:column sortable="true" titleKey="components.restaurant_menu.cathegory" property="cathegory" />
    <display:column sortable="true" titleKey="components.restaurant_menu.description" property="description" />
    <display:column sortable="true" titleKey="components.restaurant_menu.weight" property="weight" style="text-align: right;"/>
    <display:column sortable="true" titleKey="components.restaurant_menu.price" property="price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column sortable="true" titleKey="components.restaurant_menu.alergens" property="alergens" />

</display:table>

<%@ include file="/admin/layout_bottom.jsp" %>