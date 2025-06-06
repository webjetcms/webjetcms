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
}

/* carousel */

#amazingcarousel-container-<%=multipleCarouselSliderIdId%> {
	padding: 8px;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-list-container { 
	padding: 0;
}

/* item */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-item-container {
	text-align: center;
	padding: 1px;
	background-color: #fff;
}

/* arrows */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-prev {
	left: 50%;
	top: 0%;
	margin-left: -18px;
	margin-top: 0px;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-next {
	left: 50%;
	bottom: 0%;
	margin-left: -18px;
	margin-bottom: 0px;
}

/* navigation bullets */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-nav {
	position: absolute;
	top: 0%;
	left: 100%;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-bullet-wrapper {
	margin: 16px;
}
</style>
