<%@page import="java.net.URLDecoder"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@page import="org.apache.commons.codec.binary.StringUtils"%>
<%@page import="org.apache.commons.codec.binary.Base64"%>
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

String json = pageParams.getValue("editorData", "W10=");
JSONArray itemsList = new JSONArray(URLDecoder.decode(StringUtils.newStringUtf8(Base64.decodeBase64(json)), "UTF-8"));

String style = pageParams.getValue("style", "01");

int counter = 0;

//animate
boolean animate = pageParams.getBooleanValue("animate", true);
if (request.getAttribute("componentCode")!=null) animate = false; //vypnutie animacie v editore
int delay = pageParams.getIntValue("animateDelay", 200);
int dataSpeed = pageParams.getIntValue("animateSpeed", 1000);


//custom id
Integer testImonialsId = (Integer)request.getAttribute("testImonialsId");
if (testImonialsId == null) testImonialsId = Integer.valueOf(1);
%>






<style>

#testImonials<%=testImonialsId %> .testimonials_content{
	width:auto !important;
padding:0px !important;
margin-left:20px;
float:left;
margin-top:15px;
max-width:45%;
}



#testImonials<%=testImonialsId %> .testimonials-avatar{
float:left;
	border-radius:50% ;
	height:100px ;
	width:100px;
	margin:10px;
		background-position: center;
}

#testImonials<%=testImonialsId %> .testimonials-text h3{

	font-size:13px;
	padding:3px 0px !important;
}

#testImonials<%=testImonialsId %> .testimonials-text{
float:left;
	margin:0px;
	padding:20px;
	height:auto;
	width:auto;
	
	background: <%=pageParams.getValue("backgroundColor", "#fff")%> none repeat scroll 0 0 ;
    border: 1px solid <%=pageParams.getValue("backgroundColor", "#fff")%>;
    border-radius: 2px;
    font-size: 15px;
    line-height: 20px;
    padding: 15px 20px;
    position: relative;

}



#testImonials<%=testImonialsId %> .testimonials-text p{
padding:0px !important;
}

#testImonials<%=testImonialsId %> .testimonials-arrow2{
	 background: rgba(0, 0, 0, 0) url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABYAAAAUCAYAAACJfM0wAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAyBpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMC1jMDYwIDYxLjEzNDc3NywgMjAxMC8wMi8xMi0xNzozMjowMCAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RSZWY9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZVJlZiMiIHhtcDpDcmVhdG9yVG9vbD0iQWRvYmUgUGhvdG9zaG9wIENTNSBXaW5kb3dzIiB4bXBNTTpJbnN0YW5jZUlEPSJ4bXAuaWlkOkU5NkJEMTI1RkVFMjExRTRBQ0Q2QkQzRDUzNzhERkRCIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOkU5NkJEMTI2RkVFMjExRTRBQ0Q2QkQzRDUzNzhERkRCIj4gPHhtcE1NOkRlcml2ZWRGcm9tIHN0UmVmOmluc3RhbmNlSUQ9InhtcC5paWQ6RTk2QkQxMjNGRUUyMTFFNEFDRDZCRDNENTM3OERGREIiIHN0UmVmOmRvY3VtZW50SUQ9InhtcC5kaWQ6RTk2QkQxMjRGRUUyMTFFNEFDRDZCRDNENTM3OERGREIiLz4gPC9yZGY6RGVzY3JpcHRpb24+IDwvcmRmOlJERj4gPC94OnhtcG1ldGE+IDw/eHBhY2tldCBlbmQ9InIiPz5twVjIAAABAElEQVR42qzVPQ6CMBgGYOhGuEYHiGHiKt7COzh4BxP1CnoU2DRh6A1cIIGwEGtf0iaa2H+bNLRAn36Qr23KOU9MpW3bTFxoURR79LuuO4gLq+t6Ng4ErKtN02SiVuM4XrksaOMenpnGeqE+uDfqigehLngwasOjUBMejerwFA2Vp3meb5OIMk3TTeU5oi1jItVEXhKhz8uyPJM/FWnN6h9Xfd+fYqOFAQsmkRPdGWPHYRjOoZFiLAxYX3uFmCkNjVxFCkO3QLzxX6huSa+4+LSLDcU7v1DTJkRs+AdKfLdNLW5DjbAOd0HXJe1wNCElN5TSHfoypR7iaHqZxr0FGAD0S4y/qplbTAAAAABJRU5ErkJggg==") repeat scroll 0 0;
    display: none;
    height: 20px;
    position: absolute;
    top: 23px;
   background-position: right center;
    right: -11px;
    width: 11px;

}

#testImonials<%=testImonialsId %> .testimonials-text p a {
    color:<%=pageParams.getValue("textColor", "#000")%>;

}

@media screen and (max-width: 550px) {
   #testImonials<%=testImonialsId %> .testimonials-avatar{
   		display:none !important;
   }
   
   #testImonials<%=testImonialsId %> .testimonials-arrow2{
   	display:none !important;
   }
   

}

#testImonials<%=testImonialsId %> .triangle {
	width: 0; 
	height: 0; 
	border-left: 50px solid transparent; 
	border-right: 50px solid transparent; 
	border-top: 50px solid <%=pageParams.getValue("backgroundColor", "#fff")%>; 
}
</style>

<script>

$(document).ready(function(){
	
	// ------------- nastavenie rovnakej vysky a sirky ----------
	var maxHeight = -1;
	var maxWidth = -1;

	$("#testImonials<%=testImonialsId %> .testimonials-text").each(function() {
	    var h = $(this).height(); 
	    maxHeight = h > maxHeight ? h : maxHeight;
	});
	
	$("#testImonials<%=testImonialsId %> .testimonials-text").each(function() {
	    var w = $(this).width(); 
	    maxWidth = w > maxWidth ? w : maxWidth;
	});
	
	$("#testImonials<%=testImonialsId %> .testimonials-text").each(function() {
		if($( document ).width()>768){
	   $(this).height(maxHeight); 
	   $(this).width(maxWidth); }

	});
	
	// ---------END ---  nastavenie rovnakej vysky a sirky --------
	$("#testImonials<%=testImonialsId %> .triangle").each(function() {
		  $(this).css("margin-top",maxHeight+"px"); 
		});
});
</script>
<div id="testImonials<%=testImonialsId %>" class="clearfix row testimonialsBoxes1">

<% for(int i = 0; i < itemsList.length(); i++) { %>
	<% JSONObject item = itemsList.getJSONObject(i); %>	
				<%
				counter++;
				String easyingStyle = "";
				String customStyle = "";
				if (animate)
				{
					customStyle = "-webkit-transition: all "+dataSpeed+"ms ease; -moz-transition: all "+dataSpeed+"ms ease; -ms-transition: all "+dataSpeed+"ms ease; -o-transition: all "+dataSpeed+"ms ease; transition: all "+dataSpeed+"ms ease;";
					easyingStyle = "wow slideInLeft";
					if (counter > 1) easyingStyle = "wow slideInUp";
					if (counter > 2) easyingStyle = " wow slideInRight";
				}
				%><div class="col-xs-12 col-sm-6 col-md-4 testimonials_content <%=easyingStyle %>" data-delay="<%=delay %>" data-speed="<%=dataSpeed %>" style="<%=customStyle%>">
				<span class="inner">
				
				<div class="testimonials-text" >
					<p style="color: <%=pageParams.getValue("textColor", "#000")%>; <%=pageParams.getValue("customStyleTextTestimonials", "") %>">
			
					<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
						<a href="<%=item.get("redirectUrl") %>" title="<%=item.get("title") %>">
					<% } %>	
						<%=item.get("description") %>
					<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
						</a>
					<% } %>
					
					
				</p>
				
				<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
						<a  class="jms-link"  href="<%=item.get("redirectUrl") %>" title="<%=item.get("title") %>">
				<% } %>
				
					
					
				<h3 class="testimonials-name" style="color:<%=pageParams.getValue("nameColor", "#000")%>; <%=pageParams.getValue("customStyleMeno", "") %>">
				<% 	
				if(pageParams.getBooleanValue("showName", true)){
					out.print(item.get("title"));
				}
				%>
						
					
				</h3>
				
				<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
				</a>
				<% } %>
				
							<% if(item.get("image")!=null){ %>
				<span class="testimonials-arrow2"></span><%} %>
							 
				
					</div>	
					
					
				
			  <%   if(pageParams.getValue("showPhoto","").equals("yes")){%>
							 <div class="triangle"></div>
			
			  <span class="testimonials-avatar" <% if(item.get("image")!=null){ %>style="background-image:url(/thumb<%=item.get("image")%>?w=100&h=100&ip=5);"<%} %>></span>
			  <%
						 
				 } %>

				</span>		
			</div>
		
		
	<%} %>
</div>



<% request.setAttribute("testImonialsId", Integer.valueOf(testImonialsId.intValue()+1)); %>