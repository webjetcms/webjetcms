# Veřejné služby

WebJET nabízí několik "veřejných služeb" přes služby REST, které můžete využít při programování zákaznických aplikací.

Volání těchto služeb musí být povoleno, jinak jsou služby nedostupné. Povolení pro IP adresy se nastavuje pomocí konfiguračních proměnných:
- `restAllowedIpAddresses` - seznam povolených IP adres pro volání všech veřejných služeb.
- `restAllowedIpAddresses-DocRestController` - povolené IP adresy pro volání textové služby načtení webové stránky.
- `restAllowedIpAddresses-PropertiesRestController` - povolené IP adresy pro volání služby překladu klíčů

Zadání znaku `*` jsou povoleny všechny IP adresy, můžete zadat více počátečních IP adres oddělených čárkou, například: `192.168.,62.65.161.4,10.10.0.`.

Pokud máte vlastní službu REST, která rozšiřuje třídu [RestController](../../../../src/webjet8/java/sk/iway/iwcm/rest/RestController.java) název třídy se používá k získání speciálního klíče pro kontrolu oprávnění IP adres.

## Text webové stránky

Získat `DocDetails` objekt je obsluhován následujícím typem koncového bodu `GET` :

```java
    @RequestMapping(path={"/rest/documents/{param}/**"}, method=RequestMethod.GET)
```

Vstupní parametr je **id** nebo **virtuální cesta** objekt, který chcete získat.

Příklady použití:
- `/rest/documents/50124`
- `/rest/documents/en/gallery/kitchen`
- `/rest/documents/en/home-html`

## Překladové klíče

Služba vrací pouze ty překladové klíče, jejichž začátky jsou nastaveny v konfigurační proměnné `propertiesRestControllerAllowedKeysPrefixes` oddělené novým řádkem nebo čárkou. Ve výchozím nastavení je hodnota prázdná, před použitím je třeba nastavit předponu nebo znak `*` povolit všechny klávesy.

### Získání překladových textů

Pro získání textů překladu se použije koncový bod typu `GET` :

```java
    @RequestMapping(path={"/rest/properties/{lng}/{prefix:.+}"}, method=RequestMethod.GET)
```

Vstupní parametry jsou **Jazyk** (např. `sk`, `en`), ve kterém budou přeloženy hodnoty klíčů a počáteční/prefix samotných překladových klíčů.

Příklady použití:
- `/rest/properties/sk/components.abtesting`

Tento koncový bod vrací seznam párů jako mapu `Map<String, String>` kde každá dvojice se skládá z **klíč s předponou** a jeho **hodnota (překlad)**.

Příklad vrácených hodnot:
- `components.abtesting.dialog_title = AB testovanie`
- `components.abtesting.allowed = AB testovanie povolene`
- `components.abtesting.ratio = Pomer`
- `components.abtesting.variantName = Nazov varianty`
- `components.abtesting.example = Priklad`

### Získání překladů podle klíče

Získání překladů podle klíče s hodnotou ve formátu `Entry<String, String>` je použit následující typ koncového bodu `GET` :

```java
    @RequestMapping(path={"/rest/property/{lng}/{key:.+}/**"}, method=RequestMethod.GET)
```

Vstupní parametry jsou **Jazyk** (např. `sk`, `en`), který přeloží hodnotu klíče a **překladatelský klíč**. Pokud hodnota klíče překladu obsahuje proměnné `{0}, {1} ...` mohou být vyplněny dalšími proměnnými z cesty (z koncového bodu).

#### Příklady požití a návratových hodnot

Po zadání cesty `/rest/property/sk/calendar.invitation.saveok-A` je vrácena hodnota klíče překladu `calendar.invitation.saveok-A` ve slovenštině (`sk`) jazyk jako `Dakujeme za akceptovanie schodzky.`.

**Složitý příklad**

Pokud zadáte cestu jako `/rest/property/sk/converter.number.invalidNumber/4/test` chceme získat překladový klíč `converter.number.invalidNumber` s hodnotou přeloženou do jazyka `sk`. Hodnota klíče překladu je `Hodnota ({1}) v poli {0} musi byt cislo`.

Byly zadány 2 nepovinné parametry, a to `4` a `test`. Vzhledem k tomu, že parametr `4` byl první, který nahradil parametr `{0}` parametr `test` nahradí `{1}`.

Výsledná vrácená kombinace je `converter.number.invalidNumber` - `Hodnota (test) v poli 4 musi byt cislo`.
