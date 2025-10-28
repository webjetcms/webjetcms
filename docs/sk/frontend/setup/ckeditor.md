# CKEditor

Pri editácii webových stránok je použitý [CKEditor s našimi vlastnými úpravami](https://github.com/webjetcms/libs-ckeditor4/) a rozšíreniami.

## Konfiguračné premenné

Podporované sú nasledovné konfiguračné premenné:

- `editorAutomaticWordClean` - ak je nastavené na `true`, tak je pri vkladaní textu automaticky vykonané čistenie HTML kódu. Používateľ má možnosť text vložiť ako čistý text.
- `editorFontAwesomeCssPath` - nastavenie cesty k [FontAwesome](../webpages/fontawesome/README.md).
- `ckeditor_toolbar` - nastavenie položiek nástrojovej lišty pre sekciu webové stránky, hodnoty sú v JSON formáte.
- `ckeditor_toolbar-standalone` - nastavenie položiek nástrojovej lišty pre vloženie editora do rôznych dátových tabuliek, hodnoty sú v JSON formáte.
- `ckeditor_removeButtons` - zoznam tlačidiel, ktoré chcete v editore schovať (nezobraziť), nie je potrebné upraviť nastavenie `toolbar`, stačí sem nastaviť čiarkou oddelený zoznam.

## PICTURE element

Ak potrebujete vo vašom projekte podporu pre `PICTURE` element stačí, keď do konfigurácie `ckeditor_toolbar` pridáte na vhodné miesto hodnotu `WebjetPicture`. Ikona sa automaticky pridá aj ak máte nastavenú vlastnú hodnotu v konfiguračnej premennej `ckeditor_pictureDialogBreakpoints`.

![](picture-element.png)

Hodnoty `breakpoints` je možné nastaviť cez konfiguračnú premennú `ckeditor_pictureDialogBreakpoints`, príklad:

```json
[
    {
        name: "Desktop",
        minWidth: 992,
        fallback: true
    },
    {
        name: "Tablet",
        minWidth: 768
    },
    {
        name: "Mobile",
        minWidth: 0
    },
    {
        name: "XL",
        minWidth: 1240
    },
    {
        name: "XXL",
        minWidth: 1380
    }
]
```

Hodnota, ktorá má nastavený atribút `fallback: true` sa vloží aj do záložného `IMG` elementu pre prehliadače, ktoré `PICTURE` nepoznajú.