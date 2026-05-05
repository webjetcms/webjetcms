# Sémantické indexovanie

Sémantické indexovanie prevádza obsah stránok na vektorové reprezentácie (embeddingy) pomocou OpenAI API a ukladá ich do databázy [pgvector](https://github.com/pgvector/pgvector). To umožňuje efektívne sémantické vyhľadávanie.

Pre presnejšie výsledky sa obsah rozdeľuje na menšie časti – **chunky**. Každý chunk je indexovaný samostatne, čo systému umožňuje porovnávať dotazy s konkrétnymi časťami textu a nie s celou stránkou naraz.

Správu embeddingov nájdete v sekcii **Nastavenia → Sémantické indexovanie**.

Aktuálne je podporované indexovanie **webových stránok**. Ďalšie typy môžu pribudnúť v budúcnosti.

!>**Upozornenie:** Indexovanie **neprebieha okamžite**. Každá požiadavka (pridanie, úprava, vymazanie) sa zaradí do **fronty** a spracuje sa v pravidelných intervaloch pomocou cron úlohy.

## Indexovanie webových stránok

Indexuje sa čistý text stránky – názov, perex a telo bez HTML tagov. Obsah sa rozdelí na chunky, ktoré sú zobrazené v tabuľke nižšie.

Každý chunk obsahuje tieto stĺpce:

- **ID entity** – ID webovej stránky
- **Index časti** – poradie chunku v rámci stránky (0, 1, 2, ...)
- **Text časti** – text, pre ktorý bol vygenerovaný embedding (samotný embedding sa nezobrazuje)
- **Model embeddingu** – použitý OpenAI model, napr. `text-embedding-3-small`
- **Dimenzie** – počet dimenzií embeddingu, napr. `1024`
- **Jazyk** – jazyková verzia stránky
- **Stav** – stav spracovania:
  - **COMPLETED** – úspešne spracovaný
  - **ERROR** – nastala chyba
  - **PENDING** – čaká na spracovanie
- **Chybová správa** – popis chyby, ak spracovanie zlyhalo
- **Dátum vytvorenia** – čas spracovania (nie pridania do fronty)

![](datatable.png)

## Filtrovanie

V hlavičke tabuľky sú dostupné tieto filtre:

- **Výber priečinka** – zobrazí chunky len pre stránky z daného priečinka (v rámci aktuálnej domény)
- **Zobraziť aj z podpriečinkov** – zahrnie do výsledkov aj stránky z podpriečinkov

!>**Upozornenie:**  Ak vyberiete **Koreňový priečinok** bez zapnutia možnosti **Zobraziť aj z podpriečinkov**, nezískate žiadne výsledky. Koreňový priečinok je virtuálny a neobsahuje stránky priamo.

## Presmerovanie z Webových stránok

V sekcii **Webové stránky** môžete pri zvolenom priečinku kliknúť na tlačidlo <button class="btn btn-sm buttons-selected btn-outline-secondary"><span><i class="ti ti-database-search"></i></span></button> v hlavičke priečinkov. Tým sa otvorí sekcia **Sémantické indexovanie** s automaticky nastaveným filtrom pre daný priečinok.

### Automatické indexovanie

Systém automaticky zaradí stránku do fronty pri:

- **vytvorení alebo úprave** – stránka sa zaindexuje (alebo preindexuje) bez manuálneho zásahu
- **zmazaní alebo presunutí do koša** – všetky súvisiace chunky sa odstránia z databázy
- **obnovení z koša** – stránka sa opätovne zaindexuje

### Manuálne indexovanie

Kliknite na tlačidlo <button class="btn btn-sm btn-success" type="button"><span><i class="ti ti-database-plus"></i></span></button> pre otvorenie dialógu indexovania.

Dialóg zobrazí prehľad stránok zvoleného priečinka – celkový počet, počet už indexovaných a počet vo fronte. Priečinok sa prednastaví podľa aktívneho filtra. Po potvrdení sa všetky stránky zaradia do fronty. Ak stránka už má aktuálny embedding, znovu sa neindexuje.

Akciu spustíte tlačidlom <button class="btn btn-primary"><i class="ti ti-check"></i> <span>Spustiť akciu</span></button>.

![](index-dialog.png)

### Manuálne odstránenie indexovania

Kliknite na tlačidlo <button class="btn btn-sm btn-danger" type="button"><span><i class="ti ti-database-minus"></i></span></button> pre otvorenie dialógu odstránenia indexov.

Dialóg zobrazí rovnaký prehľad ako pri indexovaní. Po potvrdení sa všetky stránky zaradia do fronty na odstránenie všetky chunky pre stránky zvoleného priečinka.

Akciu spustíte tlačidlom <button class="btn btn-primary"><i class="ti ti-check"></i> <span>Spustiť akciu</span></button>.

![](remove-index-dialog.png)

## Detaily implementácie

Technický popis procesu indexovania nájdete v [dokumentácii pre vývojárov](../../../custom-apps/apps/rag/semantic-search/README.md).