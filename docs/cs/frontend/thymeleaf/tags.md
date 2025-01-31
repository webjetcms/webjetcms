# Štítky WebJET CMS

V šablonách můžete používat speciální značky WebJET CMS.

## Standardní atributy

Seznam dostupných atributů při zobrazování webové stránky naleznete v části [samostatná kapitola](webjet-objects.md).

```html
<span data-th-text="${docDetails.title}">Titulok stránky</span>
<body data-th-class="${docDetails.fieldA}">
<meta name="author" data-th-if="${!#strings.isEmpty(ninja.temp.group.author)}" data-th-content="${ninja.temp.group.author}" />
<link data-th-href="${ninja.page.url}" rel="canonical" />
<link data-th-href="${base_css_link}" rel="stylesheet" type="text/css"/>

<article data-iwcm-write="doc_data"/>
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Atributy začínající na `data-th-XXX` se provede a nahradí `XXX` atribut podle hodnoty zadaného kódu. Při vytváření prototypů bez spuštěného systému WebJET CMS můžete atribut nastavit na výchozí hodnotu a nahradit ji při spuštění prostřednictvím systému WebJET pomocí příkazu `data-th-` Atribut:

```html
<!-- content atribut sa pri zobrazeni cez WebJET nahradi za data-th-content hodnotu -->
<meta property="og:title" content="WebJET CMS" data-th-content='${ninja.page.seoTitle}'/>
<!-- class atribut sa pri zobrazeni cez WebJET nahradi za data-th-class hodnotu -->
<body class="subpage" data-th-class="${docDetails.fieldA}">
```

## Provedení značky INCLUDE

Objekty webové stránky mohou obsahovat značku `!INCLUDE(...)!`, není možné použít standardní atribut pro jeho provedení. `data-th-text` ale musíte použít speciální atribut `data-iwcm-write`. Buď lze provést [atribut stránky](webjet-objects.md) nebo přímo zadané `!INCLUDE(...)!` Soudní příkaz.

Převzato z **může** odstranění obalu `div` prvek. Chování je následující:
- pokud název atributu začíná na `doc_` (např. `doc_data`) balení `div` prvek **budou zachovány** (pro lepší kompatibilitu při prototypování a možnost nastavení atributů jako `class` a podobně)
- pokud je hodnota atributu `data-iwcm-remove` Je `false` nebo NE `tag` obalový prvek **budou zachovány**
- v ostatních případech je obal `div` prvek **odstraněno** a kód z WebJETu je vložen do stránky.

```html
<!-- zacina na doc_ standardne by header element zostal, ale nastavenim data-iwcm-remove="tag" header element odstranime -->
<header data-iwcm-write="doc_head" data-iwcm-remove="tag" />
<!-- zacina na doc_ standardne sa article element zachova -->
<article data-iwcm-write="doc_data" class="page-body"/>
<!-- NEzacina na doc_ standardne sa div element odstrani -->
<div data-iwcm-write="!INCLUDE(/components/gdpr/gtm_init.jsp)!" />
```

Použijte `data-iwcm-remove` zjednodušené z hlediska kódu HTML prováděného při nastavování `true` Volejte `element[0].outerHTML=data` (to znamená, že je nahrazen i tag, na kterém je nastaven), jinak se provede `element.html(data)`, tj. nahradí vnitřek značky.

**Doporučujeme také** využívat vlastnosti značky `data-iwcm-write` který **odstraní tělo**, `div` Znaky. Můžete tak **efektivně vytvářet prototypy na úrovni html, aniž by bylo nutné spouštět šablonu v aplikaci WebJET.**:

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

což znamená, že při zobrazení html šablony přímo v prohlížeči se zobrazí. `<article>Lorem ipsum</article>`. Když je šablona spuštěna v aplikaci WebJET, vše, co je uvnitř, se `<article data-iwcm-write="doc_data">` pro html kód webové stránky (včetně obalů `<div class="container">....`).

## Kombinování souborů

Chcete-li kombinovat soubory JavaScript a CSS, můžete použít atribut `data-iwcm-combine`. Automaticky rozdělí soubory na JavaScript a CSS podle přípony a vloží dva samostatné odkazy na. `combine.jsp`. Můžete použít libovolnou značku, v příkladu je to `combine`, po zobrazení se odstraní.

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

případně jako `PUG` Zápis:

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

Všimněte si, že v těle `combine` jsou vloženy standardní `link` a `script` Znaky. Ty jsou zpracovány a použity v kombinovaném záznamu. Výhodou je, že si je můžete nechat zobrazit přímo `.html` v prohlížeči a výše uvedené skripty a soubory CSS se do stránky vloží ve výchozím nastavení. I bez spuštění stránky ve WebJETu je stránka v prohlížeči funkční.

Kombinace nejprve vloží skripty a CSS z těla a poté přidá ty, které jsou definovány v položce `data-iwcm-combine` Atribut. Vložte pouze specifické skripty a CSS, které se používají pouze při zpracování prostřednictvím WebJET CMS.

## Vkládání skriptů

Pro vkládání skriptů z aplikace skripty existuje speciální značka. `data-iwcm-script`. Jako název obdrží pozici skriptu (např. `header`), jak je definováno v žádosti. Značka používá značku `div`, ale při zobrazení se odstraní.

```html
<div data-iwcm-script="header"/>
```

## Vložení jQuery

WebJET typicky potřebuje vestavěnou knihovnu jQuery od společnosti `/components/_common/javascript/jquery.min.js` a soubory `/components/_common/javascript/page_functions.js.jsp` a `/components/form/check_form.css`.

Ale při použití npm můžete jQuery vložit do `ninja.js` a proto nechcete, aby WebJET při zobrazování objektu duplikoval knihovnu. `doc_data`. To lze ovlivnit následujícími možnostmi:
- Pokud `data-iwcm-combine` Obsahuje `${ninja.webjet.pageFunctionsPath}` předpokládá se, že jQuery je vloženo např. prostřednictvím. `ninja.js` a již se nevkládá automaticky.
- Při použití `data-iwcm-write="doc_data"` lze přidat atribut `data-iwcm-jquery` s následujícími hodnotami:
  - `false` - jQuery je již vloženo, nechceme ho automaticky vkládat pomocí WebJETu.
  - `true` - jQuery se vkládá automaticky, ale kontroluje se, zda se neduplikuje, a vkládá se pouze tehdy, pokud ještě nebyl vložen pomocí WebJET (vkládá se také). `page_functions.js.jsp` a `check_form.css`)
  - `force` - i když si WebJET myslí, že je jQuery vloženo (např. z `data-iwcm-combine`) je nucen být znovu vložen (je také vložen `page_functions.js.jsp` a `check_form.css`)

Doporučujeme vložit jQuery prostřednictvím modulu NPM do souboru ninja.js a poté použít nástroj `combine` s `${ninja.webjet.pageFunctionsPath}` upozornit WebJET, že je vložen jQuery:

```html
<combine
    basePath="|${ninja.temp.basePath}|"
    data-iwcm-combine="${ninja.userAgent.blind ? 'css/blind-friendly.min.css' : ''}
    ${ninja.webjet.pageFunctionsPath}"
>
    <script src="js/ninja.js" type="text/javascript"></script>
</combine>
```

CSS pro `/components/form/check_form.css` musíte vložit do svého scss (stáhněte si obsah v prohlížeči z výše uvedené adresy URL). Doporučujeme jej vložit do `5-modules/md-checkform.scss`.
