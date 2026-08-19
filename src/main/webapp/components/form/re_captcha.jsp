<%
//Aby reCaptcha fungovala spravne potrebujeme vygenerovat v google acounte (https://www.google.com/recaptcha/ad min) pre konkretnu domenu data-sitekey a secret.
//data-sitekey -> Constants.getString("reCaptchaSiteKey")
//secret -> Constants.getString("reCaptchaSecret")
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %><%@
taglib prefix="iway" uri="/WEB-INF/iway.tld" %><%@
taglib prefix="display" uri="/WEB-INF/displaytag.tld" %><%@
taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes.tld"%><%@
taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%

	if(Tools.isEmpty(Constants.getString("reCaptchaSiteKey")) || Tools.isEmpty(Constants.getString("reCaptchaSecret"))){
		return;
	}


	String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

String idPrefix = "";
String functionPrefix = "";
String multistepFormPrefix = (String)request.getAttribute("multistepFormPrefix");
if (Tools.isNotEmpty(multistepFormPrefix)) {
	idPrefix = multistepFormPrefix;
}
functionPrefix = Tools.replace(idPrefix, "-", "_");

PageParams pageParams = new PageParams(request);
String colorTheme = "light";
//request.setAttribute("reCaptchaColorTheme", "dark");
if("dark".equals(""+request.getAttribute("reCaptchaColorTheme")))
	colorTheme = "dark";
out.print(Tools.insertJQuery(request)); %>
<div class="g-recaptcha" id="<%=idPrefix%>wjReCaptcha"></div>
<script src="https://www.google.com/recaptcha/api.js?onload=<%=functionPrefix%>onloadCallback&render=explicit" async defer></script>
<script type="text/javascript">
	{
		var <%=functionPrefix%>reCaptchaWidgetId = -1;
		<%//ReCaptcha je validna iba pri prvom zavolani, opakovanom zavolani je nevalidna, hoci vidget pise ze je validna%>
		function isReCaptchaValid()
		{
			return <%=functionPrefix%>serverRequest(false);
		}

		function <%=functionPrefix%>serverRequest(setId)
		{
			var isValid = false;
			var selector = '#g-recaptcha-response';
			if (<%=functionPrefix%>reCaptchaWidgetId > 0) selector = '#g-recaptcha-response-' + <%=functionPrefix%>reCaptchaWidgetId;
			var captchaId = $(selector).val();
			var url = '/components/form/re_catpcha_ajax.jsp';
			if(setId)
				url = '/components/form/set_re_catpcha_ajax.jsp';

			$.ajax({
				type: 'POST',
				url: url,
				data: { capchaId: captchaId },
				success: function( data )
				{
					if(data.trim() == 'OK')
					{
						isValid = true;
						//alert('captcha is valid');
					}
					else
						grecaptcha.reset(<%=functionPrefix%>reCaptchaWidgetId);
				},
				async:false
			});
			return isValid;
		}

		var <%=functionPrefix%>verifyCallback = function(response) {
			<%=functionPrefix%>serverRequest(true);
		};

		var <%=functionPrefix%>onloadCallback = function()
		{
			<%//tu vygenerujeme do divu captchu%>
			<%=functionPrefix%>reCaptchaWidgetId = grecaptcha.render('<%=idPrefix%>wjReCaptcha', {'sitekey' : '<%=Constants.getString("reCaptchaSiteKey")%>' , 'callback' : <%=functionPrefix%>verifyCallback , 'theme' : '<%=colorTheme%>'});//captchaLoaded
		};
	}
</script>