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
<%@page import="java.text.SimpleDateFormat"%>

<script type="text/javascript">
	function setWeek() {
		window.open("<%=PathFilter.getOrigPath(request) %>?week="+document.getElementById('datepicker').value,"_self");
	}
</script>
<%
	pageContext.include("/components/restaurant_menu/today.jsp");

%>