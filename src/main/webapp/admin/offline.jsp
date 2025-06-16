<%@page import="java.util.List"%><% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="java.io.*, sk.iway.iwcm.*, sk.iway.iwcm.i18n.*, sk.iway.iwcm.stat.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<iwcm:checkLogon admin="true" perms="export_offline"/>

<%
	Prop prop = Prop.getInstance(sk.iway.iwcm.Constants.getServletContext(), request);
	request.setAttribute("iconLink", "");
	request.setAttribute("dialogTitle", prop.getText("admin.offline.dialogTitle"));
	request.setAttribute("dialogDesc", prop.getText("admin.offline.dialogDesc")+ ".");
%>

<jsp:include page="/admin/layout_top_dialog.jsp" />

<%
	File[] dirList = null;
	String realPath = sk.iway.iwcm.Tools.getRealPath("/");
	//if (Tools.isNotEmpty(PathFilter.getCustomPath()))
	//	realPath = PathFilter.getCustomPath() + File.separatorChar + Constants.getInstallName() + File.separatorChar;

	File file = new File(realPath);
	dirList = file.listFiles();

	int groupId = -1;
	int destGroupId = -1;

	if (session.getAttribute(StatDB.SESSION_GROUP_ID)!=null)
		groupId = ((Integer)session.getAttribute(StatDB.SESSION_GROUP_ID)).intValue();

	request.setAttribute("groupId","" + groupId);
%>

<script type="text/javascript" src="<%=request.getContextPath()%>/admin/scripts/modalDialog.js"></script>
<script type="text/javascript" src="/admin/scripts/modalDialog.js"></script>

<script type="text/javascript">
<!--
	helpLink = "admin/offline.jsp";

	function Ok()
	{
		setDirs();
		document.pathForm.submit();
	}

	function setParentGroupId(returnValue)
	{
		if(returnValue.length > 15)
		{
			var groupid = returnValue.substr(0,15);
			var groupname = returnValue.substr(15);
			groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
			document.pathForm.groupId.value = groupid;
		}
	}

//-->
</script>

<div class="padding10">

<iwcm:text key="admin.offline.desc"/>. <br /><br />

<iwcm:notPresent parameter="zipArchivePath">
	<script type="text/javascript">
		var archiveDirs = new Array();
		function makeZipArchiveClick(cb)
		{
			if (cb.checked)
				document.getElementById("dirSelectTable").style.display="block";
			else
				document.getElementById("dirSelectTable").style.display="none";
		}
	</script>

	<form name="pathForm" action="/admin/offline.do" method="get" onsubmit="javascript:setDirs();" ><%=sk.iway.iwcm.tags.support.FormTag.renderToken(session)%>
		<table>
			<tr>
				<td><label for="groupId1"><iwcm:text key="stat_settings.group_id"/>:</label></td>
				<td>
					<div class="input-group">
						<input type="text" class="form-control" size=5 maxlength=5 name="groupId" value="<%=(""+groupId)%>" id="groupId1" />
						<div class="input-group-append">
							<button type="button" name="groupSelect" id="groupSelectBtn" onclick='popup("/admin/grouptree.jsp?fcnName=setParentGroupId", 300, 450);' class="btn btn-outline-secondary"><i class="ti ti-focus-2"></i></button>
						</div>
					</div>
				</td>
			</tr>
			<tr>
				<td><label for="groupId2"><iwcm:text key="stat_settings.group_id2"/>:</label></td>
				<td>
					<input class="form-control" type="text" size=10 maxlength=25 name="destination" value="/html"" id="groupId2" />
				</td>
			</tr>
			<tr>
				<td><label for="makeZipArchiveId"><iwcm:text key="admin.make_zip_archive"/></label></td>
				<td><input type="checkbox" name="makeZipArchive" id="makeZipArchiveId" value="yes" onclick="makeZipArchiveClick(this)"; /></td>
			</tr>
			<tr id="dirSelectTable" style="display:none">
		 		<td><iwcm:text key="admin.offline.choose_folders"/>:
			 		<table>
			 			<%
			 				if (dirList != null)
				  	   		{
				  	   			int index = 0;
				  	 			for (int i=0; i<dirList.length; i++)
				  	 			{
				  	 				String checkedNames=",css,files,flash,images,jscripts,";
				  	 				if (dirList[i].isDirectory() && !dirList[i].getName().equalsIgnoreCase("cvs"))
				  	 				{
				  	 	%>
						  	 			<script type="text/javascript">
											archiveDirs[<%=index%>] = "<%=Tools.escapeHtml(dirList[i].getName())%>";
										</script>
							  	 		<tr>
								  	 		<td><label for="dir_<%=index%>Id">&nbsp;&nbsp;&nbsp;<%=Tools.escapeHtml(dirList[i].getName())%></label></td>
								  	 		<td>
								  	 			<input type="checkbox" name="dir_<%=index%>" id="dir_<%=index++%>Id" value="<%=Tools.escapeHtml(dirList[i].getName())%>" <%if (checkedNames.indexOf(","+dirList[i].getName()+",")!=-1) out.print(" checked=\"checked\"");%> />
								  	 		</td>
										</tr>
				  		<%			}
				  	 			}
				  	  		}
				  	  	%>
			 		</table>
		 		</td>
			</tr>
		</table>
		<input type="hidden" name="archiveDirs"/>
	</form>

	<script type="text/javascript">
		var str = "";
		for (var i=0; i<archiveDirs.length; i++ )
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
</iwcm:notPresent>

<script type="text/javascript">
	window.scrollBy(0,10000);
</script>

</div>

<%@ include file="/admin/layout_bottom_dialog.jsp" %>
