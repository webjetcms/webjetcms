<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><iwcm:checkLogon admin="true" perms="menuWebpages"/><%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String tabs = Tools.getStringValue(Tools.getRequestParameter(request, "tabs"), "");
if (Tools.isEmpty(tabs)) {
	out.print("Nie je definovaný žiaden blok");
	return;
}

List<String> tabsFiltered = new ArrayList<String>();
for (String tab : Tools.getTokens(tabs, "|"))
{
	if (tab.indexOf("cke_editable")!=-1) break;

	tabsFiltered.add(tab);
}
request.setAttribute("tabs", tabsFiltered);
boolean isCloud = "cloud".equals(Constants.getInstallName());
int stlpec = 1;

%>
<%@ include file="/admin/skins/webjet8/layout_top_iframe.jsp" %>

<link rel="stylesheet" type="text/css" href="/admin/skins/webjet8/ckeditor/dist/plugins/webjetfloatingtools/jquery.elementBox.css">

<style type="text/css">
	body { overflow: hidden; background-image: url('/admin/skins/webjet8/assets/global/img/wj/popup_bg_header.png') !important; }
	div.toggle_content {padding: 0px;}

	.form-group .checkbox {margin: 0;}
	.form-group .checkbox label {margin-bottom: 0px;}
	.form-group .height,
	.form-group .width {display: none;}
	.form-group .checker {margin-top: -7px !important;}

	.minicolors-theme-default .minicolors-input { padding-left: 27px; }
</style>
<script type="text/javascript" src="/admin/skins/webjet8/ckeditor/dist/plugins/webjetfloatingtools/jquery.elementBox.js"></script>

<script type="text/javascript">
$(function(){
	$('.elementBox').elementBox({
		texts: {
			margin: '<iwcm:text key="editor.element_box.margin" />',
			border: '<iwcm:text key="editor.element_box.border" />',
			padding: '<iwcm:text key="editor.element_box.padding" />',
			none: '<iwcm:text key="editor.div_properties.none" />',
			dotted: '<iwcm:text key="editor.div_properties.dotted" />',
			dashed: '<iwcm:text key="editor.div_properties.dashed" />',
			solid: '<iwcm:text key="editor.div_properties.solid" />',
			valueInputLabel: '<iwcm:text key="editor.div_properties.valueInputLabel" />',
			borderSelectLabel: '<iwcm:text key="editor.div_properties.borderSelectLabel" />',
			colorInputLabel: '<iwcm:text key="editor.div_properties.colorInputLabel" />',
			allInputLabel: '<iwcm:text key="editor.div_properties.allInputLabel" />'
		}
	});

	widthHeightOwnChanged()
	$('.widthOwn, .heightOwn').change(widthHeightOwnChanged);
});

function widthHeightOwnChanged()
{
	$('.div-properties-item').each(function(){
		var tabPane = $(this);

		if (tabPane.find('.widthOwn').is(':checked')) {
			tabPane.find('.width').fadeIn();
		}
		else {
			tabPane.find('.width').val('').hide();
		}

		if (tabPane.find('.heightOwn').is(':checked')) {
			tabPane.find('.height').fadeIn();
		}
		else {
			tabPane.find('.height').val('').hide();
		}
	});
}

function OkClick()
{
	if (validate()) {
		return true;
	}

	return false;
}

function getData()
{
	var result = [];
	$('.div-properties-item').each(function(){
		var el = $(this);

		var cssClass = el.find('.cssClass').val();
		var htmlId = el.find('.htmlId').val();
		var backgroundColor = el.find('.backgroundColor').val();
		var backgroundImage = el.find('.backgroundImage').val();
		var backgroundAttachment = el.find('.backgroundAttachment').val();

		var width = el.find('.width').val();
		var height = el.find('.height').val();

		var data = el.find('.elementBox').elementBox('values');

		result.push({
			cssClass: cssClass,
			htmlId: htmlId,
			width: width,
			height: height,
			backgroundColor: backgroundColor,
			backgroundImage: backgroundImage,
			backgroundAttachment: backgroundAttachment,
			data: data
		})
	});

	//console.log("GET data");
	//console.log(result);

	return result;
}

function setData(arr)
{
	//console.log("Set data");
	//console.log(arr);

	$.each(arr, function(i, v) {
		var tab = $('#tab' + (i + 1));
		var color = v.backgroundColor;
		var image = v.backgroundImage;

		//console.log("tab", tab);
		//console.log("data", v);

		if (typeof color != 'undefined' && color.indexOf('#') != -1) {
			if (color.length == 4) {
				color = color + color.substring(1);
			}
			color = convertHex(color, 100);
		}

		if (image.indexOf('url') != -1) {
			image = image.replace(/(url\(|\)|'|")/gi, '');
		}

		if (typeof v.width != 'undefined' && v.width != '') {
			tab.find('.widthOwn').prop('checked', true);
			$.uniform.update();
			tab.find('.width').val(v.width);
		}
		else {
			tab.find('.widthOwn').prop('checked', false);
			$.uniform.update();
		}

		if (typeof v.height != 'undefined' && v.height != '') {
			tab.find('.heightOwn').prop('checked', true);
			$.uniform.update();
			tab.find('.height').val(v.height);
		}
		else {
			tab.find('.heightOwn').prop('checked', false);
			$.uniform.update();
		}

		widthHeightOwnChanged();

		tab.find('.cssClass').val(v.cssClass);
		tab.find('.htmlId').val(v.htmlId);

		tab.find('.backgroundAttachment').val(v.backgroundAttachment);

		if (color != "") {
			tab.find('.backgroundColor').minicolors('value', rgbaToHexObject(color));
		}
		else {
			tab.find('.backgroundColor').val("");
		}

		tab.find('.backgroundImage').val(image);
		if (image != null && image != "") tab.find('img').attr("src", image);
		tab.find('.elementBox').elementBox('values', v);
	});
}

function convertHex(hex,opacity){
    hex = hex.replace('#','');
    var r = parseInt(hex.substring(0,2), 16);
    var g = parseInt(hex.substring(2,4), 16);
    var b = parseInt(hex.substring(4,6), 16);

    var result = 'rgba('+r+','+g+','+b+','+opacity/100+')';
    return result;
}

function rgbaToHexObject(rgba) {

	if (typeof rgba == "undefined" || rgba == '') {
		return '';
	}

	if (rgba.indexOf('rgb') != -1 && rgba.indexOf('rgba') == -1) {
		rgba = rgba.replace(')', ', 1)').replace('rgb', 'rgba');
	}

	function rgbaOpacity(rgba) {
		var result = $.trim(rgba.replace(/^.*,(.+)\)/,'$1'));

		return result;
	}

	function rgb2hex(rgb){
		rgb = rgb.match(/^rgba?[\s+]?\([\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?,[\s+]?(\d+)[\s+]?/i);
		return (rgb && rgb.length === 4) ? "#" + ("0" + parseInt(rgb[1],10).toString(16)).slice(-2) + ("0" + parseInt(rgb[2],10).toString(16)).slice(-2) + ("0" + parseInt(rgb[3],10).toString(16)).slice(-2) : '';
	}

	var result = {
			color: rgb2hex(rgba),
			opacity: rgbaOpacity(rgba)
	};

	return result;
}

function validate()
{
	return true;
}
</script>
<%
boolean wasCkeRootElement = false;
int i = 0;
%>

<style type="text/css">
div.div-properties-list { height: 440px; background-color: #f5f5f5; overflow: auto; }
div.div-properties-item { border-bottom:2px solid blue; margin-bottom: 10px; }
</style>

<div class="div-properties-list">
	<iwcm:forEach items="${tabs}" var="item" varStatus="status" type="java.lang.String">
		<c:set var="i" value="${status.index + 1}"></c:set>
			<%
			String title = prop.getText("editor.div_properties." + item);
			if (title.startsWith("editor.div_properties.")) {
				title = item;
			}

			// zjednodusene pomenovanie v pripade cloudu
			if(isCloud)
			{
				if ("section content".equals(title)) title=prop.getText("editor.div_properties.vonkajsi_blok");
				else if ("section container".equals(title)) title=prop.getText("editor.div_properties.vnutorny_blok");
				else if ("row".equals(title))	title=prop.getText("editor.div_properties.riadok");
				else if(title.contains("col-"))
				{
						title= prop.getText("editor.div_properties.stlpec")+" "+stlpec;
						stlpec++;
				}
				else if(title.contains("SECTION")) title=prop.getText("editor.div_properties.section");
				else if(title.contains("DIV")) title=prop.getText("editor.div_properties.div");
				else if(title.contains("container")) title=prop.getText("editor.div_properties.container");
			}

			if (title == null) title = "";
			title = Tools.replace(title, " cke_floatingtools_selected", "");
			title = Tools.replace(title, "cke_floatingtools_selected", "");
			request.setAttribute("title", title);

			String liClass = "";
			if (i++==0) liClass="active";
			else if (wasCkeRootElement) liClass="hidden";

			request.setAttribute("liClass", liClass);
			%>

		<div id="tab${i}" class="div-properties-item">
			<div class="container">
				<form name="div_properties${i}" div_properties${i}">

					<h4><c:if test="${i eq 1}"><iwcm:text key="editor.div_properties.top_element"/></c:if><c:if test="${i > 1}"><iwcm:text key="editor.div_properties.element"/></c:if> (${title})</h4>

					<div class="row">
						<div class="form-group col-xs-2">
							<img class="img-thumbnail" id="backgroundImageImg${i}" onclick="openImageDialogWindow('div_properties${i}', 'backgroundImage${i}', '')" src="/components/news/admin_imgplaceholder.png" style="cursor: pointer;"/>
						</div>

						<div class="form-group col-xs-6">
					    	<label for="bgImage${i}"><iwcm:text key="editor.div_properties.background_image" />:</label>

					    	<div class="input-group">
					    		<input type="text" class="form-control col-md-6 backgroundImage" name="backgroundImage${i}" id="backgroundImage${i}" placeholder="<iwcm:text key="editor.div_properties.background_image" />">
							  	<span class="input-group-addon btn green" onclick="openImageDialogWindow('div_properties${i}', 'backgroundImage${i}', '')" >
			              		<i class="fa fa-picture-o"></i>
			              	</span>
				         </div>
					  	</div>

						<div class="form-group col-xs-4">
					    	<label for="bgColor${i}"><iwcm:text key="editor.div_properties.background_color" />:</label>
					    	<input type="text" name="bgColor" class="form-control colorpicker-rgba backgroundColor" id="backgroundColor${i}" placeholder="<iwcm:text key="editor.div_properties.background_color" />">

					    	<br/>
					    	<div style="text-align: right">
					    		<a href="#div-properties-advanced${i}" data-toggle="collapse"><iwcm:text key="editor.div_properties.advanced"/></a>
					    	</div>
					  	</div>
				  	</div>

					<div id="div-properties-advanced${i}" class="collapse">

						<div class="row" <% if("cloud".equals(Constants.getInstallName())) out.print("style='display:none;'");%>>
							<div class="form-group col-xs-6">
								<label for="cssClass${i}"><iwcm:text key="editor.div_properties.css_class" />:</label>
								<input type="text" name="cssClass" class="form-control cssClass" id="cssClass${i}" placeholder="<iwcm:text key="editor.div_properties.css_class" />">
							</div>

							<div class="form-group col-xs-6">
								<label for="htmlId${i}"><iwcm:text key="editor.div_properties.html_id" />:</label>
								<input type="text" name="htmlId" class="form-control htmlId" id="htmlId${i}" placeholder="<iwcm:text key="editor.div_properties.html_id" />">
							</div>
						</div>
						<div class="row">

						  	<div class="form-group col-xs-4">
						    	<label for="backgroundAttachment${i}"><iwcm:text key="editor.div_properties.background_position" />:</label>

						    	<div class="row">
							    	<div class="form-group col-xs-12">
							    		<div class="checkbox">
										    <select id="backgroundAttachment${i}" name="backgroundAttachment" class="backgroundAttachment">
										    	<option value=""><iwcm:text key="editor.div_properties.background_position.scroll"/></option>
										    	<option value="fixed"><iwcm:text key="editor.div_properties.background_position.fixed"/></option>
										    </select>
										  </div>
							    	</div>
						    	</div>
						  	</div>

							<div class="form-group col-xs-4">
						    	<label for="width${i}"><iwcm:text key="editor.div_properties.width" />:</label>

						    	<div class="row">
							    	<div class="form-group col-xs-7">
							    		<div class="checkbox">
										    <label>
										      <input class="widthOwn" type="checkbox"> <iwcm:text key="editor.div_properties.width_own" />
										    </label>
										  </div>
							    	</div>
							    	<div class="form-group col-xs-5">
						    			<input type="text" name="width" class="form-control width" id="width${i}" placeholder="<iwcm:text key="editor.div_properties.width" />">
						    		</div>
						    	</div>
						  	</div>

						  	<div class="form-group col-xs-4">
						    	<label for="height${i}"><iwcm:text key="editor.div_properties.height" />:</label>

						    	<div class="row">
							    	<div class="form-group col-xs-7">
							    		<div class="checkbox">
										    <label>
										      <input class="heightOwn" type="checkbox"> <iwcm:text key="editor.div_properties.height_own" />
										    </label>
										  </div>
							    	</div>
							    	<div class="form-group col-xs-5">
							    		<input type="text" name="height" class="form-control height" id="height${i}" placeholder="<iwcm:text key="editor.div_properties.height" />">
							    	</div>
						    	</div>
						  	</div>

						</div>

						<div class="elementBox"></div>
					</div>
				</form>
			</div>
		</div>
	</iwcm:forEach>
</div>
<%@ include file="/admin/skins/webjet8/layout_bottom_iframe.jsp" %>
