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

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);

List<RestaurantMenuEntity> todayList = new ArrayList<RestaurantMenuEntity>();
todayList = RestaurantMenuService.getByDate(RestaurantMenuService.getMenuDate(null), prop);

request.setAttribute("todayList", todayList);
%>

<c:choose>
    <c:when test="${not empty todayList}">
        <div class="div_menu_04">
            <table class="sort_table tabulkaStandard table">
                <tr>
                    <td colspan="5" class="headline">
                        <h2 class="menu"> <iwcm:text key="components.restaurant_menu.today"/>:</h2>
                        <h3> <iwcm:text key="dayfull.${todayList[0].dayNumber}"/> (${todayList[0].dayFormated})</h3>
                    </td>
                </tr>
                <tr>
                    <th> <iwcm:text key="components.restaurant_menu.meal_name"/> </th>
                    <th class="nowrap"> <iwcm:text key="components.restaurant_menu.cathegory"/> </th>
                    <th> <iwcm:text key="components.restaurant_menu.description"/> </th>
                    <th class="nowrap"> <iwcm:text key="components.restaurant_menu.weight"/> </th>
                    <th> <iwcm:text key="components.restaurant_menu.alergens"/> </th>
                </tr>
                <c:forEach var="restaurantMenu" items="${todayList}">
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
    </c:when>
    <c:otherwise>
        <iwcm:text key="components.restaurant_menu.noMenuToday"/>
    </c:otherwise>
</c:choose>