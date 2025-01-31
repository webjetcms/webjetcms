<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*,sk.iway.iwcm.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@page import="org.apache.struts.util.ResponseUtils"%>
<iwcm:checkLogon admin="true" perms="menuWebpages"/>
<%

request.setAttribute("cmpName", "video");

%>
<%@page import="sk.iway.iwcm.system.ConfDB"%>
<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.Constants"%>
<%@page import="sk.iway.iwcm.Tools"%>
<jsp:include page="/components/top.jsp"/>

<%-- <script src="/components/form/check_form.js" type="text/javascript"></script> --%>
<script src="/components/form/fix_e.js" type="text/javascript"></script>
<%=Tools.insertJQuery(request) %>
<script src="/components/form/event_attacher.js" type="text/javascript"></script>
<script src="/components/form/class_magic.js" type="text/javascript"></script>
<%-- <script src="/components/form/check_form_impl.jsp" type="text/javascript"></script> --%>

<style type="text/css">
	td.main {padding: 0px;}

	div.players {display: none; padding: 10px 10px 0 10px; background-color: #f5f5f5;}
	div.box_tab {display: none;}

	a.choose {float: right;}

	input {border: 0 none;}
	input.text {width: 250px; height: 25px; line-height: 25px; padding: 0 5px; border: 1px solid #BDBCBC}

	div.choose {background-color: #f5f5f5;}
	div.choose ul {margin: 0; padding: 0; height: 200px;}
	div.choose ul li {list-style: none; margin: 0px; padding: 0px; float: left; height: 100%; width: 25%;}
	div.choose ul li a {display: block; text-indent: -9999px; outline: none; height: 100%;}
	div.choose ul li.youtube a {background: url(/components/video/logo_youtube.png) center center no-repeat;}
	div.choose ul li.youtube a:hover {background: url(/components/video/logo_youtube_color.png) center center no-repeat;}
	div.choose ul li.vimeo {left: 50%; right: 0px;}
	div.choose ul li.vimeo a {background: url(/components/video/logo_vimeo.png) center center no-repeat;}
	div.choose ul li.vimeo a:hover {background: url(/components/video/logo_vimeo_color.png) center center no-repeat;}
	div.choose ul li.facebook a {background: url(/components/video/logo_facebook.png) center center no-repeat; background-size: 100px; }
	div.choose ul li.facebook a:hover {background: url(/components/video/logo_facebook_color.png) center center no-repeat; background-size: 100px; }
	div.choose ul li.video a {background: url(/components/video/logo_video.png) center center no-repeat;}
	div.choose ul li.video a:hover {background: url(/components/video/logo_video_color.png) center center no-repeat;}

	div.box_tab {padding-top: 11px;}
	ul.tab_menu {background: transparent; padding-top: 2px;}
</style>

<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	//out.println(paramPageParams);
	//System.out.println(paramPageParams);
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String align = pageParams.getValue("align", "left");
	String openPage = "";
	String file = ResponseUtils.filter(pageParams.getValue("file", ""));
	if (Tools.isNotEmpty(file))
	{
		if (file.contains("youtube.com") || file.contains("youtu.be")) {
			openPage = "youtube";
		}
		else if (file.contains("vimeo.com")) {
			openPage = "vimeo";
		}
		else if (file.contains("facebook.com")) {
			openPage = "facebook";
		}
		else {
			openPage = "video";
		}
	}
%>

<script type='text/javascript'>

$(document).ready(function(){
	$('div.video #file').on('change blur', function(){
		var data = {
			imageUrl: $('div.video #file').val()
		};

		$.ajax({
			url: '/components/_common/image_info_ajax.jsp',
			data: data,
			dataType: 'json',
			success: function(data)
			{
				if (typeof data.width == "undefined" && typeof data.height == "undefined")
				{
					$('#widthTypeResponsive3').click();
				}
				else
				{
					$('#videoForm #width').val(data.width);
					$('#videoForm #height').val(data.height);
				}
			}
		});
	});


    $('input[name=widthType]').on('change', function() {
        if (this.value == 'fixed') {
            $(".responsiveWidth").hide();
			$("input[name=percentageWidth]").val(0);
			$(".facebook input[name=width]").val(425);
			$(".fixedWidth").show();
        }
        else if (this.value == 'responsive') {
			$(".responsiveWidth").show();
			$(".facebook .responsiveWidth").hide();
			$(".fixedWidth").hide();
			$("input[name=percentageWidth]").val(100);
			$(".facebook input[name=percentageWidth]").val("auto");
			$(".facebook input[name=width]").val("auto");
        }
    });

    if('<%=pageParams.getIntValue("percentageWidth",0)%>' > 0 ){
        $('input[name=widthType][value=responsive]').prop("checked",true);
		$(".responsiveWidth").show();
		$(".facebook .responsiveWidth").hide();
		$(".fixedWidth").hide();
		function facebookWidth() {
			var openPage = "<%=openPage%>";
			if(openPage == "facebook") {
				$(".facebook input[name=width]").val("auto");
				$(".facebook input[name=percentageWidth]").val("auto");
			}
		}
	}

	$("div.choose li.youtube a").click(function(){
		$("div.choose").hide();

		$(".box_tab").show();
	   	$("div.youtube").show();

	   	resize("youtube");
	});

	$("div.choose li.vimeo a").click(function(){
		$("div.choose").hide();

		$(".box_tab").show();
		$("div.vimeo").show();

		resize("vimeo");
	});

	$("div.choose li.facebook a").click(function(){
		$("div.choose").hide();

		$(".box_tab").show();
		$("div.facebook").show();

		resize("facebook");
	})

	$("div.choose li.video a").click(function(){
		$("div.choose").hide();

		$(".box_tab").show();
	   	$("div.video").show();

	   	resize("video");
	});


	$("a.choose").click(function(){
		$("div.players").hide();
		$(".box_tab").hide();

		$("div.choose").show();

		resize("choose");
	});

	var openPage = "<%=openPage%>";

	if (openPage != "") {
		$("div.choose").hide();

		$(".box_tab").show();
		$("div." + openPage).show();

		resize(openPage);
	}
	else {
		resize("choose");
	}
});

function resize(page)
{
	var resizeMap = {
			youtube: [800, 535],
			vimeo: [800, 535],
			facebook: [800, 535],
			video: [800, 400],
			choose: [800, 270]
	}

	if (typeof window.CKEDITOR == 'undefined') {

		var w = window;
		if (typeof window.parent.window.CKEDITOR) {
			w = window.parent.window;
		}

		if (typeof window.parent.window.parent.window.CKEDITOR) {
			w = window.parent.window.parent.window;
		}

		w.resizeDialogCK(resizeMap[page][0], resizeMap[page][1]);
	}
	else {
		resizeDialogCK(resizeMap[page][0], resizeMap[page][1]);
	}
}

function getCheckboxValue(name) {
	var form = $("div.players:visible");
	return form.find("[name="+ name +"]:checked").val();
}

function Ok()
{
	var form = $("div.players:visible");

	if (form.length == 0) {
		return false;
	}

	var values = {
		file: form.find("#file").val()
	}

	if (form.find("#width").val() != "") {
		values.width = form.find("#width").val()
	}

	if (form.find("#height").val() != "") {
		values.height = form.find("#height").val();
	}

	if (values.file == "") {
		alert("Field file is required");
		return false;
	}

    values.percentageWidth = $("#percentageWidthHtml").val();
    values.align 	= $("input[name=videoAlign]:checked").val();

	/* youtube */
	if (form.hasClass("youtube")) {
		values.autoplay 	= getCheckboxValue("autoplay");
		values.controls 	= getCheckboxValue("controls");
		values.fullscreen 	= getCheckboxValue("fullscreen");
		values.branding 	= getCheckboxValue("branding");
		values.rel 			= getCheckboxValue("rel");
		values.showinfo 	= getCheckboxValue("showinfo");
		values.percentageWidth 	= $("#percentageWidthYT").val();
		values.align 	= $("input[name=videoAlignYT]:checked").val();
	}
	/* vimeo */
	else if (form.hasClass("vimeo")) {
		values.fullscreen = getCheckboxValue("fullscreen");
		values.autoplay 	= getCheckboxValue("autoplay");
		values.showinfo 	= getCheckboxValue("showinfo");
		values.portrait 	= getCheckboxValue("portrait");
		values.byline 		= getCheckboxValue("byline");
		values.badge 		= getCheckboxValue("badge");
        values.percentageWidth 	= $("#percentageWidthVimeo").val();
        values.align 	= $("input[name=videoAlignVimeo]:checked").val();
	}
	/* facebook */
	else if (form.hasClass("facebook")) {
		values.fullscreen = getCheckboxValue("fullscreen");
		values.autoplay 	= getCheckboxValue("autoplay");
		values.showinfo 	= getCheckboxValue("showinfo");
		values.portrait 	= getCheckboxValue("portrait");
		values.byline 		= getCheckboxValue("byline");
		values.badge 		= getCheckboxValue("badge");
        values.percentageWidth 	= $("#percentageWidthFacebook").val();
        values.align 	= $("input[name=videoAlignFacebook]:checked").val();
	}

	var html = '!INCLUDE(/components/video/video_player.jsp';
	$.each(values, function(index, value){
		if (typeof value != "undefined" || value != "") {
			html += ", " + index + "=" + String(value).replace('"','').replace('"','');
		}

	});

	html += ")!";

	oEditor.FCK.InsertHtml(html);

	console.log(form);
	console.log(form);
	if (form.hasClass("video") && values.file.indexOf(".mp4") == 0) {
		alert("Video musí byť vo formáte mp4");
		return false;
	}

	return true;
}
</script>

<link type="text/css" rel="stylesheet" media="screen" href="/admin/css/tabpane-luna.css" />
<div class="box_tab box_tab_thin left">
	<ul class="tab_menu" id="Tabs">
		<li class="first last openFirst"><a href="#" onclick="showHideTab('1');" id="tabLink1"><iwcm:text key="components.universalComponentDialog.title"/></a></li>
	</ul>
</div>
<div class="choose">
	<ul>
		<li class="youtube"><a href="javascript:;">Youtube</a></li>
		<li class="vimeo"><a href="javascript:;">Vimeo</a></li>
		<li class="facebook"><a href="javascript:;">Facebook</a></li>
		<li class="video"><a href="javascript:;">Video</a></li>
	</ul>
</div>

<div class="youtube players">
	<form method="get" name="youtubeForm" style="margin: 0px;" id="youtubeForm">
		<a class="choose Button button green" href="javascript:;"><iwcm:text key="button.back"/></a>
		<h1><iwcm:text key="components.video_player.youtube"/></h1>

		<div class="row">
			<div class="form-group col-sm-12">
				<label for="file" class="control-label"><iwcm:text key="components.video_player.file"/></label>
				<input class="form-control" name="file" value="<%=ResponseUtils.filter(pageParams.getValue("file", ""))%>" id="file" />
			</div>
			<div class="form-group col-sm-12">

				<input type="radio" id="widthTypeFixed1" name="widthType" value="fixed">
				<label for="widthTypeFixed1" class="control-label"><iwcm:text key="components.video_player.fixedWidth"/></label>
				<input type="radio" id="widthTypeResponsive1" name="widthType" value="responsive">
				<label for="widthTypeResponsive1" class="control-label"><iwcm:text key="components.video_player.responsiveWidth"/></label><br>
			</div>

			<div class="form-group col-sm-12">
				<iwcm:text key="components.video_player.videoAlign"/> <br>
				<input type="radio" id="videoAlign-left1" name="videoAlignYT" value="left" <%=(align.equals("left")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-left1" class="control-label"><iwcm:text key="components.video_player.videoAlign-left"/></label>
				<input type="radio" id="videoAlign-center1" name="videoAlignYT" value="center" <%=(align.equals("center")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-center1" class="control-label"><iwcm:text key="components.video_player.videoAlign-center"/></label>
				<input type="radio" id="videoAlign-right1" name="videoAlignYT" value="right" <%=(align.equals("right")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-right1" class="control-label"><iwcm:text key="components.video_player.videoAlign-right"/></label><br>
			</div>

			<div class="form-group col-sm-6 fixedWidth">
				<label for="width" class="control-label"><iwcm:text key="components.video_player.width"/></label>
				<input class="form-control" type="input" name="width" value="<%=ResponseUtils.filter(pageParams.getValue("width", "425"))%>" id="width" />
			</div>

			<div class="form-group col-sm-6 fixedWidth">
				<label for="height" class="control-label"><iwcm:text key="components.video_player.height"/></label>
				<input class="form-control" type="input" name="height" value="<%=ResponseUtils.filter(pageParams.getValue("height", "355"))%>" id="height" />
			</div>

			<div class="form-group col-sm-12 responsiveWidth" style="display: none">
				<label for="percentageWidthYT" class="control-label"><iwcm:text key="components.video_player.width"/> (%)</label>
				<input class="form-control" type="input" name="percentageWidth" value="<%=ResponseUtils.filter(pageParams.getValue("percentageWidth","0"))%>" id="percentageWidthYT" />
			</div>




			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.autoplay" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vimeoAutoplayOn" name="autoplay" value="1" <%= (pageParams.getIntValue("autoplay", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vimeoAutoplayOff" name="autoplay" value="0" <%= (pageParams.getIntValue("autoplay", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.fullscreen" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vFullscreenOn" name="fullscreen" value="1" <%= (pageParams.getIntValue("fullscreen", 1) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vFullscreenOff" name="fullscreen" value="0" <%= (pageParams.getIntValue("fullscreen", 1) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>

			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.showinfo" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vShowinfoOn" name="showinfo" value="1" <%= (pageParams.getIntValue("showinfo", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vShowinfoOff" name="showinfo" value="0" <%= (pageParams.getIntValue("showinfo", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.controls" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="controlsOn" name="controls" value="1" <%= (pageParams.getIntValue("controls", 1) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="controlsOff" name="controls" value="0" <%= (pageParams.getIntValue("controls", 1) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>

			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.branding" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="brandingOn" name="branding" value="1" <%= (pageParams.getIntValue("branding", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="brandingOff" name="branding" value="0" <%= (pageParams.getIntValue("branding", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.rel" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="relOn" name="rel" value="1" <%= (pageParams.getIntValue("rel", 1) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="relOff" name="rel" value="0" <%= (pageParams.getIntValue("rel", 1) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>
		</div>
	</form>
</div>

<div class="vimeo players">
	<form method="get" name="youtubeForm" style="margin: 0px;" id="youtubeForm">
		<a class="choose Button" href="javascript:;"><iwcm:text key="button.back"/></a>
		<h1><iwcm:text key="components.video_player.vimeo"/></h1>

		<div class="row">
			<div class="form-group col-sm-12">
				<label for="file" class="control-label"><iwcm:text key="components.video_player.file_vimeo"/></label>
				<input class="form-control" name="file" value="<%=ResponseUtils.filter(pageParams.getValue("file", ""))%>" id="file" />
			</div>
			<div class="form-group col-sm-12">

				<input type="radio" id="widthTypeFixed2" name="widthType" value="fixed">
				<label for="widthTypeFixed2" class="control-label"><iwcm:text key="components.video_player.fixedWidth"/></label>
				<input type="radio" id="widthTypeResponsive2" name="widthType" value="responsive">
				<label for="widthTypeResponsive2" class="control-label"><iwcm:text key="components.video_player.responsiveWidth"/></label><br>
			</div>
			<div class="form-group col-sm-12">
				<iwcm:text key="components.video_player.videoAlign"/> <br>
				<input type="radio" id="videoAlign-left2" name="videoAlignVimeo" value="left" <%=(align.equals("left")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-left2" class="control-label"><iwcm:text key="components.video_player.videoAlign-left"/></label>
				<input type="radio" id="videoAlign-center2" name="videoAlignVimeo" value="center" <%=(align.equals("center")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-center2" class="control-label"><iwcm:text key="components.video_player.videoAlign-center"/></label>
				<input type="radio" id="videoAlign-right2" name="videoAlignVimeo" value="right" <%=(align.equals("right")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-right2" class="control-label"><iwcm:text key="components.video_player.videoAlign-right"/></label><br>
			</div>
			<div class="form-group col-sm-6 fixedWidth">
				<label for="width" class="control-label"><iwcm:text key="components.video_player.width"/></label>
				<input class="form-control" type="input" name="width" value="<%=ResponseUtils.filter(pageParams.getValue("width", "425"))%>" id="width" />
			</div>

			<div class="form-group col-sm-6 fixedWidth">
				<label for="height" class="control-label"><iwcm:text key="components.video_player.height"/></label>
				<input class="form-control" type="input" name="height" value="<%=ResponseUtils.filter(pageParams.getValue("height", "355"))%>" id="height" />
			</div>

			<div class="form-group col-sm-12 responsiveWidth" style="display: none">
				<label for="percentageWidthVimeo" class="control-label"><iwcm:text key="components.video_player.width"/> (%)</label>
				<input class="form-control" type="input" name="percentageWidth" value="<%=ResponseUtils.filter(pageParams.getValue("percentageWidth","0"))%>" id="percentageWidthVimeo" />
			</div>

			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.autoplay" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vimeoAutoplayOn" name="autoplay" value="1" <%= (pageParams.getIntValue("autoplay", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vimeoAutoplayOff" name="autoplay" value="0" <%= (pageParams.getIntValue("autoplay", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.fullscreen" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vFullscreenOn" name="fullscreen" value="1" <%= (pageParams.getIntValue("fullscreen", 1) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vFullscreenOff" name="fullscreen" value="0" <%= (pageParams.getIntValue("fullscreen", 1) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>

			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.showinfo" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vShowinfoOn" name="showinfo" value="1" <%= (pageParams.getIntValue("showinfo", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vShowinfoOff" name="showinfo" value="0" <%= (pageParams.getIntValue("showinfo", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.portrait" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="portraitOn" name="portrait" value="1" <%= (pageParams.getIntValue("portrait", 1) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="portraitOff" name="portrait" value="0" <%= (pageParams.getIntValue("portrait", 1) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>

			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.byline" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="bylineOn" name="byline" value="1" <%= (pageParams.getIntValue("byline", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="bylineOff" name="byline" value="0" <%= (pageParams.getIntValue("byline", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.badge" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="badgeOn" name="badge" value="1" <%= (pageParams.getIntValue("badge", 1) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="badgeOff" name="badge" value="0" <%= (pageParams.getIntValue("badge", 1) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>
		</div>
	</form>
</div>

<div class="facebook players">
	<form method="get" name="youtubeForm" style="margin: 0px;" id="youtubeForm">
		<a class="choose Button" href="javascript:;"><iwcm:text key="button.back"/></a>
		<h1><iwcm:text key="components.video_player.facebook"/></h1>

		<div class="row">
			<div class="form-group col-sm-12">
				<label for="file" class="control-label"><iwcm:text key="components.video_player.file_facebook"/></label>
				<input class="form-control" name="file" value="<%=ResponseUtils.filter(pageParams.getValue("file", ""))%>" id="file" />
			</div>
			<div class="form-group col-sm-12">

				<input type="radio" id="widthTypeFixed4" name="widthType" value="fixed">
				<label for="widthTypeFixed4" class="control-label"><iwcm:text key="components.video_player.fixedWidth"/></label>
				<input type="radio" id="widthTypeResponsive4" name="widthType" value="responsive">
				<label for="widthTypeResponsive4" class="control-label"><iwcm:text key="components.video_player.responsiveWidthFacebook"/></label><br>
			</div>
			<div class="form-group col-sm-12">
				<iwcm:text key="components.video_player.videoAlign"/> <br>
				<input type="radio" id="videoAlign-left4" name="videoAlignFacebook" value="left" <%=(align.equals("left")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-left4" class="control-label"><iwcm:text key="components.video_player.videoAlign-left"/></label>
				<input type="radio" id="videoAlign-center4" name="videoAlignFacebook" value="center" <%=(align.equals("center")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-center4" class="control-label"><iwcm:text key="components.video_player.videoAlign-center"/></label>
				<input type="radio" id="videoAlign-right4" name="videoAlignFacebook" value="right" <%=(align.equals("right")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-right4" class="control-label"><iwcm:text key="components.video_player.videoAlign-right"/></label><br>
			</div>
			<div class="form-group col-sm-12 fixedWidth">
				<label for="width" class="control-label"><iwcm:text key="components.video_player.width"/></label>
				<input class="form-control" type="input" name="width" value="<%=ResponseUtils.filter(pageParams.getValue("width", "425"))%>" id="width" />
			</div>

			<%--div class="form-group col-sm-6 fixedWidth">
				<label for="height" class="control-label"><iwcm:text key="components.video_player.height"/></label>
				<input class="form-control" type="input" name="height" value="<%=ResponseUtils.filter(pageParams.getValue("height", "355"))%>" id="height" />
			</div--%>

			<div class="form-group col-sm-12 responsiveWidth" style="display: none">
				<label for="percentageWidthFacebook" class="control-label"><iwcm:text key="components.video_player.width"/> (%)</label>
				<input class="form-control" type="input" name="percentageWidth" value="<%=ResponseUtils.filter(pageParams.getValue("percentageWidth","0"))%>" id="percentageWidthFacebook" />
			</div>

			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.autoplay" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="facebookAutoplayOn" name="autoplay" value="1" <%= (pageParams.getIntValue("autoplay", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="facebookAutoplayOff" name="autoplay" value="0" <%= (pageParams.getIntValue("autoplay", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.fullscreen" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vFullscreenOn" name="fullscreen" value="1" <%= (pageParams.getIntValue("fullscreen", 1) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vFullscreenOff" name="fullscreen" value="0" <%= (pageParams.getIntValue("fullscreen", 1) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>

			<div class="form-group">
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.showinfoFacebook" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="vShowinfoOn" name="showinfo" value="1" <%= (pageParams.getIntValue("showinfo", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="vShowinfoOff" name="showinfo" value="0" <%= (pageParams.getIntValue("showinfo", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
				<div class="col-sm-6">
					<div class="control-label"><iwcm:text key="components.video_player.titles" /></div>

					<div class="row">
						<label class="col-sm-4 form-group">
					    	<input type="radio" id="bylineOn" name="byline" value="1" <%= (pageParams.getIntValue("byline", 0) == 1) ? "checked='checked'" : "" %> >
					    	<iwcm:text key="components.video_player.on"/>
					  	</label>

					  	<label class="col-sm-4 form-group">
					    	<input type="radio" id="bylineOff" name="byline" value="0" <%= (pageParams.getIntValue("byline", 0) == 0) ? "checked='checked'" : "" %>>
					    	<iwcm:text key="components.video_player.off"/>
					  	</label>
				  	</div>
				</div>
			</div>
		</div>
	</form>
</div>

<div class="video players">
	<form method="get" name="videoForm" style="margin: 0px;" id="videoForm">
		<a class="choose Button" href="javascript:;"><iwcm:text key="button.back"/></a>
		<h1><iwcm:text key="components.video_player.video"/></h1>

		<div class="row">
			<div class="form-group col-sm-12">
				<label for="file" class="control-label block"><iwcm:text key="components.video_player.file_video"/></label>
				<div class="input-group">
					<input class="form-control" name="file" value="<%=ResponseUtils.filter(pageParams.getValue("file", ""))%>" id="file" />
					<input type="button" class="btn btn-primary" onclick="openElFinderDialogWindow('videoForm', 'file')" value="Vybrať" name="videoSelect">
				</div>
			</div>
			<div class="form-group col-sm-12">

				<input type="radio" id="widthTypeFixed3" name="widthType" value="fixed">
				<label for="widthTypeFixed3" class="control-label"><iwcm:text key="components.video_player.fixedWidth"/></label>
				<input type="radio" id="widthTypeResponsive3" name="widthType" value="responsive">
				<label for="widthTypeResponsive3" class="control-label"><iwcm:text key="components.video_player.responsiveWidth"/></label><br>
			</div>
			<div class="form-group col-sm-12">
				<iwcm:text key="components.video_player.videoAlign"/> <br>
				<input type="radio" id="videoAlign-left3" name="videoAlign" value="left" <%=(align.equals("left")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-left3" class="control-label"><iwcm:text key="components.video_player.videoAlign-left"/></label>
				<input type="radio" id="videoAlign-center3" name="videoAlign" value="center" <%=(align.equals("center")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-center3" class="control-label"><iwcm:text key="components.video_player.videoAlign-center"/></label>
				<input type="radio" id="videoAlign-right3" name="videoAlign" value="right" <%=(align.equals("right")) ? "checked='checked'" : "" %>>
				<label for="videoAlign-right3" class="control-label"><iwcm:text key="components.video_player.videoAlign-right"/></label><br>
			</div>
			<div class="form-group col-sm-6 fixedWidth">
				<label for="width" class="control-label"><iwcm:text key="components.video_player.width"/></label>
				<input class="form-control" type="input" name="width" value="<%=ResponseUtils.filter(pageParams.getValue("width", ""))%>" id="width" />
			</div>

			<div class="form-group col-sm-6 fixedWidth">
				<label for="height" class="control-label"><iwcm:text key="components.video_player.height"/></label>
				<input class="form-control" type="input" name="height" value="<%=ResponseUtils.filter(pageParams.getValue("height", ""))%>" id="height" />
			</div>
			<div class="form-group col-sm-12 responsiveWidth" style="display: none">
				<label for="percentageWidthHtml" class="control-label"><iwcm:text key="components.video_player.width"/> (%)</label>
				<input class="form-control" type="input" name="percentageWidth" value="<%=ResponseUtils.filter(pageParams.getValue("percentageWidth","0"))%>" id="percentageWidthHtml" />
			</div>
		</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp"/>
