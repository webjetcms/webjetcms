<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.iwcm.system.captcha.Captcha" %><%
String value = Tools.getRequestParameter(request, "v");
boolean isCorrect = false;

if (Tools.isNotEmpty(value))
{
	session.setAttribute("sessionId", Tools.getParameter(request, "capchaId"));
	isCorrect = Captcha.isReponseCorrect(request, value);
}

System.out.println("catpcha_ajax.jsp, value="+value+", correct="+isCorrect);

if (isCorrect==false)
{
	response.setStatus(HttpServletResponse.SC_FORBIDDEN);
	out.print("ERR");
	return;
}
out.print("OK");
%>
