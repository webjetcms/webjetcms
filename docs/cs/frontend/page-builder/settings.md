# Page Builder

## Inicializace editoru

Při editaci stránky je editor inicializován na jednotlivé editovatelné bloky, tj. **na stránce je inicializováno více editorů najednou**. Nástroj Page Builder slouží k úpravě struktury stránky pomocí blokových nástrojů. Editor CK slouží k úpravě samotného textu.

Editor je automaticky inicializován na:
- sloupcové prvky (`class="col-*"`)
- prvky označené třídou CSS `pb-editable` (`class="pb-editable"`)

**Nastavení a konfigurace:**

Chcete-li správně spustit nástroj Page Builder, nastavte:
- ve vlastnostech skupiny šablon Typ editoru stránky na Typ editoru stránky
- pokud chcete, aby byl nástroj Page Builder k dispozici i při zobrazení stránky na frontendu, nastavte konfigurační proměnnou `inlineEditingEnabled` na adrese `true`
- v případě nestandardních `bootstrap gridu`, konfigurační proměnná `bootstrapColumns` který určuje počet sloupců `gridu`

Další konfigurační proměnné, které lze upravit:
- `inlineEditableObjects` - objekty, na které se kromě doc\_data vztahují úpravy (např. doc\_header, doc\_footer, doc\_right\_menu).
- `inlineEditingEnabledDefaultValue` - po nastavení na `true` režim úprav je automaticky povolen při načtení stránky (není třeba klikat na tlačítko Upravit).
- `inlineEditingDisabledUrls` - seznam adres URL, pro které nebude inline editor k dispozici.
- `pageBuilderPrefix` - prefix používaný pro třídy CSS Page Builder (ve výchozím nastavení pb), lze změnit pouze tehdy, pokud změníte prefixy i u tříd CSS Page Builder.

**Výjimky**

Režim je nastaven pro skupinu šablon, pokud potřebujete. **vypnutí konkrétního režimu šablony** můžete nastavit další možnost v šabloně na kartě Základní v poli `Typ editora stránok` jak je používá skupina šablon.

**Hlavy/podpatky**

Nástroj Page Builder v editoru se inicializuje na celou stránku, načte se včetně stylů CSS a šablony do prvku iframe. V tomto režimu však nechceme zobrazovat další prvky webové stránky (záhlaví, zápatí...), takže tyto prvky jsou nastaveny na prázdnou hodnotu. Kromě toho se při zobrazení stránky zobrazí atribut CSS `display: none` následující prvky jsou skryté:

```css
 body div.header, body header, body div.footer, body footer { display: none; }
```

## Konvence tříd CSS s podporou nástroje Ninja page builder

Správná struktura html kódu, nad kterou se může inicializovat nástroj Page Builder, je následující:

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

Styling se doporučuje pouze u `section`, `container` a `column content` protože pouze tyto prvky může uživatel upravovat pomocí nástroje Page Builder.

!>**Varování:** v `column` není dovoleno používat přímo text, je nutné použít alespoň prvek P. Navíc kvůli možnosti nastavení odsazení (`margin/padding`) v rámci `column` obsah prvku je po otevření v nástroji Page Builder zabalen do prvku DIV s třídou CSS `column-content`.

Tedy z kódu:

```html
<div class="col-4">
   Some content
</div>
```

bude vytvořen po inicializaci kódu nástroje Page Builder:

```html
<div class="col-4">
    <div class="column-content">
        <p>Some content</p>
    </div>
</div>
```

## Stylování prvků

### `SECTION` (modrá barva)

Inicializace při použití prvku: `<section>`.

Stylování podle třídy s předponou: `pb-style-section-`

```html
<section class="pb-style-section-team-26"></section>
```

Nastavením třídy CSS `pb-not-section` prvek \**nebude považován za oddíl* prvek.

### `CONTAINER` (červená)

Inicializace při použití tříd CSS: `container` nebo `pb-custom-container`. Nastavením třídy CSS `pb-not-container` s prvkem **nebude považován za kontejner** i když má třídu CSS `container`.

Stylování podle třídy s předponou: `pb-style-container-`

```html
<div class="container pb-style-container-group-26"></div>
```

### `ROW`

`<div class="row">` není v současné době možné upravovat pomocí nástroje Page Builder, používá se z důvodu kompatibility s bootstrapem.

### `COLUMN` (zelená barva)

Inicializace při použití třídy: `col-` NEBO `pb-col-` (pokud prvek DIV není standardním prvkem bootstrap `col-`). Akceptovány jsou také hodnoty `pb-col` a `pb-col-auto` - pokud prvek obsahuje tyto styly CSS, ikona nastavení šířky sloupce se na panelu nástrojů nezobrazí.

Pokud má sloupec třídu CSS `pb-not-editable` takže s **nebude považován za sloupec** (nebude automaticky obsahovat upravitelný text). Pokud u needitovatelného prvku potřebujete mít možnost nastavit šířku/kopírování/posun atd., nastavte třídu CSS. `pb-always-mark` bude prvek vybrán a zobrazí se zelené pole s jeho možnostmi.

Stylování podle třídy s předponou: `pb-style-column-`.

```html
<div class="col-12 pb-style-content-person-26"></div>
```

Nastavením třídy CSS `pb-not-column` prvek \**nebudou považovány za sloupce* i když má třídu CSS `col-`.

## Výjimky při úpravách

### Upravitelný prvek

Pokud je vyžadována úprava prvku, který není jedním z výše uvedených, je možné použít prvek s třídou `pb-editable`.

Tento prvek nebude stylovatelný pomocí nástroje Page Builder, ale bude upravitelný pomocí editoru CK.

```html
<div class="pb-editable"></div>
```

### Neupravitelný prvek

Pokud je vyžadován prvek, který nechcete upravovat, je možné použít třídu `pb-not-editable`.

Tím se zakáže inicializace editoru Page Builder a CK na prvku a všech prvcích v něm.

```html
<section class="pb-not-editable"></section>
```

## Jedinečné ID

Potřebujete-li v blocích použít jedinečné ID, můžete použít hodnotu `__ID__`, která je nahrazena náhodnou hodnotou (`timestamp`).

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

PageBuilder zavolá `windows` několik událostí. Můžete si je poslechnout takto:

```javascript
window.addEventListener("WJ.PageBuilder.gridChanged", function(e) {
    //
});
```

V současné době jsou podporovány následující události:
- `WJ.PageBuilder.loaded` - po nahrání stránky v editoru
- `WJ.PageBuilder.gridChanged` - změna v `gride`
- `WJ.PageBuilder.styleChange` - změna vlastností bloku (stylování)
- `WJ.PageBuilder.newElementAdded` - přidán nový prvek
- `WJ.PageBuilder.elementDuplicated` - duplikovaný prvek
- `WJ.PageBuilder.elementMoved` - přesunutý prvek
