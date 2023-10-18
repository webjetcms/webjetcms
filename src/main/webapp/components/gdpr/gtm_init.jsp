<%
    sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*" %><%@ page import="sk.iway.iwcm.*" %>
        <!-- Inicializácia DataLayer a východiskový stav súhlasov -->
        <script type="text/javascript">
            window.dataLayer = window.dataLayer || [];
            function gtagGetConsentJson(cookieValue) {
               var gdprCookieClassifications = [<%
               String[] gdprCookieClassifications = Tools.getTokens(Constants.getString("gdprCookieClassifications"), ",");
               boolean isFirst = true;
               for (String classification : gdprCookieClassifications) {
                  if (isFirst==true) {
                     isFirst = false;
                  } else {
                     out.print(",");
                  }
                  out.print("'"+classification+"'");
               }
               %>];
               var json = {};
               var enabled = false;
               for (var classification of gdprCookieClassifications) {
                  var gtmName = classification;
                  if ("marketingove"===classification) gtmName = "ad_storage";
                  else if ("statisticke"===classification) gtmName = "analytics_storage";

                  enabled = cookieValue.indexOf(classification)!=-1;

                  json[gtmName] = enabled;
               }
               //console.log("json=", json);
               return json;
            }
            function gtag(){dataLayer.push(arguments)};
            gtag('consent', 'default', gtagGetConsentJson('<%=org.apache.struts.util.ResponseUtils.filter(Tools.getCookieValue(request.getCookies(), "enableCookieCategory", ""))%>'));
        </script>