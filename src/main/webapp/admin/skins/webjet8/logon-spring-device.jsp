<%@ page pageEncoding="utf-8" import="sk.iway.iwcm.InitServlet,sk.iway.iwcm.system.stripes.CSRF" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="iwcm" uri="/WEB-INF/iwcm.tld" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><iwcm:text key="admin.logon.device.title"/> - WebJET CMS</title>
    <iwcm:combine type="css" set="adminStandardCss" />
    <link href="/admin/skins/webjet8/assets/admin/pages/css/login-soft.css" rel="stylesheet" type="text/css">
</head>
<body id="login" class="login">
<div class="welcome-title">
    <h1><iwcm:text key="logon.welcome.title"/></h1>
    <h2><iwcm:text key="admin.logon.device.title"/></h2>
</div>
<div class="container">
    <div class="container-inner">
        <div class="content">
            <div class="form-group logo">
                <img src="/admin/skins/webjet8/assets/global/img/wj/logo-<%=InitServlet.getBrandSuffix()%>.png" alt="WebJET CMS">
            </div>
            <div class="login_content">
                <p><iwcm:text key="admin.logon.device.description"/></p>
                <p><iwcm:text key="admin.logon.device.email"/> <strong><c:out value="${deviceEmail}"/></strong></p>
                <c:if test="${not empty deviceMessage}">
                    <p role="status"><c:out value="${deviceMessage}"/></p>
                </c:if>
                <c:if test="${not empty deviceError}">
                    <div class="alert alert-danger" role="alert"><c:out value="${deviceError}"/></div>
                </c:if>
                <form method="post" action="<c:url value='/admin/logon/device/'/>" id="deviceVerificationForm">
                    <%=CSRF.getCsrfTokenInputFiled(session)%>
                    <div class="form-group">
                        <label for="deviceCode"><iwcm:text key="admin.dashboard.newDevice.codeLabel.js"/></label>
                        <input type="text" id="deviceCode" name="code" class="form-control" inputmode="numeric"
                               autocomplete="one-time-code" pattern="[0-9]{6}" maxlength="6" required autofocus>
                    </div>
                    <div class="form-group">
                        <button type="submit" name="action" value="verify" class="btn btn-primary"><iwcm:text key="admin.dashboard.newDevice.verifyCode.js"/></button>
                        <button type="submit" name="action" value="resend" class="btn btn-secondary" formnovalidate><iwcm:text key="admin.dashboard.newDevice.resendCode.js"/></button>
                    </div>
                    <div class="form-group">
                        <button type="submit" name="action" value="cancel" class="btn btn-secondary btn-as-link" formnovalidate><iwcm:text key="button.cancel"/></button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>
</body>
</html>
