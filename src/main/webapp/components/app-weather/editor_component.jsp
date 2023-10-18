<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*, sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%
request.setAttribute("cmpName", "app-weather");
request.setAttribute("iconLink", "/components/app-weather/editoricon.png");
%>

<%@page import="org.apache.struts.util.ResponseUtils"%>
<jsp:include page="/components/top.jsp"/>

<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);

	out.println(Tools.insertJQuery(request));
%>
<script type='text/javascript'>

function Ok()
{
	var form = document.textForm;
	var place = form.city.value;
	var pocetDni = form.days.value;
	var lat = form.lat.value;
    var lon = form.lon.value;
	var parametre = "";

	if(place !== "")	parametre += ", city=\""+place+"\"";
	if(pocetDni !== "") parametre += ", days="+pocetDni;
    if(lat !== "") parametre += ", lat="+lat;
    if(lon !== "") parametre += ", lon="+lon;

	oEditor.FCK.InsertHtml("!INCLUDE(/components/app-weather/xml-pocasie.jsp"+parametre+")!");
	return true;
} // End function
</script>
<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">
	  <div >

	  	 <div class="form-group clearfix" id="pocetDniRow">
			<div class="col-xs-4"><iwcm:text key="components.pocasie.pocetDni"/>:</div>
			<div class="col-xs-8"><input type="text" name="days" value="<%=ResponseUtils.filter(pageParams.getValue("days", "8"))%>" ></div>
		</div>
	  	 <div class="form-group clearfix">
	        <div class="col-xs-4"><iwcm:text key="components.pocasie.place"/>:</div>
	        <div class="col-xs-8">
	        	<input type="text" name="city" value="<%=ResponseUtils.filter(pageParams.getValue("city", "Bratislava"))%>" >
	        </div>
	     </div>
		  <div class="form-group clearfix">
			  <div class="col-xs-4"><iwcm:text key="components.map.latitude"/></div>
			  <div class="col-xs-8">
				  <input type="text" name="lat" value="<%=ResponseUtils.filter(pageParams.getValue("lat", "48.15"))%>" >
			  </div>
		  </div>
		  <div class="form-group clearfix">
			  <div class="col-xs-4"><iwcm:text key="components.map.longitude"/></div>
			  <div class="col-xs-8">
				  <input type="text" name="lon" value="<%=ResponseUtils.filter(pageParams.getValue("lon", "17.1167"))%>" >
			  </div>
		  </div>
	</div>
  </form>
</div>

<jsp:include page="/components/bottom.jsp"/>
