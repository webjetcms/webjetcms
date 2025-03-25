# JSP šablóny

Písanie šablón v [Java Server Pages](https://www.baeldung.com/jsp) je starší spôsob prípravy dizajnu web stránok. Hlavná nevýhoda oproti [Thymeleaf](../thymeleaf/README.md) je nemožnosť zobrazenia bez spusteného WebJET CMS, pre nové projekty vždy používajte Thymeleaf.

Nasledovný príklad ilustruje vytvorenie základnej šablóny pozostávajúcej z hlavičky, navigačnej lišty, menu, samotného dokumentu a pätičky. Súbor je `/templates/tmp_generic.jsp`.

V hlavičke HTML kódu sa nastavuje `meta tag Content-type` na `text/html`, pričom kódovanie znakov nie je v stránke zadané napevno, ale sa získa z `request` objektu podľa nastavenia servera.

Do stránky sa napevno načítava CSS štýl `/css/page.css` a testuje sa, či je v šablóne nastavený dodatočný CSS štýl, ak áno, vloží sa aj ten.

Použité objekty sú prakticky rovnaké ako pre [Thymeleaf](../thymeleaf/webjet-objects.md), ale používa sa formát `<iwcm:write name="..."/>`. Značka `iwcm:write` vypíše zadaný atribút uložený v `HttpServletRequest` a zároveň vykoná značku `!INCLUDE(...)!`, čiže vykoná aplikáciu. Značka `iway:request` len vloží atribút bez vykonania aplikácie. Značka `iwcm:combine` sa používa na kombinovanie (spájanie) viacerých súborov do jednej HTTP požiadavky.

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