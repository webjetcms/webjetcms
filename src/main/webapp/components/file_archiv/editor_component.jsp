<%@page import="java.util.List"%><%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld" %>
<iwcm:checkLogon admin="true" perms="cmp_file_archiv"/>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@page import="sk.iway.iwcm.gallery.GalleryDB"%>

<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorKit"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorBean"%>
<%@page import="sk.iway.iwcm.components.file_archiv.FileArchivatorDB"%>
<%@ page import="java.util.*" %>

<%
request.setAttribute("titleKey", "components.file_archiv.name");
request.setAttribute("descKey", "components.file_archiv.desc");
Prop prop = Prop.getInstance(request);
%>
<jsp:include page="/components/top.jsp"/>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px !important; }
	td.main { padding: 0; }
</style>

<%!
public static Collection<String> createCollection(String paramName, HttpServletRequest req)
{
	System.out.println(req.getParameter(paramName));
	String[] propertyArray = Tools.getTokens(getVal(req, paramName), "+");
	if(propertyArray != null && propertyArray.length > 0)
		return Arrays.asList(propertyArray);
	return null;
}

public static List<Integer> createListInteger(String retazec)
{
	if(Tools.isEmpty(retazec))
		return null;

	Scanner scanner = new Scanner(retazec);
	scanner.useDelimiter("\\+");
	List<Integer> list = new ArrayList<>();
	while (scanner.hasNextInt())
	{
	    list.add(scanner.nextInt());
	}
	scanner.close();
	return list;
}

private static String getVal(HttpServletRequest request, String name)
{
	String returnVal = Tools.getParameter(request, name);
	if(returnVal != null)
		return returnVal;
	return "";
}
%>

<%
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
request.setAttribute("descKey","components.customer_reviews.desc");
if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("perexGroup", ""))))
{
	String[] perexGroupArray = GalleryDB.convertPerexGroupString(ResponseUtils.filter(pageParams.getValue("perexGroup", "")));
	String perexGroupString = GalleryDB.convertPerexGroupString(perexGroupArray);	//aby sa pri editaci skupiny spravne popresuvali, potrebujem
																										//vstup vo specialnom formate ","+groupPerex+","
	request.setAttribute("perexGroup", perexGroupString);
}

String dirPath = pageParams.getValue("dir", FileArchivatorKit.getArchivPath() );
String productCode = replaceAll(pageParams.getValue("productCode", ""), "\\+", ", ");
String product = replaceAll(pageParams.getValue("product", ""), "\\+", ", ");
String category = replaceAll(pageParams.getValue("category", ""), "\\+", ", ");
boolean subDirsInclude = pageParams.getBooleanValue("subDirsInclude", false);
boolean asc = pageParams.getBooleanValue("asc", true);
boolean archiv = pageParams.getBooleanValue("archiv", true);
boolean open = pageParams.getBooleanValue("open", false);
int active = pageParams.getIntValue("active", 0) + 1;
String globalIds = replaceAll(pageParams.getValue("globalIds", ""), "\\+", ", ");
String order = pageParams.getValue("order", "priority");
boolean showOnlySelected = pageParams.getBooleanValue("showOnlySelected",false);
//podmienka na orderMain kvoli spatnej kompatibilite kedy ascMain este neexistovalo a riadilo sa na zaklade asc
boolean ascMain = pageParams.getValue("orderMain", null) != null ? pageParams.getBooleanValue("ascMain", true) : pageParams.getBooleanValue("asc", true);
String orderMain = pageParams.getValue("orderMain", "time");

List<Integer> globalIdList = createListInteger(pageParams.getValue("globalIds", ""));

List<FileArchivatorBean> fileArchiveList;
if(Tools.getRequestParameter(request, "formSend") != null)
{
	fileArchiveList = FileArchivatorDB.getByConditions(createCollection("productFilter",request),
				createCollection( "categoryFilter",request),createCollection("productCodeFilter",request),
				Tools.getParameter(request, "dirFilter"), true, true, null, Tools.getParameter(request, "virtualNameFilter"), Tools.getParameter(request, "realNameFilter"),false,
				Tools.getIntValue(Tools.getParameter(request, "globalIdFilter"), -1), true);

	if(Tools.getRequestParameter(request, "dirPath") == null) dirPath = FileArchivatorKit.getArchivPath();
	else dirPath = Tools.getRequestParameter(request, "dirPath");
	if(Tools.getRequestParameter(request, "productCode") == null) productCode = "";
	else productCode = Tools.getRequestParameter(request, "productCode");
	if(Tools.getRequestParameter(request, "product") == null) product = "";
	else product = Tools.getRequestParameter(request, "product");
	if(Tools.getRequestParameter(request, "category") == null) category = "";
	else category = Tools.getRequestParameter(request, "category");
	if(Tools.getRequestParameter(request, "subDirsInclude") == null) subDirsInclude = false;
	else subDirsInclude = Tools.getBooleanValue(Tools.getRequestParameter(request, "subDirsInclude"), false);
	if(Tools.getRequestParameter(request, "asc") == null) asc = true;
	else asc = Tools.getBooleanValue(Tools.getRequestParameter(request, "asc"), false);
	if(Tools.getRequestParameter(request, "archiv") == null) archiv = true;
	else archiv = Tools.getBooleanValue(Tools.getRequestParameter(request, "archiv"), false);
	if(Tools.getRequestParameter(request, "open") == null) open = false;
	else open = Tools.getBooleanValue(Tools.getRequestParameter(request, "open"), false);
	if(Tools.getRequestParameter(request, "active") == null) active = 1;
	else active = Integer.parseInt(Tools.getRequestParameter(request, "active"));
	if(Tools.getRequestParameter(request, "globalIds") == null) globalIds = "";
	else globalIds = Tools.getRequestParameter(request, "globalIds");
	if(Tools.getRequestParameter(request, "order") == null) order = "priority";
	else order = Tools.getRequestParameter(request, "order");
	if(Tools.getRequestParameter(request, "showOnlySelected") == null) showOnlySelected = false;
	else showOnlySelected = Tools.getBooleanValue(Tools.getRequestParameter(request, "showOnlySelected"), false);
	if(Tools.getRequestParameter(request, "globalIds") == null) globalIdList = null;
	else globalIdList = createListInteger(replaceAll(Tools.getRequestParameter(request, "globalIds"), ", ", "\\+"));
	if(Tools.getRequestParameter(request, "ascMain") == null) ascMain = true;
	else ascMain = Tools.getBooleanValue(Tools.getRequestParameter(request, "ascMain"), false);
	if(Tools.getRequestParameter(request, "orderMain") == null) orderMain = "time";
	else orderMain = Tools.getRequestParameter(request, "orderMain");
}
else
{
	fileArchiveList = FileArchivatorDB.getMainFileList();
}

List<FileArchivatorBean> fileAddList = FileArchivatorDB.getByGlobalId(globalIdList);

request.setAttribute("fileAddList", fileAddList);
request.setAttribute("fileArchiveList", fileArchiveList);
%>

<%!
public String replaceAll(String str, String search, String replace)
{
	return str.replaceAll(search, replace);
}
%>

<script src="/components/form/check_form.js" type="text/javascript"></script>
<script type="text/javascript">

$(document).ready(function(){
	$('[name=order] option').filter(function() {
	    return ($(this).val() === '<%=order%>');
	}).prop('selected', true);

	$('[name=orderMain] option').filter(function() {
		return ($(this).val() === '<%=orderMain%>');
	}).prop('selected', true);

	showHideOpen();
	showHideArchiv();
});

function Ok()
{
	var subDirsInclude = false;
	var asc = false;
	var archiv = false;
	var open = false;
	var showOnlySelected = false;
	var ascMain = false;

	if (document.textForm.subDirsInclude.checked)
		subDirsInclude = true;
	if (document.textForm.asc.checked)
		asc = true;
	if (document.textForm.archiv.checked)
		archiv = true;
	if (document.textForm.open.checked)
		open = true;
	if (document.textForm.showOnlySelected.checked)
		showOnlySelected = true;
	if (document.textForm.ascMain.checked)
		ascMain = true;

	var dirPath = document.textForm.dirPath.value;
	var productCode = replaceAll(document.textForm.productCode.value, ",", "+");
	var product = replaceAll(document.textForm.product.value, ",", "+");
	var category = replaceAll(document.textForm.category.value, ",", "+");
	var order = document.textForm.order.value;
	var active = document.textForm.active.value - 1;
	var globalIds = replaceAll(document.textForm.globalIds.value, ",", "+");
	var orderMain = document.textForm.orderMain.value;

	oEditor.FCK.InsertHtml("!INCLUDE(/components/file_archiv/file_archiv.jsp, dir="+dirPath
			+ ", productCode=" + productCode
			+ ", product=" + product
			+ ", category=" + category
			+ ", subDirsInclude=" + subDirsInclude
			+ ", ascMain=" + ascMain
			+ ", orderMain=" + orderMain
			+ ", archiv=" + archiv
			+ ", asc=" + asc
			+ ", order=" + order
			+ ",open=" + open
			+ ", active=" + active
			+ ", showOnlySelected=" + showOnlySelected
			+ ", globalIds=" + globalIds + ")!");

	return true ;
} // End function

function replaceAll(str, search, replace)
{
	var temp = str.split(search);
	for (var i = 0; i < temp.length; i++)
	{
		temp[i] = temp[i].trim();
	}
	return temp.join(replace);
}

function setFileBrowserPath(path)
{
	document.getElementById("dirPathId").value = path;
	return true;
}

function showHideOpen()
{
	var openCheckbox = document.getElementById("openCheckboxId").checked;
	if(openCheckbox)
		document.getElementById("activeTrId").style.display = "table-row";
	else
	{
		document.getElementById("activeTrId").style.display = "none";
		//document.getElementById("activeId").value="";
	}
}

function showHideArchiv()
{
	var archivCheckbox = document.getElementById("archivCheckboxId").checked;
	if(archivCheckbox)
		$(".ascOrderTr").show();
	else
		$(".ascOrderTr").hide();
}

function pridajGlobal(id)
{
	var globalInput = replaceAll(document.getElementById("globalIdsId").value, ",", "+");
	var array = globalInput.split("+");

	if(array.indexOf(id.toString()) > -1)
	{
		alert("<iwcm:text key="components.file_archiv.uz_pridane"/>");
	}
	else
	{
		if(globalInput.length > 0)
			document.getElementById("globalIdsId").value += ", " + id;
		else
			document.getElementById("globalIdsId").value += id;
		document.getElementById("textFormId").submit();
	}
}

function odoberGlobal(id)
{
	var globalInput = document.getElementById("globalIdsId").value;
	var array = globalInput.split(", ");
	var index = array.indexOf(id.toString());

	if(index > -1)
	{
		array.splice(index, 1);
		document.getElementById("globalIdsId").value = array.join(", ");
		document.getElementById("textFormId").submit();
	}
	else
	{
		alert("<%=prop.getText("components.file_archiv.not_in_table")%>");
	}
}

</script>


<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block; padding: 15px;">
		<form id="textFormId" name="textForm" style="margin: 0">

		<div id="cal_udalosti">
		<table>

			<tr>
				<td><label for="dirPathId"><iwcm:text key="components.file_archiv.target_directory"/>:</label></td>
				<td>
					<input id="dirPathId" type="text" name="dirPath"  value="<%=dirPath %>" readonly="readonly" />
					<input type="button"  class="button50" name="groupSelect" value="Vybrat" onClick='popupFromDialog("<iwcm:cp/>/admin/dialog_select_dir.jsp?rootDir=<%=FileArchivatorKit.getArchivPath()%>", 500, 500);'>
				</td>
			</tr>
			<tr>
				<td><label for="productCodeId"><iwcm:text key="components.file_archiv.kod_produktu"/> :</label></td>
				<td>
					<input id="productCodeId" type="text" name="productCode" value="<%=productCode %>" />

				</td>
			</tr>
			<tr>
				<td><label for="productId"><iwcm:text key="components.file_archiv.product"/> :</label></td>
				<td>
					<%
						List<LabelValueDetails> selectProducts = new ArrayList<>();
						List<String> allProducts = FileArchivatorDB.getDistinctListByProperty("product");
						Collections.sort(allProducts);
						for(String cat : allProducts){if(Tools.isNotEmpty(cat)) selectProducts.add(new LabelValueDetails(cat, cat));}
						pageContext.setAttribute("selectProducts", selectProducts);
					%>
					<iwcm:select styleId="productId" property="product" styleClass="form-control" value='<%=product%>'>
						<html:option value=""><iwcm:text key="components.file_archiv.category_all"/></html:option>
						<html:options collection="selectProducts" property="label" labelProperty="label" />
					</iwcm:select>
				</td>
			</tr>
			<tr>
				<td><label for="categoryId"><iwcm:text key="components.bazar.category"/> :</label></td>
				<td>
					<%
						List<LabelValueDetails> selectCategories = new ArrayList<>();
						List<String> allCategories = FileArchivatorDB.getAllCategories();
						Collections.sort(allCategories);
						for(String cat : allCategories){selectCategories.add(new LabelValueDetails(cat, cat));}
						pageContext.setAttribute("selectCategories", selectCategories);
					%>
					<iwcm:select styleId="categoryId" property="category" styleClass="form-control" value='<%=category%>'>
						<html:option value=""><iwcm:text key="components.file_archiv.category_all"/></html:option>
						<html:options collection="selectCategories" property="label" labelProperty="label" />
					</iwcm:select>
				</td>
			</tr>
			<tr>
				<td><label for="subDirsIncludeId"><iwcm:text key="components.file_archiv.files_with_sub_files"/> :</label></td>
				<td>
					<input id="subDirsIncludeId" type="checkbox" name="subDirsInclude" value="true" <%if (subDirsInclude)out.print("checked='checked'");%> />
				</td>
			</tr>
			<tr>
				<td><label for="ascMainId"><iwcm:text key="components.file_archiv.ascending_sort_main_file"/> :</label></td>
				<td>
					<input id="ascMainId" type="checkbox" name="ascMain" value="true" <%if (ascMain)out.print("checked='checked'");%> />
				</td>
			</tr>
			<tr>
				<td><label for="orderMainId"><iwcm:text key="components.file_archiv.sort_main_files_by"/> :</label></td>
				<td>
					<select name="orderMain" id="orderMainId">
						<option value="priority"><iwcm:text key="components.news.ORDER_PRIORITY"/></option>
						<option value="time"><iwcm:text key="components.file_archiv.time_order"/></option>
						<option value="virtual_file_name"><iwcm:text key="components.file_archiv.order.virtual_file_name"/></option>
					</select>
				</td>
			</tr>
			<tr>
				<td><label for="archivCheckboxId"><iwcm:text key="components.file_archiv.show_archiv"/> :</label></td>
				<td>
					<input id="archivCheckboxId" type="checkbox" name="archiv" value="true" onclick="showHideArchiv()" <%if (archiv)out.print("checked='checked'");%> />
				</td>
			</tr>
			<tr class="ascOrderTr">
				<td><label for="ascId"><iwcm:text key="components.file_archiv.ascending_sort_archiv_files"/> :</label></td>
				<td>
					<input id="ascId" type="checkbox" name="asc" value="true" <%if (asc)out.print("checked='checked'");%> />
				</td>
			</tr>
			<tr class="ascOrderTr">
				<td><label for="orderId"><iwcm:text key="components.file_archiv.show_entry_by"/> :</label></td>
				<td>
					<select name="order" id="orderId">
						<option value="reference"><iwcm:text key="components.file_archiv.reference"/></option>
						<option value="priority"><iwcm:text key="components.news.ORDER_PRIORITY"/></option>
						<option value="orderId"><iwcm:text key="components.file_archiv.order"/></option>
						<option value="time"><iwcm:text key="components.file_archiv.time_order"/></option>
						<option value="virtual_file_name"><iwcm:text key="components.file_archiv.order.virtual_file_name"/></option>
					</select>
				</td>
			</tr>
			<tr style="display: none;">
				<td><label for="openCheckboxId"><iwcm:text key="components.file_archiv.entry_pre_opened"/> :</label></td>
				<td>
					<input id="openCheckboxId" type="checkbox" name="open" value="true" onclick="showHideOpen()" <%if (open)out.print("checked='checked'");%> />
				</td>
			</tr>
			<tr id="activeTrId">
				<td><label for="activeId"><iwcm:text key="components.file_archiv.main_file_pre_opened_order"/> :</label></td>
				<td>
					<input id="activeId" type="text" name="active" value="<%=active %>"/>
				</td>
			</tr>
			<tr>
				<td><label for="showOnlySelectedId"><iwcm:text key="components.file_archiv.show_only_selected_files"/> :</label></td>
				<td>
					<input id="showOnlySelectedId" type="checkbox" name="showOnlySelected" value="true" <%if (showOnlySelected)out.print("checked='checked'");%> />
				</td>
			</tr>
			<tr style="display:none;">
				<td><label for="globalIdsId"><iwcm:text key="components.file_archiv.global_id_add_to_other"/> :</label></td>
				<td>
					<input id="globalIdsId" type="text" name="globalIds" value="<%=globalIds %>" readonly="readonly" size="50"/>
				</td>
			</tr>
		</table>


		<table id="tableGlobal">
		</table>
		<logic:notEmpty name="fileAddList">
		<fieldset>
			<legend><iwcm:text key="components.file_archiv.selected_files"/></legend>
			<display:table class="sort_table" name="fileAddList" defaultsort="2" pagesize="20" defaultorder="ascending"  uid="row">
				<%
				if(row != null)
				{
				%>
				<display:column title="Id" property="nnFileArchiveId"/>
				<display:column titleKey="components.file_archiv.global_id" property="globalId"/>
				<display:column titleKey="formslist.tools" class="addWidth">
					<a href="javascript:odoberGlobal(${row.globalId});" title="<iwcm:text key="components.file_archiv.upload_new_version"/>"><iwcm:text key="components.gallery.perex.groups.tag.btn.remove"/></a>
				</display:column>
				<display:column titleKey="components.file_archiv.virtualFileName" property="virtualFileName"/>
				<display:column titleKey="components.file_archiv.real_name" property="fileName"/>
				<display:column titleKey="components.file_archiv.directory" property="filePath"/>
				<display:column titleKey="components.file_archiv.product" property="product"/>
				<display:column titleKey="components.restaurant_menu.cathegory" property="category"/>
				<display:column titleKey="components.form.admin_form.type" property="productCode"/>
				<display:column titleKey="components.file_archiv.visible" property="showFile" decorator="sk.iway.displaytag.BooleanDecorator"/>
				<display:column titleKey="components.banner.priority" property="priority"/>
				<%
				}
				%>
			</display:table>
		</fieldset>
		</logic:notEmpty>

		<fieldset>
			<legend><iwcm:text key="components.file_archiv.choose_file_ad_to_output"/></legend>
			<div class="box_toggle">
				<div class="toggle_content">
					<div id="tabMenu2">
						<fieldset>
							<p>
								<!--
								<label>
									<span style="margin-left: 5px;"><iwcm:text key="components.banner.name"/>:</span>-->
									<input type="text" class="poleMini" value="<%=getVal(request, "globalIdFilter") %>" placeHolder="<iwcm:text key="components.file_archiv.global_id"/>" name="globalIdFilter" />
									<input type="text" class="poleKratke" value="<%=getVal(request, "virtualNameFilter") %>" placeHolder="<iwcm:text key="components.file_archiv.virtualFileName"/>" name="virtualNameFilter" />
									<input type="text" class="poleMini" value="<%=getVal(request, "realNameFilter") %>" placeHolder="<iwcm:text key="components.file_archiv.real_name"/>" name="realNameFilter" />
									<input type="text" class="poleMini" value="<%=getVal(request, "dirFilter") %>" placeHolder="<iwcm:text key="editor.directory"/>" name="dirFilter" />
									<input type="text" class="poleMini" value="<%=getVal(request, "productFilter") %>" placeHolder="<iwcm:text key="components.file_archiv.product"/>" name="productFilter" />
									<input type="text" class="poleMini" value="<%=getVal(request, "categoryFilter") %>" placeHolder="<iwcm:text key="components.restaurant_menu.cathegory"/>" name="categoryFilter" />
									<input type="text" class="poleMini" value="<%=getVal(request, "productCodeFilter") %>" placeHolder="<iwcm:text key="components.form.admin_form.type"/>" name="productCodeFilter" />
								<!-- </label>  -->

								<input type="submit" class="button50" value="<iwcm:text key="components.tips.view"/>" />
								<input type="hidden" name="formSend" value="true" />
							</p>
						</fieldset>
					</div>
				</div>
			</div>

			<display:table class="sort_table" name="fileArchiveList" defaultsort="1" pagesize="20" defaultorder="descending"  uid="row">
				<%
				if(row != null)
				{
				%>
				<display:column title="Id" property="nnFileArchiveId"/>
				<display:column titleKey="components.file_archiv.global_id" property="globalId"/>
				<display:column titleKey="formslist.tools" class="addWidth">
					<a href="javascript:pridajGlobal(${row.globalId});" title="<iwcm:text key="components.file_archiv.upload_new_version"/>"><iwcm:text key="components.basket.addbasket_popup.add"/></a>
				</display:column>
				<display:column titleKey="components.file_archiv.virtualFileName" property="virtualFileName"/>
				<display:column titleKey="components.file_archiv.real_name" property="fileName"/>
				<display:column titleKey="components.file_archiv.directory" property="filePath"/>
				<display:column titleKey="components.file_archiv.product" property="product"/>
				<display:column titleKey="components.restaurant_menu.cathegory" property="category"/>
				<display:column titleKey="components.form.admin_form.type" property="productCode"/>
				<display:column titleKey="magzilla.table.projects.component.visible" property="showFile" decorator="sk.iway.displaytag.BooleanDecorator"/>
				<display:column titleKey="components.news.ORDER_PRIORITY" property="priority"/>
				<%
				}
				%>
			</display:table>
		</fieldset>


		</div>
		</form>
		</div>
</div>

<jsp:include page="/components/bottom.jsp"/>
