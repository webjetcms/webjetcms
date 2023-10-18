
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
	request.setAttribute("cmpName", "app-disqus_comments");
	request.setAttribute("titleKey", "components.app-disqus.title");
	request.setAttribute("descKey", "components.app-disqus.desc");
	request.setAttribute("iconLink", "/components/app-disqus/menuicon.png");
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
		return "!INCLUDE(/components/app-disqus/disqus.jsp, login="+ document.textForm.login.value+")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getDisqusCommnets());
		return true;
	}
	//]]>
</script>

<div class="tab-pane toggle_content" style="height:450px !important; overflow: auto; width:800px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">
<div class="row">

		<div class="form-group clearfix">
			<div class="col-xs-12">
				<iwcm:text key="components.app-disqus_info" />
			</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-xs-5">
				<label for="login"><iwcm:text key="components.app-disqus.siteName" />:</label>
			</div>
			<div class="col-xs-7">
				<input id="login" type="text" name="login" value="<%=pageParams.getValue("login", "")%>" size="40" />
			</div>
		</div>
		</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
