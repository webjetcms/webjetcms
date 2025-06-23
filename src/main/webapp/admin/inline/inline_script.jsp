<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>
<%@ page import="sk.iway.iwcm.editor.InlineEditor" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (user == null || user.isAdmin() == false)
{
return;
}

String lng = Prop.getLng(request, false);
pageContext.setAttribute("lng", lng);

DocDetails doc = (DocDetails)request.getAttribute("docDetails");

//tento JS je priamo embednuty v stranke cez WriteTag

if (doc == null) return;

boolean isFullpageNews = false;
if (doc.getData().startsWith("!INCLUDE") && doc.getData().endsWith(")!") && doc.getData().indexOf("<p")==-1)
{
	if (doc.getData().indexOf("/news/")!=-1 || doc.getData().indexOf("/showdoc.jsp")!=-1)
	{
		//v stranke je len include news komponenty pre long page
		isFullpageNews = true;
		//System.out.println("setting wjEditorFullpageNews");
		request.setAttribute("wjEditorFullpageNews", "true");
	}
}


%>

<script type="text/javascript" src="/admin/webpages/webpages.js.jsp"></script>

<%
	boolean editorSource = false;
	if ("false".equals(request.getSession().getAttribute("combineEnabled"))) editorSource = true;

	if (editorSource) {
%>
<script type="text/javascript" src="/admin/skins/webjet8/ckeditor/ckeditor.js"></script>
<% } else { %>
<script type="text/javascript">
    window.CKEDITOR_BASEPATH = "/admin/skins/webjet8/ckeditor/dist/";
</script>
<script type="text/javascript" src="/admin/skins/webjet8/ckeditor/dist/ckeditor.js?t=f3e1"></script>
<% } %>
<script>
    var isFullpageNews = <%=isFullpageNews%>;
    var templateGroupId = ${ninja.temp.group.templatesGroupBean.templatesGroupId};
</script>
<script type="text/javascript" src="/admin/skins/webjet8/assets/global/plugins/jquery.cokie.min.js"></script>
<script type="text/javascript" src="/admin/inline/inline.js.jsp?language=<%=lng %>"></script>


<link href="/admin/skins/webjet8/ckeditor/dist/plugins/webjetcomponents/samples/contents.css" rel="stylesheet" />

<%
InlineEditor.EditingMode editingMode = InlineEditor.getEditingMode(request);
//System.out.println("editingMode="+editingMode);

if((editingMode == InlineEditor.EditingMode.newsInline && Constants.getBoolean("gridEditorEnabled")) || editingMode == InlineEditor.EditingMode.gridEditor)
{
%>
    <link href="/components/grideditor/support/font-awesome-4.7.0/css/font-awesome.min.css" rel="stylesheet" />
    <script src="/components/grideditor/cloud.grideditor.js.jsp"></script>
<% } else if (editingMode == InlineEditor.EditingMode.pageBuilder) { %>
    <!-- NINJA PAGE BUILDER -->

    <link rel="stylesheet" href="/admin/webpages/page-builder/style/style.css">
    <link rel="stylesheet" href="/admin/webpages/page-builder/style/jquery.minicolors.css">

    <script src="/admin/skins/webjet8/assets/global/plugins/jquery-ui/jquery-ui.js"></script>
    <script src="/admin/webpages/page-builder/scripts/jquery.minicolors.min.js"></script>
    <script src="/admin/webpages/page-builder/scripts/ninja-page-builder.js.jsp?language=<%=lng %>"></script>
    <script src="/admin/webpages/page-builder/scripts/pagesupport.js"></script>

    <!-- /NINJA PAGE BUILDER -->
    <style>
        p.text-right { text-align: right; }
        p.text-center { text-align: center; }
        p.text-justify { text-align: justify; }
    </style>
<% } %>