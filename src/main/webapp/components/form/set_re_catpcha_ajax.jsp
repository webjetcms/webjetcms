<%@page import="sk.iway.iwcm.system.captcha.Captcha"%><%@ page
        import="sk.iway.iwcm.Constants" %><%

    if ("internal".equalsIgnoreCase(Constants.getString("captchaType"))) {
        if( Captcha.validateResponse(request, "", null))
            out.print("OK");
        else
            out.print("ERR");
    } else if ("invisible".equalsIgnoreCase(Constants.getString("captchaType")) || "reCaptcha".equalsIgnoreCase(Constants.getString("captchaType"))) {
        out.print("OK");
    }
    %>
