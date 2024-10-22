# Spam ochrana

WebJET obsahuje integrovanú ochranu proti SPAM-u, pri ktorej limituje rýchlosť požiadaviek a ich počet, aby nedošlo k preťaženiu servera. Takáto ochrana sa označuje aj ako `Rate Limiting`, čiže limitovanie počtu požiadaviek.

Ochrana je viazaná na IP adresu návštevníka, čiže limity platia pre každú IP adresu samostatne. Je teda potrebné mať správne nastavené získavanie IP adresy návštevníka. Ak je napríklad pred aplikačným serverom predradený `Load Balancer/Proxy` je potrebné nastaviť konfiguračnú premennú `serverBeyoundProxy` na hodnotu `true` a na `Load Balancer/Proxy` nastaviť HTTP hlavičky `x-forwarded-for` s IP adresou návštevníka a `x-forwarded-proto` s hodnotou použitého protokolu `http/https`. Overiť správnosť IP adresy môžete v [audite](../../sysadmin/audit/README.md) v stĺpci IP adresa pri vykonaní nejakej akcie, napríklad prihlásenie, alebo odoslanie formuláru.

!> **Upozornenie:** ak používate cluster, limitovanie sa deje samostatne na každom aplikačnom serveri/uzle clustra - dáta nie sú zdieľané medzi uzlami.

## Základné nastavenie

Pre základné nastavenie limitovania počtu požiadaviek slúžia nasledovné konfiguračné premenné:

- `spamProtectionHourlyLimit` - maximálny počet požiadaviek z jednej IP adresy za hodinu, predvolene nastavené na hodnotu 20.
- `spamProtectionTimeout` - minimálny počet sekúnd medzi dvoma požiadavkami z jednej IP adresy, predvolene nastavené na hodnotu 30 sekúnd.
- `spamProtectionIgnoreFirstRequests` - počet prvých požiadaviek z IP adresy, ktoré nie sú limitované, predvolene nastavené na 0, čiže bez výnimky limitovania.

### Nastavenie pre modul/aplikáciu

Limity je možné upraviť špecificky pre niektoré moduly/aplikácie pridaním `-appName` na koniec konfiguračnej premennej, napr. `spamProtectionHourlyLimit-appName` alebo `spamProtectionTimeout-appName`. Nastaviť je hodnoty možné pre nasledovné aplikácie:

- `dmail` - [hromadný email](../../redactor/apps/dmail/form/README.md) - limituje počet registrácií/odhlásení z hromadného emailu.
- `form` - [formuláre](../../redactor/apps/form/README.md) a odosielanie spätná väzba z úvodnej strany administrácie.
- `forum` - [diskusné fórum](../../redactor/apps/forum/README.md), limituje počty pridaných príspevkov.
- `HtmlToPdfAction` - generovanie PDF dokumentov.
- `inquiry` - [anketa](../../redactor/apps/inquiry/README.md), limituje počet hlasovaní v ankete.
- `passwordSend` - [zaslanie zabudnutého hesla](../../redactor/admin/password-recovery/README.md), limituje počet zaslaní zabudnutého hesla.
- `qa` - [otázky a odpovede](../../redactor/apps/qa/README.md), limituje pridanie novej otázky.
- `quiz` - [dotazníky](../../redactor/apps/quiz/README.md), limituje počet odoslaných dotazníkov.
- `search` - [vyhľadávanie](../../redactor/apps/search/README.md), limituje počet vyhľadávaní.
- `ThumbServlet` - [náhľadové obrázky](../../frontend/thumb-servlet/README.md), limituje počet vygenerovaní nového náhľadového obrázka, obrázku sú ukladané na disk a použité pre ďalšie požiadavky.
- `userform` - [registrácia nového používateľa](../../redactor/zaheslovana-zona/README.md), limituje počet nových registrácií a úprav profilu používateľa v zaheslovanej zóne.

Predvolene sú už upravené nasledovné hodnoty:

- `spamProtectionHourlyLimit-ThumbServlet` - počet požiadaviek na generovanie [náhľadov obrázkov](../../frontend/thumb-servlet/README.md) `/thumb` nastavené na hodnotu 300.
- `spamProtectionTimeout-ThumbServlet` - nastavené na hodnotu `-2`, čo znamená, že limitovanie rozostupu medzi požiadavkami sa pre `/thumb` neaplikuje, je to z toho dôvodu, že v stránke môže byť naraz viacero takýchto obrázkov a HTTP požiadavky sú vykonané paralelne.
- `spamProtectionHourlyLimit-search` - počet požiadaviek na vyhľadávanie cez aplikáciu vyhľadávanie, nastavené na hodnotu 200.
- `spamProtectionTimeout-search` - hodnota znížená na 10, aby bolo možné rýchlejšie vykonať ďalšie vyhľadávanie/prejsť na ďalšiu stranu výsledkov.

## Výnimky

Ochranu je možné čiastočne vypnúť nastavením nasledovných premenných:

- `spamProtectionDisabledIPs` - zoznam začiatkov IP adries oddelených čiarkou (alebo znak `*` pre všetkých), pre ktoré je spam ochrana vypnutá.

Reset stav je možné v aplikácii [Mazanie dát - Cache objekty](../../sysadmin/data-deleting/README.md), kde pomocou akcie Zmazať všetky cache objekty je resetovaný aj zoznam počtu volaní a intervalov pre všetky IP adresy.