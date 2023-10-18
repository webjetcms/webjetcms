<%@page import="sk.iway.iwcm.tags.WriteTag"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.io.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="/WEB-INF/struts-bean.tld" prefix="bean" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>
<%
//stranka pre includnutie blogu
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String style = pageParams.getValue("style", "01");

WriteTag.setInlineComponentEditTextKey("components.news.editMenu", request);

String filePath = "/components/app-social_icon/social_icon-" + style + ".jsp";
IwcmFile f = new IwcmFile(Tools.getRealPath(filePath));
if (f.exists() && f.isFile())
{
	pageContext.include("social_icon-"+style+".jsp");
}
%>