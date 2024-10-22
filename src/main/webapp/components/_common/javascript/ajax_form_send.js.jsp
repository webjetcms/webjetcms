<%@ page pageEncoding="utf-8" contentType="text/javascript" %><%@ page import="sk.iway.iwcm.Constants" %><%sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");%>

var defaultStripesExenstion = ".x";
var ajaxStripesExenstion = ".ajax";

/*
* Function that uses jQuery to invoke an action of a form. Slurps the values
* from the form using prototype's 'form.serialize()' method, and then submits
* them to the server using prototype's 'jQuery.post' which transmits the request
* and then renders the resposne text into the named container.
*
* @param formDiv reference to the form object being submitted
* @param resultDivthe name of the HTML container to insert the result into
* @param event the name of the event to be triggered, or null
*/

function invokeWJAjax(formDiv, resultDiv, event, action, callMyFunction)
{
  try
  {
    var form = null;

    $("form[name="+formDiv+"]").addClass("sending");

    //"formMailForm"==formDiv
    if (formDiv.indexOf("formMailForm") > -1)
    {

      form = $("form[name="+formDiv+"]");
      form.attr("id", formDiv);
    }
    var formByIds = null;
    if (form == null) formByIds = $("#" + formDiv + " form");
    if (form == null && formByIds != null && formByIds.length!=0)
    {
      if(formByIds.length > 1)
      {
        form = $(formByIds[1]);  //zoberiem druhy formular - prvy je webjetObfuscateForm
      }
      else
      {
        form = $(formByIds[0]);
      }
    }

    try
    {
      if ("multipart/form-data" == form.attr("enctype"))
      {
        return true;
      }
    }
    catch (ex1) {alert("1:" + ex1)}

        $("#formResult").remove();// vymaze form result
        $("input, textarea, select").removeClass("invalid");// vymaze vsetky classy invalid

    if ($("#" + resultDiv).length==0)
    {
      form.prepend("<div id=\"formResult\"><div id='"+resultDiv+"'></div></div>");
    }

    try
    {
      //checkform test
      if (checkForm)
      {
        var check = checkForm.recheckAjax(document.getElementById(formDiv));
        if (check==false)
        {

            try
            {
               if (typeof callMyFunction != 'undefined') callMyFunction(check);
            }
            catch (e) { }

          //prevent default
          return false;
        }
      }
    }
    catch (ex2) { window.alert("7" + ex2); console.log(ex2); }


    var extIndex = action.indexOf(defaultStripesExenstion);
    //compose ajax url from original form action replacing original stripes script extension
    //with stripes ajax extension configured in web.xml
    var url = action;

        //"formMailForm"==formDiv
    if (formDiv.indexOf("formMailForm") > -1)
    {
      url = form.attr("action").replace(/\/formmail\.do/gi, "/FormMailAjax.action");
    }

    //create params from serializing form inputs and add param simulating button click in order to
    //Stripes could find actionBean event handler

    var formSubmitCallback = function() {
      var params = form.serialize();
      if(event != null) params = event + '&' + params;
      //hide errors if visible
      $("#" + resultDiv).hide();
      //fire ajax request

      var callback = function(dataFromServer) {
            wjAjaxOnFormResult(formDiv, resultDiv, dataFromServer, callMyFunction);
          };
      //set up what to do if AJAX request fails
      $("").ajaxError(wjAjaxOnServerError);

      <% if ("utf-8".equals(sk.iway.iwcm.SetCharacterEncodingFilter.getEncoding())==false) { %>
      //aby filter vedel, ze ideme cez AJAX a sme UTF-8
      if (url.indexOf("?")==-1) url = url + "?ajax_utf-8=1";
      else url = url + "&ajax_utf-8=1";
      <% } %>
      $.post(url, params, callback, "text");
      //return false to button to stop standard submit of the form
    }
    //pre re-captcha v3 potrebujeme najskor volat validate
    if (typeof window.wjFormSubmit == "function") {
      window.wjFormSubmit(form, formSubmitCallback);
    } else {
      formSubmitCallback();
    }

    return false;
  }
  catch(ex)
  {
    window.alert("Error 2: " + ex);
    return true;
  }
}

/*
* Method is executed when server returns exception.
*/
function wjAjaxOnServerError(event, request, settings, error){
  alert("Server returned an error, form was not probably processed by the server.");
}

/*
* Method is called when server returns non JSON data.
*/
function wjAjaxOnServerDataError(data){
  alert("Server has returned data in unknown format.");
}

/*
* Method is called when server returns reply to ajax call.
*
* @param data contains reply from the server
*/
function wjAjaxOnFormResult(formDiv, resultDiv, data, callMyFunction) {
  try
  {
    var result = eval(data);
  } catch (ex) {
    wjAjaxOnServerDataError(data);
    return;
  }

    try
    {
       if (typeof callMyFunction != 'undefined') callMyFunction(result);
    }
    catch (e) { }

  if (result.ok)
  {
    wjAjaxOnSuccess(formDiv, resultDiv, result, callMyFunction);
  } else {
    wjAjaxOnError(formDiv, resultDiv, result);
  }
  try {
    const wjEvent = new CustomEvent("WJ.formSubmit", {
        detail: {
          success: result.ok,
          result: result,
          formDiv: formDiv,
          resultDiv: resultDiv
        }
    });
    //console.log("Dispatching event: ", wjEvent);
    window.dispatchEvent(wjEvent);
  } catch (e) { console.log(e); }
}

/*
* Method is executed, when form was submited with success.
*/
function wjAjaxOnSuccess(formDiv, resultDiv, result)
{
  $("#" + formDiv).queue(function()
  {
    //console.log(formDiv+"/"+resultDiv+"/"+result);
    $("#" + resultDiv).parent().addClass("alert alert-ajax alert-success");
    $("#" + resultDiv).parent().removeClass("alert-danger");
	  formAfterSend();
    $("#" + resultDiv).html("<div class=\"overlay\"></div><div class=\"form-dialog\">"+result.okResult).fadeIn();
    <% if (Constants.getBoolean("formmailScrollTopAfterSend")) { %> $("html,body").animate({ scrollTop: ($("#" + resultDiv).offset().top - 100) }, { duration: 'fast', easing: 'linear'}); <% } %>
    $(this).dequeue();

    <% if (Constants.getBoolean("formmailResetFormAfterSend")) { %> window.setTimeout(wjAjaxResetForm.bind(null, formDiv), 500); <% } %>
    window.setTimeout(function(){
          var div = $("#" + resultDiv),
              formResult = div.closest('#formResult');

          if (formResult.length > 0) {
              formResult.fadeOut("slow");
          }
          else {
              div.fadeOut("slow");
          }
    }, 4000);

    wjAjaxSetToken(formDiv, result.token);
  });
}

function formAfterSend() {
}

function wjAjaxResetForm(formDiv)
{
  try
  {
    var form = $('form[name="' + formDiv + '"]')
    if (formDiv.length > 0)
    {

      form.find('input[type=text], input[type=email], input[type=tel], input[type=date], input[type=checkbox], input[type=radio], textarea').each(function(){
        var el = $(this);

        $("form[name="+formDiv+"]").removeClass("sending");

        if (el.is(':checkbox') || el.is(':radio')) {
          el.prop('checked', el[0].defaultValue).trigger('change');
        }
        else {
          el.val('').trigger('change');
        }

        if (el.hasClass('selectmenu')) {
            setTimeout(function(){
                el.val('').selectmenu('refresh');
            });
        }
      });

      form.find('select').each(function() {
        this.selectedIndex = 0;
      });

      form.find(".dropzone").each(function() {
        Dropzone.forElement(this).removeAllFiles();
      });
    }
  } catch (e) {
    console.log(e);
  }
}

function wjAjaxSetToken(formDiv, token)
{
  try
  {
    if (token != undefined && token != null && token!="")
    {
      $("form[name="+formDiv+"]").find("input[name=__token]").val(token);
    }
  } catch (e) {}
}

/*
* Method is executed, when form was submitted and server returned error.
*/
function wjAjaxOnError(formDiv, resultDiv, result) {
  $("#" + resultDiv).html(result.errorResult).show("slow");
  $("#" + formDiv).queue(function ()
  {
    var callback = function()
    {
      wjAjaxColorizeLabel(this, result);
    };
    $("#" + formDiv + " label").each(callback);
    $("#wjcaptchaImg1").click();
    $("#wjcaptcha1").addClass("invalid");
    $("#" + resultDiv).parent().addClass("alert alert-ajax alert-danger");
    $("html,body").animate({ scrollTop: ($("#" + resultDiv).offset().top - 100) }, { duration: 'fast', easing: 'linear'});
    $("form").removeClass("sending");
    $(this).dequeue();

    wjAjaxSetToken(formDiv, result.token);
  });
}

/*
* Method will add class "error" to each label, whose input was found in validationErrors object.
* In the oposite way, it removes any possible "error" class from any labes, that currently is not in error.
*/
function wjAjaxColorizeLabel(label, result) {
  var error = result.validationErrors[label.htmlFor];
  var errorIndex = -1;
  if (label.className != null) errorIndex = label.className.indexOf('error');
  if (error != null && errorIndex == -1) {
    label.className = label.className + " error";
  }
  if (error == null && errorIndex > -1) {
    label.className = label.className.substring(0, errorIndex)
        + label.className.substring(errorIndex + 5, label.className.length);
  }
}