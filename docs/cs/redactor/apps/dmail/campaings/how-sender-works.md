# Jak funguje odesílání e-mailů

Odesílání e-mailových kampaní probíhá na pozadí tzv. `Sender`. Funguje to takto:
- Z databáze (tabulka `emails`), bude vybráno 50 e-mailů k odeslání.
- Z vybraných e-mailů bude vybrán náhodný e-mail.
- Při výběru náhodného e-mailu se kontrolují limity domény, pokud vybraný e-mail nelze odeslat, vyhledá se jiný.
- Pokud vybraný vzorek 50 e-mailů obsahuje všechny e-maily se stejnou doménou (např. `gmail.com` nebo firemní domény), můžete zjistit, že kvůli omezením domény **ze vzorku 50 e-mailů nelze vybrat žádný vhodný**.
- Pro vybraný e-mail se zvýší počítadlo pokusů o odeslání a datum odeslání se nastaví na aktuální datum a čas (i když e-mail ještě nebyl odeslán, což snižuje pravděpodobnost duplicitního odeslání e-mailu v případě sdružené instalace).
- Pokud počet odeslání překročí maximální počet pokusů, je e-mail označen jako nesprávný (hodnota `retry` v tabulce bude mít hodnotu `-1`).
- Webová stránka s textem a designem e-mailu je stažena prostřednictvím protokolu HTTP.
- Pokud je příjemce z databáze registrovaných uživatelů v systému WebJET CMS, je uživatel při stahování webové stránky přihlášen. Tímto způsobem je možné použít [značky pro vkládání uživatelských dat](../campaings/README.md#Základní).
- Pokud příjemce není z databáze registrovaných uživatelů, pouze [základní značky](../campaings/README.md#Základní).
- Pokud je v těle e-mailu aplikace, může v těle vygenerovat text. `SENDER: DO NOT SEND THIS EMAIL`, v takovém případě se e-mail neodešle a bude označen jako úspěšně odeslaný. Toho lze využít v případě, že aplikace kontroluje nastavení uživatele - např. má zájem pouze o zaslání nabídky na byt 3+1, ale vy jej aktuálně nemáte v nabídce.
- Tělo e-mailu bude doplněno obrázky a přílohami.
- Tělo e-mailu bude aktualizováno pomocí značky pro sledování kliknutí na odkaz v e-mailu.
- K tělu e-mailu bude připojen obrázek, který umožní sledovat otevření e-mailu.
- E-mail bude odeslán.
- Z odpovědi serveru SMTP se vyhodnotí, zda byl e-mail úspěšně odeslán (kromě stavového kódu je možné v proměnné conf. nastavit další chybové odpovědi). `dmailBadEmailSmtpReplyStatuses`)
- Pokud je odeslání neúspěšné, datum odeslání e-mailu se v databázi vymaže a odesílání se zastaví na dobu nastavenou v proměnné conf. `dmailSleepTimeAfterException`.

## Správné nastavení

Pro správné odeslání hromadného e-mailu je nutné mít správně nakonfigurovaný e-mailový server:
- Nastavení [DKIM](https://www.dkim.org) klíče domény s platnými [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) záznamu. Doporučujeme používat pro zasílání [Amazon SES](../../../../install/config/README.md#nastavení-amazon-ses) a `DKIM` automaticky nastaví také `SPF`.
- Nastavení [DMARC](https://dmarc.org) záznam. V systému DNS vytvořte nový `TXT` záznam pro doménu `_dmarc.vasadomena.sk` s hodnotou alespoň `v=DMARC1; p=none; sp=none`.

## Proč odeslání trvá dlouho

Pokud máte pocit, že nahrávání trvá příliš dlouho, jsou možné tyto příčiny:
- Odesílání funguje jako úloha na pozadí, inicializovaná při spuštění serveru. Odesílá e-maily pravidelně každých 1000 ms (nastaveno v konfigurační proměnné `dmailWaitTimeout`). Tato hodnota představuje čekací dobu mezi provedením odeslání, to znamená, že po odeslání e-mailu se za nastavený počet ms začne odesílat znovu. Pokud nastavíte hodnotu 333, neznamená to, že se odešlou 3 e-maily za sekundu (to by celý proces odesílání trval 0 ms, což rozhodně není realita).
- Odesílání e-mailů je **blokování** pokud během intervalu nastaveného pomocí proměnné conf. `dmailWaitTimeout` chybí odeslat e-mail, bude interval přeskočen a bude odeslán v dalším intervalu.
- Omezení domény - pro lepší doručení jsou zkontrolovány [omezení počtu a rychlosti doručování e-mailů do určité domény.](../domain-limits/README.md). Pokud kampaň obsahuje mnoho e-mailů se stejnou doménou (např. gmail.com nebo vaše firemní doména), dojde ke zpoždění při odesílání. Pokud vaše firemní doména neomezuje počet e-mailů, přidejte ji k limitům domény a nastavte vysoký počet e-mailů na časový interval.
- Výkonnost databáze - jak bylo uvedeno výše, při odesílání se z databáze vybere náhodný vzorek e-mailů k odeslání, ze kterého se vybere e-mail. Pokud je v databázi tabulka `emails` obsahuje mnoho záznamů, může tento výběr trvat dlouho, což zpomaluje odesílání. V Nastavení->Odstranění dat->E-maily můžete odstranit staré informace o odeslaných e-mailech, což sníží zatížení databáze.
- Rychlost serveru - jak bylo uvedeno výše, každý e-mail se stahuje jako webová stránka prostřednictvím místního připojení HTTP. Pokud je server nedostatečně výkonný, dochází i k prodlevám při tomto stahování. Výkon v době odesílání kampaně si můžete ověřit podle sekce Přehled v aplikaci Sledování serveru. V ideálním případě se e-mail stahuje lokálně přímo z aplikačního serveru, aniž by procházel celou infrastrukturou (firewall, load balancer...). Můžete použít konf. proměnnou `natUrlTranslate` pomocí kterého můžete nastavit překlad adres (např. `https://www.domena.sk/|http://localhost:8080/`).

Rychlost odesílání ovlivňují následující konfigurační proměnné:
- `dmailWaitTimeout` - interval spouštění odesílání e-mailů v milisekundách. Po změně je nutné restartovat server (výchozí hodnota 1000).
- `dmailMaxRetryCount` - maximální počet opakování odeslání e-mailu v případě, že během odesílání dojde k chybě (výchozí hodnota 5).
- `dmailSleepTimeAfterException` - Interval čekání v ms po chybě odeslání, např. pokud server SMTP přestane odpovídat (výchozí 20000),
- `dmailBadEmailSmtpReplyStatuses` - seznam čárkami oddělených výrazů vrácených ze serveru SMTP, pro které se e-mail nepokusí znovu odeslat (výchozí: Neplatné adresy,Adresa příjemce odmítnuta,Špatná adresa příjemce,Místní adresa obsahuje řídicí znaky nebo bílé znaky,Doména končí tečkou v řetězci,Doména obsahuje nepovolený znak v řetězci).
- `dmailDisableInlineImages` - umožňuje zakázat připojování obrázků k e-mailu, což zvýší rychlost odesílání a zmenší velikost e-mailu. Nevýhodou je, že příjemce musí potvrdit, že obrázky byly načteny ze serveru. Pokud máte instalaci na více doménách, můžete nastavit výjimky pro připojování obrázků k e-mailu pomocí konfigurační proměnné. `dmailWhitelistImageDomains` (pro nastavené domény jsou obrázky přiloženy).

Další nastavitelné konfigurační proměnné:
- `useSMTPServer` - povolí/zakáže odesílání všech e-mailů ze serveru (ve výchozím nastavení `true`).
- `disableDMailSender` - zakáže pouze odesílání hromadných e-mailů (ve výchozím nastavení `false`).
- `senderRunOnNode` - pokud používáte cluster více aplikačních serverů, můžete nastavit seznam názvů oddělených čárkou. `nodu` ze kterého budou odesílány hromadné e-maily. Poznámka: pokud jsou e-maily odesílány z více `nodov` Mohou být odeslány duplicitní e-maily.
- `dmailTrackopenGif` - virtuální cesta k obrázku, která označuje, že e-mail byl otevřen (ve výchozím nastavení `/components/dmail/trackopen.gif`).
- `dmailStatParam` - název parametru URL pro statistiku kliknutí (výchozí hodnota `webjetDmsp`).
- `replaceExternalLinks` - pokud je nastavena na `true` externí odkazy budou také nahrazeny přesměrováním přes server, na kterém je spuštěn hromadný e-mail pro statistiky sledování (ve výchozím nastavení). `false`).

## Nastavení zrychlení

Pokud potřebujete zrychlit odesílání, můžete provést následující kroky:
- Zvýšení limitů domén, doporučujeme nastavit vyšší limity domén. `gmail.com` a doménou vaší společnosti.
- Upravit podle `dmailWaitTimeout` na hodnotu `500`, což zvýší rychlost volání pro odeslání e-mailu kvůli blokování (viz výše). To neznamená, že e-mail bude odeslán každých 500 ms.
- Pokud databáze obsahuje velké množství neplatných e-mailů, snižte. `dmailSleepTimeAfterException`. **Varování:** pokud váš server SMTP skutečně vypadne, budou e-maily velmi rychle označeny jako odeslané, protože počet `dmailMaxRetryCount`.
- Sada `natUrlTranslate` pro přímé stahování textu e-mailu z místního aplikačního serveru. Pokud máte instalaci s více doménami, může nastat problém s výběrem správné domény. Doporučujeme v `hosts` v souboru serveru nastavíte všechny domény na IP adresu 127.0.0.1, v takovém případě nastavíte pouze přesměrování portů z 80 na 8080 (nebo jakýkoli port, na kterém běží místní aplikační server).
- Minimalizujte obrázky a přílohy. Ty zvyšují zatížení serveru a objem e-mailu. Případně nastavte konfigurační proměnnou `dmailDisableInlineImages` na adrese `false` zakázat připojování obrázků přímo do těla e-mailu.
- Pokud máte cluster, můžete povolit paralelní odesílání z více uzlů, ale tím se zvyšuje riziko, že příjemci bude odesláno více duplicitních e-mailů. Seznam uzlů, ze kterých se e-mail odesílá, se nastavuje v proměnné conf. `senderRunOnNode`.
