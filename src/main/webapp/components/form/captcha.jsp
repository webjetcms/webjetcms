<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
		taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
		taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
		taglib prefix="bean" uri="/WEB-INF/struts-bean.tld" %><%@
		taglib prefix="html" uri="/WEB-INF/struts-html.tld" %><%@
		taglib prefix="logic" uri="/WEB-INF/struts-logic.tld" %><%@
		taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
		taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
		taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

	//vygeneruje pole pre zadanie textu z captcha obrazku

	String lng = PageLng.getUserLng(request);
	pageContext.setAttribute("lng", lng);

	PageParams pageParams = new PageParams(request);
	String buttonValue = pageParams.getValue("buttonValue", "Odoslať");
	String buttonClasses = pageParams.getValue("buttonClasses", "");

	if("reCaptcha".equals(Constants.getString("captchaType")))
	{
		%><jsp:include page="/components/form/re_captcha.jsp"></jsp:include><%
	}
	else if("reCaptchaV3".equals(Constants.getString("captchaType")))
	{
      %><jsp:include page="/components/form/re_captcha_v3.jsp"></jsp:include><%
   }
	else if("invisible".equals(Constants.getString("captchaType")))
	{
		%><jsp:include page="/components/form/invisible_captcha.jsp"></jsp:include><%
	}
	else if("none".equals(Constants.getString("captchaType")))
	{
		return;    //pre vypnutie captchy
	}
	else
	{%>
		<div class="form-group form-group-captcha">
			<label><%=sk.iway.iwcm.system.captcha.Captcha.getImage(request) %></label>
			<input type="text" id="wjcaptcha1" name="wjcaptcha" class="required captcha form-control" size="20" maxlength="255" value="" />
			<style type="text/css">
				div.form-group-captcha .cs-error { display: none !important; }
			</style>
		</div>
		<%
	}%>