# Verejné služby

WebJET ponúka niekoľko "verejných služieb" cez REST služby, ktoré môžete využiť pri programovaní zákazníckych aplikácií.

Volanie týchto služieb musí byť povolené, inak sú služby nedostupné. Povolenie pre IP adresy sa nastavuje cez konfiguračné premenné:

- `restAllowedIpAddresses` - spoločný zoznam povolených IP adries pre verejné služby, ktorý sa použije pri prázdnom nastavení konkrétneho controllera
- `restAllowedIpAddresses-DocRestController` - povolené IP adresy pre volanie služby získania textu web stránky
- `restAllowedIpAddresses-PropertiesRestController` - povolené IP adresy pre volanie služby prekladových kľúčov
- `restAllowedIpAddresses-MarkdownSearchRestController` - povolené IP adresy pre [vyhľadávanie v Markdown dokumentácii](../apps/rag/markdown-search.md)

Zadaním znaku `*` sa povolia všetky IP adresy, môžete zadať viaceré začiatky IP adries oddelené čiarkou, napríklad: `192.168.,62.65.161.4,10.10.0.`.

Ak máte vlastnú REST službu, ktorá rozširuje triedu [RestController](../../../../src/main/java/sk/iway/iwcm/rest/RestController.java) použije sa meno triedy pre získanie špeciálneho kľúča pre kontrolu povolenia IP adresy.

## Text web stránky

Na získanie `DocDetails` objektu slúži nasledujúci koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/documents/{param}/**"}, method=RequestMethod.GET)
```

Vstupným parametrom je **id** alebo **virtuálna cesta** objektu, ktorý chcete získať.

Príklady použitia:
- `/rest/documents/50124`
- `/rest/documents/en/gallery/kitchen`
- `/rest/documents/en/home-html`

## Vyhľadávanie v Markdown dokumentácii

Služba sémanticky vyhľadáva v nakonfigurovaných koreňoch Markdown dokumentácie. Vracia nájdené dokumenty s ukážkami textu a pri zapnutom `ragAnswerAllowed` aj odpoveď AI zostavenú z prístupného obsahu.

Príklad volania koncového bodu typu `GET`:

```http
GET /rest/rag/markdown/search?query=How%20to%20edit%20a%20page&language=en
```

Parametre:

- `query` - povinná otázka alebo hľadaný text.
- `language` - povinný kód jazyka dokumentácie, napríklad `sk`, `cs` alebo `en`.
- `directory` - voliteľné obmedzenie na adresár vrátane podadresárov, napríklad `/en/redactor/`. Jazyk v ceste musí zodpovedať parametru `language`.

Odpoveď je JSON s poľom `results`. Každý výsledok obsahuje `title`, `url`, `sourcePath`, `snippet` a `score`. Odpoveď AI je v poli `answer`; ak nie je dostupná, má hodnotu `null`.

Aj po povolení IP adresy sa predvolene vyžaduje prihlásenie; postačuje bežný používateľský účet. Pre anonymné vyhľadávanie nastavte navyše `ragMarkdownSearchRequireLogin=false`. Naďalej platia prístupové práva k súborom a obmedzenie počtu požiadaviek.

Po IP kontrole služba overí dostupnosť sémantického vyhľadávania. Ak nie je dostupné, vráti HTTP `503` bez ďalšieho spracovania požiadavky, aj keď existujú výsledky v cache.

Nastavenie koreňov, indexovania a úplný formát odpovede nájdete v dokumentácii [Vyhľadávanie v Markdown dokumentácii](../apps/rag/markdown-search.md).

## Prekladové kľúče

Služba vráti len prekladové kľúče, ktorých začiatky sú nastavené v konfiguračnej premennej `propertiesRestControllerAllowedKeysPrefixes` oddelené novým riadkom alebo čiarkou. Predvolene je hodnota prázdna, pred použitím je potrebné nastaviť prefix, alebo znak `*` pre povolenie všetkých kľúčov.

### Získanie prekladových textov

Na získanie prekladových textov slúži koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/properties/{lng}/{prefix:.+}"}, method=RequestMethod.GET)
```

Vstupnými parametrami sú **jazyk** (napr. `sk`, `en`), v ktorom budú preložené hodnoty kľúča a samotný začiatok/prefix prekladových kľúčov.

Príklady použitia:
- `/rest/properties/sk/components.abtesting`

Tento koncový bod vráti zoznam párov ako mapu `Map<String, String>`, kde každý pár pozostáva z **kľúča s prefixom** a jeho **hodnotou (prekladom)**.

Príklad vrátených hodnôt:
- `components.abtesting.dialog_title = AB testovanie`
- `components.abtesting.allowed = AB testovanie povolene`
- `components.abtesting.ratio = Pomer`
- `components.abtesting.variantName = Nazov varianty`
- `components.abtesting.example = Priklad`

### Získanie prekladov podľa kľúča

Na získanie prekladov podľa kľúča s hodnotou vo formáte `Entry<String, String>` slúži nasledujúci koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/property/{lng}/{key:.+}/**"}, method=RequestMethod.GET)
```

Vstupnými parametrami sú **jazyk** (napr. `sk`, `en`), v ktorom bude preložená hodnota kľúča a samotný **prekladový kľúč**. Ak hodnota prekladového kľúča obsahuje premenné `{0}, {1} ...` je možné ich vyplniť ďalšími premennými z cesty (z koncového bodu).

#### Príklady požitia a vrátených hodnôt

Pri zadaní cesty `/rest/property/sk/calendar.invitation.saveok-A` sa vráti hodnota prekladového kľúča `calendar.invitation.saveok-A` v slovenskom (`sk`) jazyku ako `Dakujeme za akceptovanie schodzky.`.

**Zložitý príklad**

Pri zadaní cesty ako `/rest/property/sk/converter.number.invalidNumber/4/test` chceme získať prekladový kľúč `converter.number.invalidNumber` s hodnotou preloženou do jazyka `sk`. Hodnota prekladového kľúča je `Hodnota ({1}) v poli {0} musi byt cislo`.

Zadané boli 2 voliteľné parametre a to `4` a `test`. Keďže parameter `4` bol prvý, nahradí parameter `{0}` a parameter `test` nahradí `{1}`.

Výsledná vrátená kombinácia je `converter.number.invalidNumber` - `Hodnota (test) v poli 4 musi byt cislo`.
