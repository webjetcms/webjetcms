# Spam ochrana

WebJET obsahuje integrovanou ochranu proti SPAMu, při které limituje rychlost požadavků a jejich počet, aby nedošlo k přetížení serveru. Taková ochrana se označuje také jako `Rate Limiting`, neboli limitování počtu požadavků.

Ochrana je vázána na IP adresu návštěvníka, tedy limity platí pro každou IP adresu samostatně. Je tedy třeba mít správně nastaveno získávání IP adresy návštěvníka. Pokud je například před aplikačním serverem předřazen `Load Balancer/Proxy` je třeba nastavit konfigurační proměnnou `serverBeyoundProxy` na hodnotu `true` a na `Load Balancer/Proxy` nastavit HTTP hlavičky `x-forwarded-for` s IP adresou návštěvníka a `x-forwarded-proto` s hodnotou použitého protokolu `http/https`. Ověřit správnost IP adresy můžete v [auditu](../../sysadmin/audit/README.md) ve sloupci IP adresa při provedení nějaké akce, například přihlášení, nebo odeslání formuláře.

!> **Upozornění:** pokud používáte cluster, limitování se děje samostatně na každém aplikačním serveru/uzlu clusteru - data nejsou sdílena mezi uzly.

## Základní nastavení

Pro základní nastavení limitování počtu požadavků slouží následující konfigurační proměnné:
- `spamProtectionHourlyLimit` - maximální počet požadavků z jedné IP adresy za hodinu, ve výchozím nastavení nastaveno na hodnotu 20.
- `spamProtectionTimeout` - minimální počet sekund mezi dvěma požadavky z jedné IP adresy, ve výchozím nastavení na hodnotu 30 sekund.
- `spamProtectionIgnoreFirstRequests` - počet prvních požadavků z IP adresy, které nejsou limitovány, ve výchozím nastavení nastaveno na 0, tedy bez výjimky limitování.

### Nastavení pro modul/aplikaci

Limity lze upravit specificky pro některé moduly/aplikace přidáním `-appName` na konec konfigurační proměnné, například. `spamProtectionHourlyLimit-appName` nebo `spamProtectionTimeout-appName`. Nastavit je hodnoty možné pro následující aplikace:
- `dmail` - [hromadný email](../../redactor/apps/dmail/form/README.md) - limituje počet registrací/odhlášení z hromadného emailu.
- `form` - [formuláře](../../redactor/apps/form/README.md) a odesílání zpětná vazba z úvodní strany administrace.
- `forum` - [diskusní fórum](../../redactor/apps/forum/README.md), limituje počty přidaných příspěvků.
- `HtmlToPdfAction` - generování PDF dokumentů.
- `inquiry` - [anketa](../../redactor/apps/inquiry/README.md), limituje počet hlasování v anketě.
- `passwordSend` - [zaslání zapomenutého hesla](../../redactor/admin/password-recovery/README.md), limituje počet zaslání zapomenutého hesla.
- `qa` - [otázky a odpovědi](../../redactor/apps/qa/README.md), limituje přidání nové otázky.
- `quiz` - [dotazníky](../../redactor/apps/quiz/README.md), limituje počet odeslaných dotazníků.
- `search` - [vyhledávání](../../redactor/apps/search/README.md), limituje počet vyhledávání.
- `ThumbServlet` - [náhledové obrázky](../../frontend/thumb-servlet/README.md), limituje počet vygenerování nového náhledového obrázku, obrázku jsou ukládány na disk a použity pro další požadavky.
- `userform` - [registrace nového uživatele](../../redactor/zaheslovana-zona/README.md), limituje počet nových registrací a úprav profilu uživatele v zaheslované zóně.

Ve výchozím nastavení jsou již upraveny následující hodnoty:
- `spamProtectionHourlyLimit-ThumbServlet` - počet požadavků na generování [náhledů obrázků](../../frontend/thumb-servlet/README.md), `/thumb` nastaveno na hodnotu 300.
- `spamProtectionTimeout-ThumbServlet` - nastaveno na hodnotu `-2`, což znamená, že limitování rozestupu mezi požadavky se pro `/thumb` neaplikuje, je to z toho důvodu, že ve stránce může být najednou více takových obrázků a HTTP požadavky jsou provedeny paralelně.
- `spamProtectionHourlyLimit-search` - počet požadavků na vyhledávání přes aplikaci vyhledávání, nastavené na hodnotu 200.
- `spamProtectionTimeout-search` - hodnota snížena na 10, aby bylo možné rychleji provést další vyhledávání/přejít na další stranu výsledků.

## Výjimky

Ochranu lze částečně vypnout nastavením následujících proměnných:
- `spamProtectionDisabledIPs` - seznam začátků IP adres oddělených čárkou (nebo znak `*` pro všechny), pro které je spam ochrana vypnuta.

Reset stav je možné v aplikaci [Mazání dat - Cache objekty](../../sysadmin/data-deleting/README.md), kde pomocí akce Smazat všechny cache objekty je resetován i seznam počtu volání a intervalů pro všechny IP adresy.
