# Web components

WebJET CMS administration uses standard JavaScript [web components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) for small interactive controls. A component is an `HTMLElement` subclass registered with `customElements.define`.

Source files are located in `src/main/webapp/admin/v9/src/js/web-components`. Import a component from a JavaScript entry and use its custom element in HTML.

Pass configuration through a public component method and publish changes with a bubbling `CustomEvent`. Clear timers, network requests, and listeners in `disconnectedCallback`. Component styles belong to the administration SCSS; components intentionally use light DOM so they inherit Bootstrap and WebJET styles.

## Migrating from Vue.js

Starting with version 2026.0, the administration no longer bundles `Vue.js`, `vue-router`, `vue-loader`, or `vue-advanced-cropper`. The global `window.VueTools` object and its `createApp`, `setDefaultObjects`, `getComponent`, `getRouter`, and `getVue` methods are no longer available. A custom module must migrate to native web components or provide its own Vue dependency and build configuration.

Replace the previous integrations as follows:

- `window.VueTools.getComponent("webjet-dte-jstree")` - use the DataTable [JSON](../datatables-editor/field-json.md) field, which creates the `<webjet-dte-jstree>` component automatically.
- `<webjet-cropper-component>` - use `<webjet-image-area-selector>` and pass `getImageUrl`, `getCoordinates`, `onChange`, and `labels` to its `configure` method.
- `<vue-server-monitoring>` - use `<webjet-server-monitoring>` and pass `complex` and `labels` to its `configure` method.

Components announce readiness with the bubbling `webjet-component-ready` event. Tree selection uses `webjet-jstree-select`, and an image-area change uses `webjet-area-change`.
