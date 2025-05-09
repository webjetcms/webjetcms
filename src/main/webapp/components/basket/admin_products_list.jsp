<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.ArrayList"%>
<%@page import="sk.iway.iwcm.tags.JSEscapeTag"%>
<%@page import="sk.iway.tags.CurrencyTag"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<iwcm:checkLogon admin="true" perms="cmp_basket|menuWebpages"/>
<%@ include file="/admin/layout_top.jsp" %>



<style type="text/css">
body { background-image: none !important; background-color: #eceef0 !important; }
input[type="text"] {
    background-color: white;
    border: 1px solid #e5e5e5;
    border-radius: 0;
    box-shadow: none;
    color: #333333;
    font-size: 14px;
    font-weight: normal;
    padding: 6px 12px;
}
.poleMini, .poleKratke, .poleMax, .poleText350x100, #docIdInput, .poleDlhe, select {
    background-color: white;
    border: 1px solid #e5e5e5;
    border-radius: 0;
    box-shadow: none;
    color: #333333;
    font-size: 14px;
    font-weight: normal;
    padding: 6px 12px;
}
select.filterKategorii{
	max-width: 500px;

}
table.newsTable tr td  { border-bottom: 1px dotted #a5a6a8; padding-bottom: 16px; padding-top: 16px; padding-left: 8px; padding-right: 8px; }
table.newsTable table tr td {border: 0px none; padding: 5px 10px 5px 0;}
table.newsTable input.textInput {    width: 100%; }
table.newsTable input.textInputWithImage { width: 770px; }
table.newsTable textarea.textInput {    width: 100%; height: 80px; color: #999999; font-size: 11px; }
div.imageDiv { cursor: pointer; background-repeat: no-repeat; background-position: 11px 2px; margin-bottom: 6px; }
div.variantBox,
div.variantBox tr.template {display: none;}
table.newsTable input.variantValues {width: auto;}
.minicolors-swatch {margin-left: 8px; margin-top: -2px;}
.minicolors-input {padding-left: 30px;}
.minicolors-theme-default.minicolors {display: inline-block !important;}
.colorpicker-rgba{padding-left: 28px !important;}
</style>
<%
//ADMIN stranka pre zobrazenie zoznamu noviniek v inline editacii

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
Prop prop = Prop.getInstance(request);

if ("true".equals(request.getParameter("reload")))
{
	//volane z editora po skonceni editacie stranky
	%>
	<script type="text/javascript">
	window.parent.loadComponentIframe();
	</script>

	<%
	return;
}

String includeText = request.getParameter("include");
//out.println(includeText);
if (Tools.isEmpty(includeText))
{
	out.println("Error parsing params");
	return;
}

if (includeText.length()>6 && includeText.endsWith(")!")) includeText = includeText.substring(0, includeText.length()-2);

request.setAttribute("includePageParams", includeText);

PageParams pageParams = new PageParams(request);

String groupIds = pageParams.getValue("groupIds", "");
if(Tools.isEmpty(groupIds)) {
	int groupId = Tools.getIntValue(request.getParameter("groupId"), -1);
	if (groupId > 0)
	{
		groupIds = String.valueOf(groupId);
	}
}

if(Tools.isEmpty(groupIds)){
	out.print(prop.getText("components.basket.noGroupAlert"));
	return;
}
//mame to v takomto formate, takze to convertneme
groupIds = groupIds.replace('+', ',');

//ak je nastavene na true beru sa do uvahy aj podadresare
boolean expandGroupIds = pageParams.getBooleanValue("expandGroupIds", true);

String perexGroup = pageParams.getValue("perexGroup", "");
//mame to v takomto formate, takze to convertneme
perexGroup = perexGroup.replace('+', ',');

int orderType = DocDB.ORDER_PRIORITY; // 3
String p_order = pageParams.getValue("orderType", "priority");
if (p_order != null)
{
	if (p_order.compareTo("date") == 0)
	{
		orderType = DocDB.ORDER_DATE; // 4
	}
	else if (p_order.compareTo("id") == 0)
	{
		orderType = DocDB.ORDER_ID; // 2
	}
	else if (p_order.compareTo("priority") == 0)
	{
		orderType = DocDB.ORDER_PRIORITY; // 3
	}
	else if (p_order.compareTo("title") == 0)
	{
		orderType = DocDB.ORDER_TITLE; // 1
	}
	else if (p_order.compareTo("place") == 0)
	{
		orderType = DocDB.ORDER_PLACE; // 5
	}
	else if (p_order.compareTo("eventDate") == 0)
	{
		orderType = DocDB.ORDER_EVENT_DATE; // 6
	}
	else if (p_order.compareTo("saveDate") == 0)
	{
		orderType = DocDB.ORDER_SAVE_DATE; // 7
	}
}

boolean ascending = pageParams.getBooleanValue("asc", true);
int publishType = DocDB.PUBLISH_NEW; // 1
String p_publish = pageParams.getValue("publishType", "new");
if (p_publish!=null){
	if (p_publish.compareToIgnoreCase("new")==0)
	{
		publishType = DocDB.PUBLISH_NEW; // 1
	}
	else if (p_publish.compareToIgnoreCase("old")==0)
	{
		publishType = DocDB.PUBLISH_OLD; // 2
	}
	else if (p_publish.compareToIgnoreCase("all")==0)
	{
		publishType = DocDB.PUBLISH_ALL; // 3
	}
	else if (p_publish.compareToIgnoreCase("next")==0)
	{
		publishType = DocDB.PUBLISH_NEXT; // 4
	}
}

int truncate = pageParams.getIntValue("truncate", 100);

boolean noPerexCheck = pageParams.getBooleanValue("noPerexCheck", true);
if (noPerexCheck && publishType < 100)
{
	publishType = publishType + 100;
}

boolean paging = pageParams.getBooleanValue("paging", true);
int pageSize = 1000;
int maxCols = pageParams.getIntValue("cols", 1);
// image moznosti - none, top, bottom
String image = pageParams.getValue("image", "none");
String pagingStyle = pageParams.getValue("pagingStyle", "top");
if (pagingStyle.equals("no")) {
	paging = false;
}
String newsName = pageParams.getValue("name", "");
String newsStyle = pageParams.getValue("style", "");
boolean perex = pageParams.getBooleanValue("perex", true);
boolean date = pageParams.getBooleanValue("date", false);
boolean place = pageParams.getBooleanValue("place", false);

//ziskaj DocDB
DocDB docDB = DocDB.getInstance();

int actualDocId = -1;
try
{
	actualDocId = Integer.parseInt(request.getParameter("docid"));
}
catch (Exception ex)
{
	//sk.iway.iwcm.Logger.error(ex);
}

//vyradime zo zobrazenia aktualnu stranku + vyradime stranky s vlozenou komponentou
String whereSql = " AND doc_id NOT IN ("+actualDocId+") AND (LENGTH(field_k) > 0 OR LENGTH(perex_image) > 0)";
if (perexGroup != null && perexGroup.length()>0)
{
	request.setAttribute("perexGroup", perexGroup);
}
else
{
	request.removeAttribute("perexGroup");
}
request.setAttribute("whereSql", whereSql);

docDB.getDocPerex(groupIds, orderType, ascending, publishType, pageSize, "novinky", "pages", request);
String[] groupIdsArray = groupIds.split(",");
int groupId = Tools.getIntValue(groupIdsArray[0], -1);

if (groupId > 0)
{
	List<DocDetails> novinky = (List<DocDetails>)request.getAttribute("novinky");

	int rootGroupId = sk.iway.iwcm.common.CloudToolsForCore.getRootGroupId(request);
	GroupDetails rootGroup = GroupsDB.getInstance().getGroup(rootGroupId);

	DocDetails newDoc = new DocDetails();
	newDoc.setDocId(0);
	newDoc.setGroupId(groupId);
	newDoc.setTitle(prop.getText("componente.cloud.basket.enterTitle"));
	newDoc.setExternalLink("");

	String currency = null;
	if (rootGroup != null && Tools.isNotEmpty(rootGroup.getFieldC()))
	{
		currency = sk.iway.iwcm.common.CloudToolsForCore.getValue(rootGroup.getFieldC(), "curr");
	}
	if (Tools.isEmpty(currency)) currency = Constants.getString("basketDisplayCurrency");

	newDoc.setFieldJ(currency);

	String vat = null;
	if (rootGroup != null && Tools.isNotEmpty(rootGroup.getFieldC()))
	{
		vat = sk.iway.iwcm.common.CloudToolsForCore.getValue(rootGroup.getFieldC(), "vat");
	}
	if (Tools.isNotEmpty(vat)) newDoc.setFieldL(vat);

	novinky.add(newDoc);
}

%>
<script type="text/javascript" src="/components/basket/jscript.jsp"></script>
<script type="text/javascript">
	var selectedGroupId='';

	$(document).ready(function()
	{
		showActualCategory(true);
		$(".filterKategorii").change(function(){
			showActualCategory(false);
		});

		$('.deleteCategory').click(function(){
			deleteCategoryOrProduct(selectedGroupId,-1);
		});

		$('.addVariant').click(function(){
			addVariant($(this));
		});

		$('.newsTable').on('change', '.variantName', function(){
			var val = $(this).val();
			if (val != "") {
				var docId = $(this).parents('form').find('input[name="docid"]').val();
				markChanged(docId);
			}

			if(val == "newVariant") {
				$(this).val("");
				newVariant();
			}
		});

		$('.newsTable').on('click', '.removeVariant', function(){
			var box = $(this).parents('.variantBox');

			$(this).parents('tr').first().remove();

			if (box.find('tr').not('.template').length == 0) {
				box.hide();
			}
		});


	$(".showLabel").bind("click", function(){
		var product = $(this).data("counter");
		if($(this).is(':checked')){
			$(".label-wrapper-"+product).fadeIn();
			$(".label-wrapper-"+product+" input[name=fieldQ]").val("");

		}else{
			$(".label-wrapper-"+product).fadeOut();
		}

	});

	});

	function newVariant() {
		var name = window.prompt('<iwcm:text key="components.basket.admin_products_list.variant_title"/>', "");

		if ($('div.variantBox select').first().find('option[value="' + name + '"]').length > 0) {
			alert('<iwcm:text key="components.basket.admin_products_list.variant_title_exists"/>');
			return;
		}

		if (name) {
			$('div.variantBox select').find('option:last').before('<option value="' + name + '">' + name + '</option>');
			var el = $('div.variantBox').find('tr').not('.template').find('select.variantName');

			el.each(function(){
				if ($(this).val() != "") {
					return true;
				}
				else {
					$(this).find('option[value="' + name + '"]').prop('selected', true);
					$(this).trigger('change');
					return false;
				}
			});
		}
	}

	function addVariant(el) {
		var template = el.siblings('div.variantBox').find('table tr.template');
		var tr = template.clone().removeClass('template');
		var box = el.siblings('div.variantBox');

		tr.find('select, input').each(function(){
			var name = $(this).prop('name');
			var newName = name.replace('Template', '');

			$(this).prop('name', newName);
		});

		box.find('table').append(tr);
		box.fadeIn();
	}

	function showActualCategory(fromCookie)
	{
		if(fromCookie)
		{
			if(<%=Tools.getCookieValue(request.getCookies(), "actualMenuId", "-1")%> != '-1'){
				$(".filterKategorii").val("<%=Tools.getCookieValue(request.getCookies(), "actualMenuId", "-1")%>");
			}
		}
		if($(".filterKategorii").val() == undefined || $(".filterKategorii").val() == "") return;
		$('.canHide').not(".group_"+$(".filterKategorii").val()).hide();
		$(".group_"+($(".filterKategorii").val())).show();
		selectedGroupId = $(".filterKategorii").val();
		$('.categoryGroupIdActual').val(selectedGroupId);
		setMenuCookie(selectedGroupId);
	}

	function setMenuCookie(value)
	{
		<%--nastavime si cookie a na zaklade nej vieme aku polozku mal user zvolenu pred reloadom / pridanim polozky--%>
		var today = new Date();
		var expire = new Date();
		expire.setTime(today.getTime() + 3600*1000*2);<%--2 hodiny--%>
		document.cookie = 'actualMenuId='+value+';expires='+expire.toGMTString()+';path=/;';
	}

	<%-- Pridanie novej kategorie--%>
	function addCategory()
	{
		var categoryName = prompt("<%=prop.getText("components.basket.addNameOfCategory")%>", "");
		if(categoryName.length > 0)
		{
			var ajaxData = {
					docid:	-1,
					groupid:	selectedGroupId,
					newCategory: 'true',
					title: categoryName
			};

			$.ajax({
				url: "/components/basket/admin_save.jsp",
				data: ajaxData,
				type: "POST",
				dataType: "html",

				success: function( html )
				{
					if (html.indexOf("OK")!=0)
					{
						//window.alert(html);
					}
					if (html.indexOf("reload")!=-1)
					{
						<%--ziskame si cislo groupy ktora bola prave pridana--%>
						setMenuCookie(html.substring(html.indexOf("groupId=")+8,html.indexOf(";")));
						window.location.href="admin_products_list.jsp?rnd=<%=Tools.getNow()%>&include=<%=Tools.URLEncode(includeText) %>";
					}
				},

				error: function( xhr, status ) {
			        alert( "<iwcm:text key="dmail.subscribe.db_error"/>");
			    }
			});
		}
	}

	function selectImage(docId)
	{
		markChanged(docId);
		openImageDialogWindow("docform"+docId, "perexImage", null);
	}
	function perexImageBlur(input)
	{
		var div = $("#img"+input.form.name.substring(7));
		div.css("background-image", "url(<iwcm:cp/>/thumb"+input.value+"?w=100&h=100&ip=5)");
	}

	function deleteCategoryOrProduct(category,product)
	{
		if(!confirm('<%=prop.getText("components.connect_domain.keep_in_mind_undone")%>'))
			return false;

		var params = {
				docId:product,
				groupId:category
		};

		$.ajax({
			url: "/components/basket/admin-delete-item-ajax.jsp",
			data: params,
			type: "POST",
			dataType: "html",

			success: function( html )
			{
				if (html.indexOf("true")!=0)
				{
					window.alert(html);
				}

				if (html.indexOf("reload")!=-1)
				{
					window.location.href="admin_products_list.jsp?rnd=<%=Tools.getNow()%>&include=<%=Tools.URLEncode(includeText) %>";
				}
			},

			error: function( xhr, status ) {
		        alert( "<iwcm:text key="dmail.subscribe.db_error"/>");
		        $("#loader"+docId).hide();
		    }
		});

	}

	function clearData(data)
	{
		var nameCounter = 0;
		var valueCounter = 0;

		for (var i=0; i<data.length; i++) {
			var el = data[i];

			if (el.name == "variantNameTemplate" || el.name == "variantValuesTemplate") {
				data.splice(i, 1);
				continue;
			}


			if (el.name == "variantName") {
				el.name = el.name + "[" + nameCounter++ +"]";
			}

			if (el.name == "variantValues") {
				el.name = el.name + "[" + valueCounter++ +"]";
			}
		}

		return data;
	}

	function saveChanges(button)
	{
		var docId = button.form.docid.value;
		$("#loader"+docId).show();
		var myForm = $("#iddocform"+docId);
		var ajaxData = myForm.serializeArray();

		ajaxData = clearData(ajaxData);

		$.ajax({
			url: "/components/basket/admin_save.jsp",
			data: ajaxData,
			type: "POST",
			dataType: "html",

			success: function( html )
			{
				if (html.indexOf("OK")!=0)
				{
					//window.alert(html);
				}
				markUnChanged(docId);
				$("#loader"+docId).hide();
				if (html.indexOf("reload")!=-1)
				{
					window.location.href="admin_products_list.jsp?rnd=<%=Tools.getNow()%>&include=<%=Tools.URLEncode(includeText) %>";
				}
			},

			error: function( xhr, status ) {
		        alert( "<iwcm:text key="dmail.subscribe.db_error"/>");
		        $("#loader"+docId).hide();
		    }
		});

	}

	function addNewItemClick()
	{
		$("#rowAddNewItemButton").hide();
		$("#rowAddNewItem").show();
	}

	function elementOnChange(el)
	{
		var docid = el.form.docid.value;
		markChanged(docid);
	}

	function markChanged(docId)
	{
		var form = document.getElementById("iddocform"+docId);
		form.changed.value="1";
		$("#btnSave"+docId).removeClass("button50grey");
	}

	function markUnChanged(docId)
	{
		var form = document.getElementById("iddocform"+docId);
		form.changed.value="0";
		$("#btnSave"+docId).addClass("button50grey");
	}

	function isUnsavedForm()
	{
		var isChange = false;
		$("input[name=changed]").each(function()
		{
			 var input = $(this);
			 if (input.attr("value")=="1")
			 {
				 isChange = true;
			 }
		});

		return isChange;
	}

	function checkDefaultText(input)
	{
		var defaultText = "<iwcm:text key="componente.cloud.basket.enterTitle"/>";

		if (input.value=="") input.value=defaultText;
		else if (input.value==defaultText) input.value="";
	}
</script>
<%
GroupsDB groupsDB = GroupsDB.getInstance();
GroupDetails mainGroup = groupsDB.getGroup(groupId);

if(mainGroup != null)
{
    int counter = 0;
    %>
<div style="padding:5px; border-bottom: 1px dotted #A5A6A8;">
	<iwcm:text key="components.bazar.category"/>:
	<select class="filterKategorii">
<%
    for(String gId : groupIdsArray){
        mainGroup = groupsDB.getGroup(Tools.getIntValue(gId, -1));

		if(mainGroup!=null){
		String[] subgroupsIds = Tools.getTokens(groupsDB.getSubgroupsIds(mainGroup.getGroupId()),",");
		String helpGroupName="";
		int localGroupId = -1;


			for(String subGroupId:subgroupsIds)
			{	localGroupId=Tools.getIntValue(subGroupId,-1);
				helpGroupName = Tools.replace(groupsDB.getGroupNamePath(localGroupId), "/"+groupsDB.getDomain(localGroupId), "");%>
		<option <%=(counter==0)?"selected":""%> value="<%=subGroupId%>"><%=helpGroupName.substring(1)%></option><%
			}
            	}
            	counter++;
		}
%>
	</select>
		<span style="padding-left:25px;">
			<input class="button100 addNewCategory" type="button" onclick="addCategory();"  value="<%=prop.getText("components.basket.addCategory") %>" >
		</span>
		<span style="padding-left:10px;">
			<input  class="button100 deleteCategory" type="button"  value="<%=prop.getText("components.basket.category.delete") %>" >
		</span>

	</div>
<%
}
String defaultImage = "/components/basket/img/default_invoice_photo_thumb.jpg";//basket-icon
%>
<logic:notEmpty name="novinky">
	<table border="0" cellspacing="0" cellpadding="1" class="newsTable">
	<% int productCounter = 0; %>
	<logic:iterate id="doc" name="novinky" type="sk.iway.iwcm.doc.DocDetails">
			<% if (doc.getDocId()<1)
				{
					doc.setPerexImage(defaultImage);	%>
					<tr id="rowAddNewItemButton">
						<td colspan="2">
							<input type="button" class="button100" onclick="addNewItemClick()" value="<iwcm:text key="components.news.addNewItem"/>"/>
						</td>
					</tr>
			<% } productCounter++; %>

			<tr<% if (doc.getDocId()<1) out.print(" id=\"rowAddNewItem\" style=\"display: none;\""); else out.print("  class=\"canHide group_"+doc.getGroupId()+"\"");%>>
				<td>
				<form name="docform<%=doc.getDocId()%>" id="iddocform<%=doc.getDocId()%>">
					<table>
						<tr>
							<td valign="top" align="center"><% String perexImage = doc.getPerexImage();

								if (Tools.isEmpty(perexImage)) perexImage = "/components/news/admin_imgplaceholder.png";
								else perexImage = "/thumb"+perexImage;%>

								<div class="imageDiv" id="img<%=doc.getDocId()%>" style="background-image:url(<%=ResponseUtils.filter(perexImage) %>?w=100&h=100&ip=5)" onclick="selectImage(<%=doc.getDocId()%>)">
									<img src="/components/news/admin_imgbg.png"/>
								</div>
								<input onclick="selectImage(<%=doc.getDocId()%>)" type="button" value="<iwcm:text key="components.news.selectImage"/>" class="button50 buttonNotImportant"/>
							</td>
							<td>
								<iwcm:text key="editor.title"/>:
								<br/>
								<input type="text" name="title" value="<%=ResponseUtils.filter(doc.getTitle())%>" class="textInput" size="100" maxlength="255" onkeydown="elementOnChange(this)" onfocus="checkDefaultText(this);"  onblur="checkDefaultText(this);"/>
								<br>
								<iwcm:text key="components.basket.perex"/>:
								<br/>
					<textarea name="perex" class="textInput" cols="100" rows="5" onkeydown="elementOnChange(this)"><%=ResponseUtils.filter(doc.getPerex())%></textarea>
								<br/>
			<iwcm:text key="components.basket.show_label"/>:
			<input type="checkbox" data-counter="<%=productCounter %>" class="showLabel" name="showLabel" <% if(Tools.isNotEmpty(doc.getFieldQ())){out.print("checked");}%>><br>
			<div class="label-wrapper-<%=productCounter %>" style="<% if(Tools.isNotEmpty(doc.getFieldQ())){out.print("display:block; ");}else{out.print("display:none;");}%>">
			<iwcm:text key="components.basket.label_text"/>: <input type="text" name="fieldQ" class="textInput" value="<%=ResponseUtils.filter(doc.getFieldQ())%>" autocomplete="off"/>
			<br/><br/>
			<iwcm:text key="components.basket.label_text_color"/>: <input type="text" name="fieldR" value="<%=ResponseUtils.filter(doc.getFieldR())%>" class="colorpicker-rgba" autocomplete="off"/>
			<iwcm:text key="components.basket.label_text_bg_color"/>: <input type="text" name="fieldS" value="<%=ResponseUtils.filter(doc.getFieldS())%>" class="colorpicker-rgba" autocomplete="off"/>
			</div>
								<br/>
								<br/>

								<%
								int domainId = sk.iway.iwcm.common.CloudToolsForCore.getDomainId();
								List<AtrBean> atrs = AtrDB.getAttributes(request, domainId + "-varianty");

								request.setAttribute("variantsBeans", atrs);
								Map<String, String> variants = new HashMap<String, String>();

								//if(Tools.isNotEmpty(doc.getFieldP())){
									String variantsString = doc.getFieldP();
									for(String variantString : Tools.getTokens(variantsString, "|")) {
										String[] variant = Tools.getTokens(variantString, ":");

										variants.put(variant[0], variant[1]);
									}
								//}
								request.setAttribute("variants", variants);
								%>

								<div class="variantBox"<c:if test="${fn:length(variants) > 0 }"> style="display:block"</c:if>>
									<strong><iwcm:text key="components.basket.admin_products_list.variants"/>:</strong>
									<br/>

									<table>
										<tr class="template">
											<td><iwcm:text key="components.basket.admin_products_list.variant_title"/>:</td>
											<td>
												<select name="variantNameTemplate" class="variantName">
													<option value="">Vyberte prosím variant</option>
													<c:forEach items="${variantsBeans}" var="variant">
														<option value="${variant.atrName}">${variant.atrName}</option>
													</c:forEach>
													<option value="newVariant"><iwcm:text key="components.basket.admin_products_list.variant_new"/></option>
												</select>

											</td>
											<td><iwcm:text key="components.basket.admin_products_list.variant_values"/>:</td>
											<td><input type="text" class="textInput variantValues" name="variantValuesTemplate" value="" size="40" maxlength="255" /></td>
											<td><a href="javascript:;" class="removeVariant"><iwcm:text key="components.basket.admin_products_list.cancel"/></a></td>
										</tr>
										<c:forEach items="${variants}" var="variant">
											<tr class="">
												<td><iwcm:text key="components.basket.admin_products_list.variant_title"/>:</td>
												<td>
													<select name="variantName" class="variantName">
														<option value="">Vyberte prosím variant</option>
														<c:forEach items="${variantsBeans}" var="variantBean">
															<option <c:if test="${variant.key eq variantBean.atrName}">selected="selected"</c:if> value="${variantBean.atrName}">${variantBean.atrName}</option>
														</c:forEach>
														<option value="newVariant"><iwcm:text key="components.basket.admin_products_list.variant_new"/></option>
													</select>

												</td>
												<td><iwcm:text key="components.basket.admin_products_list.variant_values"/>:</td>
												<td><input type="text" class="textInput variantValues" name="variantValues" value="${variant.value}" size="40" maxlength="255" /></td>
												<td><a href="javascript:;" class="removeVariant"><iwcm:text key="components.basket.admin_products_list.cancel"/></a></td>
											</tr>
										</c:forEach>
									</table>
								</div>


								<a href="javascript:;" class="addVariant button100"><iwcm:text key="components.basket.admin_products_list.variant_add"/></a>

								&nbsp;
								<br/><br/>
								<iwcm:text key="components.catalog.price_without_vat"/> <input type="text" name="fieldK" value="<%=ResponseUtils.filter(doc.getFieldK())%>" class="poleMini" size="10" maxlength="10" onkeydown="elementOnChange(this)" onchange="calculateWithVat(this.form.name)" onkeyup="calculateWithVat(this.form.name)" onblur="calculateWithVat(this.form.name)" autocomplete="off"/>
								<iwcm:text key="components.basket.invoice.currency"/> <input type="text" name="fieldJ" value="<%=ResponseUtils.filter(doc.getFieldJ())%>" class="poleMini" style="width: 60px !important;"  size="3" maxlength="3" autocomplete="off" onkeydown="elementOnChange(this)"/>
								<iwcm:text key="components.catalog.vat"/> <input type="text" name="fieldL" value="<%=ResponseUtils.filter(doc.getFieldL())%>" class="poleMini" style="width: 40px !important;" size="4" maxlength="4" onkeydown="elementOnChange(this)" onchange="calculateWithVat(this.form.name)" onkeyup="calculateWithVat(this.form.name)" onblur="calculateWithVat(this.form.name)" autocomplete="off"/>
								<iwcm:text key="components.catalog.price_with_vat"/> <input type="text" name="priceWithVat" value="<%=ResponseUtils.filter(CurrencyTag.formatNumber(doc.getPriceVat())) %>" class="poleMini" size="10" maxlength="10" onkeydown="elementOnChange(this)" onchange="calculateWithoutVat(this.form.name)" onkeyup="calculateWithoutVat(this.form.name)" onblur="calculateWithoutVat(this.form.name)" autocomplete="off"/>
								<iwcm:text key="components.basket.old_price"/> <input type="text" name="fieldM" value="<%=ResponseUtils.filter(doc.getFieldM())%>" class="poleMini" size="10" maxlength="10" autocomplete="off" onkeydown="elementOnChange(this)"/>
								<br/>
								&nbsp;
								<br/>
								<iwcm:text key="components.basket.ean"/> <input type="text" name="fieldN" value="<%=ResponseUtils.filter(doc.getFieldN())%>" class="poleMini" style="width:135px !important;" autocomplete="off" onkeydown="elementOnChange(this)"/>
								<iwcm:text key="components.basket.manufacturer"/> <input type="text" name="fieldO" value="<%=ResponseUtils.filter(doc.getFieldO())%>" class="poleMini" style="width:270px !important;" autocomplete="off" onkeydown="elementOnChange(this)"/>

								<div style="padding-top: 12px;">
									<input id="btnSave<%=doc.getDocId() %>" onclick="saveChanges(this)" type="button" value="<iwcm:text key="button.save"/>" class="button50 button50grey"/>
									<input onclick="window.location.href='/admin/editor.do?docid=${doc.docId}&quitLink=3'" type="button" value="<iwcm:text key="button.edit"/>" class="button50 button50grey"/>
									<c:if test="${doc.docId > 0}">
										<input onclick="deleteCategoryOrProduct(-1,<%=doc.getDocId() %>)" type="button" value="<iwcm:text key="button.delete"/>" class="button50 button50grey"/>
									</c:if>
									<img src="/admin/images/ajax-loader.gif" id="loader<%=doc.getDocId()%>" style="display: none;"/>
									<input type="hidden" name="docid" value="<%=doc.getDocId()%>"/>
									<input class="categoryGroupIdActual" type="hidden" name="groupid" value="<%=doc.getGroupId()%>"/>
									<input type="text" name="perexImage" value="<%=ResponseUtils.filter(doc.getPerexImage())%>" onblur="perexImageBlur(this)" style="display: none;"/>
									<input type="hidden" name="changed" value="0"/>
								</div>
							</td>
						</tr>
					</table>
				</form>
				</td>
			</tr>

	</logic:iterate>
	</table>
</logic:notEmpty>
<iwcm:empty name="novinky">
	<div class="no_news"><iwcm:text key="components.news.nonews" /></div>
</iwcm:empty>
<%@ include file="/admin/layout_bottom.jsp" %>