<%@page import="sk.iway.iwcm.system.captcha.Captcha"%><%@
page import="sk.iway.iwcm.Tools"%>
<%//po spravnom vyplneni captchy sa z modulu re_captcha.jsp
session.setAttribute("sessionId", Tools.getParameter(request, "capchaId"));

//if( Captcha.validateResponse(request, "", null))
	out.print("OK");
//else
//	out.print("ERR");
%>