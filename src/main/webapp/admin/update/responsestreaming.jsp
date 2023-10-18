<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/plain");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %><iwcm:checkLogon admin="true" perms="modUpdate"/>
<h1>Test X-Accel-Buffering, call with URL parameter ?act=fix</h1><p><a href="?act=fix">Spustiť</a></p><%

if ("fix".equals(request.getParameter("act"))) {
   response.setHeader("X-Accel-Buffering", "no");

   for (int i=0; i<10; i++) {

      out.println("i="+Tools.formatDateTimeSeconds(Tools.getNow()));
      out.flush();
      try {
         Thread.sleep(1000);
      } catch (Exception ex) {

      }


   }
}

%>