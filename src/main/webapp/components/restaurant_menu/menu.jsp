<%@page import="sk.iway.iwcm.tags.WriteTag"%><%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*,sk.iway.iwcm.io.*,java.io.*"%>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib uri="/WEB-INF/struts-logic.tld" prefix="logic" %>

<%
//stranka pre includnutie noviniek


String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);

PageParams pageParams = new PageParams(request);
String style = pageParams.getValue("style", "01");

WriteTag.setInlineComponentEditTextKey("components.restaurantMenu.editMenu", request);

if (style == null || style.indexOf("bootstrap")==-1)
{
	WriteTag.setInlineComponentEditTextKey("", request);
}
%>
<section>
<div class="restaurant-menu restaurant-menu-<%=style%>">

<%
String filePath = "/components/restaurant_menu/menu-" + style + ".jsp";
IwcmFile f = new IwcmFile(Tools.getRealPath(filePath));
if (f.exists() && f.isFile())
{
	pageContext.include("menu-" + style + ".jsp");
}
%>

</div>
</section>