# Field Type - elfinder/výběr souboru

Datové pole elfinder integruje výběr odkazu na soubor pomocí aplikace elfinder/soubory. Pole se zobrazuje jako textové pole, na konci má ikonu tužky. Klepnutím na ikonu se otevře dialogové okno s elfinderem/výběrem souboru.

Pro pole obsahující odkaz na obrázek se na začátku pole zobrazuje náhled obrázku.

Hodnota pole je zobrazena s šedým pozadím, pole ale není typu `disabled`, v případě potřeby lze hodnotu upravit, nebo vložit ze schránky. Šedá barva je zvolena, aby uživatele naváděla kliknout na ikonu výběru odkazu.

![](field-type-elfinder.png)

## Použití anotace

Anotace se používá jako `DataTableColumnType.ELFINDER`, přičemž je možné nastavit následující atributy:
- `className` - pokud obsahuje hodnotu `image` zobrazí se na začátku pole náhled zvoleného obrázku (nebo ikona prázdného obrázku, pokud obrázek ještě není zvolen)

Kompletní příklad anotace:

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

Implementace je v souboru [field-type-elfinder.js](../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-elfinder.js). Podle `className` generuje vhodný HTML kód input pole. Do proměnné `conf._prepend` ukládá odkaz na prepend element (`div.input-group-prepend .input-group-text`) s náhledem obrázku. Funkce `setValue(conf, val)` se používá k nastavení hodnoty pole a zároveň k nastavení náhledového obrázku (je-li typu `.jpg` nebo `.png`).

Náhledový obrázek je nastaven jako `background-image`, zároveň je `prepend` elementu nastavená CSS třída `has-image`.

Otevření okna elfinder-a je zajištěno voláním funkce [WJ.openElFinder](../frameworks/webjetjs.md#iframe-dialog) s nastaveným `callback` na funkci `setValue`.
