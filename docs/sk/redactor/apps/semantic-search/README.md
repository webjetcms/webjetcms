# Sémantické vyhľadávanie (RAG)

Sémantické vyhľadávanie umožňuje návštevníkom nájsť relevantné stránky na základe **významu otázky**, nielen zhody kľúčových slov. Využíva technológiu RAG (Retrieval-Augmented Generation) postavenú na vektorovej databáze [pgvector](https://github.com/pgvector/pgvector) a embeddingoch generovaných prostredníctvom OpenAI API.

## Nastavenie sémantického vyhľadávania

Na spustenie sémantického vyhľadávania je potrebné:

- Povoliť sémantické vyhľadávanie nastavením konfiguračnej premennej `ragSemanticSearchEnabled` na hodnotu `true`.
- Nastaviť konfiguračnú premennú `searchType` na hodnotu `semantic` pre zvolenie tohto typu vyhľadávania.
- Overiť, že konfiguračná premenná `luceneAsDefaultSearch` je nastavená na hodnotu `false`. Ak je nastavená na `true`, bude sa namiesto toho používať Lucene bez ohľadu na hodnotu premennej `searchType`, pretože má vyššiu prioritu.
- Spustiť indexovanie cez administrátorské rozhranie na vytvorenie embeddingov a naplnenie pgvector databázy.

## Sémantické indexovanie

Na využitie sémantického vyhľadávania je potrebné mať naindexovaný obsah pomocou sémantického indexovania, ktoré je dostupné v administrátorskom rozhraní. Viac informácií nájdete v časti [Sémantické indexovanie](./embedding-chunks.md).

## Detaily implementácie

Technický popis procesu indexovania nájdete v [dokumentácii pre vývojárov](../../../custom-apps/apps/rag/semantic-search/README.md).