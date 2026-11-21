# Rozhranie pracovných verzií a publikovania

**Stav: pôvodný návrh rozhrania.** Aktuálne redakčné postupy opisuje [používateľská dokumentácia](../../sk/redactor/webpages/publishing-packages.md). Správa pracovných verzií používa štandardnú DataTable stránku v ľavom menu; prepínač v hlavičke obsahuje len výber a odkaz **Spravovať**. Potvrdený základ je živá overlay jednej domény, postupný merge, redakčné súbory a náhľad dostupný prihláseným používateľom administrácie s príslušnými oprávneniami. Existujúce aj nové stránky, priečinky a médiá zachovajú ID. Vstavané stránky, navigácia, menu, novinky a médiá podporujú branch; zákaznícke priame SQL/JPA čítanie potrebuje adaptér a vlastné aplikačné dáta sa neverzionujú. Rozhranie používa označenie **Pracovná verzia**; technický parameter zostáva `branchId`.

Zvolená jednoduchá politika rieši konflikt celého objektu voľbou produkcie alebo pracovnej verzie. Samostatné polia sa používajú na vysvetlenie rozdielov, automaticky sa nespájajú. Branch sa po publikovaní uzavrie, individuálne plány sa nesmú s balíkom potichu prekryť a zrkadlenie sa nevykoná implicitne. Zdieľané systémové stránky sú iba na čítanie. Redakčné súbory používajú existujúce externé priečinky domén a adresárové oprávnenia podľa Q20. Databázové predpoklady vrátane InnoDB rieši dátový návrh.

## Rýchly postup: od vytvorenia po prepnutie

`Package/balík` a `branch/vetva` označujú ten istý pripravovaný obsah; používateľ nevytvára dva samostatné objekty. V UI používame názov **Pracovná verzia**, napríklad „Vianočná kampaň“.

1. V administrácii vyberiem doménu a v ľavom menu otvorím **Pracovné verzie**. Rovnakú tabuľku otvorí odkaz **Spravovať** v hornom prepínači. Kliknem na štandardné tlačidlo **Pridať**.
2. V štandardnom editore vyplním názov, farbu a prípadne poznámku. **Pridať** založí prázdnu rozpracovanú verziu. Názov v tabuľke otvorí editor jej údajov.
3. Označím riadok a kliknem na tlačidlo tabuľky **Upravovať web**. Tým sa verzia vyberie v administrácii a otvorí sa bežný strom stránok. V hlavičke vidím jej názov.
4. Upravujem existujúcimi editormi. Uloženie automaticky pridá zmenu do balíka; stránky a súbory netreba osobitne označovať tlačidlom „pridať do balíka“.
5. Cez **Náhľad** preklikám pracovný web. Cez prepínač **Pracovná verzia** sa vrátim na **Produkcia** alebo vyberiem inú verziu.

Vytvorenie, výber na editovanie, otvorenie náhľadu a publikovanie sú samostatné akcie. Prepnutie verzie nič nepublikuje a zachová už uložené pracovné zmeny. Tlačidlo tabuľky **Skontrolovať zmeny** otvorí porovnanie obsahu označenej verzie. Nižšie sú pôvodné návrhy rozloženia.

## 1. Prepínač produkcie a pracovných verzií

Redaktor pracuje v existujúcom editore, strome stránok a správcovi súborov. Prepínač mení zdroj obsahu pre podporované obrazovky. Detaily ukladania, názvy tabuliek ani staging cesty sa v bežnom rozhraní nezobrazujú.

```text
┌ WebJET CMS ──────────────────────────────────────────────────────────┐
│ Webové stránky          Pracovná verzia: Vianočná kampaň  [Náhľad]   │
├───────────────────┬──────────────────────────────────────────────────┤
│ Webové stránky    │ Stránky a priečinky                              │
│ Pracovné verzie   │ ... existujúci strom a DataTable ...             │
│ Súbory            │                                                  │
│ ...               │                                                  │
│                   │                                                  │
│ Doména            │                                                  │
│ [ www.firma.sk ▾ ] │                                                  │
│ Pracovná verzia    │                                                  │
│ [ Vianočná ... ▾ ] │                                                  │
└───────────────────┴──────────────────────────────────────────────────┘
```

Prepínač je v spoločnom bočnom paneli **hneď pod výberom domény**, dostupný aj pri prechode zo stránok do súborov či médií. Ponúka `Produkcia`, neuzavreté pracovné verzie vybranej domény, `Nová pracovná verzia` a `Spravovať pracovné verzie`. Zobrazuje iba verzie, ku ktorým má používateľ prístup. Aktívny kontext je vždy napísaný aj v hlavičke; samotná farba nestačí. V produkcii je označenie `Produkcia`, v branch názov pracovnej verzie.

```text
Doména
[ www.firma.sk                       ▾ ]

Pracovná verzia
[ Vianočná kampaň                    ▾ ]
┌──────────────────────────────────────┐
│   Produkcia                          │
│ ✓ Vianočná kampaň · Rozpracovaná       │
│   Nový cenník · Rozpracovaná          │
│   Jesenná ponuka · Naplánovaná        │
│ ──────────────────────────────────── │
│ + Nová pracovná verzia               │
│   Spravovať pracovné verzie          │
└──────────────────────────────────────┘
```

Pri prvom prihlásení je vybraná produkcia. Výber platí pre administráciu daného používateľa/session; neprepína ostatných redaktorov. Viacerí oprávnení redaktori môžu vybrať tú istú pracovnú verziu a pripravovať spoločný balík. Vypnutá funkcia alebo chýbajúce funkčné oprávnenie skryje nové ovládanie; obsahové práva sa naďalej overujú pri jednotlivých operáciách.

Na menšej obrazovke zostáva názov kontextu v hlavičke dostupný aj pri zatvorenom bočnom menu. Dlhý názov sa skráti vizuálne, plný názov musí zostať dostupný používateľovi klávesnice a čítačke obrazovky.

### Prepnutie

1. Pri neuložených zmenách existujúci editor ponúkne zostať alebo zahodiť neuložené zmeny a prepnúť. Prepnutie nesmie samo uložiť rozpracovaný obsah do iného kontextu.
2. Po prepnutí sa obrazovka načíta znovu a staré výbery sa overia v novom kontexte. Stránka zmazaná v branch sa neotvorí tichým fallbackom do produkčného editora.
3. Pri zmene domény sa zvolí produkcia novej domény. Pracovná verzia patriaca predchádzajúcej doméne sa neprenesie.
4. Nepodporovaná obrazovka pri aktívnej branch zobrazí `Táto funkcia sa v pracovnej verzii neupravuje` a odkaz na vedomé prepnutie do produkcie. Podľa typu obrazovky môže poskytnúť čítanie; zápis server odmietne.

Výber inej verzie používa tú istú akciu ako `Upravovať web` v jej detaile. Pri zmene cez prepínač zostane používateľ v rovnakej podporovanej sekcii; strom a zoznam sa načítajú pre zvolenú verziu. Ak pôvodne otvorený objekt v novom kontexte neexistuje, otvorí sa zoznam s vysvetlením. Pri chybe prepnutia zostane označený pôvodný kontext, nie vizuálne premenovaný editor so starými dátami.

Pri neuloženom formulári je voľba jednoznačná:

```text
Máte neuložené zmeny vo verzii „Vianočná kampaň“.
Pred prepnutím ich môžete uložiť v otvorenom editore.

[Zostať v editore]  [Zahodiť neuložené zmeny a prepnúť]
```

Uloženie pred prepnutím prebehne bežným tlačidlom editora v pôvodnom kontexte. Tým sa vyhneme nejasnému „uložiť a prepnúť“, ktoré by mohlo vyzerať ako prenos zmien do cieľovej verzie.

| Stav vybranej verzie | Správanie UI |
| --- | --- |
| Rozpracovaná (`DRAFT`) | Bežná editácia podľa práv. |
| Naplánovaná (`SCHEDULED`) | Obsah iba na čítanie a náhľad. Úpravy sa povolia až po výslovnom `Zrušiť plánovanie` v detaile. |
| Vyžaduje zásah (`NEEDS_ATTENTION`) | Otvorí sa detail problému; `Obnoviť prípravu` použije existujúce `reopen` a až potom umožní editáciu. |
| Publikuje sa / Publikovanie zlyhalo | Otvorí sa priebeh alebo obnova; výber neodomkne obsah na editovanie. |
| Publikovaná / Zrušená | Nie sú v ponuke aktívnych verzií; dostupné zostávajú v prehľade ako záznam výsledku. |

Ak sa už vybraná verzia medzičasom uzavrie, obrazovka ukáže stav a akciu `Prejsť do produkcie`. Nevymení potichu cieľ otvoreného formulára za produkciu. Server starý zápis odmietne; nový kontext sa použije až po vedomom prepnutí a obnovení obrazovky.

### Záložky a request kontext

Každá otvorená obrazovka nesie kontext platný pri jej načítaní: doménu a `branchId`, vrátane explicitného označenia produkcie. Session uchováva jeden aktuálne vybraný administrátorský kontext. Pri zápise sa explicitný kontext requestu musí zhodovať s aktuálnym výberom v session; server zároveň overí doménu, prístup, stav branch a cieľový záznam.

Ak používateľ prepne branch v inej záložke, zápis zo staršieho kontextu server odmietne chybou `Kontext pracovnej verzie sa zmenil. Obnovte stránku.` Obsah formulára ostane dostupný na skopírovanie; používateľ môže vedome vrátiť výber na jeho pôvodnú branch alebo obnoviť obrazovku. Chýbajúce alebo neplatné `branchId` pri pracovnom zápise nesmie znamenať uloženie do produkcie.

Týmto sa nezavádza viac nezávislých administrátorských kontextov v jednej session. Session nesmie potichu preurčiť cieľ requestu už otvoreného formulára. Rovnaké pravidlo platí pre vnorené médiá, upload, strom priečinkov a pomocné dialógy. Preview je výnimkou z výberu administrácie: jeho samostatná URL nesie konkrétnu branch a používa session iba na prihlásenie a overenie prístupu.

## 2. Vytvorenie pracovnej verzie a zoznam balíkov

Nová položka administrácie `Pracovné verzie` otvorí bežný DataTable s tlačidlom **Nová pracovná verzia**. Je dostupný aj cez `Spravovať pracovné verzie` v prepínači. Tvorba a výber používajú rovnaké existujúce funkčné/obsahové práva návrhu; vytvorenie neudeľuje právo priamo publikovať.

| Stĺpec | Príklad | Správanie |
| --- | --- | --- |
| Názov | Vianočná kampaň | Otvorí detail verzie. |
| Stav | Rozpracovaná / Naplánovaná / Vyžaduje zásah / Publikuje sa / Publikovanie zlyhalo / Publikovaná / Zrušená | Text a zodpovedajúca ikona. Zodpovedá stavom `DRAFT`, `SCHEDULED`, `NEEDS_ATTENTION`, `MERGING`, `MERGE_FAILED`, `MERGED`, `CANCELLED` z merge návrhu. |
| Zmeny | 24 | Počet skutočne zmenených položiek. |
| Pripravenosť | 2 konflikty | Súhrn poslednej kontroly, vrátane času kontroly. |
| Publikovanie | 1. 12. 2026 09:00, Europe/Bratislava | Používateľ vidí aj časové pásmo. |
| Vytvoril / Upravil | Meno, dátum | Bežné auditné údaje. |

Oba vstupy `Nová pracovná verzia` otvoria rovnaký malý dialóg:

```text
Nová pracovná verzia

Doména: www.firma.sk
Základ: aktuálna produkcia

Názov *   [ Vianočná kampaň                         ]
Poznámka  [ Príprava decembrovej ponuky             ]

Nezmenené stránky a súbory preberá pracovná verzia
z aktuálnej produkcie. Vaše úpravy sa zverejnia až
publikovaním.

[Zrušiť]                                  [Vytvoriť]
```

Názov je povinný, po odstránení okrajových medzier nesmie byť prázdny a má limit dátového modelu 255 znakov. Poznámka je nepovinná. Doména a základ sú informácie iba na čítanie; doména pochádza z aktuálneho výberu administrácie. Rovnaký názov nie je technická chyba; pri zhode v ponuke sa doplní dátum vytvorenia, prípadne ID na rozlíšenie.

Formulár odošle aj očakávanú doménu z času otvorenia. Ak používateľ medzičasom prepne doménu v inej záložke, server vytvorenie odmietne cez `409` a dialóg zachová vyplnené údaje. Zobrazená doména sa nesmie potichu zmeniť pri odoslaní; klientský údaj slúži iba na kontrolu zhody, vlastníka stále určuje server.

Vytvorenie založí prázdnu `DRAFT` verziu s nulou zmien a otvorí jej detail s akciami `Upravovať web` a `Otvoriť náhľad`. **Samotné vytvorenie nemení aktuálny výber administrácie.** `Upravovať web` výslovne aktivuje novú verziu, obnoví hlavičku/prepínač a otvorí bežný strom stránok. Koordinátor tak môže verziu iba založiť a redaktori si ju vyberú neskôr. Kliknutie na názov v zozname otvorí detail, neprepne editor.

Vzniká pracovná vrstva, nie kópia všetkých stránok a súborov; prvý náhľad preto ukazuje aktuálnu produkciu. Aj keď vytvorím novú verziu pri vybranej inej pracovnej verzii, jej základom je produkcia. Rozpracované zmeny predchádzajúcej verzie sa do nej nekopírujú. Termín sa nastaví až cez `Naplánovať` v detaile. Práva na tieto akcie určujú existujúce pravidlá publikovania.

Odchod do vytvárania/detailu aj aktivácia cez `Upravovať web` rešpektujú rovnakú ochranu neuložených zmien ako prepínač. Otvorenie `Náhľad` naopak používa samostatnú záložku a nemení admin výber. Technický kontrakt vytvorenia a výberu je v [03 — Riadiace API](03-nahlad-a-java.md#10-navrhované-riadiace-api).

## 3. Detail a prehľad pripravenosti

```text
Vianočná kampaň                          Rozpracovaná
www.firma.sk · Vytvorila Jana Nováková

[Upravovať web] [Otvoriť náhľad] [Skontrolovať zmeny]

24 zmien        21 pripravených        2 konflikty        1 prekážka
Kontrola: dnes 14:32. Produkcia sa mohla odvtedy zmeniť.

[Všetko] [Stránky] [Priečinky] [Médiá] [Súbory]     [Len problémy □]

Typ        Položka                  Zmena       Pripravenosť       Akcia
Stránka    Darčekové poukazy         Upravená    Pripravená         Porovnať
Priečinok  /produkty/vianoce         Nový        Pripravený         Zobraziť
Stránka    Kontakty                 Upravená    Konflikt: text     Vyriešiť
Súbor      /files/cennik.pdf         Nahradený   Konflikt           Vyriešiť
Médium     Cenník / Darčeky          Zmazané     Pripravené         Porovnať
Stránka    Nová ponuka              Upravená    Chýba oprávnenie   Podrobnosti

Publikovanie: termín nie je nastavený
[Publikovať teraz] [Naplánovať]
```

Tabuľka zobrazuje skutočné zmeny branch. Nezahŕňa tisíce nezmenených zdedených položiek. Každý problém má konkrétny dôvod a akciu; všeobecné `Nepripravené` bez vysvetlenia nestačí.

Typové filtre vo wireframe sú dnešný rozsah. Pre POC stačí pevný zoznam/enum štyroch typov podľa [08](08-rozsirovanie-modulov.md), bez registra handlerov. Pri zapojení Bannera alebo Galérie sa zoznam rozšíri v tej istej obrazovke. Typ poskytne názov, odkaz do svojho editora a popisy podporovaných polí pre spoločný diff. Neimplementuje sa všeobecný editor ľubovoľného JSON. Nové modulové typy sa v prvej verzii ešte neponúkajú.

Zmena znamená `Nová`, `Upravená`, `Zmazaná`; pri súbore môže používateľ vidieť `Nahradený`. Presun ukáže pôvodné a cieľové umiestnenie. U záznamov sa v detaile zobrazí stabilné ID, aby používateľ vedel skontrolovať väzby. Technické ID netreba opakovať vo všetkých hlavných stĺpcoch.

Pripravenosť rozlišuje konflikt, neplatné údaje, chýbajúci súbor, nedostatočné oprávnenie, chýbajúcu závislosť a ďalšie konkrétne kontroly definované merge službou. Nevzniká nový schvaľovací workflow. Publikovať môže používateľ s existujúcim právom priameho publikovania všetkých dotknutých stránok a s právom meniť všetky dotknuté priečinky a súbory. Ostatní pripravujú obsah v rozsahu svojich existujúcich práv. Pri nedostatočnom práve detail uvedie konkrétne dotknuté položky; zelený výsledok konfliktov nenahrádza oprávnenie publikovať.

Tlačidlo `Skontrolovať zmeny` načíta nový prehľad. Publikovanie vždy vykoná vlastnú aktuálnu kontrolu na serveri; starý zelený prehľad neudeľuje výnimku. Ak produkcia medzičasom spôsobí konflikt, používateľ sa vráti k jeho riešeniu.

## 4. Porovnanie stránky alebo priečinka

Detail bežnej zmeny ukáže názov, ID, cestu a zmenené polia. Používateľ môže prepnúť medzi prehľadom zmien a náhľadmi produkcie/pracovnej verzie. Na stránke sa zachovajú obvyklé názvy polí, napríklad `Názov`, `URL adresa`, `Text stránky`, `Šablóna`, `Zobrazenie v menu`.

Porovnanie zvýrazní zmenené polia, ale rozhodnutie sa týka celého podporovaného obsahu objektu. Aj keď produkcia zmení názov a branch text tej istej stránky, používateľ musí zvoliť výslednú verziu stránky. V rozpracovanej branch pred začatím merge môže otvoriť normálny branch editor, doplniť požadovanú hodnotu a znova vykonať porovnanie. Samostatný editor merge výsledku ani automatické spájanie polí a HTML sa nezavádza. Po čiastočnom publikovaní platia obmedzenia opísané v časti 8.

Pre nový záznam sa zobrazia jeho hodnoty a cieľové umiestnenie. Pre zmazanie názov, ID, produkčný stav a konkrétne závislosti, ktoré bránia publikovaniu. Zrušenie pracovnej zmeny sa volá `Zahodiť túto zmenu` a vysvetlí, že branch začne opäť preberať produkčnú položku.

Zahodenie zmeny je dostupné iba v `DRAFT`. Ak na nej závisia iné položky (napríklad stránky nového priečinka alebo odvodené URL pri jeho premenovaní), server ju odmietne s ich zoznamom. Používateľ ich musí vedome upraviť alebo zahodiť; nástroj ich potichu neodstráni. `Zahodiť pracovnú verziu` uzavrie celý dosiaľ nepublikovaný balík ako `CANCELLED`, po potvrdení názvu a počtu zahadzovaných zmien; v `MERGING` a `MERGE_FAILED` sa neponúka. Obsah sa nezmaže z produkcie ani sa nevykoná rollback. Archivovaný detail ostane dostupný podľa práv, fyzické čistenie úložiska sa riadi retenčným postupom.

## 5. Riešenie konfliktu

```text
Konflikt: Kontakty · stránka ID 142

Pôvodný stav              Aktuálna produkcia          Pracovná verzia
Pri začatí úpravy          Zmenil Peter, dnes 10:14    Zmenila Jana, dnes 11:05
───────────────────       ─────────────────────       ─────────────────────
Naša adresa je ...         Naša nová adresa je ...     Nájdete nás na ...
[Zobraziť celý text]       [Náhľad produkcie]          [Náhľad verzie]

Výsledná verzia celej stránky:
( ) Použiť produkciu
( ) Použiť pracovnú verziu

[Otvoriť pracovnú verziu v editore]

[Späť]                                      [Uložiť riešenie]
```

Pôvodný stav znamená hodnotu zachytenú pri začatí pracovnej úpravy, nie automaticky dátum vytvorenia branch. Všetky tri panely majú zrozumiteľný názov a časový kontext. Na úzkej obrazovke sa zobrazia pod sebou alebo cez pomenované záložky.

Voľba sa aplikuje na celý objekt. Panel môže rozbaliť jednotlivé polia a zvýrazniť rozdiel HTML, ale neponúka voľby po poliach. `Otvoriť pracovnú verziu v editore` je dostupné v rozpracovanej branch pred začatím merge a používa normálny editor stránky, priečinka alebo média. Uloženie je bežná úprava branch, ktorá zneplatní predošlé riešenie konfliktu; po návrate používateľ znova porovná aktuálne verzie a potvrdí výsledok. Po čiastočnom merge sa táto editačná akcia neponúka.

`Použiť produkciu` ponechá aktuálny podporovaný obsah celého produkčného objektu. `Použiť pracovnú verziu` ho nahradí pracovným obsahom; prevádzkové počítadlá sa riadia dátovým návrhom a neprepisujú sa starou kópiou. Tlačidlá sa nevolajú neurčito `Prepísať` a `Použiť moju`, pretože branch môže upravovať viac ľudí.

Uloženie riešenia zaznamená výber verzie, ešte nič nepublikuje. Rozhodnutie sa viaže na porovnávaný produkčný a pracovný obsah. Ak sa niektorý vstup znova zmení, server vráti `Obsah sa od porovnania zmenil` a zobrazí nové porovnanie. Ručné úpravy už uložené cez branch editor zostávajú pracovným obsahom.

Podľa Q16 sa uložená voľba prejaví aj v celom preklikateľnom náhľade. Po `Použiť produkciu` náhľad zobrazí produkčný výsledok položky vrátane jej vplyvu na URL, menu a Novinky; pracovný editor si ponechá rozpracovanú verziu. Pri otvorení editora sa zobrazí `Pre publikovanie je zvolená produkcia. Uložením pracovnej úpravy treba výsledok znova skontrolovať.` Neponúka sa druhý prepínač dvoch odlišných náhľadov webu.

Konflikt úprava/zmazanie používa zrozumiteľné akcie `Ponechať produkčnú položku` a `Použiť pracovné zmazanie`, prípadne `Obnoviť pracovnú položku`, ak bola zmazaná produkcia. Nedá sa ním obísť neplatná väzba alebo chýbajúce oprávnenie.

### Binárny súbor

```text
Konflikt súboru /files/cennik.pdf

Produkcia                         Pracovná verzia
186 kB · dnes 10:14                212 kB · dnes 11:05
[Otvoriť] [Stiahnuť]               [Otvoriť] [Stiahnuť]

( ) Ponechať produkčný súbor
( ) Použiť súbor pracovnej verzie

[Späť]                            [Uložiť riešenie]
```

Obrázky zobrazia aj obrazové náhľady; ostatné formáty názov, veľkosť a čas. Automatické spájanie binárneho obsahu sa neponúka. Otvorenie pracovného súboru má rovnaké oprávnenia ako celý náhľad. Dostupnosť pôvodného binárneho súboru závisí od schválenej retencie pôvodných bajtov; samotný hash nie je súbor na stiahnutie.

## 6. Preklikateľný náhľad

`Otvoriť náhľad` otvorí samostatnú záložku. URL identifikuje branch nezávisle od aktuálne vybranej branch v administrácii. Zmena prepínača administrácie preto nezmení význam už otvoreného náhľadu.

```text
NÁHĽAD · Vianočná kampaň · www.firma.sk       [Produkcia] [Späť do úprav]
Novinky vyhodnotené k 1. 12. 2026 09:00 (Europe/Bratislava)
──────────────────────────────────────────────────────────────────────
... celý web s pracovnými stránkami, navigáciou, médiami a súbormi ...
```

Náhľad podľa Q19 beží na hostiteľovi administrácie, napríklad `https://cms.firma.sk/admin/webpages/branch-preview/42/ponuka/`, a vykresľuje doménu `www.firma.sk` uloženú vo vetve. Vyžaduje platné prihlásenie do tejto administrácie a prístup k danej verzii/obsahu. Skopírovaná URL nedáva prístup neprihlásenému klientovi. Po prihlásení sa používateľ vráti na bezpečne overenú pôvodnú preview URL. Názov branch aj logická doména sú viditeľné po navigácii na inú stránku; zmena admin domény otvorený náhľad neprepne.

Náhľad používa zvolené výsledky z porovnania, kým editor používa WORK. Ak konflikt ešte nemá rozhodnutie, použije sa v náhľade pracovná verzia s výrazným upozornením `Náhľad obsahuje nevyriešené konflikty; výsledok publikovania ešte nie je určený` a odkazom na porovnanie. Ak sa po uloženej voľbe zmení jej produkčný vstup, náhľad vyžiada nové porovnanie namiesto tichého použitia starej voľby alebo návratu na WORK. V naplánovanej vetve treba pred zmenou rozhodnutia výslovne zrušiť plánovanie; pri `NEEDS_ATTENTION` obnoviť prípravu. Po vyriešení všetkých konfliktov sa kontroluje rovnaký výsledok, ktorý použije merge. Zámerné pracovné zmazanie sa v náhľade zobrazuje ako neprítomnosť; archívny detail po merge nesľubuje historický snapshot celého webu.

Odkazy medzi stránkami, obrázky, thumbnaily a presmerovania zachovajú preview kontext. Tlačidlo `Produkcia` otvorí produkčnú URL v novej záložke, aby používateľ mohol výsledok porovnať. U novej stránky, ktorá ešte produkčnú verziu nemá, sa zobrazí `Zatiaľ nie je publikovaná`.

Preview neprepisuje admin session výber. Potrebuje autentifikačnú session, ale nesmie z nej odhadovať, ktorú branch chce zobraziť. Konkrétna URL brána a nároky na proxy sú v [analýze médií a súborov](analyza-media-a-subory.md).

Novinky sa zobrazia v pôvodnej šablóne aplikácie vrátane stránkovania a odkazov na detail. Náhľad použije parametre aplikácie a stránky zo zvoleného výsledku podľa Q16. Pri naplánovanej vetve banner uvedie budúci čas vyhodnotenia dátumových filtrov Noviniek; inak aktuálny čas requestu. Pri budúcom čase vysvetlí, že nejde o simuláciu všetkých budúcich zmien webu. Po posune termínu treba náhľad obnoviť. Presný kontrakt je v [03a](03a-novinky.md).

Editor novej novinky ponechá voľbu výslednej dostupnosti, perex aj výber existujúcich perex skupín. Nápoveda vysvetlí, že zaradenie do výpisu závisí od nastavení aplikácie; nevyhovujúca novinka sa nasilu nepridá. Kategórie sa zobrazia aj v porovnaní zmien. Nevytvára sa ďalší editor Noviniek ani automatický diagnostický systém všetkých filtrov. Vnorená správa Noviniek preberie admin branch kontext vrátane kontroly zastaranej záložky.

## 7. Publikovanie teraz a plánovanie

`Publikovať teraz` otvorí stručný súhrn s počtom stránok, priečinkov, médií a súborov a výsledkom aktuálnej kontroly. Vysvetlí, že položky sa zverejnia postupne. Ak zostávajú blokujúce problémy, ponúkne návrat na filtrovaný zoznam problémov.

Ak používateľ nemá práva na publikovanie celého výsledku, publikovanie a plánovanie nie sú dostupné; obsah môže ďalej pripravovať podľa svojich práv. Server rovnaké oprávnenia overí pri každej príslušnej akcii. Naplánovaný beh znovu overí oprávnenia používateľa, ktorý publikovanie autorizoval.

```text
Naplánovať publikovanie · Vianočná kampaň

Dátum [01.12.2026]       Čas [09:00]
Časové pásmo: Europe/Bratislava

Publikuje sa 24 zmien. Zverejnenie prebieha postupne.
Pred spustením sa znova overia konflikty a pripravenosť.

[Zrušiť]                                    [Naplánovať]
```

Po naplánovaní detail zobrazuje termín a akcie `Zmeniť termín` a `Zrušiť plánovanie`. Zmena termínu je jedna operácia nad branch. Nevyžaduje postupné otváranie jednotlivých stránok.

Ak sa plán už vykonáva, tieto akcie nie sú dostupné a server ich odmietne. Zmena termínu nemá predstierať zastavenie už prebiehajúceho publikovania. Pri probléme zistenom v termíne sa stav zmení na `Vyžaduje zásah`, s konkrétnym dôvodom a odkazom na dotknuté položky.

Pri kolízii s existujúcim jednotlivým plánom sa zobrazí stránka, termín a odkaz na správu pôvodného plánu. Riešenie vyžaduje jeho explicitné zrušenie alebo zmenu. UI ani scheduler nesmú starý plán potichu vymazať. Ak niekto vytvorí kolidujúci individuálny plán až po naplánovaní branch, aktuálna kontrola pred prvým zápisom branch publikovanie zastaví so stavom `NEEDS_ATTENTION`. Nezavádza sa nový centrálny zákaz vytvárania individuálnych plánov; konflikt sa rieši pred vykonaním balíka.

Kolízia môže pochádzať aj zo stránky mimo balíka, ak jej individuálny plán publikuje ten istý cieľový súbor. Detail preto ukáže aj cestu súboru a zdrojový plán, nie iba konflikty stránkových ID v balíku.

## 8. Priebeh, čiastočná chyba a pokračovanie

```text
Vianočná kampaň · Publikovanie zlyhalo

Dokončené: 17 z 24 položiek
17 publikovaných · 1 chyba · 6 čaká

Súbor /files/cennik.pdf
Cieľový priečinok nie je zapisovateľný.

[Zobraziť dokončené položky]  [Podrobnosti chyby]
[Znova overiť a pokračovať]

Už dokončené zmeny sú viditeľné na produkcii.
```

Priebeh sa zobrazuje podľa serverovej evidencie položiek. Obnovenie stránky aj opätovné otvorenie detailu obnoví rovnaký stav. Zatvorenie prehliadača publikovanie nezastaví.

Pri chybe UI jasne odlíši dokončené, neúspešné a čakajúce položky. `Znova overiť a pokračovať` najskôr overí aktuálny stav a oprávnenia; potom pokračuje podľa pravidiel merge služby. Detail nepoužíva všeobecné tvrdenie `Nič nebolo zmenené`, keď už časť obsahu vyšla.

V stave `MERGE_FAILED` zostáva obsah a štruktúra branch uzavretá: nepridávajú sa nové položky, nemení sa obsah, presuny ani zmazania. Rozhodnutie `LIVE/WORK` sa môže znova zvoliť iba pri položkách, na ktorých ešte nezačal produkčný zápis: `PENDING` alebo chyba pred dátovou fázou. Ak treba takú položku ručne skombinovať, oprávnený používateľ vedome upraví produkciu a následne pre nedotknutú položku zvolí `Použiť produkciu`; alternatívou je dokončiť obnovu a ďalšie úpravy pripraviť v novej branch.

Ak sa po čiastočnom zápise zmenil produkčný obsah už aplikovanej položky, nástroj zastaví pokračovanie na tejto položke. Zobrazí vykonanú fázu, zaznamenaný výsledok a aktuálny rozdiel na posúdenie oprávnenému používateľovi. Nedokončený stav potrebuje konkrétnu obnovu podľa merge návrhu; UI neponúkne slepé opakovanie zápisu ani všeobecné automatické vrátenie zmien.

Prvá verzia nemusí ponúkať automatický návrat celej produkcie. Ak sa neskôr pridá rollback, potrebuje vlastnú špecifikáciu. Ani `Zrušiť plánovanie`, ani `Zahodiť pracovnú verziu` nesmú pôsobiť ako rollback čiastočne vykonaného merge.

Počas `MERGING`/`MERGE_FAILED` sa namiesto preklikateľného náhľadu ponúkne priebeh a odkaz na aktuálnu produkciu. Po úspešnom merge je pracovná verzia iba na čítanie a zmizne z výberu aktívnych verzií. V prehľade ostane záznam výsledku. Ďalšia kampaň sa začne novou pracovnou verziou.

## 9. Hranice podporovaných úprav

Redaktor môže meniť iba podporované polia a operácie. Schválený základ nepripúšťa editáciu kódu a šablón. Doplnkové metadáta súborov, galerijné údaje a správa definícií kategórií médií sa riadia potvrdeným rozsahom Q7. Priradenie média do už existujúcich kategórií je editovateľnou súčasťou jeho pracovnej verzie a jeho porovnania/hashe; produkčná zmena členstva preto môže vyvolať konflikt celého média.

Zdieľané systémové stránky sú iba na čítanie. Redakčné súbory používajú dnešné povolené obsahové korene, externé priečinky domén a práva k priečinkom. Q20 vyžaduje doménové oddelenie; UI preto nepotrebuje variant pre spoločné súborové úložisko. Nevzniká nová evidencia vlastníctva súboru ani skener jeho použití.

Rozhranie nebude sľubovať automatické zrkadlenie pracovných úprav do ďalších stránok. Dotknuté väzby sa ukážu v kontrole pripravenosti podľa návrhu závislostí. Zmena URL stránky tiež nevytvorí potichu presmerovanie mimo rozsahu podporovaných tabuliek. Detail zmeny a záverečný súhrn uvedú pôvodnú a novú URL s upozornením `Skontrolujte odkazy na pôvodnú adresu; automatické presmerovanie sa nevytvorí.`

Správca súborov podporuje vytvorenie prázdneho adresára a operácie nad jednotlivými bežnými súbormi. Premenovanie, presun, zmazanie a rekurzívne kopírovanie celého súborového adresára sú podľa Q17 vypnuté s vysvetlením; strom webových stránok toto obmedzenie nemá. Presun/premenovanie súboru tiež nevytvorí automatický redirect ani neupraví `file_atr`/`dirprop`; zobrazí upozornenie na pôvodnú cestu. Galerijné adresáre vrátane originálov a miniatúr sú podľa Q18 iba na čítanie do rozšírenia galérie. Nový adresár dedí dnešné práva rodiča; vlastnú ochranu či galerijné nastavenia vo vetve nedostane.

Zakázanie tlačidla alebo položky menu nie je autorizácia. Každý zápisový endpoint, upload a pomocná operácia musí serverovo overiť podporu branch. Nepodporovaná operácia sa nesmie ticho vykonať nad produkciou.

## 10. Nadviazanie na existujúce zdroje

| Zdroj | Navrhovaná zmena |
| --- | --- |
| [HeaderBean](../../../src/main/java/sk/iway/iwcm/admin/layout/HeaderBean.java) | Doplniť údaje aktuálnej pracovnej verzie a dostupných možností; dnes nesie domény a aktuálnu doménu. |
| [LayoutService](../../../src/main/java/sk/iway/iwcm/admin/layout/LayoutService.java) | Pripraviť overený branch kontext a možnosti podľa domény a oprávnení. |
| [sidebar.pug](../../../src/main/webapp/admin/v9/views/partials/sidebar.pug), [header.pug](../../../src/main/webapp/admin/v9/views/partials/header.pug) | Prepínač pri doméne a trvalý textový indikátor v hlavičke. |
| [webjet.js](../../../src/main/webapp/admin/v9/src/js/webjet.js) | Využiť vzor `changeDomain`; doplniť navigáciu a potvrdenie neuložených zmien pre branch. |
| [web-pages-list.pug](../../../src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug), [web-pages-list.js](../../../src/main/webapp/admin/v9/src/js/pages/web-pages-list/web-pages-list.js) | Kontext stromu, navigácie a otvorenia editora. |
| [web-pages-datatable.js](../../../src/main/webapp/admin/v9/src/js/pages/web-pages-list/web-pages-datatable.js), [group-actions.js](../../../src/main/webapp/admin/v9/src/js/pages/web-pages-list/group-actions.js) | Prenos kontextu do zápisových a pomocných requestov; obmedzenia podporovaných akcií. |
| [preview.js](../../../src/main/webapp/admin/v9/src/js/pages/web-pages-list/preview.js) | Odlišná akcia pre uložený preklikateľný branch náhľad; dnešný `EditorPreview` posiela jednu rozpracovanú stránku do `/admin/rest/web-pages/preview/`. |
| [FsService](../../../src/main/java/sk/iway/iwcm/system/elfinder/FsService.java) | Pracovná volume a kontext správcu súborov; zachovať logické cesty. |
| [MediaRestController](../../../src/main/java/sk/iway/iwcm/components/media/MediaRestController.java) | Pracovný kontext vnoreného aj samostatného prehľadu médií. |

Nový prehľad použije existujúce DataTables/Editor, Bootstrap 5, Tabler ikony a `Prop` pre preklady. Nový frontend framework nie je potrebný. Prístupnosť sa overí klávesnicou, viditeľným fokusom, názvami ovládacích prvkov a čitateľnými stavmi. Detaily porovnania možno realizovať samostatnou stránkou alebo existujúcim veľkým dialógom; rozhodnutie musí zohľadniť čitateľnosť troch verzií textu.

Backend používa návrhové operácie pod `/admin/rest/branches/{id}/...`; konkrétne kontrakty sú v `03` a `04`. Pri čítaní a zápise ostávajú serverové kontroly oprávnení a existujúca CSRF ochrana.

## 11. Akceptačné scenáre UI

1. Redaktor po prepnutí jasne rozpozná aktívnu branch v strome, editore, médiách, súboroch a náhľade. Prepnutie s neuloženým obsahom ho neuloží ani nestratí bez vedomej voľby.
2. Po zmene branch v jednej záložke server odmietne zápis starého formulára z druhej záložky pre nezhodu explicitného a session kontextu. Formulár nezapíše do pôvodnej ani novej branch a jeho rozpracovaný obsah zostane v prehliadači.
3. Prehľad zobrazí nové, upravené a zmazané položky s konkrétnym dôvodom nepripravenosti. Opakovaná kontrola aktualizuje výsledok.
4. Porovnanie odlišuje pôvodný stav, produkciu a branch. Voľba sa týka celého objektu a nepublikuje údaje. Úprava v normálnom branch editore zneplatní staré riešenie a vyžiada nové porovnanie bez straty uloženého pracovného obsahu.
5. Pri binárnom konflikte funguje náhľad/stiahnutie oboch dostupných verzií a jednoznačná voľba výsledku.
6. Náhľad zostane v rovnakej branch pri navigácii, načítaní súborov a thumbnailov aj po zmene admin prepínača. Neprihlásený používateľ obsah nezíska.
7. Zmena termínu presunie celý balík jednou operáciou. Neskôr vytvorený kolidujúci individuálny plán zastaví balík pred prvým zápisom so stavom `NEEDS_ATTENTION`. Bežiace publikovanie už nemožno zmeniť cez formulár starého plánu.
8. Simulovaná čiastočná chyba zobrazí pravdivý počet dokončených položiek. Obnova stránky zachová priebeh a pokračovanie nezduplikuje výsledky. Obsah branch ostane uzavretý; nové `LIVE/WORK` riešenie je dostupné iba pre položky bez produkčného zápisu. Novšia produkčná zmena už aplikovanej položky vyžiada konkrétne posúdenie obnovy.
9. Nepodporovaná akcia vráti zrozumiteľnú chybu; manuálne odoslaný request nezmení produkciu. Oprávnenia sa overia aj pri publikovaní a pokračovaní.
10. Základné obrazovky, tri panely porovnania a dialógy fungujú pri šírkach pod 1200 px a pod 768 px, klávesnicou a s čitateľnými stavmi bez závislosti od farby.
11. Vytvorenie cez prehľad aj prepínač založí prázdnu `DRAFT` verziu pre aktuálnu doménu. Otvorí detail bez zmeny admin výberu; až `Upravovať web` aktivuje novú verziu. Prvé uloženie pridá zmenu automaticky.
12. Naplánovaná verzia zostáva iba na čítanie; otvorenie detailu ani výber nezruší plán. Uzavretie verzie pri otvorenom editore neprepne starý zápis do produkcie.

Existujúce vzory testov: [webpages/media.js](../../../src/test/webapp/tests/webpages/media.js), [webpages/planned-publishing.js](../../../src/test/webapp/tests/webpages/planned-publishing.js), [admin/elfinder.js](../../../src/test/webapp/tests/admin/elfinder.js) a [a11y/elfinder.js](../../../src/test/webapp/tests/a11y/elfinder.js). Nové scenáre sa vytvoria až počas príslušnej implementačnej etapy podľa `wj-codeceptjs`.
