# FontAwesome

WebJET v editore podporuje vkladanie ikon zo sady [FontAwesome](https://fontawesome.com) kliknutím na ikonu ![](editor-toolbar-icon.png ":no-zoom"), čo vyvolá zobrazenie okna pre vloženie ikony zo sady FontAwesome do stránky.

![](editor.png)

Ikony je možné vyhľadávať podľa ich názvu a kliknutím na OK sa ikona vloží do web stránky.

## Aktivácia rozšírenia

Rozšírenie sa aktivuje nastavením cesty k CSS súborom `FontAwesome` cez konfiguračnú premennú `editorFontAwesomeCssPath` do ktorej nastavíte cestu k CSS súborom. Viac súborov je možné zadať na nový riadok, napríklad:

```
/templates/jet/assets/fontawesome/css/fontawesome.css
/templates/jet/assets/fontawesome/css/solid.css
```

Po nastavení sa v editore stránok v nástrojovej lište začne zobrazovať ikona pre vloženie FontAwesome ikony. Samozrejme, cesty k CSS štýlom musia byť nastavené aj v šablóne web stránky, aby sa ikona zobrazila korektne. Rovnaké cesty nastavte do poľa **Hlavný CSS štýl** šablóny.

## Doplnkové ikony

Štandardný zoznam ikon obsahuje základné ikony vychádzajúce zo sady verzie 4. Ak potrebujete vkladať ikony z novších sád môžete ich doplniť do konfiguračnej premennej `editorFontAwesomeCustomIcons` vo formáte `css-name:text` každú na nový riadok, napríklad:

```
fa-wand-magic-sparkles:Super Magic Wand
fa-wheelchair-move:Wheelchair Move
```
