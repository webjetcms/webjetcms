
<%
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"
	import="sk.iway.iwcm.*,org.apache.commons.codec.binary.Base64,sk.iway.iwcm.i18n.*,java.util.*"%><%@
taglib
	prefix="iwcm" uri="/WEB-INF/iwcm.tld"%><%@
taglib prefix="iway"
	uri="/WEB-INF/iway.tld"%><%@
taglib prefix="display"
	uri="/WEB-INF/displaytag.tld"%><%@
taglib prefix="stripes"
	uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib
	prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<iwcm:checkLogon admin="true" perms="cmp_basket"/>
	<%@page import="sk.iway.iwcm.io.IwcmFile"%>
<%
	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);
	Prop prop = Prop.getInstance(lng);
	request.setAttribute("cmpName", "product_perex");
	request.setAttribute("titleKey",
			"components.product_perex.title");
	request.setAttribute("descKey",
			"components.product_perex.desc");
	//request.setAttribute("iconLink",
	//		"/components/cloud/app-social_icon/editoricon.png");
	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams)) {
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);
	String style = pageParams.getValue("style", "01");

	String bgColor = pageParams.getValue("bgColor", "#eee");
%>

<jsp:include page="/components/top.jsp" />
<script type="text/javascript" src="/admin/scripts/common.jsp"></script>

<script type="text/javascript">
	//<![CDATA[
	function getIncludedHtml() {
		var bgColor = $('input[name=bgColor]').val();
		var style = $('input[name=style]').val();
		return "!INCLUDE(/components/basket/product_perex.jsp, style="+style+", bgColor="+bgColor+" )!";
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getIncludedHtml());
		return true;
	}

	//]]>
</script>
<style type="text/css">

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

	select { width: 300px; }
	input { padding-left: 4px; }
</style>

<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">

<table border="0" cellspacing="0" cellpadding="5">

<table border="0">
			<tr>
				<td valign="top">
					<strong><iwcm:text key="components.product_perex.visualStyle"/>:</strong>

					<div id="styleSelectArea" style="height: 480px; width: 430px; overflow: auto;">

						<%
						int checkedInputPosition = 0;
						IwcmFile stylesDir = new IwcmFile(Tools.getRealPath("/components/basket/admin-styles-perex/"));
						if (stylesDir.exists() && stylesDir.canRead())
						{
							IwcmFile styleFiles[] = stylesDir.listFiles();
							styleFiles = FileTools.sortFilesByName(styleFiles);
							int counter = 0;
							for (IwcmFile file : styleFiles)
							{
								if (file.getName().endsWith(".png")==false) continue;
								if (file.getName().contains("bootstrap") && sk.iway.iwcm.common.CloudToolsForCore.isBootstrap(request)==false) continue;
								if (file.getName().contains("temp") && !sk.iway.iwcm.common.CloudToolsForCore.getRootTempName(request).contains(file.getName().substring(4, 6))){
									continue;
								}

								String styleValue = file.getName().substring(0, file.getName().lastIndexOf("."));
								if (styleValue.equals(style)) checkedInputPosition = counter;
								%>

									<div class="styleBox">
										<label class="image" for="style-<%=styleValue%>">
											<img src="<%=file.getVirtualPath() %>" alt="<%=styleValue%>" />
											<div class="radioSelect">
			  									<input type="radio" name="style" id="style-<%=styleValue%>" value="<%=styleValue%>" <%= styleValue.equals(style) ? " checked=\"checked\"" : "" %> onclick="changeTyp(this.value)"/>
			  									<% if ("iwcm.interway.sk".equals(request.getServerName())) out.print(styleValue); %>
			  								</div>
										</label>
									</div>
								<%
								counter++;
							}
						}
						%>
					</div>
				</td>

				<td valign="top" style="padding-left: 20px;">
					<table border="0" cellspacing="0" cellpadding="1">
<tr>
	<form name=textForm>
		<tr>
	<td colspan="3"><strong><iwcm:text key="components.product_perex.settings"/>:</strong></td>

	</tr>

	<tr>
	<td colspan="2"><iwcm:text key="components.product_perex.bg_color"/>:</td>
	<td><input type="text" name="bgColor" value="<%=bgColor%>" class="colorpicker-rgba" autocomplete="off"/>
	</td>
	</tr>

	</form>
</td>
</tr>

</table>
</tr>
</table></table></div>

<jsp:include page="/components/bottom.jsp" />
