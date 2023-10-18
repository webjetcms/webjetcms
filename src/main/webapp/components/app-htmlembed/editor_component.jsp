<%
	sk.iway.iwcm.Encoding
	.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,org.apache.commons.codec.binary.Base64,org.apache.struts.util.ResponseUtils"%><%@
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
	request.setAttribute("cmpName", "app-htmlembed");
	request.setAttribute("titleKey",
	"components.app-htmlembed.title");
	request.setAttribute("descKey",
	"components.app-htmlembed.desc");
	request.setAttribute("iconLink",
	"/components/app-htmlembed/editoricon.png");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String htmlCode = pageParams.getValue("html", "");
	if (Tools.isNotEmpty(htmlCode))
	{
		//paramPageParams = Tools.replace(paramPageParams, "|", "=");
		Base64 b64 = new Base64();
		htmlCode = new String(b64.decode(htmlCode.getBytes()));

		//out.println(htmlCode+"<br/>");
		if (htmlCode.startsWith("utf8:"))
		{
			htmlCode = Tools.URLDecode(htmlCode.substring(5));
		}
		//out.println(htmlCode+"<br/>");

	}
%>

<jsp:include page="/components/top.jsp" />
<%@include file="/components/_common/cleditor/jquery.cleditor.js.jsp"%>

<script type="text/javascript">
	//<![CDATA[
	function utf8_to_b64( str )
	{
  		return window.btoa("utf8:"+encodeURIComponent( str ));
	}

	function getAppHtmlEmbed()
	{
		var html_code = $("#inserted_code").val();
		var html = utf8_to_b64(html_code);
		return "!INCLUDE(/components/app-htmlembed/embed.jsp, html="
				+ html + ")!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getAppHtmlEmbed());
		return true;
	}

	resizeDialog(550, 550);

	//]]>
</script>

 <style type="text/css">
		ul.tab_menu { padding: 2px 0 0 10px; }
	td.main { padding: 0px; }

	.styleBox {display: block; position: relative; width: 382px; height: 160px; background: #fff; margin: 3px; padding: 10px; border: 1px solid #bcbcbc; border-radius: 4px;}
	* HTML BODY .styleBox {width: 402px; height: 180px;}

	.boxes .styleBox {height: 110px;}
	* HTML BODY .boxes .styleBox {height: 130px;}

	.styleBox .radioSelect { position: absolute; left: 0; top: 0; text-align: left; width: 100%; height: 100%;}
	.styleBox .radioSelect input {position: absolute; left: 10px; top: 80px; border: 0px none;}
	.boxes .styleBox .radioSelect input  {top: 55px;}
	.styleBox img  {position: absolute; top: 10px; left: 42px;}

	div.colBox {display: block; float: left; margin: 10px 10px 0 0; padding: 0;  width: 408px; overflow: auto;}

	div.clearer {width: 100%; clear: both; height: 0; line-height: 0; font-size: 0; display: block; visibility: hidden;}


</style>



<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
<form name="textForm" style="padding: 10px; margin: 0px;">

<label for="inserted_code_name"><iwcm:text
		key="components.app-htmlembed.editor_components.vloz_html_kod" />:</label>
</br>
<textarea name="inserted_code_name" class="required" id="inserted_code"
	rows="12" cols="72" style="width:100%"><%=ResponseUtils.filter(htmlCode)%></textarea>
	</table></form></div>

<jsp:include page="/components/bottom.jsp" />
