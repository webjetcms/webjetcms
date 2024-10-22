
<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*,org.apache.commons.codec.binary.Base64"%><%@
taglib prefix="iwcm"
	uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	PageParams pageParams = new PageParams(request);
	Prop prop = Prop.getInstance(lng);
	String encoded = pageParams.getValue("url", "");
	Base64 b64 = new Base64();
	String decoded = new String(b64.decode(encoded.getBytes()));
	//System.out.println("decoded: " + decoded);
	String width = pageParams.getValue("width", "100%");
	String height = pageParams.getValue("height", "700");

	int embedCounter = request.getAttribute("embedCounter") != null ? (Integer) request.getAttribute("embedCounter") : 0;
    embedCounter++;
    request.setAttribute("embedCounter", embedCounter);

	if (Tools.isEmpty(decoded)) {
		out.println("<div id='docsembed-"+embedCounter+"'><iframe width='100%' frameborder='0' src='data:text/html;charset=utf-8,%3Cdiv%3ENo%20preview%20available%3C/div%3E'></iframe></div>");
		return;
	}

	if (decoded.indexOf("http")==-1) decoded = Tools.getBaseHref(request) + decoded;

%>

<script>
$(document).ready(function() {

    var $embedContainer = $('#docsembed-<%=embedCounter%>');
	var $pdfViewer = $('<iframe src="https://docs.google.com/viewer?url=' + encodeURIComponent($embedContainer.data('url')) + '&embedded=true" width="<%=width%>" height="<%=height%>" style="border: none; background-color: rgb(154,160,166);"></iframe>');
	var embedTimer;
	var reloadCounter = 0;
    $pdfViewer.on("load", function(event){
		//console.log("loaded, event=", event);
        $embedContainer.find('.loader').remove();
		clearInterval(embedTimer);
    });
	$pdfViewer.appendTo($embedContainer);
	embedTimer = setInterval(function(){
		reloadCounter++;
		if (reloadCounter > 10) {
			clearInterval(embedTimer);
			return;
		}
		var url = $embedContainer.data('url');
		if (url.indexOf("?") > -1) {
			url += "&";
		} else {
			url += "?";
		}
		url += "v="+reloadCounter;
		console.log("Reloading PDF, counter=", reloadCounter, "url=", url);
		$pdfViewer.attr('src', src='https://docs.google.com/viewer?url=' + encodeURIComponent(url) + '&embedded=true');
	}, 14000);

});
</script>

<div id="docsembed-<%=embedCounter%>" class="docsembed" data-url="<%=decoded%>">
	<span class="loader"><iwcm:text key="components.app-docsembed.waitPlease"/></span>
</div>