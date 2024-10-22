# Odstraněné/nepodporované vlastnosti

## Uživatelské rozhraní

**Webové stránky**

- EditorForm se již nepoužívá při ukládání stránky, pokud používáte funkci [WebjetEvent](../developer/backend/events.md) je třeba přizpůsobit používání `EditorForm` na adrese `DocDetails` objekt.
- Webovou stránku již nelze nastavit na "Vyžadovat zabezpečené připojení (https)", doporučeným řešením je nastavit přesměrování celé domény na zabezpečené připojení (https). Nemá smysl to nastavovat individuálně pro jednotlivé stránky jako v minulosti.
- Právo `Uložiť (pracovná verzia)` se změní na pravou `Uložiť`. Pokud uživatel toto právo nemá, nemůže ukládat existující webové stránky. Původní použití pouze pro pracovní verzi nedávalo praktický smysl.

## Backend API

- Podpora `Struts` frameworku.
- Knihovna `itext` již není součástí standardní distribuce WebJET CMS, protože obsahuje neopravené zranitelnosti a nová verze je komerční. `DisplayTag` a exportovat formulář do formátu PDF (`/formtopdf.do`), pokud potřebujete v projektu, musíte získat knihovnu `itext` ručně přidat s rizikem možné zranitelnosti.
- Knihovna `commons-httpclient-3.1` již není součástí standardní distribuce WebJET CMS, protože obsahuje neopravené zranitelnosti. Integrovaná je [verze 4.5](https://hc.apache.org/httpcomponents-client-4.5.x/quickstart.html), doporučujeme použít [Fluent API](https://hc.apache.org/httpcomponents-client-4.5.x/current/tutorial/html/fluent.html).
