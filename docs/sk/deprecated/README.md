# Zrušené/nepodporované vlastnosti

## Používateľské rozhranie

**Web stránky**

- Pri uložení stránky sa už nepoužíva EditorForm, ak používate  [WebjetEvent](../developer/backend/events.md) je potrebné ho upraviť z použitia ```EditorForm``` na ```DocDetails``` objekt.
- Web stránke sa už nedá nastaviť atribút "Vyžadovať zabezpečené spojenie (https)", odporúčané riešenie je nastaviť presmerovanie celej domény na zabezpečené (https) spojenie. Nemá zmysel to individuálne nastavovať pre stránky ako v minulosti.
- Právo ```Uložiť (pracovná verzia)``` je zmenené na právo ```Uložiť```. Ak používateľ nemá toto právo, nemôže ukladať existujúce web stránky. Pôvodné použitie len na pracovnú verziu nedávalo praktický zmysel.

## Backend API

- Podpora ```Struts``` frameworku.
- Knižnica ```itext``` už nie je súčasťou štandardnej distribúcie WebJET CMS, pretože obsahuje neopravené zraniteľnosti a nová verzia je komerčná, Zrušená je možnosť exportu PDF v ```DisplayTag``` a exportu formuláru do PDF (```/formtopdf.do```), ak potrebujete v projekte, je potrebné si knižnicu ```itext``` manuálne pridať s rizikom možnej zraniteľnosti.
- Knižnica ```commons-httpclient-3.1``` už nie je súčasťou štandardnej distribúcie WebJET CMS, pretože obsahuje neopravené zraniteľnosti. Integrovaná je [verzia 4.5](https://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html), odporúčame používať [Fluent API](https://hc.apache.org/httpcomponents-client-4.5.x/current/tutorial/html/fluent.html).

