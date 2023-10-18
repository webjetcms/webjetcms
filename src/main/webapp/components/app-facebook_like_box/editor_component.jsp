
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%@
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
	request.setAttribute("cmpName", "app-facebook_like_box");
	request.setAttribute("titleKey", "components.app-facebook_like_box.title");
	request.setAttribute("descKey", "components.app-facebook_like_box.desc");
	request.setAttribute("iconLink", "/components/app-facebook_like_box/editoricon.png");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
%>

<jsp:include page="/components/top.jsp" />

<script type="text/javascript">
	//<![CDATA[

	function getFacebookLikeBox() {
		return "!INCLUDE(/components/app-facebook_like_box/facebook_like_box.jsp, dataHrefLikeBox="
				+ document.textForm.dataHrefLikeBox.value
				+ ", widthLikeBox="
				+ document.textForm.widthLikeBox.value
				+ ", heightLikeBox="
				+ document.textForm.heightLikeBox.value
				+ ", showFacesLikeBox="
				+ document.textForm.showFacesLikeBox.checked
				+ ", showPostLikeBox="
				+ document.textForm.showPostLikeBox.checked + ")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getFacebookLikeBox());
		return true;
	}
	//]]>
</script>
<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">
<div class="row">


		<div class="form-group clearfix">
			<div class="col-xs-4" nowrap="nowrap"><iwcm:text
					key="components.app-facebook_like_box.dataHref" />:</div>
			<div class="col-xs-8"><input type="text" name="dataHrefLikeBox"
				placeholder="https://www.facebook.com/interway.sk/"
				value="<%=pageParams.getValue("dataHrefLikeBox", "https://www.facebook.com/interway.sk/")%>"
				maxlength="250" /></div>
		</div>
	<div class="form-group clearfix">
		<div class="col-xs-4"><iwcm:text
				key="components.app-facebook_like_box.width" />:</div>
		<div class="col-xs-8"><input type="text" name="widthLikeBox"
			placeholder="980"
			value="<%=pageParams.getValue("widthLikeBox", "980")%>" size="4"
			maxlength="4" /> px</div>
	</div>
	<div class="form-group clearfix">
		<div class="col-xs-4"><iwcm:text
				key="components.app-facebook_like_box.height" />:</div>
		<div class="col-xs-8"><input type="text" name="heightLikeBox"
			value="<%=pageParams.getValue("heightLikeBox", "")%>" size="4"
			maxlength="4" /> px</div>
	</div>
	<div class="form-group clearfix">
		<div class="col-xs-4"><iwcm:text
				key="components.app-facebook_like_box.showFaces" />:</div>
		<div class="col-xs-8"><input type="checkbox" name="showFacesLikeBox"
			<%if (pageParams.getBooleanValue("showFacesLikeBox", true))
				out.print(" checked='checked'");%> /></div>
	</div>
	<div class="form-group clearfix">
		<div class="col-xs-4"><iwcm:text
				key="components.app-facebook_like_box.showPost" />:</div>
		<div class="col-xs-8"><input type="checkbox" name="showPostLikeBox"
			<%if (pageParams.getBooleanValue("showPostLikeBox", true))
				out.print(" checked='checked'");%> /></div>
	</div>
</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
