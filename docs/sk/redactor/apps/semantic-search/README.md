# Sémantické vyhľadávanie (RAG)

Sémantické vyhľadávanie umožňuje návštevníkom nájsť relevantné stránky na základe **významu otázky**, nielen zhody kľúčových slov. Vektory ukladá do PostgreSQL s [pgvector](https://github.com/pgvector/pgvector) alebo do vstavaného vektorového úložiska MariaDB 11.8 a novšej. Vektory generuje nastavený AI poskytovateľ.

Voliteľne môže nad rovnakým indexom zobraziť aj **RAG odpoveď** - krátku odpoveď vygenerovanú AI iba z nájdeného obsahu webu. Odpoveď sa zobrazí nad klasickým zoznamom výsledkov vyhľadávania.

![](rag-result.png)

## Čo návštevník uvidí

- Pri sémantickom vyhľadávaní sa zobrazí zoznam relevantných stránok zoradený podľa podobnosti významu otázky.
- Pri hybridnom vyhľadávaní sa kombinuje sémantické poradie s fulltextovou zhodou v indexovaných textoch.
- Pri povolenej RAG odpovedi sa nad výsledkami zobrazí blok **Odpoveď AI z vyhľadávania**.
- Ak odpoveď nie je možné zostaviť z indexovaného obsahu, nezobrazí sa žiadna RAG odpoveď.

## Podporované databázy a verzie

Na ukladanie a vyhľadávanie vektorov stačí jedna z nasledujúcich databáz:

| Databáza | Minimálna požiadavka | Odporúčanie a výhody |
| --- | --- | --- |
| **PostgreSQL + pgvector** | **PostgreSQL 16+** a kompatibilné rozšírenie **pgvector s HNSW** (od **0.5.0**). | Pre nové nasadenia odporúčame **PostgreSQL 18** s aktuálnou opravnou verziou **pgvector 0.8.x** (pri príprave dokumentácie **0.8.6**). WebJET podporuje metriky `cosine`, `l2` aj `inner_product`. Vhodné aj pri existujúcej PostgreSQL databáze. |
| **MariaDB Vector** | **MariaDB 11.8 LTS alebo novšia**. | V rade 11.8 odporúčame **11.8.9 alebo novšiu opravnú verziu**. Vektorové úložisko je vstavané, nevyžaduje rozšírenie. Pri existujúcej podporovanej MariaDB môžete použiť rovnaký server aj pre sémantické vyhľadávanie. Podporované metriky sú `cosine` a `l2`. |

**MariaDB 12.3 LTS** je voliteľná voľba pre vyšší výkon. Obsahuje optimalizáciu z verzie 12.1, ktorá automaticky zrýchľuje vyhľadávanie pri vhodných embeddingoch, napríklad Matryoshka. Nevyžaduje zmenu dotazov ani schémy; základná funkčnosť zostáva dostupná aj na 11.8. Konkrétny prínos závisí od dát a modelu. Podrobnosti a zdroje sú v [technickom porovnaní verzií](../../../custom-apps/apps/rag/semantic-search/README.md#podporované-databázy-a-verzie).

Ak primárna databáza WebJET CMS tieto požiadavky nespĺňa, napríklad používate staršiu MariaDB, MySQL, Microsoft SQL Server alebo Oracle, správca môže pripojiť samostatnú podporovanú PostgreSQL alebo MariaDB databázu cez datasource `rag_jpa`. Primárnu databázu CMS preto nie je potrebné meniť.

## Nastavenie sémantického vyhľadávania

Na spustenie sémantického vyhľadávania je potrebné:

- Pripraviť jednu z [podporovaných vektorových databáz](#podporované-databázy-a-verzie).
- Povoliť sémantické vyhľadávanie nastavením konfiguračnej premennej `ragSemanticSearchEnabled` na hodnotu `true`.
- Nastaviť typ vyhľadávania na hodnotu `semantic` alebo `hybrid`. Môžete to urobiť globálne cez konfiguračnú premennú `searchType`, alebo priamo v aplikácii **Vyhľadávanie**.
- Pri hybridnom režime overiť, že konfiguračná premenná `ragHybridSearchEnabled` je nastavená na hodnotu `true`.
- Overiť, že konfiguračná premenná `luceneAsDefaultSearch` je nastavená na hodnotu `false`. Ak je nastavená na `true`, bude sa namiesto sémantického vyhľadávania používať Lucene, pretože má vyššiu prioritu.
- Nakonfigurovať zvoleného poskytovateľa: pre externú službu nastaviť API kľúč rovnakým spôsobom ako pre AI asistentov alebo nakonfigurovať [lokálny embeddingový model](../../ai/settings/README.md#lokálne-modely), ktorý nevyžaduje API kľúč ani odosielanie obsahu externej AI službe.
- Spustiť indexovanie cez administrátorské rozhranie na vytvorenie vektorov a naplnenie vektorovej databázy.
- Nastaviť automatizovanú úlohu `sk.iway.iwcm.rag.service.RagIndexCronTask`, ktorá spracúva frontu indexovania.

Vektorová databáza sa zvolí automaticky. Ak je nastavený samostatný datasource `rag_jpa`, má prednosť; inak sa použije primárny datasource `iwcm`. Postup prípravy databázy a pripojenia je uvedený v [technickej dokumentácii](../../../custom-apps/apps/rag/semantic-search/README.md#požiadavky).

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

### Karta Sémantické nastavenia

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
- **Použiť `ILIKE` fallback pre fulltext** - použije `ILIKE` v PostgreSQL alebo `LIKE` bez rozlišovania veľkosti písmen v MariaDB, ak databázový fulltext nič nenájde.

### Karta RAG nastavenia

RAG odpoveď sa generuje až po vyhľadaní relevantných častí obsahu. Nastaviť možno:

- **AI asistent pre RAG odpoveď** - predvolený asistent sa vytvorí automaticky. Voliteľne možno vybrať vlastného asistenta určeného pre RAG odpovede.
- **Minimálna podobnosť** - mäkký prah pre časti textu použité pri tvorbe odpovede.
- **Použiť top K blokov** - koľko najlepších častí textu vstupuje do post-processingu.
- **Maximálna medzera medzi blokmi** - určuje, či sa susedné časti jednej stránky zlúčia do spoločného kontextu.
- **Maximálny počet blokov** a **maximálny počet znakov** - obmedzujú veľkosť kontextu odoslaného AI modelu.
- **Maximálny počet znakov bloku** - zabraňuje vytvoreniu príliš veľkého zlúčeného kontextového bloku.

RAG odpoveď používa iba obsah získaný zo sémantického indexu. Ak sa v kontexte nenachádza odpoveď, systém zobrazí fallback text namiesto vymýšľania odpovede.

## Sémantický index

Na využitie sémantického vyhľadávania je potrebné mať indexovaný obsah pomocou sémantického indexovania, ktoré je dostupné v administrátorskom rozhraní. Poskytovateľ a model sa nastavujú v systémových AI asistentoch `RAG-EMB-INDEX` a `RAG-EMB-SEARCH`; aby sa index použil, obe hodnoty vyhľadávacieho asistenta sa musia presne zhodovať s vytvoreným indexom. Po otvorení stránky **Sémantický index** sa aktuálna indexovacia konfigurácia zobrazí v informačnom oznámení. Indexy rôznych poskytovateľov a modelov môžu existovať súčasne. Viac informácií nájdete v časti [Sémantický index](./embedding-chunks.md).

Pri použití poskytovateľa **Lokálny embeddingový model** nastavte cestu `ai_localEmbeddingModelBundlePath`, globálnu hodnotu `ragEmbeddingDimensions` zmeňte na `768` a v oboch systémových asistentoch vyberte rovnaký lokálny model. Zmena dimenzie vymaže existujúci sémantický index, preto následne znova spustite úplné indexovanie.

## Detaily implementácie a nastavenia

Technický popis procesu indexovania, vyhľadávania, hybridného režimu a RAG odpovede nájdete v [dokumentácii pre vývojárov](../../../custom-apps/apps/rag/semantic-search/README.md).
