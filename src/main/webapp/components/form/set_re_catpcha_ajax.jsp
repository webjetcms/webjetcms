<%@page import="sk.iway.iwcm.system.captcha.Captcha"%><%@page
        import="sk.iway.iwcm.Tools"%><%@ page
        import="sk.iway.iwcm.Constants" %><%

    //po spravnom vyplneni captchy sa z modulu re_captcha.jsp
    session.setAttribute("sessionId", Tools.getParameter(request, "capchaId"));
    if ("internal".equalsIgnoreCase(Constants.getString("captchaType"))) {
        if( Captcha.validateResponse(request, "", null))
            out.print("OK");
        else
            out.print("ERR");
    } else if ("invisible".equalsIgnoreCase(Constants.getString("captchaType")) || "reCaptcha".equalsIgnoreCase(Constants.getString("captchaType"))) {
        out.print("OK");
    }
    %>