<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,org.apache.commons.codec.binary.Base64,org.apache.struts.util.ResponseUtils"%><%@
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
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	request.setAttribute("cmpName", "app-docsembed");
	request.setAttribute("titleKey",
			"components.app-docsembed.title");
	request.setAttribute("descKey",
			"components.app-docsembed.desc");
	request.setAttribute("iconLink",
			"/components/app-docsembed/editoricon.png");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	//out.println("pageParams: "+pageParams.getPageParams());
	String url = pageParams.getValue("url", "");
	///out.println("ENCODED: "+url);
	if (Tools.isNotEmpty(url))
	{
		//paramPageParams = Tools.replace(paramPageParams, "|", "=");
		Base64 b64 = new Base64();
		url = new String(b64.decode(url.getBytes()));
		//out.println("DECODED: "+url);
	}
%>

<jsp:include page="/components/top.jsp" />

<script type="text/javascript">
	//<![CDATA[
	function getAppDocsEmbed() {
		var url_coded = $("#url").val();
		var url = window.btoa(url_coded);
		return "!INCLUDE(/components/app-docsembed/embed.jsp, url="
				+ url + ", width="
				+ document.textForm.width.value + ", height="
				+ document.textForm.height.value + ")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getAppDocsEmbed());
		return true;
	}
	//]]>
</script>

<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:790px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">

		<div class="row">
			<div class="col-4"><label class="col-form-label"><iwcm:text
					key="components.app-docsembed.editor_components.url" />:</label></div>
			<div  class="col-8">
				<div class="input-group">
					<input type="text" name="url" id="url"
					value="<%=url%>"
					maxlength="250" size="60" class="form-control" />
					<div class="input-group-append">
						<button onclick="openLinkDialogWindow('textForm', 'url', null, '/');" class="btn btn-outline-secondary"><i class="far fa-crosshairs"></i></button>
					</div>
				</div>

			</div>
		</div>

		<div class="row">
			<div class="col-4"><label class="col-form-label"> <iwcm:text key="components.app-docsembed.editor_components.width" />:</label></div>
			<div class="col-8"><input type="text" name="width"
				placeholder="900" value="<%=pageParams.getValue("width", "900")%>"
				size="4" maxlength="4" /> px </div>
		</div>

		<div class="row">
			<div class="col-4"><label class="col-form-label"><iwcm:text
					key="components.app-docsembed.editor_components.height" />:</label></div>
			<div class="col-8"><input type="text" name="height" placeholder="700"
				value="<%=pageParams.getValue("height", "700")%>" size="4"
				maxlength="4" /> px </div>
		</div>

	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
