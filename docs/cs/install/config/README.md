# Konfigurace

Nejčastěji používané [konfigurační proměnné](../../admin/setup/configuration/README.md).

## Přihlášení

- `logLevel` - základní úroveň protokolování, může mít hodnotu `debug` pro podrobné protokolování nebo `normal` pro produkční nasazení.
- `logLevels` - seznam balíčků java s úrovní protokolování (každý na novém řádku), např:

```
sk.iway=DEBUG
sk.iway.iwcm=WARN
org.springframework=WARN
```

## Odesílání e-mailů

Chcete-li odesílat e-maily, musíte správně nastavit server SMTP:
- `smtpServer` - Adresa serveru SMTP pro odesílání e-mailů.
- `smtpUseSSL` - nastavením na hodnotu `true` aktivovat používání protokolu SSL.
- `smtpUseTLS` - `TLS` ověřování - pokud je port 587, `smtpUseTLS` musí být `true`.
- `smtpTLSVersion` - verze `TLS` pro připojení smtp.
- `smtpUser` - přihlašovací jméno.
- `smtpPassword` - heslo.
- `smtpPort` - port pro připojení k serveru SMTP.
- `useSMTPServer` - zakázat předávání e-mailů (např. pro uzly clusteru, které nemají k dispozici server SMTP).
- `smtpConnectionTimeoutMillis` - počet milisekund, po které se má čekat na navázání spojení SMTP.
- `emailProtectionSenderEmail` - nastavit e-mailovou adresu, která se použije jako e-mail odesílatele pro všechny e-maily, pokud není nastaven protokol SMTP. `OPEN RELAY`. Typická hodnota je `noreply@domena.sk`. Při nastavení se zadaná e-mailová adresa nastaví v záhlaví každého e-mailu. `FROM` a původně nastavená hodnota z `FROM` je nastavena na hodnotu `REPLY-TO`.

Hromadné e-maily můžete stále nastavit:
- `dmailWaitTimeout` - rychlost odesílání e-mailů z hromadných e-mailů v milisekundách. Ve výchozím nastavení je nastavena na 5000, což znamená, že e-mail je odeslán jednou za 5 sekund. Pokud hodnotu snížíte, budou webový server a server SMTP při odesílání e-mailů více zatíženy. Hodnota se projeví až po restartu serveru.
- `dmailBadEmailSmtpReplyStatuses` - Čárkou oddělený seznam výrazů vrácených ze serveru SMTP, pro které se e-mail nebude pokoušet znovu odeslat.

### Nastavení služby Amazon SES

Pro hromadné zasílání e-mailů doporučujeme použít [Služba Amazon Simple Email Services/SES](https://aws.amazon.com/ses/) pro lepší doručování e-mailů. Původně WebJET CMS používal přístup API, který se aktivoval nastavením konfigurační proměnné. `useAmazonSES` na hodnotu `true`, ale v současné době se již používá [standardní protokol SMTP](https://docs.aws.amazon.com/ses/latest/dg/send-email-smtp.html) v Amazon SES:
- Vyberte [adresa serveru SMTP](https://docs.aws.amazon.com/general/latest/gr/ses.html) pro váš region a nastavte ji v proměnné conf. `smtpServer`, např. `email-smtp.eu-west-1.amazonaws.com`. Tabulky lze na stránce posouvat, zpočátku je vidět pouze oblast USA, nebojte se tabulku posouvat.
- Vytvořit [přihlašovací údaje](https://docs.aws.amazon.com/ses/latest/dg/smtp-credentials.html) na server SMTP a nastavte je na proměnné conf. `smtpUser` a `smtpPassword`, vyberte možnost Šifrovat heslo.
- Na stránce [Amazon SES](https://console.aws.amazon.com/ses/) v části nastavení SMTP pro vybranou oblast můžete také vidět jednotlivé porty, přes které komunikuje, obvykle je nutné povolit komunikaci na portu 587 na firewallu.
- U nového projektu po otestování požádejte o zvýšení limitů pro odesílání e-mailů, které jsou ve výchozím nastavení nastaveny nízko.
- Nastavení konfigurační proměnné `smtpUseTLS` na adrese `true`.
- V [Amazon SES](https://console.aws.amazon.com/ses/) v části Identity je třeba ověřit identitu domény a nastavit `DKIM` Klíče.
- Odstranění konf. proměnné `useAmazonSES` pokud jste ji nastavili (u starších projektů, kde se původně používal přístup přes API).
- Restartujte aplikační server.
- Zkuste odeslat e-mail a v hlavičce e-mailu ověřte, že byl skutečně odeslán prostřednictvím služby Amazon SES.

Odesílání prostřednictvím sad Amazon SES `DKIM` záhlaví a zajistit vyšší doručitelnost e-mailů.

## Cluster

V případě instalace clusteru musí WebJET vědět, že běží v clusteru, a musí synchronizovat interní paměť cache.

Může běžet v módě `auto` když nepotřebuje seznam uzlů, nebo v režimu, kdy je má přesně vypsané.

### Automatický režim

Nejjednodušší je spuštění v automatickém režimu, konf. proměnná `clusterNames` nastavit na `auto` a restartujte server. V tomto případě se název uzlu/`nodu` se použije první nalezená hodnota z:
- Proměnná ENV `HOSTNAME` server
- hodnota volání `InetAddress.getLocalHost().getHostName()` - název domény počítače
- hodnota volání `InetAddress.getLocalHost().getHostAddress()` - IP adresa počítače
- Hodnota `"auto-"+Tools.getNow()`

Hodnota je zkrácena na prvních 16 znaků. Pokud je proměnná `clusterHostnameTrimFromEnd` nastavit na `true`, použije se posledních 16 znaků (např. kubernetes vytvoří `hostname` s náhodnou hodnotou na konci).

### Přesný seznam uzlů

Pokud máte stabilní konfiguraci běžících uzlů/`nodes` nastavit proměnnou conf:
- `clusterNames=node1,node2,node3` - seznam uzlů oddělených čárkou od 1 do N

Jednotlivé uzly je třeba definovat externě. `ID` uzlu, nelze to provést přes Nastavení->Konfigurace, protože všechny uzly by měly stejný název.

Doporučuje se nastavit hodnotu pomocí parametru `-DwebjetNodeId=1` nebo jinak prostřednictvím [externí konfigurace](../external-configuration.md).

### Ostatní konf. proměnné

- `clusterRefreshTimeout` - interval synchronizace uzlů v ms, výchozí `5000ms`. Pro výrobu obvykle stačí ke snížení zátěže hodnota 60000 (1 minuta).
- `clusterMyNodeType` - typ uzlu - `full` = administrativní a prezentační část, `public`=pouze prezentační část bez administrace.
- `senderRunOnNode` - pokud je nastaven na neprázdnou hodnotu, obsahuje seznam uzlů clusteru, na kterých se spustí hromadné odesílání e-mailů (např. node1 nebo node1,node2). Doporučuje se spouštět odesílání pouze v uzlu správy.

## Generátor primárního klíče

Pro některé části se v minulosti používal generátor primárního klíče, lze nastavit následující hodnoty:
- `pkeyGenIncrement` - hodnota, o kterou se zvyšuje.
- `pkeyGenOffset` - hodnota posunu pro shluk.
- `pkeyGenBlockSize` - velikost výběru bloku pro generátor primárního klíče. Ve výchozím nastavení je nastavena na 10, pro server s vysokou zátěží ji doporučujeme nastavit na vyšší hodnotu (100 - 1000).

Aby se předešlo konfliktům v konfiguraci clusteru, použije se hodnota `pkeyGenOffset` pro posun uzlů. Např. hodnota `pkeyGenIncrement` je nastavena na 5 a `offset` na 0-5 pro jednotlivé uzly. V režimu `auto` clustra je automaticky nastavená hodnota `pkeyGenBlockSize=1` aby se vždy načetla poslední hodnota z databáze. To má mírný dopad na výkon serveru.

## Licence

Některé knihovny mohou potřebovat zakoupit další licence pro své použití:
- `amchartLicense` - licenční číslo knihovny [amCharts](https://www.amcharts.com) zobrazit grafy, po nastavení licenčního klíče se v grafu nezobrazí logo amCharts.
