# Penzión Pod lipou

Scenár k PR #211 a úlohe #58521 používa lokálnu inštanciu na `http://iwcm.interway.sk`. Štyri zdrojové stránky, otázku a obsah vyhľadávacej stránky nájdete v [demo.js](demo.js). Slovenský komentár aj poradie záberov sú v [scenári](../../211-58461-semantic-search.js).

## Príprava DEV

Overte dostupnosť vektorového úložiska a nakonfigurovaného poskytovateľa AI. Asistenty `RAG-EMB-INDEX` a `RAG-EMB-SEARCH` musia používať rovnakého poskytovateľa, model a rozmer zodpovedajúci uloženým vektorom. Pre odpoveď musí fungovať aj asistent `RAG-SEARCH`. Povoľte `ragSemanticSearchEnabled` a `ragAnswerAllowed`. Prednosť Lucene musí byť vypnutá. Režim vyhľadávania scenár nastavuje v konkrétnej aplikácii.

Pri výrobe bol na tejto DEV inštancii spustený existujúci kontajner `webjetcms-rag-pgsql`. Datasource `rag_jpa` mal stĺpec `vector(768)`. Pre obe operácie bol nastavený model OpenAI `text-embedding-3-small`. Na tomto uzle sa `ragEmbeddingDimensions` nastavilo na 768 cez voľbu **Nastaviť dočasne** v editore konfiguračnej premennej. Rovnakým spôsobom sa pre ukážku zapli `ragSemanticSearchEnabled` a `ragAnswerAllowed`. Trvalé uloženie premennej rozmeru spúšťa vymazanie embeddingov. Po reštarte znovu overte účinné nastavenia aj schému. [Technická dokumentácia](../../../../../../docs/sk/custom-apps/apps/rag/semantic-search/README.md)

Príkazy spúšťajte z adresára `src/test/webapp` s lokálnym testovacím účtom a nastaveným `CODECEPT_DEFAULT_PASSWORD`.

```sh
node video/assets/211-penzion/prepare.cjs
npm run video:plan video/211-58461-semantic-search.js
```

Príprava vytvorí alebo znovu použije označený podpriečinok v priečinku Test stavov, šablónu dostupnú iba v ňom a päť demo stránok. Identifikátory uloží do `docs/feature-video/211-penzion/fixture.json`. Po prvom vytvorení počkajte, kým existujúca úloha RAG spracuje frontu. V zozname indexovaných častí overte stav štyroch zdrojových stránok. Na verejnej stránke Informácie pre hostí vyskúšajte otázku zo scenára. Odkazy musia viesť na Pobyt so zvieratami a Dopravu a parkovanie.

## Nahrávanie

```sh
CODECEPT_SHOW=false npm run video video/211-58461-semantic-search.js
CODECEPT_SHOW=false npm run video:shots video/211-58461-semantic-search.js
CODECEPT_SHOW=false VIDEO_SHOT=nastavenie npm run video video/211-58461-semantic-search.js
CODECEPT_SHOW=false npm run video:title video/211-58461-semantic-search.js
```

Samostatný záber vyberiete hodnotou `otazka`, `nastavenie`, `odpoved` alebo `dokumentacia`. Pri prvom zábere je AI odpoveď vypnutá. Záber administrácie ju zapne a uloží zmenu. Pri samostatnom opakovaní sa potrebný stav pripraví pred záberom. Vyhľadávacie požiadavky používajú neprihlásený prehliadač. Thumbnail používa vlastný beh a po snímke obnoví pôvodnú voľbu.

Platený príkaz pre ElevenLabs vyžaduje `ELEVENLABS_API_KEY`. Vygeneruje čítanie všetkých štyroch záberov.

```sh
npm run audio video/211-58461-semantic-search.js
```

Video, thumbnail a MP3 sa ukladajú do `docs/feature-video`. Editovateľný strih a finálny export tejto produkcie sú v podpriečinku `211-penzion`.
