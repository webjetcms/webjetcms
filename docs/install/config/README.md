# Konfigurácia

Najčastejšie používané konfiguračné premenné.

## Logovanie

- `logLevel` - základná úroveň logovania, môže mať hodnotu `debug` pre podrobné logovanie, alebo `normal` pre produkčné nasadenie.
- `logLevels` - zoznam java balíkov s úrovňou logovania (každý na novom riadku), napr:

```
sk.iway=DEBUG
sk.iway.iwcm=WARN
org.springframework=WARN
```

## Odosielanie emailov

Pre odosielanie emailov je potrebné nastaviť korektne SMTP server:

- `smtpServer` - adresa SMTP servera pre odosielanie emailov.
- `smtpUseSSL` - nastavením na `true` aktivujete použitie SSL.
- `smtpUseTLS` - `TLS` autentifikácia - ak je port 587, `smtpUseTLS` musí byť `true`.
- `smtpTLSVersion` - verzia `TLS` pre smtp spojenie.
- `smtpUser` - prihlasovacie meno.
- `smtpPassword` - heslo.
- `smtpPort` - port pre pripojenie na SMTP server.
- `useSMTPServer` - vypnutie odosielania emailov (napr. pre uzly clustra, ktoré nemajú dostupný SMTP server).
- `smtpConnectionTimeoutMillis` - počet milisekúnd pre čakanie na vytvorenie SMTP spojenia.

Pre hromadný email je možné ešte nastaviť:

- `dmailWaitTimeout` - rýchlosť odosielania emailov z hromadného emailu v milisekundách. Štandardne nastavené na 5000, čo znamená že sa email odošle raz za 5 sekúnd. Ak hodnotu znížite tak pri odosielaní emailov bude viac zaťažený web server a SMTP server. Hodnota sa prejaví až po reštarte servera.
- `dmailBadEmailSmtpReplyStatuses` - zoznam čiarkou oddelených výrazov vrátených zo SMTP servera pre ktoré sa email nebude znova pokúšať odoslať.

## Cluster

V prípade cluster inštalácie WebJET potrebuje vedieť, že je prevádzkovaný v clustri a ma synchronizovať interné cache pamäte.

Vie bežať v móde `auto`, kedy nepotrebuje zoznam uzlov, alebo v režime, kedy ich ma presne vymenované.

### Režim auto

Najjednoduchšie je bežať v režime auto, konf. premennú `clusterNames` nastavte na hodnotu `auto` a reštartnite server. V takomto prípade sa ako meno uzla/`nodu` použije prvá nájdená hodnota z:

- ENV premenná `HOSTNAME` servera
- hodnota volania `InetAddress.getLocalHost().getHostName()` - doménové meno počítača
- hodnota volania `InetAddress.getLocalHost().getHostAddress()` - IP adresa počítača
- hodnota `"auto-"+Tools.getNow()`

Hodnota je skrátená na prvých 16 znakov. Ak je premenná `clusterHostnameTrimFromEnd` nastavená na `true`, použije sa koncových 16 znakov (napr. kubernetes vytvára `hostname` s náhodnou hodnotou na konci).

### Presný zoznam uzlov

Ak mate stabilnú konfiguráciu bežiacich uzlov/`nodes` nastavte konf. premennú:

- `clusterNames=node1,node2,node3` - čiarkou oddelený zoznam nodov od 1 po N

Jednotlivým uzlom potrebujete externe definovať `ID` uzla, nemôžete to spraviť cez Nastavenia->Konfigurácia, pretože by všetky uzly mali rovnaký názov.

Hodnotu odporúčame nastaviť cez parameter `-DwebjetNodeId=1`, prípadne iným spôsobom cez [externú konfiguráciu](../external-configuration.md).

### Ďalšie konf. premenné

- `clusterRefreshTimeout` - interval synchronizácie uzlov v ms, predvolene `5000ms`. Pre produkciu zvyčajne postačuje hodnota 60000 (1 minúta) pre zníženie zaťaženia.
- `clusterMyNodeType` - typ uzla - `full` = administrácia aj prezentačná časť, `public`=iba prezentačná časť bez administrácie.
- `senderRunOnNode` - ak je nastavené na inú ako prázdnu hodnotu obsahuje zoznam nodov clustra na ktorých sa spúšťa odosielanie hromadných emailov (napr. node1 alebo node1,node2). Odporúčame odosielanie spúšťať len na administračnom uzle.

## Generátor primárnych kľúčov

Pre niektoré časti sa historicky používa generátor primárnych kľúčov, možné je nastaviť nasledovné hodnoty:

- `pkeyGenIncrement` - hodnota o ktorú sa zvyšuje.
- `pkeyGenOffset` - hodnota posunu pre cluster.
- `pkeyGenBlockSize` - veľkosť výberu bloku pre generátor primárnych kľúčov. Štandardne nastavené na hodnotu 10, pre server s vysokou záťažou odporúčame nastaviť na vyššiu hodnotu (100 - 1000).

Aby nedochádzalo ku konfliktu v cluster konfigurácii používa sa hodnota `pkeyGenOffset` pre posun podľa nodov. Napr. hodnota `pkeyGenIncrement` je nastavená na 5 a `offset` na 0-5 pre jednotlivé uzly. Pri režime `auto` clustra je automaticky nastavená hodnota `pkeyGenBlockSize=1` aby sa vždy čítala posledná hodnota z databázy. Má to mierny dopad na výkon servera.

