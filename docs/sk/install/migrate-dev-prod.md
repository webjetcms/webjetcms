# Migrácia medzi prostrediami

Základné informácie, ktoré je potrebné vykonať pri migrácii medzi prostrediami, napr. produkčným a vývojovým prostredím.

## Konfiguračné premenné

Skontrolujte nasledovné konf. premenné:

- `cloudStaticFilesDir` - cesta k adresáru so statickými súbormi, je potrebné ju nastaviť správne na dané prostredie (prípadne ju na prostredí zmazať, ak sa externé statické súbory nepoužívajú).
- `enableStaticFilesExternalDir` - zapnutie/vypnutie používania externého adresára pre statické súbory.
- `smtpServer` - adresa SMTP servera + premenné `smtpUsername, smtpPassword, smtpPort, smtpUseTLS` a podobne.
- `emailProtectionSenderEmail` - nastavte email adresu, ktorá sa použije ako email odosielateľa, môže byť rozdielna pre jednotlivé prostredia, musí byť povolená na SMTP serveri.
- `proxyHost/proxyPort` - nastavenie proxy
- `webEnabledIPs` - zoznam IP adries z ktorých je dostupný web, na DEV prostredí nastaviť predvolene na `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,#klient,`
- `adminEnabledIPs` - zoznam IP adries z ktorých je možné pristupovať do administrácie, predvolene nastaviť na `#localhost,127.0.0.1,10.,192.168.,#interway,85.248.107.8,195.168.35.4,#klient,`
- `multidomainAdminHost` - ak je nastavená overte doménu, používa sa na nastavenie domény pre CMS na viac doménovej inštalácii.
- `serverBeyoundProxy` - nastavenie umiestnenia aplikačného servera za proxy/load balancer.
- `logLevel` - na produkcii ponechať na `normal`, na vývojárskom prostredí môžete zmeniť na `debug`.

Skontrolujte všetky ostatné konf. premenné a zvážte ich dopad na projekt a vhodnosť nastavenia.

## Doménové adresy

Ak web používa `multidomain` je potrebné korektne nastaviť domény. Pri migrácii medzi prostrediami skontrolujte všetky koreňové adresáre a nastavte správne doménové mená.
Zaškrtnite aj možnosť Zmeniť presmerovania domény, ak je ale doména nastavená vo viacerých koreňových priečinkoch (napr. na sk aj en) zaškrtnite to len pri prvej zmene domény.

## Používatelia (pri migrácii na DEV prostredie)

Aby sa náhodou nestalo, že odošlete napr. žiadosť o schválenie web stránky z vývojárskeho prostredia odporúčam zmeniť email adresy existujúcich účtov na vhodné vývojárske účty.

Overte nastavenia používateľa `admin`, nastavte mu práva na zmeny vo všetkých adresároch (napr. nastavením režimu schvaľovania žiaden), práva na moduly a nastavte vhodné heslo.

## Úlohy na pozadí

Skontrolujte úlohy na pozadí, hlavne nastavené URL adresy úloh. Na DEV prostredí prípadne zmažte nepotrebné úlohy, na PROD prostredí v prípade clustra skontrolujte nastavenie uzla na ktorom sa úloha má vykonávať.

## Mazanie dát (pri migrácii na DEV prostredie)

Aby lokálna databáza na DEV prostredí nebola zbytočne veľká odporúčame vykonať mazanie dát:

- štatistiku ponechať len za posledné 3 mesiace
- zmazať kompletne emaily (ideálne cez `DELETE * FROM emails`)
- zmazať monitorovanie servera

## Ostatné

Zvážte špecifické vlastnosti daného projektu a jeho nastavenia a aplikujte ich. Zamyslite sa, či neobsahuje ešte špecificky nastavenia napr. v adresárovej štruktúre (voľné polia) a podobne.

## Logy

Skontrolujte logy aplikačného servera a opravte prípadné chyby počas štartu (napr. z dôvodu zlého autoupdate.xml a podobne).