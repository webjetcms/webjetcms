<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page pageEncoding="utf-8"
         import="sk.iway.iwcm.*,sk.iway.iwcm.i18n.*"
%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"
%><%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"
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

<div class="welcome-title">
    <h1><c:out value="${title}"/></h1>
    <h2><c:out value="${subtitle}"/></h2>
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

            <div class="form-group logo">
                <img src="/admin/skins/webjet8/assets/global/img/wj/logo-<%=brandSuffix%>.png" alt="WebJET CMS" />
            </div>

            <iwcm:present name="errors">
                <div class="alert alert-danger">
                    <span>
                        <iwcm:text key="user.form.errors"/>
                    </span>
                    <ul>
                        <li><iwcm:text key="admin.logon.2fa.wrongCode"/></li>
                    </ul>
                </div>
            </iwcm:present>
            <div class="login_content">
                <div class="form-group">
                    <form:form method="post" name="logonForm" modelAttribute="userForm">
                        <iwcm:present name="QRURL" scope="session">
                            <div class="form-group">
                                <label class="control-label"><iwcm:text key="user.gauth.instructions"/></label>
                                <div class="qrImageWrapper">
                                    <div id="qrImage"></div>
                                </div>
                                <p>
                                    <iwcm:text key='user.gauth.instructions2'/> <%=session.getAttribute("token")%>
                                </p>
                                <label class="control-label"><iwcm:text key="user.gauth.enterCodeAfterSetup"/></label>
                            </div>
                        </iwcm:present>
                        <iwcm:notPresent name="QRURL" scope="session">
                            <div class="form-group">
                                <label class="control-label"><iwcm:text key="user.gauth.label"/></label>
                            </div>
                        </iwcm:notPresent>
                        <div class="form-group">
                            <label class="control-label visible-ie8 visible-ie9"><iwcm:text key="logon.password"/>:</label>
                            <div class="input-icon">
                                <i class="ti ti-key"></i>
                                <input type="text" name="token" size="16" maxlength="64" class="form-control placeholder-no-fix" autocomplete="off"/>
                            </div>
                        </div>
                        <div class="form-group">
                            <button type="submit" name="login-submit" id="login-submit" class="btn btn-primary"><iwcm:text key="button.submit"/><i class="ti ti-arrow-right"></i></button>
                            <button type="button" class="btn btn-secondary btn-as-link pull-right" onclick="window.location.href='/logoff.do?forward=/admin/index.jsp';"><iwcm:text key="inlineToolbar.option.settings.logout"/></button>
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

        <iwcm:present name="QRURL" scope="session">
            var qrcode = new QRCode(document.getElementById("qrImage"), {
                text: "<%=session.getAttribute("QRURL")%>",
                width: 250,
                height: 250,
                colorDark : "#000000",
                colorLight : "#ffffff",
                correctLevel : QRCode.CorrectLevel.H
            });
        </iwcm:present>
    });

</script>


</html>
