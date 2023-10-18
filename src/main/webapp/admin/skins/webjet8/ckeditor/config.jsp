<%@page import="sk.iway.iwcm.Tools"%><%@page import="sk.iway.iwcm.FileTools"%><%@page import="sk.iway.iwcm.Constants"%><%@ page import="sk.iway.iwcm.components.dictionary.DictionaryDB"%><%@ page import="sk.iway.iwcm.editor.InlineEditor"%><%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
	sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/ckeditor/custom/config.js", null, request, response);
%><%@ page pageEncoding="utf-8" contentType="text/javascript" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
CKEDITOR.editorConfig = function( config )
{
	<% if ("false".equals(request.getSession().getAttribute("combineEnabled"))) { %>
		config.plugins =
			'basicstyles,' +
			'clipboard,' +
			'colorbutton,' +
			'colordialog,' +
			'contextmenu,' +
			'dragresize,' +
			'dialogadvtab,' +
			'elementspath,' +
			'enterkey,' +
			'entities,' +
			'find,' +
			'font,' +
			'format,' +
			'forms,' +
			'horizontalrule,' +
			'htmlwriter,' +
			'image,' +
			'indentlist,' +
			'indentblock,' +
			'justify,' +
			'language,' +
			'link,' +
			'list,' +
			'liststyle,' +
			//'magicline,' +
			'pastefromword,' +
			'pastetext,' +
			'removeformat,' +
			'showblocks,' +
			'showborders,' +
			'smiley,' +
			'sourcearea,' +
			'specialchar,' +
			'stylescombo,' +
			'tab,' +
			'table,' +
			'tabletools,' +
			'toolbar,' +
			'undo,' +
			'uploadimage,' +
			'uploadwidget,' +
			'wysiwygarea,' +
			'webjetcomponents,quicktable,forms';
	<% } %>

	config.title = false;

	config.entities_latin = false;
	config.pasteFromWordPromptCleanup = true;
	config.pasteFromWordRemoveStyles = true;
	config.pasteFromWordRemoveFontStyles = true;
	config.pasteTools_removeFontStyles = true;
	config.disableNativeSpellChecker = false;
	config.editorAutomaticWordClean = <%=Constants.getBoolean("editorAutomaticWordClean")%>

	config.fillEmptyBlocks = function( element ) {
	    //if ( element.attributes[ 'class' ].indexOf( 'clear-both' ) != -1 )
	    //console.log(element);
	    if (element.name=="div") return false;

	    return true;
	};
	<%
	boolean hasFontAwesome = false;
	if (FileTools.isFile("/css/font-awesome.min.css") || Constants.getBoolean("editorEnableFontAwesome")==true) hasFontAwesome = true;
	boolean hasTooltip = false;
	if (FileTools.isFile("/components/tooltip/tooltip.jsp"))
	{
	   if (DictionaryDB.getAll().size()>0) hasTooltip = true;
	}
	%>

	<% if ("true".equals(Tools.getRequestParameter(request, "inline"))) { %>
		config.extraPlugins = "floatingspace<% if ("cloud".equals(Constants.getInstallName()) || "webjet8".equals(Constants.getInstallName())) {%><% } %>,webjetmagicline,sharedspace<% if (hasFontAwesome) { %>,fontawesome<% } %>";
		config.sharedSpaces = {
		    top: 'wjInlineCkEditorToolbarElement'
		}
		config.removePlugins = 'maximize,resize';
		config.forceEnterMode = true;
	<% } else if ("standalone".equals(Tools.getRequestParameter(request, "toolbar"))) { %>
		config.extraPlugins = "codemirror,webjetfloatingtools<% if (hasFontAwesome) { %>,fontawesome<% } %>";
		config.height = 600;
	<% } else { %>
		config.extraPlugins = "codesnippet,codemirror,webjetfloatingtools,webjetmagicline<% if (hasFontAwesome) { %>,fontawesome<% } %>,webjettooltip";
	<% } %>
	config.magicline_color="#F7CA18";
	config.magicline_triggers = { <%=Constants.getString("editorMagiclineElements")%> };
	config.bodyId = "WebJETEditorBody";
	if (window.location.href.indexOf("inline")!=-1)
	{
		config.bodyClass = 'webjetInline';
	}

	config.toolbar = [
		<% if ("true".equals(Tools.getRequestParameter(request, "inline"))) { %>
           //{ name: 'publish', items: ['InlinePublish']},
		<% } %>
		<%
		String toolbar = Constants.getString("ckeditor_toolbar");

		String customToolbar = null;
		if (Tools.isNotEmpty(Tools.getRequestParameter(request, "toolbar"))) customToolbar = Constants.getString("ckeditor_toolbar-"+Tools.getRequestParameter(request, "toolbar"));
		if (Tools.isNotEmpty(customToolbar)) toolbar = customToolbar;
		//zatial zakomentovane, neotestovane:
		if (hasTooltip) toolbar = Tools.replace(toolbar, "SpecialChar", "SpecialChar' , 'Tooltip");
		if (hasFontAwesome) toolbar = Tools.replace(toolbar, "SpecialChar", "FontAwesome");

		if ("pageBuilder".equals(Tools.getRequestParameter(request, "inlineMode")))
		{
			//toto nam ziadno nepomoze: toolbar += ",{ name: 'Layout', items: ['layout-desktop',';','layout-tablet','layout-mobile']}";
		}
		else if( Constants.getBoolean("gridEditorEnabled") || InlineEditor.EditingMode.gridEditor.toString().equals(Tools.getRequestParameter(request, "inlineMode")) ) toolbar += ",{ name: 'Layout', items: ['GridEditor','layout-desktop',';','layout-tablet','layout-mobile']}";

		out.print(toolbar);
		%>
	];

	<% if (Tools.isNotEmpty(Constants.getString("ckeditor_removeButtons"))) { %>
	    config.removeButtons = "<%=Constants.getString("ckeditor_removeButtons")%>";
	<% } %>

    config.floatingToolsGroups = [
        <%=Constants.getString("ckeditor_floatingToolsGroups")%>
    ];

	//quicktable
	config.qtWidth = "100%";
}
