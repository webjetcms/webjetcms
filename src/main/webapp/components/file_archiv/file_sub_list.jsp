<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%
	request.setAttribute("cmpName", "components.file_archiv.name");
	request.setAttribute("dialogTitleKey", "components.file_archiv.list_of_version");
	request.setAttribute("dialogDescKey", " ");
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<script>
window.resizeTo(900,600);
function Ok()
{
	window.close();
}
</script>
<%
int editFabId = Tools.getIntValue(Tools.getRequestParameter(request, "edit"), -1);
Prop prop = Prop.getInstance();
FileArchivatorBean fab = FileArchivatorDB.getInstance().getById(editFabId);
Identity user = UsersDB.getCurrentUser(request);
if(fab != null && (user == null || !user.isFolderWritable("/"+fab.getFilePath())))
{
    Logger.debug(null, "Pouzivatel "+((user != null)?"id: "+user.getUserId():"null")+" nema pravo na ZOBRAZENIE HISTORIE suboru: "+fab.getFilePath()+fab.getFileName()+" ");
    request.setAttribute("errorText",prop.getText("logon.err.unauthorized_group"));
    %><jsp:include page="/components/maybeError.jsp" /><%
    return;
}


List<FileArchivatorBean> fabList = FileArchivatorDB.getByReferenceId(editFabId);
request.setAttribute("fabList", fabList);
FileArchivatorBean rowFab;
%>
<style type="text/css">
.sort_table{width:900px;}
body {overflow:auto;}
</style>

<display:table class="sort_table" name="fabList" uid="row" defaultsort="12" defaultorder="descending">
<%
if(row != null)
{
	rowFab = (FileArchivatorBean)row;
	%>
	<display:column title="Id" ><a href="javascript:void(0);" onClick='popup("<iwcm:cp/>/components/file_archiv/file_archiv_edit.jsp?edit=<%=((FileArchivatorBean)row).getId()%>", 600, 800);'><%=((FileArchivatorBean)row).getId()%></a> </display:column>
	<display:column titleKey="components.file_archiv.virtualFileName"  > <a href="javascript:void(0);" onClick='popup("<iwcm:cp/>/components/file_archiv/file_archiv_edit.jsp?edit=<%=((FileArchivatorBean)row).getId()%>", 600, 800);'><%=((FileArchivatorBean)row).getVirtualFileName() %></a> </display:column>
	<display:column titleKey="components.file_archiv.real_name" property="fileName"/>
	<display:column titleKey="components.file_archiv.directory" style="max-width:120px;overflow:auto;" property="filePath"/>
	<display:column titleKey="components.file_archiv.valid_from_to" ><%
	if(rowFab.getValidFrom() != null)
		out.print(Tools.formatDate(rowFab.getValidFrom()));
	out.print("<br>-<br>");
	if(rowFab.getValidTo() != null)
		out.print(Tools.formatDate(rowFab.getValidTo()));
	%></display:column>
	<display:column titleKey="components.file_archiv.product" property="product"/>
	<display:column titleKey="components.bazar.category" property="category"/>
	<display:column titleKey="components.file_archiv.code" property="productCode"/>
	<display:column titleKey="editor.show" property="showFile" decorator="sk.iway.displaytag.BooleanDecorator"/>
	<display:column titleKey="components.file_archiv.priority" property="priority"/>
	<display:column titleKey="components.file_archiv.order" property="orderId"/>
	<%
	if(user.isEnabledItem("cmp_fileArchiv_advanced_settings"))
	{
	%>
		<display:column titleKey="components.file_archiv.advanced_settings"><a onClick='popup("<iwcm:cp/>/components/file_archiv/insert_between_archives.jsp?move_below_archiv_file=<%=rowFab.getId()%>", 500, 500);' href="javascript:void(0);">
		<iwcm:text key="components.file_archiv.save_after_this" param1="<%=String.valueOf(rowFab.getOrderId()+1)%>"/></a></display:column>
	<%
	}
}
%>
</display:table>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>