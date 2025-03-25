# JSP šablony

Psaní šablon v [Java Server Pages](https://www.baeldung.com/jsp) je starší způsob přípravy designu web stránek. Hlavní nevýhoda oproti [Thymeleaf](../thymeleaf/README.md) je nemožnost zobrazení bez spuštěného WebJET CMS, pro nové projekty vždy používejte Thymeleaf.

Následující příklad ilustruje vytvoření základní šablony sestávající z hlavičky, navigační lišty, menu, samotného dokumentu a patičky. Soubor je `/templates/tmp_generic.jsp`.

V hlavičce HTML kódu se nastavuje `meta tag Content-type` na `text/html`, přičemž kódování znaků není ve stránce zadáno napevno, ale se získá z `request` objektu podle nastavení serveru.

Do stránky se napevno načítá CSS styl `/css/page.css` a testuje se, zda je v šabloně nastaven dodatečný CSS styl, pokud ano, vloží se i ten.

Použité objekty jsou prakticky stejné jako pro [Thymeleaf](../thymeleaf/webjet-objects.md), ale používá se formát `<iwcm:write name="..."/>`. Značka `iwcm:write` vypíše zadaný atribut uložený v `HttpServletRequest` a zároveň provede značku `!INCLUDE(...)!`, tedy provede aplikaci. Značka `iway:request` jen vloží atribut bez provedení aplikace. Značka `iwcm:combine` se používá ke kombinování (spojování) více souborů do jednoho HTTP požadavku.

```jsp
<%
sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
%><%@ page pageEncoding="utf-8" %>
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
