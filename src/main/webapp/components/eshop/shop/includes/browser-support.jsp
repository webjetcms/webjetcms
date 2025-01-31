<%@ page pageEncoding="utf-8" %>
<c:if test="${ninja.userAgent.browserOutdated}">
<div class="alert alert-warning md-browser-support md-browser-support--outdated" role="alert">
   Verzia vášho prehliadača nie je aktuálna, stránka sa nebude zobrazovať správne.
</div>
</c:if>

<div class="alert alert-warning md-browser-support md-browser-support--cookie" role="alert" style="display: none">
    Nemáte povolené použitie Cookie, webová stránka nebude fungovať správne.<br> Niektoré časti webu a aplikácie nebudú dostupné.
</div>

<div class="alert alert-warning md-browser-support md-browser-support--js" role="alert" style="display: none">
    Nemáte zapnutý Javascript, webová stránka nebude fungovať správne.<br> Niektoré časti webu a aplikácie nebudú dostupné.
</div>