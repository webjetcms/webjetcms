# Field Type - elfinder/výber súboru

Dátové pole elfinder integruje výber odkazu na súbor pomocou aplikácie elfinder/súbory. Pole sa zobrazuje ako textové pole, na konci má ikonu ceruzky. Kliknutím na ikonu sa otvorí dialógové okno s elfinderom/výberom súboru.

Pre pole obsahujúce odkaz na obrázok sa na začiatku pola zobrazuje náhľad obrázku.

Hodnota pola je zobrazená so šedým pozadím, pole ale nie je typu ```disabled```, v prípade potreby je možné hodnotu upraviť, alebo vložiť zo schránky. Šedá farba je zvolená, aby používateľa navádzala kliknúť na ikonu výberu odkazu.

![](field-type-elfinder.png)

## Použitie anotácie

Anotácia sa používa ako ```DataTableColumnType.ELFINDER```, pričom je možné nastaviť nasledovné atribúty:

- ```className``` - ak obsahuje hodnotu ```image``` zobrazí sa na začiatku poľa náhľad zvoleného obrázku (alebo ikona prázdneho obrázku, ak obrázok ešte nie je zvolený)

Kompletný príklad anotácie:

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

## Nastavenie

Pomocou atribútu `className` je možné nastaviť doplnkové filtrovanie zobrazených súborov:

- `image` - zobrazia sa len súbory, ktorých `mime-type` začína na `image/`.
- `video` - zobrazia sa len súbory, ktorých `mime-type` začína na `video/`.
- - `multimedia` - zobrazia sa len súbory, ktorých `mime-type` začína na `image/` alebo `video/`.

Nastavenie `mime-type` je čítané zo súboru [mime.types](../../../../src/main/webapp/WEB-INF/mime.types).

## Poznámky k implementácii

Implementácia je v súbore [field-type-elfinder.js](../../../../src/main/webapp/admin/v9/npm_packages/webjetdatatables/field-type-elfinder.js). Podľa ```className``` generuje vhodný HTML kód input poľa. Do premennej ```conf._prepend``` ukladá odkaz na prepend element (```div.input-group-prepend .input-group-text```) s náhľadom obrázka. Funkcia ```setValue(conf, val)``` sa používa na nastavenie hodnoty poľa a zároveň na nastavenie náhľadového obrázku (ak je typu ```.jpg``` alebo ```.png```).

Náhľadový obrázok je nastavený ako ```background-image```, zároveň je ```prepend``` elementu nastavená CSS trieda ```has-image```.

Otvorenie okna elfinder-a je zabezpečené volaním funkcie [WJ.openElFinder](../frameworks/webjetjs.md#iframe-dialóg) s nastaveným ```callback``` na funkciu ```setValue```.