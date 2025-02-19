# Preklady v JavaScript súboroch

Pre možnosť použitia prekladových kľúčov v JavaScript súboroch sme vytvorili samostatnú knižnicu zapúzdrujúcu získanie a použitie prekladových kľúčov.

## Backend REST služby

REST služba získania prekladových kľúčov je implementovaná v triede [AdminPropRestController.java](../../../../src/main/java/sk/iway/iwcm/admin/AdminPropRestController.java). Poskytuje nasledovné adresy:

- ```/admin/rest/properties/{lng}/``` - získanie štandardného zoznamu kľúčov pre jazyk zadaný v ceste ```{lng}```.
- ```/admin/rest/properties/{lng}/{prefix:.+}``` - získanie zoznamu kľúčov pre jazyk zadaný v ceste ```{lng}``` obsahujúci štandardné kľúče aj kľúče zadané prefixom ```{prefix}```. V prefixe môže byť zadaných viacero kľúčov oddelených čiarkou (pridajú sa všetky kľúče začínajúce na zadaný prefix).
- ```/admin/rest/properties/lastmodified/{lng}/``` - získa dátum poslednej zmeny prekladov.

### Štandardný zoznam kľúčov

REST služba vráti zoznam kľúčov podľa nasledovných pravidiel:

- kľúč končiaci na výraz ```.js```, napríklad ```datatables.error.system.js```
- kľúče zadané v konfiguračnej premennej ```propertiesAdminKeys``` v ktorej je zoznam kľúčov (každý kľúč na novom riadku, alebo oddelený čiarkou). Pridaný je presne definovaný kľúč, alebo ak kľúč končí na znak ```*``` pridané sú všetky kľúče začínajúce na daný prefix.

### Dátum poslednej zmeny

Ak je pri volaní REST služby odoslaný parameter ```since``` typu timestamp je vrátený zoznam kľúčov:

- ak nenastala zmena od zadaného timestampu je vrátený prázdny objekt
- ak nastala zmena, sú vrátené všetky kľúče a zároveň je vrátený kľúč s názvom ```lastmodified``` v ktorom je timestamp poslednej zmeny na serveri

Dátum poslednej zmeny je zároveň v [head.pug](../../../../src/main/webapp/admin/v9/views/partials/head.pug) dostupný ako ```window.propertiesLastModified```. Ušetrí sa tým jedno volanie REST služby, ak kľúče nie je potrebné aktualizovať.

### Prekladové texty

Prekladové texty pre WebJET 2021 sú v súbore [text-webjet9.properties](../../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties), do tohto súboru je potrebné dopĺňať nové prekladové kľúče.

Pri nových kľúčoch odporúčame používať príponu ```.js``` v mene kľúča pre jeho automatické zaradenie do odpovedi REST služby (napr. ```datatables.error.system.js```).

Ak je to možné, je dobré znova použiť už existujúce prekladové kľúče z WebJET 8 - ušetria sa tak náklady na preklad nových textov. Tie je možné nájsť v súbore [text.properties](../../../../src/main/webapp/WEB-INF/classes/text.properties). Tento súbor nemodifikujte, je to len statický zoznam prekladových kľúčov z WebJET 8.

Pri použití existujúceho kľúča je potrebné jeho prefix nastaviť do konfiguračnej premennej s názvom ```propertiesAdminKeys``` aby bol kľúč pridaný do odpovedi REST služby.

**Poznámka:** ak prekladáte existujúcu stránku z WebJET verzie 8 môžete si ju zobraziť s URL parametrom ```?showTextKeys=true``` čo spôsobí zobrazenie prekladových kľúčov pred textom. Stránka bude pravdepodobne rozbitá z dizajnového pohľadu (keďže texty budú príliš dlhé), ale cez inšpektor sa viete na kľúče pozrieť.

## Frontend knižnica

Knižnica [Translator](../libraries/translator.md) implementuje prevod prekladového kľúča na text s využitím systémových prekladov vo WebJETe.

Preklad zabezpečíte jednoduchým volaním funkcie ```WJ.translate('translation.key')```:

```javascript
const preklad = WJ.translate('translation.key');
```

Ak potrebujete v prekladovom texte použiť parametre môžete použiť výraz ```{1}``` až ```{X}``` pre vloženie parametrov:

```javascript
const preklad = WJ.translate(key, ...params);
```

Napr. pre kľúč ```datatables.pageLength.auto.js=Automaticky ({1})``` použijete volanie ```var pageLengthTitle = WJ.translate("datatables.pageLength.auto.js", pageLength);``` ktoré zobrazí text napr. ```Automaticky (14)```.