# Vue.js

> Od verzie 2026.0 nie je Vue.js súčasťou administrácie WebJET CMS.

Globálny objekt `window.VueTools` a Vue komponenty pôvodne dodávané s administráciou boli odstránené. Postup prechodu na natívne komponenty je opísaný v časti [Migrácia z Vue.js](web-components.md#migrácia-z-vuejs). Ak vlastný modul naďalej potrebuje Vue, musí si knižnicu aj jej zostavenie zabezpečiť samostatne.

## Formátovanie dátumu a času

Na formátovanie použite funkcie `WJ.formatDate`, `WJ.formatDateTime` a `WJ.formatDateTimeSeconds` opísané v dokumentácii [WebJET JavaScript funkcií](webjetjs.md#formátovanie-dátumu-a-času).
