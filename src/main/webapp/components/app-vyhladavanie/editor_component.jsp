
<% // aplikacia na vlozenie sluzby disqus do webstránky
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
	request.setAttribute("cmpName", "app-vyhladavanie_comments");
	request.setAttribute("titleKey", "components.app-vyhladavanie.title");
	request.setAttribute("descKey", "components.app-vyhladavanie.desc");
	request.setAttribute("iconLink", "/components/app-vyhladavanie/menuicon.png");
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

	function getDisqusCommnets() {
		return "!INCLUDE(/components/app-vyhladavanie/vyhladavanie.jsp, customSearchId="+ document.textForm.id.value+")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getDisqusCommnets());
		return true;
	}
	//]]>
</script>

<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:790px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">
<div>

		<div class="form-group clearfix">
			<div class="col-sm-12">
			<iwcm:text
					key="components.app-vyhladavanie_info" />
			</div>
		</div>

	<div class="form-group clearfix">
			<div class="col-sm-3"><iwcm:text
					key="components.app-vyhladavanie.id" />:</div>
			<div class="col-sm-9"><input type="text" name="id"
				value="<%=pageParams.getValue("customSearchId", "")%>" class="form-control"
				 /></div>
	</div>
		</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
