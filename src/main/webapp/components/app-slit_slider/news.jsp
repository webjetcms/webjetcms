<%@page import="java.util.List"%>
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*,sk.iway.iwcm.tags.support.ResponseUtils"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@page import="java.net.URLDecoder"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.apache.commons.codec.binary.StringUtils"%>
<%@page import="org.apache.commons.codec.binary.Base64"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%
	//stranka pre includnutie noviniek
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);


	int pageSize = 5;
	int maxCols = pageParams.getIntValue("cols", 1);

	String json = pageParams.getValue("editorData", "W10=");
	JSONArray itemsList = new JSONArray(URLDecoder.decode(StringUtils.newStringUtf8(Base64.decodeBase64(json)), "UTF-8"));

	int height = pageParams.getIntValue("nivoSliderHeight", 500);
	int counter = 0;
%>
<link
	href='https://fonts.googleapis.com/css?family=Open+Sans+Condensed:700,300,300italic'
	rel='stylesheet' type='text/css'>
<link rel="stylesheet" type="text/css"
	href="/components/app-slit_slider/css/style.css" />
	<link rel="stylesheet" type="text/css"
	href="/components/app-slit_slider/css/style2.css" />

<style type="text/css">

.sl-slider h2 {
padding: 0px;
margin-bottom:0px;
margin-left:20px;
}
.sl-slider blockquote {
    padding-top: 0px;
}
</style>
	<iwcm:script type="text/javascript"
	src="/components/app-slit_slider/js/modernizr.custom.79639.js"></iwcm:script>
<iwcm:script type="text/javascript"
	src="/components/app-slit_slider/js/jquery.ba-cond.min.js"></iwcm:script>

<iwcm:script type="text/javascript"
	src="/components/app-slit_slider/js/jquery.slitslider.js"></iwcm:script>


	  <div id="slider" class="sl-slider-wrapper" style="height: <%=height%>px;">
	  				<div class="sl-slider">
	  				<%
				String sliceOrientation;
	  				int randOrientation;
			%>

<% for(int i = 0; i < itemsList.length(); i++) { %>
	<% JSONObject item = itemsList.getJSONObject(i);

				counter++;
				randOrientation = (int) ( Math.random() * 2 + 1);
				if(randOrientation==1){sliceOrientation="horizontal";}else{sliceOrientation ="vertical";}
				String displayStyle = "";

				if(item.get("subtitle").equals("")){
					displayStyle = " display: none;";
					}
	%>

			<div class="sl-slide" data-orientation="<%=sliceOrientation %>" data-slice1-rotation="-25" data-slice2-rotation="-25" data-slice1-scale="1" data-slice2-scale="2">
						<div class="sl-slide-inner">
						<%

				%>
							<div class="bg-img" style="background-image: url(/thumb<%=item.get("image")%>?w=1200&ip=1); background-color: <%=item.get("backgroundColor")%>!important;"></div>
							<h2 style="padding-bottom:0px !important; padding-top: <%= pageParams.getValue("headingMargin", "0") %>px; text-align:<%= pageParams.getValue("headingAlign", "left") %> ;font-size:<%=pageParams.getValue("headingSize", "70") %>px; color:<%=item.get("headingColor")%>!important;"><%=item.get("title")%></h2>

							<blockquote style="width:100%; <%=displayStyle%>"><p  style="padding-top: <%= pageParams.getValue("subHeadingMargin", "0") %>px; color:<%=item.get("subheadingColor")%>!important; text-align:<%= pageParams.getValue("subHeadingAlign", "left") %> ;font-size:<%=pageParams.getValue("subHeadingSize", "30") %>px;">
									<%=item.get("subtitle")%>
						</p>
						<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
					<a href="<%=item.get("redirectUrl") %>"> Viac </a>
					<% } %></blockquote>
						</div>
					</div>

	<% } %>

			<%
				String currentSpan;
				boolean currentSpanAdd = true;
					%>


		          <nav id="nav-dots" class="nav-dots">
		          <% for(int i = 0; i < itemsList.length(); i++) { %>

			<% if(currentSpanAdd){currentSpan = "class=\"nav-dot-current\""; currentSpanAdd= false;} else{currentSpan="";} %>
					<span <%=currentSpan%>></span>
					<%} %>
				</nav>
	</div>
	</div>


<iwcm:script type="text/javascript">
	$(function() {
		nacitajSlider();
	});
</iwcm:script>