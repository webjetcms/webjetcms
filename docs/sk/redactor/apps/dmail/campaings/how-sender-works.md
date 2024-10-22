# Ako funguje odosielanie emailov

Odosielanie emailov kampane vykonáva na pozadí tzv. ```Sender```. Ten pracuje nasledovným spôsobom:

- Z databázy (tabuľka ```emails```) sa vyberie 50 emailov na odoslanie.
- Z vybraných emailov sa vyberie náhodný email.
- Pri výbere náhodného emailu sa kontrolujú doménové limity, ak sa vybraný email nedá odoslať hľadá sa iný.
- Ak vybraná vzorka 50 emailov obsahuje všetky s rovnakou doménou (napr. ```gmail.com``` alebo vašu firemnú doménu) môže nastať situácia, že kvôli doménovým limitom sa **nedá zo vzorky 50 emailov žiaden vhodný vybrať**.
- Pre vybraný email sa zvýši počítadlo pokusov odoslania a nastaví sa dátum odoslania na aktuálny dátum a čas (aj keď email ešte nie je odoslaný, znižuje to ale pravdepodobnosť duplicitného odoslania emailu v prípade clustrovej inštalácie).
- Ak počet odoslaní presahuje maximálny počet pokusov email sa označí ako nesprávny (hodnota ```retry``` v tabuľke bude mať hodnotu ```-1```).
- Cez HTTP protokol sa stiahne web stránka s textom a dizajnom emailu.
- Ak je príjemca z databázy registrovaných používateľov vo WebJET CMS daný používateľ sa pri sťahovaní web stránky prihlási. V stránke je tak možné využívať [značky pre vloženie údajov používateľa](../campaings/README.md#základné).
- Ak príjemca nie je z databázy registrovaných používateľov nahradia sa len [základné značky](../campaings/README.md#základné).
- Ak je v tele emailu aplikácia môže do tela vygenerovať text ```SENDER: DO NOT SEND THIS EMAIL```, v takom prípade sa email neodošle a označí sa za úspešne odoslaný. Je to možné použiť v prípade, ak aplikácia kontroluje nastavenie používateľa - napr. má záujem dostávať len ponuku na 3-izbový byt, ktorý ale aktuálne nemáte v ponuke.
- K telu emailu sa priložia obrázky a prílohy.
- Telo emailu sa doplní o značku pre sledovanie kliknutí na odkaz v emaile.
- Do tela emailu sa priloží obrázok pre sledovanie otvorenia emailu.
- Email sa odošle.
- Z odpovedi SMTP servera sa vyhodnotí, či je email odoslaný úspešne (okrem stavového kódu je možné nastaviť doplnkové chybové odpovede v konf. premennej ```dmailBadEmailSmtpReplyStatuses```)
- Ak je odoslanie neúspešné zmaže sa v databáze dátum odoslania emailu a odosielanie sa zastaví na čas nastavený v konf. premennej ```dmailSleepTimeAfterException```.

## Správne nastavenie

Pre korektne odoslaný hromadný email je potrebné mať správne nastavený emailový server:

- Nastavené [DKIM](https://www.dkim.org) kľúče domény s platným [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) záznamom. Odporúčame použiť na odosielanie [Amazon SES](../../../../install/config/README.md#nastavenie-amazon-ses) a `DKIM` nastaviť tam, automaticky sa nastaví aj `SPF`.
- Nastavený [DMARC](https://dmarc.org) záznam. V DNS vytvorte nový `TXT` záznam pre doménu `_dmarc.vasadomena.sk` s hodnotou minimálne `v=DMARC1; p=none; sp=none`.

## Prečo odoslanie trvá dlho

Ak sa vám zdá, že odosielanie trvá príliš dlho, toto sú možné dôvody:

- Odosielanie pracuje ako úloha na pozadí, inicializuje sa pri štarte servera. E-mail odosiela pravidelne každých 1000ms (nastavuje sa v konf. premennej ```dmailWaitTimeout```). Táto hodnota je čakanie medzi vykonaniami odoslania, čiže po odoslaní emailu sa znova spustí odosielanie za nastavený počet ms. Ak nastavíte hodnotu 333 neznamená to, že sa odošlú 3 emaily za sekundu (to by celý proces odosielania musel trvať 0ms, čo isto nie je realita).
- Odosielanie emailov je **blokujúce**, ak sa počas intervalu nastaveného cez konf. premennú ```dmailWaitTimeout``` nestihne email odoslať, interval sa preskočí a bude sa odosielať v ďalšom intervale.
- Doménové limity - pre lepšie doručenie sa kontroluje [limit počtu a rýchlosti doručenia emailu na konkrétnu doménu](../domain-limits/README.md). Ak kampaň obsahuje veľa emailov rovnakej domény (napr. gmail.com alebo vašej firemnej domény) bude dochádzať k oneskoreniu odoslania. Ak vaša firemná doméne nelimituje počet emailov pridajte ju k doménovým limitom a nastavte vysoký počet emailov za časový úsek.
- Výkon databázy - ako je uvedené vyššie, pri odosielaní sa z databázy vyberá náhodná vzorka emailov na odoslanie, z ktorej sa vyberie email. Ak databázová tabuľka ```emails``` obsahuje veľa záznamov môže tento výber trvať dlhšiu dobu, čo spomaľuje odosielanie. Môžete v aplikácii Nastavenia->Mazanie dát->E-maily zmazať staré informácie o odoslaných emailoch, čo zníži zaťaženie databázy.
- Rýchlosť servera - ako je uvedené vyššie, každý email sa sťahuje ako web stránka lokálnym HTTP spojením. Ak má server nedostatočný výkon dochádza aj pri tomto sťahovaní k zdržaniu. Môžete sledovať v sekcii Prehľad aplikáciu Monitorovanie servera pre overenie výkonu v čase odosielania kampane. Ideálne je, keď sa email sťahuje lokálne priamo z aplikačného servera bez prechodu celou infraštruktúrou (firewall, load balancer...). Môžete využiť konf. premennú ```natUrlTranslate``` cez ktorú viete nastaviť preklad adries (napr. ```https://www.domena.sk/|http://localhost:8080/```).

Nasledovné konfiguračné premenné ovplyvňujú rýchlosť odosielania:

- ```dmailWaitTimeout``` - interval spúšťania odoslania emailu v milisekundách. Po zmene je potrebné reštartovať server (predvolene 1000).
- ```dmailMaxRetryCount``` - maximálny počet opakovaní odoslania emailu ak dôjde k chybe pri odosielaní (predvolene 5).
- ```dmailSleepTimeAfterException``` - interval čakania v ms po chybe odosielania, napr. ak SMTP server prestane odpovedať (predvolene 20000),
- ```dmailBadEmailSmtpReplyStatuses``` - zoznam čiarkou oddelených výrazov vrátených zo SMTP servera pre ktoré sa email nebude znova pokúšať odoslať (predvolene: Invalid Addresses,Recipient address rejected,Bad recipient address,Local address contains control or whitespace,Domain ends with dot in string,Domain contains illegal character in string).
- ```dmailDisableInlineImages``` - umožňuje vypnúť prikladanie obrázkov do emailu, čo zvýši rýchlosť odoslania a zníži veľkosť emailu. Nevýhodou je, že príjemca musí potvrdiť načítanie obrázkov zo servera. Ak máte viac doménovú inštaláciu môžete výnimky pre prikladanie obrázkov do emailu nastaviť cez konf. premennú ```dmailWhitelistImageDomains``` (pre nastavené domény sa obrázky priložia).

Ďalšie konf. premenné, ktoré je možné nastaviť:

- ```useSMTPServer``` - povoľuje/úplne vypína odosielanie všetkých emailov zo servera (predvolene ```true```).
- ```disableDMailSender``` - vypne len odosielanie hromadných emailov (predvolene ```false```).
- ```senderRunOnNode``` - ak používate cluster viacerých aplikačných serverov umožňuje nastaviť čiarkou oddelený zoznam mien ```nodu```, z ktorého budú odosielané hromadné emaily. Upozornenie: ak sú emaily odosielané z viacerých ```nodov``` môže dôjsť k duplicitnému odoslaniu emailu.
- ```dmailTrackopenGif``` - virtuálna cesta k obrázku, ktorá indikuje otvorenia emailu (predvolene ```/components/dmail/trackopen.gif```).
- ```dmailStatParam``` - názov URL parametra pre štatistiku kliknutí (predvolene ```webjetDmsp```).
- ```replaceExternalLinks``` - ak je nastavené na ```true``` budú sa nahrádzať aj externé odkazy presmerovaním cez server kde je spustený hromadný email pre sledovanie štatistiky (predvolene ```false```).

## Nastavenia pre zrýchlenie

Ak potrebujete urýchliť odosielanie môžete postupovať nasledovne:

- Zvýšte doménové limity, odporúčame nastaviť vyššie limity na domény ```gmail.com``` a vašu firemnú doménu.
- Upravte ```dmailWaitTimeout``` na hodnotu ```500```, čo zvýši rýchlosť volania odoslania emailu, z dôvodu blokovania (viď vyššie). To ale neznamená, že sa email odošle každých 500ms.
- Ak databáza obsahuje veľa neplatných emailov znížte ```dmailSleepTimeAfterException```. **Upozornenie:**, ak skutočne nastane výpadok vášho SMTP servera, tak sa emaily veľmi rýchlo označia ako odoslané, pretože pretečie počet ```dmailMaxRetryCount```.
- Nastavte ```natUrlTranslate``` pre priame sťahovanie textu emailu z lokálneho aplikačného servera. Ak máte viac doménovú inštaláciu môže nastať problém s výberom správnej domény. Odporúčame v ```hosts``` súbore na serveri nastaviť všetky domény na IP adresu 127.0.0.1, v takom prípade nastavíte len presmerovanie portu z 80 na 8080 (alebo na akom porte máte spustený lokálny aplikačný server).
- Minimalizujte obrázky a prílohy. Tie zvyšujú záťaž na server a objem emailu. Prípadne nastavte konf. premennú ```dmailDisableInlineImages``` na ```false``` pre vypnutie prikladania obrázkov priamo do tela emailu.
- Ak máte cluster môžete povoliť odosielanie z viacerých nodov paralelne, zvyšuje sa ale riziko viac duplicitného odoslania emailu príjemcovi. Zoznam nodov, z ktorých sa email odosiela sa nastavuje v konf. premennej ```senderRunOnNode```.