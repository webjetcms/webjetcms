<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
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
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<link rel="stylesheet" type="text/css" href="/admin/css/datepicker.css" />
<link href="/components/form/check_form.css" type="text/css" media="screen" rel="stylesheet">
<%
	request.setAttribute("cmpName", "components.file_archiv.name");
	request.setAttribute("dialogTitleKey", "components.file_archiv.edit_already_existing");
	request.setAttribute("dialogDescKey", "");
	 %>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="padding10">
<script src="/components/form/check_form.js" type="text/javascript"></script>
<script type="text/javascript">
	window.resizeTo(700,800);
</script>


<%
if(Constants.getBoolean("fileArchivCanEdit"))
{
	out.println(prop2.getText("components.file_archiv.edit_already_existing_set_conf_variable"));
	return;
}

Identity user = UsersDB.getCurrentUser(request);
if(user.isDisabledItem("components.file_archiv.advanced_settings"))
{
	out.println(prop2.getText("components.file_archiv.file_rename.nemate_pravo_na_tuto_editaciu"));
	return;
}

boolean rename = Tools.getBooleanValue(""+Tools.getRequestParameter(request, "rename"), false);
int edit = Tools.getIntValue(Tools.getRequestParameter(request, "edit"), -1);
FileArchivatorBean fab = FileArchivatorDB.getInstance().getById(edit);
if(!rename && fab == null || !FileTools.isFile(fab.getFilePath()+fab.getFileName()))
{
	out.print(prop2.getText("components.file_archiv.file_not_found"));
	return;
}

if(Tools.isNotEmpty(fab.getReferenceToMain()))
{
	out.print(prop2.getText("components.file_archiv.file_is_pattern_cannot_edit"));
	return;
}

boolean success = true;
if(rename)
{
	String newFileName = DB.internationalToEnglish(Tools.getParameter(request, "newFileName"));
	try
	{
		String pathFrom = fab.getFilePath()+fab.getFileName();
		String pathTo = fab.getFilePath()+newFileName;
		if(FileTools.isFile(pathTo))
		{
			success = false;
			out.print(prop2.getText("components.file_archiv.file_path_already_exist", pathTo));
			Logger.debug(null, "later_edit.jsp: "+"Take meno "+pathTo+" uz uxistuje");
		}

		if(success && FileTools.isFile(fab.getFilePath()+fab.getFileName()) && newFileName != null && newFileName.length() > 4 && fab.getFileName().endsWith(newFileName.substring(newFileName.length()-4)))
		{
			Logger.debug(null, "later_edit.jsp: "+" Ideme premenovat subor z "+pathFrom+" na "+pathTo);
			String oldFileName = fab.getFileName();
			if(Tools.renameFile(Tools.getRealPath(pathFrom), Tools.getRealPath(pathTo)))
			{
				//zmenime fab
				fab.setFileName(newFileName);
				success = fab.save();
				//najdeme vzor a zmenime linku aj jemu
				FileArchivatorBean fabPattern = FileArchivatorDB.getPatern(fab.getFilePath()+oldFileName);
				if(fabPattern != null && success)
				{
					fabPattern.setFieldB(pathTo);
					fabPattern.save();
				}

				out.print(prop2.getText("components.file_archiv.result_of_operation", success?"OK":"BAD"));
			}
			else
			{
				out.print(prop2.getText("components.file_archiv.file_rename_from_to_failed",pathFrom,pathTo));
				Logger.debug(null, "later_edit.jsp: "+"Subor sa nepodarilo premenovat z "+pathFrom+" na "+pathTo);
			}

		}
		else
		{
			out.print(prop2.getText("components.file_archiv.file_ext_not_same_or_file_not_found",pathFrom));
			Logger.debug(null, "later_edit.jsp: "+"Nesedi pripona suboru, alebo subor nebol najdeny: "+pathFrom);
		}
	}
	catch(Exception ex)
	{
		out.print(prop2.getText("components.file_archiv.exception_message",ex.getMessage().replaceAll("\\n","<br/>")));
		sk.iway.iwcm.Logger.error(ex);
	}
}
%>
<form name="rename" action="<%=PathFilter.getOrigPath(request)%>" method="post">
	<span><iwcm:text key="components.file_archiv.old_name"/>: <%=fab.getFileName() %></span> <br/>
	<label for="newNameInput"><iwcm:text key="components.file_archiv.new_name"/>: </label> <input id="newNameInput" type="text" name="newFileName"/>
	<input type="hidden" name="rename" value="true">
	<input type="hidden" name="edit" value="<%=edit%>" >
	<input type="submit" value="<%=prop2.getText("button.submit")%>">
</form>
</div>
<%@ include file="/admin/layout_bottom_dialog.jsp" %>
<script>
	$("#btnOk").hide();
</script>
