<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.common.DocTools" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%=Tools.insertJQuery(request) %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<script type="text/javascript">
function makeAlert()
{
	return confirm("<iwcm:text key="components.file_archiv.rename_warning"/>");
}
</script>
<%!

private static boolean renameFile(String dirPath, String newfileName, FileArchivatorBean oldFileBean)
{
	//premenujeme meno poslednej verzie suboru na novo nahraty
	boolean renamed = true;
	//posledny subor si odlozime
	if(!Tools.renameFile(Tools.getRealPath(oldFileBean.getFilePath()+oldFileBean.getFileName()), Tools.getRealPath(dirPath+newfileName)))
		renamed = false;
	Logger.debug(FileArchivatorKit.class, "Rename from "+(oldFileBean.getFilePath()+oldFileBean.getFileName())+" to "+(Tools.getRealPath(dirPath+newfileName))+" result: "+renamed);

	return renamed;
}

private static void zmenOdkazNaHlavnySuborPriVzore(FileArchivatorBean fab, String oldFileName)
{
	FileArchivatorBean fabPatern =  FileArchivatorDB.getPatern(fab.getFilePath()+oldFileName);
	if(fabPatern != null)
	{
		fabPatern.setFieldB(fab.getFilePath()+fab.getFileName());
		fabPatern.save();
	}

}

%><%
sk.iway.iwcm.i18n.Prop prop = Prop.getInstance(request);
request.setAttribute("cmpName", null);
request.setAttribute("dialogTitleKey", prop.getText("components.file_archiv.dialog_heading"));
String adminlogText = "";
%>
<%@ include file="/admin/layout_top_dialog.jsp" %>
<div class="tab-pane toggle_content tab-pane-fullheight tab-pane-single" style="min-height: 350px;">
<%
if(!Constants.getBoolean("fileArchivCanEdit"))
{
	out.print(prop.getText("components.file_archiv.service"));
	return;
}
Identity user = UsersDB.getCurrentUser(request);
if(user.isDisabledItem("cmp_fileArchiv_edit_del_rollback"))
{
	//@Deprecated nechavame len kvoli historii
	if(Tools.isEmpty(Constants.getString("fileArchivCanEditUsers")) ||
			Tools.containsOneItem(Tools.getTokens(Constants.getString("fileArchivCanEditUsers"), ","), user.getLogin()) == false)
	{
		out.println(prop.getText("components.file_archiv.file_rename.nemate_pravo_na_tuto_editaciu"));
		return;
	}
}

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
boolean save = Tools.getRequestParameter(request, "save") != null && "true".equals(Tools.getRequestParameter(request, "save"));


int id = Tools.getIntValue(Tools.getRequestParameter(request, "edit")+"", -1);
if(id > -1)
{
	FileArchivatorBean fab =  FileArchivatorDB.getInstance().getById(id);
	if(fab == null)
	{
		out.println(prop.getText("components.file_archiv.file_not_found"));
		return;
	}
	else if(fab.getReferenceId() != -1)
	{
		out.println(prop.getText("components.file_archiv.edit_main_file"));
		return;
	}

	String oldFileName = fab.getFileName();
	String newFileName = Tools.getParameter(request, "file_name") + FileArchivatorKit.getFileExtension(fab.getFileName());
	newFileName = DocTools.removeChars(newFileName, false);
	newFileName = Tools.replace(newFileName, " ", "");

	if(save && !FileTools.isFile(fab.getFilePath()+newFileName))
	{
		//ukladame
		if(renameFile(fab.getFilePath(),newFileName , fab))
		{
			fab.setFileName(newFileName);
			zmenOdkazNaHlavnySuborPriVzore(fab, oldFileName);
			if(fab.save())
			{
				adminlogText = prop.getText("components.file_archiv.rename_successfull");
				out.println(adminlogText);
				%><script type="text/javascript">
					setTimeout(function(){
						window.opener.location.href = "/components/file_archiv/file_list.jsp";
						window.close();
					}, 2000);
				</script><%
				Adminlog.add(10110, user.getUserId(), prop.getText("components.file_archiv.rename_from") + ""+fab.getFilePath()+oldFileName+" na "+fab.getFilePath()+newFileName+" status: "+adminlogText, -1, -1);
				return;
			}
			else
			{
				adminlogText = prop.getText("components.file_archiv.error_db");
				out.println(adminlogText);
			}
		}
		adminlogText += prop.getText("components.file_archiv.error_disc");
		out.println(adminlogText);
		Adminlog.add(10110, user.getUserId(), "Premenovanie súboru z "+fab.getFilePath()+oldFileName+" na "+fab.getFilePath()+newFileName+" status: "+adminlogText, -1, -1);
	}
	else
	{
		%><div style="padding:15px;"><%
			if(FileTools.isFile(fab.getFilePath()+newFileName))
			{
				%> <p style="color:red;"><iwcm:text key="components.file_archiv.error_exist"/></p><%
			}
			%>
			<p><iwcm:text key="components.file_archiv.rename_file"/>: <%=fab.getFileName() %></p>
			<form name="rename_form" method="POST">
				<div class="row">
					<div class="col-xs-4">
						<p><label for="file_name"><iwcm:text key="components.file_archiv.new_name"/>:</label></p>
					</div>
					<div class="col-xs-8">
						<p><input type="text" class="form-control" autocomplete="off" id="file_name" name="file_name" size="<%=fab.getFileName().length() %>" value="<%=fab.getFileName().substring(0,fab.getFileName().lastIndexOf("."))%>"></p>
					</div>
				</div>

				<input type="hidden" name="save" value="true">
				<input type="submit" class="button" id="saveLogoForm" onclick="return makeAlert()" name="saveLogo" value="<iwcm:text key="components.file_archiv.rename"/>" />
			</form>
		</div>
		<%
	}
}

%>
</div>
