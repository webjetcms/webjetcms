# Riadenie realizácie pracovných vetiev

Tento dokument je vstupný bod pre agenta realizujúceho dohodnutý návrh. Aktuálne ide **iba o dokumentáciu**; implementáciu spustí až neskorší výslovný pokyn používateľa. Pripravenosť a dokončené kroky sú v [STAV.md](STAV.md).

## Ako prácu spustiť

Najskôr posúď [prehľad návrhu](README.md), [rozhodnutia](01-rozhodnutia.md) a [rozsah/odhad](06-plan-implementacie.md). Ak návrh zodpovedá zamýšľanému výsledku, môžeš v tomto alebo novom chate zadať:

```text
Implementuj iba krok P01 návrhu docs/research/publish-packages.
Riaď sa docs/research/publish-packages/ORCHESTRACIA.md.
Vychádzaj z uzatvorených rozhodnutí, výsledok zapíš do STAV.md.
Zachovaj cudzie lokálne zmeny a nevytváraj Git commity.
Po P01 skonči s výsledkami overenia a aktualizovaným odhadom.
```

Pokyn na implementáciu konkrétneho kroku zároveň oprávňuje vykonať jeho bežné úpravy a overenia. Agent nemá pýtať ďalšie potvrdenie iba preto, že tento adresár pôvodne vznikol v návrhovej fáze.

Po dokončení a posúdení výsledkov P01 sa pokračuje napríklad:

```text
Implementuj krok P02 podľa docs/research/publish-packages/ORCHESTRACIA.md.
Skontroluj výsledky P01 v STAV.md a nadviaž na existujúci kód.
Dokonči rozsah a overenia P02, aktualizuj STAV.md a skonči.
Bez Git commitov a bez nasadenia na produkciu.
```

P02 sa postupne nahradí P03 až P08. Nepotrebuješ kopírovať celý návrh do promptu. Ak chceš dlhší súvislý beh, rozsah povedz výslovne, napr. „realizuj P03–P05“. Bez takéhoto zadania agent po určenom kroku nepokračuje do ďalšieho.

Budúce moduly sa zadávajú osobitne podľa [08 — Rozširovanie modulov](08-rozsirovanie-modulov.md). P02/P06 pripravia iba spoločnú obsluhu typov; E01 vytvorí spoločné JSON úložisko až spolu s prvým modulom E02. E03 môže neskôr pridať galériu. Tieto kroky nie sú automatickým pokračovaním P08. V dokumente 08 je presný vzor zadania základného bannera bez úprav štatistík.

Nový chat nie je technickou podmienkou; dôležitý je rovnaký checkout a evidencia výsledkov. Ak používateľ chce nový chat vytvoriť, môže tak urobiť cez dostupné nástroje aplikácie. Orchestrátor sám nezakladá používateľské chaty iba kvôli internému rozdeleniu práce; na pomocné paralelné úlohy použije subagentov.

## Vstupný postup agenta

1. Prečítaj koreňové `AGENTS.md`, `.github/agents/AGENTS.md` a pracovný návod. Skontroluj `git status` a aktuálnu vetvu. Zachovaj existujúce nesúvisiace zmeny; nevytváraj commity.
2. Prečítaj tento dokument, `STAV.md`, `01-rozhodnutia.md` a **iba zadanú sekciu** v `06-plan-implementacie.md`.
3. Pre P01 načítaj aj [kontrolu pripravenosti](09-kontrola-pripravenosti.md); uzatvorené Q16–Q19 sa znova nepýtajú. Over dokončenie predpokladov kroku podľa evidencie aj skutočného kódu. Samotný štítok DONE bez výsledkov nie je dôkaz.
4. Načítaj návrhové dokumenty a zdrojové analýzy uvedené ako vstupy daného kroku. Presné implementačné správanie over proti aktuálnemu kódu; čísla riadkov v analýzach sú iba orientačné z času návrhu.
5. Zaznamenaj v `STAV.md` krok `IN_PROGRESS`, použitú vetvu, výchozí stav a stručný rozsah. Pre implementáciu použi vhodnú existujúcu feature vetvu. Ak nie je, riaď sa repozitárovým pomenovaním a prideleným issue ID; číslo issue si nevymýšľaj.
6. Vykonaj zadaný rozsah, príslušné overenia a skontroluj celý vlastný diff. Žiadna produkčná aktivácia, databázová konverzia alebo deploy nie je implicitnou súčasťou lokálnej implementácie.

**Výstup vstupného postupu:** agent pozná konkrétny krok, jeho závislosti, povolené súbory/hranice a prípady z testovacej matice. Ak závislosť reálne chýba, uvedie konkrétny chýbajúci výsledok; neoznačí ju automaticky za splnenú.

## Rozdelenie medzi agentov

Jeden hlavný agent vlastní integráciu, zmluvy a záznam stavu. Pomocní agenti môžu paralelne:

- overovať databázové profily alebo konkrétne čítacie cesty;
- implementovať samostatné súbory služieb/UI so zadaným kontraktom;
- kontrolovať hotový diff a nežiaduce produkčné vedľajšie účinky;
- vykonať nezávislé relevantné testy, ak sa nekrižujú testovacie dáta.

Pred delegovaním hlavný agent uvedie výstup, vlastníctvo súborov a závislosti. Dvaja agenti neupravujú súčasne `DocDB`, `GroupsDB`, editor pipeline, migrácie alebo spoločné kontrakty. P04/P05 možno rozdeliť po uzavretí zmlúv P02/P03. P06 nevzniká paralelne nad neurčeným pracovným formátom.

Každý pomocný agent odovzdá zmenené súbory, overené správanie a neoverené podmienky. Hlavný agent zodpovedá za konzistenciu výsledku, aj keď kontrolu vykonal ďalší agent.

## Pravidlá pri rozpore s návrhom

- Bežný názov privátnej metódy, rozdelenie helpera alebo ekvivalentná technická implementácia nevyžaduje nové produktové rozhodnutie. Agent zvolí najjednoduchšie riešenie v existujúcom štýle.
- Ak zistenie mení potvrdenú požiadavku, napr. stabilné ID, izoláciu produkcie, rozsah podporovaného preview, oprávnenia alebo význam merge, agent najskôr doloží problém a konkrétny návrh úpravy. Závislú časť neimplementuje pod opačným predpokladom; nezávislé autorizované časti môže dokončiť.
- Ak sa mení verejná Java/REST zmluva medzi etapami, aktualizuje sa jej jediný návrhový zdroj aj dotknutí volajúci. Nevytvárať súbežné alternatívne návrhy bez určenia platnej verzie.
- „Zjednodušenie“ nesmie znamenať produkčný zápis počas práce vo vetve, prečíslovanie nového objektu pri merge alebo tiché zobrazenie produkcie pri chybe náhľadu.
- P03 už obsahuje čítanie médií/súborových helperov a mapu zvoleného výsledku; P05 dodá redakčné zápisy, P06 UI rozhodnutí. ADMIN vždy uchová WORK, PREVIEW rešpektuje Q16. Neodsúvaj túto zhodu až na záverečný test.
- Nový všeobecný workflow/lock/cache framework je mimo zadania. Ak obyčajná služba a krátka DB operácia riešia konkrétny problém, použi ich.
- `NewsApp`/`NewsActionBean` je povinné jadro podľa doplnenia N1 a [03a — Novinky](03a-novinky.md). Neodsúvaj ho do voliteľného adaptéra. P03 nemá DONE bez čítacieho/render rozsahu T16; P04 musí dokázať reálne založenie novinky a P06 zhodu s produkciou po merge. Hranica vlastných ResultCriteria/contextClasses zostáva podľa `03a`.
- `menu_ul_li.jsp`/`MenuULLI` je povinné jadro podľa N4 a [03b — Menu](03b-menu.md). P03 nemá DONE bez skutočného renderovania M01–M09; P04 overí aj `MenuApp` selector a P06 zhodu menu po merge. Ponechaj existujúci renderer nad spoločnými fasádami; nový menu framework ani pracovná tabuľka sa nepridávajú.
- Ďalší modul automaticky neznamená ďalšiu pracovnú tabuľku. Použi postup z `08`; bežný modul má explicitný DTO/handler a spoločné JSON úložisko. Typovanú výnimku zdôvodni konkrétnou query potrebou. Nevytváraj druhý merge alebo scheduler a nerozširuj úzky bannerový rozsah o riešenie štatistík.

## Dokončenie kroku

Krok môže dostať `DONE`, iba ak:

1. Spĺňa všetky dokončovacie podmienky svojej sekcie plánu.
2. Sú zapísané vykonané kontroly, prostredie a výsledky; nevykonaná kritická podmienka je viditeľná. `PARTIAL` je správnejší než nepravdivé DONE.
3. Je skontrolovaný vlastný diff aj interakcia s existujúcim produkčným tokom.
4. Nasledujúci agent má popísané zavedené API, migrácie/feature flag, relevantné súbory a nevyriešené záväzky.
5. Zmeny zostali necommitnuté a používateľ dostane stručné zhrnutie výsledku, testov a obmedzení.

Záznam dokončenia vlož do `STAV.md` pod príslušný krok, napríklad:

```text
P03 — DONE
Vetva / výchozí commit: ...
Výsledok: ...
Hlavné zmenené súbory a API: ...
Overenie: príkaz, DB/profil, výsledok, report
Neoverené podmienky: ...
Odchýlky od návrhu: ...
Vstup pre P04/P05: ...
```

Výchozí commit je identifikátor na orientáciu, nie pokyn vytvoriť nový commit. Používateľ môže medzi krokmi zmeny sám commitnúť; nasledujúci agent pracuje so skutočným aktuálnym stavom.

## Dokumentačné zdroje podľa kroku

| Krok | Hlavné podklady |
| --- | --- |
| P01 | `02a-identifikatory.md`, `03-nahlad-a-java.md`, `03a-novinky.md`, `03b-menu.md`, odporúčanie/príprava v `08`, tri `analyza-*.md` |
| P02 | `02-datovy-model.md`, `02a-identifikatory.md`, kontext v `03`, spoločný kontrakt v `08` |
| P03 | `03-nahlad-a-java.md`, `03a-novinky.md`, `03b-menu.md`, `05-rozhranie.md`, čítacia/súborová analýza |
| P04 | `02`, pracovné ukladanie v `04`, `03a-novinky.md`, `03b-menu.md`, `05`, zapisovacia analýza |
| P05 | `02`, súborová analýza, `03`, mediálne/súborové časti `04` a `05` |
| P06 | `04-merge-a-publikovanie.md`, `05-rozhranie.md`, `03a-novinky.md`, `03b-menu.md`, `07-overenie.md`, obsluha typov v `08` |
| P07 | plánovanie v `04` a `05`, čas Noviniek v `03a`, relevantné prípady `07` |
| P08 | `07-overenie.md`, všetky výsledky v `STAV.md`, skutočné implementačné zmeny |

Názvy `02`, `03`, `04`, `05`, `07` v tabuľke sú skratky dokumentov v tomto adresári. Výsledný kód je po implementácii zdrojom pravdy o existujúcich API; tieto dokumenty zachovajú zámery, hranice a dôvody rozhodnutí.

`08` znamená `08-rozsirovanie-modulov.md`. Pri E01–E03 načítaj tento dokument, skutočné API dokončeného jadra a konkrétne zdroje modulu. Stav nového kroku eviduj samostatne v `STAV.md`; označenie DONE P08 nepotvrdzuje podporu ďalšieho modulu.
