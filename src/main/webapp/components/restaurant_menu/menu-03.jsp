<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@page import="sk.iway.iwcm.components.restaurant_menu.rest.RestaurantMenuService"%>
<%@page import="java.util.ArrayList"%>

<%@page import="sk.iway.iwcm.components.restaurant_menu.jpa.RestaurantMenuEntity"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>

<script type="text/javascript">
	function setWeek3() {
		window.open("<%=PathFilter.getOrigPath(request) %>?week="+document.getElementById('datepicker3_input').value,"_self");
	}
</script>

<%
if ("zobraz".equals(request.getParameter("dnes")))
{
	pageContext.include("/components/restaurant_menu/today.jsp");
	return;
}

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);

String mena = " ("+ new PageParams(request).getValue("mena", "(€)") + ")";
String week = RestaurantMenuService.getWeekDateValue(request.getParameter("week"));
List<List<RestaurantMenuEntity>> menu = RestaurantMenuService.getParsedWeekByDate(week, prop);

request.setAttribute("menu", menu);
%>

<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
<script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>

<label for="weekId"><iwcm:text key="components.restaurant_menu.vyberTyzden"/>:</label>
<input type="week" name="week" id="datepicker3_input" class="datepicker" value="<%=week%>"/>
<input type="button" class="button50" value="<iwcm:text key="components.restaurant_menu.select"/>" onclick="setWeek3()">

<c:choose>
    <c:when test="${not empty menu}">
    <div class="accordion div_menu_03" id="accordionExample">
        <c:forEach var="innerList" items="${menu}">
            <div class="card">
                <div class="card-header">
                    <h2 class="menu"> 
                        <button class="btn btn-block text-left collapsed" type="button" data-toggle="collapse" data-target="#collapse_${innerList[0].dayNumber}" aria-expanded="true">
                            <iwcm:text key="dayfull.${innerList[0].dayNumber}"/> (${innerList[0].dayFormated})
                        </button>
                    </h2>
                </div>
                <div id="collapse_${innerList[0].dayNumber}" class="collapse">
                    <table class="sort_table tabulkaStandard table">
                        <tr>
                            <th> <iwcm:text key="components.restaurant_menu.meal_name"/> </th>
                            <th class="nowrap"> <iwcm:text key="components.restaurant_menu.cathegory"/> </th>
                            <th> <iwcm:text key="components.restaurant_menu.description"/> </th>
                            <th class="nowrap"> <iwcm:text key="components.restaurant_menu.weight"/> </th>
                            <th> <iwcm:text key="components.restaurant_menu.alergens"/> </th>
                        </tr>
                        <c:forEach var="restaurantMenu" items="${innerList}">
                            <tr>
                                <td>${restaurantMenu.meal.name}</td>
                                <td class="nowrap">${restaurantMenu.meal.cathegoryName}</td>
                                <td class="nowrap">${restaurantMenu.meal.description}</td>
                                <td>${restaurantMenu.meal.weight}</td>
                                <td>${restaurantMenu.meal.alergens}</td>
                            </tr>
                        </c:forEach>
                    </table>
                </div>
            </div>
        </c:forEach>
    </div>
    </c:when>
    <c:otherwise>
        <iwcm:text key="components.restaurant_menu.noMenu"/>
    </c:otherwise>
</c:choose>