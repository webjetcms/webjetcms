# JSP templates

Writing templates in [Java Server Pages](https://www.baeldung.com/jsp) is an older way of preparing web page designs. The main disadvantage compared to [Thymeleaf](../thymeleaf/README.md) is the inability to display without running WebJET CMS, always use Thymeleaf for new projects.

The following example illustrates the creation of a basic template consisting of a header, navigation bar, menu, document itself, and footer. The file is `/templates/tmp_generic.jsp`.

In the HTML code header, `meta tag Content-type` is set to `text/html`, while the character encoding is not fixed in the page, but is obtained from the `request` object according to the server settings.

The CSS style `/css/page.css` is permanently loaded into the page and it is tested whether an additional CSS style is set in the template, if so, it is inserted as well.

The objects used are practically the same as for [Thymeleaf](../thymeleaf/webjet-objects.md), but the format `<iwcm:write name="..."/>` is used. The `iwcm:write` tag prints the specified attribute stored in `HttpServletRequest` and also executes the `!INCLUDE(...)!` tag, i.e. executes the application. The `iway:request` tag only inserts the attribute without executing the application. The `iwcm:combine` tag is used to combine (join) multiple files into a single HTTP request.

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