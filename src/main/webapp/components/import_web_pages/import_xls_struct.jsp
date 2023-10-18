<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.i18n.*,sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
request.setAttribute("iconLink", "");
request.setAttribute("dialogTitle", prop.getText("components.import_web_pages.xls.dialogTitle"));
request.setAttribute("dialogDesc", prop.getText("components.import_web_pages.xls.dialogDesc"));

int groupId = Constants.getInt("rootGroupId");
try
{
	if (Tools.getRequestParameter(request, "groupid") != null)
	{
		groupId = Integer.parseInt(Tools.getRequestParameter(request, "groupid"));
	}
	else
	{
		//skus ziskat data zo session
		if (request.getSession().getAttribute(Constants.SESSION_GROUP_ID) != null)
		{
			groupId = Integer.parseInt((String) request.getSession().getAttribute(Constants.SESSION_GROUP_ID));
		}

	}
}
catch (Exception ex)
{
}

%>
<jsp:include page="/admin/layout_top_dialog.jsp" />
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
			document.xlsImportForm.parentGroupIdString.value = parentGroupString.substr(0,15);
		}
		return 1;
	}

	function Ok()
	{
		document.xlsImportForm.submit();
	}

   function downloadDataClick(checkbox)
   {
      if (checkbox.checked)
      {

      }
      else
      {

      }
      showHideElement("cropHtmlStart", checkbox.checked);
      showHideElement("cropHtmlEnd", checkbox.checked);
   }
</script>

<div class="padding10">
	<logic:present parameter="error">
		<b><iwcm:text key="components.import_web_pages.import_error"/>.</b><br><br>
	</logic:present>


	<html:form action="/admin/importxls.do" name="xlsImportForm" type="sk.iway.iwcm.xls.ImportXLSForm" enctype="multipart/form-data">
	<table>
		<tr>
			<td>
				<b><iwcm:text key="components.import_web_pages.xls.xls_file"/>:</b>&nbsp;
			</td>
			<td colspan="2" >
				<input type="File" name="file" size="30">
			</td>
		</tr>
		<tr>
			<td><b><iwcm:text key="components.import_web_pages.parent_group_id"/>:</b>&nbsp;</td>
			<td><input type="text" name="parentGroupIdString" size="5" value="<%=groupId%>" />&nbsp;
				<input type="button" name="selGroup" value="<iwcm:text key="components.import_web_pages.select_group"/>" onclick="Javascript: openPopup();" />&nbsp;
			</td>
		</tr>
		<tr>
			<td><b><iwcm:text key="components.import_web_pages.xls.level_as_priority"/>:</b>&nbsp;</td>
			<td><input type="checkbox" name="levelAsPriority" checked value="true"/>&nbsp;</td>
		</tr>
		<tr>
			<td><b><iwcm:text key="components.import_web_pages.xls.create_empty_pages"/>:</b>&nbsp;</td>
			<td><input type="checkbox" name="createPages" checked value="yes"/>&nbsp;</td>
		</tr>
		<tr>
			<td><b><iwcm:text key="components.import_web_pages.xls.download_from_server"/>:</b>&nbsp;</td>
			<td><input type="checkbox" name="downloadData" value="yes" onClick="downloadDataClick(this);"/>&nbsp;</td>
		</tr>
		<tr id='cropHtmlStart' style='display:none'>
			<td align='right' valign='top'><iwcm:text key="components.import_web_pages.xls.crop_html_start"/>&nbsp;</td>
			<td><textarea name="cropHtmlStart" rows="5" cols="40" class="input"></textarea></td>
		</tr>
		<tr id='cropHtmlEnd' style='display:none'>
			<td align='right' valign='top'><iwcm:text key="components.import_web_pages.xls.crop_html_end"/>&nbsp;</td>
			<td><textarea name="cropHtmlEnd" rows="5" cols="40" class="input"></textarea></td>
		</tr>
	</table>
	<input type="hidden" name="type" value="sk.iway.iwcm.components.importWebPages.ImportStructureExcel">
	</html:form>
</div>







<%@ include file="/admin/layout_bottom_dialog.jsp" %>
