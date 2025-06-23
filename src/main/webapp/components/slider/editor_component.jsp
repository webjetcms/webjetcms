<%@page import="java.util.List"%><%
		sk.iway.iwcm.Encoding
				.setResponseEnc(request, response, "text/html");
	%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,org.apache.commons.codec.binary.Base64,sk.iway.iwcm.i18n.*,java.util.*"%><%@
	taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
	taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
	taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
	taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
	taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
		Prop prop = Prop.getInstance(request);
		request.setAttribute("cmpName", "slider");

		request.setAttribute("iconLink",
				"/components/slider/editoricon.png");
		String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
		if (Tools.isNotEmpty(paramPageParams)) {
			request.setAttribute("includePageParams", paramPageParams);
		}
		PageParams pageParams = new PageParams(request);

	// string input
		String autoplay_countdown_color = pageParams.getValue("autoplay_countdown_color","#ffffff");

	// number input
		int loop_number = pageParams.getIntValue("loop_number",0);
		int autoplay_interval = pageParams.getIntValue("autoplay_interval",5000);
		int sliderWidth  = pageParams.getIntValue("sliderWidth",900);
		int sliderHeight  = pageParams.getIntValue("sliderHeight",360);

		// JSON
		String editorData = pageParams.getValue("editorData", "W10=");
		boolean isJSON = pageParams.getBooleanValue("isJSON", true);

	// checkboxy
		boolean touch_swipe = pageParams.getBooleanValue("touch_swipe",true);
		boolean autoplay = pageParams.getBooleanValue("autoplay",true);
		boolean transition_on_first_slide = pageParams.getBooleanValue("transition_on_first_slide",false);
		boolean pause_on_mousover = pageParams.getBooleanValue("pause_on_mousover",false);
		boolean show_numbering = pageParams.getBooleanValue("show_numbering",false);
		boolean random_play = pageParams.getBooleanValue("random_play",false);
		boolean show_thumbnails = pageParams.getBooleanValue("show_thumbnails",true);
		boolean show_countdown = pageParams.getBooleanValue("show_countdown", true);
		boolean show_shadow_bottom = pageParams.getBooleanValue("show_shadow_bottom", true);
		boolean custom_properties = pageParams.getBooleanValue("custom_properties",false);
		boolean ken_burns_on_slide = pageParams.getBooleanValue("ken_burns_on_slide",false);
		boolean fullWidthSlider = pageParams.getBooleanValue("fullWidthSlider",true);


	// radio buttony
		int display_mode = pageParams.getIntValue("display_mode",0);

	// selectboxy
		String skin = pageParams.getValue("skin","Classic");
		String scaleMode = pageParams.getValue("scaleMode","fill");
		String nav_style = pageParams.getValue("nav_style","bullets");
		String countdown_position = pageParams.getValue("countdown_position","bottom");
		String arrow_style = pageParams.getValue("arrow_style","mouseover");



		// prechody
		String transitions_from_pageParams = pageParams.getValue("transitions_all","");
		String[] transitions_string_array = transitions_from_pageParams.split(",");

		boolean transition_fade = false; if(Arrays.asList(transitions_string_array).contains("fade")) { transition_fade = true; }
		boolean transition_cross_fade = false; if(Arrays.asList(transitions_string_array).contains("crossfade")) { transition_cross_fade = true; }
		boolean transition_slide = false; if(Arrays.asList(transitions_string_array).contains("slide")) { transition_slide = true; }
		boolean transition_elastic = false; if(Arrays.asList(transitions_string_array).contains("elastic")) { transition_elastic = true; }
		boolean transition_slice = false; if(Arrays.asList(transitions_string_array).contains("slice")) { transition_slice = true; }
		boolean transition_blinds = false; if(Arrays.asList(transitions_string_array).contains("blinds")) { transition_blinds = true; }
		boolean transition_blocks = false; if(Arrays.asList(transitions_string_array).contains("blocks")) { transition_blocks = true; }
		boolean transition_shuffle = false; if(Arrays.asList(transitions_string_array).contains("shuffle")) { transition_shuffle = true; }
		boolean transition_tiles = false; if(Arrays.asList(transitions_string_array).contains("tiles")) { transition_tiles = true; }
		boolean transition_flip = false; if(Arrays.asList(transitions_string_array).contains("flip")) { transition_flip = true; }
		boolean transition_flip_with_zoom = false; if(Arrays.asList(transitions_string_array).contains("flipwithzoom")) { transition_flip_with_zoom = true; }
		boolean transition_threed = false; if(Arrays.asList(transitions_string_array).contains("threed")) { transition_threed = true; }
		boolean transition_threed_horizontal = false; if(Arrays.asList(transitions_string_array).contains("threedhorizontal")) { transition_threed_horizontal = true; }
		boolean transition_threed_with_zoom = false; if(Arrays.asList(transitions_string_array).contains("threedwithzoom")) { transition_threed_with_zoom = true; }
		boolean transition_threed_horizontal_with_zoom = false; if(Arrays.asList(transitions_string_array).contains("threedhorizontalwithzoom")) { transition_threed_horizontal_with_zoom = true; }
		boolean transition_threed_flip = false; if(Arrays.asList(transitions_string_array).contains("threedflip")) { transition_threed_flip = true; }
		boolean transition_threed_flip_with_zoom = false; if(Arrays.asList(transitions_string_array).contains("threedflipwithzoom")) { transition_threed_flip_with_zoom = true; }
		boolean transition_threed_tiles = false; if(Arrays.asList(transitions_string_array).contains("threedtiles")) { transition_threed_tiles = true; }
		boolean transition_ken_burns = false; if(Arrays.asList(transitions_string_array).contains("kenburns")) { transition_ken_burns = true; }

	%>

<%-- JSON Editor Script --%>
<%=Tools.insertJQuery(request) %>
<%=Tools.insertJQueryUI(pageContext, "sortable") %>

<link type="text/css" rel="stylesheet" media="screen" href="/components/json_editor/editor_style.css" />
<script type="text/javascript" src="/components/json_editor/editor.js"></script>
<script type="text/javascript" src="/components/json_editor/editor_functions.js"></script>
<%-- END of JSON Editor Script --%>


<jsp:include page="/components/top.jsp" />

<script type="text/javascript">
		//<![CDATA[
	function perexImageBlur(input)
	{
		$(input).closest('.item').find(".imageDiv").css("background-image", "url(/thumb"+input.value+"?w=100&h=100&ip=5)");
	}

			// Editor Configuration
    // For fields configuration
    var editorItemFields = {
		image: {
            title: "Image",
            type: "image",
            classes: "editorLeft"
        },
        title: {
            title: '<iwcm:text key="components.app-cookiebar.cookiebar_title" />',
            type: "text",
            classes: "editorRight",
        },
        description: {
            title: '<iwcm:text key="editor.subtitle" />',
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

		function getAppSlider() {
			// set Data From JSON Editor
			editorItemsList.setDataFromDom($("#editorWrapper .item"));

			//text
			var autoplay_interval = $("input[name=autoplay_interval]").val();
			var autoplay_countdown_color = $("input[name=autoplay_countdown_color]").val();
			var sliderWidth = $("input[name=sliderWidth]").val();
			var sliderHeight = $("input[name=sliderHeight]").val();

			//checBox
			var touch_swipe = false; if ($('input[name=touch_swipe]').is(':checked')) { touch_swipe = true;	}
			var autoplay = false; if ($('input[name=autoplay]').is(':checked')) { autoplay = true;	}
			var transition_on_first_slide = false; if ($('input[name=transition_on_first_slide]').is(':checked')) { transition_on_first_slide = true;	}
			var pause_on_mousover = false; if ($('input[name=pause_on_mousover]').is(':checked')) { pause_on_mousover = true;	}
			var show_numbering = false; if ($('input[name=show_numbering]').is(':checked')) { show_numbering = true;	}
			var random_play = false; if ($('input[name=random_play]').is(':checked')) { random_play = true;	}
			var show_thumbnails = false; if ($('input[name=show_thumbnails]').is(':checked')) { show_thumbnails = true;	}
			var show_countdown = false; if ($('input[name=show_countdown]').is(':checked')) { show_countdown = true;	}
			var show_shadow_bottom = false; if ($('input[name=show_shadow_bottom]').is(':checked')) { show_shadow_bottom = true;	}
			var custom_properties = false; if ($('input[name=custom_properties]').is(':checked')) { custom_properties = true;	}
			var ken_burns_on_slide = false; if ($('input[name=ken_burns_on_slide]').is(':checked')) { ken_burns_on_slide = true;	}
			var fullWidthSlider = false; if ($('input[name=fullWidthSlider]').is(':checked')) { fullWidthSlider = true;	}

			//radioButton
			var display_mode = $("input[name=display_mode]:checked").val(); if(display_mode!=1){ loop_number = $("input[name=loop_number]").val(); }else { loop_number=0 }


			//selectBox
			var skin = $( "#skin" ).val();
			var scaleMode = $("#scaleMode").val();
			var nav_style = $("#nav_style").val();
			var countdown_position = $("#countdown_position").val();
			var arrow_style = $("#arrow_style").val();

			//transitions
			var transitions_all = "";
			if ($('input[name=transition_fade]').is(':checked')) { transitions_all +=",fade";	}
			if ($('input[name=transition_cross_fade]').is(':checked')) { transitions_all +=",crossfade";	}
			if ($('input[name=transition_slide]').is(':checked')) { transitions_all +=",slide";	}
			if ($('input[name=transition_elastic]').is(':checked')) { transitions_all +=",elastic";	}
			if ($('input[name=transition_slice]').is(':checked')) { transitions_all +=",slice";	}
			if ($('input[name=transition_blinds]').is(':checked')) { transitions_all +=",blinds";	}
			if ($('input[name=transition_blocks]').is(':checked')) { transitions_all +=",blocks";	}
			if ($('input[name=transition_shuffle]').is(':checked')) { transitions_all +=",shuffle";	}
			if ($('input[name=transition_tiles]').is(':checked')) { transitions_all +=",tiles";	}
			if ($('input[name=transition_flip]').is(':checked')) { transitions_all +=",flip";	}
			if ($('input[name=transition_flip_with_zoom]').is(':checked')) { transitions_all +=",flipwithzoom";	}
			if ($('input[name=transition_threed]').is(':checked')) { transitions_all +=",threed";	}
			if ($('input[name=transition_threed_horizontal]').is(':checked')) { transitions_all +=",threedhorizontal";	}
			if ($('input[name=transition_threed_with_zoom]').is(':checked')) { transitions_all +=",threedwithzoom";	}
			if ($('input[name=transition_threed_horizontal_with_zoom]').is(':checked')) { transitions_all +=",threedhorizontalwithzoom";	}
			if ($('input[name=transition_threed_flip]').is(':checked')) { transitions_all +=",threedflip";	}
			if ($('input[name=transition_threed_flip_with_zoom]').is(':checked')) { transitions_all +=",threedflipwithzoom";	}
			if ($('input[name=transition_threed_tiles]').is(':checked')) { transitions_all +=",threedtiles";	}
			if ($('input[name=transition_ken_burns]').is(':checked')) { transitions_all +=",kenburns";	}
			transitions_all = transitions_all.substr(1); //odsekne prvy znak - ciarku na zaciatku

			if(!custom_properties){
				return "!INCLUDE(/components/slider/slider.jsp"

					+ ", custom_properties="
					+ custom_properties

					+ ", skin="
					+ skin

					+", transitions_all="
					+ "\"" + transitions_all + "\""

					+", editorData="
					+ EditorItemsList.encodeJSONData(editorItemsList.getData())

					+ ")!";

			}else {

				return "!INCLUDE(/components/slider/slider.jsp, "
					+ ", loop_number="
					+ loop_number
					+ ", autoplay_interval="
					+ autoplay_interval
					+ ", fullWidthSlider="
					+ fullWidthSlider
					+ ", sliderWidth="
					+ sliderWidth
					+ ", sliderHeight="
					+ sliderHeight

					+ ", touch_swipe="
					+ touch_swipe
					+ ", autoplay="
					+ autoplay
					+ ", autoplay_countdown_color="
					+ autoplay_countdown_color
					+ ", transition_on_first_slide="
					+ transition_on_first_slide
					+ ", pause_on_mousover="
					+ pause_on_mousover
					+ ", show_numbering="
					+ show_numbering
					+ ", random_play="
					+ random_play
					+ ", show_thumbnails="
					+ show_thumbnails
					+ ", show_countdown="
					+ show_countdown
					+ ", show_shadow_bottom="
					+ show_shadow_bottom
					+ ", custom_properties="
					+ custom_properties
					+ ", ken_burns_on_slide="
					+ ken_burns_on_slide

					+ ", display_mode="
					+ display_mode

					+ ", skin="
					+ skin
					+ ", scaleMode="
					+ scaleMode
					+ ", nav_style="
					+ nav_style
					+ ", countdown_position="
					+ countdown_position
					+ ", arrow_style="
					+ arrow_style
					+", transitions_all="
					+ "\"" + transitions_all + "\""

					+", editorData="
					+ EditorItemsList.encodeJSONData(editorItemsList.getData())

					+ ")!";
			}
		}

		function Ok() {
			oEditor.FCK.InsertHtml(getAppSlider());
			return true;
		}

		$(document).ready(function(){

		  	//selectBoxy
			$("#skin").val("<%=skin%>");
			$("#scaleMode").val("<%=scaleMode%>");
			$("#nav_style").val("<%=nav_style%>");
			$("#arrow_style").val("<%=arrow_style%>");

			loopRadioBtn(<%=pageParams.getIntValue("display_mode",1)%>	);


		});
	function loopRadioBtn(i) {
		if (i == 2) {
			$("#loop_count").attr("checked", "checked");
			$("#loop_number").prop('disabled', false);
		} else {
			$("#loop_forever").attr("checked", "checked");
			$("#loop_number").prop('disabled', true);
		}

	}

	function showCustomProp() {
		if ($('input[name=custom_properties]').is(':checked')) {
			$('#customProp').fadeIn();
		} else
			$('#customProp').fadeOut();
	}
	function showDimensionsOptions() {
		if ($('input[name=fullWidthSlider]').is(':checked')) {
			$('#dimensions-options').fadeIn();
		} else
			$('#dimensions-options').fadeOut();
	}
	function showStyleImage(sel) {
		var chosenStyle2 = $('#skin').find(":selected").val();
		$('#styleImageIMG').attr("src",
				"/components/slider/admin-styles/" + chosenStyle2 + ".jpg");
	}
	//]]>
</script>

<iwcm:menu name="menuForum">
	<link type="text/css" rel="stylesheet" media="screen"
		href="/admin/css/tabpane-luna.css" />
	<style type="text/css">
ul.tab_menu {
	padding: 2px 0 0 10px;
}

td.main {
	padding: 0px;
}

.col-sm-6 {
	margin-bottom: 10px;
}

.leftCol {
	text-align: right;
	padding-top: 5px;
	margin-bottom: 5px;
}

.col-sm-10 {
	padding-top: 15px;
}
</style>
	<div class="box_tab box_tab_thin left">
		<ul class="tab_menu" id="Tabs">
			<li class="first openFirst"><a href="#"
				onclick="showHideTab('1');" id="tabLink1"><iwcm:text
						key="components.slider.settings" /></a></li>
			<li class=""><a href="#" onclick="showHideTab('4');"
				id="tabLink4"><iwcm:text key="datatable.tab.advanced" /></a></li>
			<li class=""><a href="#" onclick="showHideTab('2');"
				id="tabLink2"><iwcm:text key="components.slider.transitions" /></a></li>
			<li class="last"><a href="#" onclick="showHideTab('3');"
				id="tabLink3"><iwcm:text key="components.slider.files" /></a></li>
		</ul>
	</div>
</iwcm:menu>

<style type="text/css">
.paddingTop4 {
	padding-top: 4px;
}

input[type="number"] {
	border: 1px solid #e5e5e5;
	border-radius: 0;
	box-shadow: none;
	font-weight: normal;
	padding: 6px 12px;
	margin-top: 0px;
	height: auto;
	min-height: 30px;
	font-size: 14px;
	line-height: 1.42857;
	background-image: none;
	max-width: 530px;
	background-color: white;
	transition: border-color 0.15s ease-in-out 0s, box-shadow 0.15s
		ease-in-out 0s;
}
</style>
<div class="tab-pane toggle_content tab-pane-fullheight">
	<form name=textForm>
		<div class="tab-page" id="tabMenu1" style="display: block;">
			<div class="col-xs-12 form-group">
				<div class="col-xs-4 paddingTop4 text-right">
					<iwcm:text key="components.slider.skin" />
				</div>
				<div class="col-xs-8">
					<select name="skin" id="skin" onchange="showStyleImage(this);">
						<option value="Classic" selected="selected">Classic</option>
						<!-- <option value="Content">Content</option> -->
						<option value="ContentBox">ContentBox</option>
						<option value="Cube">Cube</option>
						<option value="Elegant">Elegant</option>
						<option value="Events">Events</option>
						<option value="FeatureList">FeatureList</option>
						<option value="FrontPage">FrontPage</option>
						<option value="Gallery">Gallery</option>
						<option value="Header">Header</option>
						<!-- <option value="Highlight">Highlight</option> -->
						<!-- 							<option value="Light">Light</option>  -->
						<option value="Lightbox">Lightbox</option>
						<!-- <option value="Mediapage">Mediapage</option> -->
						<!-- <option value="Multirows">Multirows</option> -->
						<!-- <option value="Navigator">Navigator</option> -->
						<!-- <option value="News">News</option> -->
						<!-- <option value="Numbering">Numbering</option> -->
						<!-- <option value="Pink">Pink</option> -->
						<!-- <option value="RedAndBlack">RedAndBlack</option> -->
						<!-- <option value="Ribbon">Ribbon</option> -->
						<!-- <option value="RightTabs">RightTabs</option> -->
						<!-- <option value="RightTabsDark">RightTabsDark</option> -->
						<!-- <option value="RightThumbs">RightThumbs</option> -->
						<!-- <option value="Rotator">Rotator</option> -->
						<!-- <option value="Showcase">Showcase</option> -->
						<!-- <option value="SimpleControls">SimpleControls</option> -->
						<!-- <option value="Simplicity">Simplicity</option> -->
						<!-- <option value="Stylish">Stylish</option> -->
						<option value="TextNavigation">TextNavigation</option>
						<!-- <option value="Thumbnails">Thumbnails</option> -->
						<!-- <option value="TopCarousel">TopCarousel</option> -->
						<!-- <option value="Vertical">Vertical</option> -->
						<!-- <option value="VerticalNumber">VerticalNumber</option> -->
						<!-- <option value="WoodBackground">WoodBackground</option> -->

					</select>
					<div id="styleImage" style="width: 100%; height: auto;">
						<img id="styleImageIMG"
							src="/components/slider/admin-styles/<%=pageParams.getValue("skin", "Classic")%>.jpg"
							style="width: 100%; margin-top: 7px; border: 4px solid #4286f4;">
					</div>
				</div>
			</div>
		</div>
		<div class="tab-page" id="tabMenu4" style="display: none;">
			<div class="col-xs-12 form-group custom-prop-check">
				<div class="col-xs-4 text-right" style="padding-top: 0px;">
					<input type="checkbox" name="custom_properties"
						onchange="showCustomProp()"
						<%if (pageParams.getBooleanValue("custom_properties", false))
				out.print("checked='checked'");%>>
				</div>
				<div class="col-xs-8 paddingTop4">
					<iwcm:text key="components.slider.otherProperties" />
				</div>
			</div>
			<div id="customProp" <%if (!custom_properties) out.print("style='display:none;'");%>>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.sliderDimensions" />
					</div>
					<div class="col-xs-8">
						<div class="col-xs-12">
							<iwcm:text key="components.slider.fullWidthSlider" />
							<input type="checkbox" name="fullWidthSlider" onchange="showDimensionsOptions()" <%if (fullWidthSlider)out.print("checked='checked'");%>>
						</div>
						<div id="dimensions-options paddingTop4">
							<div class="col-xs-6">
								<iwcm:text key="components.slider.sliderWidth" />
								<input type="number" name="sliderWidth" min="900" max="1920" step="10"
								value="<%=sliderWidth%>" style="width: 103px;">
							</div>
							<div class="col-xs-6">
								<iwcm:text key="components.slider.sliderHeight" />
								<input type="number" name="sliderHeight" min="360" max="1080" step="10"
								value="<%=sliderHeight%>" style="width: 103px;">
							</div>
						</div>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.displayArrows" />
					</div>
					<div class="col-xs-8">
						<select name="arrow_style" id="arrow_style">
							<option value="none"><iwcm:text
									key="components.slider.none" /></option>
							<option value="always"><iwcm:text
									key="components.slider.always" /></option>
							<option value="mouseover" selected="selected"><iwcm:text
									key="components.slider.mouseover" /></option>
						</select>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.kenBurnsOnSlide" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="ken_burns_on_slide"
							<%if (ken_burns_on_slide)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.navStyle" />
					</div>
					<div class="col-xs-8">
						<select name="nav_style" id="nav_style">
							<option value="none" selected="selected"><iwcm:text
									key="components.slider.none" /></option>
							<option value="bullets"><iwcm:text
									key="components.slider.bullets" /></option>
							<option value="numbering"><iwcm:text
									key="components.slider.numbers" /></option>
							<option value="thumbnails"><iwcm:text
									key="components.slider.thumbnails" /></option>
						</select>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.navShowThumb" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="show_thumbnails"
							<%if (show_thumbnails)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.touchSwipe" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="touch_swipe"
							<%if (touch_swipe)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<!-- <div class="col-xs-12 form-group">
						<div class="col-xs-8 text-right">scale mode</div>
						<div class="col-xs-4">
							<select name="scaleMode" id="scaleMode" onchange="" style="width:100px;">
								<option value="fit">FIT</option>
								<option value="fill">FILL</option>
							</select>
						</div>
					</div> -->
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.randomPlay" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="random_play"
							<%if (random_play)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.autoplay" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="autoplay"
							<%if (autoplay)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.loop" />
					</div>
					<div class="col-xs-8">
						<div>
							<input type="radio" name="display_mode" value="1"
								id="loop_forever" onclick="loopRadioBtn(1);">
							<iwcm:text key="components.slider.loopForever" />
						</div>
						<div style="margin-top: 3px;">
							<input type="radio" name="display_mode" value="2" id="loop_count"
								onclick="loopRadioBtn(2);">
							<iwcm:text key="components.slider.loopEndAfter" />
							: <input type="number" name="loop_number" id="loop_number"
								min="0" size="5" value="<%=loop_number%>" style="width: 80px">
						</div>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.interval" />
					</div>
					<div class="col-xs-8">
						<input type="number" name="autoplay_interval" min="500" step="1"
							value="<%=autoplay_interval%>" style="width: 103px;">
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.showTimer" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="show_countdown"
							<%if (show_countdown)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.timerColor" />
					</div>
					<div class="col-xs-8">
						<input type="text" name="autoplay_countdown_color" size=7
							maxlength="7" value="<%=autoplay_countdown_color%>">
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.timerPosition" />
					</div>
					<div class="col-xs-8">
						<select name="countdown_position" id="countdown_position">
							<option value="top"><iwcm:text
									key="components.slider.top" /></option>
							<option value="bottom" selected="selected"><iwcm:text
									key="components.slider.bottom" /></option>
						</select>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.transitionOnFirstSlide" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="transition_on_first_slide"
							<%if (transition_on_first_slide)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.mouseoverPause" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="pause_on_mousover"
							<%if (pause_on_mousover)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.numbering" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="show_numbering"
							<%if (show_numbering)
				out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.slider.showShadow" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="show_shadow_bottom"
							<%if (show_shadow_bottom)
				out.print("checked='checked'");%>>
					</div>
					<!-- <div class="col-xs-8"><input type="checkbox" name="show_shadow_bottom" <%if (pageParams.getBooleanValue("show_shadow_bottom", true))
				out.print("checked='checked'");%>></div> -->
				</div>
			</div>
		</div>

		<!--
														PRECHODY
																						-->
		<style>
			.tooltipHoverMe {
			    position: relative;
			    display: inline-block;
			}
			.tooltipHoverMe .tooltipContent {
				height:160px;
				width: 260px;
			    visibility: hidden;
			    background-color: #555;
			    border-radius: 6px;
			    padding: 5px;
			    position: absolute;
			    z-index: 1;
			    /* bottom: 125%;
			    left: 50%; */
			    margin-left: -130px;
			    opacity: 0;
			    transition: opacity 1s;
			}

			.tooltipHoverMe .tooltipContent::after {
			    content: "";
			    position: absolute;
			    top: 100%;
			    left: 50%;
			    margin-left: -5px;
			    border-width: 5px;
			    border-style: solid;
			    border-color: #555 transparent transparent transparent;
			}

			.tooltipHoverMe:hover .tooltipContent {
			    visibility: visible;
			    opacity: 1;
			}
		</style>
		<div class="tab-page" id="tabMenu2"
			style="display: none;">
			<div class="col-xs-6">
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_fade" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_fade"
							<%if (transition_fade)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="FADE" src="/components/slider/transitions/fade.gif" class="tooltipContent">
						</span>

					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_crossFade" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_cross_fade"
							<%if (transition_cross_fade)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="CROSS FADE" src="/components/slider/transitions/cross_fade.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_slide" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_slide"
							<%if (transition_slide)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="SLIDE" src="/components/slider/transitions/slide.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_elastic" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_elastic"
							<%if (transition_elastic)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="ELASTIC" src="/components/slider/transitions/elastic.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_slice" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_slice"
							<%if (transition_slice)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="SLICE" src="/components/slider/transitions/slice.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_blinds" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_blinds"
							<%if (transition_blinds)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="BLINDS" src="/components/slider/transitions/blinds.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_blocks" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_blocks"
							<%if (transition_blocks)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="BLOCKS" src="/components/slider/transitions/blocks.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_shuffle" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_shuffle"
							<%if (transition_shuffle)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="SHUFFLE" src="/components/slider/transitions/shuffle.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_tiles" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_tiles"
							<%if (transition_tiles)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="TILES" src="/components/slider/transitions/tiles.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_flip" />
					</div>
					<div class="col-xs-4 ">
						<input type="checkbox" name="transition_flip"
							<%if (transition_flip)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="FLIP" src="/components/slider/transitions/flip.gif" class="tooltipContent">
						</span>
					</div>
				</div>
			</div>
			<div class="col-xs-6">
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_flipWithZoom" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_flip_with_zoom"
							<%if (transition_flip_with_zoom)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="FLIP WITH ZOOM" src="/components/slider/transitions/flipWithZoom.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_3D" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_threed"
							<%if (transition_threed)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="3D" src="/components/slider/transitions/3D.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_3DHorizontal" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_threed_horizontal"
							<%if (transition_threed_horizontal)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="3D HORIZONTAL" src="/components/slider/transitions/3Dhorizontal.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_3DWithZoom" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_threed_with_zoom"
							<%if (transition_threed_with_zoom)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="3D WITH ZOOM" src="/components/slider/transitions/3DwithZoom.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_3DHorizontalWithZoom" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox"
							name="transition_threed_horizontal_with_zoom"
							<%if (transition_threed_horizontal_with_zoom)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="3D HORIZONTAL WITH ZOOM" src="/components/slider/transitions/3DhorizontalWithZoom.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_3DFlip" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_threed_flip"
							<%if (transition_threed_flip)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="3D FLIP" src="/components/slider/transitions/3Dflip.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_3DFlipWithZoom" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_threed_flip_with_zoom"
							<%if (transition_threed_flip_with_zoom)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="3D FLIP WITH ZOOM" src="/components/slider/transitions/3DflipWithZoom.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_3DTiles" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_threed_tiles"
							<%if (transition_threed_tiles)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="3D TILES" src="/components/slider/transitions/3Dtiles.gif" class="tooltipContent">
						</span>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-8 text-right paddingTop4">
						<iwcm:text key="components.slider.transition_kenBurns" />
					</div>
					<div class="col-xs-4">
						<input type="checkbox" name="transition_ken_burns"
							<%if (transition_ken_burns)
				out.print("checked='checked'");%>>
						<span class="tooltipHoverMe">
							<i class="ti ti-info-circle" aria-hidden="true" style="margin-left:4px;"></i>
							<img alt="KEN BURNS" src="/components/slider/transitions/kenBurns.gif" class="tooltipContent">
						</span>
					</div>
				</div>
			</div>
		</div>
	</form>

	<div class="tab-page" id="tabMenu3"
		style="display: none;">
		<div id="editorWrapper" class="editorWrapper"></div>

		<input type="button" id="addItem" class="button50 button50grey"
			value="<iwcm:text key="components.slider.addItem"/>">
		<!-- <input type="button" id="saveEditor" value="Save"> -->
	</div>

</div>

<jsp:include page="/components/bottom.jsp" />
