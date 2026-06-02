# Vyhľadávanie

Ponúknite návštevníkom možnosť rýchleho a presného vyhľadávania priamo na vašej stránke. Vložte vyhľadávací formulár a zobrazenie výsledkov vyhľadávania, ktoré umožňuje nastavenie adresára, počtu záznamov na stránku a spôsobu usporiadania. Využite silu vyhľadávania v databáze alebo pomocou Lucene/Elastic Search pre vyhľadávanie aj so skloňovaním. Podporované sú aj vyhľadávania v texte súborov typu `doc(x), xls(x), ppt(x), pdf, xml a txt`.

## Nastavenia aplikácie

V nastaveniach možno nastaviť:

- Adresár - ID priečinkov web stránok pre vyhľadávanie, hľadá sa aj v podpriečinkoch
- Počet odkazov na stránku - počet záznamov na jednu stranu vyhľadávania
- Kontrolovať duplicitu - ak sa web stránka nachádza vo viacerých priečinkoch, zapne sa kontrola duplicít. Zvyšuje záťaž na server.
- Usporiadať podľa - Priority, Názvu, Dátumu zmeny
- Vložiť - formulár, výsledky, spolu - nastavuje typ vloženej časti, ak chcete mať oddelené vyhľadávacie pole napr. v hlavičke vložte samostatne formulár a samostatne výsledky vyhľadávania. Pri nastavení hodnoty Formulár je potrebné zadať ID stránky s výsledkami vyhľadávania.

![](editor.png)

### Nastavenie hľadania v súboroch

Ak chcete vyhľadávať aj v súboroch, je potrebné [nastaviť indexovanie súborov](../../files/fbrowser/folder-settings/README.md#indexovanie) v časti Prieskumník a na danom priečinku so súbormi a spustiť prvotné indexovanie.

### Nastavenie typu vyhľadávania

Systém podporuje nasledujúce typy vyhľadávania:

- **Databázové vyhľadávanie** – štandardný typ vyhľadávania pomocou databázového servera.
  - Používa sa predvolene, ak nie je nastavený iný typ. Možno ho explicitne nastaviť konfiguračnou premennou `searchType` na hodnotu `db`.
- **Lucene** – vyhľadávanie pomocou knižnice [Lucene](https://lucene.apache.org/), ktorá tvorí základ aj systému Elastic Search.
  - Nastavte konfiguračnú premennú `luceneAsDefaultSearch` na hodnotu `true`, alebo premennú `searchType` na hodnotu `lucene`.
  - Spustite prvotné indexovanie cez `/components/search/lucene_console.jsp`.
- **Sémantické vyhľadávanie** – vyhľadávanie pomocou pgvector.
  - Nastavte premennú `searchType` na hodnotu `semantic`, povoľte sémantické vyhľadávanie nastavením premennej `ragSemanticSearchEnabled` na `true`.
  - Viac sa dočítate v časti [Sémantické vyhľadávanie](../semantic-search/README.md).

!>**Upozornenie:** Konfiguračná premenná `luceneAsDefaultSearch` má vyššiu prioritu ako premenná `searchType`. Ak je teda `luceneAsDefaultSearch=true`, bude sa používať Lucene bez ohľadu na nastavenú hodnotu premennej `searchType`.

### Porovnanie typov vyhľadávania

| | Databázové (`db`) | Lucene | Sémantické (`semantic`) |
| --- | --- | --- | --- |
| Technológia | SQL LIKE / FULLTEXT | `Apache Lucene` | `OpenAI embeddings` + `pgvector` |
| Zhoda | Kľúčové slová | Kľúčové slová + skloňovanie | Sémantický význam |
| Výsledky bez zhody slov | Nie | Čiastočne | Áno |
| Požiadavky | Primárna DB | Lucene index | `PostgreSQL` + `pgvector` + `OpenAI` |
| Cena | Zadarmo | Zadarmo | OpenAI API (platené) |

## Zobrazenie aplikácie

![](search.png)
