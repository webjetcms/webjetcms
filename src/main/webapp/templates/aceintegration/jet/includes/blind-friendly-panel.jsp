<%@ page pageEncoding="utf-8" %>
<c:if test="${ninja.userAgent.blind}">
    <div id="blindBlock-top">
        <address>Aktuálna stránka: <a href="${ninja.page.url}">${ninja.page.title} | ${ninja.temp.group.author}</a></address>
        <p><strong>Zobrazuje sa optimalizovaná verzia stránky pre slabo vidiacich alebo špecializované či staršie prehliadače.</strong></p>
        <p><a href="${ninja.page.urlPath}?forceBrowserDetector=pc">Prejsť na grafickú verziu</a></p>
        <hr />
        <p><em>Začiatok stránky, titulka:</em></p>
        <p><a href="#blindBlock-next">Pokračuj v čítaní</a> alebo preskoč na <a href="#blindBlock-navigation">Hlavnú navigáciu</a>.</p>
        <div id="blindBlock-next">
            <p><em>Ďalšie možnosti:</em></p>
            <ul>
                <li><a href="#blindBlock-top">Začiatok stránky</a></li>
                <li><a href="#blindBlock-navigation">Hlavná navigácia</a></li>
                <li><a href="#blindBlock-breadcrumb">Cesta k stránke</a></li>
                <li><a href="#blindBlock-article">Obsah stránky</a></li>
                <li><a href="#blindBlock-sidebar">Bočný panel</a></li>
                <li><a href="#blindBlock-footer">Pätičkové informácie</a></li>
            </ul>
        </div>
    </div>
</c:if>