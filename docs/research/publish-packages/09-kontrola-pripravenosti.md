# Kontrola pripravenosti návrhu

Dátum: 2. november 2026. Kontrola zahŕňala všetkých 16 pôvodných Markdown súborov v tomto adresári, ich vzájomné zmluvy a rozhodujúce cesty existujúceho Java/UI kódu. Tento sedemnásty dokument zaznamenáva výsledok. Implementácia sa nezačala; nevznikli nové tabuľky ani Java/JavaScript triedy.

**Záver: návrh je pripravený na zadanie P01.** Nie je to potvrdenie, že už prešli databázové alebo browser testy budúcej funkcie. P01 je vedomá brána pred väčším refaktoringom: overí stabilné ID, výsledný SQL zdroj a skutočný náhľad, potom spresní odhad. Bez jeho doloženého výsledku sa P02 nezačne ako hotové, bezrizikové pokračovanie.

## Uzatvorené nejasnosti

Používateľ počas kontroly potvrdil všetky štyri doplnenia; autor ich nevydáva za pôvodné predpoklady:

| Rozhodnutie | Výsledok pre realizáciu |
| --- | --- |
| Q16 — Čo vidím po voľbe produkcie pri konflikte? | Celý náhľad rešpektuje zvolený výsledok vrátane menu, URL, Noviniek, médií a súborov. Editor uchová WORK. Nevyriešený konflikt je označený; zastarané uložené rozhodnutie vyžiada nové porovnanie. |
| Q17 — Súborové adresáre | Podporuje sa vytvorenie prázdneho adresára a operácie nad jednotlivými súbormi. Presun/premenovanie/mazanie a rekurzívne kopírovanie celých súborových adresárov sa odkladá. Nevzťahuje sa to na strom stránok. |
| Q18 — Súbory galérie | Do neskoršieho galerijného rozšírenia sú iba na čítanie, aj pri priamom uploade. |
| Q19 — Hostiteľ náhľadu | Existujúci hostiteľ a prihlásenie administrácie; logická verejná doména pochádza z overenej vetvy. |

Jediný register rozhodnutí zostáva [01](01-rozhodnutia.md). Po týchto odpovediach nezostala otvorená ďalšia produktová otázka potrebná na začatie P01. Budúce moduly E01–E03 zostávajú samostatným rozsahom; táto kontrola ich nepovoľuje implementovať.

## Opravené technické medzery

| Zistenie | Spresnenie návrhu a miesto |
| --- | --- |
| Voľba LIVE pri merge nemenila pracovný náhľad. | Spoločná request mapa B/L/W, `resolved_choice`, výber pred SQL filtrami/count/paging. L=W aj rovná ručná WORK voľba sú LIVE/no-op. [02](02-datovy-model.md), [03](03-nahlad-a-java.md), [03a](03a-novinky.md), [04](04-merge-a-publikovanie.md). |
| RequestBean a session mohli určovať nesprávnu doménu; branchId=0 nerozlíši dve LIVE domény. | Kontext v request atribútoch aj RequestBean, logická doména pred routovaním, dvojica branch+domain v admin požiadavkách, autentifikácia na admin hostiteľovi. [03](03-nahlad-a-java.md), [čítacia analýza](analyza-citanie-a-nahlad.md). |
| Nové PDF/obrázky mohol core odfiltrovať alebo poslať na produkčný thumbnail server. | Explicitne zapojené MediaDB availability, perex obrázkové helpery a lokálny thumbnail v preview; no-store chránené až do odoslania odpovede. [03](03-nahlad-a-java.md). |
| Nové médium starej stránky mohlo zostať skryté aj po merge. | WORK UPSERT odvodí technický last_update od výsledného dátumu vlastníka; LIVE/no-op zachová marker a existujúci cutoff. Dátum/autora stránky nemení. [02](02-datovy-model.md), [súborová analýza](analyza-media-a-subory.md). |
| Pracovná kópia mohla zmraziť údaje, ktoré merge neprepisuje. | Read-only a prevádzkové hodnoty existujúceho objektu sledujú LIVE; rovnaká klasifikácia v detaile, SQL a writeri. P02 odovzdá konkrétnu maticu stĺpcov. [02](02-datovy-model.md), [03a](03a-novinky.md). |
| Mazanie podstromu by sploštilo kôš alebo stratilo údaje obnovy. | Presunie sa iba koreň, potomkovia zachovajú väzby a ID. Minimum predchádzajúceho rodiča/dostupnosti sa uchová pre existujúcu obnovu, s checkpointmi. Kontrola kôš nevytvára. [04](04-merge-a-publikovanie.md), [zapisovacia analýza](analyza-zapis-a-publikovanie.md). |
| Súbor balíka mohol kolidovať s individuálnym plánom cudzej stránky. | Kontrola všetkých relevantných plánovaných fyzických cieľov aj mimo dotknutých docId, so zohľadnením spoločného/oddeleného úložiska. [04](04-merge-a-publikovanie.md). |
| Samotná file volume neizoluje všetky elFinder vedľajšie zápisy. | Inventár rename/paste/remove/property hookov; guard pred redirect, galériou, file_atr a dirprop. DIRECTORY marker je v rovnakej tabuľke, ďalšia nevzniká. [02](02-datovy-model.md), [súborová analýza](analyza-media-a-subory.md). |
| Normalizácia mohla spustiť živé JSP alebo customer hook. | Čisté odvodenie textu sa oddeľuje od fulltextExecuteApps/loopback/custom hookov a od následného indexovania. [04](04-merge-a-publikovanie.md). |
| Pád mohol nechať prevzatú vetvu s neúplným plánom. | Ciele/hash/checkpointy a podmienené prevzatie MERGING sa uložia v jednej krátkej DB transakcii; samotné publikovanie zostáva postupné. [04](04-merge-a-publikovanie.md). |
| Niektoré UI akcie a hranice etáp nemali presný kontrakt. | Doplnený detail, metadáta, zahodenie položky/celého balíka a práva. P03 vlastní čítacie adaptéry, P05 zápisy; dokončenie P01 nepredstiera celé T01. [04](04-merge-a-publikovanie.md), [05](05-rozhranie.md), [06](06-plan-implementacie.md). |

## Čo musí dokázať implementácia

- P01: rezervácia a explicitný insert na jednom pomenovanom reálnom POC profile podľa N5, nepreniknutie technického riadka, SQL projekcia vrátane CLOB, count/paging Noviniek a náhľad na reprezentatívnej šablóne/admin hoste. Potrebné údaje obnovy koša preverí v zdrojoch; reálne zmazanie/obnovu aj pri vypnutej histórii overí P06. Ďalšie DB/driver profily sa overia pred deklarovaním ich podpory.
- P02: konkrétne migrácie, matica stĺpcov, hodnoty nových objektov, kanonické hashe, kontrola enginov a spoločné rozhodovanie pre preview/publisher. Názvy navrhovaných tried nie sú dôkaz fungovania.
- P03–P07: príslušné čítacie, zapisovacie a publikačné scenáre vrátane doplnení v [07](07-overenie.md). Chýbajúci DB profil alebo browser skúška sa eviduje ako neoverená podmienka, nie úspech.
- P08: reálna kampaň, regresia produkčných tokov, inventár podporovaných šablón/komponentov a nasadzovací postup. Zapnutie na produkcii potrebuje osobitné zadanie pre konkrétne nasadenie.

Návrhový odhad 57–96 vývojárskych dní zostáva orientačným odhadom celého rozsahu vrátane širšieho overenia, nie cenou prvého POC. Audit odstránil rozpory, nezmeral výkon ani skutočný čas realizácie. P01 má jeho predpoklady aktualizovať a oddeliť POC od overovania ďalších nasadení.

## Rozsah kontroly dokumentov

Skontrolované: `README`, `01`, `02`, `02a`, `03`, `03a`, `04`, `05`, `06`, `07`, `08`, `ORCHESTRACIA`, `STAV` a všetky tri `analyza-*.md`. Budúca JSON tabuľka modulov zostáva jedna spoločná; typované jadro sa kvôli tejto kontrole neprestavuje. Úzky bannerový rozsah sa nerozšíril o riešenie štatistík.

Záverečné statické overenie kontroluje lokálne odkazy a kotvy, párovanie blokov kódu, whitespace a rozsah Git diffu. Výsledok je evidovaný v [STAV.md](STAV.md). Nejde o runtime test budúceho CMS ani o potvrdenie univerzálnej kompatibility zákazníckeho kódu.

## Doplnenie po audite: menu, 3. november 2026

Požiadavka N4 spresňuje povinný tok `menu_ul_li.jsp` → `MenuULLI`. Nový [03b](03b-menu.md) vychádza z kontroly reálneho JSP, triedy, `MenuApp` a existujúcich menu/sitemap testov. V P03 sa overuje výsledné menu, jeho aktívna cesta, default odkazy, request premenné a plný obsah; všeobecný test `DocDB` nestačí. Etapy, orchestrácia a T03 odkazujú na M01–M11. Produktová otázka tým nepribudla; navigácia bola súčasťou jadra už podľa Q6. Adresár teraz obsahuje 18 MD súborov. Runtime overenie zostáva úlohou implementácie.

## Doplnenie po audite: priorita POC, 3. november 2026

N5 zdôrazňuje jednoduchý čitateľný kód a bežnú prácu malého redakčného tímu. Záväzné [pravidlá orchestrácie](ORCHESTRACIA.md#záväzná-priorita-jednoduchý-a-čitateľný-poc) majú prednosť pred pôvodnou požiadavkou všetkých DB profilov už v P01 a prípravným registrom handlerov. Návrh teraz začína jedným skutočným profilom a konkrétnymi službami. Obnova koša sa runtime overí až s writerom v P06. Použiteľný ručný POC po P06 predchádza plánovaniu P07 a záverečnému overeniu P08; potvrdené funkcie, izolácia, práva, stabilné ID a základné pokračovanie po chybe zostávajú zachované.
