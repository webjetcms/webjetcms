<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm" %>
<%@ page import="sk.iway.iwcm.editor.InlineEditor,sk.iway.iwcm.*,sk.iway.iwcm.doc.*" %>
<%@ page pageEncoding="utf-8" %>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="${ninja.temp.charset}">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<meta http-equiv="Content-type" content="text/html;charset=${ninja.temp.charset}" />

<title>${ninja.page.seoTitle} | ${ninja.temp.group.author}</title>

<meta name="description" content="${ninja.page.seoDescription}" />
<meta name="author" content="${ninja.temp.group.author}" />
<meta name="developer" content="${ninja.temp.group.developer}" />
<meta name="generator" content="${ninja.temp.group.generator}" />
<meta name="copyright" content="${ninja.temp.group.copyright}" />
<meta name="robots" content="${ninja.page.robots}" />
<meta property="og:title" content="${ninja.page.seoTitle}" />
<meta property="og:description" content="${ninja.page.seoDescription}" />
<meta property="og:url" content="${ninja.page.url}" />
<meta property="og:image" content="${ninja.page.urlDomain}${ninja.page.seoImage}" />
<meta property="og:site_name" content="${ninja.temp.group.siteName}" />
<meta property="og:type" content="website" />
<meta property="og:locale" content="${ninja.temp.lngIso}" />

<%--<link rel="alternate" hreflang="" href="" />--%>
<link rel="canonical" href="${ninja.page.url}" />

<% if (request.getSession().isNew() && PathFilter.getOrigPath(request).startsWith("/files")) { %><script>document.cookie = 'JSESSIONID=<%=session.getId()%>; path=/;';</script><% } %>

<link rel="icon" href="${ninja.temp.basePathImg}favicon.ico" type="image/x-icon" />

<%
String lng = PageLng.getUserLng(request);
pageContext.setAttribute("lng", lng);
PageParams pageParams = new PageParams(request);
DocDetails doc = (DocDetails)request.getAttribute("docDetails");
GroupsDB groupsDB = GroupsDB.getInstance();
String fieldA = groupsDB.getPropertyRecursive(doc != null ? doc.getGroupId() : -1, "fieldA");

    InlineEditor.setEditingMode(InlineEditor.EditingMode.pageBuilder, request);
%>

<c:choose>
    <c:when test="${ninja.userAgent.blind}">
        <iwcm:combine type="css" set="">
            ${ninja.temp.basePathCss}blind-friendly.min.css
        </iwcm:combine>
    </c:when>
    <c:otherwise>
        <iwcm:combine type="css" set="">
            ${ninja.temp.basePathCss}ninja.min.css
            ${ninja.temp.basePathCss}shame.css
        </iwcm:combine>
    </c:otherwise>
</c:choose>

<link href="/templates/aceintegration/jet/assets/fontawesome/css/fontawesome.min.css" rel="stylesheet" type="text/css">
<link href="/templates/aceintegration/jet/assets/fontawesome/css/solid.css" rel="stylesheet" type="text/css">

<iwcm:combine type="js" set="">
    /components/_common/javascript/jquery.min.js
    /components/_common/javascript/jquery.cookie.js
    ${ninja.temp.basePathJs}plugins/modernizr-custom.js
    ${ninja.temp.basePathJs}plugins/bootstrap.bundle.min.js
    ${ninja.temp.basePathJs}global-functions.min.js
    ${ninja.temp.basePathJs}plugins/aos.js
    ${ninja.temp.basePathJs}ninja.min.js
    ${ninja.webjet.pageFunctionsPath}
</iwcm:combine>

<iwcm:write>!INCLUDE(/components/gdpr/gtm_init.jsp)!</iwcm:write>

${ninja.webjet.insertJqueryFake}
<iwcm:write name="group_htmlhead_recursive"/>
<iwcm:write name="html_head"/>
<script>
    <!-- DATA LAYER -->
    <iwcm:notEmpty name="docDetails" property="fieldQ">
    dataLayer.push({'pageCategory': '<c:out value="${docDetails.fieldQ}" />'});
    </iwcm:notEmpty>
</script>

<iwcm:insertScript position="head"/>