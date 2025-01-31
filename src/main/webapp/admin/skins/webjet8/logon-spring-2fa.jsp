<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page pageEncoding="utf-8"
         import="sk.iway.iwcm.*"
%>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean"
%><%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html"
%><%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic"
%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"
%><%
String brandSuffix = InitServlet.getBrandSuffix();
%>
<!DOCTYPE html>
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
    <link href="/admin/skins/webjet8/assets/admin/pages/css/login-soft.css" rel="stylesheet" type="text/css"/>

    <!-- END THEME STYLES -->
    <link rel="shortcut icon" href="/admin/skins/webjet8/assets/global/img/wj/favicon-cms.ico"/>

    <iwcm:combine type="js" set="adminJqueryJs" />

    <script type="text/javascript" src="/admin/scripts/qrcode.js"></script>

    <script type="text/javascript">
        <!--
        if (parent.frames.length > 0){
            parent.location.href = location.href
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
    String constStr = "User";
    if(user != null && user.isAdmin()){
        constStr = "Admin";
    }
%>
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
                        <li><iwcm:text key="admin.logon.2fa.wrongCode"/></li>
                    </ul>
                </div>
            </logic:present>
            <div class="login_content">
                <div class="form-group">
                    <form:form method="post" name="logonForm" modelAttribute="userForm">
                        <logic:present name="QRURL" scope="session">
                            <div class="form-group">
                                <label class="control-label"><iwcm:text key="user.gauth.instructions"/></label>
                                <div id="qrImage"></div>
                                <p>
                                    <iwcm:text key='user.gauth.instructions2'/> <%=session.getAttribute("token")%>
                                </p>
                                <label class="control-label"><iwcm:text key="user.gauth.enterCodeAfterSetup"/></label>
                            </div>
                        </logic:present>
                        <logic:notPresent name="QRURL" scope="session">
                            <div class="form-group">
                                <label class="control-label"><iwcm:text key="user.gauth.label"/></label>
                            </div>
                        </logic:notPresent>
                        <div class="form-group">
                            <label class="control-label visible-ie8 visible-ie9"><iwcm:text key="logon.password"/>:</label>
                            <div class="input-icon">
                                <i class="ti ti-key"></i>
                                <input type="text" name="token" size="16" maxlength="64" class="form-control placeholder-no-fix" autocomplete="off"/>
                            </div>
                        </div>
                        <div class="form-actions">
                            <button type="button" class="btn btn-secondary" onclick="window.location.href='/logoff.do?forward=/admin/index.jsp';"><iwcm:text key="inlineToolbar.option.settings.logout"/></button>
                            <button type="submit" name="login-submit" id="login-submit" class="btn btn-primary pull-right"><iwcm:text key="button.submit"/></button>
                        </div>
                    </form:form>
                </div>
            </div>
        </div>
    </div>
</div>
<!-- LOGIN ERROR Ende -->


<!-- LOGIN ALL Ende -->

</body>

<iwcm:combine type="js" set="adminStandardJs" />
<script src="/components/_common/javascript/password_strenght.js.jsp" type="text/javascript"></script>

<script>
    jQuery(document).ready(function() {
        document.logonForm.token.focus();

        <logic:present name="QRURL" scope="session">
            var qrcode = new QRCode(document.getElementById("qrImage"), {
                text: "<%=session.getAttribute("QRURL")%>",
                width: 300,
                height: 300,
                colorDark : "#000000",
                colorLight : "#ffffff",
                correctLevel : QRCode.CorrectLevel.H
            });
        </logic:present>
    });

</script>


</html>
