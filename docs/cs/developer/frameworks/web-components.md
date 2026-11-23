# Web komponenty

Administrace WebJET CMS používá pro menší interaktivní prvky standardní JavaScript [web komponenty](https://developer.mozilla.org/en-US/docs/Web/API/Web_components). Komponent je třída odvozená od `HTMLElement`, zaregistrovaná přes `customElements.define`.

Zdrojové soubory jsou v adresáři `src/main/webapp/admin/v9/src/js/web-components`. Součást se importuje do vstupu JavaScript a v HTML se používá jako vlastní prvek, například:

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

Konfiguraci předávejte veřejnou metodou komponenty a změny oznamujte přes bublající `CustomEvent`. V `disconnectedCallback` zrušte časovače, síťové požadavky a listenery. Styly patří do SCSS administrace; komponenty záměrně používají light DOM, aby zdědily Bootstrap a WebJET styly.

## Migrace z Vue.js

Od verze 2026.0 administrace nepřibaluje `Vue.js`, `vue-router`, `vue-loader` ani `vue-advanced-cropper`. Není dostupný ani globální objekt `window.VueTools` a jeho metody `createApp`, `setDefaultObjects`, `getComponent`, `getRouter` a `getVue`. Vlastní modul musí přejít na nativní web komponenty nebo si Vue a jeho sestavení zajistit samostatně.

Původní integrace nahraďte následovně:

- `window.VueTools.getComponent("webjet-dte-jstree")` - ​​použijte pole datové tabulky typu [JSON](../datatables-editor/field-json.md), které komponenta `<webjet-dte-jstree>` vytvoří automaticky.
- `<webjet-cropper-component>` - ​​použijte `<webjet-image-area-selector>` a metodou `configure` odevzdejte funkce `getImageUrl`, `getCoordinates`, `onChange` a objekt `labels`.
- `<vue-server-monitoring>` - ​​použijte `<webjet-server-monitoring>` a metodou `configure` odevzdejte nastavení `complex` a `labels`.

Komponenty oznamují připravenost bublající událostí `webjet-component-ready`. Výběr ve stromu oznamuje událost `webjet-jstree-select` a změnu oblasti obrázku událost `webjet-area-change`.
