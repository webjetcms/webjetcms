
<% sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");%>
<%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.doc.GroupsDB,sk.iway.iwcm.doc.GroupDetails,java.util.List,sk.iway.iwcm.io.IwcmFile,sk.iway.iwcm.gallery.*,sk.iway.iwcm.tags.support.ResponseUtils, org.apache.commons.codec.binary.Base64"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%>
	<%@ taglib prefix="display" uri="/WEB-INF/displaytag.tld"%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%>
	<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
	<iwcm:checkLogon admin="true" perms="cmp_app-slit_slider"/>
<%
	request.setAttribute("cmpName", "app-slit_slider");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams)) {
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String jspFileName = "/components/app-slit_slider/news.jsp";


	String style = pageParams.getValue("style", "06");
	request.setAttribute("titleKey",
			"Slit Slider");
	request.setAttribute("descKey",
			"components.app-slit_slider.desc");
	request.setAttribute("iconLink",
			"/components/app-slit_slider/editoricon.png");



//JSON editor
String editorData = pageParams.getValue("editorData", "W10=");
boolean isJSON = pageParams.getBooleanValue("isJSON", true);

request.setAttribute("titleKey", "components.app-slit_slider.title");
request.setAttribute("descKey", "&nbsp;");

%>

<jsp:include page="/components/top.jsp"/>

<%-- JSON Editor Script --%>
<link type="text/css" rel="stylesheet" media="screen" href="/components/json_editor/editor_style.css" />
<script type="text/javascript" src="/components/json_editor/editor.js"></script>
<script type="text/javascript" src="/components/json_editor/editor_functions.js"></script>
<%-- END of JSON Editor Script --%>


<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />

<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px !important; }
	td.main { padding: 0px; }
	.minicolors-input{padding-left:32px !important;}
</style>

<script type="text/javascript" src="/admin/scripts/common.jsp"></script>
<script type="text/javascript" src="/components/_common/custom_styles/custom_text_style.jsp"></script>

<script type='text/javascript'>
//<![CDATA[

//Editor Configuration
	function perexImageBlur(input)
	{
		$(input).closest('.item').find(".imageDiv").css("background-image", "url(/thumb"+input.value+"?w=100&h=100&ip=5)");
	}
// For fields configuration
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
        title: '<iwcm:text key="components.app-slit_slider.admin_news_list.headingColor" />',
        type: "colorpicker",
        classes: "editorRight colorpicker",
        onkeydown:""
    },
      subheadingColor: {
        title: '<iwcm:text key="components.app-slit_slider.admin_news_list.fontColor" />',
        type: "colorpicker",
        classes: "editorRight colorpicker",
        onkeydown:""
    }, backgroundColor: {
        title: '<iwcm:text key="components.app-slit_slider.admin_news_list.colors" />',
        type: "colorpicker",
        classes: "editorRight colorpicker",
        onkeydown:""
    },
    redirectUrl: {
        title: "<iwcm:text key="components.news.redirectAfterClick"/>",
        description: "<iwcm:text key="components.news.redirectLinkHelp"/>",
        type: "conditionalText",
        classes: "editorRight"
    }
};

// Form fields to edit
var editorItemsToUse = [
	"image",
    "title",
    "subtitle",
    "redirectUrl",
    "headingColor",
    "subheadingColor",
    "backgroundColor"
];

var inputData = EditorItemsList.decodeJSONData('<%=editorData %>');

var editorItemsList = new EditorItemsList(inputData, editorItemsToUse);
var renderer = new EditorRenderer("#editorWrapper", editorItemsList, editorItemFields, editorItemsToUse, "EditorItemFieldRenderer");


//--------------

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
    console.log("getIncludeText");
	// set Data From JSON Editor
	editorItemsList.setDataFromDom($("#editorWrapper .item"));


	//--------Custom text styles------


	var jspFileName = "<%=jspFileName%>";
	var asc = "no";
	if ($("#order").val() == "yes") {
		asc = "yes";
	}
	var publishType = "new";
	var nivoSliderHeight = document.textForm.nivoSliderHeight.value;
	var headingAlign = document.textForm.customTextAlign1.value;
	var subHeadingAlign = document.textForm.customTextAlign2.value;

	var headingSize = document.textForm.headingSize.value;
	var subHeadingSize = document.textForm.subHeadingSize.value;

	var headingMargin = document.textForm.headingMargin.value;
	var subHeadingMargin = document.textForm.subHeadingMargin.value;

	var includeText = "!INCLUDE(" + jspFileName + ", asc=" + asc
			+ ", publishType=" + publishType +
			", nivoSliderHeight="+ nivoSliderHeight+
			", nivoSliderHeight="+ nivoSliderHeight+
			",editorData="+ EditorItemsList.encodeJSONData(editorItemsList.getData())+
			", headingAlign="+headingAlign+
			", subHeadingAlign="+subHeadingAlign+
			", headingSize="+headingSize+
			", subHeadingSize="+subHeadingSize+
			", headingMargin="+headingMargin+
			", subHeadingMargin="+subHeadingMargin+
			")!";
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

	function Ok()
{
	oEditor.FCK.InsertHtml(getIncludeText());


	return true ;
} // End function


	if (isFck) {
	} else {
		resizeDialog(570, 750);
	}

	function loadComponentIframe() {

	}
	$(document).ready(function(){
		changeAlign(1);
		changeAlign(2);
	})
	  function changeAlign(id){


		    $('label[for="textAlignRight'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right.png)");
		    $('label[for="textAlignLeft'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left.png)");
		    $('label[for="textAlignCenter'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center.png)");
		    $('label[for="textAlignJustify'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify.png)");


		    switch($('input[name="customTextAlign'+id+'"]:checked').val()){
		    	case "left":
		    		$('label[for="textAlignLeft'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_left_selected.png)");
		    		break;

		    case "right":
		    		$('label[for="textAlignRight'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_right_selected.png)");
		    		break;

		    case "center":
		    		$('label[for="textAlignCenter'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_center_selected.png)");
		    		break;

		    case "justify":
		    		$('label[for="textAlignJustify'+id+'"]').css("background-image","url(/components/_common/custom_styles/images/icon_align_justify_selected.png)");
		    		break;
		    }

		    }
	//]]>
</script>
<style>
	div.radio span{display: none;}
</style>

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

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">
			<form onsubmit="return false" name="textForm">
			<table class="hidden">
			<tr>
				<td valign="top" style="padding-left: 20px;"><strong><iwcm:text
							key="components.forum.admin.settingsTab" />:</strong>

												<table border="0" cellspacing="0" cellpadding="1">

							<tr style="display: none;">
								<td><label for="groupIds"><iwcm:text
											key="components.news.groupids" /></label>:</td>
								<td><input type="text" name="groupIds" id="groupIds"
										value="<%=ResponseUtils.filter(pageParams.getValue("groupIds","-1"))%>" />
								</td>
							</tr>
							<tr class="novinky hidden">
								<td><label for="orderType"><iwcm:text
											key="components.news.ordertype" /></label>:</td>
								<td><select id="orderType" name="orderType">
										<option value="priority"><iwcm:text
												key="components.news.ORDER_PRIORITY" /></option>
										<option value="title"><iwcm:text
												key="components.news.ORDER_TITLE" /></option>
										<option value="id"><iwcm:text
												key="components.news.ORDER_ID" /></option>
								</select>
							</tr>
							<tr class="hidden">
								<td><label for="order"><iwcm:text
											key="components.news.order" /></label>:</td>
								<td><select id="order" name="asc">
										<option value="yes"><iwcm:text
												key="components.inquiry.orderAsc" /></option>
										<option value="no"><iwcm:text
												key="components.inquiry.orderDesc" /></option>
								</select></td>
							</tr>





							</table>



					</td>
			</tr>

		</table>
				<div class="form-group clearfix">
					<div class="col-sm-12"><strong><iwcm:text key="components.forum.admin.settingsTab" /></strong></div>
				</div>
				<div class="form-group clearfix">

							<div class="col-sm-2">
								<label  for="nivoSliderHeight"><iwcm:text
											key="editor.table.height" /></label>:</div>
								<div class="col-sm-10">
							<input type="text" id="nivoSliderHeight"
									name="nivoSliderHeight" size="5" maxlength="5"
									value="<%=pageParams.getIntValue("nivoSliderHeight", 500)%>">
									<iwcm:text key="components.forms.file_restrictions.points" />
							</div>
							</div>

							<div class="row">

							<div class="col-sm-6">

								<div class="form-group clearfix">
									<div class="col-sm-12"><strong><iwcm:text key="components.app-slit_slider.admin.textHeadingSettings" /></strong></div>
								</div>
										<div class="form-group clearfix">
												<div class="col-sm-4 text-right"><iwcm:text key="components.app-slit_slider.admin.fontAlign" />:</div>
												<div class="col-sm-8">
													<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_left.png)" for="textAlignLeft1"></label>
													<input style="display:none" type="radio" onclick="changeAlign(1)" id="textAlignLeft1" name="customTextAlign1" value="left" <% if(pageParams.getValue("headingAlign", "left").equals("left")){%>checked="checked"<%}%>>
													<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_center.png)" for="textAlignCenter1"></label>
													<input style="display:none" type="radio" onclick="changeAlign(1)" id="textAlignCenter1" name="customTextAlign1" value="center" <% if(pageParams.getValue("headingAlign", "left").equals("center")){%>checked="checked"<%}%> >
													<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_right.png)" for="textAlignRight1"></label>
													<input style="display:none" type="radio" onclick="changeAlign(1)" id="textAlignRight1" name="customTextAlign1" value="right" <% if(pageParams.getValue("headingAlign", "left").equals("right")){%>checked="checked"<%}%>>
												</div>
								</div>



								<div class="form-group clearfix">
											<div class="col-sm-4 text-right"><iwcm:text key="components.app-slit_slider.admin.fontSize" />:</div>
											<div class="col-sm-8">
												<input type="text"  name="headingSize" size="5" maxlength="5" value="<%=pageParams.getIntValue("headingSize", 70)%>">
												<iwcm:text key="components.forms.file_restrictions.points" />
											</div>
								</div>	<div class="form-group clearfix">
											<div class="col-sm-4 text-right"><iwcm:text key="components.app-slit_slider.admin.fontMarginTop" />:</div>
											<div class="col-sm-8">
												<input type="text"  name="headingMargin" size="5" maxlength="5" value="<%=pageParams.getIntValue("headingMargin", 0)%>">
												<iwcm:text key="components.forms.file_restrictions.points" />
											</div>
							</div>

					</div>

					<div class="col-sm-6">

						<div class="form-group clearfix">
					<div class="col-sm-12"><strong><iwcm:text key="components.app-slit_slider.admin.textSubHeadingSettings" /></strong></div>
				</div>
				<div class="form-group clearfix">
								<div class="col-sm-4 text-right"><iwcm:text key="components.app-slit_slider.admin.fontAlign" />:</div>
								<div class="col-sm-8">
									<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_left.png)" for="textAlignLeft2"></label>
									<input style="display:none" type="radio" onclick="changeAlign(2)" id="textAlignLeft2" name="customTextAlign2" value="left" <% if(pageParams.getValue("subHeadingAlign", "left").equals("left")){%>checked="checked"<%}%>>
									<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_center.png)" for="textAlignCenter2"></label>
									<input style="display:none" type="radio" onclick="changeAlign(2)" id="textAlignCenter2" name="customTextAlign2" value="center" <% if(pageParams.getValue("subHeadingAlign", "left").equals("center")){%>checked="checked"<%}%> >
									<label style="display:inline-block; width:29px; height:29px; background-image:url(/components/_common/custom_styles/images/icon_align_right.png)" for="textAlignRight2"></label>
									<input style="display:none" type="radio" onclick="changeAlign(2)" id="textAlignRight2" name="customTextAlign2" value="right" <% if(pageParams.getValue("subHeadingAlign", "left").equals("right")){%>checked="checked"<%}%>>
								</div>
				</div>

					<div class="form-group clearfix">
								<div class="col-sm-4 text-right"><iwcm:text key="components.app-slit_slider.admin.fontSize" />:</div>
								<div class="col-sm-8">
									<input type="text"  name="subHeadingSize" size="5" maxlength="5" value="<%=pageParams.getIntValue("subHeadingSize", 30)%>">
									<iwcm:text key="components.forms.file_restrictions.points" />
								</div>
				</div><div class="form-group clearfix">
								<div class="col-sm-4 text-right"><iwcm:text key="components.app-slit_slider.admin.fontMarginTop" />:</div>
								<div class="col-sm-8">
									<input type="text"  name="subHeadingMargin" size="5" maxlength="5" value="<%=pageParams.getIntValue("subHeadingMargin", 0)%>">
									<iwcm:text key="components.forms.file_restrictions.points" />
								</div>
				</div>
				</div></div>
		</form>

	</div>
	<div class="tab-page" id="tabMenu2">


		<div id="editorWrapper" class="editorWrapper collapsable"
				data-collapse="<iwcm:text key="components.json_editor.collapse" />"
				data-expand="<iwcm:text key="components.json_editor.expand" />">
		</div>

		<input type="button" id="addItem" class="button50 button50grey"
			value="<iwcm:text key="components.slider.addItem"/>">
		<!-- <input type="button" id="saveEditor" value="Save"> -->

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
