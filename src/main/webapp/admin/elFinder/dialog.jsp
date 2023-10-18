<%@page import="org.apache.struts.util.ResponseUtils"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Constants,sk.iway.iwcm.FileTools" %>
<%@ page import="sk.iway.iwcm.SetCharacterEncodingFilter" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><iwcm:checkLogon admin="true" perms="menuFbrowser|menuWebpages"/><%
String titleKey = "Files";

String form = Tools.getStringValue(Tools.getRequestParameter(request, "form"), "");
String elementName = Tools.getStringValue(Tools.getRequestParameter(request, "elementName"), "");
String callback = Tools.getStringValue(Tools.getRequestParameter(request, "callback"), "");
String volumes = Tools.getStringValue(Tools.getRequestParameter(request, "volumes"), "link");

// file, directory, fileDirectory, files, directories, filesDirectories
String selectMode = Tools.getStringValue(Tools.getRequestParameter(request, "selectMode"), "file");

	boolean rememberLastDir = true;
	if (Constants.getBoolean("enableStaticFilesExternalDir"))
	{
		//pre tento pripad nepamatajme, robilo haluze pri zmenach domeny a pristupoch do /images a /files foldrov
		rememberLastDir = false;
	}
	String actualFile = "";
	if (Tools.isNotEmpty(Tools.getRequestParameter(request, "link"))) actualFile = Tools.getRequestParameter(request, "link");

	FileTools.createDefaultStaticContentFolders();

%><% Tools.insertJQuery(request); %><!doctype html><html>
<head>
	<meta http-equiv="X-UA-Compatible" content="IE=edge" />
	<meta http-equiv="Content-Type" content="text/html; charset=<%=SetCharacterEncodingFilter.getEncoding() %>">

	<title><iwcm:text key="<%=titleKey%>"/></title>
	<link rel="stylesheet" href="/components/cmp.css" />
	<iwcm:combine type="css" set="adminStandardCss" />
	<link rel="stylesheet" href="/admin/skins/webjet8/assets/global/css/custom.css" />
	<link rel="stylesheet" href="/admin/skins/webjet8/css/fck_dialog.css" />
	<link rel=stylesheet href="/admin/codemirror/lib/codemirror.css">
	<link rel=stylesheet href="/admin/codemirror/theme/eclipse.css">
	<link rel="stylesheet" href="/admin/codemirror/addon/display/fullscreen.css">
	<link rel="stylesheet" href="/admin/codemirror/addon/dialog/dialog.css">

	<iwcm:combine type="js" set="adminJqueryJs" />

	<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/common.jsp"></script>
	<script type='text/javascript' src='/components/calendar/popcalendar.jsp'></script><%request.setAttribute("sk.iway.iwcm.tags.CalendarTag.isJsIncluded", "true");%>
	<script src="/admin/codemirror/lib/codemirror.js"></script>
	<script src="/admin/codemirror/mode/xml/xml.js"></script>
	<script src="/admin/codemirror/mode/javascript/javascript.js"></script>
	<script src="/admin/codemirror/mode/css/css.js"></script>
	<script src="/admin/codemirror/mode/htmlmixed/htmlmixed.js"></script>
	<script src="/admin/codemirror/addon/mode/multiplex.js"></script>
	<script src="/admin/codemirror/mode/htmlembedded/htmlembedded.js"></script>
	<script src="/admin/codemirror/addon/edit/matchbrackets.js"></script>
	<script src="/admin/codemirror/addon/display/fullscreen.js"></script>
	<script src="/admin/codemirror/addon/dialog/dialog.js"></script>
	<script src="/admin/codemirror/addon/search/searchcursor.js"></script>
	<script src="/admin/codemirror/addon/search/search.js"></script>

	<style type="text/css">
		.tabbable-custom > .nav-tabs {
			background-color: white;
			border-top: 0px;
			padding-top: 0px;
			overflow: hidden;
		}
		.tabbable-custom > .nav-tabs li {
			font-weight: bold;
			border-top: 0px;
		}
		.tabbable-custom > .nav-tabs li a {
			color: #646979 !important;
			margin-top: 0px;
			border: 0px;
		}
		.tabbable-custom > .nav-tabs li a.active {
			color: #0063FB !important;
			background-color: white !important;
			font-weight: bold;
		}
	</style>
</head>
<body style="overflow: hidden;">

	<jsp:include page="jsinclude.jsp"/>

		<style type="text/css">
			body { font-family:arial, verdana, sans-serif;}
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
	/*
			#dialog {
				position:absolute;
				left:50%;
				top:1000px;
			}
	*/
		</style>
		<script type="text/javascript">
			//premenna pre codemirror
			var editor = null;

            var lastDocId = -1;
            var lastGroupId = -1;
			var elfinder;
            var elFinderInstance;

			function initializeElfinder()
			{
				var customData = {volumes : "<%= volumes %>"};

                customData.wjmetadata = {
                    metadata: {
						enabled: <%= Constants.getBoolean("elfinderMetadataEnabled") %>,
						title: '<iwcm:text key="components.elfinder.wjmetadata.metadata.title" />',
						iframe: '/components/elfinder/metadata/admin_metadata_form.jsp'
                    },
            		fileArchive: {
                        enabled: <%= Constants.getBoolean("elfinderFileArchiveEnabled") %>,
                        title: '<iwcm:text key="components.elfinder.wjmetadata.fileArchive.title" />',
						iframe: '/components/elfinder/metadata/admin_metadata_fielarchive_form.jsp',
                        //root: '/files/archiv'
						root: '/<%= FileArchivatorKit.getArchivPath() %>'
                    }
            	}

				try
				{
						var ckEditorInstance;

						try {
						 	ckEditorInstance = window.parent.opener.getCkEditorInstance();
						} catch (e) {}
						try {
							if(typeof ckEditorInstance == "undefined") ckEditorInstance = window.parent.getCkEditorInstance(); //webjet9
						} catch (e) {}
						try {
							if(typeof ckEditorInstance == "undefined") ckEditorInstance = window.parent.opener.opener.parent.parent.parent.getCkEditorInstance();
						} catch (e) {}
						try {
							if(typeof ckEditorInstance == "undefined") ckEditorInstance = window.parent.opener.parent.getCkEditorInstance(); //media iframe
						} catch (e) {}
						try {
							if(typeof ckEditorInstance == "undefined") ckEditorInstance = window.parent.opener.parent.parent.getCkEditorInstance(); //dialog.jsp otvoreny z aplikacie app-docsembed
						} catch (e) {}

						if (typeof ckEditorInstance != "undefined")
						{
							var docId = ckEditorInstance.element.$.form.docId.value;
							var groupId = ckEditorInstance.element.$.form.groupId.value;

							customData.volumes = "<%= volumes %>";
							customData.docId = docId;
							customData.groupId = groupId;

							lastDocId = docId;
							lastGroupId = groupId;
						}
				}
				catch (e) { console.log(e); }

				var elFinderHeight = $(".page-content").innerHeight();
				//console.log("h1="+elFinderHeight);
				elFinderHeight -= parseInt($(".page-content").css("margin-top")) + 12;
				//console.log("h2="+elFinderHeight);

				elfinder = $('#finder').elfinder({
					// requestType : 'post',

					// url : 'php/connector.php',
					url : '<iwcm:cp/>/admin/elfinder-connector/',
               enableByMouseOver: false,
					width: '100%',
					height: elFinderHeight,
               rememberLastDir: <%=rememberLastDir%>,
					uploadOverwrite: true,
					customData : customData,
					requestType: 'post',
					//transport : new elFinderSupportVer1(),
					getFileCallback : function(files, fm)
					{
						//console.log(files);
						// window.alert(files);
					},
					handlers : {
	              	select : function(event, elfinderInstance) {

							// file, directory, fileDirectory, files, directories, filesDirectories
							var selectMode = '<%= selectMode %>';
							var selected = event.data.selected;
							var inputs = $('div.inputs');
							var rows = [];
							var completed = 0;

							if ((selectMode == 'file' || selectMode == 'directory' || selectMode == 'fileDirectory') && selected.length > 0)
							{
									var files = elfinderInstance.selectedFiles();
	                        //v nazve musi byt znak bodka - prevencia pred vybratim adresara klikanim v strome
	                        //console.log(files);
	                        if (files.length == 1 && (files[0].hash.indexOf("_doc_group")!=-1 || files[0].virtualPath.indexOf(".")!=-1))
	                        {
										var row = inputs.find('.row.template').hide().clone();
										row.removeClass('template').show();

										var label = row.find('label');
										var input = row.find('input');

										//console.log(files[0].virtualPath);

										var val = files[0].virtualPath;

                                		val = val.replace("////", "/");
                                		val = val.replace("///", "/");
                                		val = val.replace("//", "/");

                                		//console.log(val);

										input.val(val);
										rows.push(row);

										htmlCompleted();
									}
							}
							else if ((selectMode == 'files' || selectMode == 'directories' || selectMode == 'filesDirectories') && selected.length > 0) {
								$.each(selected, function(i, v){
									getData(v, function(data){
										completed++;

										if (isDataOk(data)) {
											var row = inputs.find('.row.template').hide().clone();
											row.removeClass('template').show();

											var input = row.find('input');

                                            var val = data.cwd.virtualPath;

                                            val = val.replace("////", "/");
                                            val = val.replace("///", "/");
                                            val = val.replace("//", "/");

                                            //console.log(val);

											input.val(val);
											rows.push(row);
										}

										if (completed == selected.length) {
											htmlCompleted();
										}
									});
								});
							}
							else if (selected.length > 0) {
								console.warn('SelectMode ' + selectMode + ' not defined');
							}

							function htmlCompleted() {
								if (rows.length > 0) {
									inputs.find('div.row:not(.template)').remove();
									$.each(rows, function(i, v){
										var label = v.find('label');
										var input = v.find('input');

										if (rows.length > 1) {
											label.attr('for', 'file' + i).text(label.text() + ' ' + (i + 1));
											input.attr('id', 'file' + i);
										}

										inputs.append(v);
									})

								}
							}

							function isDataOk(data) {
								if (data.cwd == null || data.cwd.mime == null) {
									return false;
								}

								if ((selectMode == 'file' || selectMode == 'files') && data.cwd.mime == 'directory') {
									return false;
								}

								if ((selectMode == 'directory' || selectMode == 'directories') && data.cwd.mime != 'directory') {
									return false;
								}

								return true;
							}

							function getData(target, callback) {
								var data = {
									cmd: 'open',
									target: target,
									'_': new Date().getTime()
								}

								$.ajax({
									url: '/admin/elfinder-connector/',
									data: data,
									dataType: 'json',
									success: function(data){
										callback(data);
									}
								});
							}
	               },
	               upload: function (event) {
	                    	if (event.type == "upload") {
	                    		var data = event.data;
	                    		var added = data.added;

	                    		if (added != null && added.length > 0) {
	                    			var file = added.slice(-1)[0];
	                    			$('#file').val(file.virtualPath);
	                    		}
	                    	}

	                    	var added = typeof event.data != 'undefined' && event.data.added != 'undefined' ? event.data.added : null;
                            elFinderInstance.exec("wjmetadata", added);
	               },
                  beforeUpload: function(files) {
                            //alert(files);
                            filesRespository = files;

                            //console.log(files);

                            var cwdHash = elFinderInstance.cwd().hash;
                            var allFiles = elFinderInstance.files();
                            var filesInFolder = [];

                            $.each(allFiles, function(key, value){
                                if (value.phash != null && value.phash == cwdHash) {
                                    filesInFolder.push(value.name);
                                }
                            });

                            var filesToUpload = files.input ? files.input.files : files.files;

                            if (files.input) {
                                filesToUpload = files.input.files;
                            }

                            if (files.files && files.files.files) {
                                filesToUpload = files.files.files;
                            }


                            var filesToAction = [];
                            $.each(filesToUpload, function(i, v)
                            {
                                if (v.name != undefined)
                                {
                                    var name = getName(v.name).toLowerCase();
                                    if ($.inArray(name, filesInFolder) != -1) {
                                        filesToAction.push(name);
                                    }
                                }
                            });

                            if (filesToAction.length > 0) {
                                openDialog(filesToAction, files);
                            }
                            else {
                                return elFinderInstance.uploadFiles(files);
                            }

                            function openDialog(filesToSend, files) {
                                var confirmText = "<%= Prop.getInstance().getText("elfinder.upload.confirm") %>";
                                confirmText = confirmText.replace('{1}', filesToSend.join(", \n"));
                                if (confirm(confirmText)) {
                                    var originalOptions = jQuery.extend(true, {}, elFinderInstance.options.customData);
                                    elFinderInstance.options.customData.replace = true;
                                    var ret = elFinderInstance.uploadFiles(filesRespository);
                                    elFinderInstance.options.customData = originalOptions;
                                    return ret;
                                }
                                else {
                                    return elFinderInstance.uploadFiles(filesRespository);
                                }
                            }

                            function getName(name)
                            {
                                var ret = WJ.internationalToEnglish(name);
                                ret = ret.toLowerCase();
                                ret = ret.trim();

                                ret = ret.replace(new RegExp(" +", 'g'), "-");
                                ret = ret.replace(new RegExp("\\.+", 'g'), ".");
                                ret = ret.replace(new RegExp("\\-+", 'g'), "-");
                                ret = ret.replace(new RegExp("[^a-zA-Z/_0-9\\-\\.=]", 'g'),"");
                                ret = ret.replace("---", "-");
                                ret = ret.replace("--", "-");
                                ret = ret.replace("____", "_");
                                ret = ret.replace("___", "_");
                                ret = ret.replace("__", "_");
                                ret = ret.replace("__", "_");
                                ret = ret.replace("_-_", "-");
                                ret = ret.replace("-_", "-");
                                ret = ret.replace("_-", "-");
                                ret = ret.replace("-.-", "-");
                                ret = ret.replace("-.html", ".html");
                                ret = ret.replace("-.jpg", ".jpg");
                                ret = ret.replace("-.doc", ".doc");
                                ret = ret.replace("-.xls", ".xls");
                                ret = ret.replace("-.pdf", ".pdf");
                                ret = ret.replace("-.", "-");
                                ret = ret.replace(".-", "-");
                                ret = ret.replace("./", "/");
                                ret = ret.replace("-/", "/");

                                return ret;
                            }

                            return false;
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
					// handlers : {
					// 	select : function(e) {
					// 		console.log(e.data)
					// 	}
					// },
					// onlyMimes : ['image', 'text/plain']
					// sync : 20000,
					lang : '<%=sk.iway.iwcm.i18n.Prop.getLngForJavascript(request)%>',
					//customData : {answer : 42},
					// requestType : 'POST',
					// rememberLastDir : false,
					// ui : ['tree', 'toolbar'],
					// ui : ['toolbar', 'path', 'stat'],
                    commands : [
                        'fileopen', 'dirprops', 'fileprops', 'open', 'reload', 'home', 'up', 'back', 'forward', 'getfile', 'quicklook',
                        'download', 'rm', 'duplicate', 'rename', 'mkdir', 'mkfile', 'upload', 'copy',
                        'cut', 'paste', 'wjedit', 'extract', 'archive', 'info', 'view', 'help', 'resize', 'sort', 'netmount', 'fileupdate', 'wjfilearchive',
                        'wjsearch', 'wjmetadata'
                    ],
                    contextmenu : {
                        files  : [
                            'wjedit', 'fileopen', 'fileupdate',
							'|',
							'quicklook',
							'|',
							'download',
							'|',
							'copy', 'cut', 'paste', 'duplicate',
							'|',
							'rm',
							'|',
							'rename', 'resize',
							'|',
							'archive', 'extract',
							'|',
							'info', 'fileprops', 'dirprops', 'wjmetadata'
						]
                    },
					commandsOptions : {
                        wjedit : {
							mimes : ['text/plain', 'text/html', 'text/javascript', 'text/jsp', 'text/css'],
							editors : [{

									mimes : ['text/plain', 'text/html', 'text/jsp', 'text/javascript', 'text/css'],
									load : function(textarea) {

										editor = CodeMirror.fromTextArea(textarea, {
										    lineNumbers: true,
										    styleActiveLine: true,
										    matchBrackets: true,
										    extraKeys: {
										        "F11": function(cm) {
										        	cm.setOption("fullScreen", !cm.getOption("fullScreen"));
										        },
										        "Esc": function(cm) {
										          if (cm.getOption("fullScreen")) cm.setOption("fullScreen", false);
										        },
										        "Alt-F": "findPersistent"
										    },
										    mode: "application/x-ejs",
										    theme: "eclipse"
										  })
				                    },

				                    close : function(textarea, instance) {
				                    	editor = null;
				                    },

				                    save : function(textarea, editorX) {
				                        textarea.value = editor.getValue();
				                        editor = null;
				                    }
								}
							]
						}
					},
					// uiOptions : {
					// 	toolbar : [['help']],
					// 	cwd : {
					//    listView : {
					//      // columns to be displayed
					//		// default settings are:
					//      // columns : ['perm', 'date', 'size', 'kind'],
					//      // extra columns can be displayed if your connector supports it:
					//		columns : ['perm', 'date', 'size', 'kind', 'owner'],
					//      // custom columns labels:
					//		columnsCustomName : {
					//			owner : 'Owner'
					//		}
					//	}
					// }
					ui: ['toolbar', /*'places',*/ 'tree', 'path', 'stat'],
                    uiOptions : {
                        toolbar : [
                            ['back', 'forward'],
                            ['paste', 'cut', 'copy'],
                            ['upload', 'mkdir', 'reload' /*'mkfile', */],
                            ['view', 'sort', 'wjfilearchive'],['search'],

                            /*['home', 'up'],*/

                            ['open' /*, 'download', 'getfile' - nefunguju */],
                            ['info'],
                            ['quicklook'],

                            ['rm'],
                            ['duplicate', 'rename', 'edit' /*, 'resize' - nefunguje */],
                            ['extract', 'archive'],
                            ['wjsearch']

                            /*['help']*/
                        ]
                    }
				});

                elFinderInstance = elfinder.elfinder("instance");

				var toolbarFixApplied = false;
                elFinderInstance.bind('lazydone', function(event) {
	                if (!toolbarFixApplied) {
	                     toolbarFixApplied = true;
	                     elfinderToolbarFix();
	                     elfinderTabClick("file");

							   //vypni search button
								try { if (elFinderInstance.root().indexOf("iwcm_archiv_volume")==-1) $("#finder .elfinder-button-search").hide(); } catch (e) {}

								var actualUrl = $("#file").val();
								if (actualUrl != "")
								{
											setTimeout(function() { try {
												openElfinderInFolder(actualUrl);
											} catch (e) {} }, 500);
										}
										else
								{
											setTimeout(function () {
												openDefaultImageFolder(false);
											}, 500);
								}
	                }
				});
			}

			function Ok()
			{
				var form = "<%= form %>";
				var elementName = "<%= elementName %>";
				var callback = "<%= callback %>";

				//console.log(window.parent);
				//console.log(window.parent.opener);

				var result = false;
				if (callback != '') {
					if (typeof window.parent.opener[callback] != 'undefined') {
						var inputs = $('div.inputs');
						var results = [];

						$.each(inputs.find('.row:not(.template)'), function(i,v){
							results.push($(this).find('input').val());
						});

						if (results.length==0)
						{
                            var val = $("#file").val();
                            if ("" != val) results.push(val);
						}

						window.parent.opener[callback](results);
						result = true;
					}
				}

				if (form != "" && elementName != "")
				{
					var val = $('div.inputs').find('.row:not(.template)').find('input').val();
					//aby bolo mozne do inputu priamo napisat www.webjet.sk
					if (typeof val == "undefined") val = $("#file").val();

					if ("ckEditorDialog"==form)
					{
						var dialog = null;
						//wj8 vs wj9
						if (window.parent.opener != null) dialog = window.parent.opener.CKEDITOR.dialog.getCurrent();
						else dialog = window.parent.CKEDITOR.dialog.getCurrent();

						var tabNamePair = elementName.split(":");
						var element = dialog.getContentElement(tabNamePair[0], tabNamePair[1]);
						element.setValue(val);
						result = true;
					}
					else
					{
						var element = window.parent.opener.$('form[name="' + form + '"] input[name="' + elementName + '"]');
						if (element.length > 0)
						{
							element.val(val);
							try{
							element.trigger('change').trigger('blur');
                            }catch (err){}
							result = true;
						}
						else
						{
							var element = $(window.parent.opener[form].elements[elementName]);
							element.val(val);
                            try{
                                element.trigger('change').trigger('blur');
                            }catch (err){}
                            result =  true;
						}
					}
				}

				return result;
			}
		</script>

		<div class="tabbable tabbable-custom tabbable-full-width">
		    <ul class="nav nav-tabs">
		        <li class="nav-item">
		            <a class="nav-link active" onclick="elfinderTabClick('file')" style="cursor:pointer" data-toggle="tab">
		                <iwcm:text key="fbrowse.file"/>
		            </a>
		        </li>
		        <%
					boolean pixabay = Constants.getBoolean("pixabayEnabled");
		         String toolsLast = " class=\"nav-item last\"";
					if (pixabay) toolsLast = "";
				  %>
		        <li<%=toolsLast %>>
		            <a class="nav-link" onclick="elfinderTabClick('tools')" style="cursor:pointer" data-toggle="tab">
		                <iwcm:text key="editor_dir.tools"/>
		            </a>
		        </li>
		        <% if (pixabay) { %>
		        <li class="nav-item last">
		            <a class="nav-link" onclick="elfinderTabClick('pixabay')" style="cursor:pointer" data-toggle="tab">
		                <iwcm:text key="editor.photobank"/>
		            </a>
		        </li>
		        <% } %>
		    </ul>
			<div id="finder"><iwcm:text key="divpopup-blank.wait_please"/></div>
			<div id="pixabay" style="display: none;"><iframe id="wjImagePixabayIframeElement" style="width: 100%; height: 480px; border: 0px;" src="/admin/skins/webjet8/ckeditor/dist/plugins/webjet/wj_pixabay.jsp" border="0" ></iframe></div>
		</div>
		<div class="inputs container-fluid" style="background: #fff; padding-top: 15px; height: 160px; overflow: auto;">
			<div class="row template">
				<div class="form-group col-sm-12">
					<label for="file" class="control-label block"><iwcm:text key="editor.media.link"/></label>
					<input class="form-control" name="file" id="file" value="<%=ResponseUtils.filter(actualFile)%>" />
				</div>
				<%--
				<div class="form-group col-sm-6">
					<label for="width" class="control-label"><iwcm:text key="components.video_player.width"/></label>
					<input class="form-control" type="input" name="width" id="width" />
				</div>

				<div class="form-group col-sm-6">
					<label for="height" class="control-label"><iwcm:text key="components.video_player.height"/></label>
					<input class="form-control" type="input" name="height"  id="height" />
				</div>
				--%>
			</div>
		</div>



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
						<iwcm:menu name="fileIndexer">
							<option class="bReindex" value="bReindex"><iwcm:text key="fbrowse.reindex_file"/></option>
						</iwcm:menu>
						<option class="bUsage" value="bUsage"><iwcm:text key="fbrowse.usage_button"/></option>
						<option class="showHistory" value="showHistory"><iwcm:text key="groupslist.show_history"/></option>
					</select>
					<button type="button" name="submit" class="btn btn-primary submit"><iwcm:text key="elfinder.dirprops.save" /></button>

				</div>
			</div>
		</div>
	</div>
</body>

<iwcm:combine type="js" set="adminStandardJs" />

</html>
