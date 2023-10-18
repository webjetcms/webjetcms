<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.Alergens"%>
<%@ page import="sk.iway.iwcm.components.enumerations.EnumerationDataDB" %>
<%@ page import="sk.iway.iwcm.components.enumerations.model.EnumerationDataBean" %>

<iwcm:menu notName="cmp_restaurant_menu"><%response.sendRedirect("/admin/403.jsp");
		if (1==1) return;%></iwcm:menu>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(lng);

	List<Alergens> selectableAlergens= new ArrayList();
	for(int i=1;i<15;i++)
	{
		selectableAlergens.add(new Alergens(i,prop.getText("components.restaurant_menu.alergen"+i)));
	}

	List<String> selectableMenu = new ArrayList();
	List<EnumerationDataBean> enumerationDataBeans = EnumerationDataDB.getEnumerationDataByType("Reštauračné menu - kategórie");


	if (enumerationDataBeans.isEmpty()) {
		selectableMenu.add(prop.getText("components.restaurant_menu.polievka"));
		selectableMenu.add(prop.getText("components.restaurant_menu.hlavne_jedlo"));
		selectableMenu.add(prop.getText("components.restaurant_menu.dezert"));
		selectableMenu.add(prop.getText("components.restaurant_menu.priloha"));
	} else {
		enumerationDataBeans.sort((o1, o2) -> o1.getDecimal1().compareTo(o2.getDecimal1()));
		for (EnumerationDataBean item: enumerationDataBeans){
			selectableMenu.add(item.getString1());
		}
	}

	request.setAttribute("cmpName", "restaurant_menu");
%>

<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<script type="text/javascript">
	function Ok()
	{
		document.getElementById('priceId').value=document.getElementById('priceId').value.replace(".",",");
		document.mealForm.bSave.click();
	}
	resizeDialog(600, 680);
</script>

<%
	if (request.getParameter("bSave")!=null)
	{
	  pageContext.include("/sk/iway/iwcm/components/restaurant_menu/Restaurant.action");
	}
%>

<script type="text/javascript" src="/components/form/check_form.js"></script>

<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.restaurant_menu.RestaurantAction"/>
<iwcm:stripForm action="<%=PathFilter.getOrigPath(request)%>" beanclass="sk.iway.iwcm.components.restaurant_menu.RestaurantAction" name="mealForm" method="post">
	<table border="0">
		<tr>
			<td><label for="nameId"><iwcm:text key="components.restaurant_menu.name"/>:</label></td>
			<td><stripes:text name="meal.name" id="nameId" size="40" maxlength="255" class="required"/></td>
		</tr>
		<tr>
			<td><label for="cathegoryId"><iwcm:text key="components.restaurant_menu.cathegory"/>:</label></td>
			<td>
				<stripes:select name="meal.cathegory" id="cathegoryId">
				<stripes:options-collection collection="<%=selectableMenu %>" value="" label="" />
				</stripes:select>
			</td>
		</tr>
		<tr>
			<td><label for="descriptionId"><iwcm:text key="components.restaurant_menu.description"/>:</label></td>
			<td><stripes:textarea name="meal.description" id="descriptionId" rows="4" cols="40" /></td>
		</tr>
		<tr>
			<td><label for="weightId"><iwcm:text key="components.restaurant_menu.weight"/>:</label></td>
			<td><stripes:text name="meal.weight" id="weightId" size="40" maxlength="255"/></td>
		</tr>
		<tr>
			<td><label for="priceId"><iwcm:text key="components.restaurant_menu.price"/>:</label></td>
			<td><stripes:text name="meal.price" id="priceId" size="40" maxlength="255" class="required numbers"/></td>
		</tr>
		<tr>
			<td valign="top"><label for="alergensId"><iwcm:text key="components.restaurant_menu.alergens"/>:</label></td>
			<td>
				<stripes:select name="allergens" multiple="multiple" size="14" id="alergensId" class="allergens-multiple">
				<stripes:options-collection collection="<%=selectableAlergens %>" value="number" label="name" />
				</stripes:select>
			</td>
		</tr>
		<tr style="display: none;">
		<input type="hidden" value=" " name="allergens">
			<td><stripes:hidden name="meal.mealId"/></td>
			<td><input type="submit" name="bSave" value="<iwcm:text key="button.submit"/>"/></td>
		</tr>
	</table>
</iwcm:stripForm>
</div>
<script type="text/javascript">
$('.allergens-multiple option').mousedown(function(e) {
    e.preventDefault();
    $(this).prop('selected', $(this).prop('selected') ? false : true);
    return false;
});
</script>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>