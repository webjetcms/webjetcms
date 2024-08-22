<%@page import="sk.iway.iwcm.gallery.ImageInfo"%>
<%@page import="sk.iway.iwcm.FileTools"%>
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %><%@page import="sk.iway.iwcm.PageParams,sk.iway.iwcm.Tools,org.json.JSONObject"%><%
PageParams pageParams=new PageParams(request);

String file = pageParams.getValue("file","");
String thumbnail = file.substring(0, file.lastIndexOf(".")) + ".jpg";

int defaultWidth = 425;
int defaultHeight = 355;

if (!FileTools.isFile(thumbnail)) {
	thumbnail = "/images/videologo.png";
}
else
{
	ImageInfo ii = new ImageInfo(thumbnail);
	if (ii.getWidth()>100) defaultWidth = ii.getWidth();
	if (ii.getHeight()>50) defaultHeight = ii.getHeight();
}

if (Tools.isEmpty(file))
{
	return;
}

final int TYPE_YOUTUBE = 0;
final int TYPE_VIMEO = 1;
final int TYPE_HTML = 2;
final int TYPE_FACEBOOK = 3;

String width 	= pageParams.getValue("width", "" + defaultWidth);
int height 		= pageParams.getIntValue("height", defaultHeight);
int autoplay 	= pageParams.getIntValue("autoplay", -1);
int controls 	= pageParams.getIntValue("controls", -1);
int branding 	= pageParams.getIntValue("branding", -1);
int rel 		 	= pageParams.getIntValue("rel", -1);
int showinfo 	= pageParams.getIntValue("showinfo", -1);
int fullscreen = pageParams.getIntValue("fullscreen", -1);
int portrait 	= pageParams.getIntValue("portrait", -1);
int byline 		= pageParams.getIntValue("byline", -1);
int badge 		= pageParams.getIntValue("badge", -1);
int percentageWidth = pageParams.getIntValue("percentageWidth", 0);
String align = pageParams.getValue("align", "left");

if (pageParams.getIntValue("width", -1)==-1 && percentageWidth==0)
{
	//vlozene cez ikonu obrazok, nastavime na 100%
	percentageWidth = 100;
}


int videoCounter = 1;
if (request.getAttribute("videoCounter") != null)
{
	videoCounter = Tools.getIntValue(request.getAttribute("videoCounter").toString(), 1) + 1;
}
request.setAttribute("videoCounter", videoCounter);

/*
*	Youtube video moze byt vo formatoch:
*	http://www.youtube.com/watch?v=mThslEcqO1g
*	http://www.youtube.com/v/mThslEcqO1g
*   http://www.youtu.be/watch?v=mThslEcqO1g
*	http://www.youtu.be/v/mThslEcqO1g
*/
int type = TYPE_HTML;
String link = "";

String iframeVersion = "";
String allowFullscreen = "";

if (file.indexOf("youtube.com") != -1 || file.indexOf("youtu.be") != -1)
{

	if (file.indexOf("youtu.be")!=-1 && file.indexOf("v=") == -1 && file.indexOf("/v/") == -1){
		file = file.substring(file.indexOf(".be/") + 4);
	}
	else if (file.indexOf("v=") != -1)
	{
		file = file.substring(file.indexOf("v=") + 2);
	}
	else if (file.indexOf("/v/") != -1)
	{
		file = file.substring(file.indexOf("/v/") + 3);
	}
	else if (file.indexOf("/shorts/") != -1)
	{
		file = file.substring(file.indexOf("/shorts/") + 8);
	}

	link = "http://www.youtube.com/v/" + file + "?version=3";
	if(percentageWidth > 0) {
		iframeVersion += "<div class=\"embed-responsive embed-responsive-16by9 ratio ratio-16x9 video_align-"+align+" clearfix videoPlaceholder"+ videoCounter +" \" >";
	}
	if(fullscreen > 0)  allowFullscreen = "allowfullscreen";
	iframeVersion += "<iframe class=\"embed-responsive-item\" id=\"videoPlaceholder"+ videoCounter +"\" type=\"text/html\" width=\""+ width +"\" height=\""+ height +"\" src=\"//www.youtube.com/embed/"+ file +"?enablejsapi=1&showinfo="+showinfo+"&autoplay="+autoplay+"&modestbranding="+branding+"&controls="+controls+"&rel="+rel+"&origin=" + Tools.getBaseHref(request) + "\" frameborder=\"0\" "+allowFullscreen+"></iframe>";

	if(percentageWidth >0) {
		iframeVersion += "</div>";
	}

	type = TYPE_YOUTUBE;
}

/*
*	Vimeo video moze byt vo formate:
*	http://vimeo.com/29950141
*/
else if (file.indexOf("vimeo.com") != -1)
{
	file = file.substring(file.indexOf("vimeo.com/") + 10);
	link = "http://www.vimeo.com/moogaloop.swf";

	if(percentageWidth > 0) {
		iframeVersion += "<div class=\"embed-responsive embed-responsive-16by9 ratio ratio-16x9 clearfix video_align-"+align+" videoPlaceholder"+ videoCounter +" \" >";
	}

	if(fullscreen > 0)  allowFullscreen = "webkitallowfullscreen mozallowfullscreen allowfullscreen";
	iframeVersion += "<iframe class=\"embed-responsive-item\" id=\"videoPlaceholder"+ videoCounter +"\" src=\"//player.vimeo.com/video/"+ file +"?autoplay="+autoplay+"&portrait="+portrait+"&title="+showinfo+"&badge="+badge+"&byline="+byline+"\" width=\""+ width +"\" height=\""+ height +"\" frameborder=\"0\" "+allowFullscreen+"></iframe>";

	if(percentageWidth > 0) {
		iframeVersion += "</div>";
	}

	type = TYPE_VIMEO;
}
else if (file.indexOf("facebook.com/") != -1) {
	%>
		<div id="fb-root"></div>
		<script async defer src="https://connect.facebook.net/en_US/sdk.js#xfbml=1&version=v3.2"></script>
	<%

	if(percentageWidth > 0) {
		iframeVersion += "<div class=\"embed-responsive embed-responsive-16by9 ratio ratio-16x9 video_align-"+align+" clearfix videoPlaceholder"+ videoCounter +" \" >";
	}
	String allowAutoplay = "";
	String allowByline = "";
	String allowShowinfo = "";
	if(fullscreen > 0)  allowFullscreen = "true";
	else allowFullscreen = "false";
	if(autoplay > 0)  allowAutoplay = "true";
	else  allowAutoplay = "false";
	if(byline > 0)  allowByline = "true";
	else allowByline = "false";
	if(showinfo > 0)  allowShowinfo = "true";
	else  allowShowinfo = "false";
	iframeVersion += "<div class=\"fb-video\" data-href=\""+file+"\" data-width=\""+width+"\" data-allowfullscreen=\""+allowFullscreen+"\" data-autoplay=\""+autoplay+"\" data-show-captions=\""+byline+"\" data-show-text=\""+showinfo+"\"></div>";
	if(percentageWidth >0) {
		iframeVersion += "</div>";
	}

	type = TYPE_FACEBOOK;
}
else if (file.indexOf(".mp4") != -1) {
	type = TYPE_HTML;
}
%>
<style>
	.embed-responsive.videoPlaceholder<%=videoCounter%> {
		position: relative;
		display: block;
		width: <%=percentageWidth%>%;
		overflow: hidden;
	}
	.embed-responsive .embed-responsive-item, .embed-responsive embed, .embed-responsive iframe, .embed-responsive object, .embed-responsive video {
		width: 100%;
	}
	.video_align-left{
		left: 0%;
	}
	.video_align-center{
		left: 50%;
		transform: translateX(-50%);
		-webkit-transform: translateX(-50%);
		-ms-transform: translateX(-50%);
	}
	.video_align-right{
		left: 100%;
		transform: translateX(-100%);
		-webkit-transform: translateX(-100%);
		-ms-transform: translateX(-100%);
	}
</style>
<% if (type == TYPE_VIMEO || type == TYPE_YOUTUBE || type == TYPE_FACEBOOK ) { %>
	<div class="videoBox videoBox<%=videoCounter%>" style="text-align:<%=align%>">
		<%= iframeVersion %>
	</div>
<% }
	else if (type == TYPE_HTML) {
%>
	<% if (request.getAttribute("videojsIncluded")==null) { %>
		<link href="/components/video/videojs/video-js.css" rel="stylesheet">
		<!-- Support for IE8 -->
		<script src="/components/video/videojs/videojs-ie8.min.js"></script>
	<% } %>

<%
	String sizeOptions = "width=\""+width+"\" height = \""+height+"\"";
	if(percentageWidth > 0) {
		sizeOptions = "style=\"width: 100%; height:auto; \"";

		%>
<style>
	.video_<%=videoCounter%> .video-js{width:<%=percentageWidth%>% !important; height: auto; }
	.video_<%=videoCounter%> .video-js.audio{width:<%=percentageWidth%>% !important; height: 30px; }
</style>

<%
	}

	String poster = "";
	String posterPath = Tools.replace(file, ".mp4", ".jpg");
	if (posterPath.indexOf("?")>1) posterPath=posterPath.substring(0, posterPath.indexOf("?"));
	if (FileTools.isFile(posterPath))
	{
		poster = " poster=\""+posterPath+"\"";
	}

	if(file.indexOf(".mp3")!=-1) {	%>
		<div class="video-wrapper video_<%=videoCounter%>">

				<audio controlsList="nodownload nofullscreen" id="video_<%=videoCounter%>" class="video-js audio video_align-<%=align%>" controls  data-setup="{}">
					<source src="<%= file %>" type='audio/mp3'>

					<p class="vjs-no-js">
						To view this video please enable JavaScript, and consider upgrading to a web browser that
						<a href="http://videojs.com/html5-video-support/" target="_blank">supports HTML5 video</a>
					</p>
				</audio>
		</div>
	<% } else{ %>
		<div class="video-wrapper video_<%=videoCounter%>">
			<video controlsList="nodownload" id="video_<%=videoCounter%>" class="video-js video_align-<%=align%>" controls preload="auto" <%=sizeOptions%><%=poster%>
				   data-setup="{}">
				<source src="<%= file %>" type='video/mp4'>

				<p class="vjs-no-js">
					To view this video please enable JavaScript, and consider upgrading to a web browser that
					<a href="http://videojs.com/html5-video-support/" target="_blank">supports HTML5 video</a>
				</p>
			</video>
		</div>
	<% }%>
	<% if (request.getAttribute("videojsIncluded")==null) { %>
		<script src="/components/video/videojs/video.js"></script>
		<% request.setAttribute("videojsIncluded", true); %>
	<% } %>
<%}
%>