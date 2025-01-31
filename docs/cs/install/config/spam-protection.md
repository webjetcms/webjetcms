# Ochrana proti spamu

WebJET obsahuje integrovanou ochranu proti SPAMu, která omezuje rychlost požadavků a jejich počet, aby nedošlo k přetížení serveru. Tato ochrana se také označuje jako `Rate Limiting`, tj. omezení počtu požadavků.

Ochrana je vázána na IP adresu návštěvníka, takže limity se vztahují na každou IP adresu zvlášť. Je proto nutné mít správně nastaveno načítání IP adresy návštěvníka. Pokud je například aplikační server předem nastaven před. `Load Balancer/Proxy` je nutné nastavit konfigurační proměnnou `serverBeyoundProxy` na hodnotu `true` a na `Load Balancer/Proxy` nastavit hlavičky HTTP `x-forwarded-for` s IP adresou návštěvníka a `x-forwarded-proto` s hodnotou použitého protokolu `http/https`. Správnost IP adresy si můžete ověřit na stránce [audit](../../sysadmin/audit/README.md) ve sloupci IP adresa, když provedete nějakou akci, například přihlášení nebo odeslání formuláře.

!> **Varování:** pokud používáte cluster, omezování probíhá na každém aplikačním serveru/uzlu clusteru zvlášť - data nejsou sdílena mezi uzly.

## Základní nastavení

Následující konfigurační proměnné slouží k nastavení základního omezení počtu požadavků:
- `spamProtectionHourlyLimit` - Maximální počet požadavků z jedné IP adresy za hodinu, ve výchozím nastavení 20.
- `spamProtectionTimeout` - Minimální počet sekund mezi dvěma požadavky ze stejné IP adresy, ve výchozím nastavení 30 sekund.
- `spamProtectionIgnoreFirstRequests` - Počet prvních požadavků z IP adresy, které nejsou omezeny, ve výchozím nastavení nastaveno na 0, tj. žádná výjimka pro omezení.

### Nastavení modulu/aplikace

Limity lze upravit speciálně pro určité moduly/aplikace přidáním. `-appName` na konec konfigurační proměnné, např. `spamProtectionHourlyLimit-appName` nebo `spamProtectionTimeout-appName`. Hodnoty lze nastavit pro následující aplikace:
- `dmail` - [hromadný e-mail](../../redactor/apps/dmail/form/README.md) - omezuje počet registrací/odhlášení z hromadného e-mailu.
- `form` - [formuláře](../../redactor/apps/form/README.md) a odesílání zpětné vazby z domovské stránky správy.
- `forum` - [diskusní fórum](../../redactor/apps/forum/README.md), omezuje počet přidaných příspěvků.
- `HtmlToPdfAction` - generování dokumentů PDF.
- `inquiry` - [průzkum](../../redactor/apps/inquiry/README.md), omezuje počet hlasů v anketě.
- `passwordSend` - [odeslání zapomenutého hesla](../../redactor/admin/password-recovery/README.md), omezuje počet odeslání zapomenutého hesla.
- `qa` - [otázky a odpovědi](../../redactor/apps/qa/README.md), omezuje přidávání nových otázek.
- `quiz` - [dotazníky](../../redactor/apps/quiz/README.md), omezuje počet zaslaných dotazníků.
- `search` - [Vyhledávání](../../redactor/apps/search/README.md), omezuje počet vyhledávání.
- `ThumbServlet` - [náhledové obrázky](../../frontend/thumb-servlet/README.md), omezuje počet generování nového náhledového obrazu, obraz se uloží na disk a použije se pro další požadavky.
- `userform` - [registrace nového uživatele](../../redactor/zaheslovana-zona/README.md), omezuje počet nových registrací a změn profilu uživatele v zóně chráněné heslem.

Ve výchozím nastavení jsou již nastaveny následující hodnoty:
- `spamProtectionHourlyLimit-ThumbServlet` - počet žádostí o generování [náhledy obrázků](../../frontend/thumb-servlet/README.md), `/thumb` nastavena na 300.
- `spamProtectionTimeout-ThumbServlet` - nastavit na `-2`, což znamená, že omezení odstupu mezi požadavky na `/thumb` se nepoužije, protože takových obrázků může být na stránce současně několik a požadavky HTTP se provádějí paralelně.
- `spamProtectionHourlyLimit-search` - Počet požadavků na vyhledávání prostřednictvím vyhledávací aplikace, nastaveno na 200.
- `spamProtectionTimeout-search` - hodnota snížena na 10, aby bylo možné rychleji vyhledávat/pokračovat na další stránku výsledků.

## Výjimky

Ochranu lze částečně vypnout nastavením následujících proměnných:
- `spamProtectionDisabledIPs` - seznam začátků IP adres oddělený čárkou (nebo znakem `*` pro všechny), pro které je ochrana proti spamu vypnutá.

Obnovení stavu je možné v aplikaci [Odstranění dat - objekty mezipaměti](../../sysadmin/data-deleting/README.md), kde se seznam počtu volání a intervalů pro všechny IP adresy vynuluje pomocí akce Odstranit všechny objekty mezipaměti.
