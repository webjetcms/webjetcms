# Šablony JSP

Psaní šablon v [Stránky serveru Java](https://www.baeldung.com/jsp) je starší způsob navrhování webových stránek. Hlavní nevýhodou ve srovnání s [Thymeleaf](../thymeleaf/README.md) nelze zobrazit bez spuštěného WebJET CMS, pro nové projekty vždy používejte Thymeleaf.

Následující příklad ilustruje vytvoření základní šablony, která se skládá ze záhlaví, navigačního panelu, nabídky, samotného dokumentu a zápatí. Soubor je `/templates/tmp_generic.jsp`.

Záhlaví HTML nastavuje `meta tag Content-type` na adrese `text/html`, zatímco kódování znaků není na stránce pevně nastaveno, ale je získáno ze souboru `request` objekt podle nastavení serveru.

Do stránky je načten styl CSS `/css/page.css` a otestuje, zda je v šabloně nastaven další styl CSS, a pokud ano, vloží se také.

Použité předměty jsou prakticky stejné jako v případě [Thymeleaf](../thymeleaf/webjet-objects.md), ale použitý formát je `<iwcm:write name="..."/>`. Značka `iwcm:write` vypíše zadaný atribut uložený v `HttpServletRequest` a současně provede značku `!INCLUDE(...)!`, tj. provede aplikaci. Značka `iway:request` pouze vloží atribut bez spuštění aplikace. Tag `iwcm:combine` slouží ke spojení (konkatenaci) více souborů do jednoho požadavku HTTP.

```jsp
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="windows-1250" %>
<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<HTML>
<HEAD>
<TITLE><iwcm:write name="doc_title"/></TITLE>
   <meta http-equiv="Content-type" content="text/html;charset=<%=(String)request.getAttribute("SetCharacterEncodingFilter.encoding")%>">
   <link rel="stylesheet" href="<iway:request name="base_css_link"/>" type="text/css">
   <c:if test="${not empty css_link}">
      <link rel="stylesheet" type="text/css" href="<iway:request name="css_link"/>">
   </c:if>
   <iwcm:combine type="js" set="">
        ${ninja.temp.basePathJs}plugins/jquery.min.js
        ${ninja.temp.basePathJs}plugins/bootstrap.bundle.min.js
        ${ninja.temp.basePathJs}ninja.min.js
        ${ninja.webjet.pageFunctionsPath}
    </iwcm:combine>
</HEAD>

<body>

<iwcm:write name="doc_header"/><br>

<b>Navigácia: </b><iwcm:write name="navbar"/>

<table width="770" border="0" cellspacing="0" cellpadding="0" height="300">
   <tr>
      <td align="left" valign="top">
         <iwcm:write name="doc_menu"/>
      </td>
      <td>
         <iwcm:write name="doc_data"/>
      </td>
   </tr>
</table>

<iwcm:write name="doc_footer"/>

</body>

<iwcm:write name="after_body"/>

</html>
```
