<%@ page pageEncoding="utf-8" %><%@ page import="sk.iway.iwcm.Cache" %><%@ page import="sk.iway.iwcm.Tools" %><%

sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");

Object spamProtectionDisable = request.getAttribute("spamProtectionDisable");
request.setAttribute("spamProtectionDisable", "1");

String key = Tools.getRequestParameter(request, "key");
if (key != null && key.startsWith("fulltext_preview-"))
{
   Cache c = Cache.getInstance();
   String data = (String)c.getObject(key);
   if (Tools.isNotEmpty(data))
   {
       request.setAttribute("fulltext_preview", data);
   }
}

//System.out.println("fulltext_preview.jsp, data=");
//System.out.println(request.getAttribute("fulltext_preview"));

%><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"

%><iwcm:write name="fulltext_preview"/><%

if (spamProtectionDisable==null) request.removeAttribute("spamProtectionDisable");
%>
