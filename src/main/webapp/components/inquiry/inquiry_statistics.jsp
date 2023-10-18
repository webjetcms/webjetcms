<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*, sk.iway.iwcm.inquiry.*, sk.iway.iwcm.users.*, java.util.*, sk.iway.iwcm.stat.*, java.text.*, sk.iway.iwcm.i18n.Prop" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@ 
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@ 
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="menuInquiry"/>
<%@ include file="/admin/layout_top.jsp" %>
<script language="JavaScript">
var helpLink = "";
</script>
<%
String lng = PageLng.getUserLng(request);
Prop prop = Prop.getInstance(lng);

List<UserVoteBean> statistics = null;
List admins = UsersDB.getAdmins();
request.setAttribute("admins",admins);

Integer adminId = -3;
Integer answerId = -1;

Date startDate = null;
Date endDate = null;

Calendar endCal = Calendar.getInstance();
Calendar startCal = Calendar.getInstance();
startCal.set(Calendar.MONTH, endCal.get(Calendar.MONTH)-1);

//ak mam v session, nastav
if (session.getAttribute("adminId") != null)
{
	adminId = (Integer)session.getAttribute("adminId");
}
if (session.getAttribute("answerId") != null)
{
	answerId = (Integer)session.getAttribute("answerId");
}
if (session.getAttribute(Constants.STAT_START_DATE)!=null)
{
	startDate = (Date)session.getAttribute(Constants.STAT_START_DATE);
} else {
	startDate = new Date(startCal.getTime().getTime());	//ak nie je datum v session nastavim defaultny
}
if (session.getAttribute(Constants.STAT_END_DATE)!=null)
{
	endDate = (Date)session.getAttribute(Constants.STAT_END_DATE);
} else {
	endDate = new Date(endCal.getTime().getTime());	//ak nie je datum v session nastavim defaultny
}

/**
*	-1 -> len neprihlaseni pouzivatelia
*	-2 -> len prihlaseni pouzivatelia
*	-3 -> vsetci pouzivatelia
**/

//ak mam nieco v requeste, nastav
if(Tools.isNotEmpty(request.getParameter("adminId")))	
	adminId = Tools.getIntValue(request.getParameter("adminId"), -3);	//vsetci pouzivatelia

if(Tools.isNotEmpty(request.getParameter("answerId")))	
	answerId = Tools.getIntValue(request.getParameter("answerId"), -1);


if(Tools.isNotEmpty(request.getParameter("startDate"))){
	startCal.setTimeInMillis(DB.getTimestamp(request.getParameter("startDate")));
	startDate = new Date(startCal.getTime().getTime());
}

if(Tools.isNotEmpty(request.getParameter("endDate"))){
	endCal.setTimeInMillis(DB.getTimestamp(request.getParameter("endDate")));
	endCal.set(Calendar.HOUR, 23);
	endCal.set(Calendar.MINUTE, 59);
	endCal.set(Calendar.SECOND, 59);
	endDate = new Date(endCal.getTime().getTime());
}

SimpleDateFormat formatter = new SimpleDateFormat(Constants.getString("dateFormat"));
String startDateString = Tools.formatDate(startDate);
String endDateString = Tools.formatDate(endDate);

int questionId = Tools.getIntValue(request.getParameter("qID"),-1);	//id otazky(ankety)
if(questionId > 0) {	
	statistics = InquiryDB.getUsersVoteFromTo(startDateString, endDateString, adminId, questionId, answerId);	//zoznam odpovedi podla zadanych kriterii
	request.setAttribute( "result", statistics);
}

List answers = InquiryDB.getAnswers(questionId, request);
request.setAttribute("answers",answers);

AnswerForm af = InquiryDB.getQuestion(questionId, request);
String questionString = af.getQuestionString();


session.setAttribute(Constants.STAT_START_DATE, startDate);
session.setAttribute(Constants.STAT_END_DATE, endDate);

session.setAttribute("adminId", adminId);
session.setAttribute("answerId", answerId);

%>

<!-- Tabuľka statistik pre zadanu anketu -->
<h1><%=questionString %></h1>

<div class="box_tab box_tab_thin">	<!--search filter -->
		<ul class="tab_menu">
			<li class="last open">
				<div class="first">&nbsp;</div>
				<a id="tabLink1" class="activeTab" onclick="showHideTab('1');" href="#">
					<iwcm:text key="components.filter" />
				</a>			
			</li>
		</ul>
	</div>
	
	<div class="box_toggle">
		<div class="toggle_content">
			<div id="tabMenu1">
				<form name="settings" action="inquiry_statistics.jsp">
				<input type="hidden" name="search" value="true" />
				<input type="hidden" name="qID" value="<%=questionId %>" />
				
				<table border="0">
				<tr>
				   <td><label for="startDateId"><iwcm:text key="components.forum.od" />:</label></td>	<!--date -->
					<td>
						<input type="text" size="10" maxlength="10" name="startDate" id="startDateId" value="<%=startDateString %>"" onblur="checkDate(document.settings.startDate);return false;" class="poleMini datepicker" />
					</td>
				   <td><label for="endDateId"><iwcm:text key="components.forum.do" />:</label></td>
				   <td>
						<input type="text" size="10" maxlength="10" name="endDate" id="endDateId" value="<%=endDateString %>" onblur="checkDate(document.settings.endDate);return false;" class="poleMini datepicker" />
				   </td>
				   <td>
				   	    <label for="admins"><iwcm:text key="groupslist.approve.authorName"/>:</label>
					</td>
					<td>	<!--user -->
						<select name="adminId" id="admins">
							<option value="-3" <%if(adminId == -3) out.println("selected=\"selected\"");%>><iwcm:text key="users.authType.all"/></option>
							<option value="-1" <%if(adminId == -1) out.println("selected=\"selected\"");%>><iwcm:text key="users.authType.unlogged"/></option>
							<option value="-2" <%if(adminId == -2) out.println("selected=\"selected\"");%>><iwcm:text key="users.authType.logged"/></option>
							<%
							for (Iterator iter = admins.iterator(); iter.hasNext();)
							{
								UserDetails userDet = (UserDetails)iter.next();
							%>
								<option value="<%=userDet.getUserId()%>" <%if(adminId == userDet.getUserId()) out.println("selected=\"selected\"");%> ><%=userDet.getFullName()%></option>
							<%
							}
							%>
						</select>
				   </td>
				   <td>		<!-- answer -->
				   	    <label for="answers"><iwcm:text key="groupslist.approve.answerString"/>:</label>
					</td>
					<td>	<!--answer -->
						<select name="answerId" id="answers">
							<option value="-1" <%if(answerId == -1) out.println("selected=\"selected\"");%>><iwcm:text key="groupslist.answers.all"/></option>
							<%
							for (Iterator iter = answers.iterator(); iter.hasNext();)
							{
								AnswerForm answerForm = (AnswerForm)iter.next();
							%>
								<option value="<%=answerForm.getAnswerID()%>" <%if(answerId == answerForm.getAnswerID()) out.println("selected=\"selected\"");%> ><%=answerForm.getAnswerString()%></option>
							<%
							}
							%>
						</select>
				   </td>
				  
				   <td>&nbsp;</td>
				   <td><input type="submit" value="<iwcm:text key="components.forum.zobrazit" />" class="button50"></td>
				</tr>
				</table>
				</form>
			</div>
		</div>
	</div>

<%
int width = 450;
int height = 320;

String userName = "";
String answerName = "";
if(statistics != null && statistics.size() > 0) {%>
<display:table class="sort_table" name="result" export="true" pagesize="20" defaultsort="1" defaultorder="ascending"  id="StatisticTable" >
   <display:setProperty name="export.excel.filename" value="docs_waiting_for_publishing.xls" />
	<display:setProperty name="export.csv.filename" value="docs_waiting_for_publishing.csv" />
	<display:setProperty name="export.xml.filename" value="docs_waiting_for_publishing.xml" />
	<display:setProperty name="export.pdf.filename" value="docs_waiting_for_publishing.pdf" />
  <% 
  	if(((UserVoteBean)StatisticTable).getUserId() == -1) userName = "Neprihlaseny";
  	else{
   	UserDetails user = UsersDB.getUser(((UserVoteBean)StatisticTable).getUserId());
  		if(user != null) userName = user.getFullName();	//meno pouzivatela
  	}
  	af = InquiryDB.getAnswer(((UserVoteBean)StatisticTable).getAnswerId(), request);
  	if(af != null) answerName = af.getAnswerString();	//znenie odpovede
  %>
  
  
  <display:column titleKey="components.inquiry.inquiry_statistics.user_name" sortable="true" headerClass="sortable" escapeXml="true">
  	<%=userName %>
  </display:column>
  <display:column property="ip" titleKey="components.inquiry.inquiry_statistics.ip" sortable="true" headerClass="sortable" escapeXml="true"/>
  <display:column titleKey="components.inquiry.inquiry_statistics.answer_text" sortable="true" headerClass="sortable" escapeXml="true">
  	<%=answerName %>
  </display:column>
  <display:column titleKey="components.inquiry.inquiry_statistics.create_date" sortable="true" headerClass="sortable" escapeXml="true">
  	<%=((UserVoteBean)StatisticTable).getCreateDateString() %> <%=((UserVoteBean)StatisticTable).getCreateTimeString() %>
  </display:column>
</display:table>
<br/>

<div style="clear: both;"></div>
<div id="pieChart" style="width: 100%; height: 300px;"></div>
<div id="lineChart" style="width: 100%; height: 300px;"></div>
<div id="lineChartLegend" style="border: 2px dotted #3f3; margin: 5px 0 20px 0;"></div>	

<%
Map<String, Number> c_data = StatGraphDB.getInquiryPieData(startDate, endDate, questionId, adminId, prop, request);

String pieChartData = "[";
for(Map.Entry<String, Number> e : c_data.entrySet())
{
	pieChartData+="{\"value1\": ";
	pieChartData+="\""+e.getKey()+"\"";
	pieChartData+=",\"value2\": ";
	pieChartData+=e.getValue();
	pieChartData+="},";
}
pieChartData = pieChartData.substring(0, pieChartData.length()-1);
pieChartData+="]";

Map<String, Map<Date, Number>> data = StatGraphDB.getInquiryTimeData(5, startDate, endDate, questionId, adminId, request);
String lineChartData = StatNewDB.amChartsData(data);
%>

<script src="/components/_common/amcharts/amcharts/amcharts.js"></script>
<script src="/components/_common/amcharts/amcharts/serial.js"></script>
<script src="/components/_common/amcharts/amcharts/pie.js"></script>
<script src="/components/_common/amcharts/amcharts/themes/light.js"></script>
<script src="/components/_common/amcharts/amcharts/plugins/export/export.js" type="text/javascript"></script>
<link rel="STYLESHEET" href="/components/_common/amcharts/amcharts/plugins/export/export.css" type="text/css">

<script type="text/javascript">
var chart = AmCharts.makeChart("pieChart", {
	  "type": "pie",
	  "groupPercent": 3,
	  "startDuration": 1,
	   "theme": "light",
	  "addClassNames": true,
	  "titles": [{"text": "<iwcm:text key="stat.graph.inquiryPie"/>",
     		"size": 15}],
	  "legend":{
	   	"position":"right",
	    "marginRight":100,
	    "autoMargins":false
	  },
	  "innerRadius": "30%",
	  "dataProvider": <%=pieChartData%>,
	  "valueField": "value2",
	  "titleField": "value1",
	  "depth3D": 15,
	  "angle": 30,
	  "outlineAlpha": 0.4,
	  "export": {
	    "enabled": true
	  }
	});

var lineChart = AmCharts.makeChart("lineChart", {
    "type": "serial",
    "theme": "light",
    "marginRight": 80,
    "autoMarginOffset": 20,
    "marginTop": 7,
    "dataProvider": <%=lineChartData%>,
    "valueAxes": [{
        "axisAlpha": 0.2,
        "dashLength": 1,
        "position": "left"
    }],
    "titles": [{"text": "<iwcm:text key="stat.graph.answer"/>",
	       		"size": 15}],
    "graphs": [
    <%int i = 0; 
    for(Map.Entry<String, Map<Date, Number>> e : data.entrySet()) {%>
    {
        "id": "g<%=i%>",
        "balloonText": "[[category]]<br/><b><span style='font-size:14px;'><%=e.getKey()%>: [[value]]</span></b>",
        "bullet": "none",
        "bulletBorderAlpha": 1,
        "bulletColor": "#FFFFFF",
        "hideBulletsCount": 50,
        "title": "<%=e.getKey()%>",
        "valueField": "value<%=i%>",
        "useLineColorForBulletBorder": true
    },
    <%i++;
    }%>
   ],
    "chartCursor": {},
    "categoryField": "date",
    "categoryAxis": {
        "parseDates": false,
        "axisColor": "#DADADA",
        "dashLength": 1,
        "minorGridEnabled": true,
        "minHorizontalGap": 130
    },
    "legend": {
    	  divId: "lineChartLegend"
    	},
    "export": {
        "enabled": true
    }
});
</script>

<%} else { %>
<h3><iwcm:text key="components.inquiry.inquiry_statistics.not_found"/></h3>
<%} %> 
<%@ include file="/admin/layout_bottom.jsp" %>