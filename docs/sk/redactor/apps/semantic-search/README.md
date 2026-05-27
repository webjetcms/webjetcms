# Sémantické vyhľadávanie (RAG)

Sémantické vyhľadávanie umožňuje návštevníkom nájsť relevantné stránky na základe **významu otázky**, nielen zhody kľúčových slov. Využíva technológiu postavenú na vektorovej databáze [pgvector](https://github.com/pgvector/pgvector) a vektorov generovaných prostredníctvom OpenAI API.

## Nastavenie sémantického vyhľadávania

Na spustenie sémantického vyhľadávania je potrebné:

- Povoliť sémantické vyhľadávanie nastavením konfiguračnej premennej `ragSemanticSearchEnabled` na hodnotu `true`.
- Nastaviť konfiguračnú premennú `searchType` na hodnotu `semantic` pre zvolenie tohto typu vyhľadávania.
- Overiť, že konfiguračná premenná `luceneAsDefaultSearch` je nastavená na hodnotu `false`. Ak je nastavená na `true`, bude sa namiesto toho používať Lucene bez ohľadu na hodnotu premennej `searchType`, pretože má vyššiu prioritu.
- Spustiť indexovanie cez administrátorské rozhranie na vytvorenie vektorov a naplnenie vektorovej databázy.

### Nové konfiguračné premenné

- `ragSemanticSearchEnabled` (predvolené: `false`) - zapne/vypne semantické vyhľadávanie cez RAG nad `pgvector`.
- `searchType` (predvolené: `db`) - typ vyhľadávania. Pre semantické vyhľadávanie nastavte hodnotu `semantic`.
- `ragEmbeddingModel` (predvolené: `text-embedding-3-small`) - názov embedding modelu, ktorý sa použije pri indexovaní aj vyhľadávaní.
- `ragEmbeddingDimensions` (predvolené: `1536`) - počet dimenzií embedding vektora. Musí sa zhodovať s modelom aj definíciou stĺpca vo vektorovej databáze.
- `ragChunkSize` (predvolené: `1000`) - maximálna veľkosť jedného textového chunku (v znakoch) pri indexovaní.
- `ragChunkOverlap` (predvolené: `200`) - počet prekrývajúcich sa znakov medzi susednými chunkmi.
- `ragSemanticSearchMinSimilarity` (predvolené: `0.2`) - minimálna hodnota similarity pre výsledky. Hodnota mimo intervalu 0-1 sa orezáva na najbližšiu hranicu.
- `ragSemanticSearchMinResults` (predvolené: `3`) - minimálny počet výsledkov semantického vyhľadávania; pri menšom počte sa doplnia podľa najvyššej similarity.

## Sémantický index

Na využitie sémantického vyhľadávania je potrebné mať indexovaný obsah pomocou sémantického indexovania, ktoré je dostupné v administrátorskom rozhraní. Viac informácií nájdete v časti [Sémantický index](./embedding-chunks.md).

## Detaily implementácie

Technický popis procesu indexovania nájdete v [dokumentácii pre vývojárov](../../../custom-apps/apps/rag/semantic-search/README.md).