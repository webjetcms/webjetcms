
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.i18n.Prop"%><%@
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
	Prop prop = Prop.getInstance(lng);

	DocDetails doc = (DocDetails)request.getAttribute("docDetails");
	int docId = ((doc==null)? -1 : doc.getDocId());
	String docTitle = ((doc==null)? " " : doc.getTitle());
	String docLink = ((doc==null)? " " : doc.getDocLink());

	PageParams pageParams = new PageParams(request);

	String cookie_title = pageParams.getValue("cookie_title", null);
	if (Tools.isEmpty(cookie_title)) cookie_title=prop.getText("components.app-cookiebar.cookie_title");
	String cookie_text = pageParams.getValue("cookie_text", null);
	if (Tools.isEmpty(cookie_text)) cookie_text=prop.getText("components.app-cookiebar.cookie_text");
	String cookie_ButtonText = pageParams.getValue("cookie_ButtonText", null);
	if (Tools.isEmpty(cookie_ButtonText)) cookie_ButtonText=prop.getText("components.app-cookiebar.cookie_ButtonText");
	String cookie_ButtonTextDecline = pageParams.getValue("cookie_ButtonTextDecline", null);
	if (Tools.isEmpty(cookie_ButtonTextDecline)) cookie_ButtonTextDecline=prop.getText("components.app-cookiebar.cookie_ButtonTextDecline");

	String color_text = pageParams.getValue("color_text", "");
	String color_button = pageParams.getValue("color_button", "");
	String color_background = pageParams.getValue("color_background", "");
	String color_buttonText = pageParams.getValue("color_buttonText", "");
	String color_title = pageParams.getValue("color_title","");

	String position = pageParams.getValue("position","bottom");

	boolean checkbox_title = pageParams.getBooleanValue("checkbox_title",false);

	int padding_top = pageParams.getIntValue("padding_top",25);
	int padding_bottom = pageParams.getIntValue("padding_bottom",25);

	//custom id
	Integer multipleSliderId = (Integer)request.getAttribute("multipleSliderId");
	if (multipleSliderId == null) multipleSliderId = Integer.valueOf(1);
%>
<script type="text/javascript" src="/components/_common/javascript/jquery.cookie.js"></script>
<script type="text/javascript">
	function cookiesButtonClicked(acceptAll){
		$.cookie("userUnderstandsOurCookiePolicy","accepted", { expires: 365, path: '/' });
		$.cookie("cookies-gdpr-policy", "saved", {path: '/', expires: 365});

		var rels = [];
		if (acceptAll) {
			var cookieClassification = "<%=Constants.getString("gdprCookieClassificationsDefault")%>";

			$.each(cookieClassification.split(","), function(index, item) {
				rels.push(item);
			});
		} else {
			rels.push("nutne");
		}

		var categories = rels.join("_")
		$.cookie("enableCookieCategory", categories, {path: '/', expires: 365});

		if (typeof window.dataLayer != "undefined") {
				try {
					gtag('consent', 'update', gtagGetConsentJson(categories));
					dataLayer.push({'event': 'consent-update'});
				} catch (e) {}
		}

		$.ajax({
			url: "/components/gdpr/cookie_save_ajax.jsp",
			method: "post",
			data: {
				categories: categories
			},
			success: function() {
				<% if (pageParams.getBooleanValue("reload", true)) {%>
				window.location.reload();
				<% } %>
			}
		});

		$('#cookiebar-<%=multipleSliderId%>').fadeOut();
	}
	$( document ).ready(function() {
		var cookieValue = $.cookie("userUnderstandsOurCookiePolicy");
		var gdprCookieValue = $.cookie("cookies-gdpr-policy");
		if(!(cookieValue==="accepted") && !(gdprCookieValue==="saved")){
			$('#cookiebar-<%=multipleSliderId%>').fadeIn();
		}
	});
</script>
<style>
	.editPeviewWindow #cookiebar-<%=multipleSliderId%> {
		display:block !important;
	}
	#cookiebar-<%=multipleSliderId%> {
		display:none;
		width: 100%;
		height: auto;
		margin: 0px;
		<% if (Tools.isNotEmpty(color_background)) out.println("background-color:"+color_background+";"); %>
		position: fixed;
		<% if(position.equals("bottom")){%>
			bottom:0;
			border-top: 3px solid <% if (Tools.isNotEmpty(color_button)) out.print(color_button); %>;
		<% } else {%>
			top:0;
			border-bottom: 3px solid <% if (Tools.isNotEmpty(color_button)) out.print(color_button); %>;
		<% } %>

		left:0;
		right:0;
		z-index:99998;
	}
	#cookiebar-<%=multipleSliderId%> div.container {
		padding: <%=padding_top%>px 0px <%=padding_bottom%>px;
	}
	<% if(checkbox_title) { %>
		#cookiebar-<%=multipleSliderId%> h2 {
			margin: 0 0 20px 0;
			padding:0px;
			<% if (Tools.isNotEmpty(color_title)) out.println("color:"+color_title+";"); %>
			font-size: 30px;
		}
	<%}%>
	#cookiebar-<%=multipleSliderId%> p {
		vertical-align:middle;
		<% if (Tools.isNotEmpty(color_text)) out.println("color:"+color_text+";"); %>
		letter-spacing: 1px;
	}
	#cookiebar-<%=multipleSliderId%> button.cookie-btn {
		<%
		if (Tools.isNotEmpty(color_button)) out.println("background-color:"+color_button+";");
		if (Tools.isNotEmpty(color_buttonText)) out.println("color:"+color_buttonText+";");
		%>
		min-height: 50px;
		margin-bottom: 8px;
	}
</style>
<div id="cookiebar-<%=multipleSliderId%>" class="alert alert-secondary cookiebar">
	<div class="container">
		<div class="row">
			<div class="col-md-10 col-xs-12 col-sm-9">
				<%
					if(checkbox_title){
				%>
					<h2><%=cookie_title%></h2>
				<%} %>
				<p><%=cookie_text%></p>
			</div>
			<div class="col-md-2 col-xs-12 col-sm-3">
					<button onclick="cookiesButtonClicked(true)" class="btn btn-primary cookie-btn"><%=cookie_ButtonText%></button>
					<button onclick="cookiesButtonClicked(false)" class="btn btn-secondary cookie-btn"><%=cookie_ButtonTextDecline%></button>
			</div>
		</div>
	</div>
</div>

<% if (pageParams.getBooleanValue("showLink", false)) { %>
    <iwcm:text key="components.app-cookiebar.cookiesSettingsLink" param1="<%=String.valueOf(multipleSliderId)%>"/>
<% } %>

<% request.setAttribute("multipleSliderId", Integer.valueOf(multipleSliderId.intValue()+1)); %>

