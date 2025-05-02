# Zoznam zmien verzia 2025

## 2025-SNAPSHOT

> Vývojová verzia

### Prelomové zmeny

- Aplikácia Kalendár noviniek oddelená do samostatnej aplikácie, ak kalendár noviniek používate je potrebné upraviť cestu `/components/calendar/news_calendar.jsp` na `/components/news-calendar/news_calendar.jsp` (#57409).
- Aplikácia Archív súborov bola prerobená na Spring aplikáciu. Bližšie informácie nájdete v sekcii pre programátora (#57317).
- Aplikácia Elektronický obchod bola na `BE` časti prerobená. Bližšie informácie nájdete v sekcii pre programátora (#56609).

### Archív súborov

- **Zoznam súborov** prerobený do nového dizajnu s pridaním novej logiky oproti starej verzií. Viac sa dočítate v časti [Archív súborov](redactor/files/file-archive/README.md) (#57317).

![](redactor/files/file-archive/datatable_allFiles.png)

- **Manažér kategórií** opravený a prerobený do nového dizajnu. Viac sa dočítate v časti [Manažér kategórií](redactor/files/file-archive/category-manager.md) (#57317).
- **Manažér produktov** bol pridaný ako nová sekcia. Viac sa dočítate v časti [Manažér produktov](redactor/files/file-archive/product-manager.md) (#57317).
- **Export hlavných súborov** bol upravený tak, aby ponúkal širšie možnosti exportu súborov a zlepšil prehľadnosť výpisov. Viac sa dočítate v časti [Export hlavných súborov](redactor/files/file-archive/export-files.md) (#57317).

![](redactor/files/file-archive/export_all.png)

- **Import hlavných súborov** bol opravený a upravený, aby dokázal pracovať s rozšírenými možnosťami exportu. Viac sa dočítate v časti [Import hlavných súborov](redactor/files/file-archive/import-files.md) (#57317).

### Aplikácie

Prerobené nastavenie vlastností aplikácií v editore zo starého kódu v `JSP` na `Spring` aplikácie. Aplikácie automaticky získavajú aj možnosť nastaviť [zobrazenie na zariadeniach](custom-apps/appstore/README.md#podmienené-zobrazenie-aplikácie). Dizajn je v zhode so zvyškom WebJET CMS a dátových tabuliek (#57409).

- [Anketa](redactor/apps/inquiry/README.md)
- [Bannerový systém](redactor/apps/banner/README.md)
- [Dátum a čas, Dátum a meniny](redactor/apps/app-date/README.md) - zlúčené do jednej spoločnej aplikácie
- [Dotazníky](redactor/apps/quiz/README.md)
- [Hromadný e-mail](redactor/apps/dmail/form/README.md)
- [Kalendár udalostí](redactor/apps/calendar/README.md)
- [Kalendár noviniek](redactor/apps/news-calendar/README.md)

![](redactor/apps/dmail/form/editor.png)

### Hromadný e-mail

- **Presunuté pole Web stránka** – teraz sa nachádza pred poľom **Predmet**, aby sa po výbere stránky predmet automaticky vyplnil podľa názvu zvolenej web stránky (#57541).
- **Úprava poradia v karte Skupiny** – e-mailové skupiny sú teraz zobrazené pred skupinami používateľov (#57541).
- **Nové možnosti pre meno a e-mail odosielateľa** – ak sú konfiguračné premenné `dmailDefaultSenderName` a `dmailDefaultSenderEmail` nastavené, použijú sa tieto hodnoty. Ak sú prázdne, systém automaticky vyplní meno a e-mail aktuálne prihláseného používateľa. (#57541)
  - Pomocou týchto premenných je možné nastaviť **fixné hodnoty** (napr. názov spoločnosti) pre všetky [kampane](redactor/apps/dmail/campaings/README.md), bez ohľadu na to, kto je prihlásený.

![](redactor/apps/dmail/campaings/editor.png)

- Hromadný email - optimalizácia tvorby zoznamu príjemcov - karta [skupiny](redactor/apps/dmail/campaings/README.md#pridanie-zo-skupiny) presunutá do dialógového okna. Po zvolení skupiny príjemcov ich ihneď vidíte v karte Príjemcovia a viete ich ľahko upravovať, už nie je potrebné email najskôr uložiť pre zobrazenie príjemcov (#57537).

![](redactor/apps/dmail/campaings/users.png)

### Kalendár noviniek

- Kalendár noviniek oddelený ako samostatná aplikácia, pôvodne to bola možnosť v aplikácii Kalendár (#57409).
- Zobrazuje kalendár napojený na zoznam noviniek s možnosťou filtrovať novinky podľa zvoleného dátumu v kalendári.

![](redactor/apps/news-calendar/news-calendar.png)

### Rezervácie

- **Podpora pre nadmernú rezerváciu** – umožňuje administrátorom vytvoriť viac rezervácií `overbooking` na ten istý termín (#57405).
- **Vylepšená validácia pri importe** – teraz je možné importovať [rezervácie](redactor/apps/reservation/reservations/README.md) aj do minulosti, alebo vytvoriť `overbooking` rezervácie pri importe údajov (#57405).
- **Podpora pre pridanie rezervácie do minulosti** – umožňuje administrátorom vytvoriť rezervácie v minulosti (#57389).
- Do [rezervačných objektov](redactor/apps/reservation/reservation-objects/README.md) bol pridaný stĺpec **Emaily pre notifikácie**, ktorý pre každý zadaný platný email (oddelený čiarkou) odošle email ak bola rezervácia pridaná a schválená (#57389).
- Notifikáciám pre potvrdenie rezervácie a ďalším systémovým notifikáciám je možné nastaviť meno a email odosielateľa pomocou konfiguračných premenných `reservationDefaultSenderName,reservationDefaultSenderEmail` (#57389).
- Pridaná nová aplikácia [Rezervácia dní](redactor/apps/reservation/day-book-app/README.md), pre rezerváciu celodenných objektov na určitý interval pomocou integrovaného kalendára (#57389).

![](redactor/apps/reservation/day-book-app/app-table_B.png)

### Elektronický obchod

- Pridaná karta **Osobné informácie** do zoznamu objednávok - obsahuje podrobné informácie o **adrese doručenia** ako aj **kontaktné informácie** všetko na jednom mieste (#57685).
- Pridaná karta **Voliteľné polia** do zoznamu objednávok - [voliteľné polia](frontend/webpages/customfields/README.md) podľa potreby implementácie (#57685).
- Export zoznamu objednávok - doplnené stĺpce celková cena s DPH a počet položiek (#57685).
- Formulár pre objednanie - doplnená možnosť definovať dostupný zoznam krajín cez konfiguračnú premennú `basketInvoiceSupportedCountries` (#57685).
- Upravené zobrazenie údajov z karty **Osobné údaje** v zozname objednávok, ich logické rozdelenie do častí pre lepší prehľad (#57685).
- V zozname objednávok boli pridané stĺpce **Počet položiek**, **Cena bez DPH** a **Cena s DPH**. Hodnoty sa automaticky prepočítajú pri zmene položiek objednávky (#57685).
- V zozname objednávok prerobený výber krajiny cez výberové pole, ktorý ponúka iba krajiny zadefinované konštantou `basketInvoiceSupportedCountries` (#57685).

![](redactor/apps/eshop/invoice/editor_personal-info.png)

- Nová verzia [konfigurácie spôsobov platby](redactor/apps/eshop/payment-methods/README.md) a integrácie na platobné brány. Údaje sú oddelené podľa domén. Pridali sme podporu [platobnej brány GoPay](https://www.gopay.com), čo znamená aj akceptáciu platobných kariet, podporu `Apple/Google Pay`, platby cez internet banking, `PayPal`, `Premium SMS` atď. Okrem toho sú podporované platby prevodom a dobierka. Pre každý typ platby je možné nastaviť aj cenu, ktorá pri zvolení možnosti bude automaticky pripočítaná k objednávke. Nastavené spôsoby platby sa aj automaticky premietnu do možností pri vytváraní objednávky zákazníkom.

![](redactor/apps/eshop/payment-methods/datatable.png)

- Nová aplikácia Zoznam objednávok so zoznamom objednávok aktuálne prihláseného používateľa. Kliknutím na objednávku je možné zobraziť detail objednávky a stiahnuť ju v PDF formáte (#56609).

### Oprava chýb

- Hromadný email - pri duplikovaní kampane doplnené duplikovanie zoznamu príjemcov (#57533).
- Dátové tabuľky - import - upravená logiky **Preskočiť chybné záznamy** pri importe tak, aby sa pri tejto možnosti spracovali aj generické chyby `Runtime` a zabezpečilo sa dokončenie importu bez prerušenia. Tieto chyby sa následne zobrazia používateľovi pomocou notifikácie v priebehu importovania (#57405).
- Súbory - opravený výpočet veľkosti súborov/priečinkov v pätičke prieskumníka a pri zobrazení detailu priečinka (#57669).
- Autocomplete - opravená chyba pri poli typu `Autocomplete`, kde prvá získaná hodnota v prípade `jstree` nebola korektná (#57317).

### Pre programátora

- Pridaná možnosť vykonať [doplnkový HTML/JavaScript kód](custom-apps/appstore/README.md#doplnkový-html-kód) v Spring aplikácii s anotáciou `@WebjetAppStore` nastavením atribútu `customHtml = "/apps/calendar/admin/editor-component.html"` (#57409).
- V datatable editore pridaný typ poľa [IMAGE_RADIO](developer/datatables-editor/standard-fields.md#image_radio) pre výber jednej z možnosti pomocou obrázka (#57409).
- Pridaný typ poľa `UPLOAD` pre [nahratie súboru](developer/datatables-editor/field-file-upload.md) v editore datatabuľky (#57317).
- Pri inicializácii [vnorenej datatabuľky](developer/datatables-editor/field-datatable.md) pridaná možnosť upraviť `columns` objekt zadaním JavaScript funkcie do atribútu `data-dt-field-dt-columns-customize` anotácie (#57317).
- Pridaná podpora pre získanie mena a emailu odosielateľa pre rôzne emailové notifikácie použitím `SendMail.getDefaultSenderName(String module, String fallbackName), getDefaultSenderEmail(String module, String fallbackEmail)` (#57389).
- Prerobená aplikácia Manažér dokumentov (Archív súborov) na Spring aplikáciu. Ak používate pôvodnú verziu a chcete ju zachovať, musíte pridať späť súbory `/components/file_archiv/file_archiv.jsp` a `components/file_archiv/editor_component.jsp` a potrebné triedy zo [staršej verzie WebJET CMS](https://github.com/webjetcms/webjetcms/tree/release/2025.0/src/webjet8/java/sk/iway/iwcm/components/file_archiv).
- Manažér dokumentov (Archív súborov) - upravené API `FileArchivatorBean.getId()/getReferenceId()/saveAndReturnId()` vráti `Long`, môžete použiť `getFileArchiveId()` pre vrátanie `int` hodnoty. Zmazané nepoužívané metódy, v prípade ich potreby ich preneste do vašich tried. Neodporúčame modifikovať WebJET triedy, vytvorte si nové triedy typu `FileArchivatorProjectDB` vo vašom projekte kde metódy pridáte. Ak sme zmazali celú triedu, ktorú používate (napr. `FileArchivatorAction`), môžete si ju priamo pridať do vášho projektu (#57317).
- Pridané automatické nastavenie filtrovania stĺpca na hodnotu `false`, v prípade že hodnota je `null` (nenastavená) a ide o stĺpec, ktorý je vnorený, ako napr `editorFields` stĺpce (#57685).
- Pridaná možnosť [špeciálneho usporiadania](developer/datatables/restcontroller.md#usporiadanie) prepísaním metódy `DatatableRestControllerV2.addSpecSort(Map<String, String> params, Pageable pageable)` (#57685).
- Pridaná možnosť v anotácii `@DataTableColumn` nastaviť atribút `orderProperty` ktorý určí [stĺpce pre usporiadanie](developer/datatables/restcontroller.md#usporiadanie), napr. `orderProperty = "contactLastName,deliverySurName"`. Výhodné pre `EditorFields` triedy, ktoré môžu agregovať dáta z viacerých stĺpcov (#57685).
- Prerobená aplikácia Elektronický obchod na `BE` časti. Nakoľko sa využívajú už nové triedy, pre správne fungovanie musíte:
  - využiť aktualizačný skript `/admin/update/update-2023-18.jsp` pre základnú aktualizáciu vašich JSP súborov
  - nakoľko sa teraz využíva typ `BigDecimnal` namiesto `float`, musíte naviac upraviť všetky porovnania týchto hodnôt. Typ `BigDecimal` sa neporovnáva klasicky pomocou `<, =, >` ale pomocou `BigDecimal.compareTo( BigDecimal )`
  - musíte odstrániť volania súborov, alebo spätne pridať všetky súbory, ktoré boli odstránené, nakoľko sa nevyužívali

### Testovanie

- Média - doplnený test vkladania médii vo web stránke ak používateľ nemá právo na všetky média (#57625).
- Web stránky - doplnený test vytvorenia novej stránky s publikovaním v budúcnosti (#57625).
- Galéria - doplnený test vodoznaku s porovnaním obrázku, doplnený test kontroly práv (#57625).
- Web stránky - doplnený test voliteľných polí pri vytváraní web stránky (#57625).

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