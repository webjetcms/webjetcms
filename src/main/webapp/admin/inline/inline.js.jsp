/*
<%@page import="org.json.JSONArray"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.Tools"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/a-inline.js", null, request, response);
%><%@ page pageEncoding="utf-8" contentType="text/javascript" import="sk.iway.iwcm.common.WriteTagToolsForCore" %><%@ page import="sk.iway.iwcm.stat.BrowserDetector"%><%@ page import="sk.iway.iwcm.editor.InlineEditor"%><%@ page import="com.sun.org.apache.bcel.internal.classfile.Constant"%><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
*/

<%
InlineEditor.EditingMode editingMode = InlineEditor.getEditingMode(request);
String pbPrefix = Constants.getString("pageBuilderPrefix", "pb");
%>

function isInlineOn()
{
	if ($("body").hasClass("is-toolbar-hidden")) return false;
	if ($("body").hasClass("is-view-mode")) return false;
	return true;
}

var logonErrorDetected = 0;
var logonDetectorInterval = window.setInterval(function()
{
    $.ajax({
    	type: 'POST',
    	url: "/admin/refresher.jsp?ajax=true",
    	cache: false,
    	timeout: 10000,
    	success: function(html)
		{
    		logonErrorDetected = 0;
		},
		error:function(xmlHttpRequest, textStatus, errorThrown)
		{
			<% if ("cloud".equals(Constants.getInstallName())==false) { %>
			try
			{
				if (logonErrorDetected > 3)
				{
					alert("<iwcm:text key="user.notLoggedError"/>");
					$("#inlineEditorToolbarTop").hide();
					window.clearInterval(logonDetectorInterval);
				}
				logonErrorDetected++;
			}
			catch (ex) {}
			<% } %>
		}
    });
}, 1000 * 30);

//otvorenie dialogoveho okna IE/Mozilla - based on FCKDialog
var WJInlineDialog = new Object() ;
var WJInlineDialogArguments = null;

// This method opens a dialog window using the standard dialog template.
WJInlineDialog.OpenDialog = function( dialogName, dialogTitle, dialogPage, width, height, customValue, docId, parentWindow )
{
	// Setup the dialog info.
	var oDialogInfo = new Object() ;
	oDialogInfo.Title = dialogTitle ;
	oDialogInfo.Page = dialogPage ;
	oDialogInfo.Editor = window ;
	oDialogInfo.CustomValue = customValue ;		// Optional
	oDialogInfo.DocId = docId ;		// Optional

	//window.alert("WJInlineDialog");

	window.FCKLang = new Object();
	window.FCKConfig = new Object();
	//link dialog setup
	window.FCKConfig.LinkDlgHideAdvanced = true;

	var sUrl = '<iwcm:cp/>/admin/inline/dialogframe_inline.jsp' ;
	this.Show( oDialogInfo, dialogName, sUrl, width, height, parentWindow ) ;
}

WJInlineDialog.Show = function( dialogInfo, dialogName, pageUrl, dialogWidth, dialogHeight, parentWindow )
{
	var iTop  = (screen.height - dialogHeight) / 2 ;
	var iLeft = (screen.width  - dialogWidth)  / 2 ;

	if (iLeft < 0) iLeft = 0;
	if (iTop < 0) iTop = 0;

	var sOption  = "location=no,menubar=no,resizable=no,toolbar=no,dependent=yes,dialog=yes,minimizable=no,modal=yes,alwaysRaised=yes" +
		",width="  + dialogWidth +
		",height=" + dialogHeight +
		",top="  + iTop +
		",left=" + iLeft ;

	if ( !parentWindow )
		parentWindow = window ;

	var oWindow = parentWindow.open( pageUrl, 'FCKEditorDialog_' + dialogName, sOption, true ) ;
	//window.alert("iLeft="+iLeft+" iTop="+iTop);

	if (navigator.userAgent.indexOf("Chrome")==-1)
	{
		//ked tu bolo toto, Chrome zle pozicioval okno
		oWindow.moveTo( iLeft, iTop ) ;
		if (dialogWidth>0 && dialogHeight>0) oWindow.resizeTo( dialogWidth, dialogHeight ) ;
	}

	oWindow.focus() ;
	oWindow.dialogArguments = dialogInfo;

	//oWindow.location.href = pageUrl ;
	WJInlineDialogArguments = dialogInfo;

	// On some Gecko browsers (probably over slow connections) the
	// "dialogArguments" are not set to the target window so we must
	// put it in the opener window so it can be used by the target one.
	parentWindow.FCKLastDialogInfo = dialogInfo ;

	this.Window = oWindow ;

	// Try/Catch must be used to avoit an error when using a frameset
	// on a different domain:
	// "Permission denied to get property Window.releaseEvents".
	try
	{
		/*parentWindow.captureEvents( Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS ) ;
		parentWindow.top.addEventListener( 'mousedown', this.CheckFocus, true ) ;
		parentWindow.top.addEventListener( 'mouseup', this.CheckFocus, true ) ;
		parentWindow.top.addEventListener( 'click', this.CheckFocus, true ) ;
		parentWindow.top.addEventListener( 'focus', this.CheckFocus, true ) ;*/

		window.captureEvents( Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS ) ;
		window.addEventListener( 'mousedown', this.CheckFocus, true ) ;
		window.addEventListener( 'mouseup', this.CheckFocus, true ) ;
		window.addEventListener( 'click', this.CheckFocus, true ) ;
		window.addEventListener( 'focus', this.CheckFocus, true ) ;
	}
	catch (e)
	{}
}

WJInlineDialog.CheckFocus = function()
{
	//WJInlineDialog.Window.status = "check focus: " + new Date();

	// It is strange, but we have to check the WJInlineDialog existence to avoid a
	// random error: "WJInlineDialog is not defined".
	if ( typeof( WJInlineDialog ) != "object" )
		return ;

	if ( WJInlineDialog.Window && !WJInlineDialog.Window.closed )
	{
	   try
	   {
			//WJInlineDialog.Window.focus();
			WJInlineDialog.Window.document.getElementById('frmMain').contentWindow.focus();
		}
		catch (e)
		{}
		//WJInlineDialog.Window.status = WJInlineDialog.Window.location.href + " " + new Date();
		return false ;
	}
	else
	{
		//WJInlineDialog.Window.status = "XXX: " + new Date();

		// Try/Catch must be used to avoit an error when using a frameset
		// on a different domain:
		// "Permission denied to get property Window.releaseEvents".
		try
		{
			window.top.releaseEvents(Event.CLICK | Event.MOUSEDOWN | Event.MOUSEUP | Event.FOCUS) ;
			window.top.parent.removeEventListener( 'onmousedown', FCKDialog.CheckFocus, true ) ;
			window.top.parent.removeEventListener( 'mouseup', FCKDialog.CheckFocus, true ) ;
			window.top.parent.removeEventListener( 'click', FCKDialog.CheckFocus, true ) ;
			window.top.parent.removeEventListener( 'onfocus', FCKDialog.CheckFocus, true ) ;
		}
		catch (e)
		{}
	}
}

function inlineReloadWindow()
{
	try
	{
		var href = window.location.href;
		if (href.indexOf("?")==-1) href += "?";
		else href += "&";

		var start = href.indexOf("dirnd");
		if (start!=-1)
		{
		   var end = href.indexOf("&", start);
		   if (end > start) href = href.substring(0, start) + href.substring(end+1);
		   else href = href.substring(0, start);
		}

		href += "dirnd="+(new Date()).valueOf();

		window.location.href = href;
	} catch (e) {}
}

function inlineComponentEdit(includeFileName, pageParams, includeText, editorComponent, docId)
{
	var url = editorComponent+"?pageParams="+encodeURIComponent(pageParams)+"&jspFileName="+encodeURIComponent(includeFileName)+"&includeText="+encodeURIComponent(includeText);
	WJInlineDialog.OpenDialog( 'Component_edit' , "Edit component properties", url, 900, 670, includeText, docId) ;
}

function inlineComponentDelete(includeFileName, pageParams, includeText, editorComponent, docId)
{
	if (window.confirm("<iwcm:text key="components.media.confirm_delete"/>"))
	{
		$.ajax({
			  type: 'POST',
			  url: "/admin/inline/component_save_ajax.jsp?&rnd=" + (new Date()).getTime(),
			  data: { docid: docId, oldText: includeText, newText: "DELETE" },
			  dataType: 'html',
			  cache: false,
			  timeout: 300000,
			  success: function(html)
			  {
				  if (html.indexOf("SAVEOK")!=-1)
				  {
					  inlineReloadWindow();
				  }
				  else
				  {
				  	  window.alert(html);
				  }
				  //$("#statusIframe").html(html);
				  //Cancel();
			  },
				error:function(xmlHttpRequest, textStatus, errorThrown)
				{
					try
					{
						if (xmlHttpRequest.status == 200)
						{
							alert("<iwcm:text key="editor.ajax.save.error.login"/>");
						}
						else
						{
							alert("<iwcm:text key='editor.ajax.save.error'/>");
						}
					}
					catch (ex) {}
				}
			});
	}
}
<%
String baseLink = "/components/_common/admin/inline/inline.css";
        String styleLink = WriteTagToolsForCore.getCustomPage(baseLink, request);
if (Tools.isEmpty(styleLink)) styleLink = baseLink;
%>
if (document.createStyleSheet)
{
	document.createStyleSheet("<%=styleLink%>");
}
else
{
	$('head').append('<link rel="stylesheet" type="text/css" href="<%=styleLink%>">');
}
$('head').append('<link rel="stylesheet" type="text/css" href="/admin/skins/webjet8/assets/global/css/webjet2021.css">');

var wjInlineOverlay = new wjInlineOverlayClass();

/**
 * This object encapsulates the elements and actions of the overlay.
 */
function wjInlineOverlayClass() {

    this.width = this.height = this.left = this.top = 0;

    // outer parent
    var outer = $("<div class='wjInlineHighlighter'><span class='wjInlineHighlighterHint'><iwcm:text key='editor.dubleclickToEdit'/></span></div>").appendTo("body");

    this.show = function show() {
       outer.css("opacity", "1");
    };

    this.hide = function hide() {
       outer.css("opacity", "0");
    };

    this.render = function render(el)
    {


		  var offset = el.offset();

		  var paddingLeft = parseInt(el.css("padding-left"));
		  var paddingRight = parseInt(el.css("padding-right"));
		  var paddingTop = parseInt(el.css("padding-top"));
		  var paddingBottom = parseInt(el.css("padding-bottom"));

        this.width = el.innerWidth() - paddingLeft - paddingRight;
        this.height = el.outerHeight() - paddingTop - paddingBottom;
        this.left = offset.left + paddingLeft;
        this.top = offset.top + paddingTop;

		// uprava textov a pozicie labela
		if(el.id == "doc_footerEditor"){
			outer.addClass("footerEditor");
			$('.wjInlineHighlighterHint').html('<iwcm:text key='editor.dubleclickToEdit.footer'/>');
		}
		else if(el.id == "doc_headerEditor"){
			outer.removeClass("footerEditor");
			$('.wjInlineHighlighterHint').html('<iwcm:text key='editor.dubleclickToEdit.header'/>');
		}
		else{
			outer.removeClass("footerEditor");
			$('.wjInlineHighlighterHint').html('<iwcm:text key='editor.dubleclickToEdit'/>');
		}
		if($('#'+el.id).hasClass('cke_editable')){
			$('.wjInlineHighlighterHint').html('<i class="fa fa-exclamation-triangle " aria-hidden="true"></i> <iwcm:text key='editor.dubleclickToEdit.change'/>');
		}
		// end - uprava textov a pozicie labela


        outer.css({
          top:   this.top,
          left:  this.left,
          width: this.width,
          height: this.height
        });

        this.show();
    };
}

CKEDITOR.disableAutoInline = true;
CKEDITOR.dtd.$editable.span = 1
CKEDITOR.dtd.$editable.a = 1
var ckEditorAutofocusInstanceName = null;

CKEDITOR.on('instanceReady', function(ev) {
	//console.log("Ckeditor "+ev.editor.name+" instance ready");
	//console.log(ev);
   ckEditorInstance = ev.editor;

 	//ev.editor.on("blur", function(event) { console.log("blured"); $("#wjInlineCkEditorToolbarElement").hide(); });
   ev.editor.on("focus", function(event)
   {
   	//console.log("editor focused, name="+event.editor.name);
   	ckEditorInstance = event.editor;
   });

   if (ckEditorAutofocusInstanceName != null && ckEditorAutofocusInstanceName == ev.editor.name)
   {
   	ev.editor.focus();
   }


});

function setInlineEditingButtons()
{
	$("#inlineToolbarEditPage").hide();
	$("#inlineToolbarSaveInlineEditing").show();
	$("#inlineToolbarQuitInlineEditing").show();
}

function getSaveData()
{
	var saveData = {
	    editable : []
	};

    $("[data-wjapp='newsInline'], [data-wjapp='gridEditor']").each(function(index)
    {
        $(".cke_editable").each(function (index)
        {
            var docId = $(this).data("wjappkey");
            var wjApp = $(this).data("wjapp");
            var wjAppField = $(this).data("wjappfield");
            if (wjAppField === undefined) wjAppField = "";

            var editor = CKEDITOR.instances[$(this).attr("id")];
            //console.log($(this).attr("id") + " dirty=" + editor.checkDirty())
            if (editor != undefined && (editor.checkDirty() || "wjInline-docdata" == $(this).attr("id"))) {
                var htmlCode = editor.getData();
                var item = {wjApp: wjApp, wjAppKey: docId, wjAppField: wjAppField, data: htmlCode};
                saveData.editable.push(item);
            }
        });
    });

    $("[data-wjapp='pageBuilder']").each(function(index)
    {
        var docId = $(this).data("wjappkey");
        var wjApp = $(this).data("wjapp");
        var wjAppField = $(this).data("wjappfield");
        if (wjAppField === undefined) wjAppField = "";

        //var node = $(this).clone();
		 //console.log($(this));

		 if ($(this).data('plugin_ninjaPageBuilder') === undefined)
		 {
		 	console.log("PageBuilder is not defined, skipping");
		 	return;
		 }

		 var pageBuilderInstance = $(this).data('plugin_ninjaPageBuilder');
        var node = pageBuilderInstance.getClearNode();
        //console.log(node);

        //console.log("Node html:");
        //console.log(node.html());

        var editableElements = node.find("*[class*='editableElement']");
        editableElements.each(function()
        {
           var editorName =  $(this).attr("data-ckeditor-instance");
           //console.log("editorName="+editorName);
			  //console.log($(this));
           //console.log($(this).html());

           var editorData = CKEDITOR.instances[editorName].getData();
           $(this).html(editorData);
        });

		  //console.log("Before unwrap HTML:", node.html());
		  pageBuilderInstance.clearEditorAttributes(node);

        var htmlCode = node.html();

		  htmlCode = htmlCode.replace(/data-<%=pbPrefix%>-toggle="/gi, 'data-toggle="');

        //console.log("Final HTML: ", htmlCode);

        var item = {wjApp: wjApp, wjAppKey: docId, wjAppField: wjAppField, data: htmlCode};
        saveData.editable.push(item);
    });

	return saveData;
}

function saveInlineEditing()
{
	var saveData = getSaveData();

	//console.log(saveData);

	$("#editorFormJsonField").val(JSON.stringify(saveData));

	var actualForm = $("#editorFormId").serialize();
	//console.log("actualForm:");
	//console.log(actualForm);
	console.log("PageBuilder: saveData=", saveData);

	$.ajax({
        type: "POST",
        url: "/admin/inline/page_save.jsp",
        success: function (msg)
        {
        		//TODO: potrebujeme tento reload?
        		if (""==msg) window.location.reload();
            else window.alert(msg);
        },

        data: actualForm
    });
}

var sessionCssParsed = null;
<%
if (session.getAttribute("sessionCssParsed") != null)
{
	JSONArray cssJson = (JSONArray) session.getAttribute("sessionCssParsed");
	out.println("sessionCssParsed = " + cssJson.toString() + ";" );
}
%>

function initializePageData(editingMode, html, pageDiv) {
	//console.log("GetPage success, mode=", editingMode, "isNew=", window.location.href.indexOf("inlineEditingNewPage=true"));

	if (editingMode == "pageBuilder")
	{
			//pageDiv.attr('contenteditable','true');

			//parameter inlineEditingNewPage=true je tam, ked potrebujeme zobrazit cistu stranku (pri novej stranke je zobrazena default stranka adresara)
			if (window.location.href.indexOf("inlineEditingNewPage=true")!=-1) html = "<p><iwcm:text key='editor.newDocumentName'/></p>";

			if (html.indexOf("<section")==-1)
			{
				//console.log("HTML kod neobsahuje ziadnu section, pridavam, html=", html);
				if ("<p>&nbsp;</p>"==html) html = "<p>Text</p>";
				html = "<section><div class=\"container\"><div class=\"row\"><div class=\"col-md-12\">"+html+"</div></div></div></section>";
			}

			html = html.replace(/data-toggle="/gi, 'data-<%=pbPrefix%>-toggle="');

		//console.log("Settng HTML into div=", pageDiv, " html=", html);

			pageDiv.html(html);

			pageDiv.data('ckEditorInitialized','true');
			pageDiv[0].form = document.editorForm;

			//inicializuj pageBuilder
			pageDiv.ninjaPageBuilder({
				max_col_size: <%=Constants.getInt("bootstrapColumns", 12)%>,
				prefix: "<%=pbPrefix%>",
				//toto sa berie z inline_script.jsp kde sa hodnota inicializuje
				template_group_id: templateGroupId,
				<%
				String pageBuilderGrid = Constants.getString("pageBuilderGrid");
				if (Tools.isNotEmpty(pageBuilderGrid))
				{
					%>grid: { <%=pageBuilderGrid%> },<%
				}
				%>

				onGridChanged: function() {
					//console.log('Element bol zmeneny');
					var changedElement = pageDiv.data('plugin_ninjaPageBuilder').getChangedElement();
					//console.log(changedElement);
					//console.log($(changedElement).html());
					//console.log($(changedElement).find("div.npb-column__content"));
					//musime zrusit oznacenie editableElement lebo po naklonovani sa skopiruje aj toto a potom sa nam neinicializuje ckeditor
					$(changedElement).find("div.<%=pbPrefix%>-column__content").removeClass("editableElement");
					initPageBuilderEditors(pageDiv);
					WJ.fireEvent("WJ.PageBuilder.gridChanged");
				},
			});

			initPageBuilderEditors(pageDiv);

			setInlineEditingButtons();

			WJ.fireEvent("WJ.PageBuilder.loaded");
	}
	else
	{
		pageDiv.attr('contenteditable', 'true');
		pageDiv.html(html);

		pageDiv[0].form = document.editorForm;

		//CKEDITOR.inline(pageDiv.attr("id"), { allowedContent: true, language: "sk", startupFocus: true, floatSpacePinnedOffsetY: 50, customConfig: "/admin/skins/webjet8/ckeditor/config.jsp?inline=true" });
		initializeCkEditorImpl(pageDiv.attr("id"), CKEDITOR.inline, "/admin/skins/webjet8/ckeditor/config.jsp?inline=true&inlineMode="+editingMode);
		setInlineEditingButtons();
	}
}

function setNewsAppEditing()
{
	$("[data-wjapp='newsPopup']").each(function(index)
	{
		$(this).on("dblclick", function() { popupPageEditDocId($(this).data("wjappkey")); return false });
	});

	$("[data-wjapp='newsInline'], [data-wjapp='pageBuilder'], [data-wjapp='gridEditor']").each(function(index)
	{
		var id = $(this).attr("id");

		if (isFullpageNews == true && id.indexOf("wjInline-docdata")!=-1) return true;


		if (id != "wjInline-docdata")
		{
			$(this).addClass("wjInlineEditable");

			$(this).mouseover(function(e){
			  var el = $(e.target);
			  wjInlineOverlay.render($(this));
			});

			$(this).mouseout(function(e){
			  wjInlineOverlay.hide();
			});
		}

		//console.log(index+" "+$(this)+" id="+id);
		$(this).on("dblclick", function(event)
		{
			//console.log("click event, isInlineOn()="+isInlineOn());
			//console.log(this);
			//console.log(event);

			// ak je inicializovany grid editor, tak skonci
			if($('body').find('.ge-editing').length >0){
			    return;
			}
			if (isInlineOn()==false) return;

			var pageDiv = $(this);
			var editingMode = $(this).data("wjapp");
			var docId = $(this).data("wjappkey");
			var wjAppField = $(this).data("wjappfield");
			var id = $(this).attr("id");
			if (wjAppField === undefined) wjAppField = "";

			//console.log("setNewsAppEditing, initializing editor for id="+id);

			if ("doc_headerEditor"==id || "doc_right_menuEditor"==id || "doc_menuEditor"==id)
			{

				var eventTarget = event.target.tagName;
				//console.log("eventTarget="+eventTarget);

				//kliknutie na tlacitko Upravit aplikaciu
				if (eventTarget == "SPAN") eventTarget = event.target.parentNode.tagName

				if (eventTarget == "A")
				{
					//console.log("not editing");
					//asi klikol v hlavicke v menu na A odkaz
					return;
				}
			}


          if(pageDiv.attr('contenteditable') === 'true' || pageDiv.data("ckEditorInitialized") === 'true')
		    {
		    	//console.log("ckeditor allready initialized");
		    	$("div[_moz_abspos]").each(function()
		    	{
		    		//firefox pre absolutne poziciovane elementy nastavuje tento atribut ktory da element biely, tymto sa toho zbavime
				    $(this).removeAttr("_moz_abspos");
				});
		        //pageDiv.attr('contenteditable','false');
		        //editor.destroy();
		    }
		    else
		    {
		    	  //console.log("ckeditor editing docId=" + docId + " wjAppField="+wjAppField);
		    	  //console.log(pageDiv);

				  if (typeof window.inlineEditorDocData != "undefined" && window.inlineEditorDocData != null) {
					//data mame injectnute v stranke, pouzijeme ich
					initializePageData(editingMode, window.inlineEditorDocData.doc_data, pageDiv);
					window.inlineEditorDocData = null;
				  }
				  else {
					$.ajax({
				    	type: 'POST',
				    	url: "/admin/inline/get_page.jsp",
				    	data: { docId: docId, wjAppField: wjAppField},
				    	cache: false,
				    	timeout: 10000,
				    	success: function(html)
						{
							initializePageData(editingMode, html, pageDiv);
						}
				  	});
				  }
		    }


			return false;
		});
	});

	if (isFullpageNews==false)
	{
		$("#wjInline-docdata").dblclick();
	}
}

function initPageBuilderEditors(pageDiv)
{
    <%--var editableElements = pageDiv.find("* [class*='npb-column__content']");--%>
	//console.log("initPageBuilderEditors, pageDiv=", pageDiv);
    var editableElements = pageDiv.find("*[class*='<%=pbPrefix%>-editable'], *[class*='<%=pbPrefix%>-content']");
    editableElements.each(function()
    {
        //console.log("Has class editableElement: "+$(this).hasClass("editableElement"), this);

        //na tomto elemente je to uz inicializovane, preskocme
        if ($(this).hasClass("editableElement")) return;

        //console.log(this);
        this.form = document.editorForm;
        $(this).attr('contenteditable','true');
        $(this).addClass('editableElement');

        <%--console.log("Initializing editor on element:");--%>
        <%--console.log(this);--%>

        //inicializuj ckEditor
        var ckEditorInstanceInitialized = initializeCkEditorImpl(this, CKEDITOR.inline, "/admin/skins/webjet8/ckeditor/config.jsp?inline=true&inlineMode=pageBuilder");
		  $(this).attr("data-ckeditor-instance", ckEditorInstanceInitialized.name);

			//nastav CSS styly
			ckEditorInstanceInitialized.on("instanceReady", function() {
				//console.log("instanceReady");
				setStylesDef(window.editorStyles, ckEditorInstanceInitialized);
				WJ.fireEvent("WJ.PageBuilder.instanceReady", {ckinstance: ckEditorInstanceInitialized});
			});
    });
}

function newsInlineAfterSave(form)
{
	$("[data-wjapp='newsInline'], [data-wjapp='gridEditor']").each(function(index)
	{
		var pageDiv = $(this);
		var docId = $(this).data("wjappkey");

		if (docId == form.docId.value)
		{
			$.ajax({
		    	type: 'POST',
		    	url: "/admin/inline/render_page.jsp",
		    	data: {html: form.data.value, docId: docId},
		    	cache: false,
		    	timeout: 10000,
		    	success: function(html)
				{
					pageDiv.html(html);
				}
			});
		}
	});
}

function FCKeditor_OnComplete()
{
	//console.log("On complete");
}

$(document).ready(function() {
	setTimeout(function() { setNewsAppEditing(); }, 1000);

	document.addEventListener("keydown", function(e) {
		//zachytenie CTRL+S/CMD+S
		if ((window.navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)  && e.key === 's') {
			e.preventDefault();
			//console.log("Dispatching WJ.DTE.save");
			try {
				window.top.WJ.dispatchEvent("WJ.DTE.save", {});
			} catch (ex) {}
		}
	}, false);
});

function fixFirefox()
{
	console.log("Disabling inline resize in firefox");
	document.designMode = 'on';
	document.execCommand('enableObjectResizing', false, false);
	document.execCommand('enableInlineTableEditing', false, false);
	document.designMode = 'off';
}

//osetrenie zobrazenia resize handlerov vo firefoxe pre abspos div elementy
var isFirefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
if (isFirefox)
{
	CKEDITOR.on('instanceReady', function(event) {
		event.editor.on('mode', function(ev) {
			if (ev.editor.mode === 'wysiwyg') {
				// gets executed everytime the editor switches from source -> WYSIWYG
				fixFirefox();
			}
		});

		// this gets executed on init
		fixFirefox();
	});
}

function wjSetReadOnly(isReadOnly)
{
	var toolbar = $(".wj-toolbar-panel");
	if (isReadOnly)
	{
		toolbar.addClass("is-view-mode");
		$("body").removeClass("is-edit-mode");
		$("body").addClass("is-view-mode");

		if (typeof ckEditorInstance != "undefined" && ckEditorInstance != null) editorsSetReadOnly(true) //ckEditorInstance.setReadOnly(true);
		$("*").removeClass("cke_floatingtools_selected");

		$.cookie("wjPreview", "true", { expires: 10, path: '/' });

		$('iframe.wj_component').each(function()
		{
			$(this).contents().find("body").removeClass("is-edit-mode");
		   $(this).contents().find("body").addClass("is-view-mode");
		});


		<%--  mute ninja page builder when view-mode is on. V podstate sa len vypne stylovanie. --%>
		$('.pb-wrapper').addClass('pb-wrapper-muted').removeClass('pb-wrapper');
	}
	else
	{
		toolbar.removeClass("is-view-mode");
		$("body").removeClass("is-view-mode");
		$("body").addClass("is-edit-mode");

		if (ckEditorInstance != null) editorsSetReadOnly(false); //ckEditorInstance.setReadOnly(false);
		else
		{
			inlinePageEditInit(false);
		}

		$.cookie("wjPreview", "false", { expires: 10, path: '/' });

		$('iframe.wj_component').each(function()
		{
		   $(this).contents().find("body").removeClass("is-view-mode");
		   $(this).contents().find("body").addClass("is-edit-mode");
		});

		$('.pb-wrapper-muted').addClass('pb-wrapper').removeClass('pb-wrapper-muted');
	}
}

function editorsSetReadOnly(isReadOnly)
{
	try
	{
		for(var i in CKEDITOR.instances)
		{
			//console.log(i);
			CKEDITOR.instances[i].setReadOnly(isReadOnly);
		}
	}
	catch (e) { console.log(e); }
}

function webjetToolbarPopup(url, width, height)
{
   var options = "toolbar=no,scrollbars=yes,resizable=yes,width="+width+",height="+height+";";
   popupWindow=window.open(url,"_blank",options);
}

function pbSetWindowSize(size) {
	var iframeElement = window.parent.$("iframe.md-pageBuilder");
	//console.log("iframeElement=", iframeElement, "size=", size);
	var maxWidth = "";
	if ('tablet'==size) {
		maxWidth = "991px";
	} else if ('phone'==size) {
		maxWidth = "576px";
	}
	//console.log("Setting width: ", maxWidth);
	iframeElement.css("max-width", maxWidth);
}