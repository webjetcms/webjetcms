<%@page import="sk.iway.iwcm.tags.CombineTag"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" import="java.io.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true"/>

<%
response.setHeader("Pragma","No-Cache");
response.setDateHeader("Expires",0);
response.setHeader("Cache-Control","no-Cache");

String cmpName = (String)request.getAttribute("cmpName");
String titleKey = (String)request.getAttribute("titleKey");
String descKey = (String)request.getAttribute("descKey");
String iconLink = "/components/"+cmpName+"/editoricon.png";
if (cmpName != null)
	iconLink = "/components/"+cmpName.replace(".", File.separator)+"/editoricon.png";
if (request.getAttribute("iconLink")!=null)
{
	iconLink = (String)request.getAttribute("iconLink");
}
if (titleKey == null) titleKey = "components."+cmpName+".title";
if (descKey == null) descKey = "components."+cmpName+".desc";

//otestuj ci existuje...
if ((new File(sk.iway.iwcm.Tools.getRealPath(iconLink))).isFile()==false)
{
   iconLink = "/components/"+cmpName+"/editoricon.gif";
}
if ((new File(sk.iway.iwcm.Tools.getRealPath(iconLink))).isFile()==false)
{
   iconLink = "/components/editoricon.png";
}
%>

<%@page import="sk.iway.iwcm.SetCharacterEncodingFilter"%><html>
<head>

   <title><iwcm:text key="<%=titleKey%>"/></title>
   <meta http-equiv="Content-Type" content="text/html; charset=<%=SetCharacterEncodingFilter.getEncoding() %>">
   <link rel="stylesheet" href="/components/cmp.css" />

	<iwcm:combine type="css" set="adminStandardCssWj9" />
	<link href="/admin/skins/webjet8/assets/global/css/webjet2021.css" rel="stylesheet" type="text/css"/>

	<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
   <style type="text/css">
   	 td, input, select, textarea, label { font-size: 13px !important; }
   	 input[type="text"] {
   	     background-color: white;
	    border: 1px solid #e5e5e5;
	    border-radius: 6px;
	    box-shadow: none;
	    color: #333333;
	    font-size: 14px;
	    font-weight: normal;
	    padding: 6px 12px;
   	 }
   	 div.toggle_content { padding: 0px; }
   	 body { overflow: hidden; }
   	 td.main { padding: 0px; }
   	 div.toggle_content { border-bottom: 0px !important; }
   	 ul.tab_menu { padding: 2px 0 0 10px !important; }
   	 body { overflow: hidden; }
	    * { -webkit-box-sizing: border-box; box-sizing: border-box; }
	    /* div.toggle_content div.tab-page { padding: 0px; } */
	    div.input-group { width: 100%; }
	    div.tab-page { min-height: 300px; }

	<jsp:include page="/admin/css/perms-css.jsp"/>
   </style>

   <iwcm:combine type="js" set="adminJqueryJs" />
   <iwcm:combine type="js" set="adminStandardJs" />
   <script src="/admin/scripts/common.jsp?v=<%=CombineTag.getVersion()%>&lng=<%=CombineTag.getLng(pageContext, request) %>" type="text/javascript"></script>
   <script type='text/javascript' src='/components/calendar/popcalendar.jsp'></script></html><%request.setAttribute("sk.iway.iwcm.tags.CalendarTag.isJsIncluded", "true");%>
   <script type="text/javascript" src="/components/form/check_form.js"></script>
   <script type="text/javascript">

	    var isFck = true;
		var oEditor = null;
		var recommendedWidth = 0;

		$(document).ready(function() {
			try {
				var minWidth = 0;
				var minHeight = 0;
				var myWindow = window.parent.parent;
				var innerHeight = myWindow.innerHeight;
				var innerWidth = myWindow.innerWidth;
				var hasFullHeightTab = $("#componentsWindowTableMainDiv div.tab-pane-fullheight").length > 0;
				var isInlineEditor = myWindow.location.href.indexOf("inlineEditorAdmin=true")!=-1;

				//console.log("innerHeight=", innerHeight, "innerWidth=", innerWidth, "hasFullHeightTab=", hasFullHeightTab);

				if (innerHeight >= 1300) minHeight = 1300;
				else if (innerHeight >= 1200) minHeight = 1200;
				else if (innerHeight >= 1050) minHeight = 1050;
				else if (innerHeight >= 950) minHeight = 950;
				else if (innerHeight >= 850) minHeight = 850;
				else if (innerHeight >= 750) minHeight = 750;
				else if (innerHeight >= 650) minHeight = 650;
				else if (innerHeight >= 600) minHeight = 600;

				if (innerWidth >= 1800) minWidth = 1800;
				else if (innerWidth >= 1700) minWidth = 1700;
				else if (innerWidth >= 1600) minWidth = 1600;
				else if (innerWidth >= 1500) minWidth = 1500;
				else if (innerWidth >= 1400) minWidth = 1400;
				else if (innerWidth >= 1300) minWidth = 1300;
				else if (innerWidth >= 1200) minWidth = 1200;

				//console.log("window.parent.parent.innerHeight=", innerHeight, "innerWidth=", innerWidth);

				if (minHeight > 0) {
					//div.min-height-780 div.tab-pane-fullheight .tab-page { height: 480px; }
					//div.min-height-780 div.tab-pane-fullheight .tab-page-iframe, div.tab-pane-fullheight .tab-page-iframe iframe { height: 480px; }

					//780->480
					var height = minHeight;

					if (isInlineEditor) height = height - 120; //inline editor, it's iframed, so make window bigger 80=28top+42botttom+free space
					else height = Math.floor((height-150)*0.9);

					//380 is default value, check for fullheight tab
					if (height > 380 && hasFullHeightTab) {
						//console.log("minHeight=", minHeight, "height=", height);

						var heightTabs = height;
						if (isInlineEditor) {
							//console.log(window.parent.parent.$(".cke_dialog_title").outerHeight());
							heightTabs = heightTabs - window.parent.parent.$(".cke_dialog_title").outerHeight() -2; //28 is height of toolbar header
						}

						$("#componentsWindowTableMainDiv").css("height", height+"px");
						$("#componentsWindowTableMainDiv div.tab-pane-fullheight .tab-page").css("height", heightTabs+"px");
						$("#componentsWindowTableMainDiv div.tab-pane-fullheight .tab-page").css("max-height", heightTabs+"px");

						$("#componentsWindowTableMainDiv div.tab-pane-fullheight .tab-page-iframe, div.tab-pane-fullheight .tab-page-iframe iframe").css("height", heightTabs+"px");
						$("#componentsWindowTableMainDiv div.tab-pane-fullheight .tab-page-iframe, div.tab-pane-fullheight .tab-page-iframe iframe").css("max-height", heightTabs+"px");

						$("#componentsWindowTableMainDiv div.tab-pane-fullheight .tab-page-iframe, div.tab-pane-fullheight .tab-page-iframe iframe").each(function() {
							var $this = $(this);
							var marginTop = 0;
							if (typeof $this.data("margin-top") != "undefined") marginTop = parseInt($this.data("margin-top"));
							//console.log("marginTop", marginTop, "this=", this);
							$this.css("height", (heightTabs - marginTop) + "px");
							$this.css("max-height", (heightTabs - marginTop) + "px");
						});


						recommendedWidth = minWidth - 150;
					}
				}

				if (hasFullHeightTab==false) {
					$("#componentsWindowTableMainDiv").css("height", "100vh");
					$("#componentsWindowTableMainDiv").css("overflow", "auto");
					$("#componentsWindowTableMainDiv").css("padding-bottom", "30px"); //video player
				}
			} catch (e) {console.log(e);}
		});

		$(document).ready(function() {
			try
			{
			   oEditor = window.parent.InnerDialogLoaded();
			}
			catch (ex) {}
			//window.alert("oEditor="+oEditor);

			if (typeof window.parent.parent.CKEDITOR != "undefined") {
				var currentDialog = window.parent.parent.CKEDITOR.dialog.getCurrent();
				//window.alert(currentDialog);

				//zrus padding
				var dialogElement = currentDialog.getElement().getFirst().$;
				//window.alert(dialogElement.outerHTML);
				dialogElement.getElementsByClassName("cke_dialog_contents_body")[0].style.padding="0px 0px 0px 0px";


				//nastav velkost okna
				var componentsWindowTable = $("#componentsWindowTableMainDiv");
				//window.alert("Async, w="+componentsWindowTable.width()+"x"+componentsWindowTable.height());

				var width = componentsWindowTable.width(); // + 20; //20 je magicka konstanta aby okno nemenilo pri opakovanych volaniach sirku
				var height = componentsWindowTable.height() + $("#wjDialogHeaderTableRow").height();

				//try to make window wider
				if (recommendedWidth>1000) width = recommendedWidth;

				var windowHeight = window.innerHeight;
				try {
					var ckeditorDialogHeight = $(window.parent.parent.parent.document).find("div.modal.DTED.show div.modal-body").height();
					if (typeof ckeditorDialogHeight != "undefined") {
						//sme v editore vo web stranke
						//odrataj velkost horneho toolbaru a paticky s tlacidlami okna CK dialogu (lebo resize to berie do uvahy)
						windowHeight = ckeditorDialogHeight-30-55;

						if ($(window.parent.parent.document.body).hasClass("page-builder-in-admin")==false) {
							//ak to nie je PB tak windowHeight je celkove okno, nielen vnutro
							windowHeight = $(window.parent.parent).height();
						}

						//musime spravit aj tu, aby sa nam dobre pocitala velkost max-height
						if (height > windowHeight) {
							height = windowHeight-5;
						}

						//console.log("dialog=", currentDialog);
						//nastav max-height
						var componentDocument = $(currentDialog.parts.contents.$).find("iframe.cke_dialog_ui_iframe").contents().find("#editorComponent").contents();
						if (componentDocument.length>0) {
							var headerHeight = componentDocument.find("#wjDialogHeaderTableRow").outerHeight();
							var tabsHeight = componentDocument.find("div.box_tab").outerHeight();
							//v2023
							tabsHeight = 0;
							if (typeof tabsHeight == "undefined") tabsHeight = 0;

							var maxHeight = height-headerHeight-tabsHeight;

							//console.log("componentDocument=", componentDocument, "headerHeight=", headerHeight, "windowHeight=", windowHeight, "height=", height, "tabsHeight=", tabsHeight, "maxHeight=", maxHeight);
							componentDocument.find("div.tab-pane").css("max-height", maxHeight+"px");
						}

						var top = Math.floor((windowHeight - height - 30 - 51) / 2);
						if (top < 0) top = 0;
						currentDialog.parts.dialog.$.style.top = top+"px";

						var windowWidth = $(window.parent.parent).width();
						var left = Math.floor((windowWidth - width) / 2);
						if (left < 0) left = 0;
						//console.log("windowHeight=", windowHeight, "height=", height, "top=", top, "windowWidth=", windowWidth, "width=", width, "left=", left);
						currentDialog.parts.dialog.$.style.left = left+"px";

						currentDialog.parts.dialog.$.style.position = "fixed";
					}
				} catch (e) {}

				if (height > windowHeight) {
					height = windowHeight-5;
				}

				if (height < 250) height = 250;

				//window.alert("New size: "+width+"x"+height+" old size:"+currentDialog.getSize().width+"x"+currentDialog.getSize().height+" windowHeight="+windowHeight+" ckeditorDialogHeight="+ckeditorDialogHeight);
				//console.log("Resizing dialog to: "+width+"x"+height, "windowHeight=", windowHeight, "window.innerHeight=", window.innerHeight, "window.parent.parent.innerHeight=", window.parent.parent.innerHeight, "recommendedWidth=", recommendedWidth);
				currentDialog.resize(width, height);
			}
		});

		function onLoadHandler()
		{
			//caled as document.ready before DT init
			//setTimeout(onLoadHandlerAsync, 100);
		}

   </script>
</head>

<body onload="onLoadHandler()" bgcolor="#FFFFFF" leftmargin="2" topmargin="2" marginwidth="2" marginheight="2">
<table border="0" cellspacing="0" cellpadding="0" width="100%" height="100%" id="componentsWindowTable">
<tr id="wjDialogHeaderTableRow">
   <td class="header">
		<h1><iwcm:text key="<%=titleKey%>"/></h1>
		<span class="component-desc"><iwcm:text key="<%=descKey%>"/></span>
   </td>
   <td class="header headerImage" nowrap="nowrap" valign="middle" align="right">
		<img src="<%=iconLink%>" alt="" />
   </td>
</tr>
<tr>
   <td class="main" valign="top" colspan="2"><div id="componentsWindowTableMainDiv">

