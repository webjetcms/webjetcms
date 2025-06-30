<%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ 
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@ 
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@ 
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@ 
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--
	
	A jsp file collecting mouse clicks for the purpose of generating a heat map.
	Clicks are collected in a cookie variable and handed out for processing
	on the subsequent request.  
	
	PREREQUISITES: A div with class 'mainContainer' must exist
	
	
 --%><%

	if (Tools.canSetCookie("statisticke", request.getCookies())==false)
	{
		//nemam povolene vkladat statisticke cookies, nemozem pridat ani toto meranie
		return;
	}

 	if (BrowserDetector.isStatIpAllowedFast(request))
 	{	
 		if (request.getCookies()!=null)
 		{
			for (Cookie cookie : request.getCookies())
			{
				if ("click_storage".equals(cookie.getName()))
				{
					String value = Tools.URLDecode(cookie.getValue());
					HeatMapDB.recordCookie(value);
				}
			}
 		}
 	}
 
 PageParams pageParams = new PageParams(request);
 String statHeatMapMainContainerSelector = pageParams.getValue("container", Constants.getString("statHeatMapMainContainerSelector"));
%>
<%@page import="sk.iway.iwcm.stat.heat_map.HeatMapDB"%>
<%@page import="sk.iway.iwcm.stat.BrowserDetector"%>
<%=Tools.insertJQuery(request) %>
<c:if test='<%=session.getAttribute("heatMapEnabled") ==null %>'>
	<script type="text/javascript" src="/components/_common/javascript/jquery.cookie.js"></script>
	<script type="text/javascript">
<!--

	clickStorage = '<%=request.getAttribute("doc_id")%>';

	function initCookie(){
		jQuery.cookie('click_storage', clickStorage);
	}
	
	$(document.body).mousedown(function(event)
	{
		try
		{
			if ($("<%=statHeatMapMainContainerSelector%>").length >0)
			{			
				x = Math.ceil(event.pageX - $("<%=statHeatMapMainContainerSelector%>").offset().left);
				y = Math.ceil(event.pageY - $("<%=statHeatMapMainContainerSelector%>").offset().top);
				if (x < 0 || y < 0) return;
				//window.alert("x="+x+" y="+y);
				clickStorage += "["+x+","+y+"]";
				jQuery.cookie('click_storage', clickStorage);
			}
		}
		catch (e) {
			console.error(e);
		}
	})

	initCookie();
-->
</script>
</c:if>

<c:if test='<%=session.getAttribute("heatMapEnabled") !=null %>'>
	<div id="heatMap" style="display: none; position: absolute; z-index: 9999; pointer-events: none; width: 100%; height: 100%; text-align: center; background-color: black; filter:alpha(opacity=60); opacity: 0.6;"><p id="heatmapInfo" style="margin-top: 300px; font-weight: bold; font-size: 18px; color: green; text-align: center;"><iwcm:text key="components.stat.heatmap.waitPleaseGenerating"/></p><img src="/components/stat/images/ajax-loader.gif" alt="" id="heatMapImage"/></div>
	<script type="text/javascript">
	<!--
	$(document).ready(function()
	{
		//<c:if test="<%=BrowserDetector.MSIE.equals(BrowserDetector.getInstance(request).getBrowserName()) %>">
			$('#heatMapImage').click(function(){$('#heatMap').hide()})
			$('#heatMap').click(function(){$('#heatMap').hide()})
		//</c:if>
		$('<%=statHeatMapMainContainerSelector%>').prepend( $('#heatMap') );
		$('#heatMap').show();
		$('#heatMapImage').show();
		$('#heatMapImage').attr('src', heatMapUrl = "/components/stat/heat_map_image.jsp?document_id=<%=request.getAttribute("doc_id") %>");
		$('#heatMapImage').on('load',function()
		{
			if ($('#heatMapImage').width()==1 && $('#heatMapImage').height()==1)
			{
				$('#heatmapInfo').html("<iwcm:text key="components.stat.heatmap.noDataFound"/>");
				$('#heatMapImage').hide();
			}
			else
			{
				$('#heatmapInfo').hide();
				$('#heatMap').attr('style', "position: absolute; z-index: 9999; pointer-events: none; filter:alpha(opacity=60); opacity: 0.6;");
				$('#heatMapImage').attr('style', "pointer-events: none; cursor: hand;");
				$('#heatMapImage').attr('alt', "<iwcm:text key="components.stat.heatmap.clickToCloseAndContinue"/>");
			}
		})
	});
-->
</script>	
</c:if>