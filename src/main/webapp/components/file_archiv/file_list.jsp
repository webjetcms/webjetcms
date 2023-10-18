<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit" %>
<%@ page import="sk.iway.iwcm.components.file_archiv.FileArchivatorSearchBean" %>
<%@ page import="sk.iway.iwcm.i18n.Prop" %>
<%@ page import="sk.iway.iwcm.users.UserDetails, sk.iway.iwcm.users.UsersDB, java.util.*" %>
<%@ page import="sk.iway.iwcm.*" %>
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
public static String getNextUpdateFileDate(FileArchivatorBean fab, List<FileArchivatorBean> list)
{
	if(fab == null || Tools.isEmpty(list))
		return "";

	for(FileArchivatorBean f:list)
	{
		if(fab.getId() == f.getReferenceId())
			return f.getDateUploadLater().toString();
	}

	return "";
}

public static boolean isMatchById(FileArchivatorBean fab, List<FileArchivatorBean> list)
{
	if(fab == null || Tools.isEmpty(list))
		return false;

	for(FileArchivatorBean f:list)
	{
		if(fab.getId() == f.getReferenceId())
			return true;
	}

	return false;
}

public static Collection<String> createCollection(String paramName, HttpServletRequest req)
{
	//System.out.println(req.getParameter(paramName));
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

public static List<FileArchivatorBean> filterByUserRights(List<FileArchivatorBean> fabList, UserDetails user)
{
	List<FileArchivatorBean> filtred = new ArrayList<>();
	if(Tools.isEmpty(user.getWritableFoldersList()))
    {
        return new ArrayList<>(fabList);
    }

	for (FileArchivatorBean fab : fabList)
	{
		//System.out.println("isFolderWritable(/"+fab.getFilePath()+")"+user.isFolderWritable("/"+fab.getFilePath()));
		if (user.isFolderWritable("/" + fab.getFilePath()))
		{
			filtred.add(fab);
		}
	}

	// removing duplicates
	Set<FileArchivatorBean> hs = new HashSet<>(filtred);
	filtred.clear();
	filtred.addAll(hs);

	return filtred;
}
%>
<%=Tools.insertJQueryUI(pageContext, "autocomplete")%>
<script>
$(document).ready(function(){
	$('.deleteOneFiledeleteId').click(function(){
		if( confirm('<iwcm:text key="components.file_archiv.list.confirm.delete_this_file"/>') && confirm('<iwcm:text key="components.file_archiv.list.confirm.confirm"/>'))
			$(this).next().next().click();
		return false;
	});

	$('.deleteOneFiledeleteStructure').click(function(){
		if( confirm('<iwcm:text key="components.file_archiv.list.confirm.delete_this_structure"/>') && confirm('<iwcm:text key="components.file_archiv.list.confirm.confirm"/>'))
			$(this).next().next().click();
		return false;
	});

	$('.rollback').click(function(){
		if( confirm('<iwcm:text key="components.file_archiv.list.confirm.make_rollback"/>') && confirm('<iwcm:text key="components.file_archiv.list.confirm.confirm"/>'))
			$(this).next().next().click();
		return false;
	});
});

function loadComponentIframe()
{
	const url = "/components/file_archiv/file_list_cakajuce.jsp";
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

<div class="row title">
    <h1 class="page-title"><i class="fa icon-layers"></i><iwcm:text key="components.file_archiv.name"/><i class="fa fa-angle-right"></i><iwcm:text key="components.file_archiv.list_files"/></h1>
</div>

<%
Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);
%>

<div class="tabbable tabbable-custom tabbable-full-width">
	<ul class="nav nav-tabs">
		<li class="active">
			<a href="#tabMenu1" data-toggle="tab">
				<iwcm:text key="components.filter"/>
			</a>
		</li>
		<li class="">
			<a href="#tabMenu2" data-toggle="tab" onclick="loadComponentIframe();">
				<iwcm:text key="components.file_archiv.waiting_files"/>
			</a>
		</li>
	</ul>

	<div class="tab-content">
		<div id="tabMenu1" class="tab-pane active">
			<div class="form-group">
				<button onclick="openPopupDialogFromLeftMenu('/components/file_archiv/file_archiv_upload.jsp')" type="button" class="btn default imageButton">
					<i class="wjIconBig wjIconBig-uploadDoc"></i>
					<iwcm:text key="components.file_archiv.insert_new"/>
				</button>
				<!-- filter -->
				<form name="fileArchivFilterForm" method="GET" action="<%=PathFilter.getOrigPath(request)%>" class="zobrazenie">
					<fieldset>
						<div class="form-group">
							<div class="col-lg-3 col-sm-3">
								<p><label for="file_bean_id"><iwcm:text key="components.file_archiv.file_bean_id"/></label>
								<input type="text" id="file_bean_id" class="form-control" value="<%=getVal(request, "file_bean_id") %>" placeHolder="<iwcm:text key="components.file_archiv.file_bean_id"/>" name="file_bean_id" /></p>
							</div>
							<div class="col-lg-3 col-sm-3">
								<p><label for="virtualName"><iwcm:text key="components.file_archiv.virtualFileName"/></label>
								<input type="text" id="virtualName" class="form-control" value="<%=getVal(request, "virtualName") %>" placeHolder="<iwcm:text key="components.file_archiv.virtualFileName"/>" name="virtualName" /></p>
							</div>
							<div class="col-lg-3 col-sm-3">
								<p><label for="realName"><iwcm:text key="components.file_archiv.real_name"/></label>
								<input type="text" id="realName" class="form-control" value="<%=getVal(request, "realName") %>" placeHolder="<iwcm:text key="components.file_archiv.real_name"/>" name="realName" /></p>
							</div>
							<div class="col-lg-3 col-sm-3">
								<p><label for="dir"><iwcm:text key="components.file_archiv.directory"/></label>
								<input type="text" id="dir" class="form-control" value="<%=getVal(request, "dir") %>" placeHolder="<iwcm:text key="components.file_archiv.directory"/>" name="dir" /></p>
							</div>
						</div>
						<div class="form-group">
							<div class="col-lg-3 col-sm-3">
								<p>
									<label for="product"><iwcm:text key="components.file_archiv.product"/></label>
									<%
									List<LabelValueDetails> selectProducts = new ArrayList<>();
									List<String> allProducts = FileArchivatorDB.getDistinctListByProperty("product");
									Collections.sort(allProducts);
									for(String cat : allProducts){if(Tools.isNotEmpty(cat)) selectProducts.add(new LabelValueDetails(cat, cat));}
									pageContext.setAttribute("selectProducts", selectProducts);
									%>
									<iwcm:select styleId="product" property="product" styleClass="form-control" value='<%=getVal(request, "product")%>'>
										<html:option value=""><iwcm:text key="components.file_archiv.category_all"/></html:option>
										<html:options collection="selectProducts" property="label" labelProperty="label" />
									</iwcm:select>
									<%
									if(Constants.getBoolean("fileArchivCanEdit") &&
										(user.isEnabledItem("cmp_fileArchiv_edit_del_rollback") ||
										(Tools.isNotEmpty(Constants.getString("fileArchivCanEditUsers")) &&
										Tools.containsOneItem(Tools.getTokens(Constants.getString("fileArchivCanEditUsers"), ","), user.getLogin()))))
									{
										%><p><a href="javascript:void(0);" title="<iwcm:text key="forum.edit"/>" onclick="openPopupDialogFromLeftMenu('/components/file_archiv/file_archiv_edit_list.jsp?type=product'); return false;" ><iwcm:text key="components.file_archiv.edit_product_names"/></a></p><%
									}
									%>
								</p>
							</div>
							<div class="col-lg-3 col-sm-3">
								<p><label for="category"><iwcm:text key="components.bazar.category"/></label>
								<%
								List<LabelValueDetails> selectCategories = new ArrayList<>();
								List<String> allCategories = FileArchivatorDB.getAllCategories();
								Collections.sort(allCategories);
								for(String cat : allCategories){selectCategories.add(new LabelValueDetails(cat, cat));}
								pageContext.setAttribute("selectCategories", selectCategories);
								%>
								<iwcm:select styleId="category" property="category" styleClass="form-control" value='<%=getVal(request, "category")%>'>
									<html:option value=""><iwcm:text key="components.file_archiv.category_all"/></html:option>
									<html:options collection="selectCategories" property="label" labelProperty="label" />
								</iwcm:select>
								<%
								if(Constants.getBoolean("fileArchivCanEdit") &&
									(user.isEnabledItem("cmp_fileArchiv_edit_del_rollback") ||
									(Tools.isNotEmpty(Constants.getString("fileArchivCanEditUsers")) &&
									Tools.containsOneItem(Tools.getTokens(Constants.getString("fileArchivCanEditUsers"), ","), user.getLogin()))))
								{
									%><p><a href="javascript:void(0);" title="<iwcm:text key="forum.edit"/>" onclick="openPopupDialogFromLeftMenu('/components/file_archiv/file_archiv_edit_list.jsp?type=category'); return false;" ><iwcm:text key="components.file_archiv.edit_category_names"/></a></p><%
								}
								%>
							</div>
							<div class="col-lg-3 col-sm-3">
								<p>
									<label for="productCode"><iwcm:text key="components.file_archiv.code"/></label>
									<input type="text" id="productCode" class="form-control" value="<%=getVal(request, "productCode") %>" placeHolder="<iwcm:text key="components.file_archiv.code"/>" name="productCode" />
								</p>
								<script type="text/javascript">
									(new AutoCompleter('#productCode').setMinLength(2)).setUrl('/components/file_archiv/autocomplete_json.jsp?type=product_code').transform();
								</script>
							</div>
						</div>
						<div class="form-group">
							<div class="col-lg-12 col-sm-12">
								<p>
									<label for="onlyMainFile"><iwcm:text key="components.file_archiv.filter.main_files"/></label>
									<input type="checkbox" id="onlyMainFile" value="true" placeHolder="<iwcm:text key="components.file_archiv.filter.main_files"/>"
										   name="onlyMainFile" <%=Tools.getRequestParameter(request, "formSend") == null || "true".equals(Tools.getParameter(request, "onlyMainFile")) ? "checked" : ""%>/>
									<label for="showFile"><iwcm:text key="components.file_archiv.filter.show_file"/></label>
									<input type="checkbox" id="showFile" value="true" placeHolder="<iwcm:text key="components.file_archiv.filter.show_file"/>"
										   name="showFile" <%=Tools.getRequestParameter(request, "formSend") == null || "true".equals(Tools.getParameter(request, "showFile")) ? "checked" : ""%>/>
									&nbsp;<iwcm:text key="components.file_archiv.show_file.not_checked"/>
								</p>
							</div>
						</div>
						<div class="form-group">
							<div class="col-lg-12 col-sm-12">
								<p><input type="submit" class="button50" value="<iwcm:text key="components.tips.view"/>" />
								<input type="hidden" name="formSend" value="true" /></p>
							</div>
						</div>
					</fieldset>
				</form>
			</div>





<%
int deleteId = Tools.getIntValue(Tools.getRequestParameter(request, "deleteId"), -1);
int deleteStructureId = Tools.getIntValue(Tools.getRequestParameter(request, "deleteStructure"), -1);
int deleteRollback = Tools.getIntValue(Tools.getRequestParameter(request, "deleteRollback"), -1);

if(Constants.getBoolean("fileArchivCanEdit") && (deleteId > 0 || deleteStructureId > 0 || deleteRollback > 0))
{

	String messsage=prop.getText("components.file_archiv.file_delete_fail");
	if(deleteStructureId > 0)
		messsage=prop.getText("components.file_archiv.file_or_files_delete_fail");
	if(deleteRollback > 0)
		messsage=prop.getText("components.file_archiv.rollback.failed");
	String color = "red";
	if( (deleteId > 0 && FileArchivatorKit.deleteFile(deleteId,user)) || (deleteStructureId > 0 && FileArchivatorKit.deleteStructure(deleteStructureId, user) ) ||
		(deleteRollback > 0 && FileArchivatorKit.rollback(deleteRollback, user)) )
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
if(Tools.getRequestParameter(request, "formSend") != null)
{
	if(Tools.getRequestParameter(request, "file_bean_id") != null && Tools.getRequestParameter(request, "file_bean_id").length() > 0 )
	{
		fileArchiveList = new ArrayList<>();

		FileArchivatorBean fabBeanById = FileArchivatorDB.getInstance().getById(Tools.getIntValue(Tools.getRequestParameter(request, "file_bean_id"), -1));
		if(fabBeanById != null)
			fileArchiveList.add(fabBeanById);
	}
	else
	{
		FileArchivatorSearchBean fileArchivatorSearchBean = new FileArchivatorSearchBean();
		fileArchivatorSearchBean.setProduct(createCollection("product", request));
		fileArchivatorSearchBean.setCategory(createCollection("category", request));
		fileArchivatorSearchBean.setProductCode(createCollection("productCode", request));
		fileArchivatorSearchBean.setDirPath(Tools.getParameter(request, "dir"));
		fileArchivatorSearchBean.setIncludeSubdirs(true);
		fileArchivatorSearchBean.setAsc(true);
		fileArchivatorSearchBean.setVirtualFileName(Tools.getParameter(request, "virtualName"));
		fileArchivatorSearchBean.setFileName(Tools.getParameter(request, "realName"));
		fileArchivatorSearchBean.setUseCache(false);
		fileArchivatorSearchBean.setOnlyMain("true".equals(Tools.getParameter(request, "onlyMainFile")));
		fileArchivatorSearchBean.setShowFile(request.getParameter("showFile") == null ? null : "true".equals(Tools.getParameter(request, "showFile")));
		fileArchiveList = FileArchivatorDB.search(fileArchivatorSearchBean);
	}
}
else
{
	fileArchiveList = FileArchivatorDB.getMainFileList();
	out.print("<p style='color: orange'>INFO: "+prop.getText("components.file_archiv.show_only_main_files")+"</p>");
}
fileArchiveList = filterByUserRights(fileArchiveList, user);

request.setAttribute("fileArchiveList", fileArchiveList);
FileArchivatorBean rowFab;
String deleteStructure;

List<FileArchivatorBean> waitingFab = FileArchivatorDB.getWaitingFileList();
%>

<display:table class="sort_table" name="fileArchiveList" defaultsort="1" pagesize="20" export="true" defaultorder="descending"  uid="row">
<% 
if(row != null)
{
	rowFab = (FileArchivatorBean)row;
	%>
	<display:column title="Id" property="nnFileArchiveId" sortable="true"/>
	<display:column titleKey="formslist.tools" class="addWidth">
		<% // pre podpriecinky vysledkov vyhladavania, nastroje nezobrazujeme
		if(row != null && rowFab.getReferenceId() == -1 )
		{%>
				<a href="javascript:void(0);" title="<iwcm:text key="components.file_archiv.upload_new_version"/>" onclick="openPopupDialogFromLeftMenu('/components/file_archiv/file_archiv_upload.jsp?oldId=<%=rowFab.getId()%>'); return false;"><img alt="<iwcm:text key="components.file_archiv.upload_new_version"/>" class="aFileUpload"  src="/admin/skins/webjet6/images/icon/import.gif"></a>
				<%
				if(!Constants.getBoolean("fileArchivCanEdit") && user.isEnabledItem("components.file_archiv.advanced_settings"))
				{
					%><a href="javascript:void(0);" title="<iwcm:text key="forum.edit"/>"  onclick='popup("/components/file_archiv/later_edit.jsp?edit=<%=rowFab.getId()%>", 600, 800);'><img alt="<iwcm:text key="forum.edit"/>" src="/admin/skins/webjet6/images/icon/icon-edit.png"></a><%
				}
				else if(Constants.getBoolean("fileArchivCanEdit"))
				{
					%><a href="javascript:void(0);" title="<iwcm:text key="forum.edit"/>" onclick="openPopupDialogFromLeftMenu('/components/file_archiv/file_archiv_edit.jsp?edit=<%=rowFab.getId()%>'); return false;" ><img alt="<iwcm:text key="forum.edit"/>" src="/admin/skins/webjet6/images/icon/icon-edit.png"></a><%
				}
				%>
				<a href="javascript:void(0);" title="<iwcm:text key="components.file_archiv.file_rename"/>"  onclick='popup("/components/file_archiv/file_rename.jsp?edit=<%=rowFab.getId()%>", 600, 400);'><img alt="<iwcm:text key="groupslist.rename"/>" src="/admin/skins/webjet6/images/icon/icon-preview2.png"></a>
				<a style="display: none" class="historical_version_<%=rowFab.getId()%>" href="javascript:void(0);" title="<iwcm:text key="components.file_archiv.list_of_version"/>"   onclick='popup("/components/file_archiv/file_sub_list.jsp?edit=<%=rowFab.getId()%>", 500, 500);'>
					<img alt="<iwcm:text key="components.file_archiv.list_of_version"/>" class="aFileHistory"  src="/admin/skins/webjet6/images/icon/icon-archive.png">
				</a><%

			//ak existuje subor ktory sa ma nahrat neskorej, roollback ani mazanie nepovolime
			if(!isMatchById(rowFab, waitingFab))
			{
				if(user.isEnabledItem("cmp_fileArchiv_edit_del_rollback") ||
						//@Deprecated nechavame len kvoli historii
						Tools.isNotEmpty(Constants.getString("fileArchivCanEditUsers")) &&
						Tools.containsOneItem(Tools.getTokens(Constants.getString("fileArchivCanEditUsers"), ","), user.getLogin()))
				{%>
					<form   class="historical_version_<%=rowFab.getId()%>"  name="deleteRollback" action="<%=PathFilter.getOrigPath(request) %>" method="POST" style="display:none;">
						<a class="rollback" href="<%=request.getContextPath()%>"> <img title="<iwcm:text key="components.file_archiv.rollback_last"/>" alt="<iwcm:text key="components.file_archiv.rollback_last"/>" src="/admin/skins/webjet6/images/icon/icon-redo-red.png"></a>
						<input type="hidden" name="deleteRollback" value="<%=rowFab.getId()%>" >
						<input type="submit" class="submitButtonArchivForm" style="display:none;">
					 </form>
				<%}
				deleteStructure = "deleteStructure";

				if(Constants.getBoolean("fileArchivCanEdit"))
				{
					%>
					<form name="deleteFile<%=deleteStructure%>" action="<%=PathFilter.getOrigPath(request) %>" method="POST" style="display:inline-block;">
						<a class="deleteOneFile<%=deleteStructure %>" href="<%=request.getContextPath()%>">
							<img alt="<iwcm:text key="components.file_archiv.file_delete"/>" title="<iwcm:text key="components.file_archiv.file_delete"/>" style="padding-top:4px;"  src="/admin/skins/webjet6/images/icon/icon-delete.png">
						</a>
						<input type="hidden" name="<%=deleteStructure%>" value="<%=rowFab.getId()%>" >
						<input type="submit" class="submitButtonArchivForm" style="display:none;">
					 </form><%
				}
			}
			else
			{

				%><img title="<iwcm:text key="components.file_archiv.file_will_replaced"/> : <%=getNextUpdateFileDate(rowFab, waitingFab) %>" alt="<iwcm:text key="components.file_archiv.file_will_replaced"/>"  style="padding-top:4px;"  src="/admin/images/warning.gif"><%
			}
		}
		else
		{
			out.print("- - - - - - - - - - -");
		}%>

    <script type="text/javascript">
        $(document).ready(function(){
            $.post("/components/file_archiv/exists_history_ajax.jsp?fab_id=<%=rowFab.getId()%>", function(data, status){
                if(data.trim() === 'true')
                {
                    $('form.historical_version_<%=rowFab.getId()%>').attr('style','display:inline-block;');
                    $('a.historical_version_<%=rowFab.getId()%>').attr('style','display:initial;');
                }
            });
        });
    </script>
	</display:column>
	<display:column titleKey="components.file_archiv.virtualFileName" property="virtualFileName"  sortable="true"/>
	<display:column titleKey="components.file_archiv.real_name" property="fileName" sortable="true"/>
	<display:column titleKey="components.file_archiv.directory" property="filePath" sortable="true"/>
	<display:column titleKey="inquiry.valid_since" property="validFrom" decorator="sk.iway.displaytag.DateDecorator" sortable="true"/>
	<display:column titleKey="inquiry.valid_till" property="validTo" decorator="sk.iway.displaytag.DateDecorator" sortable="true"/>
	<display:column titleKey="components.file_archiv.product" property="product" sortable="true"/>
	<display:column titleKey="components.bazar.category" property="category" sortable="true"/>
	<display:column titleKey="components.file_archiv.code" property="productCode" sortable="true"/>
	<display:column titleKey="editor.show" property="showFile" decorator="sk.iway.displaytag.BooleanDecorator" sortable="true"/>
	<display:column titleKey="components.banner.priority" property="priority" sortable="true"/>
	<display:column titleKey="components.file_archiv.pattern" sortable="true">
	<% if(row != null && Tools.isNotEmpty(rowFab.getReferenceToMain()))
	{
		out.print(rowFab.getReferenceToMain());
	}else{%>
		<img height="12px" width="12px" src="/components/_common/images/icon_false.png" alt="">
	<%} %>
	</display:column>
	<display:column titleKey="components.file_archiv.reference" property="referenceId" sortable="true"/>
	<%
}
%>
</display:table>
	</div>
	<div id="tabMenu2" class="tab-pane tab-page">
		<iframe id="componentIframeWindowTab" name="componentIframeWindowTab" style="width: 100%; min-height: 510px; height: auto; background: none;" src="/admin/iframe_blank.jsp"></iframe>
	</div>

</div>
<%@ include file="/admin/layout_bottom.jsp" %>
