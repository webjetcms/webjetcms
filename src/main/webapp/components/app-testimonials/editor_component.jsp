<%@page import="sk.iway.iwcm.doc.GroupsDB"%>
<%@page import="sk.iway.iwcm.doc.GroupDetails"%>
<%@page import="java.util.List"%>
<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*, org.apache.commons.codec.binary.Base64, sk.iway.iwcm.i18n.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="cmp_testimonials"/>
<%@page import="sk.iway.iwcm.gallery.*"%>
<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<%
request.setAttribute("cmpName", "app-testimonials");
request.setAttribute("iconLink", "/components/app-testimonials/editoricon.png");
String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
String jspFileName = ResponseUtils.filter(request.getParameter("jspFileName"));
String jspFileNameKalendar = "/components/calendar/news_calendar.jsp";




if (Tools.isNotEmpty(jspFileName))
{
	if (jspFileName.startsWith("!INCLUDE(")) jspFileName = jspFileName.substring(9);
	jspFileNameKalendar = jspFileName;
	request.setAttribute("descKey", jspFileName);
}
else
{
	jspFileName = "/components/app-testimonials/news.jsp";
}

if (Tools.isNotEmpty(paramPageParams))
{
	request.setAttribute("includePageParams", paramPageParams);
}
PageParams pageParams = new PageParams(request);

//json
String editorData = pageParams.getValue("editorData", "W10=");
boolean isJSON = pageParams.getBooleanValue("isJSON", true);

if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("perexGroup", ""))))
{
	String[] perexGroupArray = GalleryDB.convertPerexGroupString(ResponseUtils.filter(pageParams.getValue("perexGroup", "")));
	String perexGroupString = GalleryDB.convertPerexGroupString(perexGroupArray);	//aby sa pri editaci skupiny spravne popresuvali, potrebujem
																										//vstup vo specialnom formate ","+groupPerex+","
	request.setAttribute("perexGroup", perexGroupString);
}

String style = pageParams.getValue("style", "03");

request.setAttribute("titleKey", "components.app-testimonials.editMenu");
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
	.nahladC{
position:relative;
display:none;
width:20px;
height:17px;
top:5px;
border:1px solid #a0a0a0;
}


.nahladC{
position:relative;
display:inline-block;
width:20px;
height:17px;
top:5px;
border:1px solid #a0a0a0;

}
.colorpicker-rgba{padding-left: 28px !important;}
</style>

<script type="text/javascript" src="/components/_common/custom_styles/custom_text_style.jsp"></script>


<script type='text/javascript'>

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
        title: "<iwcm:text key="editor.app-testimonials.name"/>",
        type: "text",
        classes: "editorRight",
    },
    description: {
        title: "<iwcm:text key="editor.app-testimonials.text"/>",
        type: "textArea",
        classes: "editorRight"
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
    "description",
    "redirectUrl"
];

var inputData = EditorItemsList.decodeJSONData('<%=editorData %>');

var editorItemsList = new EditorItemsList(inputData, editorItemsToUse);
var renderer = new EditorRenderer("#editorWrapper", editorItemsList, editorItemFields, editorItemsToUse, "EditorItemFieldRenderer");

//--------------


$(document).ready(function(){


	$('input#paging').click(function(){
		if ($('input#paging:checked').length == 0) {
			$('tr.pagingStyle').css("display","none");
		}
		else {
			$('tr.pagingStyle').css("display","table-row");
		}
	});

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
		document.textForm.groupIds.value = groupid;
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

	var typNoviniek = $(".styleBox input:checked").val();

	var jspFileName = "<%=jspFileName%>";

	groupIds = document.textForm.groupIds.value;
//	orderType = document.textForm.orderType.value;

	asc = "no";
	if ($("#order").val() == "yes")
	{
		asc = "yes";
	}
	publishType = "new";
	pageSize = document.textForm.pageSize.value;

	var showName = "yes";


	//pagingStyle = document.textForm.pagingStyle.value;
	//nivoSliderHeight = document.textForm.nivoSliderHeight.value;
	var effect = document.textForm.nivoSliderEffect.value;
	var animate = "yes";
	var showPhoto="yes";


	var noPerexCheck = "no";
	if (document.textForm.noPerexCheck.checked)
	{
		noPerexCheck = "yes";
	}

	if (document.textForm.showPhoto.checked==false)
	{
		showPhoto = "no";
	}

	if (document.textForm.showName.checked==false)
	{
		showName = "no";
	}

	function addStyle(style,add){
		style+=add;
		return style;
	}

var backgroundColor = $("#backgroundColor").val();
var nameColor = $("#nameColor").val();
var textColor = $("#textColor").val();

var customStyleMeno = $("#customStyleName").val();
var customStyleTextTestimonials = $("#customStyleTextTestimonials").val();

	var includeText = "!INCLUDE("+jspFileName+
			", style="+typNoviniek+
			", groupIds="+groupIds+
			", asc="+asc+
			", publishType="+publishType+
		//	", nivoSliderHeight="+nivoSliderHeight+
			", pageSize="+pageSize+
			", showName="+showName+
			", showPhoto="+showPhoto+
			",editorData="+ EditorItemsList.encodeJSONData(editorItemsList.getData())+
			", noPerexCheck="+noPerexCheck+", customStyleMeno=\""+customStyleMeno+"\", customStyleTextTestimonials=\""+customStyleTextTestimonials+"\", backgroundColor=\""+backgroundColor+"\", nameColor=\""+nameColor+
			"\", textColor=\""+textColor+
			"\")!";
	return includeText;
}


function Ok()
{
	oEditor.FCK.InsertHtml(getIncludeText());


	return true ;
} // End function


function checkChanges()
{
	return true;
}

function Cancel()
{
	if (checkChanges()==false) return false;

	return true;
}

function changeTyp(sel)
{
	if(sel == '001' || sel=="002" || sel=="003" || sel=="003b" || sel=="004")
	{
		$(".nivoSlider").show();
		$(".animateRow").hide();
	}
	else
	{
		$(".nivoSlider").hide();
	}


}

function loadComponentIframe()
{

}

</script>
<style type="text/css">

	.styleBox {display: block; position: relative; width: 420px; height: 160px; background: #fff; margin: 3px; padding: 10px; border: 1px solid #bcbcbc; border-radius: 4px;}
	* HTML BODY .styleBox {width: 402px; height: 180px;}

	.boxes .styleBox {height: 110px;}
	* HTML BODY .boxes .styleBox {height: 130px;}

	.styleBox .radioSelect { position: absolute; left: 0; top: 0; text-align: left; width: 100%; height: 100%;}
	.styleBox .radioSelect input {position: absolute; left: 10px; top: 80px; border: 0px none;}
	.boxes .styleBox .radioSelect input  {top: 55px;}
	.styleBox img  {position: absolute; top: 10px; left: 42px;}

	div.colBox {display: block; float: left; margin: 10px 10px 0 0; padding: 0;  width: 408px; overflow: auto;}

	div.clearer {width: 100%; clear: both; height: 0; line-height: 0; font-size: 0; display: block; visibility: hidden;}

	select { width: 300px; }
	input { padding-left: 4px; }
	input.colorpicker-rgba{
	width: 115px;
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

	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.news.styleAndSettings" /></a></li>
			<li class="last"><a href="#" onclick="loadComponentIframe();showHideTab('2');" id="tabLink2"><iwcm:text key="components.app-testimonials.items"/></a></li>
		</ul>
	</div>

<div class="tab-pane toggle_content tab-pane-fullheight">

	<div class="tab-page" id="tabMenu1" style="display: block; width:850px;">

		<!--<iwcm:text key="components.app-testimonials.desc"/> -->
		<br>

		<div class="row">
				<div class="col-sm-7">

					<strong><iwcm:text key="components.app-testimonials.visualStyle"/>:</strong>

					<div id="styleSelectArea" style="height: 450px; width: 445px; overflow: auto;">

						<%
						int checkedInputPosition = 0;
						IwcmFile stylesDir = new IwcmFile(Tools.getRealPath("/components/app-testimonials/admin-styles/"));
						if (stylesDir.exists() && stylesDir.canRead())
						{
							IwcmFile styleFiles[] = stylesDir.listFiles();
							styleFiles = FileTools.sortFilesByName(styleFiles);
							int counter = 0;
							for (IwcmFile file : styleFiles)
							{
								if (file.getName().endsWith(".png")==false) continue;
								if (file.getName().contains("bootstrap") && sk.iway.iwcm.common.CloudToolsForCore.isBootstrap(request)==false) continue;

								String styleValue = Tools.escapeHtml(file.getName().substring(0, file.getName().lastIndexOf(".")));

								if (styleValue.equals(style)) checkedInputPosition = counter;
								%>

									<div class="styleBox">
										<label class="image" for="style-<%=styleValue%>">
											<img src="<%=Tools.escapeHtml(file.getVirtualPath()) %>" alt="<%=styleValue%>" />
											<div class="radioSelect">
			  									<input type="radio" name="style" id="style-<%=styleValue%>" value="<%=styleValue%>" <%= styleValue.equals(style) ? " checked=\"checked\"" : "" %> onclick="changeTyp(this.value)"/>
			  									<% if ("iwcm.interway.sk".equals(request.getServerName())) out.print(styleValue); %>
			  								</div>
										</label>
									</div>
								<%
								counter++;
							}
						}
						%>
					</div>
				</div>
				<div class="col-sm-5">
					<strong><iwcm:text key="components.forum.admin.settingsTab"/>:</strong>
					<p>&nbsp;</p>

					<form onsubmit="return false" name="textForm">
						<div class="" style="width: 400px;">

							<div class="selectFolder" style="display: none;">
								<div><label for="groupIds"><iwcm:text key="components.app-testimonials.groupids"/></label>:</div>
								<div>
									<input type="text" name="groupIds" id="groupIds" value="<%=ResponseUtils.filter(pageParams.getValue("groupIds", "-1"))%>"/>
									<input type="button" class="button50" name="groupSelect" value="<iwcm:text key="editor.perex_group.select"/>" onClick='popupFromDialog("<iwcm:cp/>/admin/grouptree.jsp", 500, 500);'>
									<br/>
									<input type="checkbox" name="noPerexCheck" value="yes" <%if (pageParams.getBooleanValue("noPerexCheck", true)) out.print("checked='checked'");%>> <iwcm:text key="components.app-testimonials.noPerexCheck"/>
								</div>
							</div>

							<div class="novinky photoRow form-group clearfix">
								<div class="col-sm-6"><label for="showPhoto"><iwcm:text key="components.app-testimonials.showPhoto"/>:</label></div>
								<div class="col-sm-6">
									<input type="checkbox" name="showPhoto" <% if (pageParams.getBooleanValue("showPhoto", true)) { out.print(" checked='checked'"); } %>/>
								</div>
							</div>

								<div class="nameRow form-group clearfix">
								<div class="col-sm-6"><label for="showName"><iwcm:text key="components.app-testimonials.showName"/></label>:</div>
								<div class="col-sm-6">
									<input type="checkbox" name="showName" <% if (pageParams.getBooleanValue("showName", true)) { out.print(" checked='checked'"); } %>/>
								</div>
							</div>

								<div class="novinky photoRow form-group clearfix">
								<div class="col-sm-6"><label for="nameColor"><iwcm:text key="components.app-testimonials.nameColor"/></label>:</div>
								<div class="col-sm-6">
									<input type="text" id="nameColor"  class="colorpicker-rgba" name="nameColor" value="<%=pageParams.getValue("nameColor", "#000")%>"/>
									<input id="customStyleName" value="<%=pageParams.getValue("customStyleMeno", "") %>" type="text" style="display:none" />
								<a style="cursor:pointer" class="rozsireneLink" onclick="openCustomStyle(customStyleName)"></a>
								<div class="rozsirene"></div>
								</div>
							</div>

									<div class="novinky photoRow form-group clearfix">
								<div class="col-sm-6"><label for="textColor"><iwcm:text key="components.app-testimonials.textColor"/></label>:</div>
								<div class="col-sm-6">
									<input type="text" id="textColor" class="colorpicker-rgba" name="textColor" value="<%=pageParams.getValue("textColor", "#000")%>"/>
									<input id="customStyleTextTestimonials" value="<%=pageParams.getValue("customStyleTextTestimonials", "") %>" type="text" style="display:none" />
								<a style="cursor:pointer" class="rozsireneLink" onclick="openCustomStyle(customStyleTextTestimonials)"></a>
								</div>
							</div>

							<div class="novinky photoRow form-group clearfix">
								<div class="col-sm-6"><label for="backgroundColor"><iwcm:text key="components.app-testimonials.backgroundColor"/></label>:</div>
								<div class="col-sm-6">
								<input type="text" id="backgroundColor" class="colorpicker-rgba" name="backgroundColor" value="<%=pageParams.getValue("backgroundColor", "#fff")%>"/>

								</div>
							</div>



							<div class="novinky" style="display: none;">
								<div class="col-sm-6"><label for="pageSize"><iwcm:text key="components.app-testimonials.pageSize"/></label>:</div>
								<div class="col-sm-6">
									<input type="text" id="pageSize" name="pageSize" size=5 maxlength="5" value="<%=pageParams.getIntValue("pageSize", 15)%>">
								</div>
							</div>
							<div class="nivoSlider">
								<div class="col-sm-6"><label for="nivoSliderEffect"><iwcm:text key="components.app-testimonials.nivoSlider.effectType"/></label>:</div>
								<div class="col-sm-6">
									<select id="nivoSliderEffect" name="nivoSliderEffect">
										<option value="random"><iwcm:text key="components.app-testimonials.nivoSlider.random"/></option>
										<option value="sliceDownRight"><iwcm:text key="components.app-testimonials.nivoSlider.sliceDownRight"/></option>
										<option value="sliceDownLeft"><iwcm:text key="components.app-testimonials.nivoSlider.sliceDownLeft"/></option>
										<option value="sliceUpRight"><iwcm:text key="components.app-testimonials.nivoSlider.sliceUpRight"/></option>
										<option value="sliceUpLeft"><iwcm:text key="components.app-testimonials.nivoSlider.sliceUpLeft"/></option>
										<option value="sliceUpDown"><iwcm:text key="components.app-testimonials.nivoSlider.sliceUpDown"/></option>
										<option value="sliceUpDownLeft"><iwcm:text key="components.app-testimonials.nivoSlider.sliceUpDownLeft"/></option>
										<option value="fold"><iwcm:text key="components.app-testimonials.nivoSlider.fold"/></option>
										<option value="fade"><iwcm:text key="components.app-testimonials.nivoSlider.fade"/></option>
										<option value="boxRandom"><iwcm:text key="components.app-testimonials.nivoSlider.boxRandom"/></option>
										<option value="boxRain"><iwcm:text key="components.app-testimonials.nivoSlider.boxRain"/></option>
										<option value="boxRainReverse"><iwcm:text key="components.app-testimonials.nivoSlider.boxRainReverse"/></option>
										<option value="boxRainGrow"><iwcm:text key="components.app-testimonials.nivoSlider.boxRainGrow"/></option>
										<option value="boxRainGrowReverse"><iwcm:text key="components.app-testimonials.nivoSlider.boxRainGrowReverse"/></option>
									</select>
								</div>
							</div>

						</div>
					</form>


			</div>
	</div>


	</div>
	<div class="tab-page" style="min-height:800px;" id="tabMenu2">


	<div id="editorWrapper" class="editorWrapper"></div>

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



showHideRow('truncate', $('#perex').is(':checked'));

<% if (checkedInputPosition > 0) { %>
$(document).ready(function(){
	$("#styleSelectArea").scrollTop(<%=(checkedInputPosition*185) - 104%>);
});
<% } %>

try
{
	changeTyp($(".styleBox input:checked").val());
}
catch (e) {}

</script>

<jsp:include page="/components/bottom.jsp" />
