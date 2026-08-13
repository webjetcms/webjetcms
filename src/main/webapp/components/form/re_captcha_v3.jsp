<%
    //Aby reCaptcha fungovala spravne potrebujeme vygenerovat v google acounte (https://www.google.com/recaptcha/ad min) pre konkretnu domenu data-sitekey a secret.
//data-sitekey -> Constants.getString("reCaptchaSiteKey")
//secret -> Constants.getString("reCaptchaSecret")
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.Constants" %>
<%@ page import="sk.iway.iwcm.PageLng" %>
<%@ page import="sk.iway.iwcm.PageParams" %>
<%@ page import="sk.iway.iwcm.Tools" %>
<%@
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

String idPrefix = "";
String multistepFormPrefix = (String) request.getAttribute("multistepFormPrefix");
if (Tools.isNotEmpty(multistepFormPrefix)) {
    idPrefix = multistepFormPrefix;
}

int recaptchaV3Counter = Tools.getIntValue((String) request.getAttribute("recaptchaV3Counter"), 1);
String recaptchaInputId = idPrefix + "g-recaptcha-response";
if (Tools.isEmpty(idPrefix) && recaptchaV3Counter > 1) {
    recaptchaInputId += "-" + recaptchaV3Counter;
}
request.setAttribute("recaptchaV3Counter", String.valueOf(recaptchaV3Counter + 1));

out.print(Tools.insertJQuery(request));
%>

<input type="hidden" id="<%=recaptchaInputId%>" name="g-recaptcha-response" data-type="V3">
<input type="hidden" name="action" value="validate_captcha">

<script src="https://www.google.com/recaptcha/api.js?render=<%=Constants.getString("reCaptchaSiteKey")%>"></script>
<script>
    function wjFormSubmit(form, callback, recaptchaInput) {
        if (!recaptchaInput) {
            var formElement = form && form.jquery ? form[0] : form;
            if (formElement) {
                recaptchaInput = formElement.querySelector('input[name="g-recaptcha-response"][data-type="V3"]');
            }
        }

        grecaptcha.ready(function () {
            // do request for recaptcha token
            // response is promise with passed token
            grecaptcha.execute('<%=Constants.getString("reCaptchaSiteKey")%>', {action: 'validate_captcha'})
                .then(function (token) {
                    // Add token to the reCAPTCHA input belonging to the submitted form.
                    if (recaptchaInput) {
                        recaptchaInput.value = token;
                    }
                    if (typeof callback === 'function') {
                        callback();
                    }
                    else {
                        form.submit();
                    }
                });
        })
    }

    $(function () {
        var recaptchaInput = document.getElementById('<%=recaptchaInputId%>');
        if (!recaptchaInput) return;

        $(recaptchaInput).closest('form').find('input[type="submit"]').on('click', function () {
            var el = $(this),
                form = el.closest('form');

            if (form.attr('action').indexOf("/formmail.do") === -1) {
                <% // negenerujeme pre custom formulare %>
                return true;
            }

            if (el.attr('onclick') && el.attr('onclick').indexOf('invokeWJAjax') !== -1) {
                <% // negenerujeme pre ajaxove volanie, to si vola formSubmit same (v webapp/components/_common/javascript/ajax_form_send.js.jsp)  %>
                return false;
            }

            var isValid = checkForm.checkImpl(form[0], true, null) !== false;
            if (isValid) {
                wjFormSubmit(form, null, recaptchaInput);
            }

            return false
        })
    })
</script>
