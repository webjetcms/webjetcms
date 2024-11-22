<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@
taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><%@
taglib uri="/WEB-INF/iway.tld" prefix="iway" %><%@
taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %><%@
taglib uri="/WEB-INF/struts-html.tld" prefix="html" %><%@
taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %><%@
taglib uri="/WEB-INF/displaytag.tld" prefix="displaytag" %><%

//vypise cas generovania stranky

String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
boolean hide = pageParams.getBooleanValue("hide", true);
boolean onlyForAdmin = pageParams.getBooleanValue("onlyForAdmin", false);

Identity user = (Identity)session.getAttribute(Constants.USER_KEY);
if (onlyForAdmin) {
	if (user == null || user.isAdmin()==false) return;
}

long now = System.currentTimeMillis();
Long start = (Long)request.getAttribute("pathFilet.requestStartTime");
if (start != null)
{
	long ms = now - start.longValue();
	if (hide)
	{
		out.print("<!-- generation time: ");
	}
	out.print(ms + " ms");
	if (hide)
	{
		out.println(" -->");
	}
}
%>