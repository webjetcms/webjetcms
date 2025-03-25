# Značky WebJET CMS

V šablonách můžete používat speciální značky WebJET CMS.

## Standardní atributy

Seznam dostupných atributů při zobrazení web stránky naleznete v [samostatné kapitole](webjet-objects.md).

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
<body data-th-class="${docDetails.fieldA}">
<meta name="author" data-th-if="${!#strings.isEmpty(ninja.temp.group.author)}" data-th-content="${ninja.temp.group.author}" />
<link data-th-href="${ninja.page.url}" rel="canonical" />
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>

<article data-iwcm-write="doc_data"/>
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Atributy začínající na `data-th-XXX` se provedou a nahradí `XXX` atribut hodnotou zadaného kódu. Při prototypování bez spuštěného WebJET CMS můžete nastavit atribut na výchozí hodnotu a ten při spuštění přes WebJET nahradit pomocí `data-th-` atributu:

```html
<!-- content atribut sa pri zobrazeni cez WebJET nahradi za data-th-content hodnotu -->
<meta property="og:title" content="WebJET CMS" data-th-content='${ninja.page.seoTitle}'/>
<!-- class atribut sa pri zobrazeni cez WebJET nahradi za data-th-class hodnotu -->
<body class="subpage" data-th-class="${docDetails.fieldA}">
```

## Provedení INCLUDE značky

Objekty web stránky mohou obsahovat značku `!INCLUDE(...)!`, pro její provedení nelze použít standardní atribut `data-th-text` ale je třeba použít speciální atribut `data-iwcm-write`. Provést je možné buď [atribut stránky](webjet-objects.md) nebo přímo zadaný `!INCLUDE(...)!` příkaz.

Provedení **může** odstranit obalovací `div` element. Chování je následující:
- pokud název atributu začíná na `doc_` (Např. `doc_data`) obalovací `div` element **se zachová** (pro lepší kompatibilitu při prototypování a možnosti nastavení atributů jako `class` a podobně)
- pokud hodnota atributu `data-iwcm-remove` je `false` nebo NENÍ `tag` obalovací element **se zachová**
- v ostatních případech se obalovací `div` element **odstraní** a do stránky se vloží kód z WebJETu

```html
<!-- zacina na doc_ standardne by header element zostal, ale nastavenim data-iwcm-remove="tag" header element odstranime -->
<header data-iwcm-write="doc_head" data-iwcm-remove="tag" />
<!-- zacina na doc_ standardne sa article element zachova -->
<article data-iwcm-write="doc_data" class="page-body"/>
<!-- NEzacina na doc_ standardne sa div element odstrani -->
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Použití `data-iwcm-remove` zjednodušeně z pohledu HTML kódu provádí při nastavení `true` volání `element[0].outerHTML=data` (čili nahradí se i tag na kterém je to nastaveno), jinak provede `element.html(data)`, neboli nahradí vnitřek tag-u.

**Zároveň doporučujeme** využít vlastnost značky `data-iwcm-write`, která **odstraňuje tělo**, `div` značky. Můžete tak **efektivně prototypovat na úrovni html bez spuštění šablony ve WebJETu**:

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

což znamená, že pokud zobrazíte přímo html šablonu v prohlížeči budete vidět `<article>Lorem ipsum</article>`. Když se šablona provede ve WebJETu nahradí se vše uvnitř `<article data-iwcm-write="doc_data">` za html kód web stránky (včetně obalovačů `<div class="container">....`).

## Kombinování souborů

Pro kombinování JavaScript a CSS souborů lze použít atribut `data-iwcm-combine`. Ten automaticky podle přípony rozdělí soubory na JavaScript a CSS a vloží dva samostatné odkazy na `combine.jsp`. Můžete použít libovolný tag, v příkladu je to `combine`, při zobrazení se odstraní.

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

případně jako `PUG` zápis:

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

Všimněte si, že v těle `combine` jsou vloženy standardní `link` a `script` značky. Tyto se zpracují a použijí se do kombinovaného zápisu. Výhoda je v tom, že můžete mít zobrazen přímo `.html` soubor v prohlížeči a uvedené skripty a CSS soubory se standardně vloží do stránky. I bez provedení stránky ve WebJETu je funkční v prohlížeči.

Kombinace nejprve vloží skripty a CSS z těla a následně doplní ty definované v `data-iwcm-combine` atribute. Tam vkládejte jen specifické skripty a CSS, které se používají pouze při zpracování přes WebJET CMS.

## Vkládání skriptů

Pro vkládání skriptů z aplikace skripty existuje speciální značka `data-iwcm-script`. Jako jméno dostává pozici skriptu (např. `header`) podle definice v aplikaci. Značka používá tag `div`, ten se ale při zobrazení odstraní.

```html
<div data-iwcm-script="header"/>
```

## Vkládání jQuery

WebJET k zobrazení stránky typicky potřebuje vloženou knihovnu jQuery z `/components/_common/javascript/jquery.min.js` a soubory `/components/_common/javascript/page_functions.js.jsp` a `/components/form/check_form.css`.

Při použití npm ale můžete vkládat jQuery do `ninja.js` a tedy nechcete, aby knihovnu duplicitně vložil WebJET při zobrazení objektu `doc_data`. To lze ovlivnit následujícími možnostmi:
- Pokud `data-iwcm-combine` obsahuje `${ninja.webjet.pageFunctionsPath}` předpokládá se, že jQuery je vloženo přes např. `ninja.js` a už se automaticky nevloží.
- Při použití `data-iwcm-write="doc_data"` je možné přidat atribut `data-iwcm-jquery` s následujícími hodnotami:
  - `false` - jQuery už je vloženo, nechceme jej automaticky vložit WebJETem
  - `true` - jQuery se vkládá automaticky, kontroluje se ale duplicita a vloží se jen pokud dosud nebylo WebJETem vloženo (vloží se i `page_functions.js.jsp` a `check_form.css`)
  - `force` - i když si WebJET myslí, že je jQuery vloženo (např. z `data-iwcm-combine`) vynutí se jeho znovu vložení (vloží se i `page_functions.js.jsp` a `check_form.css`)

Doporučujeme vkládat jQuery přes NPM modul do ninja.js a následně použít `combine` s `${ninja.webjet.pageFunctionsPath}` pro oznámení WebJETu, že jQuery je vloženo:

```html
<combine
    basePath="|${ninja.temp.basePath}|"
    data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
    ${ninja.webjet.pageFunctionsPath}"
>
    <script src="js/ninja.js" type="text/javascript"></script>
</combine>
```

CSS pro `/components/form/check_form.css` musíte vložit do vašeho scss (obsah si stáhněte v prohlížeči z uvedené URL adresy). Doporučujeme jej vložit do `5-modules/md-checkform.scss`.
