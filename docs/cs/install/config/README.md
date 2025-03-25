# Základní konfigurace

Nejčastěji používané [konfigurační proměnné](../../admin/setup/configuration/README.md).

## Logování

- `logLevel` - základní úroveň logování, může mít hodnotu `debug` pro podrobné logování, nebo `normal` pro produkční nasazení.
- `logLevels` - seznam java balíků s úrovní logování (každý na novém řádku), např.:

```
sk.iway=DEBUG
sk.iway.iwcm=WARN
org.springframework=WARN
```

## Odesílání emailů

Pro odesílání emailů je třeba nastavit korektně SMTP server:
- `smtpServer` - adresa SMTP serveru pro odesílání emailů.
- `smtpUseSSL` - nastavením na `true` aktivujete použití SSL.
- `smtpUseTLS` - `TLS` autentifikace - je-li port 587, `smtpUseTLS` musí být `true`.
- `smtpTLSVersion` - verze `TLS` pro smtp spojení.
- `smtpUser` - přihlašovací jméno.
- `smtpPassword` - heslo.
- `smtpPort` - port pro připojení na SMTP server.
- `useSMTPServer` - vypnutí odesílání emailů (např. pro uzly clusteru, které nemají dostupný SMTP server).
- `smtpConnectionTimeoutMillis` - počet milisekund pro čekání na vytvoření SMTP spojení.
- `emailProtectionSenderEmail` - nastavte email adresu, která se použije jako email odesílatele pro všechny emaily, pokud SMTP nemá nastaven `OPEN RELAY`. Typická hodnota je `noreply@domena.sk`. Při nastavení se zadaná emailová adresa nastaví do každého emailu do hlavičky `FROM` a původně nastavená hodnota z `FROM` se nastaví do `REPLY-TO`.

Pro hromadný email lze ještě nastavit:
- `dmailWaitTimeout` - rychlost odesílání emailů z hromadného emailu v milisekundách. Standardně nastaveno na 5000, což znamená, že se email odešle jednou za 5 sekund. Pokud hodnotu snížíte tak při odesílání emailů bude více zatížen web server a SMTP server. Hodnota se projeví až po restartu serveru.
- `dmailBadEmailSmtpReplyStatuses` - seznam čárkou oddělených výrazů vrácených ze SMTP serveru pro které se email nebude znovu pokoušet odeslat.

Pro různé systémové notifikace lze nastavit jméno a email odesílatele:
- `defaultSenderName` - jméno odesílatele - například jméno firmy
- `defaultSenderEmail` - email adresa odesílatele - například `no-reply@company-name.com`

konfigurační hodnotu lze nastavit speciálně pro moduly pomocí prefixu, jako například `reservationDefaultSenderEmail`. Pokud taková hodnota existuje použije se prioritně před hodnotou `defaultSenderEmail`. Lze použít následující prefixy:
- `dmail` - odesílatel nového hromadného emailu.
- `formmail` - odesílatel oznámení pro návštěvníka, který vyplnil formulář.
- `reservation` - odesílatel schválení/zamítnutí rezervace.

### Nastavení Amazon SES

Pro hromadný email doporučujeme použít službu [Amazon Simple Email Services/SES](https://aws.amazon.com/ses/) pro lepší doručování emailů. Původně WebJET CMS používal API přístup, který se aktivoval nastavením konf. proměnné `useAmazonSES` na hodnotu `true`, aktuálně už je ale používán [standardní SMTP protokol](https://docs.aws.amazon.com/ses/latest/dg/send-email-smtp.html) ve službě Amazon SES:
- Vyberte [adresu SMTP serveru](https://docs.aws.amazon.com/general/latest/gr/ses.html) pro váš region a nastavte ji do konf. proměnné `smtpServer` Např. `email-smtp.eu-west-1.amazonaws.com`. Tabulky jsou na stránce rolovatelné, vidět nejprve jen US region, nebojte se tabulku rolovat.
- Vytvořte [přihlašovací údaje](https://docs.aws.amazon.com/ses/latest/dg/smtp-credentials.html) na SMTP server a nastavte je do konf. proměnných `smtpUser` a `smtpPassword`, pro heslo zvolte možnost Šifrovat.
- Na stránce [Amazon SES](https://console.aws.amazon.com/ses/) v sekci `SMTP settings` je pro zvolený region vidět i jednotlivé porty, přes které komunikuje, typicky je třeba na `firewall` povolit komunikaci na port 587.
- Pro nový projekt po otestování požádejte o navýšení limitů odesílání emailů, standardně jsou nastaveny nízko.
- Nastavte konf. proměnnou `smtpUseTLS` na `true`.
- V [Amazon SES](https://console.aws.amazon.com/ses/) v sekci `Identities` je třeba verifikovat identitu domény a nastavit `DKIM` klíče.
- Smažte konf. proměnnou `useAmazonSES` pokud ji máte nastavenou (u starších projektů kde se původně používal API přístup).
- Restartujte aplikační server.
- Vyzkoušejte odeslat email, ověřte v hlavičkách emailu, že byl skutečně odeslán přes službu Amazon SES.

Odesílání přes Amazon SES nastaví `DKIM` hlavičky a zajistí vyšší doručitelnost emailů.

## Clustr

V případě cluster instalace WebJET potřebuje vědět, že je provozován v clusteru a má synchronizovat interní cache paměti.

Umí běžet v módě `auto`, kdy nepotřebuje seznam uzlů, nebo v režimu, kdy je mě přesně jmenovány.

### Režim auto

Nejjednodušší je běžet v režimu auto, konf. proměnnou `clusterNames` nastavte na hodnotu `auto` a restartujte server. V takovém případě se jako jméno uzlu/`nodu` použije první nalezená hodnota z:
- ENV proměnná `HOSTNAME` serveru
- hodnota volání `InetAddress.getLocalHost().getHostName()` - doménové jméno počítače
- hodnota volání `InetAddress.getLocalHost().getHostAddress()` - IP adresa počítače
- hodnota `"auto-"+Tools.getNow()`

Hodnota je zkrácena na prvních 16 znaků. Je-li proměnná `clusterHostnameTrimFromEnd` nastavena na `true`, použije se koncových 16 znaků (např. kubernetes vytváří `hostname` s náhodnou hodnotou na konci).

### Přesný seznam uzlů

Pokud máte stabilní konfiguraci běžících uzlů/`nodes` nastavte konf. proměnnou:
- `clusterNames=node1,node2,node3` - čárkou oddělený seznam nodů od 1 po N

Jednotlivým uzlem potřebujete externě definovat `ID` uzlu, nemůžete to udělat přes Nastavení->Konfigurace, protože by všechny uzly měly stejný název.

Hodnotu doporučujeme nastavit přes parametr `-DwebjetNodeId=1`, případně jiným způsobem přes [externí konfiguraci](../external-configuration.md).

### Další konf. proměnné

- `clusterRefreshTimeout` - interval synchronizace uzlů v ms, výchozí `5000ms`. Pro produkci obvykle postačuje hodnota 60000 (1 minuta) pro snížení zatížení.
- `clusterMyNodeType` - typ uzlu - `full` = administrace i prezentační část, `public`=pouze prezentační část bez administrace.
- `senderRunOnNode` - pokud je nastaveno na jinou než prázdnou hodnotu obsahuje seznam nodů clusteru na kterých se spouští odesílání hromadných emailů (např. node1 nebo node1, node2). Doporučujeme odesílání spouštět pouze na administračním uzlu.

## Generátor primárních klíčů

Pro některé části se historicky používá generátor primárních klíčů, lze je nastavit následující hodnoty:
- `pkeyGenIncrement` - hodnota o kterou se zvyšuje.
- `pkeyGenOffset` - hodnota posunu pro cluster.
- `pkeyGenBlockSize` - velikost výběru bloku pro generátor primárních klíčů. Standardně nastaveno na hodnotu 10, pro server s vysokou zátěží doporučujeme nastavit na vyšší hodnotu (100 - 1000).

Aby nedocházelo ke konfliktu v cluster konfiguraci používá se hodnota `pkeyGenOffset` pro posun podle nodů. Např. hodnota `pkeyGenIncrement` je nastavena na 5 a `offset` na 0-5 pro jednotlivé uzly. Při režimu `auto` clusteru je automaticky nastavená hodnota `pkeyGenBlockSize=1` aby se vždy četla poslední hodnota z databáze. Má to mírný dopad na výkon serveru.

## Licence

Pro některé knihovny může být nutné zakoupit dodatečné licence k jejich použití:
- `amchartLicense` - licenční číslo pro knihovnu [amCharts](https://www.amcharts.com) pro zobrazení grafů, po nastavení licenčního klíče se nebude v grafu zobrazovat logo amCharts.
