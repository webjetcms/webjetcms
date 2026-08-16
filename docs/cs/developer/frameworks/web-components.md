# Webové komponenty

Administrace WebJET CMS používá pro menší interaktivní prvky standardní JavaScript [webové komponenty](https://developer.mozilla.org/en-US/docs/Web/API/Web_components). Komponenta je třída odvozená od `HTMLElement`, registrovaná pomocí `customElements.define`.

Zdrojové soubory jsou v adresáři `src/main/webapp/admin/v9/src/js/web-components`. Komponenta se importuje v JavaScript vstupu a v HTML se používá jako vlastní element.

Konfiguraci předávejte veřejnou metodou komponenty a změny oznamujte pomocí bublajícího `CustomEvent`. V `disconnectedCallback` zrušte časovače, síťové požadavky a listenery. Styly patří do SCSS administrace; komponenty používají light DOM, aby zdědily Bootstrap a WebJET styly.

## Migrace z Vue.js

Od verze 2026.0 administrace již nepřibaluje `Vue.js`, `vue-router`, `vue-loader` ani `vue-advanced-cropper`. Globální objekt `window.VueTools` a jeho metody `createApp`, `setDefaultObjects`, `getComponent`, `getRouter` a `getVue` již nejsou dostupné. Vlastní modul musí přejít na nativní webové komponenty nebo si zajistit vlastní závislost na Vue a konfiguraci sestavení.

Původní integrace nahraďte následovně:

- `window.VueTools.getComponent("webjet-dte-jstree")` - použijte pole datové tabulky typu [JSON](../datatables-editor/field-json.md), které komponentu `<webjet-dte-jstree>` vytvoří automaticky.
- `<webjet-cropper-component>` - použijte `<webjet-image-area-selector>` a metodě `configure` předejte funkce `getImageUrl`, `getCoordinates`, `onChange` a objekt `labels`.
- `<vue-server-monitoring>` - použijte `<webjet-server-monitoring>` a metodě `configure` předejte nastavení `complex` a `labels`.

Komponenty oznamují připravenost bublající událostí `webjet-component-ready`. Výběr ve stromu oznamuje událost `webjet-jstree-select` a změnu oblasti obrázku událost `webjet-area-change`.
