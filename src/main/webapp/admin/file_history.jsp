<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.io.FileHistoryBean"%>
<%@page import="sk.iway.iwcm.io.FileHistoryDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.filebrowser.*,java.util.*,sk.iway.iwcm.FileTools,sk.iway.iwcm.stat.Column" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ include file="/admin/layout_top_popup.jsp" %><%!
String format(long size)
{
String length="";
DecimalFormat decimalFormat=  new DecimalFormat("0.##");
if (size > (1024 * 1024))
{
   length = decimalFormat.format(size / (1024 * 1024))+ "&nbsp;<span class='lenMB'>MB</span>";
}
else if (size > 1024)
{
   length = decimalFormat.format(size / 1024)+ "&nbsp;<span class='lenKB'>kB</span>";
}
else
{
   length = decimalFormat.format(size)+ "&nbsp;<span class='lenB'>B</span>";
}
return length;
}
%>


<%@page import="sk.iway.iwcm.io.IwcmFsDB"%>
<%@page import="sk.iway.iwcm.sync.FileBean"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DecimalFormat"%>
<%@page import="sk.iway.iwcm.Tools"%><script language="JavaScript">
<logic:present parameter="refresh">
if (window.parent)
{
   window.parent.location.href="/admin/fbrowser.browse.do?rnd=<%=sk.iway.iwcm.Tools.getNow()%>";
}
</logic:present>
function reindexFile(fileName)
{
   document.reindexForm.file.value = document.fbrowserEditForm.dir.value + "/" + fileName;
   document.reindexForm.submit();
}

function viewUsage()
{
  window.location.href="/admin/fbrowser/fileprop/?prop=yes&usage=true&dir=<%=Tools.getRequestParameter(request, "dir")%>&file=<%=Tools.getRequestParameter(request, "file")%>";
}

function zmen()
{
  if ( document.all.fbrowserEditForm.changeName.checked == true )
		{
		  document.all.fbrowserEditForm.zmenNazov.value = 1;
		}
}

function openFileHistory(dirName, fileName)
{
  	popup("/admin/fbrowser/fileprop/?dir="+dirName+"&file="+fileName, 700, 500);
}

function editFileHistory(dirName, fileName)
{
  	popup("/admin/fbrowser/fileprop/?dir="+dirName+"&file="+fileName, 700, 500);
}

</script>


<link rel="stylesheet" type="text/css" href="css/editor.css" />

<style>
body
{
   padding: 0px;
   margin: 0px;
}
tr.fhistoryRowfalse td { font-weight: bold; }
</style>
<table border="0" width="100%" cellpadding=0 cellspacing=0 class="tableclass" style="padding: 0px; margin: 0px;">
      <tr class='tab-pane'>
         <td valign="top">
   <%
   	String virtualPath = Tools.getRequestParameter(request, "file");
   	Prop prop = Prop.getInstance();
   	String currentDir = virtualPath.substring(0, virtualPath.lastIndexOf("/"));
   	String fileName = virtualPath.substring(virtualPath.lastIndexOf("/")+1, virtualPath.length());
   	//normalizacia cesty aby to bolo mozne spojit so suborom
   	String currentDirForFile = currentDir;
   	if (currentDirForFile.endsWith("/")==false) currentDirForFile = currentDirForFile+"/";
   	if (currentDirForFile.startsWith("/")==false) currentDirForFile = "/" + currentDirForFile;
   %>

	   <%
	   SimpleDateFormat sdf=new SimpleDateFormat("dd.MM.yyyy:HH:mm");
	   if(IwcmFsDB.useDBStorage(virtualPath))
		{%>
	   	<table border=0 width="100%" height="100%" cellpadding=0 cellspacing=0 class="tableclass">
  		   	<tr>
  		   		<td><b><iwcm:text key="fbrowse.datum_zmeny" /></b></td>
  		   		<td><b><iwcm:text key="fbrowse.size" /></b></td>
  		   		<td><b><iwcm:text key="formslist.tools" /></b></td>
  		   	</tr>
  		   <%
	   	List<FileBean> historyList= IwcmFsDB.getVersionList(virtualPath);
		   for (FileBean fileBean: historyList)
		   { %>
		   <tr class="fhistoryRow<%=fileBean.getFilePath()%>">
			   <td><%=sdf.format(new Date( fileBean.getLastModified()))%></td>
			   <td><%=format(fileBean.getFileSize()) %></td>
			   <td>
			   	<a target="_blank" href="<%=Tools.getRequestParameter(request, "file")%>?fid=<%=fileBean.getLocalLastModified()%>" title="<iwcm:text key="components.tips.view"/>"><img border="0" src="/admin/skins/webjet6/images/icon/icon-preview.png" /></a>
			   	<a href="javascript:void(0)"  onclick="replace(<%=fileBean.getLocalLastModified() %>)" title="<iwcm:text key="fbrowse.replace"/>"><img border="0" src="/admin/skins/webjet6/images/icon/arrow-refresh.gif" /></a>
			   	<a target="_blank" href="/admin/file_compare.jsp?firstFile=<%=Tools.getRequestParameter(request, "file")%>&fatId=<%=fileBean.getLocalLastModified()%>" title="<iwcm:text key="components.sync.porovnat"/>"><img border="0" src="/admin/skins/webjet6/images/icon/icon-compare.png" /></a>
			   </td>
		   </tr>
		   <%}%>
		   </table>
		   <%
		}
	   else
		{%>
	   	<table border=0 width="100%" height="100%" cellpadding=0 cellspacing=0 class="tableclass">
		   	<tr>
		   		<td><b><iwcm:text key="fbrowse.datum_zmeny" /></b></td>
		   		<td><b><iwcm:text key="editor_dir.tools" /></b></td>
		   		<td><b><iwcm:text key="user.user" /></b></td>
		   		<td><b>IP</b></td>
		   	</tr>
		  	<%
		 	List<FileHistoryBean> historyList = new FileHistoryDB().getHistoryByPath(virtualPath);
		   for (FileHistoryBean fileBean: historyList)
		   { %>
		   <tr class="fhistoryRow<%=fileBean.getFileUrl()%>">
			   <td><%=sdf.format(fileBean.getChangeDate())%></td>
			   <td>
			   		<a target="_blank" href="/admin/file_compare.jsp?firstFile=<%=Tools.getRequestParameter(request, "file")%>&secondFile=<%=fileBean.getHistoryPath()+fileBean.getId()%>" title="<iwcm:text key="components.sync.porovnat"/>"><img border="0" src="/admin/skins/webjet6/images/icon/icon-compare.png" /></a>
						<a href="<%=currentDirForFile+fileName %>?fHistoryId=<%=fileBean.getId()%>" class="iconPreview" target='_blank' title="<iwcm:text key="components.banner.open" />"> </a>
				</td>
			   <td><%=UsersDB.getUser(fileBean.getUserId()) != null ? UsersDB.getUser(fileBean.getUserId()).getFullName() : prop.getText("editor.link.target_none")%></td>
			   <td><%=fileBean.getIpAddress() %></td>
		   </tr>
		 <%}%>
		   </table>
	<% }%>
</table>

</script>
<script>
function replace(id)
{
	if (window.confirm("<iwcm:text key="fbrowse.replace_question" />"))
	{
		$.get("/admin/replace_file_with_history.jsp?file=<%=Tools.getRequestParameter(request, "file")%>&historyId="+id);
		alert("<iwcm:text key="fbrowse.file_replaced"/>");
		window.parent.popupHide();
	}
}
</script>
<%@ include file="/admin/layout_bottom_popup.jsp" %>
