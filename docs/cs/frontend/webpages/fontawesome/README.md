# FontAwesome

WebJET v editoru podporuje vkládání ikon ze sady [FontAwesome](https://fontawesome.com) klepnutím na ikonu ![](editor-toolbar-icon.png ":no-zoom"), což vyvolá zobrazení okna pro vložení ikony ze sady FontAwesome do stránky.

![](editor.png)

Ikony lze vyhledávat podle jejich názvu a kliknutím na OK se ikona vloží do webové stránky.

## Aktivace rozšíření

Rozšíření se aktivuje nastavením cesty k CSS souborům `FontAwesome` přes konfigurační proměnnou `editorFontAwesomeCssPath` do které nastavíte cestu k CSS souborům. Více souborů lze zadat na nový řádek, například:

```
/templates/jet/assets/fontawesome/css/fontawesome.css
/templates/jet/assets/fontawesome/css/solid.css
```

Po nastavení se v editoru stránek v nástrojové liště začne zobrazovat ikona pro vložení FontAwesome ikony. Samozřejmě, cesty k CSS stylům musí být nastaveny také v šabloně web stránky, aby se ikona zobrazila korektně. Stejné cesty nastavte do pole **Hlavní CSS styl** šablony.

## Doplňkové ikony

Standardní seznam ikon obsahuje základní ikony vycházející ze sady verze 4. Pokud potřebujete vkládat ikony z novějších sad můžete je doplnit do konfigurační proměnné `editorFontAwesomeCustomIcons` ve formátu `css-name:text` každou na nový řádek, například:

```
fa-wand-magic-sparkles:Super Magic Wand
fa-wheelchair-move:Wheelchair Move
```
