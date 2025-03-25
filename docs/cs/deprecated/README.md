# Zrušené/nepodporované vlastnosti

## Uživatelské rozhraní

**Web stránky**

- Při uložení stránky se již nepoužívá EditorForm, pokud používáte [WebjetEvent](../developer/backend/events.md) je třeba jej upravit z použití `EditorForm` na `DocDetails` objekt.
- Web stránce již nelze nastavit atribut "Vyžadovat zabezpečené spojení (https)", doporučené řešení je nastavit přesměrování celé domény na zabezpečené (https) spojení. Nemá smysl to individuálně nastavovat pro stránky jako dříve.
- Právo `Uložiť (pracovná verzia)` je změněno na právo `Uložiť`. Pokud uživatel nemá toto právo, nemůže ukládat stávající web stránky. Původní použití jen na pracovní verzi nedávalo praktický smysl.

## Backend API

- Podpora `Struts` frameworku.
- Knihovna `itext` již není součástí standardní distribuce WebJET CMS, protože obsahuje neopravené zranitelnosti a nová verze je komerční, Zrušena je možnost exportu PDF v `DisplayTag` a exportu formuláře do PDF (`/formtopdf.do`), pokud potřebujete v projektu, je třeba si knihovnu `itext` manuálně přidat s rizikem možné zranitelnosti.
- Knihovna `commons-httpclient-3.1` již není součástí standardní distribuce WebJET CMS, protože obsahuje neopravené zranitelnosti. Integrovaná je [verze 4.5](https://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html), doporučujeme používat [Fluent API](https://hc.apache.org/httpcomponents-client-4.5.x/current/tutorial/html/fluent.html).
