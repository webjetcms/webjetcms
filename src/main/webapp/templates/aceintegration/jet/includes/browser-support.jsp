<%@ page pageEncoding="windows-1250" %>
<c:if test="${ninja.userAgent.browserOutdated}">
<div class="alert alert-warning md-browser-support md-browser-support--outdated" role="alert">
   Verzia vášho prehliadaèa nie je aktuálna, stránka sa nebude zobrazova správne.
</div>
</c:if>

<div class="alert alert-warning md-browser-support md-browser-support--cookie" role="alert" style="display: none">
    Nemáte povolené pouitie Cookie, webová stránka nebude fungova správne.<br> Niektoré èasti webu a aplikácie nebudú dostupné.
</div>

<div class="alert alert-warning md-browser-support md-browser-support--js" role="alert" style="display: none">
    Nemáte zapnutı Javascript, webová stránka nebude fungova správne.<br> Niektoré èasti webu a aplikácie nebudú dostupné.
</div>