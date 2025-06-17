<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.i18n.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%
Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
request.setAttribute("iconLink", "");
request.setAttribute("dialogTitle", prop.getText("components.import_web_pages.dialogTitle"));
request.setAttribute("dialogDesc", prop.getText("components.import_web_pages.dialogDesc"));

String groupId = Tools.getRequestParameter(request, "groupId");
if (groupId==null) groupId = "";

%>
<jsp:include page="/admin/layout_top_dialog.jsp" />
<%@page import="sk.iway.iwcm.Constants"%><%@page import="java.io.File"%>
<%@ page import="sk.iway.iwcm.Tools" %>
<%
if (sk.iway.iwcm.FileTools.exists("/components/sync/archive_upload.jsp")==false) {
	%>
	<style>
		.noperms-cmp_sync { display: none !important; }
	</style>
	<%
}
%>

<div class="padding10">
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>
<script type="text/javascript">

	window.helpLink = "/redactor/webpages/import-export";

	function Ok()
	{
	   var url = "";
		for (i=0; i<document.importWebPagesForm.length; i++)
		{
			if (document.importWebPagesForm[i].checked)
			{
			   url = document.importWebPagesForm[i].value;
			}
		}
		window.location.href=url;
	}

</script>

<style type="text/css">
form.importExportTable table tr td { padding-bottom: 16px; }
form.importExportTable h2 { padding-bottom: 4px; }
form.importExportTable h2 label { font-weight: bold; }
</style>

<form action="import_pages.jsp" name="importWebPagesForm" class="importExportTable">
  <table>
  	 <tr>
  	 	<td valign="top">
  	 		<input type="radio"  id="type2" name="type" value="import_xls_struct.jsp">
  	 	</td>
  	 	<td colspan="2" >
  	 		<h2><label for="type2"> <iwcm:text key="components.import_web_pages.xls.dialogTitle"/></label></h2>
  	 		<iwcm:text key="components.import_web_pages.xls.dialogDesc"/>
  	 	</td>
  	 </tr>
  	 <tr class="noperms-cmp_sync" style="display: none;">
  	 	<td valign="top">
  	 		<input type="radio" id="type4"  name="type" value="/components/sync/sync_dir.jsp?localGroupId=${param.groupId}&remoteGroupId=${param.groupId}">
  	 	</td>
  	 	<td colspan="2" >
  	 		<h2><label for="type4"><iwcm:text key="components.syncDir.import_from_remote_server"/></label></h2>
  	 		<iwcm:text key="components.syncDir.dialog_desc"/>
  	 	</td>
  	 </tr>
  	 <tr class="noperms-cmp_sync">
  	 	<td valign="top">
  	 		<input type="radio" id="type3" name="type" value="/components/sync/archive_upload.jsp?localGroupId=${param.groupId}&remoteGroupId=${param.groupId}">
  	 	</td>
  	 	<td colspan="2" >
  	 		<h2><label for="type3"><iwcm:text key="components.import_web_pages.xml.dialogTitle"/></label> </h2>
  	 		<iwcm:text key="components.import_web_pages.xml.dialogDesc"/>
  	 	</td>
  	 </tr>
  	 <tr class="noperms-cmp_sync">
  	 	<td valign="top">
  	 		<input type="radio" id="type5"name="type" value="/components/sync/download_archive.jsp?localGroupId=${param.groupId}&remoteGroupId=${param.groupId}&iframed=true">
  	 	</td>
  	 	<td colspan="2" >
  	 		<h2><label for="type5"><iwcm:text key="components.import_web_pages.export_xml.dialogTitle"/></label></h2>
  	 		<iwcm:text key="components.import_web_pages.export_xml.dialogDesc"/>
  	 	</td>
  	 </tr>
  	 <tr class="noperms-cmp_sync">
  	 	<td valign="top">
  	 		<input type="radio" id="type6"name="type" value="/components/sync/rollback_documents.jsp?localGroupId=${param.groupId}&remoteGroupId=${param.groupId}">
  	 	</td>
  	 	<td colspan="2" >
  	 		<h2><label for="type6"><iwcm:text key="components.import_web_pages.rollback.dialogTitle"/></label></h2>
  	 		<iwcm:text key="components.import_web_pages.rollback.dialogDesc"/>
  	 	</td>
  	 </tr>
  </table>
</form>

</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>
