
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*"%><%@ 
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@ 
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
	final String WIDTH_LIKE = "";
	final String STANDARD = "standard";
	final String LIKE = "like";
	PageParams pageParams = new PageParams(request);
	String dataHrefLike = Tools.getBaseHref(request);
	if ("lajkovat_aktualne".equals(pageParams.getValue("dataHrefLike", "lajkovat_cely_web"))){
				dataHrefLike += PathFilter.getOrigPath(request);
	}
	String widthLike = pageParams.getValue("widthLike",
			WIDTH_LIKE);
	if (widthLike == "") {
		widthLike = WIDTH_LIKE;
	}
	String layoutLikeButton = pageParams.getValue("layoutLikeButton",
				STANDARD);
	if (layoutLikeButton == "") {
		layoutLikeButton = STANDARD;
	}
	String actionLikeButton = pageParams.getValue("actionLikeButton",
				LIKE);
	if (actionLikeButton == "") {
		actionLikeButton = LIKE;
	}
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

<div class="fb-like" data-href="<%=dataHrefLike%>"
	data-width="<%=widthLike%>" data-layout="<%=layoutLikeButton%>"
	data-action="<%=actionLikeButton%>" data-show-faces="true"
	data-share="true"></div>