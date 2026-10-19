# Plánované publikovanie s chránenými súbormi

Budúcu verziu existujúcej stránky môžete pripraviť aj s obrázkami a dokumentmi, ktoré do termínu zverejnenia nesmú byť dostupné návštevníkom. Živý obsah a bežná pracovná verzia zostávajú samostatné. Funkciu musí správca zapnúť konfiguračnou premennou `plannedPublishingEnabled=true`; predvolene je vypnutá.

## Príprava stránky

1. Otvorte existujúcu stránku. Úplne novú stránku najskôr uložte ako nezverejnenú.
2. V karte **Perex** nastavte **Dátum začiatku** a zapnite **Naplánovať zmenu stránky po tomto dátume**. WebJET jednorazovo skopíruje živé médiá do plánovanej sady.
3. Nové podklady nahrajte vo výbere súborov do **Médiá tejto stránky → Obrázky - plánované** alebo **Súbory - plánované**. Tieto priečinky sú dostupné aj pri výbere perexového obrázka a odkazov v karte **Médiá**.
4. Upravte obsah a plánované médiá. Kliknite na **Naplánovať**.
5. Pri ďalšej úprave použite **Otvoriť plánovanú verziu** v karte Perex alebo otvorte príslušnú verziu cez Históriu. Opätovné otvorenie už nekopíruje živé médiá.

Na jednu stránku je podporovaný jeden plán. Zmena termínu nahradí pôvodné naplánovanie. Plánovaná verzia pri publikovaní nahradí obsah a celú sadu médií vrátane ich poradia a skupín. Novšie opravy živej stránky preto do pripravovanej verzie preneste ručne. Ak odstránite všetky plánované médiá, po publikovaní bude živá sada prázdna.

Zmeny médií sa ukladajú priebežne. Zatvorenie editora cez **Zrušiť** ich nevracia. Odškrtnutie časovania pripravené údaje neodstráni. Tlačidlo **Zrušiť plánované zmeny** zruší naplánovanie, odstráni plánované médiá a vymaže všetky obrázky a súbory z plánovaných priečinkov vrátane podpriečinkov. HTML živej stránky ani záznamov histórie nemení. Odkazy na vymazané podklady preto v zrušenej histórii už nebudú fungovať.

## Súbory a odkazy

Príklad pre stránku s ID `42`:

```text
/files/protected/planned-changes/42/images/apple/foto.webp
→ /images/apple/foto.webp

/files/protected/planned-changes/42/files/apple/parametre.pdf
→ /files/apple/parametre.pdf
```

Cieľ je uložený v ceste súboru, preto ho neskoršie premenovanie stránky nezmení. **Publikujú sa všetky súbory z priečinkov Obrázky - plánované a Súbory - plánované vrátane podpriečinkov**, aj keď na ne stránka ani médiá neodkazujú. Po úspešnom uložení stránky, médií a histórie sa tieto pracovné priečinky vyčistia. Ďalší plán tak začína s prázdnymi priečinkami.

Podporované sú obsah, anotácia, perexový obrázok, `htmlHead`, voliteľné polia a odkazy, náhľady či popisy médií. Pri publikovaní sa každý výskyt prefixu `/files/protected/planned-changes/<docId>/` danej stránky nahradí za `/`. Náhrada funguje aj v absolútnych URL, `/thumb/`, CSS a parametroch `!INCLUDE(...)!` bez rozlišovania domény či HTML atribútov. Existujúce parametre vrátane `v` a fragmenty sa zachovajú bez zmeny; nové parametre sa nepridávajú. Rovnaký prepis sa uloží aj do konkrétneho publikovaného záznamu histórie. Ostatné záznamy histórie sa nemenia; ich pôvodné odkazy do vyčistených plánovaných priečinkov môžu prestať fungovať.

Dialóg **Vlastnosti obrázka** pri výbere obrázka nastavuje parameter `v` podľa času poslednej zmeny súboru. Pri nahradení obrázka ho v dialógu vyberte znova, aby sa nová hodnota dostala do HTML. Pri zapnutom `elfinderUseFastLoading` sa čas zmeny nečíta a hodnota je `0`; vtedy použite nový názov súboru.

Pre perexové obrázky používajte pri výmene **nový názov súboru**. Ich URL bežne neobsahuje parameter `v`, preto pri zachovaní rovnakého názvu môže prehliadač alebo CDN aj po publikovaní zobrazovať starý obrázok z cache. Rovnaké obmedzenie platí pre ostatné odkazy bez zmeneného parametra `v`. WebJET pri prepísaní súboru vyčistí vlastnú cache súboru a miniatúr, cache prehliadača ani CDN tým však nevymaže.

Nerozpoznané použitie chránenej URL sa odmietne pri ukladaní alebo publikovaní. Externé CSS/JS, iframe a ich závislosti sa neprechádzajú. Produktový prehliadač a e-shop potrebujú vlastný publikačný postup.

Prepísanie existujúceho súboru ovplyvní všetky stránky používajúce rovnakú cestu. Pri ukladaní a publikovaní sa kontroluje, či už iný plán nepoužíva rovnaký cieľový súbor. Táto kontrola nezamyká databázu, preto nezaručuje odhalenie kolízie pri súčasnom ukladaní dvoch plánov. Už verejne nahraný súbor nie je možné týmto postupom spätne utajiť. Ani odverejnenie stránky automaticky neskryje predtým publikované súbory.

## Nasadenie a prevádzka

- Databázová aktualizácia pridáva stĺpec `documents.planned_media_initialized` s predvolenou hodnotou `false`. Nová tabuľka nevzniká.
- Podporované sú aj tabuľky **MyISAM**. Funkcia nevyžaduje InnoDB ani konverziu existujúcich tabuliek. Publikovanie sa nespolieha na rollback celej operácie; pri chybe môže zostať časť zmien zapísaná a ďalší pokus pokračuje podľa uloženého stavu.
- Plánované aj verejné súbory musia byť na spoločnom súborovom systéme podporujúcom atómové premenovanie. V klastri musia všetky publikačné a verejné uzly vidieť rovnaké súbory. Databázové úložisko súborov nie je podporované.
- Proxy/CDN/statický server musia posielať `/files/protected/planned-changes/` a zodpovedajúce `/thumb/files/protected/planned-changes/` požiadavky cez WebJET a nesmú ich ukladať do verejnej cache. Zabezpečte rovnaké pravidlo aj pre alternatívne hosty a URL aliasy. Náhľad a `historyid` sa nesmú verejne cachovať.
- Originál aj miniatúra sú dostupné prihlásenému redaktorovi s právom zápisu do cieľového verejného priečinka. Neoprávnený používateľ dostane `404`; odpoveď obsahuje `Cache-Control: private, no-store`. Ani už vytvorená miniatúra kontrolu neobchádza.
- Upload, úpravy obrázkov a kontrola zápisu v prieskumníkovi súborov používajú právo na zápis do cieľového verejného priečinka. Pri presune obrázka sa kontroluje pôvodný aj nový priečinok. Prístup k súborom nevyžaduje právo upravovať príslušnú stránku. Editácia a náhľad samotnej stránky naďalej overujú jej oprávnenia.
- Existujúce vylúčenie `/files/protected/` z indexovania ponechajte zapnuté. Pred sprístupnením funkcie overte priamy originál, PDF, miniatúru a históriu bez prihlásenia aj po ich zobrazení administrátorom.
- Schvaľovanie používa existujúci postup editora. Schválenie pred termínom ponechá verziu naplánovanú; schválenie po termíne rovno spustí publikovanie vrátane súborov, médií a prepisu ciest.
- Publikovanie spúšťa existujúca kontrola cez `DocDB.getInstance()`, napríklad pri zobrazení stránky, najviac raz za päť sekúnd. Bez volania tejto kontroly sa publikovanie odloží do ďalšieho volania po nastavenom termíne. Dokončenie závisí aj od veľkosti súborov.
- V rámci jednej inštancie aplikácie sa prekrývajúce publikačné volanie pre tú istú stránku preskočí. Táto ochrana nekoordinuje viac uzlov klastra ani úpravy či rušenie plánu počas publikovania. Súbežné publikovanie z viacerých uzlov nie je týmto mechanizmom zabezpečené.
- Pri vypnutí funkcie sa existujúce plány s inicializovanými plánovanými médiami pozastavia. Ich chránené súbory sa nesprístupnia cez pôvodný publikačný postup. Po opätovnom zapnutí možno pokračovať.

## Chyby publikovania

Pred prvou výmenou sa kontrolujú súbory nájdené v plánovaných priečinkoch, ich ciele a aktuálne oprávnenia autora. Zoznam na kopírovanie vzniká výhradne z obsahu priečinkov; existencia súborov uvedených v odkazoch sa nekontroluje. Jednotlivé súbory sa nahrádzajú atómovo, takže návštevník nedostane čiastočne zapísaný súbor. Pôvodné súbory sa uchovávajú v chránenom podpriečinku `<docId>/backup/<historyId>/`; správca ich môže obnoviť cez súborový systém. Použije sa aj existujúca história súborov, ak je zapnutá.

Najprv sa publikujú súbory, potom obsah stránky a médiá. Zápisy neprebiehajú v jednej spoločnej transakcii. Pri chybe preto môžu byť súbory alebo obsah už nové a živá sada médií môže byť dočasne prázdna alebo neúplná.

Plánované médiá vrátane pôvodných chránených odkazov zostávajú zachované, kým sa neuložia všetky živé kópie. Ak výmena zlyhá, ďalší pokus vymaže neúplnú živú sadu a vytvorí ju znovu z kompletného plánu. Po uložení celej sady sa vynuluje `planned_media_initialized`, odstránia sa plánované záznamy médií a až potom sa história označí za publikovanú. Ak zlyhá tento záverečný krok, ďalší pokus ponechá dokončenú živú sadu a dokončí upratanie a históriu. Pracovné súbory sa vymažú až po úspešnom dokončení; zálohy pôvodných verejných súborov v `backup/<historyId>/` zostávajú na disku. Ak zlyhá samotné čistenie priečinkov, publikovanie zostáva dokončené a chyba sa zapíše do logu. Zvyšné pracovné súbory možno odstrániť tlačidlom **Zrušiť plánované zmeny**.

Rovnako sa dá zopakovať inicializácia: pokiaľ príznak ešte nie je nastavený, prípadné neúplné plánované kópie sa najprv odstránia a živé médiá sa skopírujú znova. Pri zmene plánu sa pôvodné naplánovanie zruší až po uložení novej verzie do histórie. Chyba publikovania sa zaznamená do logu spolu s ID histórie.

Pred produkčným použitím vykonajte akceptačný test na pripravovanej WebJET stránke Apple vrátane produkčnej proxy/CDN a oprávnení obmedzeného redaktora.
