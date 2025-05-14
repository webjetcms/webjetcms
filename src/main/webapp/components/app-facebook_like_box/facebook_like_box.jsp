
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@ 
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
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	final String FACEBOOK_HREF = "https://www.facebook.com/webjetcloud.sk";
	final String WIDTH_LIKE_BOX = "300";
	PageParams pageParams = new PageParams(request);
	String dataHrefLikeBox = pageParams.getValue("dataHrefLikeBox",
			FACEBOOK_HREF);
	if (dataHrefLikeBox == "") {
		dataHrefLikeBox = FACEBOOK_HREF;
	}
	String widthLikeBox = pageParams.getValue("widthLikeBox",
			WIDTH_LIKE_BOX);
	if (widthLikeBox == "") {
		widthLikeBox = WIDTH_LIKE_BOX;
	}
	String heightLikeBox = pageParams.getValue("heightLikeBox", "");
	String showFacesLikeBox = pageParams.getValue("showFacesLikeBox",
			"true");
	String showPostLikeBox = pageParams.getValue("showPostLikeBox",
			"true");
	//&appId=660749814016508
%>

<div id="fb-root"></div>
<script type="text/javascript">
	//<![CDATA[

	<% if(request.getAttribute("facebook_js_inserted") == null ||
				request.getAttribute("facebook_js_inserted").equals("false")){ %>
	(function(d, s, id) {
		var js, fjs = d.getElementsByTagName(s)[0];
		if (d.getElementById(id))
			return;
		js = d.createElement(s);
		js.id = id;
		js.src = "//connect.facebook.net/sk_SK/sdk.js#xfbml=1&version=v2.0";
		fjs.parentNode.insertBefore(js, fjs);
	}(document, 'script', 'facebook-jssdk'));
	<% request.setAttribute("facebook_js_inserted", "true"); } %>
	//]]>
</script>

<div class="fb-like-box"  style="background-color: white;" data-href="<%=dataHrefLikeBox%>"
	data-width="<%=widthLikeBox%>" data-height="<%=heightLikeBox%>"
	data-colorscheme="light" data-show-faces="<%=showFacesLikeBox%>"
	data-header="true" data-stream="<%=showPostLikeBox%>"
	data-show-border="true"></div>