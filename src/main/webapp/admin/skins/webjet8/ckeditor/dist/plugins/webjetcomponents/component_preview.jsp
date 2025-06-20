<%@page import="sk.iway.iwcm.doc.TemplatesDB"%>
<%@page import="sk.iway.iwcm.doc.TemplateDetails"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDetails"%>
<%@page import="sk.iway.iwcm.doc.DocDB"%>

<%@page import="org.apache.commons.codec.binary.Base64"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.tags.WriteTag"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms='<%=Constants.getString("webpagesFunctionsPerms")%>'/><%

String lng = PageLng.getUserLng(request);
//In case if user is logged in EN lng (for example) and in banner preview he want this language (not group language)
String origLng = lng;
int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docid"), 0);
if (docId > 0) {
	DocDetails doc = DocDB.getInstance().getDoc(docId);
	lng = doc.getGroup().getLng();
}
else {
	int groupId = Tools.getIntValue(Tools.getRequestParameter(request, "groupId"), 0);
	if (groupId < 1) groupId = Tools.getIntValue((String) request.getSession().getAttribute("iwcm_group_id"), 0);
	//out.println("groupId: " + groupId);
	if (groupId > 0) {
		GroupDetails groupDetails = GroupsDB.getInstance().getGroup(groupId);
		if (groupDetails != null) {
			lng = groupDetails.getLng();
			docId = groupDetails.getDefaultDocId();
		}
	}
}
//set language also to Spring Locale object
PageLng.setUserLng(request, response, lng);

pageContext.setAttribute("lng", lng);
request.setAttribute("PageLng", lng);
request.setAttribute("inPreviewMode", true);

PageParams pageParams = new PageParams(request);

Identity user = UsersDB.getCurrentUser(request);
if (user == null || user.isAdmin()==false) return;

String componentCode = Tools.getRequestParameterUnsafe(request, "componentCode");  // "!INCLUDE(/components/gallery/gallery.jsp, dir=/images/gallery/banners-tpl_02, alsoSubfolders=false, galleryStyle=nivoSlider, orderBy=priority, orderDirection=asc, start=random, advance=10000, nav=yes, navVAlign=bottom, navHAlign=center, navVSpace=0, navHSpace=0, link=no)!";

//
if (Tools.isNotEmpty(Tools.getRequestParameterUnsafe(request, "encodedComponentCode")))
{
	String encodedComponentCode = Tools.getRequestParameterUnsafe(request, "encodedComponentCode");
	encodedComponentCode = Tools.replace(encodedComponentCode, "|", "=");
    Base64 b64 = new Base64();
	componentCode = new String(b64.decode(encodedComponentCode.getBytes()));
}
//out.print("--sdsdas" + Tools.URLDecode(componentCode) + "--");
boolean showForm = true;

if ("POST".equals(request.getMethod().toUpperCase())) showForm = false;
//if there is no correct refefer dismiss HTML code
String referer = request.getHeader("Referer");
if (referer == null || referer.contains("component_preview.jsp")==false) showForm = true;

//out.print("--sdsdas" + Tools.URLDecode(componentCode) + "--");
if (showForm)
{
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/a-component-preview.js", null, request, response);
	%>
	<html>
	<head></head>
	<body>
		<p>
			<iwcm:text key="editor.inline.waitLoadingPreview"/>
		</p>
		<form action="component_preview.jsp?v=<%=Tools.getNow()%>" method="post" name="componentForm">
			<input type="hidden" name="componentCode" value="-"/>
			<input type="hidden" name="cssStyles" value=""/>
			<input type="hidden" name="docid" value=""/>
			<input type="hidden" name="groupId" value=""/>
		</form>
		<script type="text/javascript">
			var element = window.frameElement;
			var htmlCode = decodeURIComponent( element.getAttribute( 'data-cke-realelement' ) );
			//window.alert(htmlCode);
			var index = htmlCode.indexOf("<article>");
			if (index==-1) index = htmlCode.indexOf("<ARTICLE>");
			var componentCode = htmlCode.substring(index+9, htmlCode.length-10);
			//window.alert("|"+componentCode+"|");

			document.componentForm.componentCode.value=componentCode;

			//ziskaj FCK zoznam stylov
			try
			{
				var ownerDocument = window.frameElement.parentNode.ownerDocument;
				var win= 'defaultView' in ownerDocument ? ownerDocument.defaultView : ownerDocument.parentWindow;

				var editorWindow = win.parent.parent;
				if (win.parent.document.editorForm) editorWindow = win.parent;

				document.componentForm.docid.value = editorWindow.document.editorForm.docId.value;
				document.componentForm.groupId.value = editorWindow.document.editorForm.groupId.value;

				//window.alert(editorWindow.getCkEditorInstance().config.contentsCss);
				if (editorWindow.document.editorForm.ckEditorContentCss && editorWindow.document.editorForm.ckEditorContentCss.value)
				{
					document.componentForm.cssStyles.value = editorWindow.document.editorForm.ckEditorContentCss.value;
				}
				else
				{
					document.componentForm.cssStyles.value = editorWindow.getCkEditorInstance().config.contentsCss.join(",");
				}

			} catch (e) { console.log(e); }
			var timeout = Math.floor(Math.random() * 200);
			setTimeout(function() { document.componentForm.submit(); }, timeout+50);
		</script>
	</body>
	</html>
	<%
}
else
{
	request.setAttribute("componentCode", componentCode);

	if (docId > 0)
	{
		DocDetails doc = DocDB.getInstance().getDoc(docId);
		if (doc != null)
		{
			request.setAttribute("docDetails", doc);
			if (Tools.isNotEmpty(doc.getVirtualPath())) {
				request.setAttribute("path_filter_orig_path", doc.getVirtualPath());
				RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
				if (rb != null) {
					rb.setUrl(doc.getVirtualPath());
					rb.setDocId(doc.getDocId());
            		rb.setGroupId(doc.getGroupId());
				}
			}

			request.setAttribute("doc_id", Integer.valueOf(doc.getDocId()));
			request.setAttribute("group_id", Integer.toString(doc.getGroupId()));
			request.setAttribute("doc_title", doc.getTitle());
			request.setAttribute("doc_title_original", doc.getTitle());
			if (Constants.getBoolean("docTitleIncludePath"))
			{
				request.setAttribute("doc_title", DocDB.getTitleWithPath(doc));
			}
			request.setAttribute("doc_navbar", doc.getNavbar());
			request.setAttribute("html_head", doc.getHtmlHead());
			request.setAttribute("html_data", doc.getHtmlData());
			request.setAttribute("perex_data", doc.getPerex());
			request.setAttribute("perex_pre", doc.getPerexPre());
			request.setAttribute("perex_place", doc.getPerexPlace());
			request.setAttribute("perex_image", doc.getPerexImage());

			GroupDetails group = GroupsDB.getInstance().getGroup(doc.getGroupId());
			if (group != null) request.setAttribute("pageGroupDetails", group);

			TemplateDetails temp = TemplatesDB.getInstance().getTemplate(doc.getTempId());
			if (temp != null) {
				sk.iway.iwcm.doc.ShowDoc.setRequestData(temp, request);
			}
		}
	}

	boolean isMenu = false;
	boolean isSocialIcons = false;
	boolean isGoogleSearch = false;

	if (componentCode.indexOf("/menu/")!=-1) isMenu = true;
	if (componentCode.indexOf("/structuremirroring/")!=-1) isMenu = true;
	if (componentCode.indexOf("app-social_icon")!=-1) isSocialIcons = true; //problem robia oba vizualne varianty
	if (componentCode.indexOf("app-vyhladavanie")!=-1) isGoogleSearch = true;

	%>
	<html>
	<head>
		<%=Tools.insertJQuery(request) %>

		<% if ("cloud".equals(Constants.getInstallName())) { %>
			<link rel="stylesheet" href="/css/menu.css" type="text/css">
		<% } %>

		<%
		String cssStyles = Tools.getRequestParameterUnsafe(request, "cssStyles");
		if (Tools.isEmpty(cssStyles)) cssStyles = "/css/page.css";
		String cssTokens[] = Tools.getTokens(cssStyles, ",");
		String pageCss = null;
		for (String cssStyle : cssTokens)
		{
			%><link rel="stylesheet" href="<%=cssStyle %>" type="text/css" /><%
			if (cssStyle.endsWith("page.css")) pageCss = cssStyle;

			//over ci neexistuje JS verzia suboru
			String jsFile = cssStyle;
			jsFile = Tools.replace(jsFile, "/css/", "/js/");
			jsFile = Tools.replace(jsFile, ".min.css", ".js");

			if (FileTools.isFile(jsFile) && jsFile.endsWith(".js")) {
				%><script type="text/javascript" src="<%=jsFile%>" ></script><%
			}
			//ak to nejde z dist priecinka skus pohladat aj global-functions.js
			//v dist to mame typicky integrovane do ninja.js
			if (jsFile.contains("/dist/")==false) {
				jsFile = Tools.replace(jsFile, "/ninja.js", "/global-functions.js");
				if (FileTools.isFile(jsFile) && jsFile.endsWith(".js")) {
					%><script type="text/javascript" src="<%=jsFile%>" ></script><%
				}
			}
		}
		if (pageCss != null && cssStyles.indexOf("bootstrap")==-1)
		{
			String bootstrap = Tools.replace(pageCss, "/page.css", "/bootstrap.min.css");

			if (FileTools.isFile(bootstrap))
			{
				%><link rel="stylesheet" href="<%=bootstrap %>" type="text/css" media="screen"/>
				<script type="text/javascript">
				//$(window.frameElement).css("min-width", "993px");
				</script>
				<%
			}
		}

		//request.setAttribute("writeTagDontShowError", "true");
		%>
		<% if (FileTools.isFile("/css/basket.css")) { %>
		<link rel="stylesheet" href="/css/basket.css" type="text/css" />
		<% } %>
		<link rel="stylesheet" href="/components/_common/admin/inline/inline.css" type="text/css" />
		<style type="text/css">
			/*
			a.clickDisabler
			{
				position: absolute;
				top: 0px;
				left: 0px;
				display: block;
				w1idth: 100%;
				h1eight: 1000px;
				cursor: pointer;
				z-index: 9999;
				text-decoration: none;
				background-color: green;
			}
			*/

			/*Menu FIX*/
			.navbar-nav>li {
   				 float: left;
				}
			.navbar-wrapper.navSetColor-navNo .navbar-nav {
   				 background-color: transparent !important;
				}
			.navbar-wrapper.nav-B .navbar-nav {
  				  float: none;
 				  display: inline-block;
				}
			.navbar-wrapper .navbar-nav {
				    padding-right: 0;
				    padding-left: 0;
				    background-color: #666;
				}

			.navbar-nav {
				    margin: 0;
				}

			body { background-color: transparent; background-image: none; padding: 0px; overflow: hidden; }
			#content
			{
				margin-top: 0px;
				top: 0px;
			}
			#mobMenu { display: none !important; }
			div.slider-wrapper { width:979px !important; }
			div.inlineComponentButtonsWrapper {top: 17px;}
			div.inlineComponentEdit > div.inlineComponentButtonsWrapper { top: 17px !important; }
			div.inlineComponentButtonsWrapper div.inlineComponentButtons { opacity: 1; }
			div.inlineComponentButtonsWrapper div.inlineComponentButtons:before { top: -10px; border-top: 0px; border-bottom: 10px solid #F7CA18; }
			div.inlineComponentButtonsWrapper div.inlineComponentButtons:after { top: -9px; border-top: 0px; border-bottom: 9px solid #fceba9; }
			div.inlineComponentEdit, div.inlineComponentEdit > div.inlineComponentButtonsWrapper { z-index: 99999; }
			div.inlineComponentEdit { cursor: help; }
			div.deviceInfo span.deviceInfoTitle {
				font-weight: bold;
			}
		</style>
		<% if (FileTools.isFile("/jscripts/common.js")) { %>
		<script type="text/javascript" src="/jscripts/common.js"></script>
		<% } %>
		<script type="text/javascript">
			function clickDisablerClick()
			{
				return false;
			}
			var parentNode = null;

			var CKEDITOR = window.parent.parent.CKEDITOR;
			//var editor = window.parent.parent.getCkEditorInstance();

			var isMenu = <%=isMenu%>;
			var isSocialIcons = <%=isSocialIcons%>;
			var isGoogleSearch = <%=isGoogleSearch%>;

			function bodyLoaded()
			{
				<% if (componentCode.indexOf("app-facebook")!=-1) { %>
					window.setTimeout(bodyLoadedImpl, 2000);
				<% } else { %>
					window.setTimeout(bodyLoadedImpl, 100);
					window.setTimeout(bodyLoadedImpl, 1500);
					window.setTimeout(bodyLoadedImpl, 4000);
				<% } %>
			}

			function bodyLoadedImpl()
			{
				<%
				PageParams myPageParams = new PageParams(componentCode);
				%>
				//window.alert(document.body.innerHTML);

				parentNode = window.frameElement;
				//window.alert(parentNode.innerHTML);
				var height = $("#content").height();
				//console.log("Preview height="+height);
				if (height < 200 && isMenu == false) height = 200;
				if (height < 200 && isMenu == true) height = 50;
				if(isSocialIcons || isGoogleSearch) height = 50;
				//parentNode.style.width = width+"px";
				var jParentNode = $(parentNode);
				var width = jParentNode.width();
				var paramsWidth = <%=myPageParams.getIntValue("width", 0)%>
				if (width < 100) width = <%=myPageParams.getIntValue("width", 400)%>;
				if (width < paramsWidth) width = paramsWidth;

				//parentNode.style.height = height+"px";
				jParentNode.height(height+5);
				//jParentNode.parent().height(height+5);
				//jParentNode.width("100%");

				//console.log("Preview setting w="+width+" h="+height);
				//window.alert(jParentNode.parent().html());

				$("#componentCodeDiv a").each(function()
				{
					//console.log("Setting clicks");
					//console.log($(this));

				    //$(this).on("mousedown", function() { console.log("CLICK"); console.log(this); });

				});
			}

			function inlineComponentEdit(event)
			{
				//console.log("inline component edit");
				//console.log(event);
				event.stopPropagation();

				/*
				var element = new CKEDITOR.dom.element( parentNode );
				console.log("1");
				var rangeObjForSelection = new CKEDITOR.dom.range( editor.document );
				console.log("2");
				rangeObjForSelection.selectNodeContents( element );

				console.log("3f");

				var iframe1 = window.parent.document.getElementById("iframe1");
				console.log(iframe1);

				//editor.getSelection().selectRanges( [ rangeObjForSelection ] );
				editor.getSelection().selectElement( new CKEDITOR.dom.element(iframe1) );
				console.log("4");
				*/

				var element = new CKEDITOR.dom.element( parentNode );
				//console.log("PARENT: "+element);
				//console.log(element);
				element.$.focus();
				//console.log("FOCUSED");
				element.setAttribute("id", "actuallyEditedComponent");

				//console.log("Opening component editor, window=", window.parent.parent);
				//musime ziskat aktualnu instanciu ckeditora
				if (window.parent.location.href.indexOf("inlineEditorAdmin=true")!=-1) {
					//ak sa jedna o inline v administracii tak ber editor z iframe, nie celkoveho parenta
					try {
						var scrollY = window.parent.scrollY;
						//console.log("scrollY: ", window.parent, "y=", scrollY);

						window.parent.getCkEditorInstance().execCommand("webjetcomponentsDialog");

						setTimeout(function() {
							//FF ma bug, ze z nejakeho dovodu v PB scroluje uplne dole po kliknuti na editaciu komponenty
							//console.log("Scrolling to: ", scrollY);
							window.parent.scrollTo(0, scrollY);
						}, 2000);
					} catch (e) {
						console.log(e);
						//niekedy zblbne urcenie window parent, pokracuj a skus realny ckeitor
					}
				}
				window.parent.parent.getCkEditorInstance().execCommand("webjetcomponentsDialog");
			}

			function inlineComponentDelete(event)
			{
				//console.log(event);
				event.stopPropagation();

				if (window.confirm("<iwcm:text key="components.media.confirm_delete"/>"))
				{
					var element = new CKEDITOR.dom.element( parentNode );
					element.remove(true);
				}
			}

			function bubbleIframeMouseMove(iframe){
			    // Save any previous onmousemove handler
			    var existingOnMouseMove = iframe.contentWindow.onmousemove;

			    // Attach a new onmousemove listener
			    iframe.contentWindow.onmousemove = function(e){
			        // Fire any existing onmousemove listener
			        if(existingOnMouseMove) existingOnMouseMove(e);

			        // Create a new event for the this window
			        var evt = document.createEvent("MouseEvents");

			        // We'll need this to offset the mouse move appropriately
			        var boundingClientRect = iframe.getBoundingClientRect();

			        // Initialize the event, copying exiting event values
			        // for the most part
			        evt.initMouseEvent(
			            "mousemove",
			            true, // bubbles
			            false, // not cancelable
			            window,
			            e.detail,
			            e.screenX,
			            e.screenY,
			            e.clientX + boundingClientRect.left,
			            e.clientY + boundingClientRect.top,
			            e.ctrlKey,
			            e.altKey,
			            e.shiftKey,
			            e.metaKey,
			            e.button,
			            null // no related element
			        );

			        // Dispatch the mousemove event on the iframe element
			        //console.log("Passing event to iframe: "+evt.clientX+"x"+evt.clientY);
			        iframe.dispatchEvent(evt);
			    };
			}

			// Get the iframe element we want to track mouse movements on
			var myIframe = window.frameElement; //document.getElementById("myIframe");

			// Run it through the function to setup bubbling
			bubbleIframeMouseMove(myIframe);
		</script>
		<style type="text/css">
			body div.inlineComponentEdit div.inlineComponentButtonsWrapper { display: block; }
		</style>

	</head>
	<body onload="bodyLoaded();" onclick="inlineComponentEdit(event);" class="editPeviewWindow">
		<div id="content" class="cb_content">

			<div class="inlineComponentEdit" onclick="inlineComponentEdit(event);">
				<div class='inlineComponentButtonsWrapper'>
					<div class='inlineComponentButtons'>
						<a onclick="javascript:inlineComponentEdit(event);" title ="<iwcm:text key="editor.inline.editComponent"/>" class="inlineComponentButton cke_button" ><span style="background-image:url('/admin/skins/webjet8/ckeditor/dist/plugins/webjetfloatingtools/image/edit.png?t=H21D');"></span></a>
						<a onclick="inlineComponentDelete(event)" title="<iwcm:text key="button.delete"/>" class="inlineComponentButton inlineComponentButtonDelete cke_button"><span style="background-image:url('/admin/skins/webjet8/ckeditor/dist/plugins/webjetfloatingtools/image/delete.png?t=H21D');"></span></a>
					</div>
				</div>
			</div>

			<%
			String device = myPageParams.getValue("device", null);
			if (Tools.isNotEmpty(device)) {
				Prop prop = Prop.getInstance(origLng);

				device = Tools.replace(device, "pc", prop.getText("apps.devices.pc"));
				device = Tools.replace(device, "tablet", prop.getText("apps.devices.tablet"));
				device = Tools.replace(device, "phone", prop.getText("apps.devices.phone"));
				%>
				<div class="deviceInfo">
					<span class="deviceInfoTitle"><%=prop.getText("apps.devices.title")%>:</span>
					<span class="deviceInfoTypes"><%=device%></span>
				</div>
				<%
			}
			String showForLoggedUser = myPageParams.getValue("showForLoggedUser", null);
			if (Tools.isNotEmpty(showForLoggedUser)) {
				Prop prop = Prop.getInstance(origLng);

				showForLoggedUser = Tools.replace(showForLoggedUser, "onlyLogged", prop.getText("apps.showForLoggedUser.onlyLogged"));
				showForLoggedUser = Tools.replace(showForLoggedUser, "onlyNotLogged", prop.getText("apps.showForLoggedUser.onlyNotLogged"));
				%>
				<div class="deviceInfo">
					<span class="deviceInfoTitle"><%=prop.getText("apps.showForLoggedUser.title")%>:</span>
					<span class="deviceInfoTypes"><%=showForLoggedUser%></span>
				</div>
				<%
			}
			%>

			<div id="componentCodeDiv">
				<iwcm:write name="componentCode"/>
			</div>

			<div style="clear: both;"></div>
		</div>
		<div id="menu" style="display: none !important; margin-top: 400px !important;"></div>
	</body>
	</html>
	<%
}
%>
