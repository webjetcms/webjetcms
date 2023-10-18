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
	request.setAttribute("adminJqueryVersion", "adminJqueryJs");
%>
<%@ include file="/admin/layout_top_popup.jsp" %>

<script type="text/javascript">
	var wjCkMapping = [
		{wj: 'txtUrl', tab:'info', ck:'url'},
	];
</script>

<script type="text/javascript" src="js/fck_dialog_common.jsp"></script>
<script type="text/javascript">
		<!--
		function selectLink(link)
		{
			var urlPath = link;

			//window.parent.CKEDITOR.dialog.getCurrent().getContentElement("info", "url").setValue(link);

			GetE('txtUrl').value=urlPath;
			updateValuesToCk();

			getFileInfoTitle(link);
		}

		function getFileInfoTitle(link)
		{
			try
			{
				if (window.parent && window.parent.tempId)
				{
					var tempId = window.parent.editorForm.tempId.value;
					var title = window.parent.CKEDITOR.dialog.getCurrent().getValueOf("advanced", "advTitle");

					$.ajax({
			   	  type: "POST",
			   	  url: "/admin/skins/webjet8/ckeditor/dist/plugins/webjet/link_file_info_ajax.jsp",
			   	  data: "time="+<%=System.currentTimeMillis()%>+"&link="+link+"&tempId="+tempId+"&title="+encodeURIComponent(title),
			   	  success: function(data){
				   	  if($.trim(data) != "")
				   	  {
		   	  		  		//$("#txtAttTitle").val($.trim(data));
		   	  		  		window.parent.CKEDITOR.dialog.getCurrent().getContentElement("advanced", "advTitle").setValue($.trim(data));
				   	  }
			   	  }
			   	});
				}
			} catch (e) { console.log(e); }
		}
		//-->
		</script>

		<%
		//je to tu takto, pretoze inak to spomalovalo pracu v editore
		request.setAttribute("uploadIcon", "wjIconBig-uploadDoc");
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
		iframe.iframe {width: 100%; height: 300px; border: 0 none;}
	</style>

	<script>
		var lastDocId = -1;
		var lastGroupId = -1;
        var lastVirtualPath = "";
        var elFinderInstance;

		function checkSelectionText()
		{
			try
			{
				var selectionText = window.parent.getCkEditorInstance().getSelectedHtml().$.textContent;
				var linkHref = window.parent.CKEDITOR.dialog.getCurrent().getContentElement("info", "url").getValue();
				//console.log("selectionText="+selectionText+" urlValue="+linkHref);
				if (linkHref == "")
				{
					var setLink = null;
					if (selectionText.indexOf("@")!=-1 && selectionText.indexOf(".")!=-1)
					{
						setLink = "mailto:"+selectionText;
					}
					else if (selectionText.indexOf("http")==0)
					{
						setLink = selectionText;
					}
					else if (selectionText.indexOf("www")==0)
					{
						setLink = "http://"+selectionText;
					}

					if (setLink != null)
					{
						setTimeout(function() { selectLink(setLink); }, 100);
					}
				}
			}
			catch (e)
			{
				console.log(e);
			}
		}

		function initializeElfinder()
		{
            var docId = window.parent.getCkEditorInstance().element.$.form.docId ? window.parent.getCkEditorInstance().element.$.form.docId.value : -1;
            var groupId = window.parent.getCkEditorInstance().element.$.form.groupId ? window.parent.getCkEditorInstance().element.$.form.groupId.value : -1;
            var virtualPath = window.parent.getCkEditorInstance().element.$.form.virtualPath ? window.parent.getCkEditorInstance().element.$.form.virtualPath.value : '/';

            lastDocId = docId;
            lastGroupId = groupId;
            lastVirtualPath = virtualPath;

            var customData = {
                volumes : "link",
                docId: docId,
                groupId: groupId
            }

            var file = $('#txtUrl').val();
            //console.log(file);
            if (file != "") {
                customData.startPath = file.substring(0, file.lastIndexOf('/'));
            }

			//console.log("Initializing elfinder, docid="+docId+" groupId="+groupId);

			elFinderInstance = $('#finder').elfinder({
				// requestType : 'post',

				// url : 'php/connector.php',
				url : '<iwcm:cp/>/admin/elfinder-connector/',
            enableByMouseOver: false,
				width: '100%',
				height: 430,
				resizable: false,
				rememberLastDir: false,
				requestType: 'post',
				customData : customData,
				//transport : new elFinderSupportVer1(),
				 getFileCallback : function(files, fm)
				 {
					 //console.log("getFileCallback");
					 //console.log(files);

						// 	console.log(files);
						//window.alert(files);
					 selectLink(files.virtualPath);
					 window.parent.CKEDITOR.dialog.getCurrent()._.buttons['ok'].click()
				 },
				handlers : {
					select : function(event, elfinderInstance)
					{
						//console.log("select");
						//console.log(event);

						var selected = event.data.selected;
						//console.log("1"+selected);

						var files = elfinderInstance.selectedFiles();
						//v nazve musi byt znak bodka - prevencia pred vybratim adresara klikanim v strome (okrem web stranok)
						if (files.length == 1 && (files[0].hash.indexOf("_doc_group")!=-1 || files[0].virtualPath.indexOf(".")!=-1))
						{
							selectLink(files[0].virtualPath);
							//console.log(files[0]);
						}

						setTimeout(function() {
								txtUrlOnChange($("#txtUrl")[0]);
						}, 100);
						//console.log("2"+files);
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
			}).elfinder('instance');

			elFinderInitialized = true;

			var toolbarFixApplied = false;
				elFinderInstance.bind('lazydone', function(event) {
                if (!toolbarFixApplied) {
                    toolbarFixApplied = true;
                    elfinderToolbarFix();

                    if (file != "")
                    {
                        openElfinderInFolder(file);
                    }
                    else
                    {
                        //zvol prvy element v "Aktualna stranka"
                        setTimeout(function () {
                            //console.log("CLICKING 2: ");
                            //console.log($("elfinder-navbar-wrapper").first());
                            if ($("span.elfinder-navbar-dir").size() > 1 && $($("span.elfinder-navbar-dir")[1]).hasClass("ui-state-active") == false) {
                                //console.log("REALLY CLICKING 0");
                                //$($("span.elfinder-navbar-dir.ui-droppable")[1]).trigger("click");
                                openDefaultImageFolder(false);
                            }
                        }, 500);
                    }
                }
			});

         <% if (Constants.getBoolean("elfinderMetadataEnabled") && Constants.getBoolean("elfinderMetadataAutopopup")) { %>
        	elFinderInstance.bind('upload', function(event)
        	{
        		if (event.data == null || event.data.added == null || event.data.added.length < 1) return;

        		var hashes = $.map(event.data.added, function(v, i){
                	if (v.virtualPath.indexOf("/files/") == 0) {
                    	return v.hash;
                	}

                	return false;
                });

                if (hashes.length > 0) {
                	elFinderInstance.exec("wjmetadata", hashes);
                }
         	});
         <% } %>

			/*
			//zvol prvy element v "Aktualna stranka"
			setTimeout(function() {
				//console.log("CLICKING 2: ");
				//console.log($("elfinder-navbar-wrapper").first());
				$("span.elfinder-navbar-root").each(function(index)
				{
					if ($(this).attr("id").indexOf("nav-iwcm_doc_group_volume")==0)
					{
						$(this).children("span.elfinder-navbar-arrow").trigger("click");
					}
					//$("span.elfinder-navbar-dir.ui-droppable:eq(1)").trigger("click");
				});

				checkSelectionText();
			}, 200);

			//zvol prvy element v "Aktualna stranka"
			setTimeout(function()
			{
				//console.log("CLICKING 2: ");
				//console.log($("elfinder-navbar-wrapper").first());
				if ($("span.elfinder-navbar-dir").size()>1 && $($("span.elfinder-navbar-dir")[1]).hasClass("ui-state-active")==false)
				{
					//console.log("REALLY CLICKING 0");
					//$($("span.elfinder-navbar-dir.ui-droppable")[1]).trigger("click");
					openDefaultImageFolder(false);
				}
			}, 500);
			*/
		}

		/*
		function openDefaultImageFolder(refreshData)
		{
			if (refreshData)
			{
				$("span.elfinder-navbar-dir").first().trigger("click");
				setTimeout(function() { $('#finder').elfinder('instance').sync(); }, 100);
			}

			setTimeout(function(){

				$("#finder .elfinder-navbar").scrollTop(0);

				//console.log("CLICKING: ");
				//console.log($("#nav-iwcm_fs_ap_volume_ elfinder-navbar-wrapper").first());
				//console.log($("span.elfinder-navbar-dir.ui-droppable").size()+" "+$($("span.elfinder-navbar-dir.ui-droppable")[1]).hasClass("elfinder-navbar-expanded"));
				//console.log($("span.elfinder-navbar-dir.ui-droppable")[1]);
				if ($("span.elfinder-navbar-dir").size()>1 && $($("span.elfinder-navbar-dir")[2]).hasClass("ui-state-active")==false)
				{
					//console.log("REALLY CLICKING 1");
					$($("span.elfinder-navbar-dir")[2]).trigger("click");
				}

				setTimeout(function(){
					if ($("span.elfinder-navbar-dir").size()>1 && $($("span.elfinder-navbar-dir")[2]).hasClass("elfinder-navbar-expanded")==false)
					{
						//console.log("REALLY CLICKING 2");
						//$("span.elfinder-navbar-dir.ui-droppable").first().trigger("click");
						$($("span.elfinder-navbar-dir")[2]).trigger("click");
					}
				}, 500);

				setTimeout(function() { $('#finder').elfinder('instance').sync(); }, 1500);
			}, 500);
		}
		*/

		function updateElfinderCustomData()
		{
			//console.log("updateElfinderCustomData");

			var docId = window.parent.getCkEditorInstance().element.$.form.docId.value;
			var groupId = window.parent.getCkEditorInstance().element.$.form.groupId.value;
            var virtualPath = window.parent.getCkEditorInstance().element.$.form.virtualPath.value;

			var elfinder = $('#finder').elfinder('instance');

            var customData = {
                volumes : "link",
                docId: docId,
                groupId: groupId
            }

            var file = $('#txtUrl').val();
            //console.log("file="+file);
            if (file != "")
            {
                customData.startPath = file.substring(0, file.lastIndexOf('/'));
            }
            //console.log(customData);

            elfinder.options.customData = customData;

			//console.log("elFinder custom data: docId="+docId+" groupId="+groupId);
            var openTimeout = 100;
            var reload = false
            if (lastDocId != docId || lastGroupId != groupId || virtualPath != lastVirtualPath)
            {
                reload = true;
                openTimeout = 500;
            }
            if (reload || file == "")
            {
                openDefaultImageFolder(reload);
            }

            lastDocId = docId;
            lastGroupId = groupId;
            lastVirtualPath = virtualPath;

            if (file != "")
            {
                setTimeout(function() {
                    openElfinderInFolder(file);
                }, openTimeout);
            }

			//elfinder.commands.places.add("/images/gallery/test/");

			checkSelectionText();
		}

	</script>

	<div id="finder"><iwcm:text key="divpopup-blank.wait_please"/></div>

	<div class="innerPadding">
		<style type="text/css">
			table.urlFormTable { width: 100%; }
		</style>
		<script type="text/javascript">
			function txtUrlOnChange(field)
			{
				//console.log(field.value)
				if (field.value.indexOf("http")==0 || field.value.indexOf("www.")==0 || field.value.indexOf("/files/")==0)
				{
					//console.log(window.parent.CKEDITOR.dialog.getCurrent().getContentElement("target", "linkTargetType").getElement());
					window.parent.CKEDITOR.dialog.getCurrent().getContentElement("target", "linkTargetType").setValue("_blank");
				}
				if (field.value.indexOf("www.")==0)
				{
					//field.value = "http://"+field.value;
					window.parent.CKEDITOR.dialog.getCurrent().getContentElement("info", "protocol").setValue("http://");
				}
			}
		</script>
		<table class="urlFormTable">
			<tr>
				<td style="width: 80px;" title="<iwcm:text key="elfinder.image.urlTitle"/>"><iwcm:text key="components.groupEdit.url_info"/>:</td>
				<td><input id="txtUrl" style="width: 100%; outline: none !important;" type="text" onkeyup="txtUrlOnChange(this);" /></td>
			</tr>
		</table>
	</div>

<script type="text/javascript">
   refreshValuesFromCk();
   setChangeHandlerToUpdateCk();
</script>

	<div class="modal fade" id="elfinder-modal" tabindex="-1" role="dialog" aria-labelledby="elfinder-modal">
		<div class="modal-dialog" role="document">
			<div class="modal-content">
				<div class="modal-header">
					<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
					<h4 class="modal-title" id="modalLabel"><iwcm:text key="fbrowser.dirprop.dir"/></h4>
				</div>
				<div class="modal-body">

				</div>
				<div class="modal-footer">

					<button type="button" class="btn btn-default closeModal" data-dismiss="modal"><iwcm:text key="webpages.modal.close" /></button>
					<select class="btn form-control action" name="action" style="display: inline-block; width: 150px;">
						<option class="bSubmit" value="bSubmit"><iwcm:text key="fbrowse.save_button"/></option>
						<option class="bConvert2Pdf" value="bConvert2Pdf"><iwcm:text key="editor_dir.convert2pdf"/></option>
						<option class="bConvert" value="bConvert"><iwcm:text key="editor_dir.convert"/></option>
				        <iwcm:menu name="fileIndexer">
				        	<option class="bReindex" value="bReindex"><iwcm:text key="fbrowse.reindex_file"/></option>
				        </iwcm:menu>
				        <option class="bUsage" value="bUsage"><iwcm:text key="fbrowse.usage_button"/></option>
           				<option class="showHistory" value="showHistory"><iwcm:text key="groupslist.show_history"/></option>
           				<option class="bUnzip" value="bUnzip"><iwcm:text key="fbrowse.unzip_button"/></option>
						<option class="bBackup" value="bBackup"><iwcm:text key="fbrowse.backup_button"/></option>
           				<option class="bDelete" value="bDelete"><iwcm:text key="fbrowse.delete_file"/></option>
					</select>
					<button type="button" name="submit" class="btn btn-primary submit"><iwcm:text key="elfinder.dirprops.save" /></button>

				</div>
			</div>
		</div>
	</div>

<%@ include file="/admin/layout_bottom_popup.jsp" %>
