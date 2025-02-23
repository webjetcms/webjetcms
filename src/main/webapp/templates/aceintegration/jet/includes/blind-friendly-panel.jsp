<%@ page pageEncoding="windows-1250" %>
<c:if test="${ninja.userAgent.blind}">
    <div id="blindBlock-top">
        <address>Aktuálna stránka: <a href="${ninja.page.url}">${ninja.page.title} | ${ninja.temp.group.author}</a></address>
        <p><strong>Zobrazuje sa optimalizovaná verzia stránky pre slabo vidiacich alebo špecializované èi staršie prehliadaèe.</strong></p>
        <p><a href="${ninja.page.urlPath}?forceBrowserDetector=pc">Prejs na grafickú verziu</a></p>
        <hr />
        <p><em>Zaèiatok stránky, titulka:</em></p>
        <p><a href="#blindBlock-next">Pokraèuj v èítaní</a> alebo preskoè na <a href="#blindBlock-navigation">Hlavnú navigáciu</a>.</p>
        <div id="blindBlock-next">
            <p><em>Ïalšie monosti:</em></p>
            <ul>
                <li><a href="#blindBlock-top">Zaèiatok stránky</a></li>
                <li><a href="#blindBlock-navigation">Hlavná navigácia</a></li>
                <li><a href="#blindBlock-breadcrumb">Cesta k stránke</a></li>
                <li><a href="#blindBlock-article">Obsah stránky</a></li>
                <li><a href="#blindBlock-sidebar">Boènı panel</a></li>
                <li><a href="#blindBlock-footer">Pätièkové informácie</a></li>
            </ul>
        </div>
    </div>
</c:if>