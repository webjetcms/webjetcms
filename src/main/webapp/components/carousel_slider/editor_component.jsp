<%
		sk.iway.iwcm.Encoding
				.setResponseEnc(request, response, "text/html");
	%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,java.util.*"%><%@
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
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%
		Prop prop = Prop.getInstance(request);
		request.setAttribute("cmpName", "carousel_slider");
		request.setAttribute("iconLink",
				"/components/carousel_slider/editoricon.png");
		String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
		if (Tools.isNotEmpty(paramPageParams)) {
			request.setAttribute("includePageParams", paramPageParams);
		}
		PageParams pageParams = new PageParams(request);

	// number input
		int loop_number = pageParams.getIntValue("loop_number",0);
		int autoplay_interval = pageParams.getIntValue("autoplay_interval",5000);
		int rowNumber = pageParams.getIntValue("rowNumber",1);
		int imgPerSlide  = pageParams.getIntValue("imgPerSlide",4);
		int carouselWidth  = pageParams.getIntValue("carouselWidth",900);
		int carouselHeight  = pageParams.getIntValue("carouselHeight",300);
		int imageWidth  = pageParams.getIntValue("imageWidth",300);
		int imageHeight  = pageParams.getIntValue("imageHeight",300);

	// JSON
		String editorData = pageParams.getValue("editorData", "W10=");
		boolean isJSON = pageParams.getBooleanValue("isJSON", true);

	// checkboxy
		boolean touch_swipe = pageParams.getBooleanValue("touch_swipe",true);
		boolean autoplay = pageParams.getBooleanValue("autoplay",true);
		boolean pause_on_mouse_over = pageParams.getBooleanValue("pause_on_mouse_over",false);
		boolean circular = pageParams.getBooleanValue("circular",true);
		boolean random_play = pageParams.getBooleanValue("random_play",false);
		boolean show_shadow_bottom = pageParams.getBooleanValue("show_shadow_bottom", false);
		boolean custom_properties = pageParams.getBooleanValue("custom_properties",false);
		boolean showLightbox = pageParams.getBooleanValue("showLightbox",true);

	// selectboxy
		String skin = pageParams.getValue("skin","Classic");
		String nav_style = pageParams.getValue("nav_style","bullets");
		String direction = pageParams.getValue("direction","horizontal");
		String arrow_style = pageParams.getValue("arrow_style","mouseover");
	%>

<%-- JSON Editor Script --%>
<%=Tools.insertJQuery(request) %>
<%=Tools.insertJQueryUI(pageContext, "sortable") %>
<style>
.editorWrapper .item {
	border: 1px solid #000;
	background: #fff;
	margin: 10px 0;
	padding: 20px;
}

.editorWrapper textarea, .editorWrapper input[type=text] {
	width: 100%;
}

.editorWrapper .propertyWrapper .imageDiv {
	width: 100px;
	height: 100px;
	background-image:
		url(/components/news/admin_imgplaceholder.png?w=100&h=100&ip=5);
	cursor: pointer;
}

.editorWrapper .editorLeft {
	float: left;
	width: 20%;
}

.editorWrapper .propertyWrapper {
	margin: 0 0 10px 0;
}

.editorWrapper .editorRight {
	margin-left: 20%;
}

.editorWrapper .item {
	position: relative;
}

.editorWrapper .removeItem {
	position: absolute;
	top: 10px;
	right: 10px;
	cursor: pointer;
}

.editorWrapper .imageWrapper {
	height: 0;
}

.editorWrapper .imageWrapper label {
	display: none;
}

.editorWrapper .imageWrapper>div {
	width: 100%;
}
</style>
<script type="text/javascript" src="/components/json_editor/editor.js"></script>
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
		        title: '<iwcm:text key="editor.create_table.caption" />',
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

		$(function() {
			renderer.setAfterRenderCallback(function(self) {
				// remove item
		        $("#editorWrapper .removeItem").on('click', function(e) {
		            e.preventDefault();
		            $(this).closest('.item').remove();
		            self.editorItemsList.setDataFromDom($(self.selector + " .item"));
		        });

				// choose image
		        $('#editorWrapper .imageDiv').on('click', function(e) {
		        	e.preventDefault();
		        	console.log($(this).closest('.item').attr('id'));
		        	openImageDialogWindow($(this).closest('.item').attr('id'), "image", null);

		        	//perexImageBlur(document.getElementById($(this).closest('.item').find('input[name=image]').attr('id')));
		        });

				$('#editorWrapper input[name=image]').on('change', function() {
					perexImageBlur(document.getElementById($(this).attr('id')));
				});

				// show redirect url
		        $('#editorWrapper .editorItemCheckbox').on('click', function() {
		        	var itemVal = $(this).closest('.propertyWrapper').find('.editorItemValue').val();
		        	if(itemVal != undefined && itemVal != "") {
		        		$(this).closest('.propertyWrapper').find('.editorItemValue').val("");
		        	}
		        	$(this).closest('.propertyWrapper').find('.editorItemValue').toggle();
		        });

				// choose redirect url
		        $('#editorWrapper input[name=redirectUrl]').on('click', function(e) {
		        	e.preventDefault();
		        	openLinkDialogWindow($(this).closest('.item').attr('id'), "redirectUrl", null, null);
		        });

				// show/hide each redirect url
				$('#editorWrapper .editorItemCheckbox').each(function() {
					var itemVal = $(this).closest('.propertyWrapper').find('.editorItemValue').val()
					if(itemVal != undefined && itemVal != "") {
						$(this).prop('checked', true);
						$(this).closest('.propertyWrapper').find('.editorItemValue').show();
					} else {
						$(this).prop('checked', false);
						$(this).closest('.propertyWrapper').find('.editorItemValue').hide();
					}
		        });

				// show choosen images
		        $('#editorWrapper .imageDiv').each(function() {
		        	if($(this).closest('.item').find('input[name=image]').val() != "" && $(this).closest('.item').find('input[name=image]').val() != undefined) {
		        		perexImageBlur(document.getElementById($(this).closest('.item').find('input[name=image]').attr('id')));
		        	}
		        });
		    });

		   /*  $('#saveEditor').on('click', function(e) {
		        e.preventDefault();
		        editorItemsList.setDataFromDom($("#editorWrapper .item"));
		        console.log(editorItemsList.getData());
		        console.log(JSON.stringify(editorItemsList.getData()));
		        // tu implementuj do includovanie
		    }); */

		    $('#addItem').on('click', function(e) {
		        e.preventDefault();
		        editorItemsList.setDataFromDom($("#editorWrapper .item"));
		        editorItemsList.addNewItem();
		        renderer.render();
		    });

		    renderer.render();
		});

		function getAppCarouselSlider() {
			// set Data From JSON Editor
			editorItemsList.setDataFromDom($("#editorWrapper .item"));

			//number
//			var autoplay_interval = $("input[name=autoplay_interval]").val();
			var carouselWidth = $("input[name=carouselWidth]").val();
			var carouselHeight = $("input[name=carouselHeight]").val();
			var imageWidth = $("input[name=imageWidth]").val();
			var imageHeight = $("input[name=imageHeight]").val();
			var rowNumber = $("input[name=rowNumber]").val();

			//checBox
			var touch_swipe = false; if ($('input[name=touch_swipe]').is(':checked')) { touch_swipe = true;	}
			var autoplay = false; if ($('input[name=autoplay]').is(':checked')) { autoplay = true;	}
			var pause_on_mouse_over = false; if ($('input[name=pause_on_mouse_over]').is(':checked')) { pause_on_mouse_over = true;	}
			var circular = false; if ($('input[name=circular]').is(':checked')) { circular = true;	}
			var random_play = false; if ($('input[name=random_play]').is(':checked')) { random_play = true;	}
			var show_shadow_bottom = false; if ($('input[name=show_shadow_bottom]').is(':checked')) { show_shadow_bottom = true;	}
			var custom_properties = false; if ($('input[name=custom_properties]').is(':checked')) { custom_properties = true;	}
			var showLightbox = false; if ($('input[name=showLightbox]').is(':checked')) { showLightbox = true;	}

			//selectBox
			var skin = $( "#skin" ).val();
			var nav_style = $("#nav_style").val();
			var direction = $('#direction').val();
			var imgPerSlide = parseInt($("#imgPerSlide").val());
			var arrow_style = $("#arrow_style").val();

			if(!custom_properties){
				return "!INCLUDE(/components/carousel_slider/carousel_slider.jsp"

					+ ", custom_properties="
					+ custom_properties

					+ ", skin="
					+ skin

					+", editorData="
					+ EditorItemsList.encodeJSONData(editorItemsList.getData())

					+ ")!";

			}else {
				return "!INCLUDE(/components/carousel_slider/carousel_slider.jsp, "
					+ ", skin="
					+ skin
					+ ", carouselWidth="
					+ carouselWidth
					+ ", carouselHeight="
					+ carouselHeight
					+ ", imageWidth="
					+ imageWidth
					+ ", imageHeight="
					+ imageHeight
					+ ", imgPerSlide="
					+ imgPerSlide
					+ ", direction="
					+ direction
					+ ", nav_style="
					+ nav_style
					+ ", arrow_style="
					+ arrow_style
					+ ", touch_swipe="
					+ touch_swipe
					+ ", autoplay="
					+ autoplay
					+ ", pause_on_mouse_over="
					+ pause_on_mouse_over
					+ ", random_play="
					+ random_play
					+ ", show_shadow_bottom="
					+ show_shadow_bottom
					+ ", rowNumber="
					+ rowNumber
					+ ", circular="
					+ circular
					+ ", showLightbox="
					+ showLightbox



//					+ ", loop_number="
//					+ loop_number
//					+ ", autoplay_interval="
//					+ autoplay_interval



					+ ", custom_properties="
					+ custom_properties





					+", editorData="
					+ EditorItemsList.encodeJSONData(editorItemsList.getData())

					+ ")!";
			}
		}

		function Ok() {
			oEditor.FCK.InsertHtml(getAppCarouselSlider());
			return true;
		}

		$(document).ready(function(){

		  	//selectBoxy
			$("#skin").val("<%=skin%>");
			$("#nav_style").val("<%=nav_style%>");
			$("#direction").val("<%=direction%>");
			$("#arrow_style").val("<%=arrow_style%>");
			$("#imgPerSlide").val("<%=imgPerSlide%>");

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
				"/components/carousel_slider/admin-styles/" + chosenStyle2 + ".jpg");
	}
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
				onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.carousel_slider.skin" /></a></li>
			<li class=""><a href="#"
				onclick="showHideTab('3');" id="tabLink3"><iwcm:text key="components.carousel_slider.settings" /></a></li>
			<li class="last"><a href="#" onclick="showHideTab('2');"
				id="tabLink2"><iwcm:text key="components.carousel_slider.files" /></a></li>
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
		<div class="tab-page" id="tabMenu1"
			style="display: block;">
			<div class="col-xs-12 form-group">
				<div class="col-xs-4 paddingTop4 text-right">
					<iwcm:text key="components.carousel_slider.skin" />
				</div>
				<div class="col-xs-8">
					<select name="skin" id="skin" onchange="showStyleImage(this);">
						<option value="AutoScroller"><iwcm:text key="components.carousel_slider.skin.autoScroller" /></option>
						<option value="Classic" selected="selected"><iwcm:text key="components.carousel_slider.skin.classic" /></option>
						<option value="Fashion"><iwcm:text key="components.carousel_slider.skin.fashion" /></option>
						<option value="Gallery"><iwcm:text key="components.carousel_slider.skin.gallery" /></option>
						<option value="Rotator"><iwcm:text key="components.carousel_slider.skin.rotator" /></option>
						<option value="Simplicity"><iwcm:text key="components.carousel_slider.skin.simplicity" /></option>
						<option value="Stylish"><iwcm:text key="components.carousel_slider.skin.stylish" /></option>
					</select>
					<div id="styleImage" style="width: 100%; height: auto;">
						<img id="styleImageIMG"
							src="/components/carousel_slider/admin-styles/<%=pageParams.getValue("skin", "Classic")%>.jpg"
							style="width: 100%; margin-top: 7px; border: 4px solid #4286f4;">
					</div>
				</div>
			</div>
		</div>
		<div class="tab-page" id="tabMenu3">
			<div class="col-xs-12 form-group custom-prop-check">
				<div class="col-xs-4 text-right" style="padding-top: 0px;">
					<input type="checkbox" name="custom_properties"
						onchange="showCustomProp()"
						<%if (pageParams.getBooleanValue("custom_properties", false))
				out.print("checked='checked'");%>>
				</div>
				<div class="col-xs-8 paddingTop4">
					<iwcm:text key="components.carousel_slider.otherProperties" />
				</div>
			</div>

			<!---CUSTOM PROPERTIES----- -->
			<div id="customProp" <%if (!custom_properties) out.print("style='display:none;'");%>>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.sliderDimensions" />
					</div>
					<div class="col-xs-8">
						<!--
						<div class="col-xs-12">
							<iwcm:text key="components.carousel_slider.fullWidthSlider" />
							<input type="checkbox" name="fullWidthSlider" onchange="showDimensionsOptions()" <%//if (fullWidthSlider)out.print("checked='checked'");%>>
						</div>
						-->
						<div id="dimensions-options" class="paddingTop4">
							<div class="row">
								<div class="col-xs-6">
									<div class="pull-right">
										<iwcm:text key="components.carousel_slider.carouselWidth" />
										<input type="number" name="carouselWidth" min="300" max="1920" step="10"
										value="<%=carouselWidth%>" style="width: 80px;">
									</div>
								</div>
								<div class="col-xs-6">
									<div class="pull-right">
										<iwcm:text key="components.carousel_slider.carouselHeight" />
										<input type="number" name="carouselHeight" min="200" max="1080" step="10"
										value="<%=carouselHeight%>" style="width: 80px;">
									</div>
								</div>
							</div>
							<div class="row">
								<div class="col-xs-12" style="padding-top:10px;">
									<iwcm:text key="components.carousel_slider.thumbQuality" />
								</div>
							</div>
							<div class="row paddingTop4">
								<div class="col-xs-6">
									<div class="pull-right">
										<iwcm:text key="components.carousel_slider.imageWidth" />
										<input type="number" name="imageWidth" min="30" max="1920" step="1"
										value="<%=imageWidth%>" style="width: 80px;">
									</div>
								</div>
								<div class="col-xs-6">
									<div class="pull-right">
										<iwcm:text key="components.carousel_slider.imageHeight" />
										<input type="number" name="imageHeight" min="20" max="1080" step="1"
										value="<%=imageHeight%>" style="width: 80px;">
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.imgPerSlide" />
					</div>
					<div class="col-xs-8">
						<select name="imgPerSlide" id="imgPerSlide">
							<option value="1">1</option>
							<option value="2">2</option>
							<option value="3">3</option>
							<option value="4" selected='selected'>4</option>
							<option value="5">5</option>
							<option value="6">6</option>
						</select>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.direction" />
					</div>
					<div class="col-xs-8">
						<select name="direction" id="direction">
							<option value="horizontal"><iwcm:text key="components.carousel_slider.direction.horizontal" /></option>
							<option value="vertical"><iwcm:text key="components.carousel_slider.direction.vertical" /></option>
						</select>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.showLightbox" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="showLightbox"
							<%if (touch_swipe) out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.rowNumber" />
					</div>
					<div class="col-xs-8">
						<input type="number" name="rowNumber" min="1" max="5" step="1"
							value="<%=rowNumber%>" style="width: 103px;">
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.navStyle" />
					</div>
					<div class="col-xs-8">
						<select name="nav_style" id="nav_style">
							<option value="none" selected="selected"><iwcm:text key="components.carousel_slider.none" /></option>
							<option value="bullets"><iwcm:text key="components.carousel_slider.bullets" /></option>
						</select>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.displayArrows" />
					</div>
					<div class="col-xs-8">
						<select name="arrow_style" id="arrow_style">
							<option value="none"><iwcm:text
									key="components.carousel_slider.none" /></option>
							<option value="always"><iwcm:text
									key="components.carousel_slider.always" /></option>
							<option value="mouseover" selected="selected"><iwcm:text
									key="components.carousel_slider.mouseover" /></option>
						</select>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.touchSwipe" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="touch_swipe"
							<%if (touch_swipe) out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.randomPlay" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="random_play"
							<%if (random_play)	out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.autoplay" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="autoplay"
							<%if (autoplay)	out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.mouseoverPause" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="pause_on_mouse_over"
							<%if (pause_on_mouse_over) out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.circular" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="circular"
							<%if (circular) out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.showShadow" />
					</div>
					<div class="col-xs-8">
						<input type="checkbox" name="show_shadow_bottom"
							<%if (show_shadow_bottom) out.print("checked='checked'");%>>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.loop" />
					</div>
					<div class="col-xs-8">
						<div>
							<input type="radio" name="display_mode" value="1"
								id="loop_forever" onclick="loopRadioBtn(1);">
							<iwcm:text key="components.carousel_slider.loopForever" />
						</div>
						<div style="margin-top: 3px;">
							<input type="radio" name="display_mode" value="2" id="loop_count"
								onclick="loopRadioBtn(2);">
							<iwcm:text key="components.carousel_slider.loopEndAfter" />
							: <input type="number" name="loop_number" id="loop_number"
								min="0" size="5" value="<%=loop_number%>" style="width: 80px">
						</div>
					</div>
				</div>
				<div class="col-xs-12 form-group">
					<div class="col-xs-4 text-right paddingTop4">
						<iwcm:text key="components.carousel_slider.interval" />
					</div>
					<div class="col-xs-8">
						<input type="number" name="autoplay_interval" min="500" step="1"
							value="<%=autoplay_interval%>" style="width: 103px;">
					</div>
				</div>
			</div>
		</div>
	</form>
	<div class="tab-page" id="tabMenu2"
		style="display: none;">
		<div id="editorWrapper" class="editorWrapper"></div>

		<input type="button" id="addItem" class="button50 button50grey"
			value="<iwcm:text key="components.carousel_slider.addItem"/>">
		<!-- <input type="button" id="saveEditor" value="Save"> -->
	</div>

</div>

<jsp:include page="/components/bottom.jsp" />
