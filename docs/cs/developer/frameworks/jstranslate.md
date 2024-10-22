# Překlady v souborech JavaScript

Abychom umožnili používání překladových klíčů v souborech JavaScript, vytvořili jsme samostatnou knihovnu, která zapouzdřuje získávání a používání překladových klíčů.

## Backendové služby REST

Služba REST pro získání překladových klíčů je implementována ve třídě [AdminPropRestController.java](../../../src/main/java/sk/iway/iwcm/admin/AdminPropRestController.java). Poskytuje tyto adresy:
- `/admin/rest/properties/{lng}/` - získání standardního seznamu klíčů pro jazyk zadaný v cestě `{lng}`.
- `/admin/rest/properties/{lng}/{prefix:.+}` - získání seznamu klíčů pro jazyk zadaný v cestě `{lng}` obsahující jak standardní klíče, tak klíče určené předponou `{prefix}`. V předponě lze zadat více klíčů oddělených čárkou (přidají se všechny klíče začínající zadanou předponou).
- `/admin/rest/properties/lastmodified/{lng}/` - získá datum poslední změny překladu.

### Standardní seznam klíčů

Služba REST vrací seznam klíčů podle následujících pravidel:
- klíč končící výrazem `.js`, například `datatables.error.system.js`
- klíče zadané v konfigurační proměnné `propertiesAdminKeys` který obsahuje seznam klíčů (každý klíč na novém řádku nebo oddělený čárkou). Přidá se dobře definovaný klíč, nebo pokud klíč končí znakem `*` jsou přidány všechny klíče začínající daným prefixem.

### Datum poslední změny

Pokud je při volání služby REST odeslán parametr. `since` typu timestamp je vrácený seznam klíčů:
- pokud od zadaného časového razítka nedošlo k žádné změně, je vrácen prázdný objekt
- pokud dojde ke změně, jsou vráceny všechny klíče a klíč s názvem `lastmodified` obsahující časové razítko poslední změny na serveru.

Datum poslední změny je také v [head.pug](../../../src/main/webapp/admin/v9/views/partials/head.pug) k dispozici jako `window.propertiesLastModified`. Tím se ušetří jedno volání služby REST, pokud není třeba klíče aktualizovat.

### Překladové texty

Texty překladů pro WebJET 2021 jsou v souboru [text-webjet9.properties](../../../src/main/webapp/WEB-INF/classes/text-webjet9.properties), je třeba do tohoto souboru přidat nové překladové klíče.

Pro nové klíče doporučujeme použít příponu `.js` jménem klíče pro jeho automatické zahrnutí do odpovědi služby REST (např. `datatables.error.system.js`).

Pokud je to možné, je vhodné znovu použít stávající překladové klíče z WebJETu 8 - ušetříte tak náklady na překlad nových textů. Tyto klíče najdete v souboru [translations.properties](../../../src/main/webapp/files/preklady.properties). Tento soubor neupravujte, je to jen statický seznam překladových klíčů z WebJET 8.

Při použití existujícího klíče musí být jeho předpona nastavena v konfigurační proměnné s názvem `propertiesAdminKeys` přidat klíč do odpovědi služby REST.

**Poznámka:** pokud překládáte existující stránku z WebJETu verze 8, můžete ji zobrazit pomocí parametru URL. `?showTextKeys=true` což způsobí, že se před textem zobrazí překladové klávesy. Stránka bude z hlediska designu pravděpodobně nefunkční (protože text bude příliš dlouhý), ale na klíče se můžete podívat prostřednictvím inspektora.

## Frontendová knihovna

Knihovna [Translator](../libraries/translator.md) implementuje převod překladového klíče na text pomocí systémových překladů v systému WebJET.

Chcete-li zajistit překlad, stačí zavolat funkci `WJ.translate('translation.key')`:

```javascript
const preklad = WJ.translate('translation.key');
```

Pokud potřebujete v textu překladu použít parametry, můžete použít výraz `{1}` na `{X}` pro vložení parametrů:

```javascript
const preklad = WJ.translate(key, ...params);
```

Např. pro klíč `datatables.pageLength.auto.js=Automaticky ({1})` použít volání `var pageLengthTitle = WJ.translate("datatables.pageLength.auto.js", pageLength);` který zobrazí text, např. `Automaticky (14)`.
