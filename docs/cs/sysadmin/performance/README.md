# Výkon serveru

Pro optimální výkon serveru je potřeba splnit několik požadavků a nastavení. Každá vložená aplikace (např. foto galerie, anketa atd.) do web stránky způsobuje zpomalení. Aplikace typicky provádějí dodatečné databázové požadavky, nebo potřebují číst data ze souborového systému.

Výrazný vliv na výkon mohou mít také vyhledávače, které neustále procházejí a indexují web stránky na vašem serveru. Jejich návštěvnost nemusí být vidět například. v Google Analytics, ale je vidět v [statistice](../../redactor/apps/stat/README.md), kterou poskytuje WebJET CMS.

## Identifikace problémů

Jako první je třeba identifikovat místa, kde dochází ke zpomalení. Pokud umíte na první pohled identifikovat web stránku, která se vám zdá pomalá, můžete použít URL parametr `?_writePerfStat=true`. Jinak zapněte monitorování serveru, ve kterém umíte identifikovat web stránky, které jsou prováděny nejdéle.

### URL parametrem

Pomocí URL parametru `?_writePerfStat=true` je možné získat výpis aplikací vložených ve web stránce s časem jejich provedení. Například stránku `/sk/` zobrazíte jako `/sk/?_writePerfStat=true`.

Při takovém zobrazení web stránky se do HTML kódu vloží po každé aplikaci výraz typu `PerfStat: 3 ms (+3) !INCLUDE(...)`. Ve standardně zobrazené webové stránce nemusí být dobře dohledatelný, proto doporučujeme zobrazit zdrojový kód stránky - v Chrome menu Zobrazit-Vývojář-Zobrazit zdrojový kód. Následně použijte vyhledávání v prohlížeči výrazu `PerfStat:`.

Tento výraz je ve formátu `PerfStat: 3 ms (+3)` kde první číslo znamená celkový čas provedení jednoho `iwcm:write` výrazu a číslo v závorce je čas provedení této aplikace. Následně následuje cesta k aplikaci a její parametry. Vás tedy zajímá primární číslo v závorce.

Pomocí URL parametru `_disableCache=true` lze vypnout vyrovnávací paměť aplikací.

### Monitorování serveru

Pro komplexní pohled můžete zapnout funkci [monitorování serveru](../monitoring/README.md) nastavením následujících konfiguračních proměnných:
- `serverMonitoringEnable` - zapne funkci monitorování serveru a zaznamenávání hodnot
- `serverMonitoringEnablePerformance` - zapne funkci monitorování výkonu aplikací a web stránek
- `serverMonitoringEnableJPA` - zapne funkci monitorování SQL požadavků

!>**Upozornění:** monitorování výkonu aplikací a SQL požadavků zatěžuje server, nedoporučujeme mít tuto funkci permanentně zapnutou.

Po nastavení konfiguračních proměnných je třeba provést **restart aplikačního serveru**, aby se při inicializaci aktivovalo sledování výkonu.

Následně v sekci Monitorování serveru - Aplikace/WEB stránky/SQL dotazy umíte identifikovat části, které se dlouho provádějí. Zaměřte se na nejčastěji prováděné aplikace/SQL dotazy a jejich optimalizaci.

### Celkový čas generování web stránky

Existuje aplikace `/components/_common/generation_time.jsp` kterou pokud vložíte do patičky šablony web stránky vygeneruje do HTML kódu celkovou dobu generování web stránky.

Lze nastavit následující parametry aplikace:
- `hide` - ve výchozím nastavení `true` - čas generování se zobrazí jako komentář v HTML kódu
- `onlyForAdmin` - ve výchozím nastavení `false` - čas generování se zobrazí jen pokud je přihlášen administrátor

Do patičky (nebo vhodného volného pole) šablony web stránky vložte následující kód:

```html
!INCLUDE(/components/_common/generation_time.jsp, hide=true, onlyForAdmin=false)!
```

Na místě vložené aplikace se zobrazí informace o době provedení celé web stránky v ms:

```html
<!-- generation time: 4511 ms -->
```

## Měření výkonu databázového serveru a souborového systému

Pro srovnání výkonu prostředí testovací VS produkční prostředí lze použít níže uvedené skripty. Jejich spuštění vyžaduje právo na aktualizaci WebJET. Měřit a porovnávat prostředí můžete bez zátěže, ale i během provozu nebo výkonnostních testů.

- `/admin/update/dbspeedtest.jsp` - měří výkon čtení údajů z databázového serveru.

Dobré hodnoty jsou například:

```html
Image read, count=445
...
Total time: 649 ms, per item: 1.4584269662921348 ms
Total bytes: 4.8050469E7, per second: 7.403770261941448E7 B/s

Random web page read, count=3716
...
Total time: 3608 ms, per item: 0.9709364908503767 ms
Total bytes: 1371566.0, per second: 380145.78713968955 B/s

Only documents.data web page read, count=3716
...
Total time: 2205 ms, per item: 0.5933799784714747 ms
Total bytes: 685783.0, per second: 311012.6984126984 B/s

Documents read using web page API, count=3716
...
Total time: 1869 ms, per item: 0.5029601722282023 ms
Total bytes: 685783.0, per second: 366925.09363295883 B/s
```

Z důvodu rozdílného počtu záznamů v databázi je třeba srovnávat `per item` hodnoty.

- `/admin/update/fsspeedtest.jsp` - kontroluje rychlost čtení seznamu souborů ze souborového systému, je třeba ověřit hlavně pokud používáte síťový souborový systém.

Dobré hodnoty jsou například:

```html
Testing mime speed, start=0 ms
has base file object, fullPath=/Users/jeeff/Documents.nosync/workspace-visualstudio/webjet/webjet8v9-hotfix/src/main/webapp/components/_common/mime diff=1 ms
listFiles, size=678, diff=284 ms
listing done, diff=16 ms


Testing modinfo speed, start=0 ms
modinfo list, size=102, diff=1 ms
modinfo listing done, diff=220 ms
Total time=522ms
```

## Optimalizace databázových požadavků

Optimalizovat počet databázových požadavků lze zapnutím vyrovnávací paměti - `cache`.

### Web stránky

Každá web stránka má v kartě Základní možnost **Povolit uložení stránky do vyrovnávací paměti**. Zapnutím této možnosti se obsah web stránky z tabulky `documents` uloží do vyrovnávací paměti. Při zobrazení web stránky nebude nutné provést databázové volání pro získání obsahu web stránky.

Tuto možnost doporučujeme zapnout na nejvíce navštěvovaných web stránkách, jejichž seznam získáte v aplikaci [statistika](../../redactor/apps/stat/README.md#top-stránky).

![](../../redactor/webpages/editor/tab-basic.png)

### Aplikace

Podobně jako pro web stránky lze zapnout vyrovnávací paměť i pro aplikace. Některé aplikace mají tuto možnost dostupnou přímo v [nastavení aplikace](../../custom-apps/appstore/README.md#karta-zobrazení) vložené ve web stránce v kartě Zobrazení jako pole **Čas vyrovnávací paměti**.

![](../../custom-apps/appstore/common-settings-tab.png)

Pokud aplikace toto nastavení nemá dostupné stále můžete parametr nastavit v HTML kódu textu web stránky přidáním parametru `, cacheMinutes=xxx` k parametrům vložené aplikace, například:

```html
!INCLUDE(sk.iway.iwcm.components.reservation.TimeBookApp, reservationObjectIds=2560+2561, device=, cacheMinutes=10)!
```

!>**Upozornění:** je třeba si uvědomit, že vyrovnávací paměť je globální pro celý aplikační server. Jako klíč se použije cesta k souboru aplikace, jednotlivé parametry zadané v HTML kódu web stránky a jazyk aktuálně zobrazené web stránky. Neberou se v úvahu URL parametry web stránky.

Vyrovnávací paměť nelze tedy použít například pokud se zobrazí například stránkování seznamu kde číslo strany se přenáší pomocí URL parametru. Aby ale bylo možné uložit seznam novinek existuje výjimka – pro aplikace obsahující v názvu souboru `/news/news` se použije vyrovnávací paměť pouze pokud v URL adrese není zadán parametr `page`, respektive hodnota tohoto parametru je jiná než `1`. Takto se vyrovnávací paměť použije i pro seznam novinek, ale uloží se do ní jen první strana výsledků. Další strany nejsou ukládány.

## Optimalizace souborového systému

Web stránky typicky obsahují spoustu doplňkových souborů – obrázky, CSS styly, JavaScript soubory a podobně, které je třeba spolu s web stránkou načíst. Rychlost zobrazení tedy závisí i na počtu a velikosti těchto souborů.

### Nastavení vyrovnávací paměti

Pro soubory web stránky lze nastavit použití vyrovnávací paměti v prohlížeči - soubor se tak nebude opakovaně číst při každém zobrazení web stránky, ale pokud jej již má prohlížeč ve vyrovnávací paměti, tak se použije. Zrychlí se tak zobrazení web stránky a sníží zátěž na server. Příkladem je obrázek loga, který je typicky na každé stránce, ale jeho změna je vysoce nepravděpodobná – respektive mění se řádově jednou za několik měsíců.

Lze nastavit následující konfigurační proměnné, které ovlivňují HTTP hlavičku `Cache-Control`:
- `cacheStaticContentSeconds` - nastavený počet sekund, ve výchozím nastavení `300`.
- `cacheStaticContentSuffixes` - seznam přípon, pro které se HTTP hlavička `Cache-Control` vygeneruje, ve výchozím nastavení `.gif,.jpg,.png,.swf,.css,.js,.woff,.svg,.woff2`.

Pro přesnější nastavení lze použít aplikaci [HTTP hlavičky](../../admin/settings/response-header/README.md), kde můžete nastavit rozdílné hodnoty pro rozdílné URL adresy.

![](../../admin/settings/response-header/editor-wildcard.png)

## Chování pro administrátora

Pokud je přihlášen administrátor vyrovnávací paměť pro aplikace se nepoužije (předpokládá se, že administrátor vždy chce vidět aktuální stav).

Toto chování lze změnit nastavením konfigurační proměnné `cacheStaticContentForAdmin` na hodnotu `true`. Vhodné je tuto hodnotu nastavit hlavně pro intranet instalace, kde se uživatelé ověřují vůči `SSO/ActiveDirectory` serveru ai při běžné práci v intranet prostředí mají práva administrátora.

## Vyhledávače

Vyhledávače a různé jiné roboty mohou výrazně zatížit server. Zvláště s nástupem učení umělé inteligence dochází k výraznému prohledávání internetu a plnění databází pro učení umělé inteligence. Roboty často zkoušejí různé URL parametry pro získání dodatečných dat.

### Nastavení robots.txt

Chování robotů lze ovlivnit nastavením v souboru `/robots.txt`. Tento pokud neexistuje je generován ve výchozím stavu. Vámi upravenou verzi umístěte do `/files/robots.txt`, z tohoto webu jej WebJET zobrazí při volání `/robots.txt`.

Pomocí souboru [robots.txt](https://en.wikipedia.org/wiki/Robots.txt) můžete ovlivnit chování robotů a vyhledávačů - omezit URL adresy, které mohou použít, nastavit odstup mezi požadavky atp.

## Ostatní nastavení

### Reverzní DNS server

Statistika, audit a další aplikace mohou získávat reverzní DNS záznam z IP adresy. Používá se API volání `InetAddress.getByName(ip).getHostName()`. Na serverech/v DMZ nemusí ale být DNS server dostupný a toto volání může trvat několik sekund než nastane chyba. Obecně takové volání zpomaluje provedení HTTP požadavku.

Nastavením konfigurační proměnné `disableReverseDns` na hodnotu `true` je možné vypnout získávání DNS názvu z IP adresy návštěvníka a zrychlit provádění požadavků. Do pole pro hodnotu `hostname` se tehdy zapíše hodnota IP adresy.

### Vypnutí statistiky

Zapisování údajů statistiky je asynchronní, provádí se v dávkách tak, aby zobrazení web stránky nečekalo na zápis údajů statistiky do databáze.

Při vysoké návštěvnosti nebo hledání problémů s výkonem můžete dočasně vypnout zapisování statistik návštěvnosti nastavením konfigurační proměnné `statMode` na hodnotu `none`. Standardní hodnota je `new`.
