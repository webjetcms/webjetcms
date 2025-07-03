<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>

<c:if test="${ninja.userAgent.blind}">
    <a href="${ninja.page.urlPath}?forceBrowserDetector=blind">Textová verzia stránky</a>
</c:if>
<iwcm:write name="doc_header"/>