# Web components

WebJET CMS administration uses standard JavaScript [web components](https://developer.mozilla.org/en-US/docs/Web/API/Web_components) for small interactive controls. A component is an `HTMLElement` subclass registered with `customElements.define`.

Source files are located in `src/main/webapp/admin/v9/src/js/web-components`. Import a component from a JavaScript entry and use its custom element in HTML.

Pass configuration through a public component method and publish changes with a bubbling `CustomEvent`. Clear timers, network requests, and listeners in `disconnectedCallback`. Component styles belong to the administration SCSS; components intentionally use light DOM so they inherit Bootstrap and WebJET styles.
