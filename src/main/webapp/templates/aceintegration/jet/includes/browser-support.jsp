<%@ page pageEncoding="utf-8" %>
<c:if test="${ninja.userAgent.browserOutdated}">
<div class="alert alert-warning md-browser-support md-browser-support--outdated" role="alert">
   Verzia v�ho prehliada�a nie je aktu�lna, str�nka sa nebude zobrazova� spr�vne.
</div>
</c:if>

<div class="alert alert-warning md-browser-support md-browser-support--cookie" role="alert" style="display: none">
    Nem�te povolen� pou�itie Cookie, webov� str�nka nebude fungova� spr�vne.<br> Niektor� �asti webu a aplik�cie nebud� dostupn�.
</div>

<div class="alert alert-warning md-browser-support md-browser-support--js" role="alert" style="display: none">
    Nem�te zapnut� Javascript, webov� str�nka nebude fungova� spr�vne.<br> Niektor� �asti webu a aplik�cie nebud� dostupn�.
</div>