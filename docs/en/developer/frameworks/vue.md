# Vue.js

> Since version 2026.0, Vue.js is not part of the WebJET CMS administration.

The global object `window.VueTools` and the Vue components originally shipped with the administration have been removed. The procedure for switching to native components is described in the [Migration from Vue.js](web-components.md#migration-from-vuejs) section. If your own module still needs Vue, you must provide the library and its compilation separately.

## Date and time formatting

For formatting, use the `WJ.formatDate`, `WJ.formatDateTime` and `WJ.formatDateTimeSeconds` functions described in the [WebJET JavaScript functions] documentation (webjetjs.md#date-and-time-formatting).
