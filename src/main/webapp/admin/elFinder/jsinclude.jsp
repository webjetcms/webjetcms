<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%!
List<IwcmFile> getFiles(String path) {
	List<IwcmFile> list = new ArrayList<IwcmFile>();
	IwcmFile dir = new IwcmFile(Tools.getRealPath(path));
	if (dir.exists())
	{
		IwcmFile[] filesInDir = FileTools.sortFilesByName(dir.listFiles());
   	for (IwcmFile file : filesInDir)
   	{
   		if (file.isFile()) list.add(file);
	   }
	}

	return list;
}
%><logic:notPresent name="dontCheckAdmin"><iwcm:checkLogon admin="true"/></logic:notPresent><logic:present name="dontCheckAdmin"><iwcm:checkLogon /></logic:present>
<%
request.setAttribute("elfinderCss", getFiles("/admin/elFinder/css/"));
request.setAttribute("elfinderUi", getFiles("/admin/elFinder/js/ui/"));
request.setAttribute("elfinderCommands", getFiles("/admin/elFinder/js/commands/"));
%>
<link rel="stylesheet" href="/admin/elFinder/jquery/jquery-ui-1.12.0.css" type="text/css">

	<c:forEach items="${elfinderCss}" var="item">
		<link rel="stylesheet" href="${item.virtualPath}"    type="text/css">
	</c:forEach>
	<%--
	<link rel="stylesheet" href="/admin/elFinder/css/commands.css"    type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/common.css"      type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/contextmenu.css" type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/cwd.css"         type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/dialog.css"      type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/fonts.css"       type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/navbar.css"      type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/places.css"      type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/quicklook.css"   type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/statusbar.css"   type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/theme.css"       type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/toast.css"       type="text/css">
	<link rel="stylesheet" href="/admin/elFinder/css/toolbar.css"     type="text/css">
	--%>

	<!-- elfinder core -->
	<script src="/admin/elFinder/js/elFinder.js"></script>
	<script src="/admin/elFinder/js/elFinder.version.js"></script>
	<script src="/admin/elFinder/js/jquery.elfinder.js"></script>
	<script src="/admin/elFinder/js/elFinder.options.js"></script>
	<script src="/admin/elFinder/js/elFinder.options.netmount.js"></script>
	<script src="/admin/elFinder/js/elFinder.history.js"></script>
	<script src="/admin/elFinder/js/elFinder.command.js"></script>
	<script src="/admin/elFinder/js/elFinder.resources.js"></script>

	<!-- elfinder dialog -->
	<script src="/admin/elFinder/js/jquery.dialogelfinder.js"></script>

	<!-- elfinder ui -->
	<c:forEach items="${elfinderUi}" var="item">
		<script src="${item.virtualPath}"></script>
	</c:forEach>

	<%--
	<script src="/admin/elFinder/js/ui/button.js"></script>
	<script src="/admin/elFinder/js/ui/contextmenu.js"></script>
	<script src="/admin/elFinder/js/ui/cwd.js"></script>
	<script src="/admin/elFinder/js/ui/dialog.js"></script>
	<script src="/admin/elFinder/js/ui/fullscreenbutton.js"></script>
	<script src="/admin/elFinder/js/ui/mkdirbutton.js"></script>
	<script src="/admin/elFinder/js/ui/navbar.js"></script>
	<script src="/admin/elFinder/js/ui/overlay.js"></script>
	<script src="/admin/elFinder/js/ui/panel.js"></script>
	<script src="/admin/elFinder/js/ui/path.js"></script>
	<script src="/admin/elFinder/js/ui/places.js"></script>
	<script src="/admin/elFinder/js/ui/searchbutton.js"></script>
	<script src="/admin/elFinder/js/ui/sortbutton.js"></script>
	<script src="/admin/elFinder/js/ui/stat.js"></script>
	<script src="/admin/elFinder/js/ui/toast.js"></script>
	<script src="/admin/elFinder/js/ui/toolbar.js"></script>
	<script src="/admin/elFinder/js/ui/tree.js"></script>
	<script src="/admin/elFinder/js/ui/uploadButton.js"></script>
	<script src="/admin/elFinder/js/ui/viewbutton.js"></script>
	<script src="/admin/elFinder/js/ui/workzone.js"></script>
	--%>

	<!-- elfinder commands -->
	<c:forEach items="${elfinderCommands}" var="item">
		<script src="${item.virtualPath}"></script>
	</c:forEach>

	<!-- elfinder languages -->
	<script src="/admin/elFinder/js/i18n/elfinder.cs.js.jsp" charset="UTF-8"></script>
	<script src="/admin/elFinder/js/i18n/elfinder.de.js"></script>
	<script src="/admin/elFinder/js/i18n/elfinder.en.js"></script>
	<script src="/admin/elFinder/js/i18n/elfinder.sk.js.jsp" charset="UTF-8"></script>

	<!-- elfinder 1.x connector API support
	<script src="/admin/elFinder/js/proxy/elFinderSupportVer1.js"></script>
	-->


<% if (Constants.getBoolean("fbrowserShowOnlyWritableFolders") && Tools.isEmpty(Constants.getString("fbrowserAlwaysShowFolders"))) { %>
<style>
	.elfinder-perms {display: none;}
</style>
<% } %>

	<style type="text/css">
		#finder .elfinder-button-icon, #finder .elfinder-button-icon { background-image: url('/admin/elFinder/img/toolbar.png'); }
		#finder .elfinder-lock, #finder .elfinder-perms, #finder .elfinder-symlink { background-image: url('/admin/elFinder/img/toolbar-webjet.png'); }
		#finder .elfinder-cwd-view-list td .elfinder-cwd-icon { background-image: url('/admin/elFinder/img/icons-small-webjet.png'); }
		#finder .elfinder-cwd-icon { background-image: url('/admin/elFinder/img/icons-big-webjet.png'); }
		#finder .elfinder-navbar-icon { background-image: url('/admin/elFinder/img/toolbar-webjet.png'); }
		div.elfinder-toolbar { height: 60px; }
		div.elfinder-buttonset { border: 0px; height: 50px; padding-top: 14px; border-right: 1px solid #ccc; padding-right: 12px; margin-right: 12px; border-radius: 0px; }
		#finder .ui-state-default { background: none; }
		#finder .ui-widget-header { background: none; border: 0px; border-radius: 0px; border-bottom: 2px solid e5e5e5; background-color: #f5f5f5; }
		#finder { border: 0px; }
		#finder .elfinder-navbar { background: none; background-color: white; border-right: 1px solid #e5eef9;}
		#finder .elfinder-statusbar { background: none; background-color: #f5f5f5; border-top: 1px solid #e5eef9;}
		#finder .elfinder-toolbar-button-separator  { padding-left: 8px; margin-left: 8px; border: 0px; background: none; }
		#finder .elfinder-navbar .ui-state-hover { border-color: #ffd600; background: none; background-color: #ffd600; color: black; }
		#finder input { color: black; }
		.elfinder .elfinder-button form input { height: 50px; }
		.elfinder-button:first-child, .elfinder-button:last-child { border-radius: 0px; }

		.elfinder .elfinder-navbar { font-size: 12px; }
		#finder .elfinder-navbar .ui-state-active { border-color: #ffd600; background-color: #ffd600; color: #555555; }

		.elfinder-buttonset br { line-height: 24px; }

		.elfinder-ltr .elfinder-path { display: none; }

		/* nastylovanie editacie obrazka */
		#finder div.ui-corner-all, #finder div.ui-dialog-buttonset button { border-top-left-radius: 0px; border-top-right-radius: 0px; border-bottom-left-radius: 0px; border-bottom-right-radius: 0px; }
		#finder div.ui-dialog-buttonset button { background-color: #29c01a; color: #fff; padding: 2px; border: 0px; font-size: 11px; }
		#finder div.ui-dialog-buttonset button:first-child { background-color: #7d7d7d; }
		div.elfinder-dialog  { z-index: 9999 !important; }

		.elfinder-button-icon-fileopen {
		    background-position: 0 -529px;
		}
		.elfinder-button-icon-fileprops {
		    background-position: 0 -673px;
		}
		.elfinder-button-icon-wjmetadata {
		    background-position: 0 -63px;
		}

		iframe.iframe {width: 100%; height: 350px; border: 0 none;}
		.modal-header, .modal-body { padding: 5px; }

		#elfinder-finder-cwd-thead { background-color: white; }

	</style>

	<script type="text/javascript">

		//kompatibilita s novym jQuery, po aktualizacii elfindera sa moze zrusit
		var rxhtmlTag = /<(?!area|br|col|embed|hr|img|input|link|meta|param)(([\w:-]+)[^>]*)\/>/gi;
		jQuery.extend( {
			htmlPrefilter: function( html ) {
				var fixedHtml = html.replace( rxhtmlTag, "<$1></$2>" );
				//if (html != fixedHtml) console.log("htmlPrefilter: ", html, "fixed=", fixedHtml);
				//else console.log("SAME: ", html);
				return fixedHtml;
			}
		});

		var elFinderInitialized = false;

	   //upravi tlacitka na toolbare podla webjet8 grafiky
		function changeElfinderIconToWj(iconName, wjIconName, hideFirstSeparator, replaceThirdToBr)
		{
			var copyIcon = $(iconName);
			var copyIconDiv = copyIcon.parent();

			copyIconDiv.addClass("wjIconBigSize");
			copyIcon.addClass("wjIconBig "+wjIconName);

			if ("wjIconBig-forward" == wjIconName) copyIconDiv.addClass("wjIconBig-short");

			var iconButtonset = copyIconDiv.closest('.elfinder-buttonset');

			//console.log(copyIconDiv);
			//console.log(iconButtonset);

			iconButtonset.css("padding-top", "0px");

			if (hideFirstSeparator) iconButtonset.children().eq(1).hide();
			if (replaceThirdToBr) iconButtonset.children().eq(3)[0].outerHTML="<br/>";

			return iconButtonset
		}

		function elfinderToolbarFix()
		{
			changeElfinderIconToWj(".elfinder-button-icon-paste", "wjIconBig-paste", true, true);
			changeElfinderIconToWj(".elfinder-button-icon-back", "wjIconBig-back", true, false);
			changeElfinderIconToWj(".elfinder-button-icon-forward", "wjIconBig-forward", false, false);
			<%
			String uploadIcon = "wjIconBig-upload";
			if (request.getAttribute("uploadIcon")!=null) uploadIcon = (String)request.getAttribute("uploadIcon");
			%>
			changeElfinderIconToWj(".elfinder-button-icon-upload", "<%=uploadIcon%>", true, true);
		}

		function elfinderTabClick(name)
		{
			//window.alert(name);
			var elfToolbar = $(".elfinder-toolbar div.elfinder-buttonset");

			var i = 0;
			for (i=0; i<elfToolbar.length; i++)
			{
				$(elfToolbar[i]).hide();
			}

			if ("file"==name)
			{
				$("#finder").show();
				$("#pixabay").hide();

				$(elfToolbar[0]).show();
				$(elfToolbar[1]).show();
				$(elfToolbar[2]).show();
				$(elfToolbar[3]).show();
			}
			else if ("tools"==name)
			{
				$("#finder").show();
				$("#pixabay").hide();

				$(elfToolbar[4]).show();
				$(elfToolbar[5]).show();
				$(elfToolbar[6]).show();
				$(elfToolbar[7]).show();
				$(elfToolbar[8]).show();
				$(elfToolbar[9]).show();
			}
			else if ("pixabay"==name)
			{
				$("#finder").hide();
				$("#pixabay").show();
			}
		}

		$(document).ready(function()
		{
			setTimeout(initializeElfinder, 100);
		});

        function openDefaultImageFolder(refreshData)
        {
            var elFinderInstance = $('#finder').elfinder('instance');

			if (refreshData)
			{
                $($("span.elfinder-navbar-dir")[0]).trigger("click");
                setTimeout(function() {
                    elFinderInstance.sync().done(function () {
                        setTimeout(function() { clickDefaultImageFolder(); }, 100);
                    });
                }, 100);
            }
			else clickDefaultImageFolder();
        }

        //private funkcia
        function clickDefaultImageFolder()
		{
		    //console.log(window.location.href);
		    if (window.location.href.indexOf("wj_link")!=-1)
			{
                //console.log("Clicked");
                //$($("span.elfinder-navbar-dir")[2]).trigger("click");
                openElfinderInFolder("/files:default");
			}
			else
			{
				//console.log("Clicked");
				//$($("span.elfinder-navbar-dir")[1]).trigger("click");
                openElfinderInFolder("/images:default");
            }
		}

        function openElfinderInFolder(url) {
            $.ajax({
                method: "POST",
                url: "/admin/elFinder/gethash.jsp",
                data: {url: url, docId: lastDocId, groupId: lastGroupId}
            })
                .done(function (msg) {
                    //console.log(msg);
                    elFinderInstance.exec("open", msg.volume).done(function () {
                        //console.log("open root");
                        setTimeout(function () {
                            elFinderInstance.request({
                                data: {cmd: 'parents', target: msg.hashParent},
                                preventFail: true
                            })
                                .done(function (data) {
                                    //console.log("DONE parents");
                                    //console.log(data);
                                    setTimeout(function () {
                                        //console.log("Opening parent="+msg.hashParent);
                                        elFinderInstance.exec('open', msg.hashParent).done(function () {
                                            //console.log("open parent");
                                            setTimeout(function () {
                                                try {
                                                    //console.log("select file " + msg.hash);
                                                    elFinderInstance.selectfiles({files: [msg.hash]})
                                                } catch (e) {
                                                }
                                            }, 300);

                                        });
                                    }, 100);
                                });
                        }, 10);
                    });
                });
        }


		$(function(){
			$('#elfinder-modal button.submit').click(function(){
                var action = $('#elfinder-modal .action').is(":visible") ? $('#elfinder-modal .action').val() : null
				    iframe = $('#elfinderIframe')[0].contentWindow;

                if (action != null) {
                    if ($('#elfinderIframe').attr('src').indexOf('/admin/fbrowser/fileprop/') != -1) {
                        switch(action) {
        					case "bSubmit":
        						iframe.submitForm();
        						//$("#elfinderFileIframe").contents().find('form#fbrowserFileEditForm').submit();
        						break;

        					case "bReindex":
        						iframe.reindexFile();
        						//iframe.reindexFile('<%=Tools.getRequestParameter(request, "file")%>');
        						break;

        					case "bUsage":
        						iframe.viewUsage();
        						break;

        					case "showHistory":
                                iframe.showHistory();
        						//window.location.href='/admin/file_history.jsp?file=<%=Tools.getRequestParameter(request, "dir")+"/"+Tools.getRequestParameter(request, "file") %>';
        						break;

        					default:
        						break;
        				}
                    }
                    else {
        				switch(action) {
        					case "bReindex":
        						iframe.reindexDir();
        						break;

							case "bUsage":
        						iframe.viewUsage();
        						break;

							case "bSubmit":
        						$("#elfinderIframe").contents().find('form#fbrowserEditForm').submit();
        						break;

        					default:
        						break;
        				}
                    }
                }
                else {
                    if (typeof iframe.Ok != "undefined") {
                        iframe.Ok();
                    }
                }
			});
		});

		//vola sa po nastaveni metadat
		function fbrowserDone(reload)
		{
            if (typeof reload == "undefined") {
                reload = true;
            }

			if (typeof elFinderInstance != "undefined") {
                if (reload) {
                    // reloadujem dva krat, lebo po prvom reloade sa v elfindri bugne oznacenie suborov
				    elFinderInstance.exec('reload');
                    setTimeout(function(){
                        elFinderInstance.exec('reload');
                    }, 800);
                }
				$('#elfinder-modal').modal('hide');
			}
			else {
				window.location.reload();
			}
		}

		function processEventReload(event, elfinderInstance) {
			try {
				//console.log("event=", event, "elfinderInstance=", elfinderInstance);
				if (event.data.forceReloadTree === true) {
					//console.log("processing reload");
					setTimeout(function() {
						//console.log("RELOADING");
						elfinderInstance.exec('reload');
					}, 2000);
				}
			} catch (e) {console.log(e);}
		}
	</script>
