<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>

<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MealDB"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MenuDB"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MealBean"%>
<%@page import="sk.iway.iwcm.components.restaurant_menu.MenuBean"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.Calendar"%>
<%@page import="java.util.Date"%>

<%
if ("zobraz".equals(request.getParameter("menu")))
{
	pageContext.include("/components/restaurant_menu/menu.jsp");
	return;
}

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);

List<MenuBean> todayList = new ArrayList<MenuBean>();

String week=getWeek();
int w=Integer.parseInt(week.substring(0, week.indexOf("-")));
int y=Integer.parseInt(week.substring(week.indexOf("-")+1, week.length()));

Calendar cal = Calendar.getInstance();
cal.set(Calendar.YEAR, y);
cal.set(Calendar.WEEK_OF_YEAR, w);        
cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
Date day = cal.getTime();

todayList = MenuDB.getInstance().getByDate(day);

request.setAttribute("todayList", todayList);
%>
<%!
	public String getWeek() 
	{ 
		String week = Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
		String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
    	return week+"-"+year;	
 	} 
%>
<a href="<%=PathFilter.getOrigPath(request) %>?menu=zobraz"><%= prop.getText("components.restaurant_menu.listMenu") %></a>
<br/><br/>
<h2><iwcm:text key="components.restaurant_menu.today"/>:</h2>
<display:table class="sort_table" defaultsort="2" cellspacing="0" cellpadding="0" name="todayList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" />
    <display:column titleKey="components.restaurant_menu.price" property="meal.price" />
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>