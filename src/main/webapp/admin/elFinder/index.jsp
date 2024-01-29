<%@page import="sk.iway.iwcm.Constants"%>
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.FileTools" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<iwcm:menu notName="menuFbrowser">
	<%
		response.sendRedirect("/admin/403.jsp");
	 	if (1==1) return;
	%>
</iwcm:menu>
<%
request.setAttribute("adminJqueryVersion", "adminJqueryJs");
%>
<%@ include file="/admin/layout_top.jsp" %>
<%
	boolean rememberLastDir = true;
	if (Constants.getBoolean("enableStaticFilesExternalDir"))
	{
	   //pre tento pripad nepamatajme, robilo haluze pri zmenach domeny a pristupoch do /images a /files foldrov
		rememberLastDir = false;
	}
	FileTools.createDefaultStaticContentFolders();
%>
<link rel="stylesheet" href="/admin/codemirror/lib/codemirror.css">
<link rel="stylesheet" href="/admin/codemirror/theme/eclipse.css">
<link rel="stylesheet" href="/admin/codemirror/addon/display/fullscreen.css">
<link rel="stylesheet" href="/admin/codemirror/addon/dialog/dialog.css">
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

<!-- BEGIN TITLE -->
<div class="row title">
    <h1 class="page-title"><i class="icon-folder"></i><iwcm:text key="menu.fbrowser"/></h1>
</div>
<!-- END TITLE -->

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

		tr.template {display: none;}
		div.change_name_box {}
/*
		#dialog {
			position:absolute;
			left:50%;
			top:1000px;
		}
*/
    /*
    #elfinder-modal .modal-dialog {width: 770px;}
    */
	</style>
	<script>
		//premenna pre codemirror
		var editor = null;
		var elFinderInstance;
		var filesRespository;

		function initializeElfinder()
		{
			var elFinderHeight = $(".page-content").innerHeight();
			//console.log("h1="+elFinderHeight);
			elFinderHeight -= parseInt($(".row.title").height()) + parseInt($("ul.nav.nav-tabs").outerHeight()) + 1;
			//console.log("h2="+elFinderHeight);

			elFinderInstance = $('#finder').elfinder({
				// requestType : 'post',
                'bind': {
                    'upload.pre mkdir.pre mkfile.pre rename.pre archive.pre ls.pre': [
                        'Plugin.Sanitizer.cmdPreprocess'
                    ],
                    'ls': [
                        'Plugin.Sanitizer.cmdPostprocess'
                    ],
                    'upload.presave': [
                        'Plugin.Sanitizer.onUpLoadPreSave'
                    ]
                },
				// url : 'php/connector.php',
				uploadOverwrite: true,
				overwriteUploadConfirm: true,
				url : '<iwcm:cp/>/admin/elfinder-connector/',
            	enableByMouseOver: false,
				width: '100%',
				height: elFinderHeight,
				rememberLastDir: <%=rememberLastDir%>,
				customData : {volumes : "files"},
				sortType : 'sort_priority',
				requestType: 'post',
				sortRules : {
					sort_priority : function(file1, file2) {
						var sort = 0;
						if (file1.mime == "directory" && file2.mime == "text/html") {
							sort = -1;
						}
						else if (file1.mime == "text/html" && file2.mime == "directory") {
							sort = 1;
						}
						else if (file1.mime == "directory" && file2.mime == "directory" ||
							file1.mime == "text/html" && file2.mime == "text/html") {

							sort = file1.sort_priority - file2.sort_priority;
						}

						if (sort == 0) {
							return file1.name.toLowerCase().localeCompare(file2.name.toLowerCase());
						}
					}
				},
				//transport : new elFinderSupportVer1(),
				getFileCallback : function(files, fm)
				{
						//window.alert(files);
				},
				handlers : {
					dblclick : function(event, elfinderInstance)
					{
						event.preventDefault();

						//console.log(event);

						elfinderInstance.exec('getfile')
							.done(function() { elfinderInstance.exec('edit'); })
							.fail(function() { elfinderInstance.exec('open'); });
					},
               		select : function(event, elfinderInstance) {
                        var selected = event.data.selected;

						if (selected.length) {
							// console.log(elfinderInstance.file(selected[0]))
						}
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
				// onlyMimes : ['image', 'text/plain']
				// sync : 20000,
				lang : '<%=sk.iway.iwcm.i18n.Prop.getLngForJavascript(request)%>',
				// customData : {answer : 42},
				// requestType : 'POST',
				// rememberLastDir : false,
				// ui : ['tree', 'toolbar'],
				// ui : ['toolbar', 'path', 'stat'],
				commands : [
                    'fileopen', 'dirprops', 'fileprops', 'open', 'reload', 'home', 'up', 'back', 'forward', 'getfile', 'quicklook',
                    'download', 'rm', 'duplicate', 'rename', 'mkdir', 'mkfile', 'upload', 'copy',
                    'cut', 'paste', 'wjedit', 'extract', 'archive', 'search', 'info', 'view', 'help', 'resize', 'sort', 'netmount', 'fileupdate', 'wjfilearchive', 'wjsearch'
                    <% if (Constants.getBoolean("elfinderMetadataEnabled")) { %>,'wjmetadata'<% } %>
				],
				contextmenu : {
					files  : ['wjedit', 'fileopen', 'fileupdate', '|', 'quicklook', '|', 'download', '|', 'copy', 'cut', 'paste', 'duplicate', '|', 'rm', '|', 'rename', 'resize', '|', 'archive', 'extract', '|', 'info', 'fileprops', 'dirprops'<% if (Constants.getBoolean("elfinderMetadataEnabled")) { %>, 'wjmetadata'<% } %>]
				},
				commandsOptions : {
                    wjedit : {
						//mimes : ['text/plain', 'text/html', 'text/javascript', 'text/jsp', 'text/css', 'text/xml', 'text/x-js'],
						editors : [{
								mimes : ['text/plain', 'text/html', 'text/jsp', 'text/javascript', 'text/css', 'text/xml', 'text/x-js'],
								load : function(textarea) {
									var ta = $(textarea),
						                taBase = ta.parent(),
						                dialog = taBase.parent();

									$(window).on('resize', function(){
										//resizeElfinderDialog(textarea);
									});

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
									  });

									resizeElfinderDialog(textarea);
			                    },

			                    close : function(textarea, instance) {
			                    	//editor = null;
			                    },

			                    save : function(textarea, editorX) {
			                        textarea.value = editor.getValue();
			                        //editor = null;
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

			           ['view', 'sort'],

			           /*['home', 'up'],*/

			           ['open' /*, 'download', 'getfile' - nefunguju */],
			           ['info'],
			           ['quicklook'],

			           ['rm'],
			           ['duplicate', 'rename', 'edit' /*, 'resize' - nefunguje */],
			           ['extract', 'archive'],
						['wjfilearchive'],
						['wjsearch']
			           /*['search'],*/

			           /*['help']*/
					]
				}
			}).elfinder('instance');

            var toolbarFixApplied = false;
            elFinderInstance.bind('lazydone', function(event) {
                if (!toolbarFixApplied) {
                    toolbarFixApplied = true;
                    elfinderToolbarFix();
                    elfinderTabClick("file");
                }
			});

            <% if (Constants.getBoolean("elfinderMetadataEnabled") && Constants.getBoolean("elfinderMetadataAutopopup")) { %>
            elFinderInstance.bind('upload', function(event) {


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
		}
		var lastDialogOptions = {};
		function resizeElfinderDialog(textarea)
		{
			console.log("resizeElfinderDialog");


			var ta = $(textarea),
                taBase = ta.parent(),
                dialog = taBase.parent();

			var realWidth = $(".page-content").width();
			var realHeight = $(window).height();
			var width = realWidth / 100 * 90;
			var height = realHeight / 100 * 60;
			var left = realWidth / 100 * 5;

			//jeeff: robime vzdy lebo ked zavrem dialog ma potom originalnu velkost
			//if (height != lastDialogOptions.height ||
			//	width != lastDialogOptions.width ||
			//	left !=  lastDialogOptions.left)
			{
				lastDialogOptions.height = height;
				lastDialogOptions.width = width;
				lastDialogOptions.left = left;

				dialog.css("height", height);
				dialog.css("width", width);
				dialog.css("left", left);
				dialog.css("top", 0);

				var dialogContent = dialog.find(".ui-dialog-content");
				var dialogContentCodeMirror = dialog.find(".CodeMirror");

				setTimeout(function(){
		            	dialogContent.css("height", height-90);
		            	dialogContentCodeMirror.css("height", "99%");
		            }, 20);

			}
		}

		function hideButtons()
		{
			$('#elfinder-modal .submitButton').hide();
		}

        function showAllowedButtons(allowed)
        {

            var modal = $('#elfinder-modal');
            allowed.useVersioning ? modal.find('.showHistory').show() : modal.find('.showHistory').hide() ;
        }

	</script>

	<!-- <div style="width:670px; float:left"> -->
		<div class="tabbable tabbable-custom tabbable-full-width">
		    <ul class="nav nav-tabs">
		        <li class="active">
		            <a onclick="elfinderTabClick('file')" style="cursor:pointer" data-toggle="tab">
		                <iwcm:text key="fbrowse.file"/>
		            </a>
		        </li>


		        <li>
		            <a onclick="elfinderTabClick('tools')" style="cursor:pointer" data-toggle="tab">
		                <iwcm:text key="editor_dir.tools"/>
		            </a>
		        </li>

		        <li class="last noperms-cmp_sync">
		            <a href="javascript:popup('/components/sync/export_setup.jsp', 650, 500);" style="cursor:pointer">
		                Export - Import
		            </a>
		        </li>

		    </ul>
			<div id="finder"><iwcm:text key="divpopup-blank.wait_please"/></div>
		</div>
	<!-- </div> -->


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
<%@ include file="/admin/layout_bottom.jsp" %>
