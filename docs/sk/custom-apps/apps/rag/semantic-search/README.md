# Sémantické vyhľadávanie (RAG)

Sémantické vyhľadávanie umožňuje návštevníkom nájsť relevantné stránky na základe **významu otázky**, nielen zhody kľúčových slov. Využíva technológiu RAG (Retrieval-Augmented Generation) postavenú na vektorovej databáze [pgvector](https://github.com/pgvector/pgvector) a embeddingoch generovaných cez OpenAI API.

## Ako to funguje

Systém pracuje v dvoch fázach:

### 1. Indexovanie (offline)

Keď je webová stránka uložená alebo zmenená, systém ju zaradí do fronty na indexovanie. Cron úloha ([RagIndexCronTask](../../../../../../src/main/java/sk/iway/iwcm/rag/service/RagIndexCronTask.java)) pravidelne spracúva frontu:

1. **Extrakcia obsahu** – z `DocDetails` sa extrahuje čistý text (názov + perex + telo stránky bez HTML tagov).
2. **Rozdelenie na časti (chunking)** – text sa rozdelí na prekrývajúce sa časti pomocou algoritmu posuvného okna (`SlidingWindowChunker`). Predvolená veľkosť časti je 500 znakov s prekryvom 100 znakov.
3. **Generovanie embeddingov** – každá časť sa odošle do OpenAI API (`/v1/embeddings`) a vráti sa vektor s 1 536 dimenziami (model `text-embedding-3-small`).
4. **Uloženie do pgvector** – vektory sa uložia do tabuľky `rag_embedding_chunks` v `PostgreSQL` databáze s rozšírením `pgvector`.

### 2. Vyhľadávanie (online)

Keď návštevník zadá vyhľadávací dotaz:

1. Dotaz sa prevedie na embedding vektor cez OpenAI API.
2. Vykoná sa vektorové vyhľadávanie v pgvector databáze (kosinusová podobnosť).
3. Výsledky sa agregujú podľa dokumentu – za každý dokument sa vyberie najlepší chunk.
4. Dokumenty sa vrátia zoradené podľa podobnosti a zobrazia sa rovnakým spôsobom ako výsledky štandardného vyhľadávania.

## Požiadavky

- **PostgreSQL** s rozšírením **pgvector** (obraz: `pgvector/pgvector:pg18-trixie` alebo novší)
- **OpenAI API kľúč** – ten istý, ktorý sa používa pre AI asistentov (`ai_openAiAuthKey`)
- Sémantické vyhľadávanie funguje **len s PostgreSQL**. Pre ostatné databázy (MySQL/MariaDB, MSSQL, Oracle) je potrebné nastaviť samostatnú pgvector databázu cez datasource `rag_jpa`.

### PostgreSQL ako primárna databáza

Ak WebJET CMS beží priamo na PostgreSQL, pgvector databáza sa použije automaticky bez ďalšej konfigurácie.

### Samostatná pgvector databáza (vedľajšia)

Ak primárna databáza nie je PostgreSQL, vytvorte Docker kontajner s pgvector a nakonfigurujte datasource `rag_jpa` v súbore `poolman.xml`:

```xml
<datasource name="rag_jpa">
  <driver-class>org.postgresql.Driver</driver-class>
  <url>jdbc:postgresql://localhost:15433/webjetcms_rag</url>
  <username>webjetcms_rag</username>
  <password>...</password>
</datasource>
```

Pre lokálny vývoj je pripravený súbor `.devcontainer/db/docker-compose-rag-pgsql.yml`:

```bash
docker compose -f .devcontainer/db/docker-compose-rag-pgsql.yml up -d
```

## Konfigurácia

Aktivácia a nastavenie sémantického vyhľadávania v `Konfigurácia` (skupina `rag`):

| Premenná | Predvolená hodnota | Popis |
|---|---|---|
| `ragSemanticSearchEnabled` | `false` | Zapne sémantické vyhľadávanie. Nastavte na `true` pre aktiváciu. |
| `ragEmbeddingModel` | `text-embedding-3-small` | Názov OpenAI embedding modelu. |
| `ragEmbeddingDimensions` | `1536` | Počet dimenzií vektora. Musí zodpovedať použitému modelu a tabuľke v databáze. |
| `ragChunkSize` | `500` | Maximálna veľkosť jednej časti textu v znakoch. |
| `ragChunkOverlap` | `100` | Počet znakov, o ktoré sa susedné časti prekrývajú. |
| `searchType` | `db` | Typ vyhľadávania: `db` (databázové), `lucene` (Lucene fulltext), `semantic` (pgvector sémantické). |

!> Pre aktiváciu sémantického vyhľadávania nastavte `ragSemanticSearchEnabled=true` **aj** `searchType=semantic`.

## Používanie v šablónach

Sémantické vyhľadávanie sa aktivuje rovnako ako štandardné vyhľadávanie – vložením aplikácie **Vyhľadávanie** do stránky. Rozdiel je len v nastavení parametra `searchType`.

### Globálne zapnutie cez konfiguráciu

Nastavte `searchType=semantic` v konfigurácii WebJET CMS. Všetky vyhľadávania budú používať pgvector.

## Automatické indexovanie

Systém automaticky zaradí stránku do indexovacej fronty pri jej:

- **Uložení** (vytvorenie alebo úprava)
- **Zmazaní** (embedding sa vymaže z vektorovej databázy)

Toto zabezpečuje listener [DocSaveEventListener](../../../../../../src/main/java/sk/iway/iwcm/rag/listener/DocSaveEventListener.java), ktorý reaguje na udalosti ukladania dokumentov.

## Cron úloha

Frontu spracúva cron úloha [RagIndexCronTask](../../../../../../src/main/java/sk/iway/iwcm/rag/service/RagIndexCronTask.java). Odporúčané nastavenie je spúšťanie každých 5 minút:

Cron úloha je bezpečná voči súbežnému spusteniu – pri behu sa nastaví príznak v cache s platnosťou 60 minút. Chybné záznamy sa nevymažú a opätovne sa spracujú pri ďalšom behu.

## Databázová schéma

Systém vytvára dve tabuľky (automatická aktualizácia cez `autoupdate-webjet9.xml`):

### `rag_index_queue`

Fronta pre asynchrónne indexovanie. Implementované triedou [IndexQueueEntity](../../../../../../src/main/java/sk/iway/iwcm/rag/jpa/IndexQueueEntity.java).

### `rag_embedding_chunks` (pgvector databáza)

Uložené embedding vektory. Implementované triedou [EmbeddingChunkEntity](../../../../../../src/main/java/sk/iway/iwcm/rag/pgvector/EmbeddingChunkEntity.java).

!>**Upozornenie:** Stĺpec `embedding` je typu `vector(N)` – natívny pgvector typ. Nie je mapovaný cez JPA, všetky operácie s vektormi prebiehajú cez natívne SQL dotazy v triede [PgVectorStore](../../../../../../src/main/java/sk/iway/iwcm/rag/vectorstore/PgVectorStore.java).