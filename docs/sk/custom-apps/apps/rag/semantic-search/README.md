# Sémantické vyhľadávanie (RAG)

Sémantické vyhľadávanie umožňuje návštevníkom nájsť relevantné stránky na základe **významu otázky**, nielen zhody kľúčových slov. Využíva technológiu RAG (Retrieval-Augmented Generation) postavenú na vektorovej databáze [pgvector](https://github.com/pgvector/pgvector) a vektoroch generovaných cez OpenAI API.

## Ako to funguje

Systém pracuje v dvoch fázach:

### 1. Indexovanie (offline)

Keď je webová stránka uložená alebo zmenená, systém ju zaradí do fronty na indexovanie. Cron úloha ([RagIndexCronTask](../../../../../../src/main/java/sk/iway/iwcm/rag/service/RagIndexCronTask.java)) pravidelne spracúva frontu:

1. **Extrakcia obsahu** – z `DocDetails` sa extrahuje čistý text (názov + perex + telo stránky bez HTML tagov).
2. **Rozdelenie na časti (chunking)** – text sa rozdelí na prekrývajúce sa časti pomocou algoritmu posuvného okna (`SlidingWindowChunker`). Predvolená veľkosť časti je 500 znakov s prekryvom 100 znakov.
3. **Generovanie vektorov** – každá časť sa odošle do OpenAI API (`/v1/embeddings`) a vráti sa vektor s 1 536 dimenziami (model `text-embedding-3-small`).
4. **Uloženie do databázy** – vektory sa uložia do tabuľky `rag_embedding_chunks` v `PostgreSQL` databáze s rozšírením `pgvector`.

### 2. Vyhľadávanie (online)

Keď návštevník zadá vyhľadávací dotaz:

1. Dotaz sa prevedie na embedding vektor cez OpenAI API.
2. Vykoná sa vektorové vyhľadávanie v databáze (kosínusová podobnosť).
3. Výsledky sa agregujú podľa dokumentu – za každý dokument sa vyberie najlepší chunk.
4. Dokumenty sa vrátia zoradené podľa podobnosti a zobrazia sa rovnakým spôsobom ako výsledky štandardného vyhľadávania.

## Požiadavky

- **PostgreSQL** s rozšírením **pgvector** (obraz: `pgvector/pgvector:pg18-trixie` alebo novší)
- **OpenAI API kľúč** – ten istý, ktorý sa používa pre AI asistentov (`ai_openAiAuthKey`)
- Sémantické vyhľadávanie funguje **len s PostgreSQL**. Pre ostatné databázy (MySQL/MariaDB, MSSQL, Oracle) je potrebné nastaviť samostatnú vektorovú databázu cez datasource `rag_jpa`.

### PostgreSQL ako primárna databáza

Ak WebJET CMS beží priamo na PostgreSQL, vektorová databáza sa použije automaticky bez ďalšej konfigurácie.

Musí byť iba nastavený datasource ako v prípade `poolman-docker-pgsql.xml`.

### Samostatná vektorová databáza (vedľajšia)

Ak primárna databáza nie je PostgreSQL, vytvorte Docker kontajner s `pgvector`.

Pre lokálny vývoj je pripravený súbor `.devcontainer/db/docker-compose-rag-pgsql.yml`:

```bash
docker compose -f .devcontainer/db/docker-compose-rag-pgsql.yml up -d
```

už s nakonfigurovanými datasource:

- `poolman-docker-mariadb.xml`
- `poolman-docker-mssql.xml`
- `poolman-docker-oracle.xml`

## Konfigurácia

Aktivácia a nastavenie sémantického vyhľadávania v `Konfigurácia` (skupina `rag`):

| Premenná | Predvolená hodnota | Popis |
| --- | --- | --- |
| `ragSemanticSearchEnabled` | `false` | Zapne sémantické vyhľadávanie. Nastavte na `true` pre aktiváciu. |
| `ragEmbeddingModel` | `text-embedding-3-small` | Názov OpenAI embedding modelu |
| `ragEmbeddingDimensions` | `1536` | Počet dimenzií vektora. Musí zodpovedať použitému modelu a tabuľke v databáze. |
| `ragChunkSize` | `1000` | Maximálna veľkosť jednej časti textu v znakoch. |
| `ragChunkOverlap` | `200` | Počet znakov, o ktoré sa susedné časti prekrývajú. |
| `searchType` | `db` | Typ vyhľadávania: `db` (databázové), `lucene` (Lucene fulltext), `semantic` (pgvector sémantické). |

!> Pre aktiváciu sémantického vyhľadávania nastavte `ragSemanticSearchEnabled=true` **aj** `searchType=semantic`.

!>**Upozornenie:** pri zmene konfiguračnej premennej `ragEmbeddingDimensions` sa vymaže celá tabuľka `rag_embedding_chunks`, pretože vektory nebudú kompatibilné. Zvážte zálohu dát pred zmenou tejto hodnoty. Tabuľka sa automaticky znova vytvorí s novou dimenziou.

### Odporúčania pre slovenský a český obsah

Predvolené hodnoty (`text-embedding-3-small`, `chunkSize=1000`, `chunkOverlap=200`) sú vyvážený kompromis medzi cenou, rýchlosťou a presnosťou pre bežné web stránky v slovenčine a češtine.

Pri ladení sa riaďte týmito odporúčaniami:

- **Veľkosť časti (`ragChunkSize`)** – pre webové stránky v SK/CZ je vhodný rozsah **800–1 200 znakov** (cca 6–10 viet). Pri kratších častiach sa stráca kontext odseku, pri dlhších klesá presnosť výberu konkrétnej pasáže.
- **Prekryv (`ragChunkOverlap`)** – udržiavajte pomer **15–25 %** zo `ragChunkSize`. Prekryv zabraňuje strate kontextu na hraniciach medzi časťami.
- **Limit modelu** – modely `text-embedding-3-*` zvládnu max. 8 191 tokenov na jeden vstup. Pri slovenčine a češtine je to s rezervou ~6 000 znakov, takže pri odporúčanom rozsahu chunku nie je potrebné sa o limit obávať.
- **Vyhodnotenie kvality** – pripravte si testovaciu sadu 10–20 reprezentatívnych otázok v slovenčine/češtine a porovnávajte TOP-5 výsledky pri rôznych nastaveniach modelu a veľkosti chunku.

### Alternatívne embedding modely

Predvolený model `text-embedding-3-small` je multilingválny a slovenčinu/češtinu zvláda v dostatočnej kvalite pre väčšinu webových projektov. Ak požadujete vyššiu presnosť pre slovanské jazyky, k dispozícii sú tieto alternatívy:

| Model | `ragEmbeddingModel` | `ragEmbeddingDimensions` | Kvalita pre SK/CZ | Poznámka |
| --- | --- | --- | --- | --- |
| OpenAI `text-embedding-3-small` | `text-embedding-3-small` | `1536` | Dobrá | Predvolený model – lacný a rýchly. |
| OpenAI `text-embedding-3-large` | `text-embedding-3-large` | `3072` | Vysoká | Najpresnejší OpenAI viacjazyčný model, cca 6× drahší než `small`. |
| OpenAI `text-embedding-3-large` (skrátený) | `text-embedding-3-large` | `1024` alebo `1536` | Vysoká | Vďaka technike `Matryoshka` (MRL) je možné vektor bezpečne skrátiť bez výraznej straty kvality. Ušetríte miesto v databáze a zrýchlite vyhľadávanie pri zachovaní vyššej presnosti než `small`. |

!>**Upozornenie:** všetky vektory v tabuľke `rag_embedding_chunks` musia pochádzať z toho istého modelu a mať rovnakú dimenziu. Pri zmene modelu alebo dimenzie sa tabuľka vymaže a musíte spustiť úplnú indexáciu obsahu.

#### Čo je Matryoshka (MRL)

Modely `text-embedding-3-small` aj `text-embedding-3-large` sú trénované technikou `Matryoshka Representation Learning`. Najdôležitejšie informácie sú sústredené na začiatku vektora, takže vektor je možné **bezpečne skrátiť** (napr. použiť iba prvých 1 024 alebo 1 536 hodnôt z 3 072) bez geometrického rozpadu reprezentácie.

V praxi to znamená, že môžete použiť kvalitnejší `text-embedding-3-large`, ale výstup si nechať vrátiť napríklad v 1 536 dimenziách – získate vyššiu presnosť než `small@1536` pri rovnakej veľkosti tabuľky aj rovnakej rýchlosti vyhľadávania.

## Používanie v šablónach

Sémantické vyhľadávanie sa aktivuje rovnako ako štandardné vyhľadávanie – vložením aplikácie **Vyhľadávanie** do stránky. Rozdiel je len v nastavení parametra `searchType`.

### Globálne zapnutie cez konfiguráciu

Nastavte `searchType=semantic` v konfigurácii WebJET CMS. Všetky vyhľadávania budú používať vektory.

## Automatické indexovanie

Systém automaticky zaradí stránku do indexovacej fronty pri jej:

- **Uložení** (vytvorenie alebo úprava)
- **Zmazaní** (embedding sa vymaže z vektorovej databázy)

Toto zabezpečuje listener [DocSaveEventListener](../../../../../../src/main/java/sk/iway/iwcm/rag/listener/DocSaveEventListener.java), ktorý reaguje na udalosti ukladania dokumentov.

## Úloha na pozadí

Frontu spracúva úloha na pozadí [sk.iway.iwcm.rag.service.RagIndexCronTask](../../../../../../src/main/java/sk/iway/iwcm/rag/service/RagIndexCronTask.java). Odporúčané nastavenie je spúšťanie každých 5 minút:

Cron úloha je bezpečná voči súbežnému spusteniu – pri behu sa nastaví príznak v cache s platnosťou 60 minút. Chybné záznamy sa nevymažú a opätovne sa spracujú pri ďalšom behu.

## Databázová schéma

Systém vytvára dve tabuľky (automatická aktualizácia cez `autoupdate-webjet9.xml`):

### `rag_index_queue`

Fronta pre asynchrónne indexovanie. Implementované triedou [IndexQueueEntity](../../../../../../src/main/java/sk/iway/iwcm/rag/jpa/IndexQueueEntity.java).

### `rag_embedding_chunks` (pgvector databáza)

Uložené embedding vektory. Implementované triedou [EmbeddingChunkEntity](../../../../../../src/main/java/sk/iway/iwcm/rag/pgvector/EmbeddingChunkEntity.java).

!>**Upozornenie:** Stĺpec `embedding` je typu `vector(N)` – natívny pgvector typ. Nie je mapovaný cez JPA, všetky operácie s vektormi prebiehajú cez natívne SQL dotazy v triede [PgVectorStore](../../../../../../src/main/java/sk/iway/iwcm/rag/vectorstore/PgVectorStore.java).