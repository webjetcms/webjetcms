# Riadenie realizácie pracovných vetiev

Tento dokument je vstupný bod pre agenta realizujúceho dohodnutý návrh. Aktuálne ide **iba o dokumentáciu**; implementáciu spustí až neskorší výslovný pokyn používateľa. Pripravenosť a dokončené kroky sú v [STAV.md](STAV.md).

## Záväzná priorita: jednoduchý a čitateľný POC

Podľa požiadavky N5 z [rozhodnutí](01-rozhodnutia.md#jednoduchosť-a-poc-3-november-2026) teraz overujeme, či pracovná vetva funguje v bežnej redakčnej praxi s približne 3–5 spolupracujúcimi redaktormi. **Zvoľ najjednoduchší čitateľný kód, ktorý splní zadaný krok.** Technický návrh popisuje potrebné správanie a miesta zásahov; nie je pokynom vybudovať všetky naznačené vrstvy vopred.

- Použi konkrétne služby, bežné metódy a explicitné mapovanie. Nevytváraj ku každej triede interface + implementáciu, generické repository, hierarchiu stratégií či factory bez skutočnej potreby v aktuálnom kroku. Krátke opakovanie je prijateľnejšie než abstrakcia, ktorá sťažuje čítanie.
- Spoločné rozhranie vyčleň, keď ho odôvodňujú existujúci volajúci alebo dohodnuté integračné API. Pre štyri typy jadra stačí enum a priame volania služieb; `BranchTypeHandler` a Spring register nie sú povinnou prípravou na budúce moduly. Podrobnosti v [08](08-rozsirovanie-modulov.md#čo-pripraviť-už-v-p01p08).
- Nerieš vopred hypotetické zriedkavé súbehy, automatický failover, distribuované zámky, všeobecný workflow, univerzálny SQL adaptér ani optimalizácie bez merania. Nepodporovanú situáciu radšej zrozumiteľne odmietni a zapíš obmedzenie. Nepridávaj retry/recovery framework iba pre prípad, že by raz mohol byť potrebný.
- Zachovaj izoláciu produkcie, stabilné ID, existujúce oprávnenia, detekciu bežných konfliktov a pravdivý náhľad vrátane Noviniek a menu. Potrebné sú aj základná evidencia vykonaných položiek a zastavenie pri chybe. Krátke bežné DB transakcie pre rezerváciu ID a zápis položky zostávajú; globálna transakcia celého balíka sa nepridáva.
- Najskôr over funkčný tok na jednej inštancii, jednom skutočnom DB profile a reprezentatívnej šablóne. Vybraný profil a obmedzenia zaznamenaj v `STAV.md`; ďalšie DB/topológie over pred deklarovaním alebo zapnutím ich podpory. Nepotrebujú blokovať prvé overenie POC. Nesplnená podmienka zvoleného profilu však zostáva prekážkou.
- Testuj správanie redaktora a vyššie uvedené vlastnosti. Použi existujúce testovacie zázemie; nevytváraj vlastný framework, záťažové prostredie pre stovky redaktorov ani kombinácie málo pravdepodobných porúch. Konkrétne overenia zadaného kroku zostávajú platné v rozsahu zvoleného profilu.

Pred odovzdaním odstráň nepoužité prípravné vrstvy a uveď zostávajúce obmedzenia. Ekvivalentné zjednodušenie nevyžaduje nové povolenie; zjednodušením nesmie zaniknúť potvrdená funkcia. Táto priorita platí pre všetky kroky a budúce moduly, aj pri čítaní starších technických odporúčaní v tomto adresári.

## Modely a spôsob riadenia

**Odporúčaný začiatok: GPT-6.1 Sol, reasoning High, pre všetky P01–P08.** Jeden hlavný agent zároveň implementuje, riadi pomocníkov a kontroluje výsledok. Tento Markdown je jeho pracovný návod, nie spustiteľný orchestrátor ani proces, ktorý sám rozbehne všetkých osem etáp. Netreba pridávať Agents SDK, API klienta ani vlastný plánovač agentov do WebJETu.

Odporúčanie je určené pre tento projekt a bolo overené voči OpenAI Docs 3. novembra 2026. Oficiálna dokumentácia odporúča Sol pre náročnú prácu s kódom a rozlišuje model od úrovne uvažovania. High tu volíme pre zásahy do existujúcich čítacích a zapisovacích ciest; nie je to požiadavka na zložitejšiu implementáciu. [Modely a reasoning v Codexe](https://learn.chatgpt.com/docs/models).

| Práca / rola | Odporúčaný model a reasoning | Použitie |
| --- | --- | --- |
| Hlavný agent, implementácia P01–P08 | GPT-6.1 Sol (`gpt-6.1-sol`), High | Predvolená voľba pre celý postup; medzi etapami netreba meniť model. |
| Pomocník na kód, SQL, preview alebo testy | Rovnaký model a reasoning ako hlavný agent | Predvolený režim delegovania, ak má úloha jasné hranice a samostatné súbory/dáta. |
| Nezávislá kontrola ID, izolácie a merge | GPT-6.1 Sol, High | Samostatný kontext kontrolóra; iný model nie je podmienkou nezávislej kontroly. |
| Voliteľný úsporný pomocník | GPT-6 Luna (`gpt-6-luna`), High | Iba úzky inventár súborov, sumarizácia výsledkov alebo jednoduchá dokumentácia. O správnosti merge/ID rozhoduje hlavný agent. |
| Voliteľná konzultácia zložitého problému | GPT-6 Astra (`gpt-6-astra`), Medium | Konkrétny nevyriešený problém P01/P02/P03/P06; nie povinná druhá implementácia každej etapy. |

Rozdelenie úloh v tabuľke je odporúčanie autora návrhu. Sol je podľa OpenAI určený aj na komplexné programovanie, Astra na najnáročnejšiu prácu a Luna na úzko vymedzené opakované úlohy. Dostupnosť závisí od účtu a klienta. [Katalóg modelov](https://developers.openai.com/api/docs/models), [nastavenie modelov subagentov](https://learn.chatgpt.com/docs/agent-configuration/subagents).

Hlavný model a reasoning si vyber v ovládaní modelu pri poli správy; samotné meno modelu v tomto MD ho neprepne. V CLI na to slúži `/model`. Pre prvý beh stačí High; Max/Ultra nie sú podmienkou realizácie. Ultra môže využívať subagentov, ale delegovanie možno vyžiadať aj samostatným pokynom. [Ovládanie modelov](https://learn.chatgpt.com/docs/models), [delegovanie v Codexe](https://learn.chatgpt.com/docs/agent-configuration/subagents).

Predvolený režim tohto plánu používa pre pomocníkov rovnaký model ako hlavný agent. Voliteľnú Lunu/Astru použi až po výslovnom výbere používateľa; netreba kvôli nim vytvárať `.codex/agents/` ani meniť globálnu konfiguráciu. Pri delegovaní rešpektuj skutočné možnosti klienta: jeho konfigurácia môže model pomocníka ovplyvniť. Ak požadovaný model alebo reasoning nemožno zvoliť, uveď dostupnú alternatívu; nevydávaj požiadavku v prompte za potvrdené nastavenie. Do výsledku zaznamenaj známe použité modely, pri neznámom nastavení napíš „neoverené“.

## Ako prácu spustiť

1. Otvor existujúci worktree `webjetcms-feature-58858-publishing-packages` na vetve `feature/58858-publishing-packages`. Pokračovať možno v tomto chate; nový chat má používať ten istý worktree a rozpracované súbory.
2. Vyber GPT-6.1 Sol / High, ak je v ponuke. Použi bežný režim, ktorý vie upravovať súbory a spúšťať overenia; samotný plánovací režim implementáciu nevykoná.
3. Maj dostupné lokálne/testovacie WebJET prostredie. Agent podľa [pracovného návodu](../../../.github/agents/CONTRIBUTING.md) najskôr zistí konfiguráciu, testový admin host, šablónu a jeden reálny DB profil. P01 používa jednorazovú testovaciu DB; ak potrebné prostredie chýba, agent má uviesť konkrétne čo chýba a dokončiť nezávislú prácu. Prihlasovacie údaje patria do lokálnej konfigurácie, nie do MD alebo promptu.
4. Zadaj iba P01. Po odovzdaní posúď výsledok, až potom zadaj P02. Ďalej pokračuj po jednej etape podľa tabuľky nižšie.

Štartovací prompt pre tento checkout:

```text
Pracuj v existujúcom worktree:
/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms-feature-58858-publishing-packages

Implementuj iba krok P01 návrhu docs/research/publish-packages.
Riaď sa docs/research/publish-packages/ORCHESTRACIA.md.
Buď hlavný implementátor a koordinátor. Na nezávislú prácu môžeš
použiť najviac dvoch pomocných agentov naraz s rovnakým modelom;
ich výsledky skontroluj a integruj. Ak delegovanie nie je dostupné,
vykonaj krok samostatne.
Dodrž prioritu jednoduchého POC; over jeden skutočný DB profil a zapíš ktorý.
Vychádzaj z uzatvorených rozhodnutí, výsledok zapíš do STAV.md.
Zachovaj cudzie lokálne zmeny a nevytváraj Git commity.
Bez produkčného nasadenia a bez začatia P02.
Po P01 skonči s výsledkami overenia a aktualizovaným odhadom.
```

Pokyn na implementáciu konkrétneho kroku zároveň oprávňuje vykonať jeho bežné úpravy a overenia. Agent nemá pýtať ďalšie potvrdenie iba preto, že tento adresár pôvodne vznikol v návrhovej fáze.

Po dokončení a posúdení výsledkov P01 sa pokračuje napríklad:

```text
Implementuj krok P02 podľa docs/research/publish-packages/ORCHESTRACIA.md.
Pokračuj v rovnakom worktree, na rovnakom POC profile
a s rovnakým režimom modelov a delegovania.
Skontroluj v STAV.md dokončenie predpokladov zadaného kroku
podľa plánu 06 a nadviaž na existujúci kód.
Dokonči rozsah a overenia P02, aktualizuj STAV.md a skonči.
Bez Git commitov a bez nasadenia na produkciu.
```

P02 sa postupne nahradí P03 až P08. Nepotrebuješ kopírovať celý návrh do promptu. Ak chceš dlhší súvislý beh, rozsah povedz výslovne, napr. „realizuj P03–P05“. Bez takéhoto zadania agent po určenom kroku nepokračuje do ďalšieho.

### Čo očakávať po jednotlivých etapách

| Etapa | Čo si overiť v odovzdaní |
| --- | --- |
| P01 | Dôkaz ID, SQL a reálneho náhľadu na zvolenom profile; výsledky overenia a spresnený odhad. Ešte nejde o hotovú editáciu. |
| P02 | Úložisko, kontext a konečné ID; pri vypnutej funkcii funguje dnešná administrácia. |
| P03 | Preklikateľný náhľad, menu, Novinky, obrázky a médiá. Dáta ešte môžu pripravovať testovacie fixture. |
| P04 | Vytvorenie a úpravy stránok/priečinkov vo vetve bez zmeny produkčného obsahu. |
| P05 | Pracovný upload, médiá a súbory; verejné URL stále vracajú produkčné súbory. |
| P06 | Konflikty, voľba LIVE/WORK a ručný merge so zachovanými ID. **Prvý použiteľný ručný POC.** |
| P07 | Spoločný termín, posun/zrušenie a zastavenie pri novom probléme. |
| P08 | Regresia, konkrétne obmedzenia a návod na nasadenie. Produkčné zapnutie je osobitný pokyn. |

Po každej etape skontroluj zhrnutie a diff: čo funguje, aké testy prešli, čo zostáva neoverené a či tomu zodpovedá `STAV.md`. Od P03 má zmysel pozrieť aj náhľad; po P06 si prejsť celý ručný redakčný tok. Agent dodá konkrétne testovacie URL a kroky reprodukcie. Commity môžeš medzi etapami vytvoriť sám; nie sú podmienkou pokračovania agenta. Presné dokončovacie podmienky zostávajú v [pláne](06-plan-implementacie.md).

Pri `PARTIAL` dokonči tú istú etapu; ďalší závislý krok ešte nespúšťaj. Pri `BLOCKED` najskôr odstráň uvedenú prekážku. Chýbajúci iný DB profil neblokuje POC, chýbajúci dôkaz na zvolenom profile áno. Prerušenie chatu neznamená začať implementáciu odznova.

### Pokračovanie po prerušení alebo v novom chate

Vyber ten istý worktree a model. Nahraď P03 skutočným rozpracovaným krokom:

```text
Pracuj v existujúcom worktree:
/Users/jeeff/Documents.nosync/workspace-visualstudio/github.com_webjetcms/webjetcms-feature-58858-publishing-packages

Pokračuj v rozpracovanom P03 podľa docs/research/publish-packages/ORCHESTRACIA.md.
Prečítaj STAV.md, git diff a skutočný stav kódu.
Nadviaž na hotové časti a dokonči chýbajúcu prácu a overenia
na rovnakom POC profile. Zachovaj existujúce zmeny.
Použi predvolený režim modelov a delegovania z orchestrácie.
Aktualizuj STAV.md, odovzdaj výsledok a skonči.
Bez Git commitov, produkčného nasadenia a ďalšej etapy.
```

Pri novom chate na už dokončenej hranici použi prompt pre ďalšiu etapu vyššie a doplň túto cestu worktree. Dokumenty a výsledný kód prenášajú stav; netreba ručne kopírovať celú históriu rozhovoru.

### Budúce rozšírenia a chaty

Budúce moduly sa zadávajú osobitne podľa [08 — Rozširovanie modulov](08-rozsirovanie-modulov.md). P02/P06 pripravia spoločnú identitu a spracovanie dnešných typov, bez povinného rozhrania/DI registra; E01 vytvorí spoločné JSON úložisko až spolu s prvým modulom E02. E03 môže neskôr pridať galériu. Tieto kroky nie sú automatickým pokračovaním P08. V dokumente 08 je presný vzor zadania základného bannera bez úprav štatistík.

Nový chat nie je technickou podmienkou; dôležitý je rovnaký checkout a evidencia výsledkov. Ak používateľ chce nový chat vytvoriť, môže tak urobiť cez dostupné nástroje aplikácie. Orchestrátor sám nezakladá používateľské chaty iba kvôli internému rozdeleniu práce; na pomocné paralelné úlohy použije subagentov.

## Vstupný postup agenta

1. Prečítaj koreňové `AGENTS.md`, `.github/agents/AGENTS.md` a pracovný návod. Skontroluj `git status` a aktuálnu vetvu. Zachovaj existujúce nesúvisiace zmeny; nevytváraj commity.
2. Prečítaj tento dokument, `STAV.md`, `01-rozhodnutia.md` a **iba zadanú sekciu** v `06-plan-implementacie.md`.
3. Pre P01 načítaj aj [kontrolu pripravenosti](09-kontrola-pripravenosti.md); uzatvorené Q16–Q19 sa znova nepýtajú. Over dokončenie predpokladov kroku podľa evidencie aj skutočného kódu. Samotný štítok DONE bez výsledkov nie je dôkaz.
4. Načítaj návrhové dokumenty a zdrojové analýzy uvedené ako vstupy daného kroku. Presné implementačné správanie over proti aktuálnemu kódu; čísla riadkov v analýzach sú iba orientačné z času návrhu.
5. Zaznamenaj v `STAV.md` krok `IN_PROGRESS`, použitú vetvu, výchozí stav a stručný rozsah vrátane POC profilu (DB/verzia/driver, jedna inštancia, šablóna). Nadviaž na profil predchádzajúceho kroku. Pre implementáciu použi vhodnú existujúcu feature vetvu. Ak nie je, riaď sa repozitárovým pomenovaním a prideleným issue ID; číslo issue si nevymýšľaj.
6. Vykonaj zadaný rozsah, príslušné overenia a skontroluj celý vlastný diff. Žiadna produkčná aktivácia, databázová konverzia alebo deploy nie je implicitnou súčasťou lokálnej implementácie.

**Výstup vstupného postupu:** agent pozná konkrétny krok, jeho závislosti, povolené súbory/hranice a prípady z testovacej matice. Ak závislosť reálne chýba, uvedie konkrétny chýbajúci výsledok; neoznačí ju automaticky za splnenú.

## Rozdelenie medzi agentov

Jeden hlavný agent vlastní integráciu, zmluvy a záznam stavu; sám aj implementuje. Pre tento POC použi najviac **dvoch aktívnych pomocníkov naraz** v celom tíme, bežne stačí jeden. Krátku alebo závislú úlohu vykoná hlavný agent sám. Ak prostredie neposkytuje subagentov, celý krok možno dokončiť jedným agentom; nezavádza sa náhradný vlastný orchestrátor.

Modely určuje [tabuľka vyššie](#modely-a-spôsob-riadenia). Pomocní agenti môžu paralelne:

- overovať databázové profily alebo konkrétne čítacie cesty;
- implementovať samostatné súbory služieb/UI so zadaným kontraktom;
- kontrolovať hotový diff a nežiaduce produkčné vedľajšie účinky;
- vykonať nezávislé relevantné testy, ak sa nekrižujú testovacie dáta.

Pred delegovaním hlavný agent uvedie výstup, vlastníctvo súborov a závislosti. Dvaja agenti neupravujú súčasne `DocDB`, `GroupsDB`, editor pipeline, migrácie alebo spoločné kontrakty. P04/P05 možno rozdeliť po uzavretí zmlúv P02/P03. P06 nevzniká paralelne nad neurčeným pracovným formátom.

Prakticky: v P01 môže jeden pomocník preveriť rezerváciu ID a druhý čítacie cesty menu/Noviniek; hlavný agent spojí dôkazy. V implementačných etapách pridelí pomocníkovi oddelený súbor alebo kontrolu diffu. Kontrolór najskôr odovzdá zistenia bez úprav rovnakých súborov, v ktorých práve pracuje implementátor. Zapisovacie testy nad rovnakou DB/fixture sa spúšťajú sekvenčne. Na začiatok je jednoduchšie zadať P04 a P05 postupne.

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

1. Spĺňa všetky dokončovacie podmienky svojej sekcie plánu na uvedenom POC profile. `DONE` potvrdzuje tento rozsah, nie pripravenosť všetkých DB a nasadení. Ďalší profil sa nesmie aktivovať bez jeho príslušných kontrol.
2. Sú zapísané vykonané kontroly, prostredie a výsledky; nevykonaná kritická podmienka je viditeľná. `PARTIAL` je správnejší než nepravdivé DONE.
3. Je skontrolovaný vlastný diff aj interakcia s existujúcim produkčným tokom.
4. Nasledujúci agent má popísané zavedené API, migrácie/feature flag, relevantné súbory a nevyriešené záväzky.
5. Zmeny zostali necommitnuté a používateľ dostane stručné zhrnutie výsledku, testov a obmedzení.

Záznam dokončenia vlož do `STAV.md` pod príslušný krok, napríklad:

```text
P03 — DONE
Vetva / výchozí commit: ...
POC profil / rozsah overenia: DB, verzia, driver, inštancia, šablóna
Modely / reasoning: hlavný agent, pomocníci; neznáme nastavenie označiť
Výsledok: ...
Hlavné zmenené súbory a API: ...
Overenie: príkaz, DB/profil, výsledok, report
Neoverené podmienky: ...
Odložené produkčné overenia / nepodporované profily: ...
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
