# Sémantické vyhľadávanie (RAG)

Sémantické vyhľadávanie umožňuje návštevníkom nájsť relevantné stránky na základe **významu otázky**, nielen zhody kľúčových slov. Využíva technológiu postavenú na vektorovej databáze [pgvector](https://github.com/pgvector/pgvector) a vektorov generovaných prostredníctvom OpenAI API.

## Nastavenie sémantického vyhľadávania

Na spustenie sémantického vyhľadávania je potrebné:

- Povoliť sémantické vyhľadávanie nastavením konfiguračnej premennej `ragSemanticSearchEnabled` na hodnotu `true`.
- Nastaviť konfiguračnú premennú `searchType` na hodnotu `semantic` pre zvolenie tohto typu vyhľadávania.
- Overiť, že konfiguračná premenná `luceneAsDefaultSearch` je nastavená na hodnotu `false`. Ak je nastavená na `true`, bude sa namiesto toho používať Lucene bez ohľadu na hodnotu premennej `searchType`, pretože má vyššiu prioritu.
- Spustiť indexovanie cez administrátorské rozhranie na vytvorenie vektorov a naplnenie vektorovej databázy.

## Sémantický index

Na využitie sémantického vyhľadávania je potrebné mať indexovaný obsah pomocou sémantického indexovania, ktoré je dostupné v administrátorskom rozhraní. Viac informácií nájdete v časti [Sémantický index](./embedding-chunks.md).

## Detaily implementácie

Technický popis procesu indexovania nájdete v [dokumentácii pre vývojárov](../../../custom-apps/apps/rag/semantic-search/README.md).