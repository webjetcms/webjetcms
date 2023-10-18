
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.doc.GroupsDB,sk.iway.iwcm.doc.GroupDetails,java.util.List,sk.iway.iwcm.io.IwcmFile,sk.iway.iwcm.gallery.*,org.apache.struts.util.ResponseUtils, org.apache.commons.codec.binary.Base64"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="bean"
	uri="/WEB-INF/struts-bean.tld"%><%@
taglib prefix="html"
	uri="/WEB-INF/struts-html.tld"%><%@
taglib prefix="logic"
	uri="/WEB-INF/struts-logic.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<iwcm:checkLogon admin="true" perms="cmp_app-impress_slideshow"/>
<%
	request.setAttribute("cmpName", "app-impress_slideshow");
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams)) {
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String jspFileName = "/components/app-impress_slideshow/news.jsp";

	if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("perexGroup", ""))))
	{
		String[] perexGroupArray = GalleryDB.convertPerexGroupString(ResponseUtils.filter(pageParams.getValue("perexGroup", "")));
		String perexGroupString = GalleryDB.convertPerexGroupString(perexGroupArray);	//aby sa pri editaci skupiny spravne popresuvali, potrebujem
																										//vstup vo specialnom formate ","+groupPerex+","
		request.setAttribute("perexGroup", perexGroupString);
	}
	String style = pageParams.getValue("style", "06");
	request.setAttribute("titleKey",
			"components.app-impress_slideshow.title");
	request.setAttribute("descKey",
			"components.app-impress_slideshow.desc");
	request.setAttribute("iconLink",
			"/components/app-impress_slideshow/editoricon.png");
%>
<jsp:include page="/components/top.jsp"/>
<%-- JSON Editor Script --%>
<link type="text/css" rel="stylesheet" media="screen" href="/components/json_editor/editor_style.css" />
<script type="text/javascript" src="/components/json_editor/editor.js"></script>
<script type="text/javascript" src="/components/json_editor/editor_functions.js"></script>

<%-- END of JSON Editor Script --%>
<script type="text/javascript" src="/components/_common/custom_styles/custom_text_style.jsp"></script>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px !important; }
	td.main { padding: 0px; }
</style>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>

<script type='text/javascript'>
//<![CDATA[
$(document).ready(function(){
	if($.browser.msie)
	{
		$img = $("label img");

		$img.click(function(e)
		{
			$("#" + $(this).parent().attr("for"))
				.change()
				.click();
		});
	}
	loadComponentIframe();
	showHideTab('2');

});

function setParentGroupId(returnValue)
{
	//var returnValue = showModalDialog("<iwcm:cp/>/admin/grouptree.jsp", "", "dialogHeight: 500px; resizable: Yes;") + " ";
	if (returnValue.length > 15)
	{
		var groupid = returnValue.substr(0,15);
		var groupname = returnValue.substr(15);
		groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
		if (document.textForm.groupIds.value=="")
		{
			document.textForm.groupIds.value = groupid;
		}
		else
		{
			document.textForm.groupIds.value = document.textForm.groupIds.value + "+"+groupid;
		}
	}
}

function setRootGroup(returnValue)
{
	//var returnValue = showModalDialog("<iwcm:cp/>/admin/grouptree.jsp", "", "dialogHeight: 500px; resizable: Yes;") + " ";
	if (returnValue.length > 15)
	{
		var groupid = returnValue.substr(0,15);
		var groupname = returnValue.substr(15);
		groupid = groupid.replace(/^[ \t]+|[ \t]+$/gi, "");
		document.MenuULForm.rootGroupId.value = groupid;
	}
}

function showHelp(select)
{
	size = select.options.length;
	for (i=0; i<size; i++)
	{
		el = document.getElementById("help_"+i);
		if (el!=null)
		{
			if (i==select.selectedIndex)
			{
				el.style.display = "block";
			}
			else
			{
				el.style.display = "none";
			}
		}
	}
}

function getIncludeText()
{
		// set Data From JSON Editor
	editorItemsList.setDataFromDom($("#editorWrapper .item"));

	var jspFileName = "<%=jspFileName%>";
	var groupIds = document.textForm.groupIds.value;
	var orderType = document.textForm.orderType.value;
	var asc = "no";
	if ($("#order").val() == "yes") {
		asc = "yes";
	}

	 var imageWidth = $('#imageWidth').prop('value');
	 var imageHeight = $('#imageHeight').prop('value');

	var publishType = "new";
	var nivoSliderHeight = document.textForm.nivoSliderHeight.value;
	var includeText = "!INCLUDE(" + jspFileName + ", groupIds=" + groupIds
			+ ", orderType=" + orderType
			+ ", asc=" + asc
			+ ", imageWidth=" + imageWidth
			+ ", imageHeight=" + imageHeight
			+ ", publishType=" + publishType
			+", editorData="+ EditorItemsList.encodeJSONData(editorItemsList.getData())
			+ ", nivoSliderHeight="+ nivoSliderHeight+")!";
	return includeText;
}


	function doOKNewsFCK() {
		oEditor.FCK.InsertHtml(getIncludeText());
		return true;
	} // End function

	function checkChanges() {
		return true;
	}

	function Cancel() {
		if (checkChanges() == false)
			return false;
		return true;
	}

	function Ok() {
		if (doOKNewsFCK())
			return true;
		else
			return false;
	} // End function

	if (isFck) {
	} else {
		resizeDialog(570, 750);
	}

	function loadComponentIframe() {

	}
	//]]>
</script>

<style type="text/css">
.styleBox {
	display: block;
	position: relative;
	width: 382px;
	height: 160px;
	background: #fff;
	margin: 3px;
	padding: 10px;
	border: 1px solid #bcbcbc;
	border-radius: 4px;
}

* HTML BODY .styleBox {
	width: 402px;
	height: 180px;
}

.boxes .styleBox {
	height: 110px;
}

* HTML BODY .boxes .styleBox {
	height: 130px;
}

.styleBox .radioSelect {
	position: absolute;
	left: 0;
	top: 0;
	text-align: left;
	width: 100%;
	height: 100%;
}

.styleBox .radioSelect input {
	position: absolute;
	left: 10px;
	top: 80px;
	border: 0px none;
}

.boxes .styleBox .radioSelect input {
	top: 55px;
}

.styleBox img {
	position: absolute;
	top: 10px;
	left: 42px;
}

div.colBox {
	display: block;
	float: left;
	margin: 10px 10px 0 0;
	padding: 0;
	width: 408px;
	overflow: auto;
}

div.clearer {
	width: 100%;
	clear: both;
	height: 0;
	line-height: 0;
	font-size: 0;
	display: block;
	visibility: hidden;
}

select {
	width: 300px;
}

input {
	padding-left: 4px;
}

.rozsirene tr td{
border-bottom:0px !important;
padding: 3px !important;
}
.rozsirene label{
padding: 0px !important;
}

.rozsireneLink{
cursor:pointer;
background-image:url(/components/_common/custom_styles/images/icon_settings.png);
display:inline-block; width:20px; height:20px;
margin-bottom: -5px;
margin-right:7px;
}

.rozsireneLink:hover{
background-image:url(/components/_common/custom_styles/images/icon_settings_hover.png);
}
</style>





<script type="text/javascript">



function perexImageBlur(input)
{
	$(input).closest('.item').find(".imageDiv").css("background-image", "url(/thumb"+input.value+"?w=100&h=100&ip=5)");
}
//For fields configuration
var editorItemFields = {
image: {
    title: "Image",
    type: "image",
    classes: "editorLeft"
},
title: {
    title: '<iwcm:text key="editor.create_table.caption" />',
    type: "text",
    classes: "editorRight",
},
subtitle: {
    title: '<iwcm:text key="editor.subtitle" />',
    type: "textArea",
    classes: "editorRight"
},
  headingColor: {
    title: '<iwcm:text key="components.app-impress_slideshow.headingColor" />',
    type: "colorpicker",
    classes: "editorRight colorpicker",
    onkeydown:""
},
  subheadingColor: {
    title: '<iwcm:text key="components.app-impress_slideshow.subHeadingColor" />',
    type: "colorpicker",
    classes: "editorRight colorpicker",
    onkeydown:""
}, backgroundColor: {
    title: '<iwcm:text key="components.app-impress_slideshow.backgroundColor" />',
    type: "colorpicker",
    classes: "editorRight colorpicker",
    onkeydown:""
},

redirectUrl: {
    title: '<iwcm:text key="components.news.redirectAfterClick" />',
    description: "Zadajte adresu stranky, alebo kliknite na ikonu na konci riadku pre jej vyber",
    type: "conditionalText",
    classes: "editorRight"
},
customStyleHeading: {
    title: '',
    description: "Custom",
    type: "textStyle",
    parentField:"title",
    classes: "editorRight"
},
customStyleSubHeading: {
    title: '',
    description: "Custom",
    type: "textStyle",
    parentField:"subtitle",
    classes: "editorRight"
}


};

//Form fields to edit
var editorItemsToUse = [
"image",
"title",
"subtitle",
"redirectUrl",
"headingColor",
"subheadingColor",
"backgroundColor",
"customStyleHeading",
"customStyleSubHeading"
];

</script>

<%
//JSON editor
String editorData = pageParams.getValue("editorData", "W10=");
boolean isJSON = pageParams.getBooleanValue("isJSON", true);


%>
<script type="text/javascript">

var inputData = EditorItemsList.decodeJSONData('<%=editorData %>');

var editorItemsList = new EditorItemsList(inputData, editorItemsToUse);
var renderer = new EditorRenderer("#editorWrapper", editorItemsList, editorItemFields, editorItemsToUse, "EditorItemFieldRenderer");


</script>

<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#"
			onclick="showHideTab('1');" id="tabLink1"><iwcm:text
					key="components.news.styleAndSettings" /></a></li>
		<li class="last"><a href="#"
			onclick="loadComponentIframe();showHideTab('2');" id="tabLink2"><iwcm:text
					key="components.news.items" /></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content"
	style="height: 840px; overflow: auto; width: 990px;">
	<div class="tab-page" id="tabMenu1" style="display: block; height: 500px;">
		<br>
		<div>
			<html:form action="/admin/editor.do" onsubmit="return false" name="textForm" type="sk.iway.iwcm.editor.EditorForm">
			<div>

							<div style="display: none;">
								<div class="col-xs-4"><label for="groupIds"><iwcm:text
											key="components.news.groupids" /></label>:</div>
								<div class="col-xs-8"><input type="text" name="groupIds" id="groupIds"
										value="<%=ResponseUtils.filter(pageParams.getValue("groupIds","-1"))%>" />
								</div>
							</div>

							<div class="form-group clearfix">
								<div class="col-xs-4"><label for="orderType"><iwcm:text
											key="components.news.ordertype" /></label>:</div>
								<div class="col-xs-8"><select id="orderType" name="orderType">
										<option value="priority"><iwcm:text
												key="components.news.ORDER_PRIORITY" /></option>
										<option value="title"><iwcm:text
												key="components.news.ORDER_TITLE" /></option>
										<option value="id"><iwcm:text
												key="components.news.ORDER_ID" /></option>
								</select>
								</div></div>
							<div class="form-group clearfix">
								<div class="col-xs-4"><label for="order"><iwcm:text
											key="components.news.order" /></label>:</div>
								<div class="col-xs-8"><select id="order" name="asc">
										<option value="yes"><iwcm:text
												key="components.inquiry.orderAsc" /></option>
										<option value="no"><iwcm:text
												key="components.inquiry.orderDesc" /></option>
								</select></div>
							</div>
							<div class="form-group clearfix">
								<div class="col-xs-4"><label for="nivoSliderHeight"><iwcm:text
											key="editor.table.height" /></label>:</div>
								<div class="col-xs-8"><input type="text" id="nivoSliderHeight"
									name="nivoSliderHeight" size="5" maxlength="5"
									value="<%=pageParams.getIntValue("nivoSliderHeight", 400)%>">
									<iwcm:text key="components.forms.file_restrictions.points" /></div>
							</div>



									     <div class="form-group clearfix">
		   	<div class="col-xs-4">
					 <iwcm:text key="components.app-impress_slideshow.imageWidth"/>:&nbsp;
				</div>
				<div class="col-xs-8">
					<input type="text" id="imageWidth" size="5" maxlength="5" value="<%=pageParams.getValue("imageWidth", "400")%>"> <iwcm:text key="components.forms.file_restrictions.points" />
				</div>
		   </div>

		   		   <div class="form-group clearfix">
		   	<div class="col-xs-4">
					 <iwcm:text key="components.app-impress_slideshow.imageHeight"/>:&nbsp;
				</div>
				<div class="col-xs-8">
					<input type="text" id="imageHeight" size="5" maxlength="5" value="<%=pageParams.getValue("imageHeight", "300")%>"> <iwcm:text key="components.forms.file_restrictions.points" />
				</div>
		   </div>
		   </div>
						</html:form>

		</div>
	</div>
	<div class="tab-page" id="tabMenu2" style="height: 800px; width: 800px;">
								<div class="rozsirene" style="display:none"></div>

		<div id="editorWrapper" class="editorWrapper collapsable"
			data-collapse="<iwcm:text key="components.json_editor.collapse" />"
			data-expand="<iwcm:text key="components.json_editor.expand" />">
		</div>

		<input type="button" id="addItem" class="button50 button50grey"
			value="<iwcm:text key="components.slider.addItem"/>">
		<!-- <input type="button" id="saveEditor" value="Save"> -->

	</div>
	</div>
</div>

<script type="text/javascript">
//inicializacia poloziek
if (isFck)
{
	var oEditor = window.parent.InnerDialogLoaded();
	var FCK		= oEditor.FCK ;
}
<%if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue(
					"orderType", "")))) {%>
		document.textForm.orderType.value = "<%=ResponseUtils.filter(pageParams.getValue("orderType",""))%>";
<%}
			if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue(
					"asc", "")))) {
				String order = "yes";
				if ("no".equals(pageParams.getValue("asc", "")))
					order = "no";%>
		document.textForm.order.value = "<%=order%>";
<%}%>

</script>

<jsp:include page="/components/bottom.jsp" />
