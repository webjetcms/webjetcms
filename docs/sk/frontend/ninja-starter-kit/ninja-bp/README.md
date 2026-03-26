# Základný kód

HTML kód je rozdelený do viacerých súborov pre možnosť vloženia spoločného kódu (napr. kódu hlavičky v `head.jsp`) do rozdielnych typov stránok (`home-page.jsp/sub-page.jsp`).

<!-- tabs:start -->

#### ** home-page.jsp **
```html
<!doctype html>
<html class="no-js pg-homepage" <%@ include file="includes/html-attributes.jsp" %>>
    <head>
        <%@ include file="includes/head.jsp" %>
    </head>
    <body>
        <div class="ly-page-wrapper">

            <%@ include file="includes/blind-friendly-panel.jsp" %>

            <div class="container">

                <header class="ly-header">
                    <%@ include file="includes/header.jsp" %>
                </header><!--/.ly-header-->

                <main class="ly-content-wrapper">

                    <%@ include file="includes/breadcrumb.jsp" %>

                    <div class="ly-content">

                        <%@ include file="includes/browser-support.jsp" %>

                        <div class="row">
                            <div class="col-md-8">

                                <article class="ly-article" id="blindBlock-article">
                                    <h1><iwcm:write name="doc_title"/></h1>
                                    <iwcm:write name="doc_data"/>
                                </article><!--/.ly-article-->

                                </div><!--/.col-md-8-->
                                <div class="col-md-4">

                                    <aside class="ly-sidebar" id="blindBlock-sidebar">
                                        <%@ include file="includes/sidebar.jsp" %>
                                    </aside><!--/.ly-sidebar-->

                                </div><!--/.col-md-4-->
                            </div><!--/.row-->

                    </div><!--/.ly-content-->

                </main><!--/.ly-content-wrapper-->

                <footer class="ly-footer" id="blindBlock-footer">
                    <%@ include file="includes/footer.jsp" %>
                </footer><!--/.ly-footer-->

            </div><!--/.container-->

        </div> <!--/.ly-page-wrapper-->

    </body>
</html>
```

#### ** sidebar.jsp **
```html
<iwcm:write name="doc_right_menu"/>
```

#### ** html-attributes.jsp **
```html
lang="${ninja.temp.lngIso}" data-browser-name="${ninja.userAgent.browserName}" data-browser-version="${ninja.userAgent.browserVersion}" data-device-type="${ninja.userAgent.deviceType}" data-device-os="${ninja.userAgent.deviceOS}" data-ab-variant="${ninja.abVariant}"
```

#### ** header.jsp **
```html
<c:if test="${ninja.userAgent.blind}">
    <a href="${ninja.page.urlPath}?forceBrowserDetector=blind">Textová verzia stránky</a>
</c:if>
<iwcm:write name="doc_header"/>
<nav class="md-navigation" id="blindBlock-navigation">
    <iwcm:write name="doc_menu"/>
</nav><!--/.md-navigation-->
```

#### ** head.jsp **
```html
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="${ninja.temp.charset}">
<meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
<meta http-equiv="Content-type" content="text/html;charset=${ninja.temp.charset}" />

<title>${ninja.page.doc.title} | ${ninja.temp.group.author}</title>

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
<link rel="canonical" href="${ninja.page.canonical}" />

<%-- <% if (ninja.amp.enabled) { %> --%>
<c:if test="${requestScope.doc_temp_name == 'Blog template'}">
    <link rel="amphtml" href="${ninja.page.url}?forceBrowserDetector=amp" />
</c:if>
<%-- <% } %> --%>

${ninja.temp.insertTouchIconsHtml}
<link rel="icon" href="${ninja.temp.basePathImg}favicon.ico" type="image/x-icon" />

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
            <iwcm:write name="base_css_link"/>
            <iwcm:write name="css_link"/>
        </iwcm:combine>
    </c:otherwise>
</c:choose>

<iwcm:combine type="js" set="">
    ${ninja.temp.basePathJs}plugins/jquery.min.js
    ${ninja.temp.basePathJs}plugins/jquery.cookie.js
    ${ninja.temp.basePathJs}plugins/modernizr-custom.js
    ${ninja.temp.basePathJs}plugins/bootstrap.bundle.min.js
    ${ninja.temp.basePathJs}global-functions.min.js
    ${ninja.temp.basePathJs}ninja.min.js
    ${ninja.webjet.pageFunctionsPath}
</iwcm:combine>

${ninja.webjet.insertJqueryFake}
<iwcm:write name="group_htmlhead_recursive"/>
<iwcm:write name="html_head"/>
```

#### ** debug-info.jsp **
```html
<c:if test="${ninja.debug}">
<div class="md-debug-info">
    <div class="debug-table">
        <div class="debug-table-row"><div class="debug-table-cell">${ninja.temp.lngIso} ${ninja.temp.charset} ${ninja.webjet.generatedTime}</div></div>
        <div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlDomain}</div></div>
        <div class="debug-table-row"><div class="debug-table-cell">${ninja.page.urlPath}</div></div>
    </div>
    <div class="debug-table">
        <c:forEach items="${ninja.page.urlParameters}" var="parameter">
        <div class="debug-table-row">
            <div class="debug-table-cell">${parameter.key}</div>
            <div class="debug-table-cell">
                <c:forEach items="${parameter.value}" var="value">
                    <span>${value}</span>
                </c:forEach>
            </div>
        </div>
        </c:forEach>
    </div>
    <div class="debug-table">
        <div class="debug-table-row">
            <div class="debug-table-cell">
                ${ninja.userAgent.browserName} ${ninja.userAgent.browserVersion}<br>
                    ${ninja.userAgent.deviceType}<br>
                ${ninja.userAgent.deviceOS}
            </div>
        </div>
        <div class="debug-table-row">
            <div class="debug-table-cell html-class-list">no-js</div>
        </div>
        <div class="debug-table-row">
            <div class="debug-table-cell window-info">
                <strong class="device-type-from-css"></strong>&nbsp;
                <strong class="device-resize-w-info">false</strong>&nbsp;/&nbsp;<strong class="device-resize-h-info">false</strong>&nbsp;
                <strong class="device-size"></strong><strong class="device-resize-new-size"></strong>
            </div>
        </div>
    </div>
</div>
</c:if>
```

#### ** browser-support.jsp **
```html
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
```

#### ** breadcrumb.jsp **
```html
<div class="md-breadcrumb" id="blindBlock-breadcrumb">
    <iwcm:write name="navbar"/>
</div><!--/.md-breadcrump-->
```

#### ** blind-friendly-panel.jsp **
```html
<c:if test="${ninja.userAgent.blind}">
    <div id="blindBlock-top">
        <address>Aktuálna stránka: <a href="${ninja.page.url}">${ninja.page.doc.title} | ${ninja.temp.group.author}</a></address>
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
```


<!-- tabs:end -->

## Konfiguračný súbor

V priečinku `/templates/.../` je možné nastaviť nasledovné vlastnosti:

```properties
ninjaDebug=false
basePathSuffix=
installName=
templateFolderName=
defaultSeoImage=
minBrowserVersion.MSIE=9
minBrowserVersion.CHROME=38
minBrowserVersion.SAFARI=6
minBrowserVersion.FIREFOX=45
minBrowserVersion.EDGE=12
minBrowserVersion.ANDROID_BROWSER=4
```

všetky tieto vlastnosti je pri zobrazení stránky možné ovplyvniť URL parametrom alebo `request` atribútom s rovnakým menom.