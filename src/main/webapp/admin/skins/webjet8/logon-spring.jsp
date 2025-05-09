<%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*"
%><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"
%><%
    Prop prop = Prop.getInstance(request);
    String brandSuffix = InitServlet.getBrandSuffix();
    //brandSuffix = "net";
    String title = prop.getText("logon.welcome.title");
    String subtitle = prop.getText("logon.welcome.subtitle");
    try {
        //get texts from logon.welcome.titles, split by lines and then by ;, get random line into title and subtitle
        String[] lines = prop.getText("logon.welcome.titles."+brandSuffix).split("\n");
        if (lines.length>0) {
            int index = (int)(Math.random()*lines.length);
            int id = Tools.getIntValue(request.getParameter("id"), -1);
            if (id!=-1 && id<lines.length) {
                index = id;
            }
            String[] parts = lines[index].split(";");
            if (parts.length==2) {
                title = parts[0];
                subtitle = parts[1];
            }
        }
    } catch (Exception e) {

    }
    pageContext.setAttribute("title", title);
    pageContext.setAttribute("subtitle", subtitle);
%><!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>" >
    <title>WebJET CMS</title>
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta content="width=device-width, initial-scale=1.0" name="viewport"/>
    <meta content="www.interway.sk" name="author"/>

    <iwcm:combine type="css" set="adminStandardCss" />
    <iwcm:combine type="css" set="/admin/skins/webjet8/assets/admin/pages/css/login-soft.css" combine="false"/>

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

<div class="welcome-title">
    <h1><c:out value="${title}"/></h1>
    <h2><c:out value="${subtitle}"/></h2>
</div>

<!-- LOGIN CONTENT Start -->
<div class="container">
    <div class="container-inner">
        <div class="content">


            <form:form method="post" name="logonForm" modelAttribute="userForm" action="/admin/logon/">

                <div class="form-group language-select">
                    <div class="custom-select">
                        <i class="ti ti-world"></i>
                        <span class="selected-value"></span>
                        <form:select path="language" cssClass="lang select2 form-select" onchange="selectLanguage(this)">
                            <form:option value="sk"><iwcm:text key="logon.language.slovak"/></form:option>
                            <form:option value="cz"><iwcm:text key="logon.language.czech"/></form:option>
                            <form:option value="en"><iwcm:text key="logon.language.english"/></form:option>
                        </form:select>
                    </div>
                </div>

                <div class="form-group logo">
                    <img src="/admin/v9/dist/images/logo-<%=brandSuffix%>.svg" alt="WebJET <%=brandSuffix.toUpperCase()%>" />
                </div>
                <div id="logon-form-wrapper">

                    <iwcm:present name="errors">
                        <div class="alert-wrapper">
                            <div class="alert alert-danger">
                                <span>
                                    <iwcm:text key="user.form.errors"/>
                                </span>
                            </div>
                            <div class="infotext">
                                <ul>
                                    <li>
                                        ${errors}
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </iwcm:present>
                    <iwcm:present name="cancelChangePasswordAction">
                        <div class="alert alert-success">
                            <span>
                                <iwcm:text key="logon.change_password.action_canceled"/>
                            </span>
                        </div>
                    </iwcm:present>
                    <iwcm:present name="changePasswordActionFailed">
                        <div class="alert alert-danger">
                            <span>
                                <iwcm:text key="logon.password.invalid_parameters"/>
                            </span>
                        </div>
                    </iwcm:present>
                    <c:if test="${param.act eq 'changePasswordActionSuccess'}">
                        <div class="alert alert-success">
                            <span>
                                <iwcm:text key="logon.password.change_successful"/>
                            </span>
                        </div>
                    </c:if>
                    <iwcm:present name="passResultEmail">
                        <div class="alert alert-success">
                            <span><iwcm:text key="logon.lost_password_send_success"/></span>
                        </div>
                    </iwcm:present>

                    <div class="form-group">
                        <label class="control-label"><iwcm:text key="logon.usernameOrEmail"/></label>
                        <div class="input-icon">
                            <i class="ti ti-user"></i>
                            <form:input path="username" id="username" maxlength="255" size="16" cssClass="form-control placeholder-no-fix" autocomplete="off"/>
                        </div>
                    </div>
                    <div class="form-group">
                        <label class="control-label"><iwcm:text key="user.password"/></label>
                        <div class="input-icon">
                            <i class="ti ti-lock"></i>
                            <form:password path="password" maxlength="64" size="16" cssClass="form-control placeholder-no-fix" autocomplete="off"/>
                        </div>
                        <div class="password-strength-info"></div>
                    </div>
                    <div class="form-group">
                        <button type="submit" name="login-submit" id="login-submit" class="btn btn-primary"><iwcm:text key="button.login"/><i class="ti ti-arrow-right"></i></button>
                        <button type="button" class="btn btn-secondary btn-as-link lost-password pull-right" onclick="$('#sendPassword').show();$('#logon-form-wrapper').hide();"><iwcm:text key="logon.forgotYourPassword"/></button>
                    </div>
                </div>
            </form:form>

            <!-- LOGIN CONTENT Ende -->

            <!-- LOGIN ERROR Start -->

            <form name="f_lng" method="get" action="logon.jsp">
                <input type="hidden" name="language" value="" />
            </form>


            <div id="sendPassword" style="display: none;">
                <form id="sendPasswd" name="f_passwd" method="get" action="/admin/logon/">
                    <div class="form-group">
                        <label class="control-label"><iwcm:text key="logon.usernameOrEmail"/></label>
                        <div class="input-icon">
                            <i class="ti ti-user"></i>
                            <input type="text" name="loginName" value="" class="form-control placeholder-no-fix" />
                        </div>
                        <div class="password-strength-info"><iwcm:text key="logon.recoverPassword.tooltip"/></div>
                    </div>
                    <div class="form-group">
                        <button type="submit" id="register-submit-btn" class="btn btn-primary"><iwcm:text key="logon.recoverPassword"/><i class="ti ti-arrow-right"></i></button>
                        <button type="button" class="btn btn-secondary btn-as-link lost-password pull-right" onclick="$('#logon-form-wrapper').show();$('#sendPassword').hide();"><iwcm:text key="button.login"/></button>
                    </div>
                </form>
            </div>

        </div>
    </div>
</div>
<!-- LOGIN ERROR Ende -->


<!-- LOGIN ALL Ende -->

</body>

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
        try {
            bindPasswordStrength();
            $("#username").focus();
        } catch (e) {console.log(e);}

        //preserve hash
        if (window.location.hash!="") {
            var $form = $('#userForm');
            $form.attr('action', $form.attr('action') + window.location.hash);
        }
    });

    const customSelect = document.querySelector('.custom-select');
    const selectElement = customSelect.querySelector('select');
    const selectedValue = customSelect.querySelector('.selected-value');

    selectElement.addEventListener('change', function() {
        selectedValue.textContent = selectElement.options[selectElement.selectedIndex].text;
    });
    selectedValue.textContent = selectElement.options[selectElement.selectedIndex].text;



</script>


</html>
