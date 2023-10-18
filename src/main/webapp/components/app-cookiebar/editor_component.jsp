
<% // aplikacia na vlozenie sluzby disqus do webstránky
	sk.iway.iwcm.Encoding
			.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.Prop,org.apache.struts.util.*"%><%@
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
	Prop prop = Prop.getInstance(lng);

	request.setAttribute("cmpName", "app-cookiebar_comments");
	request.setAttribute("titleKey", "components.app-cookiebar.title");
	request.setAttribute("descKey", "components.app-cookiebar.desc");
	request.setAttribute("iconLink", "/components/app-cookiebar/menuicon.png");

	String paramPageParams = Tools.getRequestParameterUnsafe(request, "pageParams");
	if (Tools.isNotEmpty(paramPageParams))
	{
		request.setAttribute("includePageParams", paramPageParams);
	}
	PageParams pageParams = new PageParams(request);

	String cookie_title = pageParams.getValue("cookie_title", "");
	String cookie_text = pageParams.getValue("cookie_text", "");
	String cookie_ButtonText = pageParams.getValue("cookie_ButtonText", "");
	String cookie_ButtonTextDecline = pageParams.getValue("cookie_ButtonTextDecline", "");
	String color_text = pageParams.getValue("color_text", "");
	String color_button = pageParams.getValue("color_button", "");
	String color_background = pageParams.getValue("color_background", "");
	String color_buttonText = pageParams.getValue("color_buttonText", "");
	String color_title = pageParams.getValue("color_title","");

	String position = pageParams.getValue("position","bottom");

	boolean checkbox_title = pageParams.getBooleanValue("checkbox_title",true);

	int padding_top = pageParams.getIntValue("padding_top",25);
	int padding_bottom = pageParams.getIntValue("padding_bottom",25);

%>

<jsp:include page="/components/top.jsp" />

<script type="text/javascript">
	//<![CDATA[
	function getCookieBar() {

		var cookie_title = $("input[name=cookie_title]").val();
		var cookie_ButtonText = $("input[name=cookie_ButtonText]").val();
		var cookie_ButtonTextDecline = $("input[name=cookie_ButtonTextDecline]").val();
		var color_text = $("input[name=color_text]").val();
		var color_button = $("input[name=color_button]").val();
		var color_background = $("input[name=color_background]").val();
		var color_buttonText = $("input[name=color_buttonText]").val();
		var color_title = $("input[name=color_title]").val();

		var position = $( "#position" ).val();

		var cookie_text = $("#cookie_text").val();

		var checkbox_title = false; if ($('input[name=checkbox_title]').is(':checked')) { checkbox_title = true; }

		var padding_bottom = $("input[name=padding_bottom]").val();
		var padding_top = $("input[name=padding_top]").val();

		var showLink = false; if ($('input[name=showLink]').is(':checked')) { showLink = true; }

		var ret =  "!INCLUDE(/components/app-cookiebar/cookiebar.jsp"
				+ addIncludeParameter("checkbox_title", checkbox_title)
				+ addIncludeParameter("cookie_title", cookie_title)
				+ addIncludeParameter("color_title", color_title)
				+ addIncludeParameter("cookie_text", cookie_text)
				+ addIncludeParameter("cookie_ButtonText", cookie_ButtonText)
				+ addIncludeParameter("cookie_ButtonTextDecline", cookie_ButtonTextDecline)
				+ addIncludeParameter("color_text", color_text)
				+ addIncludeParameter("color_button", color_button)
				+ addIncludeParameter("color_background", color_background)
				+ addIncludeParameter("color_buttonText", color_buttonText)
				+ addIncludeParameter("padding_top", padding_top)
				+ addIncludeParameter("padding_bottom", padding_bottom)
				+ addIncludeParameter("position", position)
				+ addIncludeParameter("showLink", showLink)
			+ ")!";

		return ret;
	}

	function Ok() {
		oEditor.FCK.InsertHtml(getCookieBar());
		return true;
	}
	function showHideTitleSection(){
	    if($("input[name=checkbox_title]").is(':checked')){
	    	$('.titleSection').fadeIn();
	    } else {
	    	$('.titleSection').fadeOut();
	    }
	}
	$(document).ready(function(){
		$("#position").val("<%=position%>");
	});

	//]]>
</script>
<style>
	.colorpicker-rgba.poleDlhe {
	    padding-left: 30px;
	}
	span.minicolors-swatch {
		margin-top: 1px;
	}
	.text-right {
		margin-top: 8px;
	}
</style>
<div class="tab-pane toggle_content" style="height:440px !important; overflow: auto; width:990px; padding:10px">
	<form name="textForm" style="padding: 10px; margin: 0px;">
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right" style="margin-top:0px;">
				<iwcm:text key="components.app-cookiebar.cookiebar_title" />
			</div>
			<div class="col-sm-8">
				<input type="checkbox" name="checkbox_title" <%if (checkbox_title) out.print("checked='checked'");%> onclick="showHideTitleSection()">
			</div>
		</div>
		<div class="form-group clearfix titleSection" <% if(!checkbox_title) out.print("style='display:none'"); %>>
			<div class="col-sm-4 text-right">
				<iwcm:text key="components.app-cookiebar.titleText" />
			</div>
			<div class="col-sm-8">
				<input type="text" name="cookie_title" id="cookie_title" value="<%=ResponseUtils.filter(cookie_title)%>" placeholder="<%=ResponseUtils.filter(prop.getText("components.app-cookiebar.cookie_title"))%>" style="width:100%;">
			</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><iwcm:text key="components.app-cookiebar.text" /></div>
			<div class="col-sm-8">
				<textarea placeholder="<%=ResponseUtils.filter(prop.getText("components.app-cookiebar.cookie_text")) %>"  rows="5" cols="60" name="cookie_text" id="cookie_text" style="min-height: 30px;
				line-height: 1.42857;
			    transition: border-color 0.15s ease-in-out 0s, box-shadow 0.15s ease-in-out 0s;
			    max-width: 455px;
			    border: 1px solid #e5e5e5;
			    box-shadow: none;
			    color: #333333;
			    font-weight: normal;
			    padding: 6px 12px;"><%=cookie_text%></textarea>
			</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><iwcm:text key="components.app-cookiebar.buttonTextAccept" /></div>
			<div class="col-sm-8"><input type="text" name="cookie_ButtonText" id="cookie_ButtonText" size="50" value="<%=ResponseUtils.filter(cookie_ButtonText)%>" placeholder="<%=ResponseUtils.filter(prop.getText("components.app-cookiebar.cookie_ButtonText")) %>" style="width:100%;"></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><iwcm:text key="components.app-cookiebar.buttonTextDecline" /></div>
			<div class="col-sm-8"><input type="text" name="cookie_ButtonTextDecline" id="cookie_ButtonText" size="50" value="<%=ResponseUtils.filter(cookie_ButtonTextDecline)%>" placeholder="<%=ResponseUtils.filter(prop.getText("components.app-cookiebar.cookie_ButtonTextDecline")) %>" style="width:100%;"></div>
		</div>

		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><iwcm:text key="components.app-cookiebar.showLink" /></div>
			<div class="col-sm-8"><input type="checkbox" name="showLink" <%if (pageParams.getBooleanValue("showLink", false)) out.print("checked='checked'");%>/></div>
		</div>

		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><label for="color_background"><iwcm:text key="components.app-cookiebar.background" /></label></div>
			<div class="col-sm-8"><input type="text" id="color_background" class="colorpicker-rgba poleDlhe" name="color_background" value="<%=color_background%>"/></div>
		</div>
		<div class="form-group clearfix titleSection" <% if(!checkbox_title) out.print("style='display:none'"); %>>
			<div class="col-sm-4 text-right">
				<label for="color_title"><iwcm:text key="components.app-cookiebar.titleColor" /></label>
			</div>
			<div class="col-sm-8">
				<input type="text" id="color_title" class="colorpicker-rgba poleDlhe" name="color_title" value="<%=color_title%>" >
			</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><label for="color_title"><iwcm:text key="components.app-cookiebar.textColor" /></label></label></div>
			<div class="col-sm-8"><input type="text" id="color_text" class="colorpicker-rgba poleDlhe" name="color_text" value="<%=color_text%>"></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><label for="color_button"><iwcm:text key="components.app-cookiebar.buttonColor" /></label></label></div>
			<div class="col-sm-8"><input type="text" id="color_button" class="colorpicker-rgba poleDlhe" name="color_button" value="<%=color_button%>"></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><label for="color_buttonText"><iwcm:text key="components.app-cookiebar.buttonTextColor" /></label></div>
			<div class="col-sm-8"><input type="text" id="color_buttonText" class="colorpicker-rgba poleDlhe" name="color_buttonText" value="<%=color_buttonText%>"></div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><label for="color_background"><iwcm:text key="components.app-cookiebar.position" /></label></div>
			<div class="col-sm-8">
				<select name="position" class="poleDlhe" id="position">
						<option value="top"><iwcm:text key="components.app-cookiebar.position.top" /></option>
						<option value="bottom" selected="selected"><iwcm:text key="components.app-cookiebar.position.bottom" /></option>
				</select>
			</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><label for="color_background"><iwcm:text key="components.app-cookiebar.padding-top" /></label></div>
			<div class="col-sm-8"><input type="number" id="padding_top" class="poleKratke" name="padding_top" value="<%=padding_top%>" style="width: 65px;"/> px</div>
		</div>
		<div class="form-group clearfix">
			<div class="col-sm-4 text-right"><label for="color_background"><iwcm:text key="components.app-cookiebar.padding-bottom" /></label></div>
			<div class="col-sm-8"><input type="number" id="padding_bottom" class="poleKratke" name="padding_bottom" value="<%=padding_bottom%>" style="width: 65px;"/> px</div>
		</div>
	</form>
</div>

<jsp:include page="/components/bottom.jsp" />
