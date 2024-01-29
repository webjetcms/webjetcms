# Page Builder

## Inicializácia editora

Pri editácii stránky sa editor inicializuje na jednotlivé editovateľné bloky, čiže **v jednom čase je v stránke inicializovaných viacero editorov**. Cez Page Builder sa upravuje štruktúra stránky pomocou jeho nástrojov na prácu s blokmi. Cez CK editor sa upravujú samotné texty.

Editor sa automaticky inicializuje na:

- column elementy (```class="col-*"```)
- elementy označené CSS triedou ```pb-editable``` (```class="pb-editable"```)

**Nastavenie a konfigurácia:**

Pre správne spustenie Page Builder nastavte:

- vo vlastnostiach skupiny šablón Typ editora stránok na hodnotu Page Builder
- ak chcete, aby bol dostupný Page Builder aj pri zobrazení stránky na frontende nastavte konfiguračnú premennú ```inlineEditingEnabled``` na ```true```
- v prípade neštandardného ```bootstrap gridu```, konfiguračnú premennú ```bootstrapColumns```, ktorá určuje počet stĺpcov ```gridu```

Ďalšie konf. premenné, ktoré je možné upraviť:

- ```inlineEditableObjects``` - objekty, na ktorá sa okrem doc_data aplikuje editácia (napr. doc_header, doc_footer, doc_right_menu)
- ```inlineEditingEnabledDefaultValue``` - po nastavení na ```true``` sa automaticky zapne editačný režim pri načítaní stránky (bez potreby kliknút na Editovať)
- ```inlineEditingDisabledUrls``` - zoznam URL adries, pre ktoré nebude dostupný inline editor
- ```pageBuilderPrefix``` - prefix, ktorý sa používa pre CSS triedy Page Builder (predvolene pb), zmeniť je možné len ak zmeníte aj prefixy v CSS triedach Page Builder

**Výnimky**

Režim sa nastavuje pre skupinu šablón, ak potrebujete v niektorej **konkrétnej šablóne režim vypnúť** môžete v šablóne v karte Základné nastaviť inú možnosť v poli `Typ editora stránok` ako používa skupina šablón.

**Hlavičky/pätičky**

Page Builder v editore je inicializovaný na celú stránku, načíta sa vrátane CSS štýlov a šablóny do iframe elementu. V tomto režime ale nechceme zobrazovať ostatné elementy web stránky (hlavičku, pätičku...) preto sú tieto elementy nastavené na prázdnu hodnotu. Naviac pri zobrazení stránky sú cez CSS atribút ```display: none``` schované nasledovné elementy:

```css
 body div.header, body header, body div.footer, body footer { display: none; }
```

## Konvencia CSS tried s podporou pre Ninja page builder

Korektná štruktúra html kódu, nad ktorou sa dokáže Page Builder inicializovať, je nasledovná:

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

Štýlovanie je odporúčané len na ```section```, ```container``` a ```column content```, pretože len tieto elementy si vie používateľ upraviť pomocou Page Builder.

**POZOR:** v ```column``` elementoch nie je povolené použiť priamo text, je potrebné použiť minimálne P element.
Naviac z dôvodu možnosti nastavenia odsadení (```margin/padding```) v rámci ```column``` elementu je obsah elementu po otvorení v Page Builder-i obalený do DIV elementu s CSS triedou ```column-content```.

Čiže z kódu:

```html
<div class="col-4">
   Some content
</div>
```

vznikne po inicializácii Page Builder kód:

```html
<div class="col-4">
    <div class="column-content">
        <p>Some content</p>
    </div>
</div>
```

## Štýlovanie elementov

### SECTION (modrá farba)

Inicializácia pri použití elementu: ```<section>```.

Štýlovanie pomocou triedy, s prefixom: ```pb-style-section-```

```html
<section class="pb-style-section-team-26"></section>
```

Nastavením CSS triedy ```pb-not-section``` sa element **nebude považovať za section* element.

### CONTAINER (červená farba)

Inicializácia pri použití CSS triedy: ```container``` alebo ```pb-custom-container```. Nastavením CSS triedy ```pb-not-container``` sa element **nebude považovať za kontajner** aj keď má CSS triedu ```container```.

Štýlovanie pomocou triedy, s prefixom: ```pb-style-container-```

```html
<div class="container pb-style-container-group-26"></div>
```

### ROW

```<div class="row">``` sa momentálne nedá editovať pomocou Page Builder, je použitý z dôvodu bootstrap kompatibility.

### COLUMN (zelená farba)

Inicializácia pri použití triedy: ```col-``` ALEBO ```pb-col-``` (ak DIV element nie je štandardný bootstrap ```col-```). Akceptované sú aj hodnoty ```pb-col``` a ```pb-col-auto``` - ak element obsahuje tieto CSS štýly, tak v nástrojovej lište sa nezobrazí ikona nastavenia šírky stĺpca.

Ak má column CSS triedu ```pb-not-editable``` tak sa **nebude považovať za column** (nebude v ňom automaticky aj editovateľný text). Ak pre ne-editovateľný element potrebujete mať možnosť nastavovania šírky/kopírovania/presúvania atď. nastavením CSS triedy ```pb-always-mark``` sa element označí a bude sa zobrazovať zelený rámik aj s jeho možnosťami.

Štýlovanie pomocou triedy, s prefixom: ```pb-style-column-```.

```html
<div class="col-12 pb-style-content-person-26"></div>
```

Nastavením CSS triedy ```pb-not-column``` sa element **nebude považovať za columns* aj keď má CSS triedu ```col-```.

## Výnimky editácie

### Editovateľný element

Ak sa vyžaduje editovanie elementu, ktorý nie je z pomedzi vyššie spomenutých, je možné použiť element s triedou ```pb-editable```.

Tento element sa nebude dať naštýlovať pomocou Page Builder, ale bude editovateľný pomocou CK editora.

```html
<div class="pb-editable"></div>
```

### Ne-editovateľný element

Ak sa vyžaduje element, ktorý nechcete, aby bol editovateľný, je možné použiť triedu ```pb-not-editable```.

To zakáže inicializáciu Page Builder a CK editora na daný element a všetky elementy v ňom.

```html
<section class="pb-not-editable"></section>
```

## Unikátne ID

Ak potrebujete, aby v blokoch bolo použité unikátne ID môžete použiť hodnotu ```__ID__```, ktorá sa v HTML kóde po vložení do stránky nahradí za náhodnú hodnotu (```timestamp```).

```html
<div class="carousel slide" id="carouselControls__ID__" data-bs-ride="carousel">
    <div class="carousel-inner row">
        ...
    </div>
    <button class="carousel-control-prev" data-bs-slide="prev" data-bs-target="#carouselControls__ID__" type="button"></button>
    <button class="carousel-control-next" data-bs-slide="next" data-bs-target="#carouselControls__ID__" type="button"></button>
</div>
```

## Udalosti

PageBuilder počas práce vyvolá na ```windows``` objekte viaceré udalosti. Počúvať na ne môžete nasledovne:

```javascript
window.addEventListener("WJ.PageBuilder.gridChanged", function(e) {
    //
});
```

Aktuálne sú podporované nasledovné udalosti:

- ```WJ.PageBuilder.loaded``` - po nahratí stránky v editore
- ```WJ.PageBuilder.gridChanged``` - zmena v ```gride```
- ```WJ.PageBuilder.styleChange``` - zmena vo vlastnostiach bloku (štýlovanie)
- ```WJ.PageBuilder.newElementAdded``` - pridaný nový element
- ```WJ.PageBuilder.elementDuplicated``` - duplikovaný element
- ```WJ.PageBuilder.elementMoved``` - presunutý element

