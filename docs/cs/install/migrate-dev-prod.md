# Migrace mezi prostředími

Základní informace, které je třeba provést při migraci mezi prostředími, např. produkčním a vývojovým prostředím.

## Konfigurační proměnné

Zkontrolujte následující konfigurační proměnné:
- `cloudStaticFilesDir` - cestu k adresáři statických souborů, je třeba ji v prostředí správně nastavit (nebo v prostředí odstranit, pokud se externí statické soubory nepoužívají).
- `enableStaticFilesExternalDir` - povolení/zakázání používání externího adresáře pro statické soubory.
- `smtpServer` - Adresa serveru SMTP + proměnné `smtpUsername, smtpPassword, smtpPort, smtpUseTLS` a podobně.
- `emailProtectionSenderEmail` - nastavit e-mailovou adresu, která se má použít jako e-mail odesílatele, může být pro každé prostředí jiná, musí být povolena na serveru SMTP.
- `proxyHost/proxyPort` - nastavení proxy serveru
- `webEnabledIPs` - seznam IP adres, ze kterých je web přístupný, v prostředí DEV nastavený ve výchozím nastavení na hodnotu `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,62.168.118.90,62.168.118.67,#klient,`
- `adminEnabledIPs` - seznam IP adres, ze kterých je možné přistupovat k administraci, ve výchozím nastavení nastaven na hodnotu `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,#klient,`
- `multidomainAdminHost` - pokud je nastavena na hodnotu ověřit doménu, slouží k nastavení domény pro CMS v případě instalace s více doménami.
- `serverBeyoundProxy` - Nastavení umístění aplikačního serveru za proxy serverem/vyrovnávačem zátěže.
- `logLevel` - na výrobu, abyste mohli odejít na `normal`, ve vývojovém prostředí můžete změnit na `debug`.

Zkontrolujte všechny ostatní konf. proměnné a zvažte jejich dopad na projekt a vhodnost prostředí.

## Doménové adresy

Pokud stránka používá `multidomain` je třeba správně nastavit domény. Při migraci mezi prostředími zkontrolujte všechny kořenové adresáře a nastavte správné názvy domén. Zkontrolujte také možnost Změnit přesměrování domény, ale pokud je doména nastavena ve více kořenových adresářích (např. en i en), zkontrolujte ji pouze při první změně domény.

## Uživatelé (při migraci do prostředí DEV)

Abyste se vyhnuli náhodnému odeslání žádosti o schválení webové stránky z vývojářského prostředí, doporučuji změnit e-mailové adresy stávajících účtů na příslušné vývojářské účty.

Ověření uživatelských nastavení `admin`, nastavte jeho oprávnění ke všem adresářům (např. nastavením režimu schvalování na žádný), oprávnění k modulům a nastavte vhodné heslo.

## Úkoly na pozadí

Zkontrolujte úlohy na pozadí, zejména nastavené adresy URL úloh. V prostředí DEV v případě potřeby odstraňte nepotřebné úlohy, v prostředí PROD v případě clusteru zkontrolujte nastavení uzlu, na kterém se má úloha provádět.

## Odstranění dat (při migraci do prostředí DEV)

Abyste zabránili zbytečnému zvětšování místní databáze v prostředí DEV, doporučujeme provést odstranění dat:
- vést statistiky pouze za poslední 3 měsíce
- úplně odstranit e-maily (nejlépe prostřednictvím `DELETE * FROM emails`)
- odstranění monitorování serveru

## Další

Zvažte specifické vlastnosti projektu a jeho nastavení a použijte je. Zvažte, zda obsahuje nějaká specifičtější nastavení, např. ve struktuře adresářů (volná pole) apod.

## Loga

Zkontrolujte protokoly aplikačního serveru a opravte případné chyby při spuštění (např. kvůli špatnému souboru autoupdate.xml atd.).
