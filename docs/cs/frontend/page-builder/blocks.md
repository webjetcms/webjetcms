# Bloky

Součástí Page Builder je i vkládání připravených bloků. Jejich seznam se automaticky načte z adresáře `/templates/INSTALL_NAME/skupina_sablon/menosablony/pagebuilder`. **Vždy doporučujeme připravit sadu bloků pro šablonu.**

V kořenovém adresáři pro bloky můžete mít následující pod adresáře:
- `section` - pro bloky sekcí (modré označení v Page Builderu)
- `container` - pro kontejnery (červené označení v Page Builderu)
- `column` - pro sloupce (zelené označení v Page Builderu)

V každém z těchto podadresářů je třeba ještě vytvořit **skupiny bloků jako další pod adresáře** Např. `Contact, Features`. Teprve v těchto pod adresářích vytváříte jednotlivé HTML bloky. Příkladem je tedy adresářová struktura:

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

## Nastavení šířky sloupců

Editor umožňuje nastavit šířky sloupce podle zvoleného zařízení. V nástrojové liště u přepínače typu editoru je možnost nastavit velikost (šířku) zařízení.

![](../../redactor/webpages/pagebuilder-switcher.png)

- Desktop - je určen pro šířku větší/rovnou než 1200 bodů (nastavuje CSS třídu `col-xl`).
- Tablet - je určen pro šířku 768-1199 bodů (nastavuje CSS třídu `col-md`)
- Mobil - je určen pro šířku menší než 768 bodů (nastavuje CSS třídu `col-`)

Správné nastavení bloku obsahuje přednastavené všechny šířky, například. `col-12 col-md-6 col-xl-3`:

```html
<section>
  <div class="container">
    <div class="row">
      <div class="col-12">
        <h2 class="text-center">Etiam orci</h2>
      </div>
    </div>
    <div class="row">
      <div class="col-12 col-md-6 col-xl-3 text-center">
        <p class="text-center">
          <img src="/thumb/images/zo-sveta-financii/foto-1.jpg?w=160&h=160&ip=5" class="fixedSize-160-160-5" />
        </p>
        <h3>Etiam orci</h3>
        <p>Suspendisse interdum dolor justo, ac venenatis massa suscipit nec. Vivamus dictum malesuada mollis.</p>
      </div>
      <div class="col-12 col-md-6 col-xl-3  text-center">
        <p class="text-center">
          <img src="/thumb/images/zo-sveta-financii/foto-1.jpg?w=160&h=160&ip=5" class="fixedSize-160-160-5" />
        </p>
        <h3>Aenean </h3>
        <p>Aliquam elementum ut ante vitae dapibus. Interdum et malesuada fames ac ante ipsum primis in faucibus.</p>
      </div>
      <div class="col-12 col-md-6 col-xl-3  text-center">
        <p class="text-center">
          <img src="/thumb/images/zo-sveta-financii/foto-1.jpg?w=160&h=160&ip=5" class="fixedSize-160-160-5" />
        </p>
        <h3>Maecenas</h3>
        <p>Sed sollicitudin eros quis leo imperdiet, id congue lorem ornare. Suspendisse eleifend at ante id ultrices.</p>
      </div>
      <div class="col-12 col-md-6 col-xl-3  text-center">
        <p class="text-center">
          <img src="/thumb/images/zo-sveta-financii/foto-1.jpg?w=160&h=160&ip=5" class="fixedSize-160-160-5" />
        </p>
        <h3>Suspendisse</h3>
        <p>Nullam ornare, magna in ultrices mattis, lectus neque mollis libero, vitae varius mauris metus a risus.</p>
      </div>
    </div>
  </div>
</section>
```

## Podpora Thymeleaf kódu

Důležité je si uvědomit, že bloky se do stránky vloží bez provedení Thymeleaf kódu (technicky se vloží přímo kód z html souboru do editoru). Při vložení jsou aktuální ale podporovány následující thymeleaf atributy:
- `data-iwcm-write` - pro podporu vkládání aplikací
- `data-iwcm-remove` - pro podporu vkládání aplikací
- `data-th-src` - vkládání obrázku
- `data-th-href` - vkládání odkazu

Zároveň při vkládání jsou provedeny následující ninja objekty (zapsáno např. jako `src="./assets/images/logo.png" data-th-src="${ninja.temp.basePathImg}logo.png"`):
- `${ninja.temp.basePath}`
- `${ninja.temp.basePathAssets}`
- `${ninja.temp.basePathCss}`
- `${ninja.temp.basePathJs}`
- `${ninja.temp.basePathPlugins}`
- `${ninja.temp.basePathImg}`

Pokud pro vaši práci potřebujete provedení jiných Thymeleaf značek můžete nám zaslat požadavek přes funkci Zpětná vazba na úvodní straně administrace.

## Generování náhledových obrázků

Všimněte si, že ke každému HTML bloku existuje také obrázek se stejným názvem. Pokud existuje, zobrazí se v Page Builder v seznamu bloků jako obrázek bloku. Obrázky lze připravit manuálně, ale také automaticky vygenerovat voláním skriptu `/components/grideditor/phantom/generator.jsp`.

Skript vkládá jednotlivé bloky do zadané JSP šablony se simulací zadaného `docid` a vytváří screenshoty. Vyžaduje nainstalovaný program [PhantomJS](https://phantomjs.org) a v konfigurační proměnné `grideditorPhantomjsPath` nastavenou cestu k adresáři kde je `PhantomJS` nainstalován.

## Společné bloky

Bohužel aktuálně neexistuje možnost vkládání bloků do sebe. Může existovat požadavek vkládat `section` blok obsahující určitý `container` blok, přičemž je třeba mít možnost vložit i stejný `container` blok samostatně. V takovém případě vzniká duplicita HTML kódu bloků v adresáři `section` i `container`.

Doporučujeme bloky generovat pomocí [PugJS](https://pugjs.org).

## CSS třídy pro obrázek

Pokud má obrázek nastavenou CSS třídu `fixedSize-w-h-ip` je automaticky po změně adresy obrázku nastaven zadaný rozměr `w` a `h`, je-li zadán i poslední údaj `ip` nastaví se také [bod zájmu](../../frontend/thumb-servlet/README.md). Např. CSS třída `fixedSize-160-160-5` automaticky generuje obrázek rozměru 160 x 160 bodů s nastaveným bodem zájmu 5. Třídu doporučujeme nastavit na všechny ilustrační obrázky, u kterých je důležitý jejich rozměr.

Při kliknutí na obrázek s CSS třídou `fixedSize/w-100/autoimg` se ihned otevře okno vlastností obrázku pro jeho jednoduchou výměnu. Redaktor tak nemusí kliknout na obrázek a následně v nástrojové liště na ikonu změny obrázku.

Pokud obrázek obsahuje v URL adrese výraz `placeholder` nebo `stock` neotevře se dialogové okno výběru obrázku do složky s tímto obrázkem, ale do složky Média této stránky. Uživatel tak může snadno nahrát nový obrázek.

## Podpora karet

Pro pohodlnou editaci karet (`tabs`) je podporováno jejich automatické generování z HTML struktury. Každá karta je reprezentována kontejnerem. Kontejnery lze snadno duplikovat a přesouvat, z jejich obsahu se automaticky generují karty.

Element `UL` je třeba označit CSS třídou `pb-autotabs`. JavaScript kód v souboru `/admin/webpages/page-builder/scripts/pagesupport.js` zajistí generování karet po přidání elementu / každých 5 sekund. Jméno karty bere z `title` atributu kontejneru, nebo z elementu s CSS třídou `pb-tab-title` (což je pohodlnější pro editaci).

Samotné karty nejsou tedy editovatelné, generují se automaticky. Editovatelný je pouze obsah `tab` kontejneru. Všimněte si, že v ukázkovém kódu UL element neobsahuje žádné `LI taby`, ty se vygenerují automaticky. V HTML kódu zůstanou následně vygenerovány a také se korektně uloží. Na stránce zůstane zobrazená karta tak, jak byla zobrazena během editace (je na to třeba myslet).

Všimněte si použití CSS třídy `pb-not-container` na hlavním kontejner elementu. To zajistí, že tento element nebude označen jako kontejner a za kontejnery budou považovány až jednotlivé karty. Každá karta používá CSS třídu `pb-custom-container`, což zajistí zobrazení červeného rámu/nástrojové lišty kontejneru.

Při zvolení možnosti přesunutí tabu (v nástrojové liště kontejneru) se automaticky zobrazí všechny karty, aby bylo možné snadno označit kartu, kde se má přesunout. To je zajištěno CSS stylem Page Builder.

Ukázkový kód bloku (v `section` adresáři):

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

## Podpora accordion

`Accordion` pracuje podobně jako karty, Page Builder zajistí korektní vygenerování potřebných atributů a jejich automatickou obnovu při zduplikování položky `accordion-u`. Funkčnost je napojena na CSS třídu `pb-autoaccordion` a implementována podobně jako pro karty. Podobně se používají i kontejnery.

Ukázkový kód:

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

## Karty v accordion

Při požadavku vnořování objektů typu karta do `accordion-u` lze využít vlastnost Page Builder - označuje i **vnořené kontejnery**. Je třeba uvažovat, co bude možné editovat, jak duplikovat jednotlivé položky a podobně. Prakticky funguje vkládání karet do `accordion-ov` (což je kontejner) jako vložení dalšího `columnu` do kontejneru (přičemž vložen `column` dále obsahuje vnořené kontejnery jednotlivých tabů).

V příkladu si všimněte, že hlavní `column` má CSS styl `pb-not-editable` aby automaticky nebyl editovatelný CK editorem a zároveň CSS třídu `pb-always-mark`. Needitovatelný column se standardně neoznačí zeleným rámem, bez této možnosti by ale nebylo možné přidat za karty další column, nebo celé karty smazat (nebyly by dostupné nástroje columnu).

Při vložení HTML kódu obsahujícího výraz `container` jako column objektu je spuštěn `PageBuilder.mark_grid_elements();` pro označení všech elementů (aby se zobrazily nástrojové lišty i pro vnořené kontejnery).

Ukázkový kód karet pro accordion (v adresáři column):

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

po vložení vznikne ve web stránce struktura typu:

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

## Podpora menu

PageBuilder umí generovat menu položky do bootstrap menu, zajišťuje to funkce `pbAutoMenu` v `pagesupport.js`. Menu položky generuje do `ul.pb-automenu` ze všech `section` elementů ve web stránce. Pracuje následovně:
- `section.pb-not-automenu` je vynecháno ze seznamu.
- Název položky v menu se bere z:
  - elementu s CSS stylem `.section-title`
  - není-li nalezen bere se z `h1` elementu
  - není-li nalezen, bere se atribut `title` na `section` elemente
- Pokud section nemá nastaven `id` atribut, je nastaven podle pořadového čísla sekce.

Z nalezených údajů se vygenerují položky `li.nav-item` do `ul.pb-automenu`.

Ukázkový blok s menu:

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

V ukázce se elementy `Home, Features, Pricing a Disabled` nahradí za obsah v PageBuilder stránce. Obsah se automaticky aktualizuje po přidání, smazání nebo přesunu sekce ve web stránce. Název se aktualizuje jednou za 5 sekund, pokud tedy změníte `h1` nadpis počkejte chvíli na vygenerování nové verze menu.

Kompletní ukázka HTML kódu web stránky s ukázkovými sekcemi:

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
