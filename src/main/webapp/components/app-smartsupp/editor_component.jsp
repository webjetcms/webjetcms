<%

// [#18339 - app livechat] Aplikacia využíva službu www.smartsupp.com. Použivateľ sa musí najskôr zaregistrovať a vložit kód do aplikácie. Po vložení kódu sa na stránkach začne zobrazovať aplikacia smartsupp.

sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="java.io.*, sk.iway.iwcm.*" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%
request.setAttribute("cmpName", "app-smartsupp");
request.setAttribute("iconLink", "/components/app-smartsupp/editoricon.png");
%>

<%@page import="org.apache.struts.util.ResponseUtils"%>
<jsp:include page="/components/top.jsp"/>

<%
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");

	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);

	out.println(Tools.insertJQuery(request));


%>
<script type='text/javascript'>

function Ok()
{
	var form = document.textForm;
	var kluc = form.inputKluc.value;

	var parametre = "";

	parametre += ", kluc=\""+kluc+"\"";

	parametre += ", cacheMinutes=60";

	oEditor.FCK.InsertHtml("!INCLUDE(/components/app-smartsupp/chat.jsp"+parametre+")!");
	return true;
} // End function
</script>

<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:790px; padding:10px">
	<form name="textForm" style="padding: 10px; margin: 0px;">
	  	 <div class="form-group clearfix">
			<div><iwcm:text key="components.app-smartsupp.kluc"/>:</div>
				</div>
				 <div class="form-group clearfix">
			<div><input style="width:600px" type="text" name="inputKluc" value="" ></div>
		</div>

  </form>
</div>


<jsp:include page="/components/bottom.jsp"/>
