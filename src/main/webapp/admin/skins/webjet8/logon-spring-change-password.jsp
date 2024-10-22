<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ page pageEncoding="utf-8"
import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"
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
    <link rel="shortcut icon" href="/admin/skins/webjet6/images/favicon.ico"/>

    <iwcm:combine type="js" set="adminJqueryJs" />

    <script type="text/javascript">
        <!--
        if (parent.frames.length > 0){
            parent.location.href = location.href
        }

        function selectLanguage(){
            if (document.logonForm.username.value=="" && document.logonForm.password.value==""){
                document.f_lng.language.value=document.logonForm.language.value;
                document.f_lng.submit();
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

<%
    Identity user = (Identity)session.getAttribute(Constants.USER_KEY+"_changepassword");
    String constStr = "Admin";
    String lng = (String)session.getAttribute(Prop.SESSION_I18N_PROP_LNG);
    if (Tools.isEmpty(lng)) lng = "sk";
%>
<div class="container">
    <div class="container-inner">
        <div class="content">
            <h3 class="form-title"><iwcm:text key="logon.logon"/></h3>
                <div class="alert alert-danger display -hide">
                        <p style="color: black;">
                            <c:if test="${empty param.auth}">
                                <iwcm:text key="logon.change_password.nesplna_nastavenia"/><br/>
                            </c:if>
                            <c:if test="${not empty param.auth}">
                                <iwcm:text key="logon.password.enter_new_password"/><br/>
                            </c:if>
                            <%if (Constants.getInt("password"+constStr+"MinLength") > 0) { %>
                                - <iwcm:text key="logon.change_password.min_length" param1='<%=Constants.getString("password"+constStr+"MinLength")%>'/><br/>
                            <% } if (Constants.getInt("password"+constStr+"MinCountOfDigits") > 0) { %>
                                - <iwcm:text key="logon.change_password.count_of_digits" param1='<%=Constants.getString("password"+constStr+"MinCountOfDigits")%>'/><br/>
                            <% } if (Constants.getInt("password"+constStr+"MinUpperCaseLetters") > 0) { %>
                                - <iwcm:text key="logon.change_password.count_of_upper_case" param1='<%=Constants.getString("password"+constStr+"MinUpperCaseLetters")%>'/><br/>
                            <% } if (Constants.getInt("password"+constStr+"MinCountOfSpecialSigns") > 0) { %>
                                - <iwcm:text key="logon.change_password.count_of_special_sign" param1='<%=Constants.getString("password"+constStr+"MinCountOfSpecialSigns")%>'/><br/>
                            <% } %>
                                - <iwcm:text key="logon.change_password.used_in_history2"/><br/>
                        </p>
                        <logic:present name="errors">
                            <span>
                                <iwcm:text key="user.form.errors"/>
                            </span>
                            <ul>
                                <li>${errors}</li>
                            </ul>
                        </logic:present>
                    </div>
                <div class="login_content">
                    <form:form action="changePassword" method="post" modelAttribute="userForm">
                        <c:if test="${empty param.auth}">
                            <div class="form-group">
                                <label class="control-label"><iwcm:text key="logon.old_password"/>:</label>
                                <form:password path="password" disabled="true" showPassword="true" maxlength="64" size="16" cssClass="form-control" autocomplete="false"/>
                                <form:hidden path="password"/>
                            </div>
                        </c:if>
                        <c:if test="${not empty param.auth}">
                            <div class="form-group">
                                <label><iwcm:text key="logon.change_password.select_login"/>:</label>
                                <form:select path="selectedLogin" cssClass="form-control">
                                    <c:forEach var="login" items="${userForm.login}">
                                        <form:option value="${login}">${login}</form:option>
                                    </c:forEach>
                                </form:select>
                            </div>
                        </c:if>
                        <div class="form-group">
                            <label class="control-label"><iwcm:text key="logon.new_password"/>:</label>
                            <form:password path="newPassword" maxlength="64" size="16" cssClass="form-control"/>
                            <div class="password-strength-info"></div>
                        </div>
                        <div class="form-group">
                            <label class="control-label"><iwcm:text key="logon.retype_new_password"/>:</label>
                            <form:password path="retypeNewPassword" maxlength="64" size="16" cssClass="form-control"/>
                        </div>
                        <div class="form-actions text-right">
                            <button type="submit" name="login-submit" id="login-submit" class="btn blue"><iwcm:text key="button.submit"/> <i class="m-icon-swapright m-icon-white"></i></button>
                            <input type="hidden" name="language" value="<%=org.apache.struts.util.ResponseUtils.filter(lng)%>"/>
                            <form:hidden path="login"/>
                            <form:hidden path="auth"/>
                            <%--<form:button name="login-submit" value="Submit">Submit</form:button>--%>
                        </div>
                    </form:form>
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

        let $element = $("#newPassword");
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
    });


</script>


</html>
