# Web components

WebJET CMS administration uses standard JavaScript [web components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) for smaller interactive elements. A component is a class derived from `HTMLElement`, registered via `customElements.define`.

The source files are in the `src/main/webapp/admin/v9/src/js/web-components` directory. The component is imported in JavaScript input and used in HTML as a custom element, for example:

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

Pass the configuration through a public method of the component and announce changes via bubbling `CustomEvent`. In `disconnectedCallback`, disable timers, network requests, and listeners. Styles belong to SCSS administration; components intentionally use a light DOM to inherit Bootstrap and WebJET styles.

## Migrating from Vue.js

Since version 2026.0, the administration does not package `Vue.js`, `vue-router`, `vue-loader` or `vue-advanced-cropper`. The global object `window.VueTools` and its methods `createApp`, `setDefaultObjects`, `getComponent`, `getRouter` and `getVue` are also not available. The custom module must switch to native web components or provide Vue and its compilation separately.

Replace the original integrations as follows:

- `window.VueTools.getComponent("webjet-dte-jstree")` - ​​use a data table field of type [JSON](../datatables-editor/field-json.md), which the `<webjet-dte-jstree>` component will automatically create.
- `<webjet-cropper-component>` - ​​use `<webjet-image-area-selector>` and pass the functions `getImageUrl`, `getCoordinates`, `onChange` and the object `labels` to the method `configure`.
- `<vue-server-monitoring>` - ​​use `<webjet-server-monitoring>` and pass the settings `complex` and `labels` to the method `configure`.

Components report readiness with a bubbling event `webjet-component-ready`. Selection in the tree reports an event `webjet-jstree-select` and image area changes an event `webjet-area-change`.
