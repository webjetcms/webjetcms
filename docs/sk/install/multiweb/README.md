# MultiWeb

## Úvod

MultiWeb je špeciálny typ viac používateľskej/multi tenantnej inštalácie v jednom WebJETe. Domény sú navonok samostatné a každá sa tvári ako samostatná inštalácia WebJET CMS.

Inštalácia obsahuje riadiacu doménu (prvú zriadenú), pomocou ktorej sú dostupné zvýšené oprávnenia (napr. na konfiguráciu, prekladové kľúče atď). V jednotlivých doménach tenantov nie sú dostupné aplikácie, ktoré majú dopad na všetky domény.

## Obmedzenia

MultiWeb inštalácie obsahujú nasledovné obmedzenia:

- Web stránky
  - Média skupiny - zobrazené a upraviteľné sú len média skupiny s nastavenými právami na priečinky v aktuálnej doméne, novo pridanej média skupine sa automaticky pridajú práva na koreňové priečinky v doméne. V riadiacej doméne je možné spravovať globálne média skupiny bez zadaného obmedzenia Zobraziť pre.
- Aplikácie
  - Doménové presmerovania - spravovať je možné len doménové presmerovania smerujúce na aktuálnu doménu.
- Šablóny
  - V zozname sa zobrazujú šablóny, ktoré majú nastavený prístup k priečinkom aktuálne zobrazenej domény, pri vytvorení novej šablóny, ktorá by nemala nastavený žiaden prístup sa automatický nastaví prístup na koreňové priečinky aktuálnej domény.
  - Skupiny šablón - zobrazujú sa skupiny šablón použité v šablónach.
- Používatelia
  - Každá doména má vlastný zoznam používateľov.
  - Používateľovi, ktorý nemá nastavené žiadne obmedzenia na stromovú štruktúru priečinkov web stránok, sa interne pri kontrole práv nastaví zoznam koreňových priečinkov domény.

## Aplikácie dostupné len v riadiacej doméne

Nasledovné aplikácie sú dostupné len v riadiacej doméne:

- Úvod
  - Audit
  - Monitorovanie servera
- Aplikácie
  - GDPR - Regulárne výrazy
  - GDPR - Mazanie dát
  - Hromadný e-mail - Doménové limity
- Používatelia
  - Skupiny používateľov
  - Skupiny práv
- Ovládací panel
  - Konfigurácia
  - Prekladové kľúče
  - HTTP hlavičky
  - Mazanie dát
  - Automatizované úlohy
  - Aktualizácia WebJET CMS
  - Reštartovať

Pri potrebe doplnenia niektorého práva na používateľa v doméne je možné pridať konf. premennú `multiwebSpecialPerms-USERID` s čiarkou oddeleným zoznamom práv, ktoré má používateľ naviac získať.