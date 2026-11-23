# Vue.js

> Od verze 2026.0 není Vue.js součástí administrace WebJET CMS.

Globální objekt `window.VueTools` a Vue komponenty původně dodávané s administrací byly odstraněny. Postup přechodu na nativní komponenty je popsán v části [Migrace z Vue.js](web-components.md#migrace-z-vuejs). Pokud vlastní modul nadále potřebuje Vue, musí si knihovnu i její sestavení zajistit samostatně.

## Formátování data a času

Pro formátování použijte funkce `WJ.formatDate`, `WJ.formatDateTime` a `WJ.formatDateTimeSeconds` popsané v dokumentaci [WebJET JavaScript funkcí](webjetjs.md#formátování-datu-a-času).
