<%@page import="java.util.Map"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%@page import="sk.iway.iwcm.tags.support_logic.ResponseUtils"%>
<%

request.setAttribute("cmpName", "map");

%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.Tools"%>
<jsp:include page="/components/top.jsp"/>

<script src="/components/form/check_form.js" type="text/javascript"></script>
<script src="/components/form/fix_e.js" type="text/javascript"></script>
<%=Tools.insertJQuery(request) %>
<script src="/components/form/event_attacher.js" type="text/javascript"></script>
<script src="/components/form/class_magic.js" type="text/javascript"></script>
<script src="/components/form/check_form_impl.jsp" type="text/javascript"></script>

<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	//out.println(paramPageParams);
	//System.out.println(paramPageParams);
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>

<script type='text/javascript'>

var sizeInPercent;

$(document).ready(function(){
	showLookup();
	checkMapSize();

	$('.map-wrapper iframe').on("load", function(){
        var iframe = $('.map-wrapper iframe').contents();

        iframe.click(function(){
			var coords = localStorage.getItem('coords');
			coords = JSON.parse(coords);

			$('#latitude').val(coords.lat);
			$('#longitude').val(coords.lng);
			$('#object').val('');
			getCheckedValue();
        });
	});

	$('#object').on('keyup', function() {
     	if (this.value.length > 1) {
			$('#latitude').val('');
			$('#longitude').val('');
			getCheckedValue();
     	}
	});

	if ($('#label').val() == "") {
		$('#addComment').prop('checked', false).trigger('change');
	} else {
		$('#addComment').prop('checked', true).trigger('change');
	}

	if ($('#label').val() == "") {
		$('#addComment').prop('checked', false);
		$('.addCommentBox').slideUp();
	} else {
		$('#addComment').prop('checked', true);
		$('.addCommentBox').slideDown();
	}

	$('#scrollToMap').on('click', function(){
		$(".tab-pane").animate({
			scrollTop: 150
		}, 500);
	})

});

var coordinates = false;

function Ok()
{
	//--------------------VALIDACIE-------------------------
	var widthPercent 	= document.getElementById("widthPercent").value.replace('"','').replace('"','');
	var heightPercent 	= document.getElementById("heightPercent").value.replace('"','').replace('"','');
	var width 	= document.getElementById("width").value.replace('"','').replace('"','');
	var height 	= document.getElementById("height").value.replace('"','').replace('"','');
	var zoom 	= document.getElementById("zoom").value.replace('"','').replace('"','');
	var latitude 	= document.getElementById("latitude").value.replace('"','').replace('"','');
	var longitude	= document.getElementById("longitude").value.replace('"','').replace('"','');
	var label = document.getElementById("label").value.replace('"','').replace('"','');
	var address	= document.getElementById("object").value.replace('"','').replace('"','');
	var labelAddress = $('#showLabel').is(':checked');
	var labelComment = $('#addComment').is(':checked');
	var showLabel = $('#showLabel').is(':checked') || $('#addComment').is(':checked');
	var key =  "<%= Constants.getString("googleMapsApiKey", "") %>";
	var enable_scrollwheel = $('#scrollwheel').is(':checked');
	var enable_showControls = !$('#showControls').is(':checked');
	var enable_sizeInPercent = !$('#mapSizeSwticher').is(':checked');
	var enable_closeLabel = $('#closeLabel').is(':checked');
    var offsetX 	= document.getElementById("offsetX").value.replace('"','').replace('"','');
    var offsetY 	= document.getElementById("offsetY").value.replace('"','').replace('"','');

    //--------------KONIEC VALIDACII---------------------------

	if(coordinates == false) {
		if (sizeInPercent) {
			oEditor.FCK.InsertHtml("!INCLUDE(/components/map/map.jsp,offsetX="+offsetX+",offsetY="+offsetY+", widthPercent="+widthPercent+", heightPercent="+heightPercent+", labelAddress="+labelAddress+", labelComment="+labelComment+", zoom="+zoom+", label=\""+label+"\",sizeInPercent=\""+enable_sizeInPercent+"\", showControls=\""+enable_showControls+"\", closeLabel=\""+enable_closeLabel+"\", showContentString=\""+showLabel+"\", scrollwheel=\""+enable_scrollwheel+"\" object=\""+address+"\""+(key != "" ? ", key=\""+key+"\"" : "")+")!");
		} else {
			oEditor.FCK.InsertHtml("!INCLUDE(/components/map/map.jsp,offsetX="+offsetX+",offsetY="+offsetY+", width="+width+", height="+height+", labelAddress="+labelAddress+", labelComment="+labelComment+", zoom="+zoom+", label=\""+label+"\", sizeInPercent=\""+enable_sizeInPercent+"\", showControls=\""+enable_showControls+"\", closeLabel=\""+enable_closeLabel+"\", showContentString=\""+showLabel+"\", scrollwheel=\""+enable_scrollwheel+"\" object=\""+address+"\""+(key != "" ? ", key=\""+key+"\"" : "")+")!");
		}
	}
	else {
		if (sizeInPercent) {
			oEditor.FCK.InsertHtml("!INCLUDE(/components/map/map.jsp,offsetX="+offsetX+",offsetY="+offsetY+", widthPercent="+widthPercent+", heightPercent="+heightPercent+", labelAddress="+labelAddress+", labelComment="+labelComment+", zoom="+zoom+", label=\""+label+"\", sizeInPercent=\""+enable_sizeInPercent+"\", showControls=\""+enable_showControls+"\", closeLabel=\""+enable_closeLabel+"\", showContentString=\""+showLabel+"\", scrollwheel=\""+enable_scrollwheel+"\" latitude=\""+latitude+"\", longitude=\""+longitude+"\""+(key != "" ? ", key=\""+key+"\"" : "")+")!");
		} else {
			oEditor.FCK.InsertHtml("!INCLUDE(/components/map/map.jsp,offsetX="+offsetX+",offsetY="+offsetY+", width="+width+", height="+height+", labelAddress="+labelAddress+", labelComment="+labelComment+", zoom="+zoom+", label=\""+label+"\", sizeInPercent=\""+enable_sizeInPercent+"\", showControls=\""+enable_showControls+"\", closeLabel=\""+enable_closeLabel+"\", showContentString=\""+showLabel+"\", scrollwheel=\""+enable_scrollwheel+"\", latitude=\""+latitude+"\", longitude=\""+longitude+"\""+(key != "" ? ", key=\""+key+"\"" : "")+")!");
		}
	}

	return true ;
} // End function

<%-- ZOBRAZI A PO CHVILKE SCHOVA DIVKO S POMOCNYM TEXTOM--%>

function showObjectHelp(event)
{
	showHelpDiv("objectHelpDiv",event);
}

function showHelpDiv(divName, event)
{
	document.getElementById("objectHelpDiv").style.display = "none";
	var div = document.getElementById(divName);
	div.style.position = "absolute";
	div.style.top = (event.clientY - 130)+"px";
	div.style.left = (event.clientX - 100)+"px";
	div.style.display = 'inline';
	setTimeout('document.getElementById("'+divName+'").style.display = "none";', 7000);
}

function showLookup()
{
	//--------------------VALIDACIE-------------------------
	var widthPercent 	= document.getElementById("widthPercent").value.replace('"','').replace('"','');
	var heightPercent 	= document.getElementById("heightPercent").value.replace('"','').replace('"','');
	var width 	= document.getElementById("width").value.replace('"','').replace('"','');
	var height 	= document.getElementById("height").value.replace('"','').replace('"','');
	var zoom 	= document.getElementById("zoom").value.replace('"','').replace('"','');
	var latitude 	= document.getElementById("latitude").value.replace('"','').replace('"','');
	var longitude 	= document.getElementById("longitude").value.replace('"','').replace('"','');
	var label = document.getElementById("label").value.replace('"','').replace('"','');
	var address		= document.getElementById("object").value.replace('"','').replace('"','');
	var showLabel = $('#showLabel').is(':checked') || $('#addComment').is(':checked');
	var key =  "<%= Constants.getString("googleMapsApiKey", "") %>";
	var enable_scrollwheel = $('#scrollwheel').is(':checked');
	var enable_sizeInPercent = !$('#mapSizeSwticher').is(':checked');
	var enable_showControls = $('#showControls').is(':checked');
	var enable_closeLabel = $('#closeLabel').is(':checked');
    var offsetX 	= document.getElementById("offsetX").value.replace('"','').replace('"','');
    var offsetY 	= document.getElementById("offsetY").value.replace('"','').replace('"','');

	//console.log(showLabel);
	//--------------KONIEC VALIDACII---------------------------

	var mapType;
	var OpenStreetMap;
	var mapProvider = '<%= Constants.getString("mapProvider", "") %>';
	if (mapProvider == 'GoogleMap') {
		OpenStreetMap = false;
	} else {
		OpenStreetMap = true;
	}

	if (sizeInPercent) {
		width = widthPercent;
		height = heightPercent;
	}
	if (coordinates) {
		address = "";
	} else {
		latitude = "";
		longitude = "";
	}
	if (OpenStreetMap) {
		mapType = "open";
	} else {
		mapType = "google"
	}

	str = "/components/map/"+mapType+"_map_preview.jsp?object="+address+"&width="+width+"&height="+height+"&zoom="+zoom+"&scrollwheel="+enable_scrollwheel+"&sizeInPercent="+enable_sizeInPercent+"&showContentString="+showLabel+"&offsetX="+offsetX+"&offsetY="+offsetY+"&label="+label+"&latitude="+latitude+"&longitude="+longitude;

	$("#previewIframe").attr("src", str);
	$("#previewIframe").show();
}

</script>

<form method="get" name="textForm" style="margin: 0px;" id="googleMapForm">

<div id="objectHelpDiv" style="display: none; width: 200px; heigth: 50px; border: 2px solid; background-color: beige; z-index: 500;" >
	<iwcm:text key="components.map.objectHelp" />
</div>

<script>
REQCOND1="no";
REQCOND2="yes";

function getCheckedValue(){
	if (document.getElementById('object').value != ''){
		coordinates = false;
		REQCOND1="no";
		REQCOND2="yes";
		checkForm.checkField(getPageElement('latitude'));
		checkForm.checkField(getPageElement('longitude'));
		checkForm.checkField(getPageElement('object'));

	}
	else {
		coordinates = true;
		REQCOND1="yes";
		REQCOND2="no";
		checkForm.checkField(getPageElement('latitude'));
		checkForm.checkField(getPageElement('longitude'));
		checkForm.checkField(getPageElement('object'));

	}
}

function checkMapSize() {
	if ($('#mapSizeSwticher').is(':checked')) {
		sizeInPercent = false;
		$('#widthPercent').attr('disabled', true);
		$('#heightPercent').attr('disabled', true);
		$('#width').attr('disabled', false);
		$('#height').attr('disabled', false);
	} else {
		sizeInPercent = true;
		$('#widthPercent').attr('disabled', false);
		$('#heightPercent').attr('disabled', false);
		$('#width').attr('disabled', true);
		$('#height').attr('disabled', true);
	}
}

function checkComment() {
	if ($('#addComment').is(':checked')) {
		$('.addCommentBox').slideDown();
	} else {
		$('.addCommentBox').slideUp();
		$('#label').val('');
	}
}

</script>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<style type="text/css">
	ul.tab_menu { padding: 2px 0 0 10px; }
	td.main { padding: 0px; }
	/* UPRAVA JVY */
		.tab-page {
			margin-top: 15px;
		}
		/*tabMenu1*/
		.radiobtnSelection {
			margin-bottom: 15px;
		}
		input[type="radio"] {
 		   margin-right: 3px;
		}
		input[type="radio"]:nth-child(2) {
		    margin-left: 15px;
		}
		#coordinates {
			margin-bottom: 15px;
		}
		#address input{
			width: 90%;
			display: inline;
		}
		#address img {
			display:inline;
		}
		/*tabMenu2*/
		.sectionTitle {
			margin-bottom: 25px;
   			margin-top: 25px;
			margin-left: 30px;
		}
		.col-sm-4 {
			text-align: center;
			margin-bottom: 5px;
		}
		.col-sm-8{
			margin-bottom: 10px;
		}
		#map {
			width: 100%!important;
		}
		.action-btn .btn {
			margin-top: 22px;
		}
		.sizeFormat {
			width: 80%;
			display: inline-block;
			margin-right: 5px;
		}
		.col-sm-6 {
			margin-bottom: 20px;
		}
		.tab-pane h1 {
			margin-left: 15px;
			margin-top: 10px;
		}
		#tabMenu2 .form-control {
			margin-top: -8px;
		}
		#scrollToMap {
			margin-top: -20px;
    		margin-bottom: 25px;
		}
	/* END UPRAVA JVY*/
</style>

<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.map.address"/></a></li>
		<li class="last"><a href="#" onclick="showHideTab('2');" id="tabLink2"><iwcm:text key="components.portal.settings"/></a></li>
	</ul>
</div>

<div class="tab-pane toggle_content tab-pane-fullheight">
	<div class="tab-page" id="tabMenu1" style="display: block;">
		<div class="col-sm-12">
			<p><iwcm:text key="components.map.base_title"/>:</p>
		</div>
		<div class="col-sm-12 text-center">
			<div id="address" class="col-sm-4">
				<label class="form-label"><iwcm:text key="components.map.address"/>:</label>
				<input type="input" placeholder="<iwcm:text key="components.map.address.example"/>" id="object" onfocus="getCheckedValue();" size="60" value="<%=ResponseUtils.filter(pageParams.getValue("object", ""))%>" class="reqcond1 form-control"/>
			</div>
			<div id="coordinates" class="col-sm-6">
				<div class="col-sm-6">
					<label class="form-label"><iwcm:text key="components.map.lat"/></label>
					<input type="input" id="latitude" size="10" onfocus="getCheckedValue();" value="<%=ResponseUtils.filter(pageParams.getValue("latitude", ""))%>" class="reqcond2 form-control" />
				</div>
				<div class="col-sm-6">
					<label class="form-label"><iwcm:text key="components.map.lon"/></label>
					<input type="input" id="longitude" size="10" onfocus="getCheckedValue();" value="<%=pageParams.getValue("longitude", "")%>" class="reqcond2 form-control" />
				</div>
			</div>
			<div class="col-sm-2 action-btn">
				<input class="btn green" type="button" value="<iwcm:text key="components.map.show" />" onclick='showLookup();' />
			</div>
			<br>

			<div class="map-wrapper col-sm-12" style="clear: both;">
				<iframe id="previewIframe" src="/components/iframe_blank.jsp" width="100%" height="410" style="display: none; padding-top: 8px;" frameborder="0"></iframe>
			</div>

			<div class="col-sm-12">
				<p><iwcm:text key="components.map.pin_info"/></p>
				<p></p>
			</div>
		</div>

	</div>
	<div class="tab-page" id="tabMenu2" style="display: none;">
		<div>
			<div class="col-sm-12">
				<div class="row">
					<div class="col-sm-6">
						<div class="sectionTitle">
							<b><iwcm:text key="components.map.settings"/></b>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.width.short"/>
							</div>
							<div class="col-sm-6">
								<input type="number" max="100" value="<%=ResponseUtils.filter(pageParams.getValue("widthPercent", "100"))%>" id="widthPercent" class="numbers form-control sizeFormat"/>
								<span>%</span>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.height.short"/>
							</div>
							<div class="col-sm-6">
								<input type="number" value="<%=ResponseUtils.filter(pageParams.getValue("heightPercent", "100"))%>" id="heightPercent" class="numbers form-control sizeFormat"/>
								<span>%</span>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.own.size"/>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" name="mapSizeSwticher" id="mapSizeSwticher" <%=pageParams.getBooleanValue("sizeInPercent", true) ? "" : "checked=\"checked\""%> value="" onchange="checkMapSize();"/>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.width.short"/>
							</div>
							<div class="col-sm-6">
								<input type="input" value="<%=ResponseUtils.filter(pageParams.getValue("width", "400"))%>" id="width" class="numbers form-control sizeFormat"/>
								<span>px</span>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.height.short"/>
							</div>
							<div class="col-sm-6">
								<input type="input" value="<%=ResponseUtils.filter(pageParams.getValue("height", "400"))%>" id="height" class="numbers form-control sizeFormat"/>
								<span>px</span>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.zoom"/>
							</div>
							<div class="col-sm-6">
								<input type="number" max="21" min="0" id="zoom" size="3" class="form-control sizeFormat" value="<%=pageParams.getIntValue("zoom", 13)%>"/>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.enable_scrollwheel_short"/>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" name="scrollwheel" id="scrollwheel" <%=pageParams.getBooleanValue("scrollwheel", false) ? "checked=\"checked\"" : ""%> />
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.enable_controls"/>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" name="showControls" id="showControls" <%=pageParams.getBooleanValue("showControls", false) ? "" : "checked=\"checked\""%> />
							</div>
						</div>
					</div>

					<div class="col-sm-6">
						<div class="sectionTitle">
							<b><iwcm:text key="components.map.pin.settings" /></b>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.show_address"/>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" name="showLabel" id="showLabel" <%=pageParams.getBooleanValue("labelAddress", false) ? "checked=\"checked\"" : ""%> />
							</div>
						</div>
						<div class="row form-group checkLabelBox">
							<div class="col-sm-6">
								<iwcm:text key="components.map.add.own_text"/>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" name="addComment" id="addComment" onchange="checkComment();" />
							</div>
						</div>
						<div class="row form-group addCommentBox">
							<div class="col-sm-6">
								<iwcm:text key="components.map.label"/>
							</div>
							<div class="col-sm-6">
								<textarea id="label" class="form-control" style="min-height: 90px;" size="60" value="<%=ResponseUtils.filter(pageParams.getValue("label", ""))%>"><%=ResponseUtils.filter(pageParams.getValue("label", ""))%></textarea>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.offsetX.short"/>
							</div>
							<div class="col-sm-6">
								<input type="input" id="offsetX" size="3" class="form-control" value="<%=pageParams.getIntValue("offsetX", 0)%>"/>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.offsetY.short"/>
							</div>
							<div class="col-sm-6">
								<input type="input" id="offsetY" size="3" class="form-control" value="<%=pageParams.getIntValue("offsetY", 0)%>"/>
							</div>
						</div>
						<div class="row form-group">
							<div class="col-sm-6">
								<iwcm:text key="components.map.close_label"/>
							</div>
							<div class="col-sm-6">
								<input type="checkbox" name="closeLabel" id="closeLabel" <%=pageParams.getBooleanValue("closeLabel", false) ? "checked=\"checked\"" : ""%> />
							</div>
						</div>
					</div>
					<div class="col-sm-12 action-btn text-center">
						<input class="btn green" type="button" id="scrollToMap" value="<iwcm:text key="components.map.show_scroll" />" onclick="showLookup();showHideTab('1');" />
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

</form>

<jsp:include page="/components/bottom.jsp"/>

<script>
getCheckedValue();
<% if (Tools.isNotEmpty(ResponseUtils.filter(pageParams.getValue("view", "")))) {%>
	document.textForm.view.value = "<%=ResponseUtils.filter(pageParams.getValue("view", ""))%>";
<%}%>
</script>
