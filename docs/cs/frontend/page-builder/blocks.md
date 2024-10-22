# Bloky

Nástroj Page Builder zahrnuje také vkládání připravených bloků. Jejich seznam se automaticky načte z adresáře `/templates/INSTALL_NAME/skupina_sablon/menosablony/pagebuilder`. **Vždy doporučujeme připravit sadu bloků pro šablonu.**

V kořenovém adresáři pro bloky můžete mít následující podadresáře:
- `section` - pro bloky sekcí (modré označení v nástroji Page Builder)
- `container` - pro kontejnery (červené označení v nástroji Page Builder)
- `column` - pro sloupce (zelené označení v nástroji Page Builder)

V každém z těchto podadresářů musíte ještě vytvořit **skupiny bloků jako další podadresáře**, např. `Contact, Features`. Pouze v těchto podadresářích vytváříte jednotlivé bloky HTML. Příkladem je adresářová struktura:

```java
- section
  - Contact
    - contact01.html
    - contact01.jpg
    - form.html
    - form.jpg
  - Features
    - promo.html
    - promo.jpg
    - pricelist.html
    - pricelist.jpg
- container
  - Teams
    - teams01.html
    - teams01.jpg
    - teams02.html
    - teams02.jpg
- column
  - Text-With-Picture
    - left.html
    - left.jpg
    - right.html
    - right.jpg
```

## Podpora kódu Thymeleaf

Je důležité si uvědomit, že bloky jsou do stránky vloženy bez provedení kódu Thymeleaf (technicky je kód vložen přímo ze souboru html do editoru). V současné době jsou však při vkládání podporovány následující atributy Thymeleaf:
- `data-iwcm-write` - pro podporu vkládání aplikací
- `data-iwcm-remove` - pro podporu vkládání aplikací
- `data-th-src` - vložení obrázku
- `data-th-href` - vložení odkazu

Současně se při vkládání provádějí následující objekty ninja (zapsané např. jako `src="./assets/images/logo.png" data-th-src="${ninja.temp.basePathImg}logo.png"`):
- `${ninja.temp.basePath}`
- `${ninja.temp.basePathAssets}`
- `${ninja.temp.basePathCss}`
- `${ninja.temp.basePathJs}`
- `${ninja.temp.basePathPlugins}`
- `${ninja.temp.basePathImg}`

Pokud potřebujete pro svou práci vyrobit další štítky Thymeleaf, můžete nám poslat žádost prostřednictvím funkce Zpětná vazba na domovské stránce administrace.

## Generování náhledových obrázků

Všimněte si, že pro každý blok HTML existuje také obrázek se stejným názvem. Pokud existuje, zobrazí se v nástroji Page Builder v seznamu bloků jako obrázek bloku. Obrázky lze připravit ručně, ale lze je také automaticky vygenerovat zavoláním skriptu `/components/grideditor/phantom/generator.jsp`.

Skript vloží do zadané šablony JSP jednotlivé bloky se simulací zadaného kódu. `docid` a vytváří snímky obrazovky. Vyžaduje nainstalovaný program [PhantomJS](https://phantomjs.org) a v konfigurační proměnné `grideditorPhantomjsPath` nastavit cestu k adresáři, ve kterém `PhantomJS` instalován.

## Běžné bloky

V současné době bohužel neexistuje možnost vnořit bloky do sebe. Může existovat požadavek na vložení `section` blok obsahující určitý `container` blok, přičemž je nutné, aby bylo možné vložit i stejný `container` blok samostatně. V tomto případě se duplikuje kód HTML bloků v adresáři. `section` Také `container`.

Doporučujeme generovat bloky pomocí [PugJS](https://pugjs.org).

## Třídy CSS pro obrázek

Pokud má obrázek nastavenou třídu CSS `fixedSize-w-h-ip` zadaný rozměr se nastaví automaticky po změně adresy obrázku. `w` a `h` pokud je zadána i poslední položka `ip` je také nastaven [bod zájmu](http://docs.webjetcms.sk/v8/#/front-end/thumb-servlet/bod-zaujmu). Např. třída CSS `fixedSize-160-160-5` automaticky vygeneruje obrázek o rozměrech 160 x 160 pixelů s nastaveným bodem zájmu 5. Tuto třídu doporučujeme nastavit pro všechny ilustrační obrázky, u kterých je důležitá velikost.

Když kliknete na obrázek s třídou CSS `fixedSize/w-100/autoimg` okamžitě otevře okno s vlastnostmi obrázku pro snadnou výměnu. Editor tak nemusí klikat na obrázek a poté na ikonu pro změnu obrázku na panelu nástrojů.

## Podpora karet

Pro pohodlnou úpravu karet (`tabs`) je podporováno jejich automatické generování ze struktury HTML. Každá karta je reprezentována kontejnerem. Kontejnery lze snadno duplikovat a přesouvat a záložky se automaticky generují z jejich obsahu.

Prvek `UL` by měly být označeny třídou CSS `pb-autotabs`. JavaScriptový kód v souboru `/admin/webpages/page-builder/scripts/pagesupport.js` zajistit generování karty po přidání prvku / každých 5 sekund. Přebírá název karty z `title` atributu kontejneru nebo z prvku s třídou CSS `pb-tab-title` (což je výhodnější pro úpravy).

Samotné karty tedy nelze upravovat, jsou generovány automaticky. Upravitelný je pouze obsah `tab` kontejner. Všimněte si, že v ukázkovém kódu neobsahuje prvek UL žádné prvky. `LI taby`, budou vygenerovány automaticky. V kódu HTML zůstanou i poté a budou také správně uloženy. Na stránce zůstane zobrazena karta tak, jak byla zobrazena během úprav (na to je třeba pamatovat).

Všimněte si použití třídy CSS `pb-not-container` na hlavních prvcích kontejneru. Tím se zajistí, že tento prvek nebude označen jako kontejner a za kontejnery budou považovány pouze jednotlivé karty. Každá karta používá třídu CSS `pb-custom-container`, který zajistí zobrazení červeného rámečku/nástrojové lišty kontejneru.

Po výběru možnosti přesunout kartu (na panelu nástrojů kontejneru) se automaticky zobrazí všechny karty, takže můžete snadno označit, kam chcete kartu přesunout. To zajišťuje styl CSS nástroje Sestavení stránky.

Ukázka blokového kódu (v `section` adresář):

```html
<section>
   <div class="container pb-not-container">

         <div class="tabsBox">
            <ul class="nav nav-tabs pb-autotabs"></ul>
         </div>

         <div class="tab-content">
           <div class="tab-pane fade active show pb-custom-container">
             <div class="row">
                <div class="col-12 pb-col-12 pb-tab-title">
                  <h3>Tab 1</h3>
               </div>
               <div class="col-12 pb-col-12">
                  <p>Text 1</p>
               </div>

             </div>

           </div>

           <div class="tab-pane fade pb-custom-container">
             <div class="row">
                <div class="col-12 pb-col-12 pb-tab-title">
                  <h3>Tab 2</h3>
               </div>
               <div class="col-12 pb-col-12">
                  <p>Text 2</p>
               </div>

             </div>
           </div>

           <div class="tab-pane fade pb-custom-container">
             <div class="row">
                <div class="col-12 pb-col-12 pb-tab-title">
                  <h3>Tab 3</h3>
               </div>
               <div class="col-12 pb-col-12">
                  <p>Text 3</p>
               </div>

             </div>
           </div>
         </div>

   </div>
 </section>
```

## Podpora akordeonu

`Accordion` funguje podobně jako karty, Page Builder zajišťuje správné generování potřebných atributů a jejich automatické obnovení při duplikování položky. `accordion-u`. Funkčnost je spojena s třídou CSS `pb-autoaccordion` a implementovány podobně jako u karet. Kontejnery se používají podobným způsobem.

Vzorový kód:

```html
<section>
  <div class="container pb-not-container pb-autoaccordion">

    <h2 class="text-center pb-editable">Nadpis nad accordionom</h2>

    <div class="card pb-custom-container">
      <div class="card-header">
        <a class="accordionLink" data-toggle="collapse">
          <div class="pb-editable">
            <p>Nadpis accordionu 1</p>
          </div>
        </a>
      </div>
      <div class="collapse">
        <div class="card-body">
          <div class="row">
            <div class="pb-col-12">
              <p>Text accodrionu 1</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="card pb-custom-container">
      <div class="card-header">
        <a class="accordionLink" data-toggle="collapse">
          <div class="pb-editable">
            <p>Nadpis accordionu 2</p>
          </div>
        </a>
      </div>
      <div class="collapse">
        <div class="card-body">
          <div class="row">
            <div class="pb-col-12">
              <p>Text accodrionu 2</p>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</section>
```

## Karty v harmonice

Pokud je požadavek na vložení objektů typu karta do `accordion-u` je možné použít vlastnost Page Builder - označuje také **vnořené kontejnery**. Je třeba zvážit, co bude možné upravovat, jak duplikovat jednotlivé položky apod. Prakticky se vkládání karet do `accordion-ov` (což je kontejner) jako vložení dalšího `columnu` do kontejnera (přičemž vložený `column` dále obsahuje vnořené kontejnery jednotlivých karet).

V příkladu si všimněte, že hlavní `column` má styl CSS `pb-not-editable` aby nebyl automaticky editovatelný editorem CK a zároveň třída CSS `pb-always-mark`. Needitovatelný sloupec není ve výchozím nastavení označen zeleným rámečkem, ale bez této možnosti by nebylo možné přidat další sloupec za karty nebo celé karty odstranit (nástroje pro sloupce by nebyly k dispozici).

Při vkládání kódu HTML obsahujícího výraz `container` při spuštění sloupce objektů `PageBuilder.mark_grid_elements();` označit všechny prvky (aby se panely nástrojů zobrazily i pro vnořené kontejnery).

Ukázka kódu karty pro akordeon (v adresáři sloupců):

```html
<div class="col-12 pb-not-editable pb-always-mark">
   <div class="tabsBox" role="tablist">
      <ul class="nav nav-tabs pb-autotabs"></ul>
   </div>

   <div class="tab-content">
      <div class="tab-pane fade active show pb-custom-container">
         <div class="row">
            <div class="col-12 pb-tab-title">
               <h3>Tab 1</h3>
            </div>
            <div class="col-12">
               <p>Tab text 1</p>
            </div>

         </div>
      </div>

      <div class="tab-pane fade pb-custom-container">
         <div class="row">
            <div class="col-12 pb-tab-title">
               <h3>Tab 2</h3>
            </div>
            <div class="col-12">
               <p>Tab text 2</p>
            </div>
         </div>
      </div>
   </div>

</div>
```

po vložení se na webové stránce vytvoří struktura typu:

```html
<section>
  <div class="container pb-not-container pb-autoaccordion">

    <h2 class="text-center pb-editable">Nadpis nad accordionom</h2>

    <div class="card pb-custom-container">
      <div class="card-header">
        <a class="accordionLink" data-toggle="collapse">
          <div class="pb-editable">
            <p>Nadpis accordionu 1</p>
          </div>
        </a>
      </div>
      <div class="collapse">
        <div class="card-body">
          <div class="row">
            <div class="pb-col-12">
              <p>Text accodrionu 1</p>
            </div>

            <div class="col-12 pb-not-editable pb-always-mark">
              <div class="tabsBox" role="tablist">
                  <ul class="nav nav-tabs pb-autotabs"></ul>
              </div>
              <div class="tab-content">
                  <div class="tab-pane fade active show pb-custom-container">
                    <div class="row">
                        <div class="col-12 pb-tab-title">
                          <h3>Tab 1</h3>
                        </div>
                        <div class="col-12">
                          <p>Tab text 1</p>
                        </div>
                    </div>
                  </div>
                  <div class="tab-pane fade pb-custom-container">
                    <div class="row">
                        <div class="col-12 pb-tab-title">
                          <h3>Tab 2</h3>
                        </div>
                        <div class="col-12">
                          <p>Tab text 2</p>
                        </div>
                    </div>
                  </div>
              </div>

            </div>

          </div>
        </div>
      </div>
    </div>

  </div>
</section>
```

## Podpora nabídky

PageBuilder umí generovat položky menu do bootstrapových menu, což zajišťuje nástroj `pbAutoMenu` v `pagesupport.js`. Položka nabídky se generuje na `ul.pb-automenu` všech `section` prvky na webové stránce. Funguje to takto:
- `section.pb-not-automenu` je v seznamu vynechán.
- Název položky nabídky je převzat z:
  - prvek se stylem CSS `.section-title`
  - pokud není nalezen, je převzat z `h1` prvek
  - pokud není nalezen, je atribut převzat `title` na adrese `section` prvek
- Pokud není oddíl nastaven `id` je nastaven podle pořadového čísla sekce.

Z nalezených údajů se vygenerují položky `li.nav-item` na `ul.pb-automenu`.

Ukázkový blok s nabídkou:

```html
<section class="pb-not-automenu">
    <div class="container pb-not-container md-tabs">
        <nav class="navbar navbar-expand-lg navbar-light bg-light">
            <div class="container-fluid">
                <a class="navbar-brand" href="#">Navbar</a>
                <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav"
                    aria-controls="navbarNav" aria-expanded="false" aria-label="Toggle navigation">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="navbarNav">
                    <ul class="navbar-nav pb-automenu">
                        <li class="nav-item">
                            <a class="nav-link active" aria-current="page" href="#">Home</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="#">Features</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="#">Pricing</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link disabled" href="#" tabindex="-1" aria-disabled="true">Disabled</a>
                        </li>
                    </ul>
                </div>
            </div>
        </nav>
    </div>
</section>
```

V ukázce jsou prvky `Home, Features, Pricing a Disabled` nahradit obsah stránky PageBuilder. Obsah se automaticky aktualizuje při přidání, odstranění nebo přesunutí sekce na webové stránce. Nadpis se aktualizuje každých 5 sekund, takže pokud změníte název `h1` Chvíli počkejte, než se vygeneruje nová verze nabídky.

Kompletní ukázka webové stránky v kódu HTML s ukázkovými sekcemi:

```html
<section class="pb-not-automenu">
    <div class="container pb-not-container md-tabs">
        <nav class="navbar navbar-expand-lg navbar-light bg-light">
            <div class="container-fluid"><a class="navbar-brand" href="#">Navbar</a><button aria-controls="navbarNav"
                    aria-expanded="false" aria-label="Toggle navigation" class="navbar-toggler"
                    data-bs-target="#navbarNav" data-bs-toggle="collapse" type="button"><span
                        class="navbar-toggler-icon"></span></button>
                <div class="collapse navbar-collapse" id="navbarNav">
                    <ul class="navbar-nav pb-automenu">
                        <li class="nav-item"><a aria-current="page" class="nav-link active" href="#">Home</a></li>
                        <li class="nav-item"><a class="nav-link" href="#">Features</a></li>
                        <li class="nav-item"><a class="nav-link" href="#">Pricing</a></li>
                        <li class="nav-item"><a aria-disabled="true" class="nav-link disabled" href="#" tabindex="-1">Disabled</a></li>
                    </ul>
                </div>
            </div>
        </nav>
    </div>
</section>

<section>
    <div class="container">
        <div class="row">
            <div class="col-md-12">
                <h1>Úvod</h1>
            </div>
        </div>
    </div>
</section>

<section>
    <div class="container">
        <div class="row">
            <div class="col-md-12">
                <p class="section-title">Stred</p>
                <h1>Toto nie je nadpis</h1>
            </div>
        </div>
    </div>
</section>

<section title="TITLE nadpis">
    <div class="container">
        <div class="row">
            <div class="col-md-12">
                <p>Záver</p>
            </div>
        </div>
    </div>
</section>
```
