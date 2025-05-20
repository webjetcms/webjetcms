<%@page import="sk.iway.iwcm.tags.WriteTag"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.io.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
//custom id
Integer multipleCarouselSliderIdId = (Integer)request.getAttribute("multipleCarouselSliderIdId");
if (multipleCarouselSliderIdId == null) multipleCarouselSliderIdId = Integer.valueOf(1);

%>
<style>
/* content */

@import url(https://fonts.googleapis.com/css?family=Open+Sans);

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-image {	
	position: relative;
	padding: 0px;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-image img {
	display: block;
	width: 100%;
	max-width: 100%;
	border: 0;
	margin: 0;
	padding: 0;
	-moz-border-radius: 0px;
	-webkit-border-radius: 0px;
	border-radius: 0px;
	-moz-box-shadow:  0 1px 4px rgba(0, 0, 0, 0.2);
	-webkit-box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-text {
	position:absolute;
	left: 0px;
	bottom: 0px;
	text-align:center;
	width: 100%;
	display: none;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-image:hover div.amazingcarousel-text {	
	display: block;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-text-bg {
	position:absolute;
	top: 0px;
	left: 0px;
	width: 100%;
	height: 100%;
	background-color: #333333;
	opacity: 0.5;
	filter: alpha(opacity=50);
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-title {
	position:relative;
	font:14px 'Open Sans', sans-serif;
	color:#ffffff;
	text-align:center;
	width: 100%;
	padding: 10px 0px;
}

/* carousel */

#amazingcarousel-container-<%=multipleCarouselSliderIdId%> {
	padding: 32px 40px; 
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-list-container { 
	padding: 8px 4px;
	background-color: #fff;
	border: 1px solid #ddd;
}

/* item */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-item-container {
	text-align: center;
}

/* arrows */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-prev {
	left: 0%;
	top: 50%;
	margin-left: -48px;
	margin-top: -16px;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-next {
	right: 0%;
	top: 50%;
	margin-right: -48px;
	margin-top: -16px;
}

/* navigation bullets */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-nav {
	position: absolute;
	width: 100%;
	top: 100%;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-bullet-wrapper {
	margin: 16px auto;
}

</style>

