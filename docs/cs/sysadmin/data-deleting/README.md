# Vymazání dat

Aplikace **Vymazání dat** umožňuje odstranit nepotřebná data z databáze, což může zvýšit výkon serveru a uvolnit místo na disku. Tento nástroj naleznete v **Nastavení** pod nadpisem **Vymazání dat**.

## Záznamy v databázi

Odstranění dat z vybraných databázových tabulek, odstranění je možné z následujících skupin:
- **Statistiky**: Odstraní statistické údaje. Odstranění starších dat může výrazně zlepšit výkon serveru, ale ztratíte informace o návštěvnosti webu za zvolené období.
- **E-maily**: Umožňuje odstranit odeslané e-maily z aplikace Hromadná pošta a e-maily odeslané s časovým zpožděním (nebo e-maily odeslané v rámci clusteru s více uzly).
- **Historie webu**: Odstraní zaznamenané historické verze webových stránek, které se ukládají při každém zveřejnění webové stránky. Zobrazují se na kartě Historie při úpravách webové stránky. Odstranění nemá vliv na aktuálně zobrazené stránky, odstraňují se historické verze.
- **Monitorování serveru**: Odstraní zaznamenaná data monitorování serveru, jako jsou metriky výkonu a protokoly.
- **Audit**: Odstraní auditní záznamy, které sledují aktivitu uživatelů a systémové události, odstranit lze pouze vybrané typy záznamů.

S každým odstraněním se také provede optimalizace databázové tabulky, aby se fyzicky uvolnilo místo na disku a optimalizovalo pořadí záznamů v databázové tabulce.

![](database-delete.png)

## Objekty mezipaměti

Zobrazí seznam objektů uložených v mezipaměti aplikace a umožní je jednotlivě odstranit, což může snížit spotřebu paměti nebo vyvolat obnovení dat v mezipaměti serveru. Klepnutím na název můžete zobrazit obsah záznamu pro vybrané typy dat. Objekt se používá pro práci [Cache](../../../../../src/webjet8/java/sk/iway/iwcm/Cache.java)

![](cache-objects.png)

## Trvalé objekty mezipaměti

Správa a mazání objektů uložených v trvalé mezipaměti, která uchovává data i po restartu serveru (data jsou uložena v databázi). Objekt se používá pro práci [PersistentCacheDB](../../../../../src/webjet8/java/sk/iway/iwcm/system/cache/PersistentCacheDB.java). Do této mezipaměti lze ukládat pouze textová data, typicky metodou `downloadUrl(String url, int cacheInMinutes)` který na pozadí stahuje data ze zadané adresy URL a v nastaveném čase je aktualizuje. Aplikace použije tuto metodu a okamžitě načte data z mezipaměti.

![](persistent-cache-objects.png)
