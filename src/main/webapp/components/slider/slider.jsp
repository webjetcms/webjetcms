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

String skin = pageParams.getValue("skin","Classic");
String support_images_path = "/components/slider/support_images/";
boolean fullWidthSlider = pageParams.getBooleanValue("fullWidthSlider",true);
int sliderWidth = pageParams.getIntValue("sliderWidth",900);
int sliderHeight = pageParams.getIntValue("sliderHeight",360);

// zavolanie init slidera pre zvoleny skin:
String filePath = "/components/slider/skins/" + skin + ".jsp";
IwcmFile f = new IwcmFile(Tools.getRealPath(filePath));
if (f.exists() && f.isFile())
{
    pageContext.include("/components/slider/skins/" + skin + ".jsp");
}
if(pageParams.getBooleanValue("ken_burns_on_slide",false)){
    pageContext.include("/components/slider/scripts/kenburns.jsp");
}
//custom id
Integer multipleSliderId = (Integer)request.getAttribute("multipleSliderId");
if (multipleSliderId == null) multipleSliderId = Integer.valueOf(1);
%>

<style type="text/css">
    .amazingslider-box-<%=multipleSliderId%> {
        margin-left:0px;
    }
</style>

<iwcm:script src="/components/slider/scripts/sliderengine/amazingslider.js"></iwcm:script>
<% if(multipleSliderId==1){%>
<link rel="stylesheet" type="text/css" href="/components/slider/scripts/sliderengine/amazingslider-<%=multipleSliderId%>.css">
<% } %>

<div id="amazingslider-wrapper-<%=multipleSliderId%>" style="display:block;position:relative;margin:0px auto 56px;<% if(fullWidthSlider){out.print("max-width:100%;");}else{out.print("max-width:"+sliderWidth+"px; max-height:"+sliderHeight+"px; height:"+sliderHeight+"px;");} %>">
    <div id="amazingslider-<%=multipleSliderId%>" style="display:block;position:relative;margin:0 auto;<% if(fullWidthSlider){out.print("");}else{out.print("width:"+sliderWidth+"px; height:"+sliderHeight+"px;");} %>">
        <ul class="amazingslider-slides" style="display:none;" id="kenBurnsEffect">
        
        	<% for(int i = 0; i < itemsList.length(); i++) { %>
				<% JSONObject item = itemsList.getJSONObject(i); %>			
				<li>
					<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
						<a href="<%=item.get("redirectUrl") %>" title="<%=item.get("title") %>">
					<% }
						String image_source = "/thumb" + item.get("image") + "?ip=5&w=" + sliderWidth + "&h=" + sliderHeight;
					%>
            			<img src="<%=image_source%>" alt="<%=item.get("title") %>"  title="<%=item.get("title") %>" data-description="<%=item.get("description") %>" data-originalheight="<%=sliderHeight%>" />
            		<% if(Tools.isNotEmpty((String)item.get("redirectUrl"))) { %>
            			</a>
            		<% } %>
            	</li>
			<% } %>
        </ul>
        <ul class="amazingslider-thumbnails" style="display:none;">
            <% for(int i = 0; i < itemsList.length(); i++) { %>
				<% JSONObject item = itemsList.getJSONObject(i); %>			
				<li>
					<%
						String thumb_image_source = "/thumb" + item.get("image") + "?ip=5&w=70&h=70";
					%>
            		<img src="<%=thumb_image_source%>" alt="<%=item.get("title") %>" title="<%=item.get("title") %>" />
            	</li>
			<% } %>
        </ul>
    </div>
</div>

<% request.setAttribute("multipleSliderId", Integer.valueOf(multipleSliderId.intValue()+1)); %>