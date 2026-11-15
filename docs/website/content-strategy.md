# Obsahová stratégia

Stav k 15. 11. 2026: používateľ v dvoch kolách rozhovoru podľa skillu `grilling` potvrdil obsahový smer a rozhodnutia nižšie. Finálne texty sú v `sk/*.html`; tento dokument vysvetľuje stratégiu. Podklad používa postup `content-strategy` a zistenia z dokumentácie aj verejných stránok konkurencie.

## Zadanie a publikum

Primárne publikum tvoria veľké spoločnosti a verejná správa. Web má pomôcť posúdiť WebJET CMS ľuďom zodpovedným za obsah, marketing, IT a nákup. InterWay sa nezameriava na malé zákazkové weby; samotný produkt však možno použiť aj pre menší web. Text nemá menšieho záujemcu vylučovať.

Sekundárny obchodný cieľ je osloviť agentúry, ktoré dnes pracujú s WordPressom. Ďalšou cestou sú Java/Spring vývojári a technickí lídri. Agentúra potrebuje rozumieť vhodným zákazkám a spolupráci, vývojár možnosti rozšírení a začiatku projektu. Tieto potreby sa nemajú zlúčiť do jedného všeobecného odseku o technológiách.

Samostatný produktový marketingový kontext sa v repozitári nenašiel. Neexistujú dodané analytické dáta, export kľúčových slov ani prepisy obchodných rozhovorov. Preto tu nie sú uvádzané domnelé objemy vyhľadávania, konverzné pomery alebo podiely námietok zákazníkov.

## Potvrdený obsahový smer

Predstaviť WebJET CMS ako systém pre firemné weby a portály, ktorý spája AI pri príprave obsahu s redakčnými pravidlami a možnosťami rozvoja v Java/Spring. AI ukázať vysoko na úvode cez konkrétne úlohy. Dôveru podložiť referenciami a reálnymi obrazovkami produktu.

Samotné AI označenie nestačí: [BUXUS 2.0](https://www.ui42.sk/vyvoj-a-programovanie/buxus-flexibilne-cms) aj [WordPress VIP](https://wpvip.com/) dnes výrazne komunikujú AI. Ide o odporúčanie na základe ich komunikácie, nie o tvrdenie, že nemajú redakčné riadenie alebo iné porovnateľné funkcie. Podrobnosti sú v [konkurenčnej analýze](competitor-analysis.md).

## Obsahové piliere

| Pilier | Potreba čitateľa | Doložiteľný materiál | Kam patrí |
| --- | --- | --- | --- |
| Obsah pod kontrolou organizácie | Rozdeliť prácu, kontrolovať publikovanie a dohľadať zmeny. | Oprávnenia, schvaľovanie, história, plánovanie, audit, domény a jazyky. | Úvod, Benefity, Funkcie. |
| AI pri konkrétnej práci | Pripraviť, upraviť alebo preložiť obsah bez prepínania medzi nástrojmi. | AI v editore a poliach, PageBuilder, práca s podkladmi a obrázkami; dostupnosť podľa nastavenia. | Úvod, Funkcie, vybrané aplikácie. |
| Java/Spring pre vlastné riešenia | Posúdiť zhodu s tímom a možnosti integrácií a rozšírení. | Vlastné aplikácie, JPA/DataTable administrácia, REST/headless, basecms, dokumentácia, AI skills. | Samostatná stránka Pre vývojárov. |
| Platforma pre agentúrne projekty | Odovzdať obsah klientovi, spravovať viaceré weby a rozvíjať zákazky. | PageBuilder, šablóny, klientské práva, MultiWeb; rozhovor o prvom projekte a rozdelení práce. | Samostatná stránka Pre agentúry. |

## Úloha jednotlivých stránok

| Stránka / pracovná téma | Typ a fáza | Vyhľadávací zámer na overenie | Obsahová úloha a vlastný prínos |
| --- | --- | --- | --- |
| Úvod: WebJET CMS pre firmy a verejnú správu | Produktová vstupná stránka; zvažovanie a rozhodovanie; vyhľadateľná aj zdieľateľná. | WebJET CMS, firemný CMS, redakčný systém pre organizácie. | Vysvetliť vhodnosť produktu, ukázať AI v reálnom rozhraní a nasmerovať na ďalší krok. Vlastným dôkazom sú dokumentácia a slovenské nasadenia. |
| Benefity: čo sa zmení v práci tímu | Prehľad pracovných situácií; zvažovanie; vyhľadateľný. | Správa firemných webov, schvaľovanie webového obsahu. | Prepojiť schopnosti s konkrétnym výsledkom práce: redaktor pripraví obsah, schvaľovateľ rozhodne, správca rozdelí práva. |
| Funkcie: ako WebJET rieši jednotlivé úlohy | Produktový prehľad s odkazmi na dokumentáciu; technické overovanie; vyhľadateľný. | WebJET funkcie, CMS s AI, viacjazyčný CMS. | Ukázať overiteľné schopnosti a relevantné podmienky bez sľubu, že každá inštalácia obsahuje všetko. |
| Aplikácie: funkcie webu podľa potreby návštevníka | Katalóg podľa použitia; zvažovanie; vyhľadateľný. | WebJET aplikácie, formuláre, vyhľadávanie a dokumenty v CMS. | Vysvetliť, na čo aplikácie slúžia, a umožniť pokračovať do návodu. Neprepisovať licenčnú maticu. |
| Pre vývojárov: CMS pre Java/Spring tím | Technická vstupná stránka; implementačné overovanie; vyhľadateľná aj zdieľateľná. | Java CMS, Spring CMS, Java headless CMS. | Vlastný prínos: konkrétna cesta od basecms k aplikácii v editore a administrácii nad dátami; kód a návody produktu. Spring Boot len podľa skutočného stavu. |
| Pre agentúry: prvý klientsky projekt | Stránka použitia a spolupráce; rozhodovanie; vyhľadateľná aj zdieľateľná. | CMS pre agentúry, WebJET pre agentúry. | Vlastný prínos: ukázať rozdelenie práce agentúra–klient–výrobca a prípravu projektu. Ponuka vedie k rozhovoru o spolupráci; neobsahuje nepotvrdené zľavy ani bezplatné školenia. |

Priorita prvých štyroch stránok vyplýva zo zadania. Ako ďalšie samostatné fragmenty vznikli Pre vývojárov a Pre agentúry. Číselné SEO skórovanie by bez dát vytváralo neodôvodnenú presnosť.

## Mapa obsahu

```text
Úvod
├── Benefity: pracovné situácie a výsledky
│   └── Funkcie: konkrétne schopnosti → dokumentácia
├── Aplikácie: prehľad podľa účelu → návody
├── Pre vývojárov (nový vstup)
│   ├── basecms a vlastné aplikácie
│   ├── REST / headless
│   └── dokumentácia a AI skills
└── Pre agentúry (nový vstup)
    ├── klientská redakcia a viac webov
    ├── technická cesta → Pre vývojárov
    └── rozhovor o spolupráci
```

Hlavné menu už má osem obsahových položiek a kontakt. Nové cesty majú byť obsahovo samostatné, ale presné zaradenie do menu musí rešpektovať šírku existujúcej navigácie. HTML fragment menu automaticky nemení. Aktuálne stránky Služby, Aktuálna verzia a Licencie zostávajú mimo obsahového prepisu.

## Konverzia a meranie

Potvrdené hlavné CTA je „Dohodnúť si ukážku“ a smeruje na existujúci kontakt `/kontakt/`. Sekundárne odkazy vedú k funkciám a dokumentácii. Agentúrna stránka používa „Porozprávať sa o spolupráci“. Rezervačný kalendár, bezplatný pilot ani termín odpovede nie sú sľúbené.

Pri neskoršom vyhodnotení sledovať kvalifikované dopyty a cestu z jednotlivých stránok ku kontaktu; technické návštevy dokumentácie oddeliť od záujmu o realizáciu webu. Ide o návrh merania, nie súčasť implementácie tohto balíka. Do fragmentov sa nepridávajú nové trackovacie skripty.

## Distribúcia v rozsahu tohto zadania

Stránky majú fungovať aj ako konkrétny odkaz v obchodnej komunikácii: benefity pre zadávateľa, vývojárska stránka pre IT a agentúrna stránka pre záujemcu o spoluprácu. Prípadné sociálne príspevky a články môžu neskôr vychádzať z rovnakých doložených ukážok. V tomto zadaní sa nevytvára publikačný kalendár, newsletter ani ďalšie kanály.

## Hranice tvrdení

Funkcie a obrázky overovať podľa [produktových podkladov](product-evidence.md) a [vývojárskych podkladov](developer-agency-evidence.md). Nepoužívať nepodložené percentá úspor, bezpečnosť odvodenú z nízkej rozšírenosti, zaručený zákonný súlad ani označenie celého produktu za bezplatný open source. Referencie nedokazujú používanie všetkých prezentovaných funkcií konkrétnym klientom.

## Potvrdené rozhodnutia z rozhovoru

1. Hlavný ďalší krok: dohodnutie osobnej ukážky.
2. Firemný CMS s konkrétnou AI pomocou; AI samotná nie je celý prísľub.
3. Jednoduchosť redakcie, kontrola, integrácie a rozvoj; používateľ osobitne zdôraznil schvaľovanie a plánovanie zmien.
4. Vlastná stránka pre agentúry s rozhovorom o spolupráci; ďalšie obchodné výhody neboli potvrdené.
5. Spring Boot a spoločné plánovanie viacerých stránok z PR #388 označiť „Pripravujeme“, bez termínu.
6. Na úvode ponechať len odkazy na Licencie a Aktuálnu verziu.
7. Následné doplnenie zadania: na úvode živá sekcia noviniek z `https://docs.webjetcms.sk/json/wjnews.sk.json`, zhodného zdroja ako v dashboarde. Zobrazuje až šesť príspevkov/videí; pri nedostupnosti zdroja zostáva statický odkaz na dokumentáciu. Podrobnosti sú v [integrácii noviniek](news-feed.md).
8. Ak dokumentácia obsahuje tematicky zodpovedajúce video, uprednostniť ho pred statickou snímkou. Použiť aplikáciu `video_player.jsp` cez zadaný `!INCLUDE`, bez automatického prehrávania. Výber a umiestnenie sú v [manifeste videí](video-manifest.md).
