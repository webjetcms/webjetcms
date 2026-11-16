# Redakčné rozhodnutia a metadáta

Stav: 15. 11. 2026. Používateľ potvrdil obsahový smer a rozsah šiestich stránok. Dokument obsahuje interné poznámky k textom; nevkladá sa do verejného článku.

Diagnóza podľa skillu `copywriting`: **A, nový text podľa konkrétneho zadania**. Podkladom sú potvrdené obchodné priority a dokumentácia; existujúci web určuje vizuálny rámec, nie povinnosť zachovať staré argumenty.

## Dohodnuté posolstvo

- WebJET CMS predstavujeme ako podnikový CMS s AI pre veľké spoločnosti a verejnú správu. Menšie projekty text nevylučuje.
- Hlavným prínosom je kontrola publikovania: rozdelenie práv, schvaľovanie obsahu a plánovanie zverejnenia. Pri dostupných funkciách opisujeme prácu s jednotlivými stránkami.
- AI ukazujeme pri konkrétnych úlohách v redakcii. Podmienky dostupnosti a použité služby musia zodpovedať dokumentácii; samotná integrácia neznamená, že údaje zostávajú v infraštruktúre zákazníka.
- Pripravovaný branching a Spring Boot patria do zreteľne oddeleného bloku **Pripravujeme**. Neprezentujú sa ako súčasť dnešného štandardného nasadenia a nepridáva sa termín vydania.
- Vývojári majú samostatnú cestu cez Java/Spring, vlastné aplikácie, REST/headless a dokumentáciu. Agentúry majú vlastnú stránku zameranú na klientské projekty a rozhovor o spolupráci.
- Referencie zo zadania možno pomenovať. Nepripisujeme klientom používanie AI, konkrétne výsledky ani citácie, ktoré neposkytli.
- Na úvode sú Licencie a Aktuálna verzia iba odkazy. Obsah týchto stránok ani stránky Služby sa v tomto balíku neprepisuje.

Hlavné CTA je **Dohodnúť si ukážku**, smeruje na existujúcu adresu `/kontakt/`. Ide o dohodnutie ďalšieho kroku cez kontakt, nie okamžitú rezerváciu v kalendári. Na stránke Pre agentúry sa používa **Porozprávať sa o spolupráci**, tiež s cieľom `/kontakt/`. Nepridávajú sa sľuby partnerských zliav, SLA, školení alebo automatickej migrácie.

Zvolený H1 v dodanom úvode je **WebJET CMS pre firemné weby. AI pre váš redakčný tím.** V HTML je medzi vetami zalomenie riadka. Nadpis pomenúva produkt, publikum a pomoc AI; perex ho dopĺňa konkrétnou prácou so schvaľovaním a plánovaním. Výsledná verzia je v [sk/index.html](sk/index.html).

## Návrh názvov a popisov stránok

Metadáta sa nastavujú vo vlastnostiach stránky v CMS, mimo obsahu `article.ly-article`. Do fragmentov sa nevkladajú elementy `title`, `meta` ani `link rel="canonical"`. Nasledujúce názvy sú návrhom pre titulok dokumentu; viditeľné H1 môžu byť dlhšie a vysvetľovať konkrétny prínos.

| Stránka a súbor | Verejná URL | Titulok stránky | Meta description |
| --- | --- | --- | --- |
| Úvod, `sk/index.html` | `/` | WebJET CMS: redakčný systém s AI pre firmy | Spravujte firemné weby a portály vo WebJET CMS. AI pomáha s obsahom, schvaľovanie a plánovanie držia publikovanie pod kontrolou. Dohodnite si ukážku. |
| Benefity, `sk/benefity.html` | `/benefity/` | Benefity WebJET CMS: AI, headless a viackrokové formuláre | Spoznajte hlavné funkcie WebJET CMS: AI, schvaľovanie a plánovanie obsahu, headless režim aj viackrokové formuláre pre žiadosti a dopyty. |
| Funkcie, `sk/funkcie.html` | `/funkcie/` | Funkcie WebJET CMS: AI, editor a správa webov | Spoznajte AI nástroje, PageBuilder, schvaľovanie, oprávnenia a správu jazykových verzií vo WebJET CMS. Pozrite si funkcie a ukážky z administrácie. |
| Aplikácie, `sk/aplikacie.html` | `/aplikacie/` | Aplikácie WebJET CMS pre firemné weby a portály | Formuláre, novinky, dokumenty, vyhľadávanie a ďalšie aplikácie WebJET CMS. Vyberte si podľa úloh, ktoré má váš web riešiť pre návštevníkov a redakciu. |
| Pre vývojárov, `sk/pre-vyvojarov.html` | `/pre-vyvojarov/` | WebJET CMS pre vývojárov: Java, Spring a REST API | Rozvíjajte WebJET CMS v Java a Spring. Vytvárajte vlastné aplikácie, administráciu nad dátami a headless weby s pomocou dokumentácie a projektu basecms. |
| Pre agentúry, `sk/pre-agentury.html` | `/pre-agentury/` | WebJET CMS pre agentúry a ich klientské projekty | Pripravte klientom web s vlastnými šablónami, aplikáciami a redakčnými právami vo WebJET CMS. Porozprávajme sa o vašich projektoch a spolupráci. |

Pre úvod odporúčame zachovať verejnú adresu `/`. Ak CMS interne používa stránku s cestou `/domov/`, text sa vloží do nej podľa existujúceho smerovania; nevytvára sa druhá indexovateľná kópia úvodu. Ostatné existujúce URL zostávajú zachované. Dve nové adresy treba vytvoriť v CMS pred zverejnením odkazov na ne.

Neuvádzame odhad objemu vyhľadávania ani prísľub konkrétnej pozície vo výsledkoch. Každá stránka má odlišný účel: úvod vysvetľuje produkt, benefity pracovný výsledok, funkcie schopnosti a aplikácie použitie. Technické a agentúrne stránky obsluhujú osobitné rozhodovanie.

## Zaradenie do existujúceho menu

Menu je mimo `article.ly-article`. Vloženie šiestich fragmentov ho nezmení a samo nevytvorí nové stránky. Používateľ rozhodol ponechať **Pre agentúry** a **Pre vývojárov** zatiaľ mimo horného menu, pretože sa do existujúcej navigácie rozumne nezmestia. Stránky zostávajú dostupné cez pomenované odkazy na úvode a z relevantných sekcií.

Pre budúcu úpravu je pripravený [návrh preskupenia navigácie](navigation-proposal.md): **Čo WebJET dokáže, Služby, Licencie, Dokumentácia** a CTA **Dohodnúť si ukážku**. Prvá položka združí Benefity, Funkcie, Aplikácie, Aktuálnu verziu aj stránky pre agentúry a vývojárov do jedného dvojstĺpcového podmenu. Používateľ odmietol pôvodný názov „Produkt“ ako nejasný; „Čo WebJET dokáže“ je nový návrh, zatiaľ nepotvrdený. Zoskupenie nahrádza predchádzajúce odporúčanie pridávať do súčasného riadka samostatnú položku „Pre tímy“. Zatiaľ sa neimplementuje.

## Interné alternatívy hlavného nadpisu a CTA

Varianty sú iba podklad na budúce porovnanie; žiadny z nich nenahrádza zvolený H1 uvedený vyššie. V odovzdanom obsahu zostáva potvrdené CTA **Dohodnúť si ukážku**. Alternatívy neotvárajú schválené rozhodnutia a nie sú súčasťou publikovaného HTML.

| Variant | Hlavný H1 | CTA | Dôvod |
| --- | --- | --- | --- |
| A: produkt a publikum | WebJET CMS: AI pre obsah, kontrola nad publikovaním | Dohodnúť si ukážku | Spája dve hlavné priority. Nasledujúci perex musí pomenovať firmy a verejnú správu a ukázať, čo „kontrola“ znamená. |
| B: pracovný postup | Pripravte obsah s AI. Zverejnite ho po schválení. | Pozrieť WebJET CMS v praxi | Konkretizuje redakčnú úlohu. Vyžaduje sprievodný text o nastavených schvaľovacích pravidlách; nejde o tvrdenie, že schvaľovanie je povinné v každej inštalácii. CTA by muselo ďalej vysvetliť, že ukážku treba dohodnúť. |
| C: jasná kategória | Redakčný systém s AI pre firemné weby a portály | Ukázať WebJET CMS nášmu tímu | Rýchlo vysvetlí typ produktu. Je menej osobitý, preto potrebuje bezprostredne konkrétnu obrazovku a dôkaz redakčného riadenia. |

Finálny úvod používa produktový a redakčný prísľub zo zvoleného H1, konkrétny perex a potvrdené CTA. Technológie Java/Spring majú vlastnú sekciu a vývojársku stránku, aby hlavný prísľub zostal zrozumiteľný aj zadávateľovi webu.

## Kontrola tvrdení pred publikovaním

Opierať sa o [produktové dôkazy](product-evidence.md), [vývojárske podklady](developer-agency-evidence.md) a [konkurenčnú analýzu](competitor-analysis.md). Rozlišovať funkciu dostupnú v produkte, potrebné nastavenie a pripravovanú schopnosť. Prípadné budúce tvrdenie o úspore času potrebuje meranie konkrétnej úlohy; tvrdenie o certifikácii príslušný platný doklad; klientsky výsledok odsúhlasenú prípadovú štúdiu. Kým tieto dôkazy nie sú k dispozícii, stačí presne opísať prácu, ktorú používateľ vykoná.
