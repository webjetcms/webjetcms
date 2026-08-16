# Vue.js

> Od verze 2026.0 již Vue.js není součástí administrace WebJET CMS.

Globální objekt `window.VueTools` a Vue komponenty původně dodávané s administrací byly odstraněny. Náhrady založené na nativních komponentách popisuje část [Migrace z Vue.js](web-components.md#migrace-z-vuejs). Pokud vlastní modul nadále vyžaduje Vue, musí si knihovnu i konfiguraci sestavení zajistit samostatně.

## Formátování data a času

Pro formátování použijte funkce `WJ.formatDate`, `WJ.formatDateTime` a `WJ.formatDateTimeSeconds` popsané v dokumentaci [WebJET JavaScript funkcí](webjetjs.md#formátování-data-a-času).
