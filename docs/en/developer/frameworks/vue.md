# Vue.js

> Starting with version 2026.0, Vue.js is no longer part of the WebJET CMS administration.

The global `window.VueTools` object and the Vue components previously supplied by the administration have been removed. See [Migrating from Vue.js](web-components.md#migrating-from-vuejs) for native-component replacements. If a custom module still requires Vue, it must provide the library and its build configuration itself.

## Date and time formatting

Use `WJ.formatDate`, `WJ.formatDateTime`, and `WJ.formatDateTimeSeconds` as described in the [WebJET JavaScript functions](webjetjs.md#date-and-time-formatting) documentation.
