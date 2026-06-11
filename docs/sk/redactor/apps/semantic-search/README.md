# Sémantické vyhľadávanie (RAG)

Sémantické vyhľadávanie umožňuje návštevníkom nájsť relevantné stránky na základe **významu otázky**, nielen zhody kľúčových slov. Využíva vektorovú databázu [pgvector](https://github.com/pgvector/pgvector) a vektory generované prostredníctvom OpenAI API.

Voliteľne môže nad rovnakým indexom zobraziť aj **RAG odpoveď** - krátku odpoveď vygenerovanú AI iba z nájdeného obsahu webu. Odpoveď sa zobrazí nad klasickým zoznamom výsledkov vyhľadávania.

## Čo návštevník uvidí

- Pri sémantickom vyhľadávaní sa zobrazí zoznam relevantných stránok zoradený podľa podobnosti významu otázky.
- Pri hybridnom vyhľadávaní sa kombinuje sémantické poradie s fulltextovou zhodou v indexovaných textoch.
- Pri povolenej RAG odpovedi sa nad výsledkami zobrazí blok **Odpoveď AI z vyhľadávania**.
- Ak odpoveď nie je možné zostaviť z indexovaného obsahu, zobrazí sa text **Nemám dostatok informácií na zodpovedanie tejto otázky.**

## Nastavenie sémantického vyhľadávania

Na spustenie sémantického vyhľadávania je potrebné:

- Povoliť sémantické vyhľadávanie nastavením konfiguračnej premennej `ragSemanticSearchEnabled` na hodnotu `true`.
- Nastaviť typ vyhľadávania na hodnotu `semantic` alebo `hybrid`. Môžete to urobiť globálne cez konfiguračnú premennú `searchType`, alebo priamo v aplikácii **Vyhľadávanie**.
- Pri hybridnom režime overiť, že konfiguračná premenná `ragHybridSearchEnabled` je nastavená na hodnotu `true`.
- Overiť, že konfiguračná premenná `luceneAsDefaultSearch` je nastavená na hodnotu `false`. Ak je nastavená na `true`, bude sa namiesto sémantického vyhľadávania používať Lucene, pretože má vyššiu prioritu.
- Nastaviť OpenAI API kľúč používaný aj pre AI asistentov v premennej `ai_openAiAuthKey`.
- Spustiť indexovanie cez administrátorské rozhranie na vytvorenie vektorov a naplnenie vektorovej databázy.
- Nastaviť automatizovanú úlohu `sk.iway.iwcm.rag.service.RagIndexCronTask`, ktorá spracúva frontu indexovania.

!>**Upozornenie:** Po nasadení zmien odporúčame spustiť opätovné indexovanie stránok. Index teraz ukladá aj informácie o priečinku stránky (`group_id`, `root_group_l1`, `root_group_l2`, `root_group_l3`), ktoré sa používajú pri filtrovaní výsledkov podľa priečinkov zvolených v aplikácii **Vyhľadávanie**.

## Nastavenia aplikácie Vyhľadávanie

Sémantické vyhľadávanie sa používa cez aplikáciu **Vyhľadávanie** vloženú do stránky. Aplikácia má okrem bežných polí aj nastavenia pre sémantické, hybridné a RAG vyhľadávanie.

### Karta Základné

- **Typ vyhľadávania** - určuje, aký mechanizmus použije konkrétna vložená aplikácia:
  - **Podľa konfigurácie** - použije globálnu hodnotu `searchType`.
  - **Databázové** - použije štandardné databázové vyhľadávanie.
  - **Fulltextové/Lucene** - použije Lucene.
  - **Sémantické** - použije vektorové vyhľadávanie bez hybridnej fulltextovej vetvy.
  - **Hybridné** - použije vektorové vyhľadávanie spolu s fulltextom nad indexovanými časťami textu.
- **Pridať odpoveď RAG** - zapne alebo vypne AI odpoveď pre konkrétnu aplikáciu. Hodnota **Podľa konfigurácie** rešpektuje globálne nastavenie `ragAnswerAllowed`.
- **Adresár** - obmedzí vyhľadávanie na vybrané priečinky. Pri sémantickom a hybridnom vyhľadávaní sa obmedzenie aplikuje priamo nad sémantickým indexom.

Ak je niektorá možnosť označená ako **aktuálne nepovolené**, najskôr povoľte príslušnú globálnu konfiguračnú premennú.

### Karta Semantické nastavenia

Umožňuje prepísať globálne hodnoty pre konkrétnu vloženú aplikáciu:

- **Minimálna podobnosť** - lokálna hodnota pre `ragSemanticSearchMinSimilarity`.
- **Minimálny počet výsledkov** - lokálna hodnota pre `ragSemanticSearchMinResults`.

Prázdna hodnota znamená, že sa použije globálna konfigurácia.

### Karta Hybridné nastavenia

Umožňuje nastaviť, kedy sa má k vektorovému vyhľadávaniu pridať fulltextová vetva:

- **Režim hybridného vyhľadávania** - `off`, `always`, `short_query_only`, `fallback_on_low_vector` alebo **Podľa konfigurácie**.
- **Maximálny počet znakov/slov pre krátky dopyt** - hranice pre režim `short_query_only`.
- **Prahová podobnosť pre fallback** - hranica pre režim `fallback_on_low_vector`.
- **Váhy vektorovej a fulltextovej vetvy** - určujú výsledné poradie pri kombinovaní cez RRF.
- **Koeficient načítania blokov** - koľko textových častí sa načíta pred agregáciou na dokumenty.
- **Použiť ILIKE fallback pre fulltext** - použije jednoduché textové hľadanie, ak PostgreSQL fulltext nič nenájde.

### Karta Nastavenia RAG

RAG odpoveď sa generuje až po vyhľadaní relevantných častí obsahu. Nastaviť možno:

- **AI asistent pre RAG odpoveď** - predvolený asistent sa vytvorí automaticky. Voliteľne možno vybrať vlastného asistenta určeného pre RAG odpovede.
- **Minimálna podobnosť** - mäkký prah pre časti textu použité pri tvorbe odpovede.
- **Použiť top K blokov** - koľko najlepších častí textu vstupuje do post-processingu.
- **Maximálna medzera medzi blokmi** - určuje, či sa susedné časti jednej stránky zlúčia do spoločného kontextu.
- **Maximálny počet blokov** a **maximálny počet znakov** - obmedzujú veľkosť kontextu odoslaného AI modelu.
- **Maximálny počet znakov bloku** - zabraňuje vytvoreniu príliš veľkého zlúčeného kontextového bloku.

RAG odpoveď používa iba obsah získaný zo sémantického indexu. Ak sa v kontexte nenachádza odpoveď, systém zobrazí fallback text namiesto vymýšľania odpovede.

## Sémantický index

Na využitie sémantického vyhľadávania je potrebné mať indexovaný obsah pomocou sémantického indexovania, ktoré je dostupné v administrátorskom rozhraní. Viac informácií nájdete v časti [Sémantický index](./embedding-chunks.md).

## Detaily implementácie a nastavenia

Technický popis procesu indexovania, vyhľadávania, hybridného režimu a RAG odpovede nájdete v [dokumentácii pre vývojárov](../../../custom-apps/apps/rag/semantic-search/README.md).
