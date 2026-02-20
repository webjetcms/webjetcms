# CKEditor

Při editaci webových stránek je použit [CKEditor s našimi vlastními úpravami](https://github.com/webjetcms/libs-ckeditor4/) a rozšířeními.

## Konfigurační proměnné

Podporovány jsou následující konfigurační proměnné:
- `editorAutomaticWordClean` - pokud je nastaveno na `true`, tak je při vkládání textu automaticky provedeno čištění HTML kódu. Uživatel má možnost text vložit jako čistý text.
- `editorFontAwesomeCssPath` - nastavení cesty k [FontAwesome](../webpages/fontawesome/README.md).
- `ckeditor_toolbar` - nastavení položek nástrojové lišty pro sekci webové stránky, hodnoty jsou v JSON formátu.
- `ckeditor_toolbar-standalone` - nastavení položek nástrojové lišty pro vložení editoru do různých datových tabulek, hodnoty jsou v JSON formátu.
- `ckeditor_removeButtons` - seznam tlačítek, která chcete v editoru schovat (nezobrazit), není třeba upravit nastavení `toolbar`, stačí sem nastavit čárkou oddělený seznam.

Nastavení pro tabulky:
- `ckeditor_table_class` - Výchozí CSS třída pro tabulky v CKEditoru, standardně `table table-sm tabulkaStandard`.
- `ckeditor_table_cols` - Výchozí počet sloupců tabulky v CKEditoru, standardně 5.
- `ckeditor_table_rows` - Výchozí počet řádků tabulky v CKEditoru, standardně 2.
- `ckeditor_table_width` - Výchozí šířka tabulky v CKEditoru, standardně 100%.
- `ckeditor_table_height` - Výchozí výška tabulky v CKEditoru.
- `ckeditor_table_border` - Výchozí hodnota okraje tabulky v CKEditoru, standardně 1.
- `ckeditor_table_cellpadding` - Výchozí hodnota `cellpadding` tabulky v CKEditoru, standardně 1.
- `ckeditor_table_cellspacing` - Výchozí hodnota `cellspacing` tabulky v CKEditoru, standardně 1.
- `ckeditor_table_wrapper_class` - CSS třída obalovače pro tabulku v CKEditoru, standardně `table-responsive`. Pokud je prázdné, tabulka se nebude obalovat do responzivního kontejneru.

## PICTURE element

Pokud potřebujete ve vašem projektu podporu pro `PICTURE` element, stačí, když do konfigurace `ckeditor_toolbar` přidáte na vhodné místo hodnotu `WebjetPicture`. Ikona se automaticky přidá i pokud máte nastavenou vlastní hodnotu v konfigurační proměnné `ckeditor_pictureDialogBreakpoints`.

![](picture-element.png)

Hodnoty `breakpoints` lze nastavit přes konfigurační proměnnou `ckeditor_pictureDialogBreakpoints`, příklad:

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

Hodnota, která má nastavený atribut `fallback: true` se vloží i do záložního `IMG` elementu pro prohlížeče, které `PICTURE` neznají.

## SVG ikony

CKEditor podporuje použití SVG ikon namísto klasických ikon ve formátu PNG. Pro zapnutí této funkce je třeba nastavit konfigurační proměnnou `ckeditor_svgIcon_path` na cestu k SVG sprite souboru, který obsahuje definice ikon.

![](svgicon.png)

Volitelně můžete nastavit i další konfigurační proměnné pro přizpůsobení ikon:
- `ckeditor_svgIcon_icons` - JSON objekt definující ikony pro CKEditor, viz příklad níže. Pokud je prázdné získá se ze SVG souboru podle hodnoty ID elementů, skupiny v takovém případě nejsou dostupné.
- `ckeditor_svgIcon_width` - Šířka SVG ikony v bodech.
- `ckeditor_svgIcon_height` - Výška SVG ikony v bodech.
- `ckeditor_svgIcon_sizes` - Dostupné velikosti ikon oddělené čárkou, například. `small,medium,large,xlarge,xxlarge,huge`.
- `ckeditor_svgIcon_colors` - Dostupné barvy ikon oddělené čárkou, například. `info,success,warning,danger,orange`.

Příklad nastavení `ckeditor_svgIcon_icons` včetně skupin:

```json
{
    "4g": ["other"],
    "5g": ["other"],
    "airbox-auto": ["vehicles", "waves"],
    "alias-numbers": ["people"],
    "android-manage": ["other"],
}
```

SVG soubor musí obsahovat definice ikon s nastaveným ID atributem, příklad:

```xml
<?xml version="1.0" encoding="utf-8"?>
<svg
	xmlns="http://www.w3.org/2000/svg"
	xmlns:xlink="http://www.w3.org/1999/xlink">
	<symbol viewBox="0 0 80 80" id="4g"
		xmlns="http://www.w3.org/2000/svg">
		<path fill-rule="evenodd" clip-rule="evenodd" d="M22.56 29.6 12.32 43.36h10.4V29.6h-.16ZM6 50v-7.44l17.2-23.04h7.2v23.76h5.2v6.64h-5.2v9.2h-7.68V50H6Zm56.102 8.809A15.027 15.027 0 0 1 56.24 60c-3.092 0-5.837-.512-8.306-1.587-2.47-1.075-4.553-2.554-6.25-4.44-1.697-1.886-2.998-4.1-3.903-6.646-.905-2.545-1.357-5.288-1.357-8.23 0-3.016.453-5.815 1.357-8.399.905-2.582 2.206-4.835 3.903-6.758 1.697-1.924 3.78-3.432 6.25-4.525 2.47-1.093 5.25-1.64 8.343-1.64a19.63 19.63 0 0 1 6.024.933c1.9.6 3.68 1.529 5.26 2.743a15.043 15.043 0 0 1 3.874 4.468c1.018 1.773 1.64 3.81 1.867 6.109h-8.485c-.529-2.262-1.547-3.96-3.054-5.09-1.509-1.132-3.338-1.697-5.487-1.697-1.999 0-3.696.386-5.09 1.16a9.607 9.607 0 0 0-3.394 3.11c-.867 1.3-1.5 2.78-1.895 4.44a22.105 22.105 0 0 0-.593 5.147c-.004 1.668.196 3.33.593 4.949a13.352 13.352 0 0 0 1.895 4.327 9.696 9.696 0 0 0 3.394 3.082c1.394.773 3.091 1.16 5.09 1.16 2.941 0 5.213-.745 6.815-2.235 1.603-1.489 2.565-3.633 2.829-6.461h-8.96v-6.64h17.12L74 59.12h-5.76l-.878-4.581c-1.584 2.036-3.337 3.46-5.26 4.27Z"/>
	</symbol>
	<symbol viewBox="0 0 80 80" id="5g"
		xmlns="http://www.w3.org/2000/svg">
		<path fill-rule="evenodd" clip-rule="evenodd" d="m14.905 26.135-1.723 9.807.123.123a10.882 10.882 0 0 1 3.826-2.56 12.98 12.98 0 0 1 4.629-.77c2.138 0 4.03.389 5.673 1.17a12.52 12.52 0 0 1 4.167 3.146c1.127 1.316 1.993 2.873 2.59 4.658.603 1.84.908 3.767.894 5.71a14.909 14.909 0 0 1-1.295 6.196 15.805 15.805 0 0 1-3.487 4.967 15.41 15.41 0 0 1-5.091 3.236 15.878 15.878 0 0 1-6.167 1.08 21.46 21.46 0 0 1-6.015-.833 15.01 15.01 0 0 1-5.09-2.56 12.826 12.826 0 0 1-3.546-4.287c-.891-1.705-1.35-3.69-1.393-5.953h8.764c.203 1.975.945 3.55 2.218 4.72 1.273 1.171 2.898 1.757 4.873 1.757 1.152 0 2.189-.236 3.116-.71a7.394 7.394 0 0 0 2.345-1.85 8.329 8.329 0 0 0 1.48-2.655 9.891 9.891 0 0 0 .03-6.261 7.37 7.37 0 0 0-1.48-2.586A6.965 6.965 0 0 0 22 39.953a7.555 7.555 0 0 0-3.145-.618c-1.564 0-2.837.276-3.826.832-.985.557-1.913 1.43-2.774 2.618H4.356l4.255-23.869h24.123v7.219H14.905ZM64.2 61.753a16.235 16.235 0 0 1-6.353 1.327c-3.374 0-6.407-.585-9.102-1.76a19.504 19.504 0 0 1-6.818-4.84 21.426 21.426 0 0 1-4.254-7.247 26.61 26.61 0 0 1-1.48-8.978c0-3.288.49-6.342 1.48-9.16a22.019 22.019 0 0 1 4.254-7.371 19.441 19.441 0 0 1 6.819-4.935c2.689-1.193 5.726-1.789 9.1-1.789 2.229-.004 4.445.34 6.568 1.018a18.562 18.562 0 0 1 5.738 2.993 16.327 16.327 0 0 1 4.229 4.873c1.109 1.93 1.789 4.152 2.036 6.661h-9.254c-.582-2.469-1.688-4.32-3.335-5.552-1.644-1.237-3.636-1.851-5.982-1.851-2.181 0-4.032.422-5.553 1.265a10.457 10.457 0 0 0-3.701 3.393c-.946 1.418-1.637 3.033-2.07 4.844a24.096 24.096 0 0 0-.647 5.61c0 1.819.215 3.637.648 5.4a14.614 14.614 0 0 0 2.069 4.72 10.557 10.557 0 0 0 3.701 3.36c1.52.844 3.372 1.266 5.553 1.266 3.207 0 5.684-.815 7.433-2.436 1.745-1.626 2.764-3.978 3.055-7.066h-9.746v-7.21h18.506v23.81h-6.171l-.986-4.996c-1.727 2.218-3.64 3.767-5.738 4.65Z"/>
	</symbol>
</svg>
```

Ukázkový `sprite.svg` soubor můžete získat v [@orangesk/orange-design-system](https://www.npmjs.com/package/@orangesk/orange-design-system?activeTab=code) balíčku v cestě `build/sprite.svg`.

Klepnutím na existující ikonu v nástrojové liště se otevře dialog pro její nastavení, kde lze zvolit velikost, barvu a konkrétní ikonu z dostupných ikon definovaných v SVG souboru. Klepnutím pravým tlačítkem myši se zobrazí možnost jejího smazání s potvrzením.

## Tlačítko

Tlačítko se často používá jako `call to action` prvek na webových stránkách. WebJET podporuje vkládání elementu `button` nebo `a` s třídou `btn` přes vlastní tlačítko v CKEditoru. Pro `button` lze nastavit následující konfigurační proměnné:
- `ckeditor_button_baseClass` - Základní CSS třída pro tlačítka v CKEditoru, standardně `btn`.
- `ckeditor_button_sizes` - Dostupné velikosti tlačítka oddělené čárkou, standardně `btn-lg,btn-sm`.
- `ckeditor_button_types` - Dostupné barvy/typy tlačítka oddělené čárkou, standardně `btn-primary,btn-secondary,btn-success,btn-danger,btn-warning,btn-info,btn-light,btn-dark,btn-link,btn-outline-primary,btn-outline-secondary,btn-outline-success,btn-outline-danger,btn-outline-warning,btn-outline-info,btn-outline-light,btn-outline-dark`.
- `ckeditor_button_textHiddenClass` - CSS třída pro schování textu tlačítka - zobrazení pouze ikony, standardně `visually-hidden`.
- `ckeditor_button_allowedClasses` - Seznam povolených CSS tříd oddělených čárkou pro otevření dialogu nastavení tlačítka. Prázdná hodnota povolí všechny CSS třídy. Příklad `btn-primary,btn-secondary,btn-lg`.
- `ckeditor_button_deniedClasses` - Seznam zakázaných CSS tříd oddělených čárkou pro otevření dialogu nastavení tlačítka. Prázdná hodnota nezakáže žádné CSS třídy. Příklad `no-button,no-btn`.
- `ckeditor_button_attrs` - Seznam nastavitelných atributů tlačítka oddělených čárkou, ve výchozím nastavení `data-bs-toggle,data-bs-target,aria-controls,aria-expanded,aria-label`.

Podporovány jsou i SVG ikony, které lze vložit do tlačítka, viz sekce výše.

Pokud tlačítko nastavíte jako zakázané - `disabled`, tak na něj nelze v editoru kliknout pro otevření dialogu pro úpravu vlastností tlačítka. Můžete ale kliknout prvním tlačítkem a v kontextové nabídce zvolit možnost Vlastnosti tlačítka.

Možnost vložit tlačítko se zobrazuje ve výběrovém menu pro vložení formuláře. Do nástrojové lišty jej přidáte přidáním hodnoty `WebjetFormButton` do konfigurační proměnné `ckeditor_toolbar`. Zobrazí se automaticky pokud máte nakonfigurované vkládání SVG ikon.

## Vlastní doplňky (plugins)

CKEditor podporuje přidání vlastních doplňků (plugins). Po vytvoření vlastního doplňku je třeba jej přidat do konfigurační proměnné `ckeditor_extraPlugins` jako čárkou oddělený seznam. Více informací o vytváření vlastních doplňků naleznete v [dokumentaci CKEditoru](https://ckeditor.com/docs/ckeditor4/latest/guide/plugin_sdk_intro.html).

Výsledný plugin umístěte do adresáře `src/main/webapp/admin/skins/webjet8/ckeditor/dist/plugins/` vašeho projektu.

Tlačítko následně přidejte do konfigurační proměnné `ckeditor_toolbar` na vhodné místo.
