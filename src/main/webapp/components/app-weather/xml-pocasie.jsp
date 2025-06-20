<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>

<%@ page import="sk.iway.iwcm.components.weather.DayForecastBean" %>
<%@ page import="java.util.List" %>
<%@ page import="sk.iway.iwcm.components.weather.WeatherForecast" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.math.BigDecimal" %>
<%!

	String encode(String str) {
		return Tools.URLEncode(DB.internationalToEnglish(str)).toLowerCase();
	}

%><%
	PageParams pageParams = new PageParams(request);

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	String city = pageParams.getValue("city", "Bratislava");
	BigDecimal lat = Tools.getBigDecimalValue(pageParams.getValue("lat", "48.15"));
	BigDecimal lon = Tools.getBigDecimalValue(pageParams.getValue("lon", "17.1167"));

	WeatherForecast weatherForecast = new WeatherForecast(city, lat, lon);
	List<DayForecastBean> dayForecastBeans = weatherForecast.getDayForecastBeanList();

	int pocetDni = pageParams.getIntValue("days", 8);
%>

<div class="app-weather">
	<%
		for (int i = 0; i < dayForecastBeans.size(); i++) {
			if (i > pocetDni)
				break;
	%>

	<div class="<%= (i == 0) ? "pocasieBlockFirst" : "pocasieBlock"%>">
		<img src="/components/app-weather/images/<%=dayForecastBeans.get(i).getSymbolId()%>.png" />

		<div class="pocTeplota">

			<% if (i != 0) { %>
			<span class="pocDatum">
				<% Calendar cal = Calendar.getInstance();
					cal.setTime(dayForecastBeans.get(i).getDate());
					out.print(cal.get(Calendar.DAY_OF_MONTH) + "." + (cal.get(Calendar.MONTH)+1) + ".");%>
			</span>
			<% } %>
			<span class="pocStupne"><%=dayForecastBeans.get(i).getMinTemperature()%>/<%=dayForecastBeans.get(i).getMaxTemperature()%><span class="c">°C</span></span>
			<% if (i == 0) { %>
			<span class="pocMesto" style="font-size: 12px;"><%=weatherForecast.getCity().getString1()%></span>
			<% } %>

		</div>
	</div>
	<%}
		//hodinova predpoved
		//		for (HourForecastBean hourForecastBean : weatherForecast.getCurrentHourForecast()) {
		//			System.out.println(hourForecastBean.getDateFrom() + " " + hourForecastBean.getDateTo() + " " + hourForecastBean.getSymbol() + " " + hourForecastBean.getTemperature() + " is_night=" + hourForecastBean.isNight());
		//		}
	%>

</div>

<style>
	.app-weather{
		background: #f0f0f0;
		max-width: 100%;
		width: 20rem;
		font-family: Arial, Helvetica, sans-serif;
		padding: 1rem;
		text-align: center;
		display: flex;
		flex-wrap: wrap;
	}
	.app-weather .pocasieBlockFirst{
		width: 100%;
		flex: 100%;
		padding: 0;
		display: flex;
		align-items: flex-start;
		justify-content: center;
		margin-bottom: 20px;
	}
	.app-weather .pocasieBlockFirst .pocTeplota{
		width: auto;
		text-align: left;
		padding: 0 0 0 10px;
	}
	.app-weather .pocasieBlockFirst .pocTeplota .pocStupne{
		font-size: 30px;
		line-height: 30px;
		display: block;
	}
	.app-weather .pocasieBlockFirst .pocTeplota .pocMesto{
		font-size: 12px;
		line-height: 16px;
	}
	.app-weather .pocasieBlock{
		margin: 10px 0 0 0;
		text-align: center;
		flex: 25%;
		min-width: 4rem;
	}
	.app-weather .pocasieBlock .pocTeplota{
		padding: 10px 0 0 0;
	}
	.app-weather .pocasieBlock .pocTeplota .pocDatum{
		display: block;
		font-size: 12px;
		line-height: 16px;
	}
	.app-weather .pocasieBlock .pocTeplota .pocStupne{
		display: block;
		font-size: 12px;
		line-height: 12px;
		font-weight: bold;
	}
</style>

