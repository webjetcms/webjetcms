<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.i18n.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
request.setAttribute("iconLink", "");
request.setAttribute("dialogTitle", prop.getText("components.import_web_pages.zip.dialogTitle"));
request.setAttribute("dialogDesc", prop.getText("components.import_web_pages.zip.dialogDesc"));

String groupId = Tools.getRequestParameter(request, "groupId");
if (groupId == null) groupId = (String)session.getAttribute(Constants.SESSION_GROUP_ID);
if (groupId==null) groupId = "";
%>
<%@page import="sk.iway.iwcm.Constants"%>

<%@page import="sk.iway.iwcm.components.importWebPages.ImportZipFileForm"%>
<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.common.DocTools"%><jsp:include page="/admin/layout_top_dialog.jsp" />
<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>
<script type="text/javascript">

	function openPopup()
	{
		var options = "toolbar=no,scrollbars=yes,resizable=yes,width=500,height=330";
		var link = "/admin/grouptree.jsp";

		popupwindow=window.open(link, "selectGroupID", options);
		if (window.focus)
		{
			popupwindow.focus();
		}
	}

 	function setParentGroupId(parentGroupString)
	{
		if(parentGroupString.length > 15)
		{
			document.importWebPagesForm.parentGroupIdString.value = parentGroupString.substr(0,15);
		}
		return 1;
	}

	function Ok()
	{
		document.importWebPagesForm.submit();
	}

	window.resizeTo(850, 450);

</script>

<logic:present parameter="error">
	<b><iwcm:text key="components.import_web_pages.import_error"/>.</b><br><br>
</logic:present>


<html:form action="/components/importWebPages.do" enctype="multipart/form-data">
<%
ImportZipFileForm form = (ImportZipFileForm)request.getAttribute("importWebPagesForm");
if (form != null)
{
	//urci podla adresara nejaku rozumnejsiu cestu
	GroupsDB groupsDB = GroupsDB.getInstance();
	GroupDetails actualGroup = groupsDB.getGroup(Tools.getIntValue(groupId, 0));
	System.out.println(groupId+"AAC"+actualGroup);
	if (actualGroup!=null)
	{
		String addPath = DocTools.removeCharsDir(actualGroup.getFullPath(), true).toLowerCase();
		//sproste ale jednoduche riesenie pre vub
		addPath = Tools.replace(addPath, "/vub-net/", "/vubnet/");
		form.setImagesRootDir("/images"+addPath+"/");
		form.setFilesRootDir("/files"+addPath+"/");
	}
}
%>
  <table>
  	 <tr>
  	 	<td>
  	 		<b><iwcm:text key="admin.archive_path"/>:</b>&nbsp;
  	 	</td>
  	 	<td>
  	 		<input type="File" name="uploadFile" size="30">
  	 	</td>
  	 </tr>
  	 <tr>
  	 	<td><b><iwcm:text key="components.import_web_pages.parent_group_id"/>:</b>&nbsp;</td>
  	 	<td><input type="text" name="parentGroupIdString" size="5" value="<%=groupId%>" />&nbsp;<input type="button" name="selGroup" value="<iwcm:text key="components.import_web_pages.select_group"/>" onclick="Javascript: openPopup();" /></td>
  	 </tr>
  	 <tr>
  	 	<td><b><iwcm:text key="components.import_web_pages.overwrite_files"/>:</b>&nbsp;</td>
  	 	<td><input type="checkbox" name="overwriteFiles" checked/>&nbsp;</td>
  	 </tr>
  	 <tr>
  	 	<td><b><iwcm:text key="components.import_web_pages.imagesRootDir"/>:</b>&nbsp;</td>
  	 	<td><html:text property="imagesRootDir" size="70" maxlength="255"/></td>
  	 </tr>
  	 <tr>
  	 	<td><b><iwcm:text key="components.import_web_pages.filesRootDir"/>:</b>&nbsp;</td>
  	 	<td><html:text property="filesRootDir" size="70" maxlength="255"/></td>
  	 </tr>
  </table>
</html:form>







<%@ include file="/admin/layout_bottom_dialog.jsp" %>
