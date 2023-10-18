<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, java.util.*,java.text.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@page import="org.apache.struts.util.ResponseUtils"%><%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String path = PathFilter.getOrigPathDocId(request);

String groupIds = ResponseUtils.filter(pageParams.getValue("groupIds", ""));
String perexGroup = ResponseUtils.filter(pageParams.getValue("perexGroup", ""));
String expandGroupIds = ResponseUtils.filter(pageParams.getValue("expandGroupIds", "false"));

String datum = Tools.getStringValue(request.getParameter("datum"),null);
SimpleDateFormat sdf = new SimpleDateFormat("d_M_yyyy");
%>
<script type="text/javascript" src="/components/calendar/jscripts/date.js"></script>
<script type="text/javascript" src="/components/calendar/jscripts/jquery.kadatepicker.js"></script>
<script type="text/javascript" src="/components/calendar/jscripts/date_locale.jsp"></script>
<%=Tools.insertJQuery(request)%>
<link rel="stylesheet" type="text/css" href="/components/calendar/css/calendar.css"/>
<div class="kalendar">
	<div class="rightObsah">
		<div class="boxKalendarCenter">
			<div id="kalendarPlace"></div>
			<div id="day_events">
			
			</div>
		   <div class="clearer"></div>
		</div>
	</div>
	<p><a href="<%=path%>" class="btn"><span><iwcm:text key="components.calendar_news.zobrazit_najnovsie"/></span></a></p>
</div>
 
<script type="text/javascript">
  <!--
  
  function loadCalEvents(){

	 var actualCalId = $('.dp-calendar').attr('id');
	 var actualCal = actualCalId.split('_');
	 var actualMonth = actualCal[0];
	 var actualYear = actualCal[1];
	 
    $.ajax({
          url: "/components/calendar/news_calendar-ajax_utf-8.jsp",
          type: "POST",
          dataType: "script",
          data: ({ mesiac: actualMonth, rok: actualYear, groupIds: '<%=groupIds%>', perexGroup: '<%=perexGroup%>', expandGroupIds: '<%=expandGroupIds%>'})
       }
    )
   } 
 
  $(document).ready(function(){

    Date.format = 'dd.mm.yyyy';
    
    var datePicker = $('#kalendarPlace').datePicker({
      inline:true,
      startDate: '01.01.1970',				
		wjDatum: '<%=datum != null ? datum : Tools.formatDate(Tools.getNow())%>'
    });
	 
    loadCalEvents();
    $('.dp-nav-prev-month').bind('click',function(){loadCalEvents();});
    $('.dp-nav-next-month').bind('click',function(){loadCalEvents();});
    $('.dp-nav-prev-year').bind('click',function(){loadCalEvents();});
    $('.dp-nav-next-year').bind('click',function(){loadCalEvents();});
    datePicker.bind(
      'dateSelected',
      function(e, date, $td)
      {
        var dateStr = new Date(date).asString();
        window.location.href = '<%=path.indexOf("?") != -1 ? "&datum=" : "?datum="%>'+dateStr;
      }
    );
  });
  // -->
</script>