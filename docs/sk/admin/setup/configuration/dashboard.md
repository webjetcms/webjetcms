# Konfigurácia úvodnej obrazovky a označenia prostredia

Pozadie uvítacieho panelu a označenie prostredia nastavíte cez **Nastavenia → Konfigurácia**. Po zmene obnovte stránku. Označenie sa zobrazuje pred názvom stránky v spoločnej hlavičke administrácie v9 a na prihlasovacej stránke. Na mobile je za tlačidlom menu. Postup úpravy hodnôt opisuje [Konfigurácia](README.md).

## Konfiguračné premenné

| Premenná | Predvolená hodnota | Význam |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `linear-gradient(111.7deg, #031D46 0%, #073780 76.92%)` | Pozadie uvítacieho panela: CSS gradient alebo cesta k obrázku. Prázdna alebo neplatná hodnota použije predvolený modrý prechod. |
| `dashboardSocialRefreshHours` | `8` | Počet hodín platnosti uloženého zoznamu externých noviniek. Hodnota `0` alebo záporná hodnota vypne jeho načítavanie aj zobrazovanie uložených položiek. Rozbalenie panela pri zapnutej funkcii vždy overí aktuálny zoznam. |
| `overviewJsonUrl` | `https://docs.webjetcms.sk/json/` | Základná URL súborov `wjnews.sk.json`, `wjnews.cs.json` a `wjnews.en.json`. Možno nastaviť vlastný HTTP(S) server alebo lokálnu cestu k priečinku s týmito súbormi. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Text označenia. Podporuje makrá aj vlastný text. Zobrazuje najviac 8 znakov veľkými písmenami; celý názov zostane v tooltipe. Prázdna hodnota označenie skryje aj z titulku karty. |
| `dashboardEnvironmentIcon` | `auto` | Ikona podľa prostredia alebo názov ikony Tabler, napr. `rocket` alebo `ti-database`. Hodnota `none` alebo prázdna hodnota ikonu skryje. Pri neexistujúcom názve sa ikona nezobrazí. |
| `dashboardEnvironmentColor` | `auto` | Farba z palety podľa prostredia a štýlu. Vlastná farba `#RGB` či `#RRGGBB` automaticky dostane čierny alebo biely text a ikonu s kontrastom aspoň 4,5 : 1. |
| `dashboardEnvironmentStyle` | `auto` | `subtle` = jemný, `strong` = výrazný. Hodnota `auto` použije výrazný štýl pre PROD, jemný pre ostatné prostredia. |
| `dashboardEnvironmentDescription` | prázdna | Doplňujúci popis za celým názvom v tooltipe. Podporuje makrá. Tooltip funguje pri ukázaní myšou aj pri fokuse klávesnicou; Escape ho skryje. |
| `adminLoginLocationApiKey` | prázdna | Zisťovanie orientačnej polohy prihlásenia podľa IP adresy: prázdna hodnota použije bezplatné API IPWHOIS, `DISABLED` zisťovanie vypne, API kľúč použije platené API. Kľúč je viditeľný návštevníkom prihlasovacej stránky. Podrobnosti opisuje [Orientačná poloha prihlásenia](#orientačná-poloha-prihlásenia). |
| `adminNewDeviceDetectionEnabled` | `true` | Zapne rozpoznávanie a blokovanie prehliadačov administrátora, upozornenia na prehľade a emaily o novom zariadení. Hodnota `false` vypne aj kontrolu zablokovaných zariadení. Zisťovanie polohy riadi samostatne `adminLoginLocationApiKey`. |
| `adminNewDeviceMaxAgeDays` | `90` | Počet dní od posledného úspešného prihlásenia, počas ktorých sa prehliadač považuje za známy pre daný účet. Určuje aj platnosť cookie a lehotu čistenia nepoužívaných nezablokovaných zariadení. Neplatná alebo nekladná hodnota použije 90 dní. |

Názov sa pridáva aj do titulku karty prehliadača a prístupného názvu hlavičky, napríklad `[TEST] Webové stránky | WebJET CMS`. Ikona je voliteľná, text zostáva povinný. Na produkcii môžete označenie vypnúť prázdnou hodnotou `dashboardEnvironmentName`.

Pozadie zmeníte priamo hodnotou `dashboardHeroBackgroundImage`, napríklad:

- `linear-gradient(135deg, #102A43 0%, #486581 100%)` – vlastné farby a smer prechodu. Podporované sú aj radiálne, kužeľové a opakované CSS gradienty.
- `/images/company/header.jpg` – obrázok z webového sídla.
- `https://cdn.example.com/header.jpg` – obrázok z úplnej HTTP(S) adresy.

Obrázok nahradí gradient a pokryje celý panel so zachovaním pomeru strán (`cover`), zarovnaný na stred a spodný okraj. Cestu zadávajte priamo, bez obalu `url(...)`. HTTP(S) adresa nesmie obsahovať prihlasovacie údaje. Adresy začínajúce `//`, adresy so spätnými lomkami alebo riadiacimi znakmi a iné protokoly nie sú povolené. Maximálna dĺžka adresy je 1024 znakov.

## Novinky zo sociálnych sietí

Panel **Čo je nové** spája novinky nainštalovanej verzie s príspevkami z YouTube a Facebooku WebJET CMS. Prehliadač číta spoločný JSON zo servera nastaveného v `overviewJsonUrl`; zákaznícky server sociálne siete nekontaktuje. Generovanie údajov na docs serveri opisuje [Centrálny zoznam noviniek](../../../developer/install/social-media-feed.md).

JSON musí byť verejne dostupný bez prihlásenia, aj pri vlastnej lokálnej ceste: požiadavka neposiela prihlasovacie cookies ani hlavičku `Referer`.

Relatívna cesta obrázka v poli `image` sa doplní podľa priečinka s JSON. Pri `overviewJsonUrl=https://docs.webjetcms.sk/json/new/` sa napríklad `social-media/images/obrazok.jpg` načíta z `https://docs.webjetcms.sk/json/new/social-media/images/obrazok.jpg`. Pri presune zoznamu preto preneste aj podpriečinok s obrázkami. Absolútne HTTPS adresy obrázkov zo starších zoznamov zostávajú podporované.

Pri otvorení úvodnej obrazovky sa použije zoznam uložený v `localStorage`. Ak chýba alebo dosiahol platnosť nastavenú v `dashboardSocialRefreshHours` (predvolene 8 hodín), obnoví sa na pozadí. Pri každom rozbalení sa zoznam overí aj pred uplynutím tejto lehoty. Cache je uložená pod kľúčom `webjet.externalNews.v1` a obsahuje aj URL načítaného zoznamu. Pri zmene URL alebo jazyka sa načíta príslušný zoznam a prepíše túto položku; česká administrácia používa súbor `wjnews.cs.json`. V zbalenej rotácii sa načíta len náhľad práve zobrazenej novinky; ostatné obrázky sa vopred nenačítavajú. Obrázky kariet sa začnú načítavať po rozbalení panela. Náhľad v rotácii nemení stav prečítania.

Štítok **Nové** zostáva zobrazený, kým používateľ nerozbalí neprečítaný obsah. Rozbalenie potvrdí lokálne novinky aj práve zobrazené sociálne položky. Úspešné uloženie do nastavení účtu obsahuje dátum prečítania `readAt`; pre sociálne príspevky sa uchovávajú aj odtlačky ich ID pod kľúčom `dashboard.externalNews.<odtlačok základnej URL>`. Prepnutie jazyka ani úprava prekladu toho istého príspevku nevytvoria nové upozornenie. Nové ID ho vytvorí bez ohľadu na dátum publikovania. Dáta prijaté až po zbalení alebo v skrytej karte prehliadača sa neoznačia za prečítané; ak je panel stále rozbalený, označia sa pri návrate do karty.

Pri chybe načítania sa externé karty, položky rotácie a ich upozornenie skryjú aj vtedy, keď prehliadač má uložené staršie údaje. Panel nezobrazuje hlásenie o načítavaní ani chybe; zostanú lokálne novinky verzie a odkazy na sociálne siete. Uložená cache sa chybou nevymaže.

Hodnota `dashboardSocialRefreshHours=0` vypne požiadavky na JSON a obrázky, externé karty, ich rotáciu aj upozornenie na neprečítané položky. Platí to aj pre dáta, ktoré už prehliadač uložil. Novinky nainštalovanej verzie a odkazy na oba sociálne kanály zostanú dostupné. Po zmene konfigurácie obnovte stránku administrácie.

## Automatické označenie prostredia

Makro `{ENVIRONMENT_NAME}` určuje prostredie podľa názvu servera, cez ktorý pristupujete do administrácie. Zmena zvolenej obsahovej domény označenie nemení.

Detekcia nerozlišuje veľkosť písmen. Hľadá celé časti názvu oddelené bodkou, pomlčkou alebo podčiarkovníkom, aj s číslom uzla, napríklad `uat01` alebo `web-prod-02`. Pri viacerých zhodách platí poradie **PROD → UAT → CIT → INT → TEST → DEMO → LOCAL → DEV**.

| Prostredie | Rozpoznané časti názvu servera | Automatická ikona | Jemná farba / text | Výrazná farba / text |
| --- | --- | --- | --- | --- |
| PROD | `prod`, `prd`, `production`, `live` | `ti-alert-triangle` | `#FFD6D7` / `#790011` | `#C4001F` / `#FFFFFF` |
| TEST | `test`, `testing`, `qa` | `ti-test-pipe` | `#FFE0CC` / `#7A2E00` | `#C24E00` / `#FFFFFF` |
| CIT | `cit` | `ti-user-check` | `#EADFFF` / `#5200A3` | `#6E00DC` / `#FFFFFF` |
| INT | `int`, `integration`, `sit` | `ti-plug-connected` | `#DCE4FF` / `#0037A6` | `#0049BE` / `#FFFFFF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `preprod`, `preproduction` | bez ikony | `#C8F0F4` / `#004F59` | `#00717F` / `#FFFFFF` |
| DEMO | `demo` | `ti-eye` | `#FFF0B3` / `#5C4300` | `#F6BE3F` / `#13151B` |
| LOCAL | `local`, `localhost`, spätná slučka IPv4 `127.*.*.*` alebo IPv6 `::1` | `ti-device-laptop` | `#E6E8EE` / `#353944` | `#353944` / `#FFFFFF` |
| DEV | Ostatné názvy, napríklad `iwcm.interway.sk` | `ti-code` | `#CFF5E4` / `#00533D` | `#007E69` / `#FFFFFF` |

Predprodukcia zahŕňa aj zápisy `pre-prod`, `pre-production` a ich varianty s bodkou či podčiarkovníkom. Tieto časti názvu sa nepovažujú za PROD. Časť slova, napríklad `int` v `interway`, sa nezhoduje s prostredím INT. TEST je samostatné prostredie; názvy `test`, `testing` a `qa` už neoznačujú UAT.

Automatický vzhľad prednostne použije úvodné označenie prostredia v nakonfigurovanom texte, za ktorým nasleduje lomka, medzera, pomlčka alebo koniec textu. Pri inom vlastnom texte vychádza z detekcie servera. Ručne nastavená ikona alebo farba má prednosť. Pri neplatnej farbe sa použije automatický výber.

## Názov uzla a vlastné označenie

Napríklad `dashboardEnvironmentName={ENVIRONMENT_NAME}/{CLUSTER_NAME}` doplní názov aktuálneho uzla z `clusterMyNodeName`. Celý názov, napríklad `UAT/node-1`, je v tooltipe; štítok zobrazí prvých 8 znakov veľkými písmenami. Pri prázdnom názve uzla sa koncová lomka odstráni.

Pre krátky štítok a dlhší popis nastavte `dashboardEnvironmentName=TEST` a `dashboardEnvironmentDescription=Testovacie prostredie, uzol {CLUSTER_NAME}`. Nastavenie prostredia používa existujúcu konfiguráciu; samostatný dialóg na úvodnej obrazovke nie je k dispozícii.

## Detekcia nových zariadení

**Detekciu vypnete nastavením `adminNewDeviceDetectionEnabled=false`** v časti **Nastavenia → Konfigurácia**. Vypne sa rozpoznávanie nových prehliadačov, upozornenia na prehľade, emaily o novom zariadení aj kontrola už zablokovaných zariadení. Predvolená hodnota je `true`. Zisťovanie polohy má [samostatné nastavenie](#orientačná-poloha-prihlásenia).

Pri zapnutej detekcii WebJET CMS upozorňuje administrátora na úspešné prihlásenie z neznámeho prehliadača. Upozornenie dostane aj pri prvom prihlásení po zapnutí funkcie. Postup potvrdenia alebo zablokovania zariadenia opisujú [Systémové upozornenia](../../../redactor/admin/welcome.md#systémové-upozornenia). Detekcia sa nevzťahuje na bežných návštevníkov, API tokeny ani HTTP Basic.

Prehliadač sa rozpoznáva pomocou cookie `wjdevice`. Premenná `adminNewDeviceMaxAgeDays` určuje dobu zapamätania od posledného úspešného prihlásenia, predvolene **90 dní**. Nastavujte ju rovnako pre všetky domény. Po uplynutí tejto lehoty sa nepoužívané nezablokované zariadenia aj s upozorneniami automaticky vymažú; čistenie pokračuje aj pri vypnutej detekcii. Zablokované zariadenia sa nemažú.

**Overte doručovanie emailov a správnosť emailových adries administrátorov.** Odosielateľ sa nastavuje rovnako ako pri obnove hesla: `passwordResetDefaultSenderName` a `passwordResetDefaultSenderEmail`, prípadne všeobecné `defaultSenderName` a `defaultSenderEmail`. Odblokovanie prehliadača vyžaduje kód doručený na email účtu, aj keď používateľ používa 2FA. Pri problémoch s prístupom použite v danom prehliadači formulár `/admin/logon/` a dokončite emailové overenie.

Po vymazaní alebo exspirácii cookie sa prehliadač považuje za nový a jeho predchádzajúce blokovanie sa neuplatní. Pri podozrení na zneužitie účtu preto treba zmeniť heslo a zapnúť 2FA.

### Orientačná poloha prihlásenia

Zisťovanie polohy funguje nezávisle od detekcie zariadení. **Externé volania vypnete nastavením `adminLoginLocationApiKey=DISABLED`**; už uložené polohy zostanú zachované.

Premenná `adminLoginLocationApiKey` má tieto možnosti:

- Prázdna hodnota (predvolená): bezplatné API služby [IPWHOIS](https://ipwhois.io/documentation).
- `DISABLED`: vypnuté zisťovanie polohy.
- API kľúč: platené API. **Kľúč je viditeľný návštevníkom prihlasovacej stránky**, pretože službu volá ich prehliadač.

Poloha je orientačná a môže zodpovedať sídlu firmy alebo VPN bráne. Nepoužíva sa na overenie ani blokovanie zariadení. Jej nedostupnosť nebráni prihláseniu.

Poskytovateľ služby vidí verejnú IP adresu a doménu CMS. Ak pravidlá organizácie takéto externé volania neumožňujú, zisťovanie vypnite; podmienky spracovania údajov uvádza poskytovateľ v [zásadách súkromia](https://ipwhois.io/privacy). Pri obmedzenej CSP povoľte v `connect-src` používaný endpoint `https://ipwho.is` alebo `https://ipwhois.pro`.

### Novinky vydania

Text `admin.overview.changelog` zapisujte ako Markdown nadpis s verziou a zoznam bodov. Každý bod začína tučným názvom, za ktorým nasleduje pomlčka a opis. V súbore `.properties` používajte oddeľovač `\\n`; editor prekladových kľúčov môže obsahovať bežné konce riadkov.

```markdown
## WebJET CMS 2026.18

- **Schvaľovanie zmien priečinkov** – Zmeny priečinkov môžu prejsť viacúrovňovým schvaľovaním.
- **Testovanie prístupnosti** – Kontroly sú súčasťou automatizovaných testov.
```

Počet pri odkaze **Čo je nové** sa odvodí zo zoznamu. Starší text v odsekoch zostáva čitateľný. V `user_settings_admin` sa pre každého používateľa a jazyk uchováva záznam `dashboard.news.<jazyk>` s odtlačkom prečítaného textu (`fingerprint`) a dátumom prečítania (`readAt`). Ukladá sa po rozbalení viditeľného panela. Zmena textu obnoví štítok **Nové** aj v rovnakej verzii; upozornenie nevyprší po uplynutí času. Menu Pomocník číta rovnaký stav lokálnych noviniek, ale jeho otvorenie ho nemení. Uloženie ani reset rozloženia dashboardu tento záznam nemení.

Starší záznam s `firstSeen` znamenal iba prvé zobrazenie a nemal informáciu o prečítaní. Po aktualizácii sa preto lokálne novinky zobrazia ako neprečítané, kým ich používateľ raz nerozbalí. Existujúce uložené ID prečítaných sociálnych príspevkov zostávajú platné.
