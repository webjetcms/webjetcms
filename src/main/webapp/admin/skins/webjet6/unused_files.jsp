<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html"); %>
<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.filebrowser.*,java.util.*,sk.iway.iwcm.FileTools" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<%@ taglib prefix="iway" uri="/WEB-INF/iway.tld" %>
<%@ taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %>
<%@ taglib prefix="html" uri="/WEB-INF/struts-html.tld" %>
<%@ taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuFbrowser"/>
<%@ include file="/admin/layout_top.jsp" %>
<%@page import="java.util.ArrayList"%>



<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%@ page import="sk.iway.iwcm.Tools" %>

<script language="JavaScript">
var helpLink = "";

window.opener.location.reload();

checked = false;
function checkedAll (checkBox)
{
	 if (checked == false)
	     checked = true
     else
	     checked = false

	 if(checkBox.length == null)
         checkBox.checked = checked;
     else
			for (var i =0; i < checkBox.length; i++)
			 	checkBox[i].checked = checked;
}
function deleteUnusedFile(url, num, width, height)
{
	if (window.confirm("<iwcm:text key="fbrowse.do_you_really_wan_to_del"/>"))
	{
		deleteFileAjax(url,num, width, height);
	}
}

function confirmDelete()
{
	var isChecked = false;
	for (var i =0; i < document.fbrowserEditForm.selectedFilesToDelete.length; i++)
	{
		if (document.fbrowserEditForm.selectedFilesToDelete[i].checked == true)
			isChecked = true;
	}

	if (document.fbrowserEditForm.selectedFilesToDelete.length == null)
	{
		isChecked = document.fbrowserEditForm.selectedFilesToDelete.checked;
	}

	if (isChecked)
	{
		if (window.confirm("<iwcm:text key="fbrowse.delete.all.files"/>"))
			document.fbrowserEditForm.submit();
	}
	else
	{
		alert('<iwcm:text key="fbrowse.delete.unselected"/>');
	}
}

function deleteFileAjax(url,num, width, height)
{
 	var options = "toolbar=no,scrollbars=no,resizable=no,width="+width+",height="+height+";";
 	var returnText;
	$.ajax({
	    url: url,
	    type: 'GET',
	    dataType: 'html',
	    timeout: 30000,
	    error: function(){alert('Error loading table');},
	    success: function(html){
			alert('<iwcm:text key="fbrowse.delete.ok"/>');
	    	reloadTableRow(num);
	    }
	});
}

function reloadTableRow(num)
{
	var tbl = document.getElementById('unusedFilesList');

    tbl.rows[num].style.display = 'none';
}

<%
	boolean hasEditor = false;
	IwcmFile f = new IwcmFile(sk.iway.iwcm.Tools.getRealPath("/admin/imageeditor"));
	if (f.exists() && f.isDirectory())
	   hasEditor = true;
%>

var hasEditor = <%=hasEditor%>;

function editFile(dir,file)
{
   fileLC = file.toLowerCase();
   if (hasEditor && fileLC.indexOf(".gif")!=-1 || fileLC.indexOf(".jpg")!=-1 || fileLC.indexOf(".png")!=-1)
   {
   		popup("/admin/v9/apps/image-editor/?iID=1&dir="+dir+"&name="+file+"",700,500);
   }
   else
   {
	   popup("/admin/fbrowser/fileprop/?dir="+dir+"&file="+file, 700, 500);
	}
}

</script>

<div id="waitDiv" style="text-align:center; color: red;">
   <iwcm:text key="fbrowse.loading.unused.files"/><br/>
   <img src="/admin/images/loading-anim.gif">
</div>
<%
	out.flush();
	for (int i = 0; i < 100; i++)
	{
		out.println("<!-- generating --------------------------->");
		out.flush();
	}

	String currentDir = Tools.getRequestParameter(request, "currentDir");
	List<UnusedFile> list = new ArrayList<UnusedFile>();
	long startTime = System.currentTimeMillis();
	list = FileTools.getDirFileUsage(currentDir, request);
	long finishTime = System.currentTimeMillis();
		System.out.println("Proces vyhladavania trval: " + ((double)(finishTime-startTime))/1000 + " s - " + list.size() + " suborov");
	UnusedFile iL;

%>

<html:form action="/admin/fbrowser.delete.do" name="fbrowserEditForm" type="sk.iway.iwcm.filebrowser.EditForm" scope="request">

	<display:table name="<%=list %>" uid="unusedFilesList" class="sort_table" cellspacing="0" cellpadding="1" export="true" pagesize="1000">

	<%iL=(UnusedFile)unusedFilesList;%>
		<display:column title="#" sortable="true" style="width: 25px; text-align: right;">
			<%=pageContext.getAttribute("unusedFilesList_rowNum")%>
		</display:column>

		<display:column style="width: 635px;"  headerClass="sort_thead_td" titleKey="fbrowse.unused_files" sortable="true" class="sort_td">
	    	<a href=
	    	<% if ((!iL.getName().endsWith(".png") && !iL.getName().endsWith(".jpg") && !iL.getName().endsWith(".gif")) && iL.getName().endsWith(".jsp")){ %>
						"javascript:editFile('<%=iL.getVirtualParent()%>','<%=iL.getName()%>');"
			<%} else { %>
						"<%=iL.getVirtualParent()+ "/" +iL.getName()%>" target="_blank"
			<%} %>
			>
	    		<%=iL.getVirtualParent()+"/"+iL.getName()%>
			</a>
	    </display:column>

	    <display:column style="white-space: nowrap;" headerClass="sort_thead_td" titleKey="fbrowse.size" sortable="true" class="sort_td" sortProperty="length"><%=FileTools.getFormatFileSize(iL.getLength(), true) %></display:column>

	    <display:column style="white-space: nowrap;" headerClass="sort_thead_td" titleKey="fbrowse.date" sortable="true" class="sort_td" property="lastModified" decorator="sk.iway.displaytag.DateTimeDecorator"/>

		<display:column media="html" titleKey="admin.conf_editor.delete" style="width: 50px; text-align: center;">
			<a href="javascript:deleteUnusedFile('/admin/fbrowser.delete.do?dir=<%=iL.getVirtualParent()%>&file=<%=iL.getName()%>','<%=pageContext.getAttribute("unusedFilesList_rowNum")%>',300,200)">
				<img src='/admin/images/icon-delete.png' border=0 alt='<iwcm:text key="button.delete" />'>
			</a>
		</display:column>


		<display:column media="html" titleKey="<input type='checkbox' name='checkall' onclick='checkedAll(document.fbrowserEditForm.selectedFilesToDelete);'>">
			<html:multibox property="selectedFilesToDelete">
						<%=iL.getVirtualParent()+"/"+iL.getName() %>
	  		</html:multibox>
		</display:column>
	</display:table>
	<input type="hidden" name="currentDir" value="<%=currentDir%>" />
	<%if (list.size() != 0) { %>
	<p style="text-align: right;">
		<input type="button" style="font-size:0.90em;" onclick="confirmDelete();" name="fdSubmit" value="<iwcm:text key="fbrowser.multiple_files_delete"/>" class="button150">
	</p>
	<%} %>
</html:form>

<%@ include file="/admin/layout_bottom.jsp" %>
