# Jak funguje odesílání emailů

Odesílání emailů kampaně provádí na pozadí tzv. `Sender`. Ten pracuje následujícím způsobem:
- Z databáze (tabulka `emails`) se vybere 50 emailů k odeslání.
- Z vybraných emailů se vybere náhodný email.
- Při výběru náhodného emailu se kontrolují doménové limity, pokud vybraný email nelze odeslat hledá se jiný.
- Pokud vybraný vzorek 50 emailů obsahuje všechny se stejnou doménou (např. `gmail.com` nebo vaši firemní doménu) může nastat situace, že kvůli doménovým limitům se **nedá ze vzorku 50 emailů žádný vhodný vybrat**.
- Pro vybraný email se zvýší počítadlo pokusů odeslání a nastaví se datum odeslání na aktuální datum a čas (i když email ještě není odeslán, snižuje to ale pravděpodobnost duplicitního odeslání emailu v případě clusterové instalace).
- Pokud počet odeslání přesahuje maximální počet pokusů email se označí jako nesprávný (hodnota `retry` v tabulce bude mít hodnotu `-1`).
- Přes HTTP protokol se stáhne web stránka s textem a designem emailu.
- Pokud je příjemce z databáze registrovaných uživatelů ve WebJET CMS daný uživatel se při stahování web stránky přihlásí. Ve stránce je tak možné využívat [značky pro vložení údajů uživatele](../campaings/README.md#základní).
- Pokud příjemce není z databáze registrovaných uživatelů nahradí se pouze [základní značky](../campaings/README.md#základní).
- Pokud je v těle emailu aplikace může do těla vygenerovat text `SENDER: DO NOT SEND THIS EMAIL`, v takovém případě se email neodešle a označí se za úspěšně odeslán. To lze použít v případě, že aplikace kontroluje nastavení uživatele - například. má zájem dostávat jen nabídku na 3-pokojový byt, který ale aktuálně nemáte v nabídce.
- K tělu emailu se přiloží obrázky a přílohy.
- Tělo emailu se doplní o značku pro sledování kliknutí na odkaz v emailu.
- Do těla emailu se přiloží obrázek pro sledování otevření emailu.
- Email se odešle.
- Z odpovědi SMTP serveru se vyhodnotí, zda je email odeslán úspěšně (kromě stavového kódu lze nastavit doplňkové chybové odpovědi v konf. proměnné `dmailBadEmailSmtpReplyStatuses`)
- Pokud je odeslání neúspěšné smaže se v databázi datum odeslání emailu a odesílání se zastaví na čas nastavený v konf. proměnné `dmailSleepTimeAfterException`.

## Správné nastavení

Pro korektně odeslaný hromadný email je třeba mít správně nastavený emailový server:
- Nastaveno [DKIM](https://www.dkim.org) klíče domény s platným [SPF](https://sk.wikipedia.org/wiki/Sender_Policy_Framework) záznamem. Doporučujeme použít k odesílání [Amazon SES](../../../../install/config/README.md#nastavení-amazon-ses) a `DKIM` nastavit tam, automaticky se nastaví i `SPF`.
- Nastaven [DMARC](https://dmarc.org) záznam. V DNS vytvořte nový `TXT` záznam pro doménu `_dmarc.vasadomena.sk` s hodnotou minimálně `v=DMARC1; p=none; sp=none`.

## Proč odeslání trvá dlouho

Pokud se vám zdá, že odesílání trvá příliš dlouho, toto jsou možné důvody:
- Odesílání pracuje jako úloha na pozadí, inicializuje se při startu serveru. E-mail odesílá pravidelně každých 1000ms (nastavuje se v konf. proměnné `dmailWaitTimeout`). Tato hodnota je čekání mezi provedeními odeslání, čili po odeslání emailu se znovu spustí odesílání za nastavený počet ms. Pokud nastavíte hodnotu 333 neznamená to, že se odešlou 3 emaily za sekundu (to by celý proces odesílání musel trvat 0ms, což jistě není realita).
- Odesílání emailů je **blokující**, pokud se během intervalu nastaveného přes konf. proměnnou `dmailWaitTimeout` nestihne email odeslat, interval se přeskočí a bude se odesílat v dalším intervalu.
- Doménové limity - pro lepší doručení se kontroluje [limit počtu a rychlosti doručení emailu na konkrétní doménu](../domain-limits/README.md). Pokud kampaň obsahuje mnoho emailů stejné domény (např. gmail.com nebo vaší firemní domény) bude docházet ke zpoždění odeslání. Pokud vaše firemní doméně nelimituje počet emailů přidejte ji k doménovým limitům a nastavte vysoký počet emailů za časový úsek.
- Výkon databáze - jak je uvedeno výše, při odesílání se z databáze vybírá náhodný vzorek emailů k odeslání, ze kterého se vybere email. Pokud databázová tabulka `emails` obsahuje mnoho záznamů může tento výběr trvat delší dobu, což zpomaluje odesílání. Můžete v aplikaci Nastavení->Mazání dat->E-maily smazat staré informace o odeslaných emailech, což sníží zatížení databáze.
- Rychlost serveru - jak je uvedeno výše, každý email se stahuje jako web stránka lokálním HTTP spojením. Pokud má server nedostatečný výkon dochází i při tomto stahování ke zdržení. Můžete sledovat v sekci Přehled aplikaci Monitorování serveru pro ověření výkonu v době odesílání kampaně. Ideální je, když se email stahuje lokálně přímo z aplikačního serveru bez průchodu celou infrastrukturou (firewall, load balancer...). Můžete využít konf. proměnnou `natUrlTranslate` přes kterou umíte nastavit překlad adres (např. `https://www.domena.sk/|http://localhost:8080/`).

Následující konfigurační proměnné ovlivňují rychlost odesílání:
- `dmailWaitTimeout` - interval spouštění odeslání emailu v milisekundách. Po změně je nutné restartovat server (výchozí 1000).
- `dmailMaxRetryCount` - maximální počet opakování odeslání emailu dojde-li k chybě při odesílání (výchozí 5).
- `dmailSleepTimeAfterException` - interval čekání v ms po chybě odesílání, např. pokud SMTP server přestane odpovídat (výchozí 20000),
- `dmailBadEmailSmtpReplyStatuses` - seznam čárkou oddělených výrazů vrácených ze SMTP serveru pro které se email nebude znovu pokoušet odeslat (výchozí: Invalid Addresses,Recipient address rejected,Bad recipient address,Local address contains control or whitespace,Domain ends with dot in string,Domain contains illegal character.
- `dmailDisableInlineImages` - umožňuje vypnout přikládání obrázků do emailu, což zvýší rychlost odeslání a sníží velikost emailu. Nevýhodou je, že příjemce musí potvrdit načtení obrázků ze serveru. Pokud máte více doménovou instalaci můžete výjimky pro přikládání obrázků do emailu nastavit přes konf. proměnnou `dmailWhitelistImageDomains` (pro nastavené domény se obrázky přiloží).

Další konf. proměnné, které lze nastavit:
- `useSMTPServer` - povoluje/úplně vypíná odesílání všech emailů ze serveru (výchozí `true`).
- `disableDMailSender` - vypne jen odesílání hromadných emailů (výchozí `false`).
- `senderRunOnNode` - pokud používáte cluster více aplikačních serverů umožňuje nastavit čárkou oddělený seznam jmen `nodu`, ze kterého budou odesílány hromadné emaily. Upozornění: jsou-li emaily odesílány z více `nodov` může dojít k duplicitnímu odeslání emailu.
- `dmailTrackopenGif` - virtuální cesta k obrázku, která indikuje otevření emailu (výchozí `/components/dmail/trackopen.gif`).
- `dmailStatParam` - název URL parametru pro statistiku kliknutí (výchozí `webjetDmsp`).
- `replaceExternalLinks` - pokud je nastaveno na `true` budou se nahrazovat i externí odkazy přesměrováním přes server kde je spuštěn hromadný email pro sledování statistiky (výchozí `false`).

## Nastavení pro zrychlení

Pokud potřebujete urychlit odesílání můžete postupovat následovně:
- Zvyšte doménové limity, doporučujeme nastavit vyšší limity na domény `gmail.com` a vaši firemní doménu.
- Upravte `dmailWaitTimeout` na hodnotu `500`, což zvýší rychlost volání odeslání emailu, z důvodu blokování (viz výše). To ale neznamená, že se email odešle každých 500ms.
- Pokud databáze obsahuje mnoho neplatných emailů snižte `dmailSleepTimeAfterException`. **Upozornění:**, pokud skutečně nastane výpadek vašeho SMTP serveru, tak se emaily velmi rychle označí jako odeslané, protože přeteče počet `dmailMaxRetryCount`.
- Nastavte `natUrlTranslate` pro přímé stahování textu emailu z lokálního aplikačního serveru. Máte-li více doménovou instalaci může nastat problém s výběrem správné domény. Doporučujeme v `hosts` souboru na serveru nastavit všechny domény na IP adresu 127.0.0.1, v takovém případě nastavíte pouze přesměrování portu z 80 na 8080 (nebo na jakém portu máte spuštěn lokální aplikační server).
- Minimalizujte obrázky a přílohy. Ty zvyšují zátěž na server a objem emailu. Případně nastavte konf. proměnnou `dmailDisableInlineImages` na `false` pro vypnutí přikládání obrázků přímo do těla emailu.
- Máte-li cluster můžete povolit odesílání z více nodů paralelně, zvyšuje se ale riziko více duplicitního odeslání emailu příjemci. Seznam nodů, ze kterých se email odesílá se nastavuje v konf. proměnné `senderRunOnNode`.
