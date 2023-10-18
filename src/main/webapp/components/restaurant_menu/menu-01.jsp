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
		window.open("<%=PathFilter.getOrigPath(request) %>?week="+document.getElementById('datepickerMenu').value,"_self");
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

List<MenuBean> menu = new ArrayList<>();
/*
List<MenuBean> ponList = new ArrayList<MenuBean>();
List<MenuBean> utoList = new ArrayList<MenuBean>();
List<MenuBean> strList = new ArrayList<MenuBean>();
List<MenuBean> stvList = new ArrayList<MenuBean>();
List<MenuBean> piaList = new ArrayList<MenuBean>();
List<MenuBean> sobList = new ArrayList<MenuBean>();
List<MenuBean> nedList = new ArrayList<MenuBean>();
*/

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

//ponList = MenuDB.getInstance().getByDate(day);
menu.addAll(MenuDB.getInstance().getByDate(day));
cal.add(Calendar.DATE, 1);
day = cal.getTime();
//utoList = MenuDB.getInstance().getByDate(day);
menu.addAll(MenuDB.getInstance().getByDate(day));
cal.add(Calendar.DATE, 1);
day = cal.getTime();
//strList = MenuDB.getInstance().getByDate(day);
menu.addAll(MenuDB.getInstance().getByDate(day));
cal.add(Calendar.DATE, 1);
day = cal.getTime();
//stvList = MenuDB.getInstance().getByDate(day);
menu.addAll(MenuDB.getInstance().getByDate(day));
cal.add(Calendar.DATE, 1);
day = cal.getTime();
//piaList = MenuDB.getInstance().getByDate(day);
menu.addAll(MenuDB.getInstance().getByDate(day));
cal.add(Calendar.DATE, 1);
day = cal.getTime();
//sobList = MenuDB.getInstance().getByDate(day);
menu.addAll(MenuDB.getInstance().getByDate(day));
cal.add(Calendar.DATE, 1);
day = cal.getTime();
//nedList = MenuDB.getInstance().getByDate(day);
menu.addAll(MenuDB.getInstance().getByDate(day));

cal.add(Calendar.DATE, -6);
SimpleDateFormat format1 = new SimpleDateFormat("dd.MM.yyyy");

request.setAttribute("menu", menu);
/*
request.setAttribute("ponList", ponList);
request.setAttribute("utoList", utoList);
request.setAttribute("strList", strList);
request.setAttribute("stvList", stvList);
request.setAttribute("piaList", piaList);
request.setAttribute("sobList", sobList);
request.setAttribute("nedList", nedList);
*/
%>
<%!
	public String getWeek()
	{
		String week = Integer.toString(Calendar.getInstance().get(Calendar.WEEK_OF_YEAR));
		String year = Integer.toString(Calendar.getInstance().get(Calendar.YEAR));
    	return week+"-"+year;
 	}
%>
<link rel='stylesheet' href='/templates/intranet/assets/css/bootstrap-datetimepicker/bootstrap-datetimepicker.css' />
<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">

<form>
    <div class="row">
        <div class="form-group col-md-4">
            <label for="datepickerMenu"><iwcm:text key="components.restaurant_menu.vyberTyzden"/>:</label>
            <div class="input-group date" id="datepickerMenu1">
                <input type="text" name="week" id="datepickerMenu" class="datepicker form-control" value="<%=week%>"/>
                <div class="input-group-append">
                    <span class="input-group-text"><i class="far fa-calendar-alt"></i></span>
                </div>
            </div>
        </div>
        <div class="form-group col-md-8">
            <input type="button" class="btn btn-primary" style="margin-top: 23px;" value="<iwcm:text key="components.restaurant_menu.select"/>" onclick="setWeek()">
        </div>
    </div>
</form>

<c:choose>
    <c:when test="${not empty menu}">
        <table class="sort_table tabulkaStandard table" id="row">

            <tbody>
                <c:set var="actualDayNumber" value="-1" />
                <c:forEach var="item" items="${menu}" varStatus="status">

                    <c:if test="${actualDayNumber ne item.dayNumber}">
                        <tr>
                            <td class="nadpis" colspan="6"><h2 class="menu"><iwcm:text key="dayfull.${item.dayNumber}" /> (${item.dayFormated})</h2></td>
                        </tr>
                        <tr>
                            <th>Jedlo</th>
                            <th class="nowrap">Kategória</th>
                            <th>Popis</th>
                            <th class="nowrap">Hmotnosť (g)</th>
                            <th>Alergény</th>
                        </tr>
                        <c:set var="actualDayNumber" value="${item.dayNumber}" />
                    </c:if>
                    <tr class="${status.index % 2 eq 0 ? 'event' : 'odd'}">
                        <td>${item.meal.name}</td>
                        <td class="nowrap">${item.meal.cathegory}</td>
                        <td class="nowrap">${item.meal.description}</td>
                        <td>${item.meal.weight}</td>
                        <td>${item.meal.alergens}</td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
        <div id="alergens">
            <h2 class="menu"><iwcm:text key="components.restaurant_menu.alergens"/></h2>
            <c:forEach var="number" begin="1" end="14" varStatus="status">
                <c:if test="${number eq 10}">
                    <br />
                </c:if>
                <iwcm:text key="components.restaurant_menu.alergen${number}" /><c:if test="${not status.last}">, </c:if>
            </c:forEach>
        </div>
    </c:when>
    <c:otherwise>
        <iwcm:text key="components.restaurant_menu.noMenu"/>
    </c:otherwise>
</c:choose>

<iwcm:combine type="js" set="/templates/intranet/assets/jscripts/moment/moment.min.js,/templates/intranet/assets/jscripts/moment/locale/USERLANG.js,/templates/intranet/assets/jscripts/lib/bootstrap-datetimepicker.js"></iwcm:combine>
<script>
    $(function() {
        $('#datepickerMenu').datetimepicker({
            format: 'DD/MM/YYYY',
            calendarWeeks: true
        });
        $('#datepickerMenu').on('dp.show', function(e){

            $("#datepickerMenu").val("");
        });

        $('#datepickerMenu').on('dp.hide', function(e){
            $('#datepickerMenu').data('DateTimePicker').defaultDate(new Date());
            var kk = $('#datepickerMenu').val();

            if(moment(kk, "DD/MM/YYYY").week() == 1 && moment(kk, "DD/MM/YYYY").month() == 11) {
                $("#datepickerMenu").val(moment(kk, "DD/MM/YYYY").week() +"-"+ (moment(kk, "DD/MM/YYYY").year() + 1));
            } else if(moment(kk, "DD/MM/YYYY").week() > 6 && moment(kk, "DD/MM/YYYY").month() == 0) {
                $("#datepickerMenu").val(moment(kk, "DD/MM/YYYY").week() +"-"+ (moment(kk, "DD/MM/YYYY").year() - 1));
            } else {
                $("#datepickerMenu").val(moment(kk, "DD/MM/YYYY").week() +"-"+ moment(kk, "DD/MM/YYYY").year());
            }
        });

        $("#datepickerMenu").val("<%=week%>");

        $('#datepickerMenu1 .input-group-append').on('click', function(){
            $('#datepickerMenu').focus();
        })
    });
</script>
