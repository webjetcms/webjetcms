# Web komponenty

Administrácia WebJET CMS používa pre menšie interaktívne prvky štandardné JavaScript [web komponenty](https://developer.mozilla.org/en-US/docs/Web/API/Web_components). Komponent je trieda odvodená od `HTMLElement`, zaregistrovaná cez `customElements.define`.

Zdrojové súbory sú v adresári `src/main/webapp/admin/v9/src/js/web-components`. Komponent sa importuje v JavaScript vstupe a v HTML sa používa ako vlastný element, napríklad:

```javascript
class WebjetExampleElement extends HTMLElement {
    connectedCallback() {
        this.textContent = "WebJET";
    }
}

if (!customElements.get("webjet-example")) {
    customElements.define("webjet-example", WebjetExampleElement);
}
```

```html
<webjet-example></webjet-example>
```

Konfiguráciu odovzdávajte verejnou metódou komponentu a zmeny oznamujte cez bublajúci `CustomEvent`. V `disconnectedCallback` zrušte časovače, sieťové požiadavky a listenery. Štýly patria do SCSS administrácie; komponenty zámerne používajú light DOM, aby zdedili Bootstrap a WebJET štýly.

## Migrácia z Vue.js

Od verzie 2026.0 administrácia nepribaľuje `Vue.js`, `vue-router`, `vue-loader` ani `vue-advanced-cropper`. Nie je dostupný ani globálny objekt `window.VueTools` a jeho metódy `createApp`, `setDefaultObjects`, `getComponent`, `getRouter` a `getVue`. Vlastný modul musí prejsť na natívne web komponenty alebo si Vue a jeho zostavenie zabezpečiť samostatne.

Pôvodné integrácie nahraďte nasledovne:

- `window.VueTools.getComponent("webjet-dte-jstree")` - použite pole dátovej tabuľky typu [JSON](../datatables-editor/field-json.md), ktoré komponent `<webjet-dte-jstree>` vytvorí automaticky.
- `<webjet-cropper-component>` - použite `<webjet-image-area-selector>` a metódou `configure` odovzdajte funkcie `getImageUrl`, `getCoordinates`, `onChange` a objekt `labels`.
- `<vue-server-monitoring>` - použite `<webjet-server-monitoring>` a metódou `configure` odovzdajte nastavenia `complex` a `labels`.

Komponenty oznamujú pripravenosť bublajúcou udalosťou `webjet-component-ready`. Výber v strome oznamuje udalosť `webjet-jstree-select` a zmenu oblasti obrázka udalosť `webjet-area-change`.
