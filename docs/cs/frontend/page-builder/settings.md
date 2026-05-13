# Page Builder

## Inicializace editoru

Při editaci stránky se editor inicializuje na jednotlivé editovatelné bloky, čili **v jednom čase je ve stránce inicializováno více editorů**. Přes Page Builder se upravuje struktura stránky pomocí jeho nástrojů pro práci s bloky. Přes CK editor se upravují samotné texty.

Editor se automaticky inicializuje na:

- column elementy (```class="col-*"```)
- elementy označené CSS třídou ```pb-editable``` (```class="pb-editable"```)

**Nastavení a konfigurace:**

Pro správné spuštění Page Builder nastavte:

- ve vlastnostech skupiny šablon Typ editoru stránek na hodnotu Page Builder
- pokud chcete, aby byl dostupný Page Builder i při zobrazení stránky na frontendu nastavte konfigurační proměnnou ```inlineEditingEnabled``` na ```true```
- v případě nestandardního ```bootstrap gridu```, konfigurační proměnnou ```bootstrapColumns```, která určuje počet sloupců ```gridu```

Další konf. proměnné, které lze upravit:

- `pagebuilderFilterAutoOpenItems` - ​​počet položek, které se při filtrování v seznamu bloků automaticky otevřou, ve výchozím nastavení 10.
- `pagebuilderLibraryImageWidth` - ​​šířka náhledových obrázků v knihovně bloků, ve výchozím nastavení 310.
- `inlineEditingDisabledUrls` - ​​seznam URL adres, pro které nebude dostupný inline editor
- `pageBuilderPrefix` - ​​prefix, který se používá pro CSS třídy Page Builder (výchozí pb), změnit je možné pouze pokud změníte i prefixy v CSS třídách Page Builder

**Výjimky**

Režim se nastavuje pro skupinu šablon, pokud potřebujete v některé **konkrétní šabloně režim vypnout** můžete v šabloně v kartě Základní nastavit jinou možnost v poli `Typ editora stránok` než používá skupina šablon.

**Hlavičky/patičky**

Page Builder v editoru je inicializován na celou stránku, načte se včetně CSS stylů a šablony do iframe elementu. V tomto režimu ale nechceme zobrazovat ostatní elementy web stránky (hlavičku, patičku...) proto jsou tyto elementy nastaveny na prázdnou hodnotu. Navíc při zobrazení stránky jsou přes CSS atribut ```display: none``` schovány následující elementy:

```css
 body div.header, body header, body div.footer, body footer { display: none; }
```

## Konvence CSS tříd s podporou pro Ninja page builder

Korektní struktura html kódu, nad kterou se dokáže Page Builder inicializovat, je následující:

```html
<section>
    <div class="container">
        <div class="pb-editable">
            <h1>Some editable heading</h1>
        </div>
        <p>
            Some NOT editable text
        </p>
        <p class="pb-editable">
            Editable paragraph
        </p>
        <div class="row">
            <div class="col-4">
               <p>Some editable paragraph</p>
            </div>
            <div class="col-4 pb-not-editable">
               <p>Some not editable content</p>
            </div>
            <div class="col-4">
               <p>Some editable content</p>
            </div>
        </div>
    </div>
</section>
```

Stylování je doporučeno pouze na ```section```, ```container``` a ```column content```, protože jen tyto elementy si umí uživatel upravit pomocí Page Builder.

!>**Upozornění:** v ```column``` elementech není povoleno použít přímo text, je třeba použít minimálně P element.
Navíc z důvodu možnosti nastavení odsazení (```margin/padding```) v rámci ```column``` elementu je obsah elementu po otevření v Page Builderu obalen do DIV elementu s CSS třídou ```column-content```.

Čili z kódu:

```html
<div class="col-4">
   Some content
</div>
```

vznikne po inicializaci Page Builder kód:

```html
<div class="col-4">
    <div class="column-content">
        <p>Some content</p>
    </div>
</div>
```

## Stylování elementů

### `SECTION` (modrá barva)

Inicializace při použití elementu: ```<section>```.

Stylování pomocí třídy, s prefixem: ```pb-style-section-```

```html
<section class="pb-style-section-team-26"></section>
```

Nastavením CSS třídy ```pb-not-section``` se element **nebude považovat za section* element.

### `CONTAINER` (červená barva)

Inicializace při použití CSS třídy: ```container``` nebo ```pb-custom-container```. Nastavením CSS třídy ```pb-not-container``` se element **nebude považovat za kontejner** i když má CSS třídu ```container```.

Stylování pomocí třídy, s prefixem: ```pb-style-container-```

```html
<div class="container pb-style-container-group-26"></div>
```

### `ROW`

```<div class="row">``` sa momentálne nedá editovať pomocou Page Builder, je použitý z dôvodu bootstrap kompatibility.

### `COLUMN` (zelená farba)

Inicializácia pri použití triedy: ```col-``` ALEBO ```pb-col-``` (ak DIV element nie je štandardný bootstrap ```col-```). Akceptované sú aj hodnoty ```pb-col``` a ```pb-col-auto``` - ak element obsahuje tieto CSS štýly, tak v nástrojovej lište sa nezobrazí ikona nastavenia šírky stĺpca.

Ak má column CSS triedu ```pb-not-editable``` tak sa **nebude považovať za column** (nebude v ňom automaticky aj editovateľný text). Ak pre ne-editovateľný element potrebujete mať možnosť nastavovania šírky/kopírovania/presúvania atď. nastavením CSS triedy ```pb-always-mark``` sa element označí a bude sa zobrazovať zelený rámik aj s jeho možnosťami.

Štýlovanie pomocou triedy, s prefixom: ```pb-style-column-```.

```html
<div class="col-12 pb-style-content-person-26"></div>
```

Nastavením CSS třídy ```pb-not-column``` se element **nebude považovat za columns* i když má CSS třídu ```col-```.

## Výjimky editace

### Editovatelný element

Pokud se vyžaduje editování elementu, který není z mezi výše zmíněných, je možné použít element s třídou ```pb-editable```.

Tento element nebude možné nastylovat pomocí Page Builder, ale bude editovatelný pomocí CK editoru.

```html
<div class="pb-editable"></div>
```

### Needitovatelný element

Pokud se vyžaduje element, který nechcete, aby byl editovatelný, je možné použít třídu ```pb-not-editable```.

To zakáže inicializaci Page Builder a CK editoru na daný element a všechny elementy v něm.

```html
<section class="pb-not-editable"></section>
```

## Unikátní ID

Pokud potřebujete, aby v blocích bylo použito unikátní ID můžete použít hodnotu ```__ID__```, která se v HTML kódu po vložení do stránky nahradí za náhodnou hodnotu (```timestamp```).

```html
<div class="carousel slide" id="carouselControls__ID__" data-bs-ride="carousel">
    <div class="carousel-inner row">
        ...
    </div>
    <button class="carousel-control-prev" data-bs-slide="prev" data-bs-target="#carouselControls__ID__" type="button"></button>
    <button class="carousel-control-next" data-bs-slide="next" data-bs-target="#carouselControls__ID__" type="button"></button>
</div>
```

## Události

PageBuilder během práce vyvolá na ```windows``` objektu více událostí. Poslouchat na ně můžete následovně:

```javascript
window.addEventListener("WJ.PageBuilder.gridChanged", function(e) {
    //
});
```

Aktuálně jsou podporovány následující události:

- ```WJ.PageBuilder.loaded``` - ​​po nahrání stránky v editoru
- ```WJ.PageBuilder.gridChanged``` - ​​změna v ```gride```
- ```WJ.PageBuilder.styleChange``` - ​​změna ve vlastnostech bloku (stylování)
- ```WJ.PageBuilder.newElementAdded``` - ​​přidán nový element
- ```WJ.PageBuilder.elementDuplicated``` - ​​duplikovaný element
- ```WJ.PageBuilder.elementMoved``` - ​​přesunutý element

## Zobrazení nepotřebných bloků

Přes Page Builder se zobrazují i ​​bloky jako hlavička, patička, menu atd., což v některých případech nemusí být žádoucí. Jednoduché řešení je tyto bloky přes CSS styl schovat. V režimu editoru je na `body` elementu CSS třída `is-edit-mode` která vám umožní schovat nepotřebné elementy.

Pokud potřebujete pouze deaktivovat odkazy, můžete nastavit v CSS stylu `pointer-events: none`:

```css
.is-edit-mode .header a {
    pointer-events: none;
}
```