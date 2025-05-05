<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.io.*, sk.iway.iwcm.system.*, sk.iway.iwcm.i18n.*,sk.iway.iwcm.FileTools" %>
<%@ page import="sk.iway.iwcm.Tools" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<iwcm:checkLogon admin="true" perms="make_zip_archive"/>
<%
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);

	request.setAttribute("iconLink", "");
	request.setAttribute("dialogTitle", prop.getText("admin.archive.dialogTitle"));
	request.setAttribute("dialogDesc", prop.getText("admin.archive.dialogDesc")+ ".");

	if (Tools.getRequestParameter(request, "zipArchivePath") != null)
	   request.setAttribute("closeTable", "true");
%>

<jsp:include page="/admin/layout_top_dialog.jsp" />

<div class="padding10">

<%
	IwcmFile[] dirList = null;
	if (Tools.getRequestParameter(request, "zipArchivePath") != null)
	{
		String zipArchivePath = Tools.getRequestParameter(request, "zipArchivePath");
		String archiveDirs = Tools.getRequestParameter(request, "archiveDirs");

		//spustenie archivacie
		Archive.makeZipArchive(request, sk.iway.iwcm.Constants.getServletContext(), archiveDirs, zipArchivePath, out);
	}
	else
	{
		String realPath = sk.iway.iwcm.Tools.getRealPath("/");
		IwcmFile file = new IwcmFile(realPath);
		dirList = FileTools.sortFilesByName(file.listFiles());
	}
%>

<iwcm:present parameter="zipArchivePath" >
   <script type="text/javascript">
   		window.scrollBy(0,10000);
		function Ok()
		{
			window.close();
		}
	</script>
</iwcm:present>

<logic:notPresent parameter="zipArchivePath" >
	<script type="text/javascript">
		var archiveDirs = new Array();
		function Ok()
		{
			setDirs();
			document.pathForm.submit();
		}
	</script>

	<form name="pathForm" action="archive.jsp" method="get" onsubmit="javascript:setDirs();">
		<table>
			<tr>
				<td><label for="zipArchivePathId"><iwcm:text key="admin.archive_path"/>:</label></td>
				<td><input type="text" name="zipArchivePath" size="25" id="zipArchivePathId" />&nbsp;</td>
			</tr>
			<tr>
				<td><iwcm:text key="admin.archive_dirs"/>:</td>
				<td></td>
			</tr>
			<%
				if (dirList != null)
				{
					int index = 0;
					for (int i=0; i<dirList.length; i++)
					{
						if (dirList[i].isDirectory() && !dirList[i].getName().equalsIgnoreCase("cvs") && !dirList[i].getName().equalsIgnoreCase(".svn"))
						{
			%>
							<script type="text/javascript">
								archiveDirs[<%=index%>] = "<%=Tools.escapeHtml(dirList[i].getName())%>";
							</script>
							<tr>
								<td></td>
								<td><input type="checkbox" name="dir_<%=index%>" id="dir_<%=index%>Id" value="<%=Tools.escapeHtml(dirList[i].getName())%>"/>
								<label for="dir_<%=index%>Id"><%=Tools.escapeHtml(dirList[i].getName())%></label></td>
								<% index++; %>
							</tr>
			<%
						}
					}
				}
			%>
		</table>
		<input type="hidden" name="archiveDirs" />
	</form>

	<script type="text/javascript">
		document.pathForm.zipArchivePath.value = "/files/protected/backup/";
		var str = "";
		for (var i = 0; i < archiveDirs.length; i++)
			str += archiveDirs[i]+", ";

		function setDirs()
		{
			var dirs = "";
			for (var i=0; i<archiveDirs.length; i++)
			{
			   cb = eval("document.pathForm.dir_"+i);
				if (cb.checked)
				{
					if (dirs.length > 0)
						dirs += ","+cb.value;
					else
						dirs = cb.value;
				}
			}
			document.pathForm.archiveDirs.value = dirs;
			return (true);
		}
	</script>
</logic:notPresent>

</div>

<jsp:include page="/admin/layout_bottom_dialog.jsp" />
