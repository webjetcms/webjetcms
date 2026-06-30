# Sémantické vyhledávání (RAG)

Sémantické vyhledávání umožňuje návštěvníkům nalézt relevantní stránky na základě **významu otázky**, nejen shody klíčových slov. Využívá vektorovou databázi [pgvector](https://github.com/pgvector/pgvector) a vektory generované prostřednictvím OpenAI API.

Volitelně může nad stejným indexem zobrazit také **RAG odpověď** - krátkou odpověď vygenerovanou AI pouze z nalezeného obsahu webu. Odpověď se zobrazí nad klasickým seznamem výsledků vyhledávání.

![](rag-result.png)

## Co návštěvník uvidí

- Při sémantickém vyhledávání se zobrazí seznam relevantních stránek seřazený podle podobnosti významu otázky.
- Při hybridním vyhledávání se kombinuje sémantické pořadí s fulltextovou shodou v indexovaných textech.
- Při povolené RAG odpovědi se nad výsledky zobrazí blok **Odpověď AI z vyhledávání**.
- Pokud odpověď nelze sestavit z indexovaného obsahu, nezobrazí se žádná RAG odpověď.

## Nastavení sémantického vyhledávání

Ke spuštění sémantického vyhledávání je potřeba:

- Povolit sémantické vyhledávání nastavením konfigurační proměnné `ragSemanticSearchEnabled` na hodnotu `true`.
- Nastavit typ vyhledávání na hodnotu `semantic` nebo `hybrid`. Můžete to provést globálně přes konfigurační proměnnou `searchType`, nebo přímo v aplikaci **Vyhledávání**.
- Při hybridním režimu ověřit, že konfigurační proměnná `ragHybridSearchEnabled` je nastavena na hodnotu `true`.
- Ověřit, že konfigurační proměnná `luceneAsDefaultSearch` je nastavena na hodnotu `false`. Pokud je nastavena na `true`, bude se místo sémantického vyhledávání používat Lucene, protože má vyšší prioritu.
- Nastavit OpenAI API klíč používaný i pro AI asistenty v proměnné `ai_openAiAuthKey`.
- Spustit indexování přes administrátorské rozhraní pro vytvoření vektorů a naplnění vektorové databáze.
- Nastavit automatizovanou úlohu `sk.iway.iwcm.rag.service.RagIndexCronTask`, která zpracovává frontu indexování.

!>**Upozornění:** Po nasazení změn doporučujeme spustit opětovné indexování stránek. Index nyní ukládá také informace o složce stránky (`group_id`, `root_group_l1`, `root_group_l2`, `root_group_l3`), které se používají při filtrování výsledků podle složek zvolených v aplikaci **Vyhledávání**.

## Nastavení aplikace Vyhledávání

Sémantické vyhledávání se používá přes aplikaci **Vyhledávání** vloženou do stránky. Aplikace má kromě běžných polí také nastavení pro sémantické, hybridní a RAG vyhledávání.

### Karta Základní

- **Typ vyhledávání** - určuje, jaký mechanismus použije konkrétní vložená aplikace:
  - **Podle konfigurace** - použije globální hodnotu `searchType`.
  - **Databázové** - použije standardní databázové vyhledávání.
  - **Fulltextové/Lucene** - použije Lucene.
  - **Sémantické** - použije vektorové vyhledávání bez hybridní fulltextové větve.
  - **Hybridní** - použije vektorové vyhledávání spolu s fulltextem nad indexovanými částmi textu.
- **Přidat odpověď RAG** ​​- zapne nebo vypne AI odpověď pro konkrétní aplikaci. Hodnota **Podle konfigurace** respektuje globální nastavení `ragAnswerAllowed`.
- **Adresář** - omezí vyhledávání na vybrané složky. Při sémantickém a hybridním vyhledávání se omezení aplikuje přímo nad sémantickým indexem.

Pokud je některá možnost označena jako **aktuálně nepovoleno**, nejprve povolte příslušnou globální konfigurační proměnnou.

### Karta Sémantická nastavení

Umožňuje přepsat globální hodnoty pro konkrétní vloženou aplikaci:

- **Minimální podobnost** - lokální hodnota pro `ragSemanticSearchMinSimilarity`.
- **Minimální počet výsledků** - lokální hodnota pro `ragSemanticSearchMinResults`.

Prázdná hodnota znamená, že se použije globální konfigurace.

### Karta Hybridní nastavení

Umožňuje nastavit, kdy se má k vektorovému vyhledávání přidat fulltextová větev:

- **Režim hybridního vyhledávání** - `off`, `always`, `short_query_only`, `fallback_on_low_vector` nebo **Podle konfigurace**.
- **Maximální počet znaků/slov pro krátkou poptávku** - hranice pro režim `short_query_only`.
- **Prahová podobnost pro fallback** - hranice pro režim `fallback_on_low_vector`.
- **Váhy vektorové a fulltextové větve** - určují výsledné pořadí při kombinování přes RRF.
- **Koeficient načítání bloků** - kolik textových částí se načte před agregací na dokumenty.
- **Použít `ILIKE` fallback pro fulltext** - použije jednoduché textové hledání, pokud PostgreSQL fulltext nic nenajde.

### Karta RAG nastavení

RAG odpověď se generuje až po vyhledání relevantních částí obsahu. Nastavit lze:

- **AI asistent pro RAG odpověď** - výchozí asistent se vytvoří automaticky. Volitelně lze vybrat vlastního asistenta určeného pro RAG odpovědi.
- **Minimální podobnost** - měkký práh pro části textu použité při tvorbě odpovědi.
- **Použít top K bloků** - kolik nejlepších částí textu vstupuje do post-processingu.
- **Maximální mezera mezi bloky** - určuje, zda se sousední části jedné stránky sloučí do společného kontextu.
- **Maximální počet bloků** a **maximální počet znaků** - omezují velikost kontextu odeslaného AI modelu.
- **Maximální počet znaků bloku** - zabraňuje vytvoření příliš velkého sloučeného kontextového bloku.

RAG odpověď používá pouze obsah získaný ze sémantického indexu. Pokud se v kontextu nenachází odpověď, systém zobrazí fallback text namísto vymýšlení odpovědi.

## Sémantický index

Pro využití sémantického vyhledávání je třeba mít indexovaný obsah pomocí sémantického indexování, které je dostupné v administrátorském rozhraní. Více informací naleznete v části [Sémantický index](./embedding-chunks.md).

## Detaily implementace a nastavení

Technický popis procesu indexování, vyhledávání, hybridního režimu a RAG odpovědi naleznete v [dokumentaci pro vývojáře](../../../custom-apps/apps/rag/semantic-search/README.md).
