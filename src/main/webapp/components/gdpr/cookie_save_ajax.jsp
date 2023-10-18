<%
	sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*"%><%
String referer = request.getHeader("Referer");
String categories = Tools.getParameter(request, "categories");
if (Tools.isNotEmpty(referer) && Tools.isNotEmpty(categories)) {
   if ("nutne".equals(categories)) {
      sk.iway.iwcm.Adminlog.addAnonymously(sk.iway.iwcm.Adminlog.TYPE_COOKIE_REJECTED, "Cookies rejected\nreferer: "+referer, -1, -1);
   } else {
      categories = Tools.replace(categories, "_", ",");
      sk.iway.iwcm.Adminlog.add(sk.iway.iwcm.Adminlog.TYPE_COOKIE_ACCEPTED, "Cookies accepted\ncategories: "+categories+"\nreferer: "+referer, -1, -1);
   }
}
%>OK