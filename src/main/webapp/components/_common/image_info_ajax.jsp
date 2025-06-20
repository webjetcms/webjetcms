<%@page import="sk.iway.iwcm.gallery.VideoConvert"%>
<%@page import="org.json.JSONObject"%><%@page import="sk.iway.iwcm.gallery.ImageInfo"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="displaytag" %><%
String imageUrl = Tools.getStringValue(Tools.getRequestParameter(request, "imageUrl"), "");

if (VideoConvert.isVideoFile(imageUrl)) {
	imageUrl = imageUrl.substring(0, imageUrl.lastIndexOf('.')) + ".jpg";
}

JSONObject result = new JSONObject();
if (Tools.isNotEmpty(imageUrl) && FileTools.isFile(imageUrl)) {
	ImageInfo imageInfo = new ImageInfo(imageUrl);

	result.put("width", imageInfo.getWidth());
	result.put("height", imageInfo.getHeight());
}

out.print(result.toString());
%>
