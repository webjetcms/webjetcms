# MultiWeb

## Úvod

MultiWeb je speciální typ více uživatelské/multi tenantní instalace v jednom WebJETu. Domény jsou navenek samostatné a každá se tváří jako samostatná instalace WebJET CMS.

Instalace obsahuje řídící doménu (první zřízenou), pomocí které jsou dostupná zvýšená oprávnění (např. ke konfiguraci, překladové klíče atd.). V jednotlivých doménách tenantů nejsou dostupné aplikace, které mají dopad na všechny domény.

## Omezení

MultiWeb instalace obsahují následující omezení:

- Web stránky
  - Média skupiny - zobrazena a upravitelná jsou pouze média skupiny s nastavenými právy na složky v aktuální doméně, nově přidané média skupině se automaticky přidají práva na kořenové složky v doméně. V řídicí doméně lze spravovat globální média skupiny bez zadaného omezení Zobrazit pro.
- Aplikace
  - Doménová přesměrování - spravovat lze pouze doménová přesměrování směřující na aktuální doménu.
- Šablony
  - V seznamu se zobrazují šablony, které mají nastaven přístup ke složkám aktuálně zobrazené domény, při vytvoření nové šablony, která by neměla nastaven žádný přístup se automatický nastaví přístup na kořenové složky aktuální domény.
  - Skupiny šablon - uživateli s omezeným přístupem ke složkám se zobrazí skupina, pokud je splněna alespoň jedna z následujících podmínek:
    - id skupiny šablon je použit v některé šabloně dostupné uživateli,
    - nebo v MultiWeb instalaci mimo řídicí doménu existuje neprázdný alias aktuální domény a zároveň platí alespoň jedna z podmínek:
      - `directory` obsahuje složku shodnou s aliasem,
      - `keyPrefix` se přesně shoduje s aliasem,
      - `name` obsahuje alias v hranatých závorkách ve formátu `[alias]`,
      - `name` začíná na `alias` + mezera.
  - Alias ​​domény se získá z konfigurační proměnné `multiDomainAlias:DOMAIN-NAME`, kde `DOMAIN-NAME` je název domény. Při [vytvoření nové domény](config.md#zřízení-nové-domény) přes MultiWeb se proměnná nastaví automaticky na poslední část názvu doménového adresáře zkrácenou po první tečku, například pro `www.example.sk` na hodnotu `example`. Pro existující doménu lze alias nastavit manuálně v konfiguraci.
  - V řídící doméně se zobrazují všechny skupiny šablon; stejně je vidí uživatel bez omezení přístupu ke složkám.
- Uživatelé
  - Každá doména má vlastní seznam uživatelů.
  - Uživateli, který nemá nastavena žádná omezení na stromovou strukturu složek web stránek, se interně při kontrole práv nastaví seznam kořenových složek domény.

## Aplikace dostupné pouze v řídicí doméně

Následující aplikace jsou dostupné pouze v řídicí doméně:

- Úvod
  - Audit
  - Monitorování serveru
- Aplikace
  - GDPR - Regulární výrazy
  - GDPR - Mazání dat
  - Hromadný e-mail - Doménové limity
- Uživatelé
  - Skupiny uživatelů
  - Skupiny práv
- Ovládací panel
  - Konfigurace
  - Překladové klíče
  - HTTP hlavičky
  - Mazání dat
  - Automatizované úkoly
  - Aktualizace WebJET CMS
  - Restartovat

Při potřebě doplnění některého práva na uživatele v doméně je možné přidat konf. proměnnou `multiwebSpecialPerms-USERID` s čárkou odděleným seznamem práv, která má uživatel navíc získat.
