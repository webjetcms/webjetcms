# Značky WebJET CMS

V šablónach môžete používať špeciálne značky WebJET CMS.

## Štandardné atribúty

Zoznam dostupných atribútov pri zobrazení web stránky nájdete v [samostatnej kapitole](webjet-objects.md).

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
<body data-th-class="${docDetails.fieldA}">
<meta name="author" data-th-if="${!#strings.isEmpty(ninja.temp.group.author)}" data-th-content="${ninja.temp.group.author}" />
<link data-th-href="${ninja.page.url}" rel="canonical" />
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>

<article data-iwcm-write="doc_data"/>
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Atribúty začínajúce na ```data-th-XXX``` sa vykonajú a nahradia ```XXX``` atribút hodnotou zadaného kódu. Pri prototypovaní bez spusteného WebJET CMS môžete nastaviť atribút na predvolenú hodnotu a ten pri spustení cez WebJET nahradiť pomocou ```data-th-``` atribútu:

```html
<!-- content atribut sa pri zobrazeni cez WebJET nahradi za data-th-content hodnotu -->
<meta property="og:title" content="WebJET CMS" data-th-content='${ninja.page.seoTitle}'/>
<!-- class atribut sa pri zobrazeni cez WebJET nahradi za data-th-class hodnotu -->
<body class="subpage" data-th-class="${docDetails.fieldA}">
```

## Vykonanie INCLUDE značky

Objekty web stránky môžu obsahovať značku ```!INCLUDE(...)!```, pre jej vykonanie nie je možné použiť štandardný atribút ```data-th-text``` ale je potrebné použiť špeciálny atribút ```data-iwcm-write```. Vykonať je možné buď [atribút stránky](webjet-objects.md) alebo priamo zadaný ```!INCLUDE(...)!``` príkaz.

Vykonanie **môže** odstrániť obaľovací ```div``` element. Správanie je nasledovné:

- ak názov atribútu začína na ```doc_``` (napr. ```doc_data```) obaľovací ```div``` element **sa zachová** (pre lepšiu kompatibilitu pri prototypovaní a možnosti nastavenia atribútov ako ```class``` a podobne)
- ak hodnota atribútu ```data-iwcm-remove``` je ```false``` alebo NIE JE ```tag``` obaľovací element **sa zachová**
- v ostatných prípadoch sa obaľovací ```div``` element **odstráni** a do stránky sa vloží kód z WebJETu

```html
<!-- zacina na doc_ standardne by header element zostal, ale nastavenim data-iwcm-remove="tag" header element odstranime -->
<header data-iwcm-write="doc_head" data-iwcm-remove="tag" />
<!-- zacina na doc_ standardne sa article element zachova -->
<article data-iwcm-write="doc_data" class="page-body"/>
<!-- NEzacina na doc_ standardne sa div element odstrani -->
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Použitie ```data-iwcm-remove``` zjednodušene z pohľadu HTML kódu vykonáva pri nastavení ```true``` volanie ```element[0].outerHTML=data``` (čiže nahradí sa aj tag na ktorom je to nastavené), inak vykoná ```element.html(data)```, čiže nahradí vnútro tag-u.

**Zároveň odporúčame** využiť vlastnosť značky ```data-iwcm-write```, ktorá **odstraňuje telo**, ```div``` značky. Môžete tak **efektívne prototypovať na úrovni html bez spustenia šablóny vo WebJETe**:

```html
<article data-iwcm-write="doc_data">
    <!-- content - toto je len ukazka pre html sablonu, vo WebJETe sa toto cele nahradi za obsah stranky -->
    <div class="container">
        <div class="row">
            <div class="col-md-12">
                <p>Lorem ipsum</p>
            </div>
        </div>
    </div>
</article>
```

čo znamená, že pokiaľ zobrazíte priamo html šablónu v prehliadači budete vidieť ```<article>Lorem ipsum</article>```. Keď sa šablóna vykoná vo WebJETe nahradí sa všetko vnútri ```<article data-iwcm-write="doc_data">``` za html kód web stránky (vrátane obalovačov ```<div class="container">....```).

## Kombinovanie súborov

Pre kombinovanie JavaScript a CSS súborov je možné použiť atribút ```data-iwcm-combine```. Ten automaticky podľa prípony rozdelí súbory na JavaScript a CSS a vloží dva samostatné odkazy na ```combine.jsp```. Môžete použiť ľubovoľný tag, v príklade je to ```combine```, pri zobrazení sa odstráni.

```html
<combine
basePath='|${ninja.temp.basePath}assets/|',
data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
${ninja.webjet.pageFunctionsPath}"
>
    <link href="css/ninja.min.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery-3.6.0.min.js" type="text/javascript"></script>
    <script src="js/jquery.cookie.js" type="text/javascript"></script>
    <script src="js/bootstrap.bundle.min.js" type="text/javascript"></script>
    <script src="js/global-functions.js" type="text/javascript"></script>
    <script src="js/ninja.min.js" type="text/javascript"></script>
</combine>
```

prípadne ako ```PUG``` zápis:

```html
combine(
basePath='|${ninja.temp.basePath}dist/|',
data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}\n"+
"${ninja.webjet.pageFunctionsPath}"
)
    <link href="css/ninja.min.css" rel="stylesheet" type="text/css"/>
    <script src="js/jquery-3.6.0.min.js" type="text/javascript"></script>
    <script src="js/jquery.cookie.js" type="text/javascript"></script>
    <script src="js/bootstrap.bundle.min.js" type="text/javascript"></script>
    <script src="js/global-functions.js" type="text/javascript"></script>
    <script src="js/ninja.min.js" type="text/javascript"></script>
```

Všimnite si, že v tele ```combine``` sú vložené štandardné ```link``` a ```script``` značky. Tieto sa spracujú a použijú sa do kombinovaného zápisu. Výhoda je v tom, že môžete mať zobrazený priamo ```.html``` súbor v prehliadači a uvedené skripty a CSS súbory sa štandardne vložia do stránky. Aj bez vykonania stránky vo WebJETe je funkčná v prehliadači.

Kombinácia najskôr vloží skripty a CSS z tela a následne doplní tie definované v ```data-iwcm-combine``` atribúte. Tam vkladajte len špecifické skripty a CSS, ktoré sa používajú len pri spracovaní cez WebJET CMS.

## Vkladanie skriptov

Pre vkladanie skriptov z aplikácie skripty existuje špeciálna značka ```data-iwcm-script```. Ako meno dostáva pozíciu skriptu (napr. ```header```) podľa definície v aplikácii. Značka používa tag ```div```, ten sa ale pri zobrazení odstráni.

```html
<div data-iwcm-script="header"/>
```

## Vkladanie jQuery

WebJET na zobrazenie stránky typicky potrebuje vloženú knižnicu jQuery z ```/components/_common/javascript/jquery.min.js``` a súbory ```/components/_common/javascript/page_functions.js.jsp``` a ```/components/form/check_form.css```.

Pri použití npm ale môžete vkladať jQuery do ```ninja.js``` a teda nechcete, aby knižnicu duplicitne vložil WebJET pri zobrazení objektu ```doc_data```. To je možné ovplyvniť nasledovnými možnosťami:

- Ak ```data-iwcm-combine``` obsahuje ```${ninja.webjet.pageFunctionsPath}``` predpokladá sa, že jQuery je vložené cez napr. ```ninja.js``` a už sa automaticky nevloží.
- Pri použití ```data-iwcm-write="doc_data"``` je možné pridať atribút ```data-iwcm-jquery``` s nasledovnými hodnotami:
  - ```false``` - jQuery už je vložené, nechceme ho automaticky vložiť WebJETom
  - ```true``` - jQuery sa vkladá automaticky, kontroluje sa ale duplicita a vloží sa len ak doteraz nebolo WebJETom vložené (vloži sa aj ```page_functions.js.jsp``` a ```check_form.css```)
  - ```force``` - aj ak si WebJET myslí, že je jQuery vložené (napr. z ```data-iwcm-combine```) vynúti sa jeho znova vloženie (vloži sa aj ```page_functions.js.jsp``` a ```check_form.css```)

Odporúčame vkladať jQuery cez NPM modul do ninja.js a následne použiť ```combine``` s ```${ninja.webjet.pageFunctionsPath}``` pre oznámenie WebJETu, že jQuery je vložené:

```html
<combine
    basePath="|${ninja.temp.basePath}|"
    data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
    ${ninja.webjet.pageFunctionsPath}"
>
    <script src="js/ninja.js" type="text/javascript"></script>
</combine>
```

CSS pre ```/components/form/check_form.css``` musíte vložiť do vášho scss (obsah si stiahnite v prehliadači z uvedenej URL adresy). Odporúčame ho vložiť do ```5-modules/md-checkform.scss```.