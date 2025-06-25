<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,java.util.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="modUpdate|users.edit_admins"/>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<h2>Delete OLD files</h2>

<%
if ("fix".equals(request.getParameter("act"))==false) {
	%><p><a href="?act=fix">Spustiť</a></p><%
}

List<String> filesToDelete = new ArrayList<String>();
filesToDelete.add("/admin/editor_functions.jsp");
filesToDelete.add("/admin/editor_functions2.jsp");
filesToDelete.add("/admin/editor_toolbar.jsp");
filesToDelete.add("/admin/images/mime/");
filesToDelete.add("/admin/helene/");

filesToDelete.add("/admin/scripts/tabs.vbs");
filesToDelete.add("/admin/scripts/tree.js");
filesToDelete.add("/admin/scripts/tree-dialog.js");
filesToDelete.add("/admin/scripts/toolbar.js");
filesToDelete.add("/admin/scripts/tables.js");
filesToDelete.add("/admin/scripts/rte.js");
filesToDelete.add("/admin/scripts/rte.1.js");
filesToDelete.add("/admin/scripts/groupstree.js");
filesToDelete.add("/admin/scripts/eventHandlers.js");
filesToDelete.add("/admin/scripts/dirsort.js");
filesToDelete.add("/admin/scripts/dhtmled.js");
filesToDelete.add("/admin/scripts/cookie.js");
filesToDelete.add("/admin/scripts/fckxhtml.js");
filesToDelete.add("/admin/scripts/dom-drag.js");
filesToDelete.add("/admin/scripts/dtree-devel.js");
filesToDelete.add("/admin/scripts/tabpane-editor.js");

filesToDelete.add("/admin/sizetest.jsp");
filesToDelete.add("/admin/pokus.html");
filesToDelete.add("/admin/editor_searchreplace.jsp");
filesToDelete.add("/admin/editor_previewimg.jsp");
filesToDelete.add("/admin/users-rep.jsp");
filesToDelete.add("/admin/updatepass.jsp");
filesToDelete.add("/admin/test_send_result.jsp");
filesToDelete.add("/admin/test_send.jsp");
filesToDelete.add("/admin/test_question.jsp");
filesToDelete.add("/admin/test_list.jsp");
//filesToDelete.add("/WEB-INF/classes/sk/iway/iwcm/doc/FileSyncAction.class"); //sposobi restart WJ, radsej kaslime na to
filesToDelete.add("/admin/sync.jsp");
filesToDelete.add("/admin/reqtest-multipart.jsp");
filesToDelete.add("/admin/reqtest.jsp");
filesToDelete.add("/admin/spellcheck_loader.jsp");
filesToDelete.add("/admin/spellcheck.jsp");
filesToDelete.add("/admin/editor_select_color.jsp");
filesToDelete.add("/admin/editor_selchar.jsp");
filesToDelete.add("/admin/update_doc_from_history.jsp");
filesToDelete.add("/admin/preview.jsp");
filesToDelete.add("/admin/editor_rowprop.jsp");
filesToDelete.add("/admin/editor_image.jsp");
filesToDelete.add("/admin/editor_cellprop.jsp");
filesToDelete.add("/admin/editor_iframe.jsp");
filesToDelete.add("/admin/editor_components.jsp");
filesToDelete.add("/admin/enviroment.jsp");
filesToDelete.add("/admin/url_search.jsp");
filesToDelete.add("/admin/editor.jsp");
filesToDelete.add("/admin/editor_menubar.jsp");
filesToDelete.add("/admin/editor_editor.jsp");
filesToDelete.add("/admin/conf_add_language.jsp");
//filesToDelete.add("/WEB-INF/classes/sk/iway/iwcm/catalog/");
filesToDelete.add("/admin/left.jsp");
filesToDelete.add("/admin/editor_create_table.jsp"); //1.4.2008
filesToDelete.add("/admin/groupslist-dtree.jsp");
filesToDelete.add("/admin/editor_uploadhtml.jsp");
filesToDelete.add("/admin/groupslist_render_normal.jsp");
filesToDelete.add("/admin/import_files.jsp");
filesToDelete.add("/admin/dbspeedtest.jsp");
filesToDelete.add("/admin/zip-dir2.jsp");
filesToDelete.add("/admin/editor_contextmenu.jsp");
filesToDelete.add("/admin/scripts/prototype.js");
filesToDelete.add("/fileatr/mime/");
filesToDelete.add("/components/htmlbox/insert.jsp");
//filesToDelete.add("/components/htmlbox/objects");
filesToDelete.add("/components/stat/stat_time.jsp");
filesToDelete.add("/admin/fbrowser_edit.jsp");
filesToDelete.add("/components/stat/docs_views.jsp");
filesToDelete.add("/components/stat/groups_views.jsp");
filesToDelete.add("/components/stat/banners_views.jsp");
filesToDelete.add("/components/stat/stat_top.jsp");
filesToDelete.add("/components/stat/stat_doc.jsp");

filesToDelete.add("/components/stat/admin_db_convert.jsp");
filesToDelete.add("/components/stat/admin_db_merge.jsp");

//TODO: editoricon.gif a asi aj menuicon.gif

boolean deleteFile = "fix".equals(request.getParameter("act"));

for (String link : filesToDelete)
{

	IwcmFile file = new IwcmFile(sk.iway.iwcm.Tools.getRealPath(link));

	if (file.exists() && deleteFile)
	{
		boolean delOk = false;
		if (file.isDirectory())
		{
			delOk = FileTools.deleteDirTree(file);
		}
		else
		{
			delOk = file.delete();
		}

		out.println("Checking file: "+link);
		out.println(" DELETE OK="+delOk);
		out.println("<br/>");
	}

}
%>

<p>Delete done.</p>

