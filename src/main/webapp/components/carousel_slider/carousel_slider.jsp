<%@page import="java.util.List"%><%@page import="java.net.URLDecoder"%>
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
<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);

String json = pageParams.getValue("editorData", "W10=");
JSONArray itemsList = new JSONArray(URLDecoder.decode(StringUtils.newStringUtf8(Base64.decodeBase64(json)), "UTF-8"));

String style = pageParams.getValue("style", "01");

String skin = pageParams.getValue("skin","Classic");
boolean fullWidthSlider = pageParams.getBooleanValue("fullWidthSlider",true);
int carouselWidth = pageParams.getIntValue("carouselWidth",900);
int carouselHeight = pageParams.getIntValue("carouselHeight",300);
int imageWidth = pageParams.getIntValue("imageWidth",300);
int imageHeight = pageParams.getIntValue("imageHeight",300);

//css styl pre kazdy skin
String filePath = "/components/carousel_slider/css/" + skin + ".jsp";
IwcmFile f = new IwcmFile(Tools.getRealPath(filePath));
if (f.exists() && f.isFile())
{
	%><style><%
		pageContext.include(filePath);
	%></style><%
}
// zavolanie init js pre zvoleny skin:
filePath = "/components/carousel_slider/skins/" + skin + ".jsp";
if (f.exists() && f.isFile())
{
    pageContext.include(filePath);
}

//custom id
Integer multipleCarouselSliderId = (Integer)request.getAttribute("multipleCarouselSliderId");
if (multipleCarouselSliderId == null) multipleCarouselSliderId = Integer.valueOf(1);
%>

  <iwcm:script src="/components/carousel_slider/carouselengine/amazingcarousel.js"></iwcm:script>

    
<div style="margin:0px auto;">
	<div id="amazingcarousel-container-<%=multipleCarouselSliderId%>">
	    <div id="amazingcarousel-<%=multipleCarouselSliderId%>" style="display:none;position:relative;width:100%;max-width:<%=carouselWidth%>px;margin:0px auto 0px;">
	        <div class="amazingcarousel-list-container">
	           <ul class="amazingcarousel-list">

				<% for(int i = 0; i < itemsList.length(); i++) { %>
            		<% JSONObject item = itemsList.getJSONObject(i); %>   
            		<li class="amazingcarousel-item">
            			<div class="amazingcarousel-item-container">
	                        <div class="amazingcarousel-image">
<%-- 		                        <% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %> --%>
<%-- 					                <a href="<%=item.get("redirectUrl") %>" title="<%=item.get("title") %>" class="html5lightbox" data-group="amazingcarousel-0"> --%>
<%-- 					    		<% } %> --%>

						<!--	NAHRADENY VOLITELNY LINK PRIAMO LIGHT BOXOM -->
								<% if(pageParams.getBooleanValue("showLightbox",true)) { %>
									<a href="<%=item.get("image") %>" class="html5lightbox" data-group="amazingcarousel-<%=multipleCarouselSliderId%>">
								<%} %>
					    				<img src="/thumb<%=item.get("image") %>?w=<%=imageWidth%>&h=<%=imageHeight%>&ip=5&q=80" alt="<%=item.get("title") %>"/>
					    		<% if(pageParams.getBooleanValue("showLightbox",true)) { %>
					    		</a>
					    		<%} %>
					    				<% if(skin.equals("Stylish")) { %>
						    				<div class="amazingcarousel-text">
												<div class="amazingcarousel-text-bg"></div>
												<div class="amazingcarousel-title"></div>
											</div>
										<% } %>
<%-- 			            		<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %> --%>
<!-- 			                		</a> -->
<%-- 			                	<% } %> --%>
			                </div>
		                    <% if(!(skin.equals("Fashion") || skin.equals("Rotator"))) { %>
					        	<div class="amazingcarousel-title"><%=item.get("title") %></div>
					        <% } %>
					        <% if(skin.equals("Gallery")){ %>
                    			<div class="amazingcarousel-description"><%=item.get("description") %></div>
                    		<% } %>
					  	</div>
            		</li>
            	 <% } %>


	           </ul>           
            
	            <div class="amazingcarousel-prev"></div>
	            <div class="amazingcarousel-next"></div>
	        </div>
	        <div class="amazingcarousel-nav"></div>
	    </div>
	</div>
</div>



<% request.setAttribute("multipleCarouselSliderId", Integer.valueOf(multipleCarouselSliderId.intValue()+1)); %>