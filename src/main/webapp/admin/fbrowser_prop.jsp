<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.users.UserDetails"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.filebrowser.*,java.util.*,sk.iway.iwcm.FileTools,sk.iway.iwcm.stat.Column" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ include file="/admin/layout_top_popup.jsp" %>
<%@page import="sk.iway.iwcm.doc.DocDB"%>

<%@page import="sk.iway.iwcm.io.IwcmFsDB"%>
<%@page import="sk.iway.iwcm.sync.FileBean"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="java.text.DecimalFormat"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.Constants"%><%
String dir = Tools.getRequestParameter(request, "dir");
String file = Tools.getRequestParameter(request, "file");

String filePath = dir;
if (filePath == null) filePath = "";
if (filePath.endsWith("/") == false) filePath = filePath + "/";
filePath += file;

UserDetails user = UsersDB.getCurrentUser(request);
boolean canUpload = user.isFolderWritable("/"+dir);

boolean useVersioning = IwcmFsDB.useVersioning();
%>
<script type="text/javascript">
$(function(){
	<iwcm:present parameter="saved">
		if (typeof window.parent.fbrowserDone == "function") {
			window.parent.fbrowserDone();
		}
	</iwcm:present>
	<iwcm:present parameter="refresh">
		refresh();
	</iwcm:present>

	showAllowedButtons();
});

function showAllowedButtons() {
	if (window.parent != null && typeof window.parent.showAllowedButtons == 'function') {
		var allowed = {
			useVersioning: <%= useVersioning %>
		}

		window.parent.showAllowedButtons(allowed);
	}
}

function refresh() {
	if (window.parent)
	{
		if (typeof window.parent.fbrowserDone == 'function') {
			window.parent.fbrowserDone();
		}
		else {
			window.parent.location.href="/admin/fbrowser.browse.do?rnd=<%=sk.iway.iwcm.Tools.getNow()%>";
		}
	}
}

function submitForm()
{
	$('#fbrowserFileEditForm').submit();
}

function reindexFile()
{
	var dir = '<%= dir %>';
	var fileName = '<%= file %>';
   document.reindexForm.file.value = dir + fileName;
   document.reindexForm.submit();
}

function viewUsage()
{
	window.location.href="/admin/fbrowser/fileprop/?prop=yes&usage=true&dir=<%= dir %>&file=<%= file %>";
	hideButtons();
}

function showHistory()
{
	window.location.href='/admin/file_history.jsp?file=<%= dir %>+"/"+<%= file %>';
	hideButtons();
}


function hideButtons()
{
	if (window.parent != null && typeof window.parent.hideButtons == 'function')
	{
		window.parent.hideButtons();
	}
}

function zmen()
{
	if ( document.all.fbrowserEditForm.changeName.checked == true)
	{
		document.all.fbrowserEditForm.zmenNazov.value = 1;
	}
}

</script>

<style>
body { background: none;}
.loginDocIdBox,
.buttonsBox {display: none;}
.loginDocIdBox {margin-top: 10px;}
fieldset {margin-bottom: 10px;}
</style>
<form:form action="/admin/fbrowser/fileprop/" modelAttribute="fbrowserEditForm" id="fbrowserFileEditForm" name="fbrowserEditForm" method="post">
	<div id="userGroupsList1">
    	<fieldset id="file_name" style="<%if ("true".equals(Tools.getRequestParameter(request, "usage"))) out.print("display:none;"); %>">
			<table>
				<tr>
					<td><iwcm:text key="fbrowse.file_name"/>:</td>
					<td><form:input type="text" path="file" styleId="file" size="35"/></td>
				</tr>
				<tr>
					<td></td>
					<td><label><input type="checkbox" name="changeName" value=0 onclick="zmen();"> <iwcm:text key="fbrowse.change_file_name"/></label></td>
				</tr>
			</table>
			<input type="hidden" name="zmenNazov" value="0">
		</fieldset>

		<fieldset id="usage" style="padding:3px; height:100%;<%if ("true".equals(Tools.getRequestParameter(request, "usage"))==false) out.print("display:none;"); %>" >
         	<strong><iwcm:text key="fbrowse.usage"/>: <%=filePath %></strong>
         	<table class="sort_table" cellspacing="0" cellpadding="1">
         		<tr>
         			<td class="sort_thead_td"><iwcm:text key="stat_top.name"/></td>
         			<td class="sort_thead_td">URL</td>
         		</tr>
				<%
				List list = new ArrayList();
				if ("true".equals(Tools.getRequestParameter(request, "usage"))) list = FileTools.getFileUsage(filePath, user);
				Column col;
				DocDB docDB = DocDB.getInstance();
			  	for(int j=0;j<list.size();j++)
			  	{
			  		col = (Column)list.get(j);
		  			%>
		  			<tr>
		  				<td class="sort_td"><%if (col.getIntColumn1()>0) {%><a class="iconEdit" href="/admin/editor.do?docid=<%=col.getIntColumn1()%>" target="_blank">&nbsp;</a>&nbsp;<%} out.print(col.getColumn1());%></td>
		  				<td class="sort_td"><a href="<%=col.getColumn2()%>" target="_blank"><%=col.getColumn2()%></a></td>
		  			</tr>
	        	<% } %>
			</table>
		</fieldset>

		<iwcm:present name="atrs">
			<fieldset style="padding:3px; height:100%;"><legend style="height: 15px;"><iwcm:text key="fbrowse.file_atrs"/></legend>
	               <script type="text/javascript">
	                  var lastAtrGroupId = 0;
	                  function atrGroupSelectChange(select)
	                  {
	                     showAtrGroup(select.selectedIndex);
	                  }
	                  function showAtrGroup(id)
	                  {
	                     showHideElement("atr_group_"+lastAtrGroupId, false);
	                     showHideElement("atr_group_"+id, true);
	                     lastAtrGroupId = id;
	                  }
	               </script>
	               <table>
	               <tr>
	                  <td valign="top">
	                     <select name="atrGroupSelect" onChange="atrGroupSelectChange(this)">
	                        <%

	                        int selectedGroup = 0;

	                        List atrs = (ArrayList)request.getAttribute("atrs");
	                        Iterator iter = atrs.iterator();
	                        FileAtrBean atr;
	                        List atrsInGroup;
	                        int i = 0;
	                        while (iter.hasNext())
	                        {
	                           atrsInGroup = (ArrayList)iter.next();
	                           atr = (FileAtrBean)atrsInGroup.get(0);
	                           out.println("<option value='atr_group_"+i+"'>"+atr.getAtrGroup()+"</option>");
	                           i++;
	                        }
	                        %>
	                     </select>
	                  </td>
	                  <td>
	                     <%

	                     atrs = (ArrayList)request.getAttribute("atrs");
	                     iter = atrs.iterator();
	                     i = 0;
	                     while (iter.hasNext())
	                     {
	                        atrsInGroup = (ArrayList)iter.next();
	                        request.setAttribute("atrsInGroup", atrsInGroup);
	                     %>

	                     <table border=0 cellspacing=0 cellpadding=0 id="atr_group_<%=i%>" style="display: none">
	                     <iwcm:iterate name="atrsInGroup" id="a" type="sk.iway.iwcm.filebrowser.FileAtrBean">
	                        <%
	                           if (a.getLink()!=null && a.getLink().length()>1)
	                           {
	                              selectedGroup = i;
	                           }
	                        %>
	                        <tr>
	                           <td>&nbsp;<iwcm:beanWrite name="a" property="atrName"/>:&nbsp;</td>
	                           <td><iwcm:beanWrite name="a" property="html" filter="false"/></td>
	                        </tr>
	                     </iwcm:iterate>
	                     </table>

	                     <%
	                        i++;
	                     }
	                     %>

	                     <script type="text/javascript">
	                        document.fbrowserEditForm.atrGroupSelect.selectedIndex = <%=selectedGroup%>;
	                        showAtrGroup(<%=selectedGroup%>);
	                     </script>
	                  </td>
	               </tr>
	               </table>
	            </fieldset>
            </iwcm:present>

            <div style="display: none;">
		        <iwcm:menu name="fileIndexer">
		        	<input type="button" name="bReindex" onClick="reindexFile()" value="<iwcm:text key="fbrowse.reindex_file"/>">
		        </iwcm:menu>

		        <input type="button" name="bUsage" value="<iwcm:text key="fbrowse.usage_button"/>" onClick="viewUsage()">
	            <input type="submit" name="bSubmit" value="<iwcm:text key="fbrowse.save_button"/>">

	   			<% if (useVersioning) { %>
					<input type="button" name="showHistory" value="<iwcm:text key="groupslist.show_history"/>" onclick="showHistory()">
				<% } %>

	            <form:hidden path="dir"/>
	            <form:hidden path="origFile"/>
	            <input type="hidden" name="prop" value="yes">
            </div>
</form:form>

<form action="/admin/fbrowser/fulltext-index/index/" name="reindexForm" style="display: none">
   <input type="hidden" name="file" value="">
</form>

<%@ include file="/admin/layout_bottom_popup.jsp" %>
