# Překlady v JavaScript souborech

Pro možnost použití překladových klíčů v JavaScript souborech jsme vytvořili samostatnou knihovnu zapouzdřující získání a použití překladových klíčů.

## Backend REST služby

REST služba získání překladových klíčů je implementována ve třídě [AdminPropRestController.java](../../../../src/main/java/sk/iway/iwcm/admin/AdminPropRestController.java). Poskytuje následující adresy:
- `/admin/rest/properties/{lng}/` - získání standardního seznamu klíčů pro jazyk zadaný v cestě `{lng}`.
- `/admin/rest/properties/{lng}/{prefix:.+}` - získání seznamu klíčů pro jazyk zadaný v cestě `{lng}` obsahující standardní klíče i klíče zadané prefixem `{prefix}`. V prefixu může být zadáno více klíčů oddělených čárkou (přidají se všechny klíče začínající na zadaný prefix).
- `/admin/rest/properties/lastmodified/{lng}/` - získá datum poslední změny překladů.

### Standardní seznam klíčů

REST služba vrátí seznam klíčů podle následujících pravidel:
- klíč končící na výraz `.js`, například `datatables.error.system.js`
- klíče zadané v konfigurační proměnné `propertiesAdminKeys` ve které je seznam klíčů (každý klíč na novém řádku, nebo oddělen čárkou). Přidán je přesně definovaný klíč, nebo pokud klíč končí na znak `*` přidány jsou všechny klíče začínající na daný prefix.

### Datum poslední změny

Pokud je při volání REST služby odeslán parametr `since` typu timestamp je vrácen seznam klíčů:
- pokud nenastala změna od zadaného timestampu je vrácen prázdný objekt
- pokud nastala změna, jsou vráceny všechny klíče a zároveň je vrácen klíč s názvem `lastmodified` ve kterém je timestamp poslední změny na serveru

Datum poslední změny je zároveň v [head.pug](../../../../src/main/webapp/admin/v9/views/partials/head.pug) dostupný jako `window.propertiesLastModified`. Ušetří se tím jedno volání REST služby, pokud klíče není třeba aktualizovat.

### Překladové texty

Překladové texty pro WebJET 2021 jsou v souboru [text-webjet9.properties](../../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties), do tohoto souboru je třeba doplňovat nové překladové klíče.

U nových klíčů doporučujeme používat příponu `.js` jménem klíče pro jeho automatické zařazení do odpovědi REST služby (např. `datatables.error.system.js`).

Pokud je to možné, je dobré znovu použít již existující překladové klíče z WebJET 8 – ušetří se tak náklady na překlad nových textů. Ty lze nalézt v souboru [text.properties](../../../../src/main/webapp/WEB-INF/classes/text.properties). Tento soubor nemodifikujte, je to jen statický seznam překladových klíčů z WebJET 8.

Při použití existujícího klíče je třeba jeho prefix nastavit do konfigurační proměnné s názvem `propertiesAdminKeys` aby byl klíč přidán do odpovědi REST služby.

**Poznámka:** pokud překládáte existující stránku z WebJET verze 8 můžete si ji zobrazit s URL parametrem `?showTextKeys=true` což způsobí zobrazení překladových klíčů před textem. Stránka bude pravděpodobně rozbitá z designového pohledu (jelikož texty budou příliš dlouhé), ale přes inspektor se umíte na klíče podívat.

## Frontend knihovna

Knihovna [Translator](../libraries/translator.md) implementuje převod překladového klíče na text s využitím systémových překladů ve WebJETu.

Překlad zajistíte jednoduchým voláním funkce `WJ.translate('translation.key')`:

```javascript
const preklad = WJ.translate('translation.key');
```

Pokud potřebujete v překladovém textu použít parametry můžete použít výraz `{1}` až `{X}` pro vložení parametrů:

```javascript
const preklad = WJ.translate(key, ...params);
```

Např. pro klíč `datatables.pageLength.auto.js=Automaticky ({1})` použijete volání `var pageLengthTitle = WJ.translate("datatables.pageLength.auto.js", pageLength);` které zobrazí text Např. `Automaticky (14)`.
