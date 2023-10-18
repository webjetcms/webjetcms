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
if ("zobraz".equals(request.getParameter("dnes")))
{
	pageContext.include("/components/restaurant_menu/today.jsp");
	return;
}

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(lng);

String mena = " ("+ new PageParams(request).getValue("mena", "(€)") + ")";

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

<link rel="stylesheet" href="//code.jquery.com/ui/1.11.4/themes/smoothness/jquery-ui.css">
  <script src="//code.jquery.com/ui/1.11.4/jquery-ui.js"></script>

  
  <style>
  
  h2.menu{
  font-size:12px;
  padding-bottom:0px;
  display:block;
  width:80%;
  background-color:black;
  color:white;
  padding:5px;
  }
  
  table.menu{
  	margin-bottom:50px;
  	  	width:80%;
  }
  
 .menu td{
  border-bottom:1px solid #d0d0d0;
  }
  
  @media (max-width: 768px) {
#content table td {

    display: table-cell !important;

  }
}
  
  
  </style>
  <script>
  $(function() {
    $( "#datepicker" ).datepicker({ dateFormat: "yy-mm-dd",
        showOtherMonths: true,
        selectOtherMonths: true,
        changeMonth: true,
        changeYear: true,
        showWeek: true,
        firstDay: 1,
        onClose: function(dateTex, inst) { 
        var wk = $.datepicker.iso8601Week(new Date(dateTex));
        if (parseInt(wk) < 10) {
            wk = "0" + wk;
        }           
        var year = $("#ui-datepicker-div .ui-datepicker-year :selected").val();

        if (isNaN(wk)) {
            $(this).val("");
        } else {
            $(this).val(wk + "-" + year);
        }

 

    }});
  });
  </script>
<label for="weekId"><iwcm:text key="components.restaurant_menu.vyberTyzden"/>:</label>
<input type="text" name="week" id="datepicker" class="datepicker" value="<%=week%>"/>
<input type="button" class="button50" value="<iwcm:text key="components.restaurant_menu.select"/>" onclick="setWeek()"> 


<br/><br/>


<% if(!ponList.isEmpty()){ %>
<h2 class="menu"><iwcm:text key="components.restaurant_menu.pondelok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h2>
<display:table class="sort_table menu" cellspacing="0" cellpadding="0" name="ponList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
	<display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="<%=prop.getText("components.restaurant_menu.price")+mena %>" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>
<% }
if(!utoList.isEmpty()){
%>
<h2 class="menu"><iwcm:text key="components.restaurant_menu.utorok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h2>
<display:table class="sort_table menu" cellspacing="0" cellpadding="0" name="utoList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="<%=prop.getText("components.restaurant_menu.price")+mena %>" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>
<% }
if(!strList.isEmpty()){
%>
<h2 class="menu"><iwcm:text key="components.restaurant_menu.streda"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h2>
<display:table class="sort_table menu" cellspacing="0" cellpadding="0" name="strList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="<%=prop.getText("components.restaurant_menu.price")+mena %>" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>
<% }
if(!stvList.isEmpty()){
%>
<h2 class="menu"><iwcm:text key="components.restaurant_menu.stvrtok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h2>
<display:table class="sort_table menu" cellspacing="0" cellpadding="0" name="stvList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="<%=prop.getText("components.restaurant_menu.price")+mena %>" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>
<% }
if(!piaList.isEmpty()){
%>
<h2 class="menu"><iwcm:text key="components.restaurant_menu.piatok"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h2>
<display:table class="sort_table menu" cellspacing="0" cellpadding="0" name="piaList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="<%=prop.getText("components.restaurant_menu.price")+mena %>" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>
<% }
if(!sobList.isEmpty()){
%>
<h2 class="menu"><iwcm:text key="components.restaurant_menu.sobota"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h2>
<display:table class="sort_table menu" cellspacing="0" cellpadding="0" name="sobList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="<%=prop.getText("components.restaurant_menu.price")+mena %>" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>
<% }
if(!nedList.isEmpty()){
%>
<h2 class="menu"><iwcm:text key="components.restaurant_menu.nedela"/><%out.print(" ("+format1.format(cal.getTime())+")"); cal.add(Calendar.DATE, 1);%>:</h2>
<display:table class="sort_table menu" cellspacing="0" cellpadding="0" name="nedList" uid="row" >
    
    <display:column titleKey="components.restaurant_menu.name" property="meal.name" />
    <display:column titleKey="components.restaurant_menu.cathegory" property="meal.cathegory" />
    <display:column titleKey="components.restaurant_menu.description" property="meal.description" />
    <display:column titleKey="components.restaurant_menu.weight" property="meal.weight" style="text-align: right;"/>
    <display:column titleKey="<%=prop.getText("components.restaurant_menu.price")+mena %>" property="meal.price" decorator="sk.iway.displaytag.CurrencyDecorator" style="text-align: right;"/>
    <display:column titleKey="components.restaurant_menu.alergens" property="meal.alergens" />
     	
</display:table>
<% } %>
<%
if(ponList.isEmpty() && utoList.isEmpty() && strList.isEmpty() && stvList.isEmpty() && piaList.isEmpty() && sobList.isEmpty() && nedList.isEmpty()){
	%><iwcm:text key="components.restaurant_menu.noMenu"/>
	<%
}else{
	%>
	<span id="alergens">
	<h2 class="menu"><iwcm:text key="components.restaurant_menu.alergens"/></h2> <br>
	<%
	for(int i = 1; i<=14; i++){ 
		if(i==10){out.print("<br>");}
	if(i==14){
	out.print(prop.getText("components.restaurant_menu.alergen"+i));
	}else{out.print(prop.getText("components.restaurant_menu.alergen"+i)+", ");}
	}
	%></span><%
}
%>

<script type="text/javascript">
$( ".week-picker" ).datepicker({
    dateFormat: "yy-mm-dd",
    showOtherMonths: true,
    selectOtherMonths: true,
    changeMonth: true,
    changeYear: true,
    showWeek: true,
    firstDay: 1,
    beforeShow: function(dateTex, inst) { 

        // for week highighting
        $(".ui-datepicker-calendar tr").live("mousemove", function() { 
            $(this).find("td a").addClass("ui-state-hover"); 
            $(this).find(".ui-datepicker-week-col").addClass("ui-state-hover");
        });
        $(".ui-datepicker-calendar tr").live("mouseleave", function() { 
            $(this).find("td a").removeClass("ui-state-hover");
            $(this).find(".ui-datepicker-week-col").removeClass("ui-state-hover");      
        });
    },
    onClose: function(dateTex, inst) { 
        var wk = $.datepicker.iso8601Week(new Date(dateTex));
        if (parseInt(wk) < 10) {
            wk = "0" + wk;
        }           
        var year = $("#ui-datepicker-div .ui-datepicker-year :selected").val();

        if (isNaN(wk)) {
            $(this).val("");
        } else {
            $(this).val(wk + "-" + year);
        }


    }
});
</script>