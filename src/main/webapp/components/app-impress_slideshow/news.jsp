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
<%
	//stranka pre includnutie noviniek
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);

	boolean ascending = pageParams.getBooleanValue("asc", true);



	int nivoSliderHeight = pageParams.getIntValue("nivoSliderHeight",400);//OK
	// image moznosti - none, top, bottom
	String image = pageParams.getValue("image", "none");
	//ziskaj DocDB
	DocDB docDB = DocDB.getInstance();
	int actualDocId = -1;
	try {
		actualDocId = Integer.parseInt(request.getParameter("docid"));
	} catch (Exception ex) {
		//sk.iway.iwcm.Logger.error(ex);
	}
	//vyradime zo zobrazenia aktualnu stranku

	int height = pageParams.getIntValue("nivoSliderHeight", 400);
	int counter = 0;

	String json = pageParams.getValue("editorData", "W10=");
	JSONArray itemsList = new JSONArray(URLDecoder.decode(StringUtils.newStringUtf8(Base64.decodeBase64(json)), "UTF-8"));
%>
<link
	href='https://fonts.googleapis.com/css?family=Open+Sans+Condensed:700,300,300italic'
	rel='stylesheet' type='text/css'>
<link rel="stylesheet" type="text/css"
	href="/components/app-impress_slideshow/css/style.css" />
<!--[if lt IE 9]>
		<link rel="stylesheet" type="text/css" href="css/style_ie.css" />
<![endif]-->
<style type="text/css">

<% if(itemsList.length()>0){ %>

		<% for(int i = 0; i < itemsList.length(); i++) {
			JSONObject item = itemsList.getJSONObject(i);
			counter++;
		%>

			<%
			if (Tools.isNotEmpty((String)item.get("backgroundColor"))) {
			%>
			.color-<%=counter%> {
				background-color: <%=item.get("backgroundColor")%>;
			}
			<% } %>
		<% }%>

<%}%>

	.jms-slideshow { height: <%=height + 40%>px; }
	.jms-wrapper { height: <%=height + 40%>px; }
	.step { height: <%=height%>px; }
</style>
<script type="text/javascript"
	src="/components/app-impress_slideshow/js/jmpress.js"></script>
<script type="text/javascript"
	src="/components/app-impress_slideshow/js/jquery.jmslideshow.js"></script>
<script type="text/javascript"
	src="/components/app-impress_slideshow/js/modernizr.custom.94160.js"></script>
<noscript>
	<style type="text/css">
	.step {
		width: 100%;
		position: relative;
	}
	.step:not(.active) {
		opacity: 1;
		filter: alpha(opacity=99);
		-ms-filter: "progid:DXImageTransform.Microsoft.Alpha(opacity=99)";
	}
	.step:not(.active) a.jms-link{
		opacity: 1;
		margin-top: 40px;
	}
	</style>
</noscript>
<%
counter = 0;

String effects[] = new String[5];
effects[0] = "";
effects[1] = "data-x=\"2000\" data-z=\"3000\" data-rotate=\"170\"";
effects[2] = "data-y=\"500\" data-scale=\"0.4\" data-rotate-x=\"30\"";
effects[3] = "data-x=\"3000\"";
effects[4] = "data-x=\"4500\" data-z=\"1000\" data-rotate-y=\"45\"";

//request.removeAttribute("novinky");
%>
<% if(itemsList.length()>0){ %>
	<section id="jms-slideshow" class="jms-slideshow">
	<% for(int i = 0; i < itemsList.length(); i++) { %>
	<% JSONObject item = itemsList.getJSONObject(i);
		counter++;
	%>
		<div class="step" data-color="color-<%=counter %>" <% if (counter<effects.length) { out.print(effects[counter-1]); } %>>
				<div class="jms-content">
					<h3 style = "color:<%=item.get("headingColor")%>!important; <%=item.get("customStyleHeading")%>"><%=item.get("title")%></h3>
					<div  style = "color:<%=item.get("subheadingColor")%>!important; <%=item.get("customStyleSubHeading")%>">
					<%=item.get("subtitle")%>
					</div>
					<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
					<a class="jms-link" href="<%=(String)item.get("redirectUrl")%>"> Viac </a>
					<% } %>
				</div>
				<%
					String perexImage = (String)item.get("image");
				%>
				<img src="/thumb/<%=perexImage%>?w=<%=pageParams.getValue("imageWidth", "400")%>&h=<%=pageParams.getValue("imageHeight", "300")%>&ip=5" />
			</div>

	<% } %>
	</section>

<%}else{ %>




	<section id="jms-slideshow" class="jms-slideshow">
		<div class="step" data-color="color-1">
			<div class="jms-content">
				<h3>Just when I thought...</h3>
				<p>From fairest creatures we desire increase, that thereby beauty's rose might never die</p>
				<a class="jms-link" href="#">Read more</a>
			</div>
			<img src="images/1.png" />
		</div>
		<div class="step" data-color="color-2" data-y="500" data-scale="0.4" data-rotate-x="30">
			<div class="jms-content">
				<h3>Holy cannoli!</h3>
				<p>But as the riper should by time decease, his tender heir might bear his memory</p>
				<a class="jms-link" href="#">Read more</a>
			</div>
			<img src="images/2.png" />
		</div>
		<div class="step" data-color="color-3" data-x="2000" data-z="3000" data-rotate="170">
			<div class="jms-content">
				<h3>No time to waste</h3>
				<p>Within thine own bud buriest thy content and, tender churl, makest waste in niggarding</p>
				<a class="jms-link" href="#">Read more</a>
			</div>
			<img src="images/3.png" />
		</div>
		<div class="step" data-color="color-4" data-x="3000">
			<div class="jms-content">
				<h3>Supercool!</h3>
				<p>Making a famine where abundance lies, thyself thy foe, to thy sweet self too cruel</p>
				<a class="jms-link" href="#">Read more</a>
			</div>
			<img src="images/4.png" />
		</div>
		<div class="step" data-color="color-5" data-x="4500" data-z="1000" data-rotate-y="45">
			<div class="jms-content">
				<h3>Did you know that...</h3>
				<p>Thou that art now the world's fresh ornament and only herald to the gaudy spring</p>
				<a class="jms-link" href="#">Read more</a>
			</div>
			<img src="images/5.png" />
		</div>
	</section>
<% } %>


<script type="text/javascript">
	$(function() {

		var jmpressOpts	=
		{
			viewPort        : {
		        height  : <%=height%>,
		        width   : 1000,
		        maxScale: 1
		   },
			animation		: { transitionDuration : '0.8s' }
		};

		$( '#jms-slideshow' ).jmslideshow( $.extend( true, { jmpressOpts : jmpressOpts }, {
			autoplay	: true,
			bgColorSpeed: '0.8s'
		}));

	});
</script>