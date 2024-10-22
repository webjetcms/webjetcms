<%@page import="java.util.Arrays"%>
<%@page import="java.util.Collection"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %>
<%@ page import="sk.iway.iwcm.users.UsersDB" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%@ include file="/admin/layout_top.jsp" %>
<%!
public static Collection<String> createCollection(String paramName, HttpServletRequest req)
{
	System.out.println(req.getParameter(paramName));
	String[] propertyArray = Tools.getTokens(getVal(req, paramName), "+");
	if(propertyArray != null && propertyArray.length > 0)
		return Arrays.asList(propertyArray);
	return null;
}

private static String getVal(HttpServletRequest request, String name)
{
	String returnVal = Tools.getParameter(request, name);
	if(returnVal != null)
		return returnVal;
	return "";
}
%>
<script>
$(document).ready(function(){
	$('.deleteOneFiledeleteId').click(function(){
		if( confirm('<iwcm:text key="components.file_archiv.list.confirm.naozaj_zmazat_tento_subor"/>') && confirm('<iwcm:text key="components.file_archiv.list.confirm.confirm"/>'))
			$(this).next().next().click();
		return false;
	});

	$('.deleteOneFiledeleteStructure').click(function(){
		if( confirm('<iwcm:text key="components.file_archiv.list.confirm.naozaj_zmazat_tuto_strukturu"/>') && confirm('<iwcm:text key="components.file_archiv.list.confirm.confirm"/>'))
			$(this).next().next().click();
		return false;
	});
});

function loadComponentIframe()
{
	var url = "/components/file_archiv/file_list_cakajuce.jsp";
 	$("#componentIframeWindowTab").attr("src", url);
}
</script>
<style>
<!--
 .aFileHistory{position:relative;top:4px;	}
 .aFileUpload{	width:15px;}
 .sort_table td a img{padding:0 2px 0 2px;}
 .addWidth {width:84px;}
-->
</style>


<div class="tab-page" id="tabMenu1">
		<%
		Prop prop = Prop.getInstance(request);
		int deleteId = Tools.getIntValue(Tools.getRequestParameter(request, "deleteId"), -1);
		int deleteStructureId = Tools.getIntValue(Tools.getRequestParameter(request, "deleteStructure"), -1);

		if(deleteId > 0 || deleteStructureId > 0)
		{

			String messsage = prop.getText("components.file_archiv.file_delete_fail");
			if(deleteStructureId > 0)
            {
                messsage = prop.getText("components.file_archiv.file_or_files_delete_fail");
            }
			String color = "red";
			Identity user = UsersDB.getCurrentUser(request);
			if( (deleteId > 0 && FileArchivatorKit.deleteFile(deleteId, user)) || (deleteStructureId > 0 && FileArchivatorKit.deleteStructure(deleteStructureId,user) )  )
			{
				messsage=prop.getText("components.file_archiv.delete_file_success");
				color = "green";
			}
			%><div style="color: <%=color %>; font-weight: bold;" class="deleteFileBean"><p><%=messsage %></p></div>
			<script type="text/javascript">
				setTimeout(function(){
				$('.deleteFileBean').hide(500);
				}, 4000);
			</script><%
		}

		List<FileArchivatorBean> fileArchiveList;
		fileArchiveList = FileArchivatorDB.getWaitingFileList();
		//fileArchiveList = FileArchivatorDB.getFilesToUpload();

		request.setAttribute("fileArchiveList", fileArchiveList);
		String deleteStructure;
	    FileArchivatorBean rowFab;
		%>

		<display:table class="sort_table" name="fileArchiveList" defaultsort="1" pagesize="20" defaultorder="descending"  uid="row">
		<%  if(row != null)
			{
				rowFab = (FileArchivatorBean)row;
			%>
			<display:column title="Id" property="nnFileArchiveId"/>
			<display:column titleKey="components.gdpr.tools" class="addWidth">
				 <%
				 List<FileArchivatorBean> fabList = FileArchivatorDB.getByReferenceId(rowFab.getId());
				 deleteStructure="deleteId";
				 if(fabList != null && fabList.size() > 0)
				 {
				    %>
					<a href="javascript:void(0);" title="<iwcm:text key="components.file_archiv.list_of_version"/>" onClick='openPopupDialogFromLeftMenu("<iwcm:cp/>/components/file_archiv/file_sub_list.jsp?edit=<%=rowFab.getId()%>", 500, 500);'><img alt="<iwcm:text key="components.file_archiv.list_of_version"/>" class="aFileHistory"  src="/admin/skins/webjet6/images/icon/icon-archive.png"></a>
					<%
					deleteStructure = "deleteStructure";
				 }%>
				 <form name="deleteFile<%=deleteStructure%>" action="<%=PathFilter.getOrigPath(request) %>" method="POST" style="display:inline-block;">
					 <a class="deleteOneFile<%=deleteStructure %>" href="<%=request.getContextPath()%>">
						<img alt="<iwcm:text key="components.file_archiv.file_delete"/>" title="<iwcm:text key="components.file_archiv.file_delete"/>" style="padding-top:4px;"  src="/admin/skins/webjet6/images/icon/icon-delete.png">
					 </a>
					 <input type="hidden" name="<%=deleteStructure%>" value="<%=rowFab.getId()%>" >
					 <input type="submit" class="submitButtonArchivForm" style="display:none;">
				 </form>
			</display:column>
			<display:column titleKey="components.file_archiv.virtualFileName"><%=rowFab.getUploaded() == -2 ? "<span style=\"color: red;\">"+rowFab.getVirtualFileName()+"</span>" : rowFab.getVirtualFileName()%></display:column>
			<display:column titleKey="components.file_archiv.FileArchivatorInsertLater.java.realne_meno"><%=rowFab.getUploaded() == -2 ? "<span style=\"color: red;\">"+rowFab.getFileName()+"</span>" : rowFab.getFileName()%></display:column>
			<display:column titleKey="editor.directory"><%=rowFab.getFilePath().replace(FileArchivatorKit.getFullInsertLaterPath(),"")%></display:column>
			<display:column titleKey="components.file_archiv.file_list_cakajuce.nahrat_po" property="dateUploadLater" decorator="sk.iway.displaytag.DateTimeDecorator"/>
			<display:column titleKey="inquiry.valid_since" property="validFrom" decorator="sk.iway.displaytag.DateDecorator"/>
			<display:column titleKey="inquiry.valid_till" property="validTo" decorator="sk.iway.displaytag.DateDecorator"/>
			<display:column titleKey="components.file_archiv.product" property="product"/>
			<display:column titleKey="components.docman.kategorie" property="category"/>
			<display:column titleKey="components.form.admin_form.type" property="productCode"/>
			<display:column titleKey="components.file_archiv.visible" property="showFile" decorator="sk.iway.displaytag.BooleanDecorator"/>
			<display:column titleKey="components.banner.priority" property="fieldC"/>
			<display:column titleKey="components.file_archiv.pattern" >
			<% if(row != null && Tools.isNotEmpty(rowFab.getReferenceToMain()))
			{
				out.print(rowFab.getReferenceToMain());
			}else{%>
			<img height="12px" width="12px" src="/components/_common/images/icon_false.png" alt="">
			<%} %>
			</display:column>
			<%-- <display:column titleKey="components.file_archiv.reference" property="referenceId" sortable="true"/>--%>
			<%}%>
		</display:table>
	</div>

<%@ include file="/admin/layout_bottom.jsp" %>