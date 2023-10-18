<%@page import="sk.iway.iwcm.tags.WriteTag"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.io.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
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
}

/* carousel */

#amazingcarousel-container-<%=multipleCarouselSliderIdId%> {
	padding: 32px 48px; 
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-list-container { 
	padding: 16px 0;
}

/* item */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-item-container {
	text-align: center;
	padding: 0px;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-item-container:hover {
	opacity: 0.7;
	filter: alpha(opacity=70);
}

/* arrows */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-prev {
	left: 0%;
	top: 50%;
	margin-left: 0px;
	margin-top: -30px;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-next {
	right: 0%;
	top: 50%;
	margin-right: 0px;
	margin-top: -30px;
}

/* navigation bullets */

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-nav {
	position: absolute;
	width: 100%;
	top: 100%;
}

#amazingcarousel-<%=multipleCarouselSliderIdId%> .amazingcarousel-bullet-wrapper {
	margin: 4px auto;
}

</style>

