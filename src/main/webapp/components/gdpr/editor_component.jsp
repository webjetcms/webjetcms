
<% // aplikacia na vlozenie nastavenia GDPR cookies v stranke
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
	request.setAttribute("cmpName", "gdpr");

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
	function getCookieBar() {

		var showLink = false; if ($('input[name=showLink]').is(':checked')) { showLink = true; }

		var ret =  "!INCLUDE(/components/gdpr/cookie_bar.jsp, showLink="+showLink+")!";

		return ret;
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getCookieBar());
		return true;
	}
	//]]>
</script>
<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
	<form name="textForm" style="padding: 10px; margin: 0px;">
		<div class="form-group clearfix">
			<div class="col-sm-6 text-right" style="margin-top:0px;">
				<iwcm:text key="components.gdpr.cookies.showLink" /></label>
			</div>
			<div class="col-sm-6">
				<input type="checkbox" name="showLink" <%if (pageParams.getBooleanValue("showLink", false)) out.print("checked='checked'");%>/>
			</div>
		</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
