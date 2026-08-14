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
  String buttonValue = pageParams.getValue("buttonValue", "Odoslať");
  String buttonClasses = pageParams.getValue("buttonClasses", "");

  out.print(Tools.insertJQuery(request));

  //custom id
  int invisibleCaptchaCounter = Tools.getIntValue((String) request.getAttribute("invisibleCaptchaCounter"), 1);
  String idPrefix = "";
  String multistepFormPrefix = (String) request.getAttribute("multistepFormPrefix");
  if (Tools.isNotEmpty(multistepFormPrefix)) {
    idPrefix = multistepFormPrefix;
  }
  String invisibleCaptchaId = idPrefix + "submitWithCaptcha" + invisibleCaptchaCounter;

  request.setAttribute("invisibleCaptchaCounter", "" + invisibleCaptchaCounter);
  request.setAttribute("loadAfterFocus" , Constants.getBoolean("captchaLoadAfterFocus"));
%>
<c:if test="${invisibleCaptchaCounter eq 1}">
  <script>
    var onloadCallback = function() {
          var els = $('.submitWithCaptcha');
          els.each(function () {
              var el = $(this);

              if (typeof el.data("recaptcha-loaded") == 'undefined') {
                  renderCaptcha(el);
              }
          });
      },
      verifyCallback = function(id, response) {
          var el = $(document.getElementById(id)),
              form  = el.closest('form').length != 0 ? el.closest('form') : el.closest('.md-online-form'),
              submit = form.find('input:submit, button:submit').first();

          /*
          console.log(el);
          console.log(id);
          console.log(form);
          console.log(form.hasClass('md-online-form'));
          */

          if (form.hasClass('md-online-form')) {
              generateSmsCode();
          }
          else if (form.hasClass('multistep-form')) {
              form[0].dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
          }
          else if (submit.data('onclick') != null) {
              eval(submit.data('onclick'));
          }
          else {
              submit.closest('form').submit();
          }

          if (!form.hasClass("form-preview")) {
              console.log('reset');
              var formId = form.data('recaptcha-id');
              grecaptcha.reset(formId);
          }
      },
        renderCaptcha = function(el) {
            var form = el.closest('form'),
            id = el.prop('id');

            el.data('recaptcha-loaded', true);
            var render = renderGrecaptcha(el[0], id);
            form.data('recaptcha-id', render);
            if (form.data('recaptcha-execute-pending') === true) {
              form.removeData('recaptcha-execute-pending');
              grecaptcha.execute(render);
            }
        }

        function renderGrecaptcha(element, id) {
          return grecaptcha.render(element, {
            'sitekey': '<%=Constants.getString("reCaptchaSiteKey")%>', 'badge': 'inline', 'callback': function (response) {
              verifyCallback(id, response);
            }
          }, true)
        }

      $(function(){

          <c:choose>
            <c:when test="${loadAfterFocus}">
              $('form').find('input, select, textarea, button').on('focus', includeScript);
            </c:when>
            <c:otherwise>
              includeScript();
            </c:otherwise>
          </c:choose>

          function includeScript() {

              var el = $(this),
                isFormElement = el.is("input, select, textarea, button");

              if (isFormElement && el.closest("form").find('.submitWithCaptcha').length == 0) {
                  return;
              }

              var id = 'invisible-captcha-script';
              if ($('#' + id).length != 0) {
                  if (window.grecaptcha && typeof window.grecaptcha.render === 'function') {
                      onloadCallback();
                  }
                  return;
              }

              var script = '<script id="' + id + '" src="https://www.google.com/recaptcha/api.js?onload=onloadCallback&render=explicit&hl=<%=lng%>" async defer></' + 'script>';
              $('body').append(script);
          }

          var forms = $('.submitWithCaptcha').closest('form');
          forms.each(function () {
              var form = $(this);
              if (form.data('invisible-captcha-handler-bound') === true) return;
              form.data('invisible-captcha-handler-bound', true);

              form.find('input:submit, button:submit').each(function(){
                  var el = $(this);

                  if (el.attr("onclick") != null) {
                      el.data('onclick', el.attr("onclick").replace('return ', ''));
                      el.removeAttr("onclick");
                  }
              });

              form.find('input:submit, button:submit').on('click.invisibleCaptcha', function () {
                  var isValid = checkForm.checkImpl(form[0], true, null) == false ? false : true;

                  if (isValid) {
                      var recaptchaId = form.data('recaptcha-id');
                      if (window.grecaptcha && typeof window.grecaptcha.execute === 'function' && recaptchaId != null) {
                          grecaptcha.execute(recaptchaId);
                      } else {
                          form.data('recaptcha-execute-pending', true);
                          includeScript.call(this);
                      }
                  }

                  return false;
              });
          });
      });
  </script>
</c:if>

<div>
  <input id="<%=invisibleCaptchaId%>" class="submitWithCaptcha hidden" style="display: none;" name="Submit" type="button" />
</div>
<% request.setAttribute("invisibleCaptchaCounter", "" + (invisibleCaptchaCounter + 1)); %>
