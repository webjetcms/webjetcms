
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
	request.setAttribute("cmpName", "app-facebook_like");
	request.setAttribute("titleKey", "components.app-facebook_like.title");
	request.setAttribute("descKey", "components.app-facebook_like.desc");
	request.setAttribute("iconLink", "/components/app-facebook_like/editoricon.png");
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

	function getFacebookLike() {

		var dataHrefLike = "lajkovat_cely_web";
		if (document.forms['textForm'].dataHrefLike[1].checked)
			dataHrefLike = "lajkovat_aktualne";

		var layoutLikeButton = "standard";
		if (document.forms['textForm'].layoutLikeButton[1].checked)
			layoutLikeButton = "button_count";

		var actionLikeButton = "like";
		if (document.forms['textForm'].actionLikeButton[1].checked)
			actionLikeButton = "recommend";

		return "!INCLUDE(/components/app-facebook_like/facebook_like.jsp, dataHrefLike="
				+ dataHrefLike
				+ ", widthLike="
				+ document.forms['textForm'].widthLike.value
				+ ", actionLikeButton="
				+ actionLikeButton
				+ ", layoutLikeButton=" + layoutLikeButton + ")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getFacebookLike());
		return true;
	}
	//]]>
</script>


<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">
<div class="row">

		<div class="form-group clearfix">


		<div class="form-group clearfix">
			<div class="col-xs-4"><iwcm:text
					key="components.app-facebook_like.lajkovat_cely_web" />:</div>
			<div class="col-xs-8" width="200px"><input type="radio" name="dataHrefLike"
				value="lajkovat_cely_web" "<%if (pageParams.getValue("dataHrefLike", null) == null
						|| "lajkovat_cely_web".equals(pageParams.getValue("dataHrefLike", null)))
				out.print(" checked='checked'");%>" />
			</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-4"><iwcm:text
					key="components.app-facebook_like.lajkovat_aktualne" />:</div>
			<div class="col-xs-8" ><input type="radio" name="dataHrefLike"
				value="lajkovat_aktualne" "<%if ("lajkovat_aktualne".equals(pageParams.getValue("dataHrefLike", null)))
				out.print(" checked='checked'");%>" />
			</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-4" ><iwcm:text
					key="components.app-facebook_like.width" />:</div>
			<div class="col-xs-8" ><input type="text" name="widthLike"
				value="<%=pageParams.getValue("widthLike", "980")%>" size="4"
				maxlength="4" /> px</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-4"><iwcm:text
					key="components.app-facebook_like.layout" />:</div>
			<div class="col-xs-8" ><input type="radio" name="layoutLikeButton"
				value="standard" "<%if (pageParams.getValue("layoutLikeButton", null) == null
						|| "standard".equals(pageParams.getValue("layoutLikeButton", null)))
				out.print(" checked='checked'");%>" />
				<iwcm:text key="components.app-facebook_like.layout.standard" />
				<input type="radio" name="layoutLikeButton" value="button_count" "<%if ("button_count".equals(pageParams.getValue("layoutLikeButton", null)))
				out.print(" checked='checked'");%>" />
				<iwcm:text
					key="components.app-facebook_like.layout.button_count" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-4"><iwcm:text
					key="components.app-facebook_like.actionType" />:</div>
			<div class="col-xs-8"><input type="radio" name="actionLikeButton"
				value="like" "<%if (pageParams.getValue("actionLikeButton", null) == null
						|| "like".equals(pageParams.getValue("actionLikeButton", null)))
				out.print(" checked='checked'");%>" />
				<iwcm:text key="components.app-facebook_like.actionType.like" />
				<input type="radio" name="actionLikeButton" value="recommend" "<%if ("recommend".equals(pageParams.getValue("actionLikeButton", null)))
				out.print(" checked='checked'");%>" />
				<iwcm:text
					key="components.app-facebook_like.actionType.recommend" /></div>
		</div>
</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
