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

PageParams pageParams = new PageParams(request);
String colorTheme = "light";
//request.setAttribute("reCaptchaColorTheme", "dark");
if("dark".equals(""+request.getAttribute("reCaptchaColorTheme")))
	colorTheme = "dark";
out.print(Tools.insertJQuery(request)); %>
<div class="g-recaptcha" id="wjReCaptcha"></div>
<script src="https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit" async defer></script>
<script type="text/javascript">
	var reCaptchaWidgetId = -1;
	<%//ReCaptcha je validna iba pri prvom zavolani, opakovanom zavolani je nevalidna, hoci vidget pise ze je validna%>
	function isReCaptchaValid()
	{
		return serverRequest(false);
	}
	
	function serverRequest(setId)
	{
		var isValid = false;
		var captchaId = $('#g-recaptcha-response').val();
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
					grecaptcha.reset(reCaptchaWidgetId);
			},
			async:false
		});
		return isValid;
	}

	var verifyCallback = function(response) {
		serverRequest(true);
	};
	
	var onloadCallback = function() 
	{
		<%//tu vygenerujeme do divu captchu%>
		reCaptchaWidgetId = grecaptcha.render('wjReCaptcha', {'sitekey' : '<%=Constants.getString("reCaptchaSiteKey")%>' , 'callback' : verifyCallback , 'theme' : '<%=colorTheme%>'});//captchaLoaded
	};
</script>