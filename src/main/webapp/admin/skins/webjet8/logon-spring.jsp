<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*"
%><%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"
%><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"
%><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"
%><%@ taglib uri="/WEB-INF/datetime.tld" prefix="dt"
%><%
Prop prop = Prop.getInstance(request);
String brandSuffix = InitServlet.getBrandSuffix();
%><!DOCTYPE html>
<!--[if IE 8]> <html class="ie8 no-js"> <![endif]-->
<!--[if IE 9]> <html class="ie9 no-js"> <![endif]-->
<!--[if !IE]><!-->
<html>
<!--<![endif]-->
<!-- BEGIN HEAD -->
<head>
    <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" >
    <title>WebJET CMS</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta content="width=device-width, initial-scale=1.0" name="viewport"/>
    <meta content="www.interway.sk" name="author"/>

    <iwcm:combine type="css" set="adminStandardCss" />
    <iwcm:combine type="css" set="/admin/skins/webjet8/assets/admin/pages/css/login-soft.css" combine="false"/>

    <!-- END THEME STYLES -->
    <link rel="shortcut icon" href="/admin/skins/webjet8/assets/global/img/wj/favicon-cms.ico"/>

    <iwcm:combine type="js" set="adminJqueryJs" />

    <script type="text/javascript">
        <!--
        if (parent.frames.length > 0){
            parent.location.href = location.href
        }

        function selectLanguage(select){
            if (select.form.username.value=="" && select.form.password.value==""){
                window.location.href="?language="+select.value;
            }
        }

        function setLoginName(){

            el = document.getElementById("sendPasswd");
            if (el!=null){
                if (document.logonForm.username.value != null){
                    document.f_passwd.loginName.value = document.logonForm.username.value;
                }
            }
        }

        //-->
    </script>
</head>
<body id="login" class="login">

<div class="logo">
    <img src="/admin/skins/webjet8/assets/global/img/wj/logo-<%=brandSuffix%>.png" style="max-width: 350px;" alt="" />
</div>

<!-- LOGIN CONTENT Start -->
<div class="container">
    <div class="container-inner">
        <div class="content">
            <h3 class="form-title"><iwcm:text key="logon.logon"/></h3>
            <logic:present name="errors">
                <div class="alert alert-danger display -hide">
                    <span>
                        <iwcm:text key="user.form.errors"/>
                    </span>
                    <ul>
                        <li>
                            ${errors}
                        </li>
                    </ul>
                </div>
            </logic:present>
            <logic:present name="passResultEmail">
                <div class="alert alert-danger">
                    <button class="close" data-close="alert"></button>
                    <span><iwcm:text key="logon.lost_password_send_success"/></span>
                </div>
            </logic:present>
            <form:form method="post" name="logonForm" modelAttribute="userForm" action="/admin/logon/">
                <div class="form-group">
                    <label class="control-label visible-ie8 visible-ie9"><iwcm:text key="logon.name"/>:</label>
                    <div class="input-icon">
                        <i class="fa fa-user"></i>
                        <form:input path="username" maxlength="40" size="16" cssClass="form-control placeholder-no-fix" autocomplete="off" placeholder='<%=prop.getText("user.login")%>'/>
                    </div>
                </div>
                <div class="form-group">
                    <label class="control-label visible-ie8 visible-ie9"><iwcm:text key="logon.password"/>:</label>
                    <div class="input-icon">
                        <i class="fa fa-lock"></i>
                        <form:password path="password" maxlength="32" size="16" cssClass="form-control placeholder-no-fix" autocomplete="off" placeholder='<%=prop.getText("user.password")%>'/>
                    </div>
                    <div class="password-strength-info"></div>
                </div>
                <div class="form-group">
                <label class="control-label visible-ie8 visible-ie9"><iwcm:text key="logon.language"/>:</label>
                <form:select path="language" cssClass="lang select2 form-control" onchange="selectLanguage(this)">
                    <form:option value="sk"><iwcm:text key="logon.language.slovak"/></form:option>
                    <form:option value="en"><iwcm:text key="logon.language.english"/></form:option>
                    <form:option value="cz"><iwcm:text key="logon.language.czech"/></form:option>
                    <form:option value="de"><iwcm:text key="logon.language.german"/></form:option>
                </form:select>
                </div>
                <div class="form-actions">
                    <%--<form:button name="login" value="Submit">Submit</form:button>--%>
                    <button type="submit" name="login-submit" id="login-submit" class="btn blue pull-right"><iwcm:text key="button.login"/></button>
                    <button type="button" class="btn lost-password" onclick="$('#sendPassword').show('slow');"><i class="fa fa-unlock-alt"></i> <iwcm:text key="logon.mail.lost_password"/></button>
                </div>
            </form:form>

            <!-- LOGIN CONTENT Ende -->

            <!-- LOGIN ERROR Start -->

            <form name="f_lng" method="get" action="logon.jsp">
                <input type="hidden" name="language" value="" />
            </form>


            <div id="sendPassword" style="display: none;">
                <br/><p><iwcm:text key="logon.lost_password_message"/></p>
                <form id="sendPasswd" name="f_passwd" method="get" action="">
                    <div class="form-group">
                        <label class="control-label visible-ie8 visible-ie9">><iwcm:text key="logon.name"/>:</label>
                        <input type="text" name="loginName" value="" class="form-control placeholder-no-fix" />
                        <%--<input type="submit" value="<iwcm:text key="button.send"/>"  class="button" />--%>
                    </div>
                    <div class="form-actions text-right">
                        <button type="submit" id="register-submit-btn" class="btn blue">
                            <iwcm:text key="button.send"/> <i class="m-icon-swapright m-icon-white"></i>
                        </button>
                    </div>
                </form>
            </div>

        </div>
    </div>
</div>
<!-- LOGIN ERROR Ende -->


<!-- LOGIN ALL Ende -->

</body>

<iwcm:combine type="js" set="adminStandardJs" />
<iwcm:combine type="js" set="adminLogonJs" />

<script>
    setLoginName();

    function checkPassword(password) {
        let result = zxcvbnts.core.zxcvbn(password);
        return result;
    }

    var translations = [];

    translations["wj-password-strength.rating.js"]='<iwcm:text key="wj-password-strength.rating.js"/>';
    translations["wj-password-strength.rating.0.js"]='<iwcm:text key="wj-password-strength.rating.0.js"/>';
    translations["wj-password-strength.rating.1.js"]='<iwcm:text key="wj-password-strength.rating.1.js"/>';
    translations["wj-password-strength.rating.2.js"]='<iwcm:text key="wj-password-strength.rating.2.js"/>';
    translations["wj-password-strength.rating.3.js"]='<iwcm:text key="wj-password-strength.rating.3.js"/>';
    translations["wj-password-strength.rating.4.js"]='<iwcm:text key="wj-password-strength.rating.4.js"/>';

    translations["wj-password-strength.warnings.straightRow.js"] = '<iwcm:text key="wj-password-strength.warnings.straightRow.js"/>';
    translations["wj-password-strength.warnings.keyPattern.js"] = '<iwcm:text key="wj-password-strength.warnings.keyPattern.js"/>';
    translations["wj-password-strength.warnings.simpleRepeat.js"] = '<iwcm:text key="wj-password-strength.warnings.simpleRepeat.js"/>';
    translations["wj-password-strength.warnings.extendedRepeat.js"] = '<iwcm:text key="wj-password-strength.warnings.extendedRepeat.js"/>';
    translations["wj-password-strength.warnings.sequences.js"] = '<iwcm:text key="wj-password-strength.warnings.sequences.js"/>';
    translations["wj-password-strength.warnings.recentYears.js"] = '<iwcm:text key="wj-password-strength.warnings.recentYears.js"/>';
    translations["wj-password-strength.warnings.dates.js"] = '<iwcm:text key="wj-password-strength.warnings.dates.js"/>';
    translations["wj-password-strength.warnings.topTen.js"] = '<iwcm:text key="wj-password-strength.warnings.topTen.js"/>';
    translations["wj-password-strength.warnings.topHundred.js"] = '<iwcm:text key="wj-password-strength.warnings.topHundred.js"/>';
    translations["wj-password-strength.warnings.common.js"] = '<iwcm:text key="wj-password-strength.warnings.common.js"/>';
    translations["wj-password-strength.warnings.similarToCommon.js"] = '<iwcm:text key="wj-password-strength.warnings.similarToCommon.js"/>';
    translations["wj-password-strength.warnings.wordByItself.js"] = '<iwcm:text key="wj-password-strength.warnings.wordByItself.js"/>';
    translations["wj-password-strength.warnings.namesByThemselves.js"] = '<iwcm:text key="wj-password-strength.warnings.namesByThemselves.js"/>';
    translations["wj-password-strength.warnings.commonNames.js"] = '<iwcm:text key="wj-password-strength.warnings.commonNames.js"/>';
    translations["wj-password-strength.warnings.userInputs.js"] = '<iwcm:text key="wj-password-strength.warnings.userInputs.js"/>';

    function translate(key) {
        let translated = translations[key];
        if (typeof translated != undefined && translated != null) return translated;
        return key;
    }

    function bindPasswordStrength () {
        // all package will be available under zxcvbnts
        const options = {
          //translations: zxcvbnts['language-en'].translations,
          graphs: zxcvbnts['language-common'].adjacencyGraphs,
          dictionary: {
            ...zxcvbnts['language-common'].dictionary,
            ...zxcvbnts['language-en'].dictionary,
          },
        }
        zxcvbnts.core.zxcvbnOptions.setOptions(options)

        let $element = $("#password");
        let infoMessageDiv = $("div.password-strength-info");
        let timeout = null;
        $element.on("keyup", function(e) {
            clearTimeout(timeout);
            timeout = setTimeout(()=>{
                try {
                    let result = checkPassword($(this).val());
                    //console.log("Keydown, result=", result, " score=", result.score, "warning=", result.feedback.warning, "suggestions=", result.feedback.suggestions);
                    let feedback = result.feedback.warning;
                    if (feedback!=null && feedback != "") feedback = "<br/>" + translate("wj-password-strength.warnings."+feedback+".js");
                    else feedback = "";
                    let message = translate("wj-password-strength.rating.js") + " " + translate("wj-password-strength.rating."+result.score+".js") + " " + feedback;
                    infoMessageDiv.html(message);
                } catch (e) {}
            }, 100);
        });
    };

    jQuery(document).ready(function() {
        Metronic.init(); // init metronic core components
        Layout.init(); // init current layout

        try {
            bindPasswordStrength();
        } catch (e) {console.log(e);}

        //preserve hash
        if (window.location.hash!="") {
            var $form = $('#userForm');
            $form.attr('action', $form.attr('action') + window.location.hash);
        }
    });



</script>


</html>
