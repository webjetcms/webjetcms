<%@page import="java.util.Map"%><%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.stat.*, java.util.*, sk.iway.iwcm.stat.heat_map.*" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %>
<iwcm:checkLogon admin="true" perms="cmp_stat"/>
<%request.setAttribute("layout_closeMainTable", "true");%>
<%@ include file="/admin/layout_top.jsp" %>

<script type="text/javascript">
<!--
	helpLink = "components/stat.jsp&book=components";
//-->
</script>

<div class="row title">
    <h1 class="page-title"><i class="fa icon-pie-chart"></i><iwcm:text key="menu.stat"/><i class="fa fa-angle-right"></i><iwcm:text key="stat_menu.heat_map"/></h1>
</div>

<div id="waitDiv" style="text-align:center; color: red;">
   <iwcm:text key="stat.wait_loading_data"/><br/>
   <img src="/admin/images/loading-anim.gif" alt="" />
</div>

<%
out.flush();

	List<DocDetails> clicksArray = null;
	Map<Integer, Integer> clicks = null;
	if (Tools.getRequestParameter(request, "heatMapOn") != null)
	{
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constants.getString("dateFormat"));
		if ("false".equals(Tools.getRequestParameter(request, "heatMapOn")))
		{
			session.setAttribute("heatMapEnabled", true);
			Date startDate = simpleDateFormat.parse(Tools.getRequestParameter(request, "startDate"));
			Date endDate = simpleDateFormat.parse(Tools.getRequestParameter(request, "endDate"));
			session.setAttribute("startDate", startDate);
			session.setAttribute("endDate", endDate);
		}
		if ("true".equals(Tools.getRequestParameter(request, "heatMapOn"))){
			session.removeAttribute("heatMapEnabled");
		}

		Date startDate = simpleDateFormat.parse(Tools.getRequestParameter(request, "startDate"));
		Date endDate = simpleDateFormat.parse(Tools.getRequestParameter(request, "endDate"));
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(startDate);
		int startMonth = calendar.get(Calendar.MONTH)+1;
		int startDay = calendar.get(Calendar.DAY_OF_MONTH);
		int startYear = calendar.get(Calendar.YEAR);
		calendar.setTime(endDate);
		int endMonth = calendar.get(Calendar.MONTH)+1;
		int endDay = calendar.get(Calendar.DAY_OF_MONTH);
		int endYear = calendar.get(Calendar.YEAR);
		clicks = new HashMap<Integer, Integer>();
		for(int j = startYear; j <= endYear; j++){
			if(startYear == endYear){	//iba jeden rok
				for(int i = startMonth; i <= endMonth; i++){
					if(startMonth == endMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, startDay, endDay, clicks);	//iba jeden mesiac
					else if(i == startMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, startDay, -1, clicks);	//prvy mesiac
					else if(i == endMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, endDay, clicks);	//posledny mesiac
					else clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, -1, clicks);	//prostredne mesiace - beriem vsetky dokumenty
				}
			}
			else if(j == startYear){	//prvy rok - od nastaveneho zaciatku az po koniec roka
				for(int i = startMonth; i <= 12; i++){
					if(i == startMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, startDay, -1, clicks);	//prvy mesiac
					else if(i == endMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, endDay, clicks);	//posledny mesiac
					else clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, -1, clicks);	//prostredne mesiace - beriem vsetky dokumenty
				}
			}
			else if(j == endYear){	//posledny rok - od zaciatku roka az po nastaveny koniec
				for(int i = 1; i <= endMonth; i++){
					if(i == startMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, startDay, -1, clicks);	//prvy mesiac
					else if(i == endMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, endDay, clicks);	//posledny mesiac
					else clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, -1, clicks);	//prostredne mesiace - beriem vsetky dokumenty
				}
			}
			else{	//prostredne roky - vsetky mesiace
				for(int i = 1; i <= 12; i++){
					if(i == startMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, startDay, -1, clicks);	//prvy mesiac
					else if(i == endMonth) clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, endDay, clicks);	//posledny mesiac
					else clicks = HeatMapDB.getClicksInDate(j+"_"+i, -1, -1, clicks);	//prostredne mesiace - beriem vsetky dokumenty
				}
			}
		}

	}
	else clicks = HeatMapDB.getClicksInDate(null, -1, -1, null);

	if(clicks != null && clicks.size() > 0)
	{
		Set set = clicks.entrySet();
	   Iterator<Map.Entry<Integer, Integer>> i = set.iterator();
		clicksArray = new ArrayList<DocDetails>();
		DocDetails dd = null;
	   while(i.hasNext())
	   {
	     Map.Entry<Integer, Integer> me = i.next();
	     dd = new DocDetails();
	     dd.setDocId(me.getKey().intValue());
		  dd.setDocLink(DocDB.getURLFromDocId(me.getKey(), request));
		  dd.setForumCount(me.getValue());
	     clicksArray.add(dd);
	   }
		request.setAttribute( "result", clicksArray );
	}

%>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.filter"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
	    <div id="tabMenu1" class="tab-pane active">
	    	<%@ include file="../seo/stat_menu.jsp" %>
		</div>
	</div>
</div>

<c:if test='<%=session.getAttribute("heatMapEnabled") == null %>'>
	<iwcm:text key="components.stat.heatmap.clickToActivate"/>
</c:if>

<c:if test='<%=session.getAttribute("heatMapEnabled") != null %>'>
	<div id="notice">
		<iwcm:text key="components.stat.heat_map.turned_on"/>
	</div>
	<h3><iwcm:text key="components.stat.heat_map.list_clicks"/></h3>
	<display:table class="sort_table" name="result" export="true" pagesize="20" defaultsort="3" defaultorder="descending"  id="HeatMapTable" >
   <display:setProperty name="export.excel.filename" value="heatMap.xls" />
	<display:setProperty name="export.csv.filename" value="heatMap.csv" />
	<display:setProperty name="export.xml.filename" value="heatMap.xml" />
	<display:setProperty name="export.pdf.filename" value="heatMap.pdf" />
	<display:column title="docId" property="docId" sortable="true"/>
	<display:column titleKey="components.stat.heat_map.url" sortable="true">
		<a href="${HeatMapTable.docLink}" target="_blank">${HeatMapTable.docLink}</a>
	</display:column>
	<display:column titleKey="components.stat.heat_map.count" property="forumCount" sortable="true"/>
	</display:table>
</c:if>

<c:if test='<%=request.getAttribute("errorDoingHeatMap") != null %>'>
	<div style="font-weight: bold; color: red" id="error">
		Pri vytváraní teplotnej mapy nastal problém: <%=request.getAttribute("errorDoingHeatMap") %>
	</div>
</c:if>
<%@ include file="/admin/layout_bottom.jsp" %>
