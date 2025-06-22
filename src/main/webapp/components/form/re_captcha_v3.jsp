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

out.print(Tools.insertJQuery(request));
%>

<input type="hidden" id="g-recaptcha-response" name="g-recaptcha-response">
<input type="hidden" name="action" value="validate_captcha">

<script src="https://www.google.com/recaptcha/api.js?render=<%=Constants.getString("reCaptchaSiteKey")%>"></script>
<script>
    function wjFormSubmit(form, callback) {
        grecaptcha.ready(function () {
            // do request for recaptcha token
            // response is promise with passed token
            grecaptcha.execute('<%=Constants.getString("reCaptchaSiteKey")%>', {action: 'validate_captcha'})
                .then(function (token) {
                    // add token value to form
                    document.getElementById('g-recaptcha-response').value = token;
                    //$('#g-recaptcha-response').closest('form').submit()
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
        $('input[type="submit"]').on('click', function () {
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
                wjFormSubmit(form);
            }

            return false
        })
    })
</script>