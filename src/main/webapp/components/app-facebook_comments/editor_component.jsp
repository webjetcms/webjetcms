
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
	request.setAttribute("cmpName", "app-facebook_comments");
	request.setAttribute("titleKey", "components.app-facebook_comments.title");
	request.setAttribute("descKey", "components.app-facebook_comments.desc");
	request.setAttribute("iconLink", "/components/app-facebook_comments/editoricon.png");
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

	function getFacebookCommnets() {
		return "!INCLUDE(/components/app-facebook_comments/facebook_commnets.jsp, numberComments="
				+ document.textForm.numberComments.value
				+ ", widthComments="
				+ document.textForm.widthComments.value + ")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getFacebookCommnets());
		return true;
	}
	//]]>
</script>

<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">
<div class="row">


		<div class="form-group clearfix">
			<div class="col-xs-4"><iwcm:text
					key="components.app-facebook_comments.numberComments" />:</div>
			<div class="col-xs-8"><input type="text" name="numberComments"
				value="<%=pageParams.getValue("numberComments", "5")%>" size="4"
				maxlength="2" /></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-4"><iwcm:text
					key="components.app-facebook_comments.widthComments" />:</div>
			<div class="col-xs-8"><input type="text" name="widthComments"
				value="<%=pageParams.getValue("widthComments", "980")%>" size="4"
				maxlength="4" /> px</div>
		</div>
		</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
