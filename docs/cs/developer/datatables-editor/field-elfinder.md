# Typ pole - výběr elfinderu/souboru

Datové pole elfinder integruje výběr odkazu na soubor pomocí aplikace elfinder/files. Pole se zobrazuje jako textové pole s ikonou tužky na konci. Kliknutím na ikonu se otevře dialogové okno pro výběr elfinderu/souboru.

U pole obsahujícího odkaz na obrázek se na začátku pole zobrazí náhled obrázku.

Hodnota pole se zobrazí s šedým pozadím, ale pole není typu `disabled`, lze hodnotu v případě potřeby upravit nebo vložit ze schránky. Šedá barva je zvolena jako vodítko pro uživatele, který má kliknout na ikonu výběru odkazu.

![](field-type-elfinder.png)

## Použití anotace

Anotace se používá jako `DataTableColumnType.ELFINDER` a lze nastavit následující atributy:
- `className` - pokud obsahuje hodnotu `image` na začátku pole se zobrazí náhled vybraného obrázku (nebo prázdná ikona obrázku, pokud obrázek ještě není vybrán).

Příklad úplné anotace:

```java
//vyber obrazka = className="image"
@DataTableColumn(inputType = DataTableColumnType.ELFINDER, title="editor.perex.image",
    tab = "perex", sortAfter = "perexPlace",
    className = "image",
)
String perexImage = "";

//vyber odkazu na subor/inu web stranku = className je prazdne/nenastavene
@DataTableColumn(
        inputType = DataTableColumnType.ELFINDER,
        title = "[[#{editor.external_link}]]",
        tab = "basic"
)
private String externalLink;
```

## Poznámky k implementaci

Implementace je v souboru [field-type-elfinder.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-elfinder.js). Podle `className` vygeneruje příslušný kód HTML pro vstupní pole. Do proměnné `conf._prepend` ukládá odkaz na prvek prepend (`div.input-group-prepend .input-group-text`) s náhledovým obrázkem. Funkce `setValue(conf, val)` slouží k nastavení hodnoty pole a také k nastavení náhledového obrázku (pokud je typu `.jpg` nebo `.png`).

Náhledový obrázek je nastaven jako `background-image`, zároveň je `prepend` nastavit třídu CSS prvku `has-image`.

Otevření okna elfinderu se provádí voláním funkce [WJ.openElFinder](../frameworks/webjetjs.md#dialog-iframe) se sadou `callback` fungovat `setValue`.
