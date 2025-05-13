# JSP templates

Writing templates in [Java Server Pages](https://www.baeldung.com/jsp) is an older way of designing web pages. The main disadvantage compared to [Thymeleaf](../thymeleaf/README.md) it is impossible to view without WebJET CMS running, always use Thymeleaf for new projects.

The following example illustrates the creation of a basic template consisting of a header, a navigation bar, a menu, the document itself, and a footer. The file is `/templates/tmp_generic.jsp`.

The HTML header is set `meta tag Content-type` at `text/html`, while the character encoding is not fixed in the page, but is obtained from `request` object according to the server settings.

CSS style is loaded into the page `/css/page.css` and tests if there is an additional CSS style set in the template, if so, it is inserted as well.

The objects used are practically the same as for [Thymeleaf](../thymeleaf/webjet-objects.md), but the format used is `<iwcm:write name="..."/>`. Brand `iwcm:write` prints the specified attribute stored in `HttpServletRequest` and at the same time executes the mark `!INCLUDE(...)!`, i.e. executes the application. Brand `iway:request` just inserts the attribute without executing the application. Tag `iwcm:combine` is used to combine (concatenate) multiple files into a single HTTP request.

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
