<%@ page pageEncoding="utf-8"  import="sk.iway.iwcm.*,sk.iway.iwcm.doc.*"%>

<c:if test="${ninja.userAgent.blind}">
    <a href="${ninja.page.urlPath}?forceBrowserDetector=blind">Textová verzia stránky</a>
</c:if>

<div class="container">
    <iwcm:write name="doc_header"/>
</div>

<div class="md-navigation" id="blindBlock-navigation">
    <div class="container">
        <iwcm:write name="doc_menu"/>
    </div>
</div><!--/.md-navigation-->