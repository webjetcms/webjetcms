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
               var gtmName = "";
               for (var classification of gdprCookieClassifications) {
                  gtmName = classification;
                  enabled = cookieValue.indexOf(classification)!=-1;

                  if ("marketingove"===classification) gtmName = "ad_storage";
                  else if ("statisticke"===classification) gtmName = "analytics_storage";

                  enabled = cookieValue.indexOf(classification)!=-1;

                  json[gtmName] = enabled?'granted':'denied';

                  if ("ad_storage"===gtmName) {
                     json['ad_user_data'] = json[gtmName];
                     json['ad_personalization'] = json[gtmName];
                  }
               }
               //console.log("json=", json);
               return json;
            }
            function gtag(){dataLayer.push(arguments)};
            gtag('consent', 'default', gtagGetConsentJson('nutne'));
            <% if (Tools.isNotEmpty(Tools.getCookieValue(request.getCookies(), "enableCookieCategory", ""))) { %>
               gtag('consent', 'update', gtagGetConsentJson('<%=org.apache.struts.util.ResponseUtils.filter(Tools.getCookieValue(request.getCookies(), "enableCookieCategory", ""))%>'));
            <% } %>
        </script>