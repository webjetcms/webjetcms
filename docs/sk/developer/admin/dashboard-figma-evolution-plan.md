# Úvodný prehľad administrácie – Figma evolúcia

Dátum: 26. 9. 2026. Vetva: `feature/58806-new-welcome-page`.

Tento dokument zaznamenáva schválený návrh a implementačný plán nad existujúcim widgetovým systémom. Nadväzuje na [technický plán widgetov](dashboard-widgets-plan.md). Používateľ schválil implementáciu Figma evolúcie vrátane nižšie uvedených pripomienok.

## Schválený návrh

- Zachovať pôvodnú ľavú navigáciu a globálnu hornú hlavičku.
- Použiť Asap, existujúce Tabler ikony a farby WebJET. Potlačiť orámovanie, oddeliť obsah priestorom a selektívnym jemným podfarbením. Návštevnosť má mätový podklad, formuláre a newsletter levanduľový, najbližšie publikovanie jemne oranžový. Bežné zoznamy zostávajú neutrálne.
- Pod uvítaním zobraziť skutočné novinky aktuálnej verzie. Po zatvorení zostane jeden riadok so skráteným textom a tlačidlom „Viac info“.
- Vpravo vedľa uvítania vždy zobrazovať aktívne prihlásenia vrátane detailov relácie. Tento bezpečnostný blok sa nedá odstrániť ani minimalizovať.
- „Upraviť prehľad“ presunúť k nadpisu widgetového prehľadu. Presúvanie, menu nastavení a „Pridať widget“ zobrazovať až v režime úprav. Zachovať ovládanie presunu bez myši.
- V „Pokračujte v práci“ zobraziť perex obrázok, ak ho stránka má; inak ikonu stránky.
- Systémové upozornenia zoskupiť do accordionu. Každá nevyriešená položka má stále viditeľný vlastný nadpis a samostatne rozbaliteľný popis s pôvodnou akciou.
- „Vaše skratky“ majú vlastné tlačidlo pridania. Povoliť oprávnenú položku administratívneho menu alebo vlastnú bezpečnú URL.

Prenosný schválený prototyp je súčasťou výstupného balíka ako `prototype.html`; dizajnové poznámky a zdroje sú v `design-notes.md`. Prototyp používa ilustračné dáta. Implementácia musí zobrazovať skutočné dáta a pravdivé prázdne/chybové stavy.

## Načítanie dát

| Obsah | Zdroj a okamih načítania |
| --- | --- |
| Používateľ, aktuálna doména, povolené menu | Ľahký bootstrap cez DashboardListener |
| Rozloženie a nastavenia | Existujúci `/admin/rest/dashboard/settings` |
| Aktívne relácie | Existujúci `/admin/rest/dashboard/data/sessions`, nezávisle od grafov |
| Novinky | Existujúci zdroj noviniek verzie a potvrdenie prečítania |
| Bezpečnostné a prevádzkové upozornenia | Nový `/admin/rest/dashboard/notices` vracajúci zoznam položiek |
| Návštevnosť, formuláre, publikovanie, schvaľovanie, newsletter a ďalšie dáta | Existujúce async dashboard REST projekcie nad službami príslušných modulov; nepočítať v DashboardListener |
| Posledné stránky + perex obrázok | Existujúci `/admin/rest/dashboard/recent-pages`, rozšírený o URL obrázka |
| Pôvodné doplnkové prehľady | Nový `/admin/rest/dashboard/legacy-data`, až po rozbalení „Ďalšie prehľady“ |

Zoznam upozornení používa stabilné `id`, `severity`, `icon`, `title`, serverom vytvorené `bodyHtml` a voliteľnú akciu. Zachovať všetky existujúce prípady: 2FA, databázová konverzia, konverzia štatistík prehliadačov, aktualizácia WebJET, Java, licencia a Amazon SES. Oprávnenia zostávajú kontrolované na serveri. Chyba načítania nesmie vyzerať ako stav bez upozornení.

Všetky dashboard požiadavky používajú existujúcu ochranu CSRF, oprávnenia a aktuálnu doménu. Zrušiť požiadavky a zlikvidovať grafy po odstránení či prekreslení widgetu. Súkromné dáta sa neukladajú do verejnej cache.

## Technický postup

1. Upraviť ľahký bootstrap, vytvoriť async zoznam upozornení a odložené načítanie pôvodných prehľadov. Rozšíriť projekciu posledných stránok o perex obrázok.
2. Oddeliť pevné oblasti uvítania, relácií, hľadania a skratiek od personalizovaného obsahu. Zachovať uložené identifikátory, nastavenia, poradie a filtre existujúcich widgetov.
3. Pridať explicitný režim úprav; uloženie, spätné vrátenie odstránenia a reset ponechať na existujúcom serverovom mechanizme.
4. Upraviť výstup dátových widgetov a skratky. Menu skratka musí rešpektovať oprávnenia; vlastná URL povoľuje HTTP(S) alebo absolútnu lokálnu cestu, zakazuje vykonateľné schémy a protocol-relative adresy. Validácia prebehne aj na serveri.
5. Doplniť modulové SCSS podľa `AGENTS-design.md`. Farby definovať SASS premennými a exportovať na `webjet-overview-dashboard` ako CSS custom properties. V pravidlách používať `var()`, vrátane farieb grafov.
6. Grafy vytvárať cez existujúci ChartTools/AmCharts helper. Zachovať textový/tabuľkový ekvivalent, predchádzajúce obdobie, životný cyklus a hlásenie chýb.
7. Doplniť slovenské, české a anglické preklady. Overiť klávesnicu, focus, zalamovanie, kontrast a responzívne rozloženie.

### Farebné premenné

Zdrojom je `src/main/webapp/admin/v9/src/scss/5-modules/_md-dashboard.scss`. SASS hodnoty vychádzajú z palety administrácie a exportujú sa na `webjet-overview-dashboard` a dashboard modal. Štýly aj grafy čítajú CSS premenné.

| Účel | Runtime premenná |
| --- | --- |
| Neutrálna plocha | `--wj-dashboard-surface` |
| Návštevnosť | `--wj-dashboard-mint` |
| Formuláre a newsletter | `--wj-dashboard-lavender` |
| Schvaľovanie a upozornenia | `--wj-dashboard-amber` |
| Najbližšie publikovanie | `--wj-dashboard-publishing` |
| Čiary a stĺpce grafov | `--wj-dashboard-chart-primary`, `--wj-dashboard-chart-comparison` |
| Mriežka a popisy grafov | `--wj-dashboard-chart-grid`, `--wj-dashboard-chart-label` |

## Overenie a odovzdanie

- Zostaviť administráciu pomocou `npm run dev`; generované súbory neupravovať ručne.
- Spustiť relevantné Java testy a existujúce JavaScript helper testy.
- Aktualizovať a spustiť relevantné CodeceptJS scenáre proti lokálnemu serveru; zachovať pôvodné nastavenia testovacieho účtu.
- V prehliadači overiť reálne dáta, accordion, zbalenie/rozbalenie noviniek, režim úprav, skratky, desktop a úzke rozloženie.
- Priložiť screenshot implementácie a doplniť výsledky kontrol do tohto dokumentu. Zmeny ponechať bez commitu.

## Výsledky

Implementované sú všetky vyššie uvedené oblasti. Aktívne relácie zobrazujú celý zoznam; pri veľkom počte má zoznam vlastné rolovanie. Novinky zobrazujú celý obsah prekladového kľúča `admin.overview.changelog` s Markdown formátovaním a odkazom na kompletný zoznam zmien. Uložené staršie rozloženia sa zachovávajú; nový predvolený variant sa uplatní pre nový alebo resetovaný profil.

Pri kontrole sa opravili aj dve vedľajšie chyby: lokalizovaný dátum `02.03.2026` sa už neinterpretuje ako americký dátum a 38px perex náhľady používajú thumbnail službu s výstupom 76 × 76 px. ChartTools dostáva runtime farby vrátane normalizácie desatinných RGB kanálov zo SASS.

Výsledky finálneho overenia:

- Java dashboard testy: **54 úspešných, 0 chýb**.
- JavaScript dashboard helper testy: **58 úspešných, 0 chýb**.
- CodeceptJS: **22 overených scenárov** — widgety 8, katalóg 6, dátové projekcie 4, nový dizajn 4. Zahŕňa úspešné cielené opakovania po oprave synchronizácie presunu, rolovania a Bootstrap animácie modalu v testoch.
- Nové dizajnové scenáre overujú stále dostupné relácie, CSRF pri načítaní upozornení, nezávislé rozbalenie upozornení, režim úprav, klávesnicový presun a návrat fokusu, zbalenie noviniek po obnovení stránky a šírky 390, 768 a 1337 px.
- Katalóg overuje skutočné oprávnené dáta, textový ekvivalent grafov, kontrolu prístupnosti a uvoľnenie AmCharts pri obnovení, zbalení, zmene veľkosti a odstránení widgetu.
- Pôvodné nastavenia testovacieho účtu sú obnovené. Nové dizajnové scenáre zachytávajú zápisy nastavení do testovacej fixture; kontrolujú, že skutočné preferencie zostali nezmenené.
- Development aj production zostava administrácie prešli. Production build hlási existujúce upozornenia na veľkosť spoločných bundle súborov.
- Kontrast sekundárneho textu na nových podkladoch je 4,51:1 až 4,68:1; hlavný text presahuje 16:1.
- Nezávislá kontrola zachovania profilov, upozornení, bezpečných URL a životného cyklu grafov prešla po zapracovaní opráv.

Samostatné E2E súbory pre reset profilu a expiráciu licencie majú aktualizované selektory, ale v tomto overení sa nespúšťali. Vyžadujú reset účtu alebo zásah do konfigurácie licencie. Relevantné správanie nastavení a upozornení pokrývajú vyššie uvedené Java a E2E kontroly.

Pri klávesnicovej kontrole sa opravilo zachovanie fokusu po zbalení noviniek. Globálna hlavička a tmavé menu zostali bez zmeny. Žiadny commit nebol vytvorený.

## Výstup pre tiket

Prenosný balík `webjet-dashboard-58806.zip` obsahuje tento plán, pôvodný technický plán widgetov, schválený interaktívny prototyp, dizajnové poznámky a snímky implementácie pre desktop a mobil. Snímky implementácie vznikli pred vložením testovacích fixture a zobrazujú skutočné dáta lokálnej administrácie testovacieho účtu. Prázdne formuláre alebo publikovanie preto zostávajú pravdivo prázdne.

Prototyp umožňuje porovnať pôvodné tri vizuálne smery; implementovaný je variant Figma evolúcia. Pri staršej otvorenej karte môže byť potrebné obnovenie bez cache, pretože existujúci serverový filter cacheuje administratívne CSS.


## Vizuálne doladenie podľa schváleného prototypu

Po priamom porovnaní prototypu a implementácie sa upravilo:

- Uvítanie ako jedna plocha s levanduľovo-mätovým gradientom, vnútorným odsadením 24 px a užším panelom prihlásení širokým 300 px. Dátum a novinky majú pokojnejšiu typografiu; odkaz na správu prihlásení je v hlavičke panelu.
- Výraznejšia hlavná hodnota návštevnosti (56 px), menšie metriky (32 px), kompaktné názvy a čitateľné popisy. Formuláre a newsletter majú levanduľový tón, schvaľovanie žltý, chyby jemný ružový a publikovanie oranžový.
- Posledné stránky používajú priehľadné riadky s jemným delením a samostatnými náhľadmi namiesto vnorených bielych kariet.
- Vyhľadávanie je jedna kompaktná plocha s natívnymi rádiami vizuálne spracovanými ako segmenty. Skratky sú menšie; upozornenia majú označenie skupiny a počet.
- Graf má jemnejšie čiary a stručnú legendu. Publikovanie zobrazuje deň/mesiac a čas. Newsletter odlišuje stav, názov, počty a priebeh odosielania.
- Nové odtiene majú SASS definície a runtime CSS premenné vrátane gradientu, odkazov a tlmeného textu. Odkazy na pastelových podkladoch používajú tmavší existujúci modrý odtieň; upravené sú aj hover/focus stavy.

Zachované sú skutočné dáta, uložené poradie a veľkosti kariet, globálna hlavička a tmavé menu. Počet položiek a prázdne stavy sa riadia reálnymi dátami, preto sa obsah líši od ilustračného prototypu.

Overenie tejto úpravy: **58 JavaScript helper testov, 4 dizajnové E2E scenáre a 6 scenárov katalógu**. Prešli kontrola prístupnosti, responzívne šírky 390/768/1337 px, životný cyklus AmCharts, vyhľadávanie, klávesnicové ovládanie a návrat pôvodných preferencií. Development a production zostava prešli. Java služby sa pri tomto doladení nemenili.

## Pripomienky k novinkám a aktívnym prihláseniam

- Rozbalené novinky zachovávajú celý preklad `admin.overview.changelog` vrátane Markdown formátovania. Zápis `\n` v preklade sa pred spracovaním prevedie na nový riadok. Obsah využíva celú dostupnú šírku vedľa prihlásení; skrátený text zostáva iba v zbalenom náhľade.
- Tlačidlo „Zbaliť novinky“ je pri odkaze „Kompletný zoznam zmien“. Po zbalení zostáva „Viac info“ a zachová sa klávesnicový fokus.
- Prihlásenia používajú Tabler ikony Chrome, Safari, Firefox a Edge; neznámy prehliadač má všeobecnú ikonu zariadenia.
- Aktuálna relácia má zelenú bodku s tooltipom „Toto prihlásenie“. Ostatné majú napravo ikonu odhlásenia s tooltipom „Odhlásiť túto reláciu“. Riadok tvorí názov prehliadača a dátum/IP bez tretieho riadku akcie.
- Dlhý zoznam má vlastné natívne rolovanie. Jeho udalosti kolieska, dotyku a navigačných klávesov sa pri pretekaní nezachytávajú hlavným scrollbarom administrácie. Listenery a tooltipy sa uvoľnia so zánikom widgetu alebo dialógu.
- Tooltipy fungujú pri hoveri aj klávesnicovom fokuse. Escape skryje viditeľný tooltip; ďalší Escape môže zatvoriť dialóg správy relácií.

Overenie týchto pripomienok: **61 JavaScript helper testov, 5 dizajnových E2E scenárov a 6 scenárov katalógu**. Test s deviatimi reláciami overil koliesko, PageDown, tooltipy aj dvojité Escape v dialógu. Prešli kontrola kompletného HTML noviniek, responzívne šírky 390/768/1337 px a obnovenie pôvodných preferencií. Development aj production zostava prešli; production hlási existujúce upozornenia na veľkosť spoločných bundle súborov. Java služby ani prekladový obsah sa nemenili.

## Doladenie návštevnosti a navigácie kariet

- Návštevnosť používa výraznú hlavnú hodnotu, popis metriky so zvoleným počtom dní a šípku pri porovnaní s predchádzajúcim obdobím. Popisy podporujú návštevy, zobrazenia aj unikátnych návštevníkov a intervaly 7/30/90 dní.
- Graf má iba jemnú vodorovnú mriežku, podfarbenú aktuálnu sériu, tlmenú prerušovanú porovnávaciu sériu a bodku na poslednej aktuálnej hodnote. Os a legenda zobrazujú stručné číselné dátumy; legenda rozlišuje skutočné dátumy oboch intervalov. Pri staršom alebo prelomovom roku rozsah zachová aj rok.
- Viditeľná sekcia „Údaje grafu“ je z návštevnosti odstránená. Presné dátumy a hodnoty zostávajú v textovej tabuľke dostupnej čítačkám obrazovky a v tooltipovom detaile grafu.
- Štatistiku otvorí kliknutie na nadpis „Návštevnosť ↗“ alebo hlavnú hodnotu. Spodný odkaz je odstránený. V karte „Pokračujte v práci“ je odkaz „Všetky ↗“ vpravo hore, aj pri zbalenej karte.
- Registrácia widgetu podporuje spoločné `headerLink` nastavenie: odkaz v nadpise alebo samostatnú akciu v hlavičke. Odkaz zostáva dostupný počas načítania, po zbalení aj po aktualizácii názvu; URL používa spoločnú validáciu.
- Uložené veľkosti a poradie kariet aj reálne dáta zostávajú zachované. Na úzkej obrazovke sa obdobie presunie pod metriku, aby neprekrývalo nadpis. Nový odtieň porovnávacej čiary má SASS definíciu a runtime CSS premennú.

Overenie tejto úpravy: **65 JavaScript helper testov, 6 scenárov katalógu a 5 dizajnových E2E scenárov**, všetky úspešné. Zahŕňa navigáciu v hlavičkách, parametre prekladov, dátové ekvivalenty, kontrolu prístupnosti, životný cyklus AmCharts a zachovanie pôvodných preferencií. Development zostava prešla. Vizuálne boli overené reálne karty na desktope a pri šírke 390 px; automatické responzívne kontroly pokrývajú aj 768/1337 px. Java služby sa nemenili.

Aktuálna snímka oboch kariet: `build/test/dashboard-implementation-traffic.png`. Prenosná kópia poznámok a snímky je v `/private/tmp/webjet-dashboard-58806/traffic-polish/`.
