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
