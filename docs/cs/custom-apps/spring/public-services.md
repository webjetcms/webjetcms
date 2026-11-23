# Veřejné služby

WebJET nabízí několik „veřejných služeb“ přes REST služby, které můžete využít při programování zákaznických aplikací.

Volání těchto služeb musí být povoleno, jinak jsou služby nedostupné. Povolení pro IP adresy se nastavuje přes konfigurační proměnné:

- `restAllowedIpAddresses` - ​​společný seznam povolených IP adres pro veřejné služby, který se použije při prázdném nastavení konkrétního controlleru
- `restAllowedIpAddresses-DocRestController` - ​​povoleny IP adresy pro volání služby získání textu web stránky
- `restAllowedIpAddresses-PropertiesRestController` - ​​povolené IP adresy pro volání služby překladových klíčů
- `restAllowedIpAddresses-MarkdownSearchRestController` - ​​povolené IP adresy pro [vyhledávání v Markdown dokumentaci](../apps/rag/markdown-search.md)

Zadáním znaku `*` se povolí všechny IP adresy, můžete zadat několik začátků IP adres oddělených čárkou, například: `192.168.,62.65.161.4,10.10.0.`.

Máte-li vlastní REST službu, která rozšiřuje třídu [RestController](../../../../src/main/java/sk/iway/iwcm/rest/RestController.java) použije se jméno třídy pro získání speciálního klíče pro kontrolu povolení IP adresy.

## Text web stránky

K získání `DocDetails` objektu slouží následující koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/documents/{param}/**"}, method=RequestMethod.GET)
```

Vstupním parametrem je **id** nebo **virtuální cesta** objektu, který chcete získat.

Příklady použití:
- `/rest/documents/50124`
- `/rest/documents/en/gallery/kitchen`
- `/rest/documents/en/home-html`

## Vyhledávání v Markdown dokumentaci

Služba sémanticky vyhledává v nakonfigurovaných kořenech Markdown dokumentace. Vrací nalezené dokumenty s ukázkami textu a při zapnutém `ragAnswerAllowed` i odpověď AI sestavenou z přístupného obsahu.

Příklad volání koncového bodu typu `GET`:

```http
GET /rest/rag/markdown/search?query=How%20to%20edit%20a%20page&language=en
```

Parametry:

- `query` - ​​povinná otázka nebo hledaný text.
- `language` - ​​povinný kód jazyka dokumentace, například `sk`, `cs` nebo `en`.
- `directory` - ​​volitelné omezení na adresář včetně podadresářů, například `/en/redactor/`. Jazyk v cestě musí odpovídat parametru `language`.

Odpověď je JSON s polem `results`. Každý výsledek obsahuje `title`, `url`, `sourcePath`, `snippet` a `score`. Odpověď AI je v poli `answer` ; pokud není dostupná, má hodnotu `null`.

I po povolení IP adresy se ve výchozím nastavení vyžaduje přihlášení; postačuje běžný uživatelský účet. Pro anonymní vyhledávání nastavte navíc `ragMarkdownSearchRequireLogin=false`. Nadále platí přístupová práva k souborům a omezení počtu požadavků.

Po IP kontrole služba ověří dostupnost sémantického vyhledávání. Pokud není dostupné, vrátí HTTP `503` bez dalšího zpracování požadavku, i když existují výsledky v cache.

Nastavení kořenů, indexování a úplný formát odpovědi naleznete v dokumentaci [Vyhledávání v Markdown dokumentaci](../apps/rag/markdown-search.md).

## Překladové klíče

Služba vrátí pouze překladové klíče, jejichž začátky jsou nastaveny v konfigurační proměnné `propertiesRestControllerAllowedKeysPrefixes` odděleny novým řádkem nebo čárkou. Ve výchozím nastavení je hodnota prázdná, před použitím je třeba nastavit prefix, nebo znak `*` pro povolení všech klíčů.

### Získání překladových textů

K získání překladových textů slouží koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/properties/{lng}/{prefix:.+}"}, method=RequestMethod.GET)
```

Vstupními parametry jsou **jazyk** (např. `sk`, `en`), ve kterém budou přeloženy hodnoty klíče a samotný začátek/prefix překladových klíčů.

Příklady použití:
- `/rest/properties/sk/components.abtesting`

Tento koncový bod vrátí seznam párů jako mapu `Map<String, String>`, kde každý pár se skládá z **klíče s prefixem** a jeho **hodnotou (překladem)**.

Příklad vrácených hodnot:
- `components.abtesting.dialog_title = AB testovanie`
- `components.abtesting.allowed = AB testovanie povolene`
- `components.abtesting.ratio = Pomer`
- `components.abtesting.variantName = Nazov varianty`
- `components.abtesting.example = Priklad`

### Získání překladů podle klíče

K získání překladů podle klíče s hodnotou ve formátu `Entry<String, String>` slouží následující koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/property/{lng}/{key:.+}/**"}, method=RequestMethod.GET)
```

Vstupními parametry jsou **jazyk** (např. `sk`, `en`), ve kterém bude přeložena hodnota klíče a samotný **překladový klíč**. Pokud hodnota překladového klíče obsahuje proměnné `{0}, {1} ...` je možné je vyplnit dalšími proměnnými ze silnice (z koncového bodu).

#### Příklady požití a vrácených hodnot

Při zadání cesty `/rest/property/sk/calendar.invitation.saveok-A` se vrátí hodnota překladového klíče `calendar.invitation.saveok-A` v českém (`sk`) jazyce jako `Dakujeme za akceptovanie schodzky.`.

**Složitý příklad**

Při zadání cesty jako `/rest/property/sk/converter.number.invalidNumber/4/test` chceme získat překladový klíč `converter.number.invalidNumber` s hodnotou přeloženou do jazyka `sk`. Hodnota překladového klíče je `Hodnota ({1}) v poli {0} musi byt cislo`.

Zadané byly 2 volitelné parametry a to `4` a `test`. Protože parametr `4` byl první, nahradí parametr `{0}` a parametr `test` nahradí `{1}`.

Výsledná vrácená kombinace je `converter.number.invalidNumber` - ​​`Hodnota (test) v poli 4 musi byt cislo`.
