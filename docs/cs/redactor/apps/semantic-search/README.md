# Sémantické vyhledávání (RAG)

Sémantické vyhledávání umožňuje návštěvníkům nalézt relevantní stránky na základě **významu otázky**, nejen shody klíčových slov. Využívá technologii postavenou na vektorové databázi [pgvector](https://github.com/pgvector/pgvector) a vektorů generovaných prostřednictvím OpenAI API.

## Nastavení sémantického vyhledávání

Ke spuštění sémantického vyhledávání je potřeba:

- Povolit sémantické vyhledávání nastavením konfigurační proměnné `ragSemanticSearchEnabled` na hodnotu `true`.
- Nastavit konfigurační proměnnou `searchType` na hodnotu `semantic` pro zvolení tohoto typu vyhledávání.
- Ověřit, že konfigurační proměnná `luceneAsDefaultSearch` je nastavena na hodnotu `false`. Je-li nastavena na `true`, bude se místo toho používat Lucene bez ohledu na hodnotu proměnné `searchType`, protože má vyšší prioritu.
- Spustit indexování přes administrátorské rozhraní pro vytvoření vektorů a naplnění vektorové databáze.

## Sémantický index

Pro využití sémantického vyhledávání je třeba mít indexovaný obsah pomocí sémantického indexování, které je dostupné v administrátorském rozhraní. Více informací naleznete v části [Sémantický index](./embedding-chunks.md).

## Detaily implementace a nastavení

Technický popis procesu indexování a způsob prvotního nastavení naleznete v [dokumentaci pro vývojáře](../../../custom-apps/apps/rag/semantic-search/README.md).