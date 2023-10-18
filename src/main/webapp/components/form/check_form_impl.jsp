<%@page import="java.util.List"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/javascript");
%><%@ page pageEncoding="utf-8" contentType="text/javascript" import="sk.iway.iwcm.Constants, sk.iway.iwcm.PageLng, sk.iway.iwcm.Tools" %><%@ page import="sk.iway.iwcm.form.FormDB"%><%@ page import="java.util.ArrayList"%>
    <%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
    <%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
    <%
    String lng = PageLng.getUserLng(request);

    if (Constants.getBoolean("nginxProxyMode"))
    {
        String lngParam = Tools.getRequestParameter(request, "lng");
        if (Tools.isNotEmpty(lngParam) && lngParam.length()==2)
        {
            lng = lngParam;
            sk.iway.iwcm.PathFilter.setStaticContentHeaders("/cache/check_form_impl.js", null, request, response);
        }
    }

    pageContext.setAttribute("lng", lng);
    boolean checkFormValidateOnInit = Constants.getBoolean("checkFormValidateOnInit");
    %>

//event_attacher.js
    function addEventCheckForm(obj, evType, fn)
    {
        if (navigator.userAgent.indexOf("Opera")!=-1)
        {
            //opera je tupa, cez addEventListener to nefunguje
            eval("obj.on"+evType+"=fn");
            return;
        }

        if (obj.addEventListener)
        {
            obj.addEventListener(evType, fn, true);
            return true;
        }
        else if (obj.attachEvent)
        {
            var r = obj.attachEvent("on"+evType, fn);
            return r;
        }
        else
        {
            return false;
        }
    }

//fix for Internet Explorer event model
    function fixECheckForm(e) {
        if (!e && window.event) e = window.event;
        if (!e.target) e.target = e.srcElement;
        return e;
    }

//class magic
    var classMagic = {};

//prida objektu novy class
    classMagic.add = function (elm, newCl) {
        if (elm && newCl) {
            elm = this.fixElm(elm);
            for (var i = 0; i < elm.length; i++) {
                // ak uz dany objekt ma dany class, nepridame ho
                if (!classMagic.has(elm[i], newCl))
                {
                    elm[i].className += (this.get(elm[i])) ? " " + newCl : newCl;
                }
                try
                {
                    if (("required"==newCl || "invalid"==newCl) && (elm[i].type=="radio" || elm[i].type=="checkbox"))
                    {
                        var newcl2 = newCl+elm[i].type;
                        if (!classMagic.has(elm[i], newcl2))
                        {
                            elm[i].className += (this.get(elm[i])) ? " " + newcl2 : newcl2;
                        }
                    }
                }
                catch (e) { }
            }
            return true;
        }
        return false;
    };

//zisti, ci dany objekt ma priradeny dany class
    classMagic.has = function (elm, cl) {
        if (elm && cl) {
            if (actCl = this.get(elm)) {
                for (var i = 0; i < actCl.length; i++) {
                    if (actCl[i] == cl) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

//nahradi class(-y) objektu novym classom
    classMagic.set = function (elm, newCl) {
        if (elm && newCl) {
            elm = this.fixElm(elm);
            for (var i = 0; i < elm.length; i++) {
                elm[i].className = newCl;
            }
            return true;
        }
        return false;
    };

//nahradi stary class objektu novym classom
    classMagic.replace = function (elm, newCl, oldCl) {
        if (elm && newCl && oldCl) {
            elm = this.fixElm(elm);
            for (var i = 0; i < elm.length; i++) {
                var cl;
                var replCl = "";
                if (cl = this.get(elm[i])) {
                    for (var j = 0; j < cl.length; j++) {
                        replCl += (j > 0) ? " " : "";
                        replCl += (cl[j] == oldCl) ? newCl : cl[j];
                    }
                    elm[i].className = replCl;
                }
            }
            return true;
        }
        return false;
    };

//odstrani stary class
    classMagic.remove = function (elm, oldCl) {
        if (elm && oldCl) {
            elm = this.fixElm(elm);
            for (var i = 0; i < elm.length; i++) {
                var cl;
                var replCl = "";
                if (cl = this.get(elm[i])) {
                    for (var j = 0; j < cl.length; j++) {
                        replCl += (j > 0) ? " " : "";
                        replCl += (cl[j] == oldCl) ? "" : cl[j];
                    }
                    elm[i].className = replCl;
                }

                try
                {
                    if (("required"==oldCl || "invalid"==oldCl) && (elm[i].type=="radio" || elm[i].type=="checkbox"))
                    {
                        var oldcl2 = oldCl+elm[i].type;
                        this.remove(elm[i], oldcl2);
                    }
                }
                catch (e) { }
            }
            return true;
        }
        return false;
    };

//vrati pole vsetkych classov, ktore objekt ma
//(toto je pomocna funkcia, ktoru vyuzivaju dalsie funkcie, ale moze sa pouzit aj samostatne)
    classMagic.get = function (elm) {
        if (elm) {
            return (elm.className == "") ? false : elm.className.split(" ");
        }
        return false;
    };

//pomocna funkcia, ktore prevedie samostatny element na pole
    classMagic.fixElm = function (elm) {
        if (elm) {
            if (!elm.sort) {
                elm = [elm];
            }
            return elm;
        }
        return false;
    }



    function checkNaN(value)
    {
        if (isNaN(value))
        {
            return(0);
        }

        value = (""+value.toFixed(2)).replace(".", ",");
        return(value);
    }

//Excel funkcie
    function rounddown(n, nd)
    {
        if(isFinite(n) && isFinite(nd))
        {
            var sign_n = (n<0)?-1:1;
            var abs_n=Math.abs(n);
            var factor=Math.pow(10,nd);
            return sign_n*Math.floor(abs_n*factor)/factor;
        }
        else
        {
            return NaN;
        }
    }

    function round2(fltValue)
    {
        return Math.round(fltValue * 100) / 100;
    }

    function getFloat(value)
    {
        value = value.replace(/,/gi, ".");
        return(parseFloat(value));
    }

    function onlyPositive(value)
    {
        if (value < 0) return 0;
        return value;
    }

    function smallerNumber(value1, value2)
    {
        if (value1 < value2) return value1;
        return value2;
    }

    function biggerNumber(value1, value2)
    {
        if (value1 > value2) return value1;
        return value2;
    }

    function ifCondition(condition, value1, value2)
    {
        if (condition) return value1;
        return value2;
    }

    var REQCOND1 = "no";
    var REQCOND2 = "no";
    var REQCOND3 = "no";
    var REQCOND4 = "no";
    var REQCOND5 = "no";
    var REQCOND6 = "no";

    /*
    checkForm by Riki "Fczbkk" Fridrich, 2002
    ver 2.0
    http://www.fczbkk.com/
    mailto:riki@fczbkk.com
    You should find latest version of this script and documentation at
    http://www.fczbkk.sk/com/check_form/
    */



    /*
    This is second version of my script for automatic checking of forms. It should
    work in in all standards-compliant browsers (Mozilla, Internet Explorer 5+), it
    downgrades OK in older browsers. No, it's not working in Opera (I said it works
    in standards-compliant browsers only).

    It requires additional functions and libraries:
    - event_attacher.js
        http://www.fczbkk.com/js/event_attacher/
    - fix_e.js
        http://www.fczbkk.com/js/fix_e/
    - class_magic.js
        http://www.fczbkk.com/js/class_magic/

    ATTENTION!!! Additional libraries and functions must be placed/linked before
    checkForm.

    All used scripts and libraries are writen by me, except "Event Attacher", which
    was writen by Scott Andrew (http://www.scottandrew.com/). Feel free to use
    and/or modify them (as far as I know, "Event Attacher" is free to use too), just
    please let me know where you used it if possible (I want to see it in action).
    Any suggestions, comments, bugreports, modifications or functionality
    enhancements are wellcome.
    */

    /*
    Formatting of the fields (e.g. highlighting of "required" or "invalid" fields)
    is defined in external CSS file "check_field.css". These definitions can be
    modified to fit your layout and they can be moved anywhere into your CSS
    definitions.
    */

// object holding all other functions and variables
    var checkForm = {};

    /*
    Regular expression builder, used for simplier adding or editing of character
    groups.
    */

    checkForm.buildRegExp = function(str) {
        if (str) {return new RegExp("^[" + str + "]{1,}$");}
        return false;
    }

    /* -------------- customizations start here -------------- */

    /*
    Message, which will be displayed in the alert, when user tries to submit
    incomplete or invalid form.
    */

    checkForm.invalidMsg = "<iwcm:text key="checkform.form_not_filled_ok"/>";
    checkForm.allreadySendingMsg = "<iwcm:text key="checkform.allready_sending_please_wait"/>";

//toto sa pouziva v custom onSubmit funkciach, ak vracaju false, musia toto nastavit na false
    checkForm.isInvalidForm = false;

//ak chceme zobrazit klasicku error hlasku hore a nie vedla inputov daj true
    checkForm.showClassicErrorMessage = <%=Constants.getBoolean("formmailShowClassicErrorMessage")%>;

    /*
    Acceptable characters or regular expressions defining valid content of checked
    classes.
    */

    checkForm.fieldType = new Array();
    checkForm.title = new Array();

    checkForm.trimRegexp = new RegExp("^[ \f\n\r\t\v]+|[ \f\n\r\t\v]+$", "i");

    <%
        FormDB myFormDB = FormDB.getInstance();
        List<String[]> regularExpr = myFormDB.getAllRegularExpression();
        for(String[] regExp: regularExpr)
          {%>
    checkForm.fieldType["<%=regExp[1] %>"] =	new RegExp("<%=regExp[2] %>", "i");
    checkForm.title["<%=regExp[1] %>"] = "<iwcm:text key="<%=regExp[0] %>"/>";
    <% }
    %>
    /*
    Default values for classes. These will be displayed, if form field with given
    class is focused and empty.
    */

    checkForm.defaultValue = new Array();
    checkForm.defaultValue["url"] =						"http://www.";
    checkForm.defaultValue["email"] =					"@";

    checkForm.formula = new Array();

    /* -------------- customizations end here -------------- */

    /*
    Initialisation of checkForm when document is loaded. It finds all forms and
    fields, checks them and adds events to them.
    */

    checkForm.allreadyInitialized = false;

    checkForm.init = function() {

        if (checkForm.allreadyInitialized == true) return;
        checkForm.allreadyInitialized = true;

        var forms = document.getElementsByTagName("form");
        for (var i = 0; i < forms.length; i++)
        {
            if(classMagic.has(forms[i], 'noCheckForm')) {
                continue;
            }
            addEventCheckForm(forms[i], "submit", checkForm.check);
            addEventCheckForm(forms[i], "reset", checkForm.resetClick);
            if (classMagic.has(forms[i], "checkFormClassicMessage")) checkForm.showClassicErrorMessage = true;
        }

        var fields = checkForm.findInputs();
        for (var i = 0; i < fields.length; i++)
        {
            if (fields[i].value && fields[i].value.indexOf("=")==0 && fields[i].form.name!="")
            {
                //window.alert("value="+fields[i].value);
                fnc = fields[i].value.replace(/]/gi, ".value)");
                fnc = fnc.replace(/\[/gi, "getFloat(document."+fields[i].form.name+".");
                fnc = fnc.replace(/=/gi, "=checkNaN(");
                fnc = fnc + ")";
                fnc = "document."+fields[i].form.name+"." + fields[i].name + ".value"+ fnc + ";";
                //window.alert("fnc: " + fnc);
                checkForm.formula[checkForm.formula.length] = fnc;
                //window.alert("size: " + checkForm.formula.length);
            }

            if (fields[i].type.toLowerCase().indexOf("select")!=-1)
            {
                //pre select box nemozu byt ostatne eventy lebo to v IE blblo
                addEventCheckForm(fields[i], "change", checkForm.checkField);
            }
            else
            {
                checkForm.fillTitles(fields[i]);

                //ak je vypnuta validacia pri inicializacii tak nevaliduj
                if(<%=checkFormValidateOnInit%>)
                {
                    checkForm.checkField(fields[i]);
                }
                else
                {
                    //vypnuta validacia pri inicializacii -> nevalidovat len ak je pole prazdne
                    if(!isEmpty(fields[i].value))
                        checkForm.checkField(fields[i]);
                }

                addEventCheckForm(fields[i], "blur", checkForm.checkImplBlur);
                addEventCheckForm(fields[i], "focus", checkForm.checkField);
                if (classMagic.has(fields[i], "noEnterSubmit"))
                {
                    addEventCheckForm(fields[i], "keydown", checkForm.checkFieldEnter);
                }
                addEventCheckForm(fields[i], "keyup", checkForm.checkField);

                addEventCheckForm(fields[i], "click", checkForm.checkField);

                if ($(fields[i]).is(":checkbox")) {
                    addEventCheckForm(fields[i], "change", checkForm.checkImplBlur);
                }
                else {
                    addEventCheckForm(fields[i], "change", checkForm.checkField);
                }

                //pridaj mu classu, aby sa to dalo rozumnejsie stylovat
                classMagic.add(fields[i], "input"+fields[i].type.toLowerCase());

                //pridaj requiredradio pre required radia
                if (fields[i].type=="radio" || fields[i].type=="checkbox")
                {
                    if (classMagic.has(fields[i], "required"))
                    {
                        var newcl = "required"+fields[i].type;
                        classMagic.add(fields[i], newcl);
                    }
                }
            }
        }
    }

    function isEmpty(obj) {
        if (typeof obj == 'undefined' || obj == null || obj == '' || obj == '@') return true;
        if (typeof obj == 'number' && isNaN(obj)) return true;
        if (obj instanceof Date && isNaN(Number(obj))) return true;
        return false;
    }

    checkForm.resetClickLastE = null;
    checkForm.resetClick = function(e)
    {
        var formResultDiv = document.getElementById("ajaxFormResultContainer");
        if (formResultDiv != null)
        {
            formResultDiv.style.display = "none";
            formResultDiv.innerHTML = "";
        }
        checkForm.resetClickLastE = e;
        window.setTimeout(checkForm.resetClickRecheck, 100);
    }
    checkForm.resetClickRecheck = function()
    {
        if (checkForm.resetClickLastE != null) checkForm.recheck(checkForm.resetClickLastE);
    }

    /*
    Helper functions, which finds all fields in given element and returns them in
    an array. If no element is specified, all fields in document are returned.
    */

    checkForm.findInputs = function(elm) {
        var fields = new Array();
        if (!elm) {elm = document;}

        var selects = elm.getElementsByTagName("select");
        var textareas = elm.getElementsByTagName("textarea");
        var inputs = elm.getElementsByTagName("input");

        for (var i = 0; i < inputs.length; i++) {
            if ((inputs[i].type == "text") || (inputs[i].type == "email") || (inputs[i].type == "tel") || (inputs[i].type == "date") || (inputs[i].type == "number") || (inputs[i].type == "password") || (inputs[i].type == "file") || (inputs[i].type == "radio") || (inputs[i].type == "checkbox")) {
                fields[fields.length] = inputs[i];
            }
        }

        for (var i = 0; i < textareas.length; i++) {
            fields[fields.length] = textareas[i];
        }

        for (var i = 0; i < selects.length; i++) {
            fields[fields.length] = selects[i];
        }

        return fields;
    }

    /*
    Checking the form before submitting. To be sure, we will check all fields of
    given form once more and cancel the submit action if some fields are invalid.
    */
    checkForm.allreadySending = false;

    checkForm.check = function(e)
    {
        if (checkForm.isInvalidForm==true)
        {
            return false;
        }

        e = fixECheckForm(e);
        var obj = e.target;
        while (obj.tagName != "FORM") {
            obj = obj.parentNode;
        }

        var isSubmit = (e.type == "submit");
        if (checkForm.checkImpl(obj, isSubmit, e)==false)
        {
            return false;
        }

        if (checkForm.allreadySending == true)
        {
            alert(checkForm.allreadySendingMsg);
            e.preventDefault();
            return false;
        }
        if (e.type == "submit")
        {
            checkForm.allreadySending = true;
        }
        checkForm.hideAutocompleteOff(obj);
        return true;
    }

    checkForm.checkImpl = function(obj, isSubmit, e)
    {
        var fields = checkForm.findInputs(obj);
        var isSubmitError = false;
        var errorMessage = checkForm.invalidMsg+"\n";
        var errorMessageHtml = '<iwcm:text key="stripes.ajax.errors.header"/>';
        for (var i = 0; i < fields.length; i++)
        {
            //sprav trim (ak treba)
            checkForm.trimField(fields[i]);
            //skontroluj
            var status = checkForm.checkField(fields[i]);
            var errorBlock = [];
            var isExistErrorBlock = false;
            try
            {
                errorBlock = checkForm.getErrorBlock(fields[i]);

                if (errorBlock.length == 0) {
                    var id = fields[i].id != null ? fields[i].id : fields[i].name;
                    if (id != '') {
                        errorBlock = $("#" + id).parent().find(".cs-error");
                        if (errorBlock.length<1) errorBlock = checkForm.getErrorBlock(fields[i]);
                    }
                    else {
                        errorBlock = $();
                    }
                }
                isExistErrorBlock = errorBlock.length > 0;
            } catch (e) { console.log(e); }

            if (status == "invalid")
            {
                if (isSubmit)
                {
                    isSubmitError = true;
                    var msg = checkForm.markAndGetLabelText(fields[i], true);
                    if (errorMessage.indexOf("- "+msg)==-1)
                    {
                        if(!isExistErrorBlock)
                        {
                            errorMessage = errorMessage + "\n- " + msg;
                            errorMessageHtml = errorMessageHtml + "\n<li>" + msg;
                            if (fields[i].title != null && fields[i].title != undefined && fields[i].title!="") {
                                errorMessageHtml += " - " + fields[i].title
                            }
                            errorMessageHtml += "</li>";

                            if(typeof $(fields[i]).attr("rel") != "undefined")
                            {
                                msg = $(fields[i]).attr("rel");
                            }
                            else {
                                msg += " " + fields[i].title;
                            }

                            if (checkForm.showClassicErrorMessage==false) {
                                var el;
                                var id = fields[i].id != null ? fields[i].id : fields[i].name;
                                if (id != '') {
                                    el = $("#" + id).next(".cs-error");
                                }
                                else {
                                    el = $();
                                }
                                el.parent().append('<div class="help-block cs-error cs-error-'+obj.id+'">'+msg+'</div>').show();
                            }
                        }
                        else {
                            if (errorBlock.html() == "") {
                                errorBlock.html(msg +' - ' + fields[i].title);
                            }
                            errorBlock.show();
                        }
                    }
                }
                else
                {
                    return false;
                }
            }
            else
            {
                //odznac label, ze je zle vyplneny
                checkForm.markAndGetLabelText(fields[i], false);
            }
        }

        if (isSubmitError)
        {
            checkForm.printAlertMessage(errorMessageHtml, errorMessage, e, obj);

            if (e!=null && e.preventDefault)
            {
                e.preventDefault();
            }

            return false;
        }
    }

    checkForm.checkImplBlur = function(e)
    {
        var errorMessage = checkForm.invalidMsg+"\n";
        var errorMessageHtml = '<iwcm:text key="stripes.ajax.errors.header"/>';
        var obj;
        if (e && e.tagName) {
            obj = e;
        } else {
            e = fixECheckForm(e);
            obj = e.target;
        }
        //window.alert(fields[i]);
        isSubmit = true;
        //sprav trim (ak treba)
        checkForm.trimField(obj);
        //skontroluj
        var status = checkForm.checkField(obj);
        var errorBlock = checkForm.getErrorBlock(obj);
        if (errorBlock.length == 0) {
            var id = obj.id != null ? obj.id : obj.name;
            if (id != '') {
                errorBlock = $("#" + id).next(".cs-error");
            }
            else {
                errorBlock = $();
            }
        }
        var isExistErrorBlock = errorBlock.length > 0;

        if (status == "invalid")
        {
            var msg = checkForm.markAndGetLabelText(obj, true);

            if (errorMessage.indexOf("- "+msg)==-1)
            {
                if(!isExistErrorBlock){
                    if($(obj).attr("rel") != undefined ){
                        msg = $(obj).attr("rel");

                    }else{
                        msg += " " + obj.title;
                    }
                    if (checkForm.showClassicErrorMessage==false) {
                        var el;
                        var id = obj.id != null ? obj.id : obj.name;
                        if (id != '') {
                            el = $("#"+obj.id);
                        }
                        else {
                            el = $();
                        }
                        el.parent().append('<div class="help-block cs-error cs-error-'+obj.id+'">'+msg+'</div>');
                    }
                }else{
                    if (errorBlock.html() == "") {
                        errorBlock.html(msg +' - ' + obj.title);
                    }
                    errorBlock.show();
                }
            }
        }
        else
        {
            if(isExistErrorBlock) {
                errorBlock.hide();
            }

            //odznac label, ze je zle vyplneny
            checkForm.markAndGetLabelText(obj, false);
        }
    }

    checkForm.getErrorBlock = function(obj) {
        var id = obj.id != null ? obj.id : obj.name;
        if (id != '') {
            return $(".cs-error-" + obj.id);
        }
        return $();
    }

    checkForm.printAlertMessage = function(errorMessageHtml, errorMessage, e, obj)
    {
        try
        {
            if (errorMessageHtml == null) errorMessageHtml="<div class='ajaxError'>"+errorMessage+"</div>";

            var errorDivElement = document.getElementById("checkFormAlertDiv");
            if (errorDivElement == null) errorDivElement = document.getElementById("ajaxFormResultContainer");
            if (errorDivElement == null && ($('.cs-error').length == 0 || checkForm.showClassicErrorMessage==true))
            {
                //console.log("Vytvaram error div message");
                //vytvor ho
                var form = null;
                if (obj != null && obj != undefined) form = $(obj)
                else form = $("form[name=formMailForm]");
                form.prepend("<div id='formResult'><div id='ajaxFormResultContainer'></div></div>");
                errorDivElement = document.getElementById("ajaxFormResultContainer");
            }

            if (errorDivElement != null) {
                //console.log("Zobrazujem, html="+$(errorDivElement).html()+"; length=", $(errorDivElement).html().length);
                if ($(errorDivElement).html().length < 5) $(errorDivElement).html(checkForm.invalidMsg);
                $(errorDivElement).addClass("alert alert-danger");
                $(errorDivElement).removeClass("alert-success");
            }

            if ($('.cs-error').length > 0 && checkForm.showClassicErrorMessage==false) {
                var csError = $('.cs-error:visible').first();
                if (csError.length > 0) {
                    $("html,body").animate({ scrollTop: (csError.offset().top - 100) }, { duration: 'fast', easing: 'linear'});
                }
            }
            else if (errorDivElement != null)
            {
                try
                {
                    //console.log("Zobrazujem error hlasenie, div=", errorDivElement, "message=", errorMessageHtml);
                    $(errorDivElement).html(errorMessageHtml+"</ol>").show("slow");
                    //div.tab-pane pridane pre tabovany content (usredit)
                    $("html,body,div.tab-pane").animate({ scrollTop: ($(errorDivElement).offset().top - 100) }, { duration: 'fast', easing: 'linear'});
                }
                catch (e) {
                    alert(errorMessage);
                }
            }
            else {
                alert(errorMessage);
            }

            if (e!=null && e.preventDefault)
            {
                e.preventDefault();
            }
        }
        catch (e)
        {
            alert(errorMessage);
        }
    }

    checkForm.markAndGetLabelText = function(field, isInvalid)
    {
        //najskor skus najst label pre dany field
        var id = field.id;
        if (id == "") id = field.name;

        var text = null;
        if (id != null)
        {
            //skus najst label pre dane pole
            var labelElm = checkForm.formGetLabel(id);
            if (labelElm != null)
            {
                if (isInvalid)
                {
                    $("form").removeClass("sending");
                    if (field.type == "radio" || field.type == "checkbox")
                    {
                        var leftLabelElm = checkForm.formGetLabel(field.name);
                        if (leftLabelElm != null) text = checkForm.getInnerText(leftLabelElm);
                    }
                    if (text == null || text == "") text = checkForm.getInnerText(labelElm);

                    if (!classMagic.has(field, "noCheckForm")) {
                        classMagic.add(labelElm, "invalidLabel");
                    }
                }
                else
                {
                    if (!classMagic.has(field, "noCheckForm")) {
                        classMagic.remove(labelElm, "invalidLabel");
                    }
                }
            }
        }

        if (text==null && id.indexOf("wjcaptcha")!=-1) {
            text = "<iwcm:text key="checkform.captchaLabelText"/>";
        }

        if (text == null) {
            text = id;
        }

        if (text == null) {
            text = $(field).prop('name');
        }

        var dataName = $(field).data('name');
        if (typeof dataName != "undefined" && dataName != "") {
            text = dataName;
        }

        return text;
    }

    checkForm.getInnerText = function(elem)
    {
        var hasInnerText = (document.getElementsByTagName("body")[0].innerText != undefined) ? true : false;
        if(!hasInnerText)
        {
            return elem.textContent;
        }
        return elem.innerText;
    }

    checkForm.formGetLabel = function(forField)
    {
        if (forField == null || forField == "") return null

        var labels = document.getElementsByTagName("LABEL");
        for (i=0; i < labels.length; i++)
        {
            if (labels[i].htmlFor == forField) return labels[i];
        }
        return null;
    }


    checkForm.hideAutocompleteOff = function(obj)
    {
        var fields = obj.elements;
        for (var i = 0; i < fields.length; i++)
        {
            var classes = classMagic.get(fields[i]);
            //taketo pole nastavim na hidden aby si FF nepamatal heslo
            if (classMagic.has(fields[i], "autocompleteoff"))
            {
                fields[i].setAttribute("type", "hidden");
            }
        }
    }

    checkForm.showAutocompleteOff = function(obj)
    {

        var fields = obj.elements;
        for (var i = 0; i < fields.length; i++)
        {
            var classes = classMagic.get(fields[i]);
            //taketo pole nastavim na hidden aby si FF nepamatal heslo
            if (classMagic.has(fields[i], "autocompleteoff"))
            {
                fields[i].setAttribute("type", "password");
            }
        }
    }

    checkForm.btnSubmit = function(obj)
    {
        if (checkForm.isInvalidForm==true)
        {
            return false;
        }

        var fields = checkForm.findInputs(obj);
        for (var i = 0; i < fields.length; i++)
        {
            //window.alert(fields[i]);

            //sprav trim (ak treba)
            checkForm.trimField(fields[i]);
            //skontroluj
            var status = checkForm.checkField(fields[i]);
            if (status == "invalid")
            {
                alert(checkForm.invalidMsg);
                return false;
            }
        }
        if (checkForm.allreadySending == true)
        {
            alert(checkForm.allreadySendingMsg);
            return false;
        }
        checkForm.allreadySending = true;
        checkForm.hideAutocompleteOff(obj);
        obj.submit();
        return true;
    }

    /*
    Prekontroluje polia, ale nespravi ziadny alert, potrebne po zmene REQCOND
    */
    checkForm.recheck = function(e)
    {
        var valid = true
        var fields = checkForm.findInputs();
        for (var i = 0; i < fields.length; i++)
        {
            status = checkForm.checkField(fields[i]);
            if (valid && status == "invalid" || status == "required")
                valid = false
        }
        return valid
    }

    checkForm.recheckAjax = function(form)
    {
        return checkForm.checkImpl(form, true, null);
    }

    /*
    checks one field, prints message if not valid
    */
    checkForm.recheckFieldAjax = function(element)
    {
        var status = checkForm.checkField(element);
        var errorMessage = checkForm.invalidMsg+"\n";
        var errorMessageHtml = '<iwcm:text key="stripes.ajax.errors.header"/>';
        if (status == "invalid")
        {
            //TODO: mark the input

            var msg = checkForm.markAndGetLabelText(element, true);
            if (errorMessage.indexOf("- "+msg)==-1)
            {
                errorMessage = errorMessage + "\n- " + msg;
                errorMessageHtml = errorMessageHtml + "\n<li>" + msg;
                if (element.title != null && element.title != undefined && element.title!="") errorMessageHtml += " - " + element.title
                errorMessageHtml += "</li>";
            }
            checkForm.printAlertMessage(errorMessageHtml, errorMessage, null, element.form);
            return false;
        }
        return true;
    }

    /*
    checks more fields, prints messages if at least one not valid
    */
    checkForm.recheckFieldsAjax = function(fields)
    {
        var ret =true;
        var errorMessage = checkForm.invalidMsg+"\n";
        var errorMessageHtml = '<iwcm:text key="stripes.ajax.errors.header"/>';
        var form = fields[0].form;
        for (var i = 0; i < fields.length; i++)
        {
            checkForm.trimField(fields[i]);
            var status = checkForm.checkField(fields[i]);
            if (status == "invalid")
            {
                ret = false;
                var msg = checkForm.markAndGetLabelText(fields[i], true);
                if (errorMessage.indexOf("- "+msg)==-1)
                {
                    errorMessage = errorMessage + "\n- " + msg;
                    errorMessageHtml = errorMessageHtml + "\n<li>" + msg;
                    if (fields[i].title != null && fields[i].title != undefined && fields[i].title!="")errorMessageHtml += " - " + fields[i].title
                    errorMessageHtml += "</li>";
                }
            }
            else
            {
                //odznac label, ze je zle vyplneny
                checkForm.markAndGetLabelText(fields[i], false);
            }
        }
        if(!ret) checkForm.printAlertMessage(errorMessageHtml, errorMessage, null, form);
        return ret;
    }



    /*
    Checking the form field.
    */
    checkForm.checkFieldEnter = function(e)
    {
        if (e.keyCode && e.keyCode==13)
        {
            window.alert("Formulár nie je možné odoslať klávesou enter, kliknite prosím na príslušné tlačítko pre odoslanie formuláru.");
            if (e.preventDefault) {
                e.preventDefault();
            }
            return false;
        }
        return true;
    }

    checkForm.trimField = function(e)
    {
        var obj;
        if (e && e.tagName) {
            obj = e;
        } else {
            e = fixECheckForm(e);
            obj = e.target;
        }

        //tieto nebudeme kontrolovat
        if (classMagic.has(obj, "trim")==false) return;

        //2x je to tu lebo to netrimovalo spravne konce
        obj.value = obj.value.replace(checkForm.trimRegexp, "");
        obj.value = obj.value.replace(checkForm.trimRegexp, "");
    }


    var checkFormSetCursorObj = null;
    var checkFormSetCursorPos = 0;
    checkForm.setCursorAsync = function()
    {
        var node = checkFormSetCursorObj;
        var pos = checkFormSetCursorPos;
        try
        {
            var node = (typeof node == "string" || node instanceof String) ? document.getElementById(node) : node;

            if(!node)
            {
                return false;
            }
            else if(node.createTextRange)
            {
                var textRange = node.createTextRange();
                textRange.collapse(true);
                textRange.moveEnd(pos);
                textRange.moveStart(pos);
                textRange.select();
                return true;
            }
            else if(node.setSelectionRange)
            {
                node.setSelectionRange(pos,pos);
                return true;
            }
        }
        catch (e) { }

        return false;
    }
    checkForm.setCursor = function(node,pos)
    {
        checkFormSetCursorObj = node;
        checkFormSetCursorPos = pos;
        setTimeout(checkForm.setCursorAsync, 50);
    }

    checkForm.checkField = function(e)
    {
        var obj;
        if (e && e.tagName) {
            obj = e;
        } else {
            e = fixECheckForm(e);
            obj = e.target;
        }

        //recalculate values
        try
        {
            if (checkForm.formula.length>0)
            {
                for (i=0; i< checkForm.formula.length; i++)
                {
                    try
                    {
                        //window.alert("vypocitavam: "+checkForm.formula[i]);
                        eval(checkForm.formula[i]);
                    }
                    catch (e2)
                    {
                        window.status= "chyba vypoctu 2: "+e2+" fnc: "+checkForm.formula[i];
                    }
                }
            }
        }
        catch (e)
        {
            window.status= "chyba vypoctu: "+e;
        }

        var fieldOK = true;
        var classes = classMagic.get(obj);

        //tieto nebudeme kontrolovat
        if (classMagic.has(obj, "noCheckForm")) return;

        if (classMagic.has(obj, "captcha"))
        {
            var actualStatus = "invalid";
            if (classMagic.has(obj, "invalid")==false) actualStatus = "valid";

            checkForm.checkCaptcha(obj);

            return actualStatus;
        }

        classMagic.add(obj, "invalid");

        // conditional required field
        if (classMagic.has(obj, "reqcond1"))
        {
            if ("yes"==REQCOND1)
            {
                classMagic.remove(obj, "invalid");
                classMagic.remove(obj, "required");
                return("valid");
            }
            else
            {
                classMagic.add(obj, "invalid");
                classMagic.add(obj, "required");
            }
        }

        if (classMagic.has(obj, "reqcond2"))
        {
            if ("yes"==REQCOND2)
            {
                classMagic.remove(obj, "invalid");
                classMagic.remove(obj, "required");
                return("valid");
            }
            else
            {
                classMagic.add(obj, "invalid")
                classMagic.add(obj, "required")
            }
        }

        if (classMagic.has(obj, "reqcond3"))
        {
            if ("yes"==REQCOND3)
            {
                classMagic.remove(obj, "invalid");
                classMagic.remove(obj, "required");
                return("valid");
            }
            else
            {
                classMagic.add(obj, "invalid");
                classMagic.add(obj, "required");
            }
        }

        if (classMagic.has(obj, "reqcond4"))
        {
            if ("yes"==REQCOND4)
            {
                classMagic.remove(obj, "invalid");
                classMagic.remove(obj, "required");
                return("valid");
            }
            else
            {
                classMagic.add(obj, "invalid")
                classMagic.add(obj, "required")
            }
        }

        if (classMagic.has(obj, "reqcond5"))
        {
            if ("yes"==REQCOND5)
            {
                classMagic.remove(obj, "invalid");
                classMagic.remove(obj, "required");
                return("valid");
            }
            else
            {
                classMagic.add(obj, "invalid")
                classMagic.add(obj, "required")
            }
        }

        if (classMagic.has(obj, "reqcond6"))
        {
            if ("yes"==REQCOND6)
            {
                classMagic.remove(obj, "invalid");
                classMagic.remove(obj, "required");
                return("valid");
            }
            else
            {
                classMagic.add(obj, "invalid")
                classMagic.add(obj, "required")
            }
        }

        var i = 0;
        // required field can't be empty
        if (classMagic.has(obj, "required"))
        {
            if (obj.type == "radio" || obj.type == "checkbox")
            {
                fieldOK = checkForm.isRadioSelected(obj);
            }
            else
            {
                i = 0;
                while (i < classes.length)
                {
                    if ((e.type == "blur") && (obj.value == checkForm.defaultValue[classes[i]])) {
                        obj.value = "";
                    }
                    i++;
                }

                fieldOK = (checkForm.isBlank(obj.value)) ? false : true;
            }
        }

        // find classes of given field and check, if there are any rules for it
        i = 0;
        while (i < classes.length)
        {
            if (checkForm.defaultValue[classes[i]]) {
                if ((e.type == "focus") && checkForm.isBlank(obj.value))
                {
                    obj.value = checkForm.defaultValue[classes[i]];
                    checkForm.setCursor(obj, 0);
                }

                if ((e.type == "blur") && (obj.value == checkForm.defaultValue[classes[i]])) {
                    obj.value = "";
                }
            }

            if (fieldOK && obj.type!="checkbox" && obj.type!="radio")
            {
                if (checkForm.fieldType[classes[i]])
                {
                    fieldOK = (!checkForm.isBlank(obj.value) && checkForm.checkChars(obj.value, checkForm.fieldType[classes[i]])) ? false : true;
                    i = classes.length;
                }
            }
            i++;
        }

        //password2 check
        //console.log("Pass 2 check, name=", obj.name);
        if ("password2"===obj.name && typeof obj.form["usr.password"] != "undefined") {
            var pass2 = obj.value;
            var pass1 = obj.form["usr.password"].value;
            //console.log("pass1=", pass1, "pass2=", pass2);
            if (pass1 != pass2) fieldOK = false;
        }

        fieldOK = fieldOK || obj.type == 'hidden';

        // if content of the field is not OK, highlight it as invalid
        (fieldOK) ? classMagic.remove(obj, "invalid") : classMagic.add(obj, "invalid");

        /*
        Function like this should return "true" or "false". But this would be
        blocking of keyboard input in case of incomplete content of the field
        (e.g. URL). That's why this function returns "valid" or "invalid".
        */
        return (fieldOK) ? "valid" : "invalid";
    }

    checkForm.fillTitles = function(obj)
    {
        try
        {
            if (obj.title!="") return;

            if (classMagic.has(obj, "required"))
            {
                obj.title = "<iwcm:text key="checkform.title.required"/>";
            }

            var classes = classMagic.get(obj);
            var i = 0;
            for (i=0; i < classes.length; i++)
            {
                var text = checkForm.title[classes[i]];
                if (text != null && text!=undefined && text != "")
                {
                    if (obj.title != "") obj.title = obj.title+", "+text;
                    else obj.title = obj.title+" "+text;
                }
            }
        } catch (e) {}
    }

    /*
    Checks if given string contains given pattern (regullar expression).
    */

    checkForm.checkChars = function(str, re) {
        if (str && re) {
            return (str.search(re) == -1) ? true : false;
        }
        return true;
    }

    /*
    Checks, if given string is blank or not.
    */

    checkForm.isBlank = function(str)
    {
        str = str.replace(checkForm.trimRegexp, "");
        str = str.replace(checkForm.trimRegexp, "");
        return (str == "" || str == " ") ? true : false;
    }

    /*
    WebJET: skontroluje, ci je zvolene aspon jedno radio/checkbox z danej skupiny
    */
    checkForm.isRadioSelected = function(obj)
    {
        try
        {
            var radioForm = obj.form;
            var radios = radioForm.elements[obj.name];

            if (radios.length==undefined)
            {
                //mame jedno radio
                if (obj.checked)
                {
                    classMagic.remove(obj, "invalid");
                    if (obj.type=="radio") classMagic.remove(obj, "required");
                }
                else
                {
                    classMagic.add(obj, "invalid");
                }
                return obj.checked;
            }

            var someChecked = false;
            var checkedCount = 0;
            for (i=0; i< radios.length; i++)
            {
                if (radios[i].checked)
                {
                    someChecked = true;
                    checkedCount++;
                }
            }

            //skontroluj hodnoty pre min a max len
            try
            {
                var classes = classMagic.get(obj);
                if (classes && classes.length)
                {
                    for (i=0; i< classes.length; i++)
                    {
                        if (classes[i].indexOf("countCond-")==0)
                        {
                            var data = classes[i].split("-");
                            if (data && data.length==3)
                            {
                                if (checkedCount < data[1]) someChecked = false;
                                if (checkedCount > data[2]) someChecked = false;
                            }
                        }
                    }
                }
            }
            catch (ex2) {}

            for (i=0; i< radios.length; i++)
            {
                if (someChecked == false)
                {
                    classMagic.add(radios[i], "invalid");

                    //najdi prislusny label a nastav aj tomu
                    var elmId = radios[i].id;
                    var label = $("label[for="+elmId+"]").addClass("invalidLabel");
                }
                else
                {
                    //cb sa uz neda odselectnut, mozeme removnut aj required aby sa to ako tak odstylovalo
                    classMagic.remove(radios[i], "invalid");

                    //najdi prislusny label a nastav aj tomu
                    var elmId = radios[i].id;
                    var label = $("label[for="+elmId+"]").removeClass("invalidLabel");

                }
            }

            return someChecked;
        }
        catch (ex)
        {
            //alert(ex);
        }
        return true;
    }

    /*
    WebJET - kontrola captcha kodu
    */
    checkForm.xmlhttp = null;
    checkForm.captchaObj = null;
    checkForm.checkCaptcha = function(obj)
    {
        var value = obj.value;
        checkForm.xmlhttp=null;
        if (window.XMLHttpRequest)
        {
            // code for IE7+, Firefox, Chrome, Opera, Safari
            checkForm.xmlhttp=new XMLHttpRequest();
        }
        else
        {
            // code for IE6, IE5
            checkForm.xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
        }

        classMagic.add(obj, "invalid");
        checkForm.captchaObj = obj;

        checkForm.xmlhttp.onreadystatechange=checkForm.checkCaptchaChange;
        checkForm.xmlhttp.open("GET", "/components/form/catpcha_ajax.jsp?v="+value, true);
        checkForm.xmlhttp.send(null);
    }

    checkForm.checkCaptchaChange = function()
    {
        try
        {
            if (checkForm.captchaObj != null)
            {
                if (checkForm.xmlhttp.readyState==4)
                {
                    // 4 = "loaded"
                    if (checkForm.xmlhttp.status==200)
                    {
                        // 200 = "OK"
                        classMagic.remove(checkForm.captchaObj, "invalid");
                    }
                    else
                    {
                        classMagic.add(checkForm.captchaObj, "invalid");
                    }
                }
            }
        } catch (e) {}
    }

    /*
    Initialize script when document is loaded.
    */
    addEventCheckForm(window, "load", checkForm.init);
//failsafe, ak uz sa nahodou okno loadlo a horny event nenastane
    window.setTimeout(checkForm.init, 2000);

    /* schova dany element ak checkbox nie je zaskrtnuty */
    function formShowHide(checkbox, elementNamesShow, elementNamesHide)
    {
        if (checkbox.checked == false) return;

        var elements = elementNamesShow.split(",");
        for (i=0; i< elements.length; i++)
        {
            var element = document.getElementById(elements[i]);
            if (element != null) element.style.display="block";
        }
        elements = elementNamesHide.split(",");
        for (i=0; i< elements.length; i++)
        {
            var element = document.getElementById(elements[i]);
            if (element != null) element.style.display="none";
        }
    }

//alert na odoslanie formularu
    if (location.href.indexOf("formsend=true")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="checkform.sent"/>", null);
    }

    if (location.href.indexOf("formfail=formIsAllreadySubmitted")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="checkform.formIsAllreadySubmitted"/>", null);
    }
    else if (location.href.indexOf("formfail=javascript")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="checkform.fail_javascript"/>", null);
    }
    else if (location.href.indexOf("formfail=probablySpamBot")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="checkform.fail_probablySpamBot"/>", null);
    }
    else if (location.href.indexOf("formfail=requiredFields")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="checkform.fail_requiredFields"/>", null);
    }
    else if (location.href.indexOf("formfail=bad_file")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="checkform.bad_file"/>", null);
    }
    else if (location.href.indexOf("formfail=captcha")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="captcha.textNotCorrect"/>", null);
    }
    else if (location.href.indexOf("formfail=emailNotSend")!=-1)
    {
        window.alert("<iwcm:text key="checkform.emailNotSend"/>");
    }
    else if (location.href.indexOf("formfail=")!=-1)
    {
        checkForm.printAlertMessage(null, "<iwcm:text key="checkform.sendfail"/>", null);
    }

    <%
    pageContext.include("/components/_common/javascript/ajax_form_send.js.jsp");
    %>
