# Plánované publikovanie s chránenými súbormi

Budúcu verziu existujúcej stránky môžete pripraviť aj s obrázkami a dokumentmi, ktoré do termínu zverejnenia nesmú byť dostupné návštevníkom. Živý obsah a bežná pracovná verzia zostávajú samostatné. Funkciu musí správca zapnúť konfiguračnou premennou `plannedPublishingEnabled=true`; predvolene je vypnutá.

## Príprava stránky

1. Otvorte existujúcu stránku. Úplne novú stránku najskôr uložte ako nezverejnenú.
2. V karte **Perex** nastavte **Dátum začiatku** a zapnite **Naplánovať zmenu stránky po tomto dátume**. WebJET jednorazovo skopíruje živé médiá do plánovanej sady.
3. Nové podklady nahrajte vo výbere súborov do **Médiá tejto stránky → Plánované obrázky** alebo **Plánované súbory**. Tieto priečinky sú dostupné aj pri výbere perexového obrázka a odkazov v karte **Médiá**.
4. Upravte obsah a plánované médiá. Kliknite na **Naplánovať**.
5. Pri ďalšej úprave použite **Otvoriť plánovanú verziu** v karte Perex alebo otvorte príslušnú verziu cez Históriu. Opätovné otvorenie už nekopíruje živé médiá.

Na jednu stránku je podporovaný jeden plán. Zmena termínu nahradí pôvodné naplánovanie. Plánovaná verzia pri publikovaní nahradí obsah a celú sadu médií vrátane ich poradia a skupín. Novšie opravy živej stránky preto do pripravovanej verzie preneste ručne. Ak odstránite všetky plánované médiá, po publikovaní bude živá sada prázdna.

Zmeny médií sa ukladajú priebežne. Zatvorenie editora cez **Zrušiť** ich nevracia. Odškrtnutie časovania pripravené údaje neodstráni. Tlačidlo **Zrušiť plánované zmeny** zruší naplánovanie aj plánované médiá; nahrané súbory ostanú chránené.

## Súbory a odkazy

Príklad pre stránku s ID `42`:

```text
/files/protected/planned-changes/42/images/apple/foto.webp
→ /images/apple/foto.webp?v=<historyId>

/files/protected/planned-changes/42/files/apple/parametre.pdf
→ /files/apple/parametre.pdf?v=<historyId>
```

Cieľ je uložený v ceste súboru, preto ho neskoršie premenovanie stránky nezmení. Kopírujú sa iba súbory použité v publikovanej verzii alebo jej médiách. Nepoužité súbory ostanú chránené a čistia sa ručne.

Podporované sú obsah, anotácia, perexový obrázok, `htmlHead`, voliteľné polia a odkazy, náhľady či popisy médií. Prepis zahŕňa `src`, `href`, `srcset`, `poster`, `data-src`, `data-srcset`, `data-original`, `data-lazy-src`, `data-lazy-srcset`, `/thumb/` a vložené CSS `url(...)`. Absolútne odkazy musia patriť doméne stránky. Existujúce parametre a fragmenty sa zachovajú; parameter `v` dostane novú hodnotu.

Nerozpoznané použitie chránenej URL sa odmietne pri ukladaní alebo publikovaní. Externé CSS/JS, iframe a ich závislosti sa neprechádzajú. Produktový prehliadač a e-shop potrebujú vlastný publikačný postup.

Prepísanie existujúceho súboru ovplyvní všetky stránky používajúce rovnakú cestu. Súbežné plány s rovnakým cieľovým súborom sa odmietnu. Už verejne nahraný súbor nie je možné týmto postupom spätne utajiť. Ani odverejnenie stránky automaticky neskryje predtým publikované súbory.

## Nasadenie a prevádzka

- Databázová aktualizácia pridáva stĺpec `documents.planned_media_initialized` s predvolenou hodnotou `false` a úlohu `sk.iway.iwcm.doc.PlannedPublishCron` raz za minútu pre `all-admin`. Nová tabuľka nevzniká.
- Pre MariaDB/MySQL musia tabuľky `documents`, `documents_history`, `media` a `media_group_to_media` používať **InnoDB**. Staršie inštalácie môžu mať MyISAM; funkcia ich odmietne. Konverziu existujúcich tabuliek musí správca pripraviť podľa veľkosti databázy a prevádzkového okna. Automatická aktualizácia nemení ich úložisko.
- Plánované aj verejné súbory musia byť na spoločnom súborovom systéme podporujúcom atómové premenovanie. V klastri musia všetky publikačné a verejné uzly vidieť rovnaké súbory. Databázové úložisko súborov nie je podporované.
- Proxy/CDN/statický server musia posielať `/files/protected/planned-changes/` a zodpovedajúce `/thumb/files/protected/planned-changes/` požiadavky cez WebJET a nesmú ich ukladať do verejnej cache. Zabezpečte rovnaké pravidlo aj pre alternatívne hosty a URL aliasy. Náhľad a `historyid` sa nesmú verejne cachovať.
- Originál aj miniatúra overujú oprávnenie redaktora k stránke, doméne a cieľovému priečinku. Neoprávnený používateľ dostane `404`; odpoveď obsahuje `Cache-Control: private, no-store`. Ani už vytvorená miniatúra kontrolu neobchádza.
- Existujúce vylúčenie `/files/protected/` z indexovania ponechajte zapnuté. Pred sprístupnením funkcie overte priamy originál, PDF, miniatúru a históriu bez prihlásenia aj po ich zobrazení administrátorom.
- Cron musí bežať na publikačnom/admin uzle. Termín označuje najskorší začiatok publikovania; dokončenie závisí od intervalu kontroly a veľkosti súborov. Otvorený editor ani návštevnosť nie sú potrebné.
- Projektové šablóny musia zachovať existujúce parametre URL perexového obrázka. Pri skladaní `/thumb/` URL nesmú za odkaz s `?v=...` pripojiť druhý otáznik; použite URL builder. Overte tiež odkazy s fragmentom a HTML kódovanými parametrami.
- Pri vypnutí funkcie sa existujúce plány s inicializovanými plánovanými médiami pozastavia. Ich chránené súbory sa nesprístupnia cez pôvodný publikačný postup. Po opätovnom zapnutí možno pokračovať.

Príklad kontroly úložiska pre MariaDB/MySQL:

```sql
SELECT TABLE_NAME, ENGINE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('documents', 'documents_history', 'media', 'media_group_to_media');
```

## Chyby publikovania

Pred prvou výmenou sa kontrolujú všetky odkazy, zdroje, ciele a aktuálne oprávnenia autora. Pri chýbajúcom súbore sa verejný stav nezmení. Jednotlivé súbory sa nahrádzajú atómovo, takže návštevník nedostane čiastočne zapísaný súbor. Pôvodné súbory sa uchovávajú v chránenom podpriečinku `<docId>/backup/<historyId>/`; správca ich môže obnoviť cez súborový systém. Použije sa aj existujúca história súborov, ak je zapnutá.

Zápis stránky, výmena médií a dokončenie histórie prebehnú v jednej databázovej transakcii. Súborový systém však nie je súčasťou tejto transakcie. Po chybe počas výmeny môže byť časť súborov už nová. Zdroje a pôvodná záloha zostávajú zachované a ďalšie spustenie môže dokončiť ten istý plán. Chyba sa zaznamená do logu spolu s ID histórie; plán sa nesmie označiť ako dokončený.

Pred produkčným použitím vykonajte akceptačný test na pripravovanej WebJET stránke Apple vrátane produkčnej proxy/CDN a oprávnení obmedzeného redaktora.
