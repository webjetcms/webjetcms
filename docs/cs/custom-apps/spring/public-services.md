# Veřejné služby

WebJET nabízí několik „veřejných služeb“ přes REST služby, které můžete využít při programování zákaznických aplikací.

Volání těchto služeb musí být povoleno, jinak jsou služby nedostupné. Povolení pro IP adresy se nastavuje přes konfigurační proměnné:
- `restAllowedIpAddresses` - seznam povolených IP adres pro volání všech veřejných služeb
- `restAllowedIpAddresses-DocRestController` - povolené IP adresy pro volání služby získání textu web stránky
- `restAllowedIpAddresses-PropertiesRestController` - povolené IP adresy pro volání služby překladových klíčů

Zadáním znaku `*` se povolí všechny IP adresy, můžete zadat několik začátků IP adres oddělených čárkou, například: `192.168.,62.65.161.4,10.10.0.`.

Máte-li vlastní REST službu, která rozšiřuje třídu [RestController](../../../../src/webjet8/java/sk/iway/iwcm/rest/RestController.java) použije se jméno třídy pro získání speciálního klíče pro kontrolu povolení IP adresy.

## Text web stránky

Na získání `DocDetails` objektu slouží následující koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/documents/{param}/**"}, method=RequestMethod.GET)
```

Vstupním parametrem je **id** nebo **virtuální cesta** objektu, který chcete získat.

Příklady použití:
- `/rest/documents/50124`
- `/rest/documents/en/gallery/kitchen`
- `/rest/documents/en/home-html`

## Překladové klíče

Služba vrátí pouze překladové klíče, jejichž začátky jsou nastaveny v konfigurační proměnné `propertiesRestControllerAllowedKeysPrefixes` odděleny novým řádkem nebo čárkou. Ve výchozím nastavení je hodnota prázdná, před použitím je třeba nastavit prefix nebo znak `*` pro povolení všech klíčů.

### Získání překladových textů

K získání překladových textů slouží koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/properties/{lng}/{prefix:.+}"}, method=RequestMethod.GET)
```

Vstupními parametry jsou **jazyk** (Např. `sk`, `en`), ve kterém budou přeloženy hodnoty klíče a samotný začátek/prefix překladových klíčů.

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

Pro získání překladů podle klíče s hodnotou ve formátu `Entry<String, String>` slouží následující koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/property/{lng}/{key:.+}/**"}, method=RequestMethod.GET)
```

Vstupními parametry jsou **jazyk** (Např. `sk`, `en`), ve kterém bude přeložena hodnota klíče a samotný **překladový klíč**. Pokud hodnota překladového klíče obsahuje proměnné `{0}, {1} ...` je možné je vyplnit dalšími proměnnými ze silnice (z koncového bodu).

#### Příklady požití a vrácených hodnot

Při zadání cesty `/rest/property/sk/calendar.invitation.saveok-A` se vrátí hodnota překladového klíče `calendar.invitation.saveok-A` v češtině (`sk`) jazyce jako `Dakujeme za akceptovanie schodzky.`.

**Složitý příklad**

Při zadání cesty jako `/rest/property/sk/converter.number.invalidNumber/4/test` chceme získat překladový klíč `converter.number.invalidNumber` s hodnotou přeloženou do jazyka `sk`. Hodnota překladového klíče je `Hodnota ({1}) v poli {0} musi byt cislo`.

Zadány byly 2 volitelné parametry a to `4` a `test`. Jelikož parametr `4` byl první, nahradí parametr `{0}` a parametr `test` nahradí `{1}`.

Výsledná vrácená kombinace je `converter.number.invalidNumber` - `Hodnota (test) v poli 4 musi byt cislo`.
