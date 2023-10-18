
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@ 
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@ 
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@ 
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@ 
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@ 
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@ 
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@ 
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@ 
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);
	final String NUMBER_COMMENTS = "5";
	final String WIDTH_COMMENTS = "980";
	String numberComments = pageParams.getValue("numberComments", NUMBER_COMMENTS);
	if (numberComments == "") {
		numberComments = NUMBER_COMMENTS;
	}
	String widthComments = pageParams.getValue("widthComments", "980");
	if (widthComments == "") {
		widthComments = WIDTH_COMMENTS;
	}
	String baseHref = Tools.getBaseHref(request) + PathFilter.getOrigPath(request);
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

<div class="fb-comments" data-href="<%=baseHref%>"
	data-width="<%=widthComments%>" data-numposts="<%=numberComments%>"
	data-colorscheme="light"></div>