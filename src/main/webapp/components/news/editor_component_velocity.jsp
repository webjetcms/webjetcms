<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.PageLng"%>
<%@page import="sk.iway.iwcm.PageParams"%>
<%@page import="sk.iway.iwcm.Tools"%>
<%@page import="sk.iway.iwcm.components.news.FieldEnum"%>
<%@page import="sk.iway.iwcm.components.news.NewsTemplateBean.PagingPosition"%>
<%@page import="java.io.File"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%@
taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
<iwcm:checkLogon admin="true" perms='<%=Constants.getString("webpagesFunctionsPerms")%>'/>
<stripes:useActionBean var="actionBean" beanclass="sk.iway.iwcm.components.news.NewsActionBean" /><%

request.setAttribute("cmpName", "news");
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

if (Tools.isNotEmpty(paramPageParams)) {
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

//out.println("PARAMS: "+pageParams.getValue("contextClasses", "--"));

pageContext.include("/sk/iway/iwcm/components/news/News.action");
%>
<jsp:include page="/components/top.jsp"/>

<style type="text/css">
	.operations,
	.truncateBox,
	.filterTemplate {display: none;}
	.checkbox > label, .form-horizontal .checkbox > label {padding-left: 20px;}
	label {margin-bottom: 0px;}
	input[type="radio"], input[type="checkbox"] {margin-top: 2px;}
	.tab-pane .tab-page {padding-top: 20px;}
	#pageSize,
	#truncate {width: auto;}
	.value,
	.operationsBox {display: inline-block;}
	.operationsBox {padding: 0 10px;}
	.operationsBox a {font-weight: bold;}
	a.btn {display: inline-block;}
	.filterLabel {width: 120px;}
	div.filter.row {padding: 5px 0;}

	.operationsBox .operationsString {font-size: 50%;}
	.templatesBox .template {display: none; padding: 8px 0;}
	#template {margin-bottom: 10px;}

	.vcenter {
    display: inline-block;
    vertical-align: middle;
    float: none;
	}

.vertical-alignment-helper {
    display:table;
    height: 100%;
    width: 100%;
    pointer-events:none; /* This makes sure that we can still click outside of the modal to close it */
}
.vertical-align-center {
    /* To center vertically */
    display: table-cell;
    vertical-align: middle;
    pointer-events:none;
}
.modal-content {
    /* Bootstrap sets the size of the modal in the modal-dialog class, we need to inherit it */
    width:inherit;
    height:inherit;
    /* To center horizontally */
    margin: 0 auto;
    pointer-events: all;
}

#templateModal {margin-top: 0 !important;}

#templateModal input[type="text"], #templateModal input[type="password"], #templateModal select, #templateModal textarea { max-width: 100%; }


.tools {position: absolute; left: 21px; top: 5px; display: none; z-index: 100;}
.template .dropdown-menu .divider {margin: 0px;}
.template .btn-group .dropdown-menu {margin: 0px;}
.templates .template {padding-bottom: 15px; position: relative;}

.templates .template.selected div.image img {border: 1px solid #ccc; border-color: #3b5998;}
.templates .template.selected div.image:after {
	position: absolute; top: -1px; right: 9px;
	content: " "; width: 0; height: 0; border-left: 10px solid transparent; border-right: 10px solid transparent; border-bottom: 10px solid #3b5998;
	-ms-transform: rotate(45deg); /* IE 9 */
	-webkit-transform: rotate(45deg); /* Safari */
	transform: rotate(45deg);
}
.templateTemplate,
.template-cancel  {display: none;}

.templates .template.selected .template-choose,
.templates .template .template-cancel {display: none;}
.templates .template .template-choose,
.templates .template.selected .template-cancel {display: inline-block;}
.context-menu {z-index: 99999 !important;}
.context-menu .dropdown-submenu .dropdown-menu {max-height: 300px; overflow: auto; background: #fff;}
.context-menu .dropdown-menu { font-size: 12px; padding: 5px; }
.context-menu .dropdown-menu li > a { padding: 2px; display: inline-block; white-space: normal; width: 100%; }
.context-menu .dropdown-menu li ul li { border-bottom: 1px solid #eee; }
.context-menu .dropdown-menu li ul li:last-child { border-bottom: 0px solid #eee; }
.context-menu .dropdown-submenu > a:after { top: 0px; }
.context-menu .dropdown-submenu .dropdown-menu { width: 300px; overflow: auto; }
.context-menu.open > ul.dropdown-menu { display: block; }
#templateModal div.modal-body {
	height: 63vh;
	overflow: auto;
}
#templateModal div.modal-body label.control-label {
	text-align: left;
	width: 300px;
}
button.template-cancel {
	border-top-right-radius: 0px !important;
    border-bottom-right-radius: 0px !important;
}
button.btn:focus {
	box-shadow: none !important;
}
div.tools i.ti.ti-chevron-down {
	font-size: 20px;
	margin-left: -8px;
	line-height: 34px;
}
.btn.dropdown-toggle {
	padding-top: 0px;
	padding-bottom: 0px;
}
.tab-page select.filterSelect {
	display: inline-block;
	width: auto;
}
.operationsBox select {
	min-width: 100px;
}
</style>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type="text/javascript" src="/components/news/js/bootstrap-contextmenu.js"></script>
<script type="text/javascript" src="/components/news/js/jquery.caret.js"></script>

<script type='text/javascript'>

$(document).ready(function(){
	//$('.dropdown-toggle').dropdown();
	//$('[data-bs-toggle="tooltip"]').tooltip();

	$('body').on('mouseover', '.template', function(){
		$(this).find('.tools').stop().fadeIn();
	});

	$('body').on('mouseout', '.template', function(){
		$('.template .tools').stop().fadeOut();
	});

	setGroupIds();
	hideElements();

	$('#paging, #perex').change(hideElements);

	$('.tab-page').on('change', '.filterSelect', showOperations);

	$('.addFilter').click(function(){
		var lastIndex = $('.filter:last').find('select').prop('id').replace(/^\D+/g, '');
		var template = $('.filterTemplate').clone()[0].outerHTML;

		template = template.replace(new RegExp("\\[\\]", "g"), Number(lastIndex) + 1);

		$('.filter:last').after(template);
		$('.filter:last').removeClass('filterTemplate').show();
		$('.filter:last').addClass('filterRow').show();
	});

	$('.tab-page').on('click', '.deleteFilter', function(){
		$(this).parents('.row').first().remove();
	});

	$('.tab-page').on('click', '.operations .btn', operationClick);

	showOperations();

	$('body').on('click', '.template-edit, .template-duplicate', function(){
		var el = $(this);
		var data = {
			loadTemplate: true,
			template:  el.closest('.template').data("key")
		};

		clearModalForm();
		$.ajax({
			url: '/components/news/admin_news_templates_ajax_utf-8.jsp',
			data: data,
			dataType: 'json',
			success: function(response) {
				var template = response.template;

				if (el.hasClass('template-duplicate')) {
					$('#keyBeforeSave').val("");
				}
				else {
					$('#keyBeforeSave').val(template.key);
				}
				/*
				console.log(el.hasClass('template-duplicate'));
				console.log($('#keyBeforeSave').val());
				*/

				$('#keyShort').val(template.keyShort);
				$('#templateValue').val(template.value);
				$('#pagingValue').val(template.pagingValue);

				//console.log(template.pagingPosition);
				if (template.pagingPosition == '<%= PagingPosition.BEFORE_AND_AFTER %>') {
					$('#pagingPositionPred').prop('checked', true);
					$('#pagingPositionZa').prop('checked', true);
				}
				else if (template.pagingPosition == '<%= PagingPosition.BEFORE %>') {
					$('#pagingPositionPred').prop('checked', true);
					$('#pagingPositionZa').prop('checked', false);
				}
				else if (template.pagingPosition == '<%= PagingPosition.AFTER %>') {
					$('#pagingPositionPred').prop('checked', false);
					$('#pagingPositionZa').prop('checked', true);
				}
				else {
					$('#pagingPositionPred').prop('checked', false);
					$('#pagingPositionZa').prop('checked', false);
				}

				$('#keyShort').val(template.keyShort);

				$('#templateModal').modal('show');
			}
		});
	});

	$('body').on('click', '.template-delete', function(){
		var el = $(this);
		var data = {
			deleteTemplate: true,
			templateUpdate:  el.closest('.template').data("key")
		};

		if ($('.templates .template.selected').length > 0) {
			data.template = $('.templates .template.selected').data("key");
		}

		//console.log(data);

		$.ajax({
			url: '/components/news/admin_news_templates_ajax_utf-8.jsp',
			data: data,
			dataType: 'json',
			success: function(response) {
				publishTemplates(response);
			}
		});
	});

	$('body').on('click', '.template-create', function(){
		clearModalForm();
		$('#templateModal').modal('show');
	});

	$('body').on('click', '.template-choose, .template:not(".selected") .image', function(){
		var templateBox = $(this).closest('.template');
		$('#template').val(templateBox.data("key"));

		$('.template.selected').removeClass('selected');
		templateBox.addClass('selected');
	});

	$('body').on('click', '.template-cancel, .template.selected .image', function(){
		var el = $(this);
		var templateBox = $(this).closest('.template');

		$('#template').val("");
		templateBox.removeClass('selected');
	});

	$('#templateModal').on('click', '.template-save', function(){
		var data = {
			saveTemplate: true,
			'templateUpdate.keyBeforeSave': $('#keyBeforeSave').val(),
			'templateUpdate.keyShort': $('#keyShort').val(),
			'templateUpdate.value': $('#templateValue').val(),
			'templateUpdate.pagingValue': $('#pagingValue').val()
		};

		if ($('.templates .template.selected').length > 0) {
			data.template = $('.templates .template.selected').data("key");
		}

		if ($('#pagingPositionPred').is(':checked') && $('#pagingPositionZa').is(':checked')) {
			data['templateUpdate.pagingPosition'] = '<%= PagingPosition.BEFORE_AND_AFTER %>';
		}
		else if ($('#pagingPositionPred').is(':checked')) {
			data['templateUpdate.pagingPosition'] = '<%= PagingPosition.BEFORE %>';
		}
		else if ($('#pagingPositionZa').is(':checked')) {
			data['templateUpdate.pagingPosition'] = '<%= PagingPosition.AFTER %>';
		}
		else {
			data['templateUpdate.pagingPosition'] = '<%= PagingPosition.NONE %>';
		}

		//console.log(data.template['templateUpdate']);

		$.ajax({
			url: '/components/news/admin_news_templates_ajax_utf-8.jsp',
			type: "post",
			data: data,
			dataType: 'json',
			success: function(response) {
				publishTemplates(response);
				$('#templateModal').modal('hide');
			}
		});
	});

	$('body').on('click', '.helpBtn', function(){
		var url = $(this).prop('href');
		var options = "status=no,menubar=no,toolbar=no,scrollbars=yes,resizable=yes,width=800,height=680";
		window.open(url, "", options);

		return false;
	});

	initContextMenu();
});

function initContextMenu()
{
	var el = $('#templateValue');
	if (el.hasClass('context-menu-initialized')) {
		return;
	}

	//console.log(el);

	$('#templateModal textarea').contextmenu({
		target:'#context-menu',
		before: function(e,context) {
			var contextMenu = $('#context-menu');
			var id = context.prop("id");

			contextMenu.find('.dropdown-submenu').hide();
			contextMenu.find('.velocity').show();

			if (id == "templateValue") {
				contextMenu.find('.group').show();
				contextMenu.find('.doc').show();
			}
			else {
				contextMenu.find('.paging').show();
			}
			// execute code before context menu if shown
		},
		onItem: function(context,e) {
			e.stopPropagation();

			var textarea = context;
			var position = textarea.caret();
			var content = textarea.val().substring(0, position);
			var foreachPosition = content.lastIndexOf('#foreach(');
			var item = $(e.target);
			var value = item.data('value');
			var parent = item.closest('li.dropdown-submenu');

			if (typeof value == "undefined") {
				return false;
			}

			value = value.trim();

			if ((parent.hasClass('doc') || parent.hasClass('group')) && foreachPosition == -1) {
				alert('Musi byt vlozene do foreach');
				return false;
			}

			if (!parent.hasClass('velocity') && !parent.hasClass('paging')) {
				content = content.substring(foreachPosition + 9);
				var cycleValue = content.substring(0, content.indexOf(')'));
				var bean = $.trim(cycleValue.split('in')[0]);

				if (parent.hasClass('group')) {
					value = bean + ".group." + value;
				}
				else if (value == 'link') {
					value = '$context.link(' + bean +')';
				}
				else {
					value = bean + "." + value;
				}
			}

			if (parent.hasClass('paging')) {
				if (value == "page.link" && foreachPosition == -1) {
					alert('Musi byt vlozene do foreach');
					return false;
				}

				if (value == "page.link") {
					content = content.substring(foreachPosition + 9);
					var cycleValue = content.substring(0, content.indexOf(')'));
					var bean = $.trim(cycleValue.split('in')[0]);

					value = bean + ".link";
				}
				else if (value.indexOf("#") != 0) {
					value = '$' + value;
				}
			}

			textarea.caret(value);

			this.closemenu();
			return true;
		}
	});

	$('#templateModal textarea#pagingValue').contextmenu({
		target:'#context-menu-paging',
		before: function(e,context) {
			// execute code before context menu if shown
		},
		onItem: function(context,e) {
			e.stopPropagation();

			var textarea = $('#pagingValue');
			var position = textarea.caret();
			var content = textarea.val().substring(0, position);
			var foreachPosition = content.lastIndexOf('#foreach(');
			var item = $(e.target);
			var value = item.data('value');
			var parent = item.closest('li.dropdown-submenu');

			if (typeof value == "undefined") {
				return false;
			}

			value = value.trim();

			if ((parent.hasClass('doc') || parent.hasClass('group')) && foreachPosition == -1) {
				alert('Musi byt vlozene do foreach');
				return false;
			}

			if (!parent.hasClass('velocity')) {
				content = content.substring(foreachPosition + 9);
				var cycleValue = content.substring(0, content.indexOf(')'));
				var bean = $.trim(cycleValue.split('in')[0]);

				if (parent.hasClass('group')) {
					value = bean + ".group." + value;
				}
				else if (value == 'link') {
					value = '$context.link(' + bean +')';
				}
				else {
					value = bean + "." + value;
				}
			}

			textarea.caret(value);

			this.closemenu();
			return true;
		}
	});

	$('#templateValue').addClass('context-menu-initialized');
}

function loadTemplates() {
	//console.log('loadTemplates');

	var data = {
		loadTemplates: true
	}

	$.ajax({
		url: '/components/news/admin_news_templates_ajax_utf-8.jsp',
		type: "post",
		data: data,
		dataType: 'json',
		success: function(response) {
			publishTemplates(response);
		}
	});
}

function publishTemplates(response) {

	$('.templates').empty();

	var html = "";
	var template = $('.templateTemplate').clone();
	$.each(response.templates, function(i, v){

		template.find('.keyShort').text(v.keyShort);
		template.find('.image img').attr('src', v.image);
		template.find('.template').attr('data-key', v.key);

		if (v.selected) {
			template.find('.template').addClass('selected');
		}
		else {
			template.find('.template').removeClass('selected');
		}

		html += template.html();
	});

	$('.templates').html(html);

	$('.templates .dropdown-toggle').dropdown();
}

function clearModalForm()
{
	$('#templateModal').find("input, textarea").val('');
}

function setGroupIds()
{
	var groupIds = $('#groupIds').val();
	var groupIdEl = $('#groupIdInputBox', window.parent.parent.document);

	if (groupIds == "" && groupIdEl.length > 0) {
		$('#groupIds').val(groupIdEl.val());
	}
}

function operationClick()
{
	var el = $(this);
	var operationClass = "active";
	var operations = el.parents('.operations').first();
	var operation = el.data('operation');

	if (el.hasClass(operationClass)) {
		el.removeClass(operationClass);
	}
	else {
		if (operations.hasClass('operationsString')) {
			$(this).siblings('.btn').removeClass(operationClass);
			$(this).addClass(operationClass);
		}
		else if (operations.hasClass('operationsInteger') || operations.hasClass('operationsDate')) {

			if (operation == "lt") {
				$(this).siblings('.btn[data-operation="gt"]').removeClass(operationClass);
			}
			if (operation == "gt") {
				$(this).siblings('.btn[data-operation="lt"]').removeClass(operationClass);
			}

			$(this).addClass(operationClass);
		}
	}

	el.blur();
}

function showOperations()
{
	var hiddenValue = ['Boolean'];
	$("div.filter.row").each(function(){
		var el  = $(this).find(".filterSelect");
		var val = el.val();
		var fieldType = el.find('option:selected').data("fieldType");

		$(this).find('div.operationsBox .operations').hide();
		$(this).find('div.operationsBox .operations'+fieldType).show();

		if ($.inArray(fieldType, hiddenValue) > -1) {
			$(this).find('div.value').hide();
		}
		else {
			$(this).find('div.value').show();
		}
	});
}

function hideElements() {
	if ($('#perex').is(':checked')) {
		$('.truncateBox').show();
	}
	else {
		$('.truncateBox').hide();
	}
}

function getInclude() {
	var ignore = ['disabledItemsLeft', 'disabledItemsRight', 'filter[]', 'groupIdsSelect', 'filterValue', 'perexGroupSearch', '_sourcePage', '__fp', 'device'];
	var inc = "/components/news/news-velocity.jsp";
	var params = {};

	$("form.newsForm").find("input, select, textarea").each(function(){
		var name = $(this).prop('name');
		var val = $(this).val();



		if ($.inArray(name, ignore) > -1 || name == "" || name.indexOf('filter') == 0) {
			return true;
		}

		if ($(this).is(':checkbox')) {
			val = $(this).is(':checked');
		}

		if (val == null) {
			val = "";
		}

		if ($.isArray(val)) {
			val = val.join("+");
		}

		inc += ", " + name + "=\"" + clearVal(val)+"\"";
	});

	//inc += ", template=\"" + clearVal($("#template").val())+"\"";

	inc = getFilterParams(inc);

	return "!INCLUDE(" + inc + ")!";
}

function clearVal(val)
{
	val += "";

	val = val.replace(/,/g, "+");

	if (val.indexOf(" ") != -1) {
		//toto uz robime hore
		//val = '"' + val + '"';
	}

	return val;
}

function getFilterParams(inc)
{
	var rows = $('.tab-page .row.filter.filterRow');

	if (rows.length > 0) {
		rows.each(function()
		{
			//console.log($(this).find('.operationsBoolean').css('display'));
			//console.log($(this));

			if ($(this).find('.operationsBoolean').css('display') == 'block')
			{
				//console.log("boolean");
				inc += ", " + "filter[" + clearKey($(this).find('.filterSelect').val()) + "_eq]=" + clearVal($(this).find('.operationsBoolean select').val());
			}
			else if ($(this).find('.operationsString').css('display') == 'block')
			{
				//console.log("string");

				inc += ", " + "filter[" + clearKey($(this).find('.filterSelect').val()) + "_" + $(this).find('.operationsString select').val() + "]=" + clearVal($(this).find('.filterValue').val());
			}
			else
			{
				//console.log("integer");

				inc += ", " + "filter[" + clearKey($(this).find('.filterSelect').val()) + "_" + $(this).find('.operationsInteger select').val() + "]=" + clearVal($(this).find('.filterValue').val());
			}
		});
	}

	return inc;
}

function clearKey(key) {
	key = key.replace(/_/g, '');
	return key;
}

function doOKNewsFCK() {
	/*
	console.log('doOKNewsFCK');
	console.log(getInclude());
	*/
	oEditor.FCK.InsertHtml(getInclude());
	return true;
}

function Ok()
{
	if (doOKNewsFCK()) return true;
	else return false;
}

if (!isFck) {
	resizeDialog(570, 750);
}

function loadComponentIframe() {
	var url = "/apps/news/admin/?include="+encodeURIComponent(getInclude());
 	$("#componentIframeWindowTab").attr("src", url);
}

function setParentGroupId(value) {

	var valueArr = value.split(" ");
	valueArr = valueArr.filter(function(v){return v!==''});

	var val = $('#groupIds').val();

	if (val != "") {
		val = val + "," + valueArr[0];

	}
	else {
		val = valueArr[0];
	}

	$('#groupIds').val(val);
}

function escapeHtml(unsafe) {
    return unsafe
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
 }
</script>

<div class="box_tab box_tab_thin left"  style="width:1100px;">
	<ul class="tab_menu" id="Tabs" style="background-color:transparent;">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
		<li class="last"><a href="#" onclick=";showHideTab('2');" id="tabLink2"><iwcm:text key="editor.tab.template" /></a></li>
		<li class=""><a href="#" onclick="showHideTab('3');" id="tabLink3"><iwcm:text key="editor.perex.group" /></a></li>
		<li class=""><a href="#" onclick="showHideTab('4');" id="tabLink4"><iwcm:text key="components.gallery.perex.filter.title" /></a></li>
		<iwcm:menu name="cmp_news">
			<li class=""><a href="#" onclick="loadComponentIframe();showHideTab('5');" id="tabLink5"><iwcm:text key="components.news.title"/></a></li>
		</iwcm:menu>
	</ul>
</div>


<iwcm:stripForm action="/admin/editor.do" class="newsForm form-horizontal" name="textForm" beanclass="sk.iway.iwcm.components.news.NewsActionBean">
	<div class="tab-pane toggle_content tab-pane-fullheight">
		<div class="tab-page" id="tabMenu1" style="display: block;">
			<div class="col-sm-12">

				<div class="form-group">
					<label for="groupIds" class="col-sm-3 control-label"><iwcm:text key="components.news.groupids"/></label>
					<div class="col-sm-6">
						<div class="input-group">
							<input type="text" name="groupIds" id="groupIds" class="form-control" value="${actionBean.groupIdsString}" />
							<input type="button" class="btn btn-primary" name="groupIdsSelect" value="<iwcm:text key="components.news.addgroup"/>" onClick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp", 500, 500);'>
						</div>
					</div>
				</div>

				<div class="form-group">
					<div class="col-sm-offset-3 col-sm-9">
						<div class="checkbox">
					    	<label>
					    		<stripes:checkbox name="alsoSubGroups" value="true"></stripes:checkbox> <iwcm:text key="components.calendar_news.zahrnut_podadresare"/>
					    	</label>
						</div>
					</div>
				</div>

				<div class="form-group">
					<label for="publishType" class="col-sm-3 control-label"><iwcm:text key="components.news.publishtype"/></label>
					<div class="col-sm-6">
						<stripes:select name="publishType" id="publishType" class="form-control">
							<option <c:if test="${actionBean.publishType eq 'NEW'}">selected="selected"</c:if> value="new"><iwcm:text key="components.news.PUBLISH_NEW"/></option>
							<option <c:if test="${actionBean.publishType eq 'OLD'}">selected="selected"</c:if> value="old"><iwcm:text key="components.news.PUBLISH_OLD"/></option>
							<option <c:if test="${actionBean.publishType eq 'ALL'}">selected="selected"</c:if> value="all"><iwcm:text key="components.news.PUBLISH_ALL"/></option>
							<option <c:if test="${actionBean.publishType eq 'NEXT'}">selected="selected"</c:if> value="next"><iwcm:text key="components.news.PUBLISH_NEXT"/></option>
							<option <c:if test="${actionBean.publishType eq 'VALID'}">selected="selected"</c:if> value="valid"><iwcm:text key="components.news.PUBLISH_VALID"/></option>
						</stripes:select>
					</div>
				</div>

				<div class="form-group">
					<label for="orderType" class="col-sm-3 control-label"><iwcm:text key="components.news.ordertype"/></label>
					<div class="col-sm-6">
						<stripes:select name="order" id="orderType" class="form-control">
							<option <c:if test="${actionBean.order eq 'PRIORITY'}">selected="selected"</c:if> value="priority"><iwcm:text key="components.news.ORDER_PRIORITY"/></option>
							<option <c:if test="${actionBean.order eq 'DATE'}">selected="selected"</c:if> value="date"><iwcm:text key="components.news.ORDER_DATE"/></option>
							<option <c:if test="${actionBean.order eq 'EVENT_DATE'}">selected="selected"</c:if> value="event_date"><iwcm:text key="components.news.ORDER_EVENT_DATE"/></option>
							<option <c:if test="${actionBean.order eq 'SAVE_DATE'}">selected="selected"</c:if> value="save_date"><iwcm:text key="components.news.ORDER_SAVE_DATE"/></option>
							<option <c:if test="${actionBean.order eq 'TITLE'}">selected="selected"</c:if> value="title"><iwcm:text key="components.news.ORDER_TITLE"/></option>
							<option <c:if test="${actionBean.order eq 'PLACE'}">selected="selected"</c:if> value="place"><iwcm:text key="components.news.ORDER_PLACE"/></option>
							<option <c:if test="${actionBean.order eq 'ID'}">selected="selected"</c:if> value="id"><iwcm:text key="components.news.ORDER_ID"/></option>
							<option <c:if test="${actionBean.order eq 'RATING'}">selected="selected"</c:if> value="rating"><iwcm:text key="components.news.RATING"/></option>
						</stripes:select>
					</div>
				</div>

				<div class="form-group">
					<div class="col-sm-offset-3 col-sm-9">
						<div class="checkbox">
					    	<label>
					    		<stripes:checkbox name="ascending" value="true"></stripes:checkbox> <iwcm:text key="components.news.asc"/>
					    	</label>
						</div>
					</div>
				</div>

				<div class="form-group">
					<div class="col-sm-offset-3 col-sm-9">
						<div class="checkbox">
					    	<label>
					    		<stripes:checkbox name="paging" id="paging" value="true"></stripes:checkbox> <iwcm:text key="components.news.paging"/>
					    	</label>
						</div>
					</div>
				</div>

				<div class="form-group pageSizeBox">
					<label for="pageSize" class="col-sm-3 control-label"><iwcm:text key="components.news.pageSize"/></label>
					<div class="col-sm-6">
						<stripes:text name="pageSize" id="pageSize" size="3" maxlength="3" class="form-control"></stripes:text>
					</div>
				</div>

				<div class="form-group pageSizeBox">
					<label for="pageSize" class="col-sm-3 control-label"><iwcm:text key="components.news.offset"/></label>
					<div class="col-sm-6">
						<stripes:text style="width: auto;" name="offset" id="offset" size="3" maxlength="3" class="form-control"></stripes:text>
					</div>
				</div>

				<div class="form-group">
					<div class="col-sm-offset-3 col-sm-9">
						<div class="checkbox">
					    	<label>
					    		<stripes:checkbox name="perexNotRequired" id="perexNotRequired" /> <iwcm:text key="components.news.noPerexCheck"/>
					    	</label>
						</div>
					</div>
				</div>

				<div class="form-group">
					<div class="col-sm-offset-3 col-sm-9">
						<div class="checkbox">
					    	<label>
					    		<stripes:checkbox name="loadData" value="true"></stripes:checkbox> <iwcm:text key="components.news.no_data"/>
					    	</label>
						</div>
					</div>
				</div>

				<div class="form-group">
					<div class="col-sm-offset-3 col-sm-9">
						<div class="checkbox">
							<label>
								<stripes:checkbox name="checkDuplicity" value="true"></stripes:checkbox> <iwcm:text key="components.news.check_duplicty"/>
							</label>
						</div>
					</div>
				</div>

				<div class="form-group">
					<div class="col-sm-offset-3 col-sm-9">
						<div class="checkbox">
							<label>
								<stripes:checkbox name="removeDefaultDocs" value="true"></stripes:checkbox> <iwcm:text key="components.news.remove_default_docs"/>
							</label>
						</div>
					</div>
				</div>

				<c:if test="${actionBean.canEdit}">
					<div class="form-group">
						<label for="contextClasses" class="col-sm-3 control-label"><iwcm:text key="components.news.contextClasses"/></label>
						<div class="col-sm-6">
							<stripes:text name="contextClasses" id="contextClasses" size="3" class="form-control"></stripes:text>
						</div>
					</div>
				</c:if>

				<div class="form-group">
					<label for="pageSize" class="col-sm-3 control-label"><iwcm:text key="components.news.cacheMinutes"/></label>
					<div class="col-sm-6">
						<stripes:text style="width: auto;" name="cacheMinutes" id="cacheMinutes" size="3" maxlength="3" class="form-control"></stripes:text>
					</div>
				</div>

			</div>
		</div>
		<div class="tab-page" id="tabMenu2">
			<c:if test="${actionBean.canEdit}">
				<p>
					<a class="btn btn-primary template-create" href="javascript:;"><iwcm:text key="components.news.new_template" /></a>
				</p>
			</c:if>

			<div class="templates">

				<c:forEach var="template" items="${actionBean.templates}">
						<div class="col-sm-6  template<c:if test="${template.selected}"> selected</c:if>" style="max-width: 420px; " data-key="${template.key}">
							<div class="image">
								<img src="${template.image}" class="img-thumbnail img-responsive">
							</div>
							<div class="tools">
								<div class="btn-group">
									<button type="button" class="btn btn-primary template-choose"><iwcm:text key="components.news.choose_template" /></button>
									<button type="button" class="btn btn-primary template-cancel"><iwcm:text key="components.news.cancel_template" /></button>
									<c:if test="${actionBean.canEdit}">
										<button type="button" class="btn btn-primary dropdown-toggle" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
											<i class="ti ti-chevron-down"></i>
											<span class="visually-hidden"><iwcm:text key="components.news.new_template" /></span>
										</button>
										<ul class="dropdown-menu">
											<li><a href="javascript:;" class="template-edit"><iwcm:text key="components.news.edit_template" /></a></li>
											<li><a href="javascript:;" class="template-duplicate"><iwcm:text key="components.news.duplicate_template" /></a></li>
											<li role="separator" class="divider"></li>
											<li><a href="javascript:;" class="template-delete"><iwcm:text key="components.news.delete_template" /></a></li>
										</ul>
									</c:if>
								</div>
							</div>
							<p>
								${template.keyShort}
							</p>
						</div>
				</c:forEach>
			</div>

			<input type="hidden" id="template" name="template" value="${actionBean.template.key}" />

		</div>
		<div class="tab-page" id="tabMenu3">
			<div class="col-xs-12">
				<table style="width: 100%;">
					<tr>
						<td valign="top" style="padding-top: 13px;"><iwcm:text key="components.news.perexGroup"/>:</td>
						<td>
							<%
								request.setAttribute("perexGroup", "," + actionBean.getPerexGroupString() + ",");
								//otestuj ci existuje nahrada za tuto stranku
								String forward = "/admin/spec/"+Constants.getInstallName()+"/perex_group.jsp";
								File fForward = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
								if (fForward.exists())
								{
									pageContext.include(forward);
								}
								else
								{
									pageContext.include("/admin/spec/perex_group.jsp");	//presuva skupiny do praveho stlpca podla perexGroupString
								}
								%>
						</td>
					</tr>
					<tr>
						<td valign="top" style="padding-top: 13px;"><iwcm:text key="components.news.perexGroupNot"/>:</td>
						<td>
							<%
								request.setAttribute("perexGroupNot", "," + actionBean.getPerexGroupNotString() + ",");
								//otestuj ci existuje nahrada za tuto stranku
								String forward2 = "/components/"+Constants.getInstallName()+"/news/perex_group_not.jsp";
								File fForward2 = new java.io.File(sk.iway.iwcm.Tools.getRealPath(forward));
								if (fForward2.exists())
								{
									pageContext.include(forward2);
								}
								else
								{
									pageContext.include("/components/news/perex_group_not.jsp");	//presuva skupiny do praveho stlpca podla perexGroupString
								}
							%>
						</td>
					</tr>
				</table>
			</div>
		</div>
		<div class="tab-page" id="tabMenu4">
			<div class="col-xs-12">
				<c:set var="fieldEnum" value="<%= FieldEnum.values() %>" />

				<div class="row filter filterTemplate filterNumber[]">
					<div class="col-xs-12">
						<label class="filterLabel" for="filter[]">Filter []:</label>
						<select name="filter[]" id="filter[]" class="filterSelect">
							<c:forEach items="${fieldEnum}" var="enumValue">
								<option data-field-type="${enumValue.fieldTypeString}">${enumValue}</option>
							</c:forEach>
						</select>

						<div class="operationsBox">
							<div class="operationsString operations">
								<select>
									<option value="eq">equals</option>
									<option value="sw">starts with</option>
									<option value="eq">ends with</option>
									<option value="co">contains</option>
								</select>
							</div>
							<div class="operationsInteger operationsDate operations">
								<select>
									<option value="gt">&gt;</option>
									<option value="ge">&ge;</option>
									<option value="eq" selected>=</option>
									<option value="le">&le;</option>
									<option value="lt">&lt;</option>

								</select>
							</div>

							<div class="operationsBoolean operations">
								<select>
									<option>true</option>
									<option>false</option>
								</select>
							</div>
						</div>

						<div class="value">
							<input type="text" name="filterValue" class="filterValue" />
						</div>

						<a class="btn btn-primary deleteFilter"><iwcm:text key="components.news.remove_filter" /></a>
					</div>
				</div>

				<c:forEach items="${actionBean.filter}" var="filter" varStatus="status">
					<c:set var="index" value="${status.index + 1}"></c:set>
					<c:set var="value" value=""></c:set>

					<c:forEach items="${filter.value}" var="val" varStatus="varStatus">
						<c:choose>
							<c:when test="${varStatus.first}"><c:set var="value" value="${val}"></c:set></c:when>
							<c:otherwise><c:set var="value" value="${value}, ${val}"></c:set>	</c:otherwise>
						</c:choose>
					</c:forEach>

					<div class="row filter filterRow filterNumber${index}">
						<div class="col-xs-12">
							<label class="filterLabel" for="filter${index}">Filter ${index}:</label>
							<select name="filter${index}" id="filter${index}" class="filterSelect">

								<c:set var="fieldType" value=""/>
								<c:forEach items="${fieldEnum}" var="enumValue">
									<c:set var="key" value="${fn:substring(filter.key, 0, fn:indexOf(filter.key, '_'))}" />
									<c:set var="fieldType" value="${fn:replace(enumValue, '_', '')}"></c:set>
									<option <c:if test="${key eq fieldType}">selected="selected"</c:if> data-field-type="${enumValue.fieldTypeString}">${enumValue}</option>
								</c:forEach>
							</select>

							<div class="operationsBox">
								<div class="operationsString operations">
									<select>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'eq')}">selected="selected"</c:if> value="eq">equals</option>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'sw')}">selected="selected"</c:if> value="sw">starts with</option>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'ew')}">selected="selected"</c:if> value="ew">ends with</option>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'co')}">selected="selected"</c:if> value="co">contains</option>
									</select>
								</div>
								<div class="operationsInteger operationsDate operations">
									<select>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'gt')}">selected="selected"</c:if> value="gt">></option>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'ge')}">selected="selected"</c:if> value="ge">&ge;</option>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'eq')}">selected="selected"</c:if> value="eq">=</option>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'le')}">selected="selected"</c:if> value="le">&le;</option>
										<option <c:if test="${fn:containsIgnoreCase(filter.key, 'lt')}">selected="selected"</c:if> value="lt"><</option>

									</select>
								</div>

								<div class="operationsBoolean operations">
									<select>
										<option <c:if test="${value eq 'true'}">selected="selected"</c:if>>true</option>
										<option <c:if test="${value eq 'false'}">selected="selected"</c:if>>false</option>
									</select>
								</div>
							</div>

							<div class="value">
								<input type="text" name="filterValue" class="filterValue" value="${value}" />
							</div>

							<a class="btn btn-primary deleteFilter"><iwcm:text key="components.news.remove_filter" /></a>
						</div>
					</div>
				</c:forEach>

				<div class="row">
					<div class="col-12">
						<a class="btn btn-primary addFilter"><iwcm:text key="components.news.add_filter" /></a>
					</div>
				</div>
			</div>
		</div>
		<div class="tab-page tab-page-iframe" style="padding-top:0px;" id="tabMenu5">
			<iframe id="componentIframeWindowTab" frameborder="0" name="componentIframeWindowTab" width="100%" src="/admin/iframe_blank.jsp"></iframe>
		</div>
	</div>
</iwcm:stripForm>

<c:if test="${actionBean.canEdit}">
	<div id="templateModal" class="modal fade" tabindex="-1" role="dialog">
		<div class="vertical-alignment-helper">
			<div class="modal-dialog vertical-align-center">
				<div class="modal-content" style="width: 762px;">
					<div class="modal-header">
						<button type="button" class="close" data-bs-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
						<h4 class="modal-title"><iwcm:text key="components.news.template" /></h4>
					</div>
					<div class="modal-body">
						<div class="form-horizontal">
						  <div class="form-group">
							<label for="key" class="col-12 control-label"><iwcm:text key="components.news.template_title" /></label>
							<div class="col-12">
							  <input type="text" class="form-control" id="keyShort" name="template.keyShort" id="title" placeholder="<iwcm:text key="components.news.template_title" />">
								  <input type="hidden" id="keyBeforeSave" name="template.keyBeforeSave" />
							</div>
						  </div>
						  <div class="form-group">
							<label for="value" class="col-12 control-label"><iwcm:text key="components.news.template_html" /></label>
							<div class="col-12">
							  <textarea class="form-control" wrap="off" id="templateValue" name="template.value" rows="10" placeholder="<iwcm:text key="components.news.template_html" />"></textarea>
							</div>
						  </div>
						  <div class="form-group">
							<label for="pagingValue" class="col-12 control-label"><iwcm:text key="components.news.template_paging_html" /></label>
							<div class="col-12">
							  <textarea class="form-control" wrap="off" id="pagingValue" name="template.pagingValue" rows="4" placeholder="<iwcm:text key="components.news.template_paging_html" />"></textarea>
							</div>
						  </div>

						  <div class="form-group">
							<label for="pagingValue" class="col-12 control-label"><iwcm:text key="components.news.template_paging_position" /></label>
							<div class="col-12">
							  	<label class="checkbox-inline">
							  		<input type="checkbox" id="pagingPositionPred" name="template.pagingPosition" value="before"> <iwcm:text key="components.news.template_paging_position_before" />
								</label>
								<br/>
								<label class="checkbox-inline">
									<input type="checkbox" id="pagingPositionZa" name="template.pagingPosition" value="after"> <iwcm:text key="components.news.template_paging_position_after" />
								</label>
							</div>
						  </div>

						</div>
					</div>
					<div class="modal-footer">
						<button type="button" class="btn btn-default" data-bs-dismiss="modal"><iwcm:text key="javascript.datepicker.closeText" /></button>
						<button type="button" class="btn btn-primary template-save"><iwcm:text key="admin.useredit.categories.save" /></button>
					</div>
				</div>
			</div>
		</div>
	</div>
</c:if>

<div class="templateTemplate">
		<div class="col-sm-6 template" style="max-width: 420px; ">
			<div class="image">
				<img src="/components/news/images/placeholder-380-200.png" class="img-thumbnail img-responsive">
			</div>
			<div class="tools">
				<div class="btn-group">
					<button type="button" class="btn btn-primary template-choose"><iwcm:text key="components.news.choose_template" /></button>
					<button type="button" class="btn btn-primary template-cancel"><iwcm:text key="components.news.cancel_template" /></button>
					<c:if test="${actionBean.canEdit}">
						<div class="dropdown">
							<button type="button" class="btn btn-primary dropdown-toggle" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
								<i class="ti ti-chevron-down"></i>
								<span class="visually-hidden"><iwcm:text key="components.news.new_template" /></span>
							</button>
							<ul class="dropdown-menu">
								<li><a href="javascript:;" class="template-edit"><iwcm:text key="components.news.edit_template" /></a></li>
								<li><a href="javascript:;" class="template-duplicate"><iwcm:text key="components.news.duplicate_template" /></a></li>
								<li role="separator" class="divider"></li>
								<li><a href="javascript:;" class="template-delete"><iwcm:text key="components.news.delete_template" /></a></li>
							</ul>
						</div>
					</c:if>
				</div>
			</div>
			<p>
				<span class="keyShort"></span>
			</p>
		</div>
</div>

<div id="context-menu" class="context-menu">
	<ul class="dropdown-menu" role="menu">
		<li class="dropdown-submenu velocity">
			<a tabindex="-1" href="javascript:;"><iwcm:text key="components.news.cycles" /></a>
			<ul class="dropdown-menu">
			<c:forEach items="${actionBean.velocityProperties}" var="property">
				<li>
				    <a href="javascript:;" data-value="${property.value}">${property.title} - ${property.description}</a>
				</li>
			</c:forEach>
			</ul>
		</li>
		<li class="dropdown-submenu doc">
			<a tabindex="-1" href="javascript:;"><iwcm:text key="components.news.properties_web_page" /></a>
			<ul class="dropdown-menu">
			<c:forEach items="${actionBean.docDetailsProperties}" var="property">
				<li>
				    <a href="javascript:;" data-value="${property.value}">${property.title} - ${property.description}</a>
				</li>
			</c:forEach>
			</ul>
		</li>
		<li class="dropdown-submenu group">
			<a tabindex="-1" href="javascript:;"><iwcm:text key="components.news.properties_directory" /></a>
			<ul class="dropdown-menu">
			<c:forEach items="${actionBean.groupDetailsProperties}" var="property">
				<li>
				    <a href="javascript:;" data-value="${property.value}">${property.title} - ${property.description}</a>
				</li>
			</c:forEach>
			</ul>
		</li>

		<li class="dropdown-submenu paging">
			<a tabindex="-1" href="javascript:;"><iwcm:text key="components.news.paging" /></a>
			<ul class="dropdown-menu">
			<c:forEach items="${actionBean.pagingProperties}" var="property">
				<li>
				    <a href="javascript:;" data-value="${property.value}">${property.title}<c:if test="${not empty property.description}"> - ${property.description}</c:if></a>
				</li>
			</c:forEach>
			</ul>
		</li>
	</ul>
</div>

<jsp:include page="/components/bottom.jsp"/>
