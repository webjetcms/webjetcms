# Migrace mezi prostředími

Základní informace, které je třeba provést při migraci mezi prostředími. produkčním a vývojovým prostředím.

## Konfigurační proměnné

Zkontrolujte následující konf. proměnné:
- `cloudStaticFilesDir` - cesta k adresáři se statickými soubory, je třeba ji nastavit správně na dané prostředí (případně ji na prostředí smazat, pokud se externí statické soubory nepoužívají).
- `enableStaticFilesExternalDir` - zapnutí/vypnutí používání externího adresáře pro statické soubory.
- `smtpServer` - adresa SMTP serveru + proměnné `smtpUsername, smtpPassword, smtpPort, smtpUseTLS` a podobně.
- `emailProtectionSenderEmail` - nastavte email adresu, která se použije jako email odesílatele, může být rozdílná pro jednotlivá prostředí, musí být povolena na SMTP serveru.
- `proxyHost/proxyPort` - nastavení proxy
- `webEnabledIPs` - seznam IP adres ze kterých je dostupný web, na DEV prostředí nastavit ve výchozím nastavení na `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,62.168.118.90,62.168.118.67,#klient,`
- `adminEnabledIPs` - seznam IP adres ze kterých lze přistupovat do administrace, ve výchozím nastavení na `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,#klient,`
- `multidomainAdminHost` - pokud je nastavena ověřte doménu, používá se k nastavení domény pro CMS na více doménové instalaci.
- `serverBeyoundProxy` - nastavení umístění aplikačního serveru za proxy/load balancer.
- `logLevel` - na produkci ponechat na `normal`, na vývojářském prostředí můžete změnit na `debug`.

Zkontrolujte všechny ostatní konf. proměnné a zvažte jejich dopad na projekt a vhodnost nastavení.

## Doménové adresy

Pokud web používá `multidomain` je třeba korektně nastavit domény. Při migraci mezi prostředími zkontrolujte všechny kořenové adresáře a nastavte správná doménová jména. Zaškrtněte také možnost Změnit přesměrování domény, pokud je ale doména nastavena ve více kořenových složkách (např. na sk i en) zaškrtněte to jen při první změně domény.

## Uživatelé (při migraci na DEV prostředí)

Aby se náhodou nestalo, že odešlete např. žádost o schválení web stránky z vývojářského prostředí doporučuji změnit email adresy stávajících účtů na vhodné vývojářské účty.

Ověřte nastavení uživatele `admin`, nastavte mu práva na změny ve všech adresářích (např. nastavením režimu schvalování žádný), práva na moduly a nastavte vhodné heslo.

## Úkoly na pozadí

Zkontrolujte úlohy na pozadí, zejména nastavené URL adresy úloh. Na DEV prostředí případně smažte nepotřebné úlohy, na PROD prostředí v případě clusteru zkontrolujte nastavení uzlu na kterém se úloha má provádět.

## Mazání dat (při migraci na DEV prostředí)

Aby lokální databáze na DEV prostředí nebyla zbytečně velká doporučujeme provést mazání dat:
- statistiku ponechat jen za poslední 3 měsíce
- smazat kompletně emaily (ideálně přes `DELETE * FROM emails`)
- smazat monitorování serveru

## Ostatní

Zvažte specifické vlastnosti daného projektu a jeho nastavení a aplikujte je. Zamyslete se, zda neobsahuje ještě specificky nastavení např. v adresářové struktuře (volná pole) a podobně.

## Logy

Zkontrolujte logy aplikačního serveru a opravte případné chyby během startu (např. z důvodu špatného autoupdate.xml a podobně).
