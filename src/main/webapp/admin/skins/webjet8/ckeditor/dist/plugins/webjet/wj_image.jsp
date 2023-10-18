<%@page import="sk.iway.iwcm.users.UsersDB"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.editor.*,java.util.*"%>

<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"%>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"%>
<%@ taglib uri="/WEB-INF/datetime.tld" prefix="dt"%><iwcm:checkLogon admin="true" perms="menuWebpages"/>

<%
	boolean denyImageSize = false;
	Identity user = UsersDB.getCurrentUser(request);
	if (user != null)
	{
		if (user.isDisabledItem("editorMiniEdit")==false) denyImageSize = Constants.getBoolean("FCKConfig.DenyImageSize[Basic]");
		else denyImageSize = Constants.getBoolean("FCKConfig.DenyImageSize[Default]");
	}

	FileTools.createDefaultStaticContentFolders();
	request.setAttribute("adminJqueryVersion", "adminJqueryJs");
%>
<%@ include file="/admin/layout_top_popup.jsp" %>

<script type="text/javascript">
	var wjCkMapping = [
		{wj: 'txtUrl', tab:'info', ck:'txtUrl'},
		{wj: 'txtAlt', tab:'info', ck:'txtAlt'},
		{wj: 'cmbAlign', tab:'info', ck:'cmbAlign'}
	];
</script>

<script type="text/javascript" src="js/fck_dialog_common.jsp"></script>
<script type="text/javascript">
		<!--
		function selectLink(link, lastModified)
		{
			var urlPath = link;
			if (urlPath.indexOf("?")==-1) urlPath = urlPath + "?v="+lastModified; // Date.now();

			try
			{
				var classNames = window.parent.CKEDITOR.dialog.getCurrent().getContentElement("advanced", "txtGenClass").getValue();
				if (classNames != undefined && classNames != null && classNames.indexOf("fixedSize")!=-1)
				{
					var actualWidth = window.parent.CKEDITOR.dialog.getCurrent().getContentElement("info", "txtWidth").getValue();
					var actualHeight = window.parent.CKEDITOR.dialog.getCurrent().getContentElement("info", "txtHeight").getValue();
					var ip=6;

					try
					{
						//podpora pre zadanie css ako fixedSize-700-400
						var fixedSizeRegex = /fixedSize-(\d+)-(\d+)(?:-(\d+))?/gi;
						var matched = fixedSizeRegex.exec(classNames);
						//console.log(matched);
						if (matched != null && matched.length == 4)
						{
							actualWidth = matched[1];
							actualHeight = matched[2];
							if (matched[3]!=undefined) ip = matched[3];

							if ("0"==actualWidth || 0==actualWidth) actualWidth="";
							if ("0"==actualHeight || 0==actualHeight) actualHeight="";
						}
					} catch (e) {}

					if (urlPath.indexOf("http")!=0 && (urlPath.indexOf(".jpg")>0 || urlPath.indexOf(".jpeg")>0 || urlPath.indexOf(".png")>0 || urlPath.indexOf(".gif")>0))
					{
						urlPath = "/thu"+"mb" + urlPath;
						if (urlPath.indexOf("?")==-1) urlPath += "?";
						else urlPath += "&";
						urlPath += "w="+actualWidth+"&h="+actualHeight+"&ip="+ip;
					}
				}

				if (classNames != undefined && classNames != null && classNames=="")
				{
					window.parent.CKEDITOR.dialog.getCurrent().getContentElement("advanced", "txtGenClass").setValue("img-responsive img-fluid");
				}

			}
			catch (e) { console.log(e); }

			GetE('txtUrl').value=urlPath;

			//GetE('txtWidth').value = width;
			//GetE('txtHeight').value = height;

			updateValuesToCk();
		}

		function copyAltToTitle()
		{
			GetE('txtAttTitle').value = GetE('txtAlt').value;
		}
		//-->
		</script>

		<%
		//je to tu takto, pretoze inak to spomalovalo pracu v editore
		request.setAttribute("uploadIcon", "wjIconBig-uploadImage");
		pageContext.include("/admin/elFinder/jsinclude.jsp");
		%>

	<style type="text/css">
		body { font-family:arial, verdana, sans-serif; background-color: #f5f5f5;}
		.button {
			width: 100px;
			position:relative;
			display: -moz-inline-stack;
			display: inline-block;
			vertical-align: top;
			zoom: 1;
			*display: inline;
			margin:0 3px 3px 0;
			padding:1px 0;
			text-align:center;
			border:1px solid #ccc;
			background-color:#eee;
			margin:1em .5em;
			padding:.3em .7em;
			border-radius:5px;
			-moz-border-radius:5px;
			-webkit-border-radius:5px;
			cursor:pointer;
		}
		div.innerPadding { padding-left: 12px; padding-right: 12px; padding-top: 8px; }
	</style>

	<script>
		var lastDocId = -1;
		var lastGroupId = -1;
		var lastVirtualPath = "";
		var elfinder;
		var elFinderInstance;

		function initializeElfinder()
		{
			var docId = window.parent.getCkEditorInstance().element.$.form.docId.value;
			var groupId = window.parent.getCkEditorInstance().element.$.form.groupId.value;
			var virtualPath = window.parent.getCkEditorInstance().element.$.form.virtualPath.value;


			//console.log("Initializing elfinder, docid="+docId+" groupId="+groupId);

			lastDocId = docId;
			lastGroupId = groupId;
            lastVirtualPath = virtualPath;

			var customData = {
					volumes : "images",
					docId: docId,
					groupId: groupId
			}

			var file = $('#txtUrl').val();
            if (file.indexOf("/images")!=0 || file.indexOf("/files")!=0)
            {
                file = "";
            }
			//console.log(file);
			if (file != "") {
				customData.startPath = file.substring(0, file.lastIndexOf('/'));
			}
			//console.log(customData);

			elfinder = $('#finder').elfinder({
				// requestType : 'post',

				// url : 'php/connector.php',
				url : '<iwcm:cp/>/admin/elfinder-connector/',
            enableByMouseOver: false,
				width: '100%',
				height: 395,
				resizable: false,
				rememberLastDir: false,
				requestType: 'post',
				customData : customData,
				 getFileCallback : function(files, fm)
				 {
					 	//console.log("getFileCallback");
					 	//console.log(files);
						//window.alert(files);
					 	selectLink(files.virtualPath, files.ts);
					 	window.parent.CKEDITOR.dialog.getCurrent()._.buttons['ok'].click();
				 },
				handlers : {
					select : function(event, elfinderInstance)
					{
						//console.log("select");
						//console.log(event);
						//console.log(event.target);

						var selected = event.data.selected;

						var files = elfinderInstance.selectedFiles();
						//v nazve musi byt znak bodka - prevencia pred vybratim adresara klikanim v strome
						if (files.length == 1 && files[0].virtualPath.indexOf(".")!=-1) {
							selectLink(files[0].virtualPath, files[0].ts);
						}

						//console.log("2"+selected.url);

						/*if (selected.length) {
							// console.log(elfinderInstance.file(selected[0]))
							selectLink(selected[0].url);
						}*/

					},
					add : function(event, elfinderInstance) {
						processEventReload(event, elfinderInstance);
					},
					remove : function(event, elfinderInstance) {
						processEventReload(event, elfinderInstance);
					},
					change : function(event, elfinderInstance) {
						processEventReload(event, elfinderInstance);
					}
            },

				lang : '<%=sk.iway.iwcm.i18n.Prop.getLngForJavascript(request)%>',

                commands : [
                    'fileopen', 'dirprops', 'fileprops', 'open', 'reload', 'home', 'up', 'back', 'forward', 'getfile', 'quicklook',
                    'download', 'rm', 'duplicate', 'rename', 'mkdir', 'mkfile', 'upload', 'copy',
                    'cut', 'paste', 'edit', 'extract', 'archive', 'search', 'info', 'view', 'help', 'resize', 'sort', 'netmount', 'fileupdate'
                    <% if (Constants.getBoolean("elfinderMetadataEnabled")) { %>,'wjmetadata'<% } %>
                ],
                contextmenu : {
                    files  : ['edit', 'fileopen', 'fileupdate', '|', 'quicklook', '|', 'download', '|', 'copy', 'cut', 'paste', 'duplicate', '|', 'rm', '|', 'rename', 'resize', '|', 'archive', 'extract', '|', 'info', 'fileprops', 'dirprops'<% if (Constants.getBoolean("elfinderMetadataEnabled")) { %>, 'wjmetadata'<% } %>]
                },

				ui: ['toolbar', /*'places',*/ 'tree', 'path', 'stat'],

				uiOptions : {
					toolbar : [
			           ['back', 'forward'],
			           ['paste', 'cut', 'copy'],
			           ['upload', 'mkdir', 'reload' /*'mkfile', */],

			           ['view', 'sort']
					]
				}
			});

			elFinderInstance = elfinder.elfinder("instance");

			elfinderToolbarFix();

			var toolbarFixApplied = false;
            elfinder.elfinder('instance').bind('lazydone', function(event) {
                if (!toolbarFixApplied) {
                    toolbarFixApplied = true;
                    elfinderToolbarFix();

                    if (file != "")
                    {
                        setTimeout(function() { openElfinderInFolder(file); }, 300);
                    }
                    else
                    {

                        setTimeout(function() { openDefaultImageFolder(true); }, 300);
                    }
                }
			});

			elFinderInitialized = true;

			//updateElfinderCustomData();
			//console.log("file="+file);
		}

		function updateElfinderCustomData()
		{
			var docId = window.parent.getCkEditorInstance().element.$.form.docId.value;
			var groupId = window.parent.getCkEditorInstance().element.$.form.groupId.value;
            var virtualPath = window.parent.getCkEditorInstance().element.$.form.virtualPath.value;

			//console.log("Update elfinder data, docid="+docId+" groupId="+groupId);

			var elfinder = $('#finder').elfinder('instance');

			var customData = {
					volumes : "images",
					docId: docId,
					groupId: groupId
			}

			var file = $('#txtUrl').val();
            if (file.indexOf("/images")!=0 || file.indexOf("/files")!=0)
            {
                file = "";
            }
			//console.log(file);
			if (file != "")
			{
				customData.startPath = file.substring(0, file.lastIndexOf('/'));
			}
			//console.log(customData);

			elfinder.options.customData = customData;

			//console.log("elFinder custom data: docId="+docId+" groupId="+groupId);

			//console.log( elfinder.commands );
			//console.log( elfinder.commands.places );

			//zvol prvy element v "Aktualna stranka"
			var openTimeout = 100;
			var reload = false
			if (lastDocId != docId || lastGroupId != groupId || virtualPath != lastVirtualPath)
			{
			    //console.log("RELOADING, virtualPath="+virtualPath+" lastVirtualPath="+lastVirtualPath);
				reload = true;
				openTimeout = 500;
			}
			if (reload || file == "")
			{
                setTimeout(function() {
                    openDefaultImageFolder(reload);
                }, openTimeout);
            }

            lastDocId = docId;
            lastGroupId = groupId;
            lastVirtualPath = virtualPath;

            //console.log("FILE="+file);
			if (file != "")
			{
			    setTimeout(function() {
                    openElfinderInFolder(file);
                }, openTimeout);
			}

			//elfinder.commands.places.add("/images/gallery/test/");
		}

	</script>

	<div id="finder"><iwcm:text key="divpopup-blank.wait_please"/></div>

	<div class="innerPadding">
		<style type="text/css">
			table.urlFormTable { width: 100%; }
		</style>
		<table class="urlFormTable">
			<tr>
				<td title="<iwcm:text key="elfinder.image.urlTitle"/>"><iwcm:text key="components.groupEdit.url_info"/>:</td>
				<td colspan="2"><input id="txtUrl" style="width: 100%" type="text" /></td>
			</tr>
			<tr>
				<td title="<iwcm:text key="elfinder.image.altTitle"/>" style="white-space: nowrap; width: 90px;"><iwcm:text key="editor.image.alt"/>:</td>
				<td><input id="txtAlt" style="width: 100%;" type="text" <%if (Constants.getBoolean("editorImageDialogCopyAltToTitle")) { out.print("onkeydown='copyAltToTitle()' onfocus='copyAltToTitle()' onblur='copyAltToTitle()'"); } %> /></td>
				<td style="white-space: nowrap; width: 200px;">
					<iwcm:text key="editor.image.align"/>:
					<select id="cmbAlign" style="width: 110px;">
						<option value=""><iwcm:text
							key="editor.image.align_none" /></option>
						<option value="left"><iwcm:text key="editor.image.align_left"/></option>
						<option value="right"><iwcm:text key="editor.image.align_right"/></option>
						<%if (Constants.getBoolean("editorAdvancedImageAlignment")) {%>
						<option value="baseline"><iwcm:text key="editor.image.align_baseline"/></option>
						<option value="top"><iwcm:text key="editor.image.align_top"/></option>
						<option value="text-top"><iwcm:text key="editor.image.align_texttop"/></option>
						<option value="middle"><iwcm:text key="editor.image.align_middle"/></option>
						<option value="bottom"><iwcm:text key="editor.image.align_bottom"/></option>
						<%} %>
					</select>
				</td>
			</tr>
		</table>
	</div>

<script type="text/javascript">
	<logic:present parameter="setfield">
   GetE('txtUrl').value = window.parent.dialogArguments.Editor.document.<%=Tools.getRequestParameter(request, "setfield")%>.value;
   </logic:present>
   <%if (Constants.getBoolean("editorImageDialogCopyAltToTitle")) { out.print("window.setTimeout(copyAltToTitle, 2000);"); } %>

   refreshValuesFromCk();
   setChangeHandlerToUpdateCk();
</script>

<%@ include file="/admin/layout_bottom_popup.jsp" %>
