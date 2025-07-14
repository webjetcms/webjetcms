# Zoznam zmien verzia 2025

## 2025-SNAPSHOT

> Vývojová verzia

## 2025.0.x

> Opravná verzia pôvodnej verzie 2025.0.

- PDF - opravené nastavenie cesty do `fonts` priečinka s písmami. Je potrebné zadať plnú cestu na disku servera (#57657).
- Aktualizovaná knižnica `Apache Commons BeanUtils` na verziu 1.11.0.
- Inicializácia - doplnená kontrola existencie súboru `autoupdate.xml`, aby na verejných uzloch clustra nepísalo chybu pri štarte WebJETu (#54273-68).
- Bezpečnosť - doplnená kontrola výrazu `onwebkit` pri URL parametroch pre zamedzenie vykonania XSS útoku (#54273-68).
- Pole typu `QUILL` (malý HTML editor používaný v Banneroch, Galérii...) - opravené duplikovanie `P` elementu ak obsahuje CSS triedu alebo iný atribút (#54273-69).
- Bezpečnosť - v anotácii `@AllowSafeHtmlAttributeConverter` povolené vkladanie atribútov `alt,title` pre `img` a `class` pre elementy `a,img,div,span,p,h1,h2,h3,h4,h5,h6,i,b,strong,em` (#54273-69).
- Bezpečnosť - aktualizovaná knižnica `hibernate-validator` na verziu `6.2.5.Final` (#54273-69).
- Administrácia - pridaná možnosť vkladať [doplnkový CSS/JavaScript](custom-apps/apps/customize-admin.md) súbor do administračnej časti, napr. pre vlastné CSS štýly pre [pole typu Quill](developer/datatables-editor/standard-fields.md#quill) (#54273-69).
- Dátové tabuľky - pre Oracle a Microsoft SQL vypnutá možnosť usporiadania podľa stĺpcov obsahujúcich dlhý text (`ntext/clob`) - tieto databázové systémy nepodporujú usporiadanie v prípade použitia tohto dátového typu. Atribút musí v `Entite` mať anotáciu `@Lob`, ktorá pre uvedené databázy vypne možnosť usporiadania pre daný stĺpec. Pre MariaDB a PostgreSQL je usporiadanie stále podporované (#54273-70).
- Dátové tabuľky - opravené vyhľadávanie ak v jednom poli zvolíte možnosť "Začína na" a v inom poli napr. "Končí na" (#54273-70).
- Dátové tabuľky / vyhľadávanie v administrácii - povolené špeciálne znaky (napr. úvodzovky) pre vyhľadávanie v dátových tabuľkách (#54273-70).
- Formuláre - schované zbytočné tlačidlo na vytvorenie nového záznamu v zozname vyplnených formulárov (#54273-70).
- Webové stránky - doplnená možnosť vkladať HTML kód do názvov priečinkov ako napríklad `WebJET<sup>TM</sup>` - v zozname webových stránok sa z dôvodu bezpečnosti HTML kód nevykoná, ale v aplikáciach ako Menu a navigačná lišta sa HTML kód zobrazí korektne a vykoná sa. Dôležitá podmienka je, aby kód obsahoval uzatváraciu značku `</...>`. HTML kód je odstránený aj z automaticky generovanej URL adresy. Povolený je len bezpečný HTML kód povolený v triede `AllowSafeHtmlAttributeConverter` (#54273-70).

## 2025.0.23

> Opravná verzia pôvodnej verzie 2025.0.

### Oprava chýb

- Dátové tabuľky - opravené chybné zobrazenie kariet, ktoré sa nemajú zobrazovať pri vytváraní nového záznamu (napr. v šablónach) (#57533).
- Dátové tabuľky - doplnený limit počtu záznamov pri zobrazení všetky. Hodnota je zhodná s maximálnym počtom riadkov pre exportu, nastavuje sa v konfiguračnej premennej `datatablesExportMaxRows` (#57657-2).
- Dátové tabuľky - opravený počet záznamov na strane keď stránka obsahuje navigačné karty (#57725-1).
- Dátové tabuľky - opravený nadpis Duplikovať namiesto Upraviť pri duplikovaní záznamu, upravená ikona tlačidla na duplikovanie (#57725-3).
- Dátové tabuľky - zjednotený názov `ID` stĺpca z pôvodných `ID, Id, id` na zjednotený `ID`. Pre `DataTableColumnType.ID` nie je potrebné nastaviť `title` atribút, automaticky sa použije kľúč `datatables.id.js`. Niektoré prekladové kľúče zmazané, keďže nie sú potrebné (#49144)
- Editor obrázkov - pri editácii obrázku zo vzdialeného servera doplnená notifikácia o potrebe stiahnutia obrázka na lokálny server (#57657-2).
- Web stránky - opravené vloženie bloku obsahujúce aplikáciu (#57657-2).
- Web stránky - doplnený `ninja` objekt pri vkladaní aplikácie do novej web stránky (#57389).
- Web stránky - stránky v koši sa už nebudú zobrazovať v karte Neschválené, ak schvaľovateľ klikne na odkaz v emaile zobrazí sa chyba Stránka je v koši, aby sa náhodou neschválila stránka, ktorá bola medzi tým zmazaná (#54273-62).
- Web stránky - schvaľovanie - opravené načítanie zoznamu v karte Neschválené pri použití databázového servera `Oracle` (#54273-62).
- Web stránky - opravená aktualizácia nodov clustra pri zmene značiek (#57717).
- Web stránky - opravené zobrazenie zoznamu stránok ak má používateľ právo iba na vybrané webové stránky (#57725-4).
- Web stránky - doplnený prepínač domén aj keď nie je nastavená konfiguračná premenná `enableStaticFilesExternalDir` ale len `multiDomainEnabled` (#57833).
- Aplikácie - opravené zobrazenie karty prekladové kľúče pri použití komponenty `editor_component_universal.jsp` (#54273-57).
- Aplikácie - pridaná podpora vkladania nového riadku cez klávesovú skratku `SHIFT+ENTER` do jednoduchého textového editora používaného napr. v Otázky a odpovede (#57725-1).
- Číselníky - presunutý výber číselníka priamo do nástrojovej lišty dátovej tabuľky (#49144).
- Novinky - presunutý výber sekcie priamo do nástrojovej lišty dátovej tabuľky (#49144).
- Prihlásenie - opravená chyba prihlásenia pri exspirovaní časovej platnosti hesla (#54273-57).
- Prihlásenie - opravené prihlásenie v multiweb inštalácii (#54273-57).
- GDPR - opravené zobrazenie karty Čistenie databázy pri použití `Oracle/PostgreSQL` databázy (#54273-57).
- Archív súborov - opravené zobrazenie ikon v dialógu dátumu a času (#54273-57).
- Bezpečnosť - aktualizovaná knižnica `Swagger UI` na verziu `5.20.0`, doplnené výnimky v `dependency-check-suppressions.xml`.
- Aktualizácia - doplnené mazanie nepotrebných súborov pri aktualizácii rozbalenej verzie (#57657-4).
- Multiweb - doplnená kontrola `ID` domény pri registrácii návštevníka web sídla (#57657-4).
- Používatelia - pridaná možnosť vybrať aj Koreňový priečinok v právach používateľa v sekcii Nahrávanie súborov do adresárov (54273-60).
- Používatelia - upravené nastavenie práv - zjednodušené nastavenie práv administrátorov a registrovaných používateľov (už nie je potrebné zvoliť aj právo Používatelia), opravené duplicitné položky, upravené zoskupenie v sekcii Šablóny (#57725-4).
- Prieskumník - doplnené lepšie hlásenia pri chybe vytvorenia ZIP archívu (#56058).
- Štatistika - opravené vytvorenie tabuľky pre štatistiku kliknutí v teplotnej mape.
- Prekladač - implementácia inteligentného oneskorenia pre prekladač `DeepL` ako ochrana proti chybe `HTTP 429: too many requests`, ktorá spôsobovala výpadok prekladov (#57833).
- Klonovanie štruktúry - opravené nechcené prekladanie implementácie aplikácií `!INCLUDE(...)!`, pri automatickom preklade tela stránky (#57833).
- Klonovanie štruktúry - pridaný preklad perex anotácie automatickom preklade stránok (#57833).
- Prieskumník - opravené práva nastavenia vlastností priečinku a súboru (#57833).
- Monitorovanie servera - opravené hlásenie o nastavení konfiguračnej premennej pre Aplikácie, WEB stránky a SQL dotazy (#57833).
- Úvod - opravené zobrazenie požiadavky na dvojstupňové overovanie pri integrácii cez `IIS` (#57833).
- Klonovanie/zrkadlenie štruktúry - opravené nastavenie URL adresy priečinku (odstránenie diakritiky a medzier) (#57657-7).
- Galéria - doplnené chýbajúce značky (#57837).
- Značky - opravené nastavenie priečinkov existujúcej značky v sekcii Zobraziť pre (#57837).

### Bezpečnosť

- Aktualizovaná knižnica `Apache POI` na verziu 5.4.1 pre opravu zraniteľností `CVE-2025-31672`.

## 2025.0

> Vo verzii **2025.0** sme priniesli **nový dizajn administrácie** pre ešte lepšiu prehľadnosť a používateľský komfort.
>
> Jednou z hlavných zmien je presunutie **druhej úrovne menu** do **kariet v hlavičke stránky**, čím sa zjednodušila navigácia. Vo webových stránkach sme tiež **zlúčili karty priečinkov a webových stránok**, aby ste mali všetko prehľadne na jednom mieste. Ak hlavička neobsahuje karty, tabuľky sa automaticky prispôsobia a zobrazia **riadok navyše**.
>
> Prosíme vás o spätnú väzbu prostredníctvom **formulára Spätná väzba**, ak pri používaní novej verzie identifikujete **akýkoľvek problém so zobrazením**. Pripomienku môžete doplniť aj o **fotku obrazovky**, čo nám pomôže rýchlejšie identifikovať a vyriešiť prípadné nedostatky.
>
> Ďakujeme za spoluprácu a pomoc pri vylepšovaní WebJET CMS!

### Prelomové zmeny

- Web stránky - zrušená inline editácia. Možnosť priamej editácie stránky v režime jej zobrazenia bola odstránená, keďže využívala staršiu verziu editora, ktorá už nie je podporovaná. Ako alternatívu je možné aktivovať nástrojový panel zobrazovaný v pravom hornom rohu webovej stránky. Tento panel umožňuje rýchly prístup k editoru web stránky, priečinka alebo šablóny. Môžete vypnúť alebo zapnúť pomocou konfiguračnej premennej `disableWebJETToolbar`. Po aktivácii sa začne zobrazovať na web stránke po vstupe do sekcie Webové stránky v administrácii (#57629).
- Prihlásenie - pre administrátorov nastavená [požiadavka na zmenu hesla](sysadmin/pentests/README.md#pravidlá-hesiel) raz za rok. Hodnotu je možné upraviť v konfiguračnej premennej `passwordAdminExpiryDays`, nastavením na hodnotu 0 sa kontrola vypne (#57629).
- Úvod - pridaná požiadavka na aktivácii dvojstupňového overovania pre zvýšenie bezpečnosti prihlasovacích údajov. Výzva sa nezobrazuje, ak je overovanie riešené cez `LDAP` alebo ak je prekladový kľúč `overview.2fa.warning` nastavený na prázdnu hodnotu (#57629).

### Dizajn

Vo verzii **2025.0** sme priniesli vylepšený **dizajn administrácie**, ktorý je prehľadnejší a efektívnejší.

**Upravený prihlasovací dialóg** – nové pozadie a presunutie prihlasovacieho dialógu na pravú stranu. Na **prihlásenie** je možné použiť nielen prihlasovacie meno ale **už aj email adresu**.
![](redactor/admin/logon.png)

**Prehľadnejšia hlavička** – názov aktuálnej stránky alebo sekcie sa teraz zobrazuje priamo v hlavičke.

**Nová navigácia v ľavom menu** – pod položky už nie sú súčasťou ľavého menu, ale zobrazujú sa **ako karty v hornej časti** stránky.
![](redactor/admin/welcome.png)

**Zlúčené karty v sekcii Webové stránky** – prepínanie typov priečinky a typov web stránok sa teraz zobrazujú v spoločnej časti, čím sa zjednodušila navigácia. **Výber domény** bol presunutý na spodnú časť ľavého menu.
  ![](redactor/webpages/domain-select.png)

**Preorganizované menu položky**:

- **SEO** presunuté do sekcie **Prehľady**.
- **GDPR a Skripty** presunuté do sekcie **Šablóny**.
- **Galéria** je teraz v sekcii **Súbory**.
- Niektoré názvy položiek boli upravené, aby lepšie vystihovali ich funkciu.

### Web stránky

- Pridaná možnosť nastaviť inkrement poradia usporiadania pre priečinky v konfiguračnej premennej `sortPriorityIncrementGroup` a web stránky v konfiguračnej premennej `sortPriorityIncrementDoc`. Predvolené hodnoty sú 10 (#57667-0).

### Testovanie

- Štandardné heslo pre `e2e` testy sa získa z `ENV` premennej `CODECEPT_DEFAULT_PASSWORD` (#57629).

### Oprava chýb

- Webové stránky - vkladanie odkazov na súbor v PageBuilder (#57649).
- Webové stránky - doplnené informácie o odkaze (typ súboru, veľkosť) do atribútu Pomocný titulok `alt` (#57649).
- Webové stránky - opravené nastavenie poradia usporiadania web stránok pri použití `Drag&Drop` v stromovej štruktúre (#57657-1).
- Webové stránky - pri duplikovaní webovej stránky/priečinka sa nastaví hodnota `-1` do poľa Poradie usporiadania pre zaradenie na koniec zoznamu. Hodnotu `-1` môžete zadať aj manuálne pre získanie novej hodnoty poradia usporiadania (#57657-1).
- Webové stránky - import webových stránok - opravené nastavenie média skupín pri importe stránok obsahujúcich média. Pri importe sa automaticky vytvoria všetky Média skupiny (aj nepoužívané) z dôvodu, že sa pri importe stránok prekladá aj média skupina nastavená pre média aplikáciu `/components/media/media.jsp` v stránke (tá môže obsahovať aj ID média skupiny mimo importovaných stránok) (#57657-1).
- Firefox - znížená verzia sady `Tabler Icons` na `3.0.1`, pretože Firefox pri použití novších verzií výrazne zaťažuje procesor. Optimalizované čítanie CSS štýlu `vendor-inline.style.css` (#56393-19).

Zvyšný zoznam zmien zmien je zhodný s verziou [2024.52](CHANGELOG-2024.md).

![meme](_media/meme/2025-0.jpg ":no-zoom")
