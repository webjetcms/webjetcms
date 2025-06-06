# WebJET CMS 2025

Vítejte v dokumentaci k WebJET CMS verze 2025. Doporučujeme přečíst si [seznam změn](CHANGELOG-2025.md) a [roadmap](ROADMAP.md).

## Seznam změn ve verzi 2025.18

> Verze **2025.18** přináší kompletně předělaný modul **Elektronického obchodu** s podporou **platební brány GoPay** a vylepšeným seznamem objednávek. Aplikace **Kalendář novinek** byla oddělena jako **samostatná aplikace** a zároveň jsme předělali nastavení více aplikací v editoru stránek do nového designu. **Manažer dokumentů** (původně Archiv souborů) prošel **vizuálním i funkčním restartem** včetně nových nástrojů pro správu, export a import dokumentů.
>
> Vylepšen byl i systém **Hromadného e-mailu** s novými možnostmi pro odesílatele a pohodlnějším výběrem příjemců. **Rezervace** získali nové možnosti jako**nadměrné rezervace**, vytváření rezervací zpětně do minulosti a zasílání notifikací na specifické emaily pro každý rezervační objekt.
>
> Optimalizovali jsme počet souborů v **Průzkumníku**, což vede k **rychlejšímu načítání** a přidali nové informace do **Monitorování serveru**.

### Průlomové změny

- Aplikace Kalendář novinek oddělena do samostatné aplikace, pokud kalendář novinek používáte je třeba upravit cestu `/components/calendar/news_calendar.jsp` na `/components/news-calendar/news_calendar.jsp` (#57409).
- Upravená inicializace Spring a JPA, více informací v sekci pro programátora (#43144).
- Předělaná backend část aplikace elektronický obchod, více v sekci pro programátora (#57685).

### Datové tabulky

- Při nastavení filtru číselné hodnoty od-do se pole zvětší pro lepší zobrazení zadané hodnoty podobně jako to dělá datové pole (#57685).
- Aplikace Archiv souborů byla předělána na Spring aplikaci. Bližší informace naleznete v sekci pro programátora (#57317).
- Aplikace Elektronický obchod byla na `BE` části předělaná. Bližší informace naleznete v sekci pro programátora (#56609).

### Manažer dokumentů (Archiv souborů)

- **Seznam souborů** předělaný do nového designu s přidáním nové logiky oproti staré verzi. Více se dočtete v části [Archiv souborů](redactor/files/file-archive/README.md) (#57317).

![](redactor/files/file-archive/datatable_allFiles.png)

- **Manažer kategorií** opraven a předělán do nového designu. Více se dočtete v části [Manažer kategorií](redactor/files/file-archive/category-manager.md) (#57317).
- **Manažer produktů** byl přidán jako nová sekce. Více se dočtete v části[Manažer produktů](redactor/files/file-archive/product-manager.md) (#57317).
- **Export hlavních souborů** byl upraven tak, aby nabízel širší možnosti exportu souborů a zlepšil přehlednost výpisů. Více se dočtete v části [Export hlavních souborů](redactor/files/file-archive/export-files.md) (#57317).

![](redactor/files/file-archive/export_all.png)

- **Import hlavních souborů** byl opraven a upraven, aby dokázal pracovat s rozšířenými možnostmi exportu. Více se dočtete v části [Import hlavních souborů](redactor/files/file-archive/import-files.md) (#57317).
- **Indexování** dokumentů ve vyhledávačích typu `Google` upraveno tak, aby se neindexovaly staré/historické verze dokumentů a dokumenty mimo datum platnosti (nastavená HTTP hlavička `X-Robots-Tag=noindex, nofollow`). Indexování těchto dokumentů lze povolit v editoru v manažerovi dokumentů (#57805).

### Aplikace

Předěláno nastavení vlastností aplikací v editoru ze starého kódu v `JSP` na `Spring` aplikace. Aplikace automaticky získávají i možnost nastavit [zobrazení na zařízeních](custom-apps/appstore/README.md#podmíněné-zobrazení-aplikace). Design je ve shodě se zbytkem WebJET CMS a datových tabulek (#57409).
- [Anketa](redactor/apps/inquiry/README.md)
- [Bannerový systém](redactor/apps/banner/README.md)
- [Datum a čas, Datum a svátek](redactor/apps/app-date/README.md) - sloučeno do jedné společné aplikace
- [Dotazníky](redactor/apps/quiz/README.md)
- [Hromadný e-mail](redactor/apps/dmail/form/README.md)
- [Kalendář událostí](redactor/apps/calendar/README.md)
- [Kalendář novinek](redactor/apps/news-calendar/README.md)
- [Mapa stránek](redactor/apps/sitemap/README.md)
- [Média](redactor/webpages/media.md)
- [Příbuzné stránky](redactor/apps/related-pages/README.md)
- [Rating](redactor/apps/rating/README.md)
- [Rezervace](redactor/apps/reservation/reservation-app/README.md)

![](redactor/apps/dmail/form/editor.png)

- Zrychlené načtení dat aplikace v editoru - data jsou vložena přímo ze serveru, není třeba provést volání REST služby (#57673).
- Upravený vizuál - název aplikace při vkládání do stránky přesunut do hlavního okna (namísto původního nadpisu Aplikace) pro zvětšení velikosti plochy pro nastavení aplikaci (#57673).

![](redactor/apps/menu/editor-dialog.png)

- Doplněny fotky obrazovky aplikací v české jazykové mutaci pro většinu aplikací (#57785).

### Hromadný e-mail
- **Přesunuté pole Web stránka** – nyní se nachází před polem **Předmět**, aby se po výběru stránky předmět automaticky vyplnil podle názvu zvolené web stránky (#57541).
- **Úprava pořadí v kartě Skupiny** – e-mailové skupiny jsou nyní zobrazeny před skupinami uživatelů (#57541).
- **Nové možnosti pro jméno a e-mail odesílatele** – jsou-li konfigurační proměnné `dmailDefaultSenderName` a `dmailDefaultSenderEmail` nastaveno, použijí se tyto hodnoty. Pokud jsou prázdné, systém automaticky vyplní jméno a e-mail aktuálně přihlášeného uživatele. (#57541)
  - Pomocí těchto proměnných lze nastavit **fixní hodnoty** (např. název společnosti) pro všechny [kampaně](redactor/apps/dmail/campaings/README.md), bez ohledu na to, kdo je přihlášen.

![](redactor/apps/dmail/campaings/editor.png)

- Hromadný email - optimalizace tvorby seznamu příjemců - karta [skupiny](redactor/apps/dmail/campaings/README.md#přidání-ze-skupiny) přesunuta do dialogového okna. Po zvolení skupiny příjemců je ihned vidíte v kartě Příjemci a umíte je snadno upravovat, již není potřeba email nejprve uložit pro zobrazení příjemců (#57537).

![](redactor/apps/dmail/campaings/users.png)

- Odhlášení - při přímém zadání emailu na odhlášení (ne kliknutí na odkaz v emailu) je zaslán na zadanou email adresu potvrzující email. V něm je třeba kliknout na odkaz pro odhlášení. Původní verze nekontrolovala žádným způsobem platnost/vlastnictví email adresy a bylo možné odhlásit i cizí email (#57665).

### Kalendář novinek

- Kalendář novinek oddělen jako samostatná aplikace, původně to byla možnost v aplikaci Kalendář (#57409).
- Zobrazuje kalendář napojený na seznam novinek s možností filtrovat novinky podle zvoleného data v kalendáři.

![](redactor/apps/news-calendar/news-calendar.png)

### Monitorování serveru

- Doplněna tabulka s informací o databázových spojeních a obsazené paměti (#54273-61).
- Doplněna informace o verzi knihoven`Spring (Core, Data, Security)` do sekce Monitorování serveru-Aktuální hodnoty (#57793).

### Rezervace

- **Podpora pro nadměrnou rezervaci** – umožňuje administrátorům vytvořit více rezervací `overbooking` na tentýž termín (#57405).
- **Vylepšená validace při importu** – nyní lze importovat [rezervace](redactor/apps/reservation/reservations/README.md) i do minulosti, nebo vytvořit `overbooking` rezervace při importu údajů (#57405).
- **Podpora pro přidání rezervace do minulosti** – umožňuje administrátorům vytvořit rezervace v minulosti (#57389).
- Do [rezervačních objektů](redactor/apps/reservation/reservation-objects/README.md) byl přidán sloupec **Emaily pro notifikace**, který pro každý zadaný platný email (oddělený čárkou) odešle email pokud byla rezervace přidána a schválena (#57389).
- Notifikacím pro potvrzení rezervace a dalším systémovým notifikacím lze nastavit jméno a email odesílatele pomocí konfiguračních proměnných `reservationDefaultSenderName,reservationDefaultSenderEmail` (#57389).
- Přidána nová aplikace [Rezervace dní](redactor/apps/reservation/day-book-app/README.md), pro rezervaci celodenních objektů na určitý interval pomocí integrovaného kalendáře (#57389).

![](redactor/apps/reservation/day-book-app/app-table_B.png)

### Elektronický obchod

!> **Upozornění:** z důvodu aktualizace databáze může první start serveru trvat déle - do databáze se vypočítají hodnoty pro počet položek a cenu pro rychlejší načtení seznamu objednávek.
- Přidána karta **Osobní informace** do seznamu objednávek - obsahuje podrobné informace o **adrese doručení** jakož i **kontaktní informace** vše na jednom místě (#57685).
- Přidána karta **Volitelná pole** do seznamu objednávek - [volitelná pole](frontend/webpages/customfields/README.md) podle potřeby implementace (#57685).
- Export seznamu objednávek - doplněné sloupce celková cena s DPH a počet položek (#57685).
- Formulář pro objednání - doplněna možnost definovat dostupný seznam zemí přes konfigurační proměnnou `basketInvoiceSupportedCountries` (#57685).
- Upravené zobrazení údajů z karty **Osobní údaje** v seznamu objednávek, jejich logické rozdělení do částí pro lepší přehled (#57685).
- V seznamu objednávek byly přidány sloupce **Počet položek**, **Cena bez DPH** a **Cena s DPH**. Hodnoty se automaticky přepočítají při změně položek objednávky (#57685).
- Do seznamu položek doplněna možnost zobrazení web stránky produktu kliknutím na ikonu, produkt se zobrazí také v kartě Náhled při otevření editoru položky (#57685).
- V seznamu objednávek předělán výběr země přes výběrové pole, který nabízí pouze země definované konstantou `basketInvoiceSupportedCountries` (#57685).

![](redactor/apps/eshop/invoice/editor_personal-info.png)

- Nová verze[konfigurace způsobů platby](redactor/apps/eshop/payment-methods/README.md) a integrace na platební brány. Údaje jsou odděleny podle domén. Přidali jsme podporu [platební brány GoPay](https://www.gopay.com), což znamená i akceptaci platebních karet, podporu `Apple/Google Pay`, platby přes internet banking, `PayPal`, `Premium SMS` atd. Kromě toho jsou podporovány platby převodem a dobírka Pro každý typ platby je možné nastavit i cenu, která při zvolení možnosti bude automaticky připočtena k objednávce.

![](redactor/apps/eshop/payment-methods/datatable.png)

- Nová aplikace Seznam objednávek se seznamem objednávek aktuálně přihlášeného uživatele. Klepnutím na objednávku lze zobrazit detail objednávky a stáhnout ji v PDF formátu (#56609).

### Jiné menší změny

- Vyhledávání v administraci - upravené rozhraní na vlastní `RestController` a `Service` (#57561).
- Průzkumník - rychlejší načítání a nižší zatížení serveru snížením počtu souborů/požadavek na server (#56953).

### Oprava chyb

- Hromadný email - při duplikování kampaně doplněno duplikování seznamu příjemců (#57533).
- Datové tabulky - import - upravená logiky **Přeskočit vadné záznamy** při importu tak, aby se při této možnosti zpracovaly i generické chyby `Runtime` a bylo zajištěno dokončení importu bez přerušení. Tyto chyby se následně zobrazí uživateli pomocí notifikace v průběhu importování (#57405).
- Soubory - opraven výpočet velikosti souborů/složek v patičce průzkumníka a při zobrazení detailu složky (#57669).
- Navigace - opravená navigace pomocí karet v mobilním zobrazení (#57673).
- Autocomplete - opravená chyba u pole typu `Autocomplete`, kde první získaná hodnota v případě `jstree` nebyla korektní (#57317).

### Pro programátora

!> **Upozornění:** upravená inicializace Spring a JPA, postupujte podle [návodu v sekci instalace](install/versions.md#změny-při-přechodu-na-20250-snapshot).

Jiné změny:
- Přidána možnost provést [doplňkový HTML/JavaScript kód](custom-apps/appstore/README.md#doplňkový-html-kód) ve Spring aplikaci s anotací `@WebjetAppStore` nastavením atributu `customHtml = "/apps/calendar/admin/editor-component.html"` (#57409).
- V datatable editoru přidán typ pole [IMAGE\_RADIO](developer/datatables-editor/standard-fields.md#image_radio) pro výběr jedné z možnosti pomocí obrázku (#57409).
- Přidán typ pole `UPLOAD` pro [nahrání souboru](developer/datatables-editor/field-file-upload.md) v editoru datatabulky (#57317).
- Při inicializaci[vnořené datatabulky](developer/datatables-editor/field-datatable.md) přidána možnost upravit `columns` objekt zadáním JavaScript funkce do atributu `data-dt-field-dt-columns-customize` anotace (#57317).
- Přidána podpora pro získání jména a emailu odesílatele pro různé emailové notifikace použitím `SendMail.getDefaultSenderName(String module, String fallbackName), getDefaultSenderEmail(String module, String fallbackEmail)` (#57389).
- Přidána možnost nastavit kořenovou složku pro [pole typu JSON](developer/datatables-editor/field-json.md) ve formátu ID i cesty: `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")` nebo `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "26")`.
- Spuštění úloh na pozadí se provede až po kompletní inicializaci včetně `Spring` (#43144).
- Doplněna možnost nastavit [všechny vlastnosti HikariCP](install/setup/README.md#vytvoření-db-schématu) (#54273-61).
- Doplněna kontrola, zda databázový ovladač podporuje nastavení sekvencí (#54273-61).
- Upravená funkce `WJ.headerTabs`, pokud posloucháte na změnu karty doporučujeme použít událost typu `$('#pills-tabsFilter a[data-wj-toggle="tab"]').on('click', function (e) {`, kde v `e` získáte kartu, na kterou se kliklo (#56845-20250325).
- Předělaná aplikace Manažer dokumentů (Archiv souborů) na Spring aplikaci. Pokud používáte původní verzi a chcete ji zachovat, musíte přidat zpět soubory `/components/file_archiv/file_archiv.jsp` a `components/file_archiv/editor_component.jsp` a potřebné třídy ze [starší verze WebJET CMS](https://github.com/webjetcms/webjetcms/tree/release/2025.0/src/webjet8/java/sk/iway/iwcm/components/file_archiv).
- Manažer dokumentů (Archiv souborů) - upravené API `FileArchivatorBean.getId()/getReferenceId()/saveAndReturnId()` vrátí `Long`, můžete použít `getFileArchiveId()` pro včetně `int` hodnoty. Smazané nepoužívané metody, v případě jejich potřeby je přeneste do vašich tříd. Nedoporučujeme modifikovat WebJET třídy, vytvořte si nové třídy typu.`FileArchivatorProjectDB` ve vašem projektu kde metody přidáte. Pokud jsme smazali celou třídu, kterou používáte (např. `FileArchivatorAction`), můžete si ji přímo přidat do vašeho projektu (#57317).
- Přidáno automatické nastavení filtrování sloupce na hodnotu `false`, v případě že hodnota je `null` (nenastavená) a jde o sloupec, který je vnořený, jako např. `editorFields` sloupce (#57685).
- Přidána možnost [speciálního uspořádání](developer/datatables/restcontroller.md#uspořádání) přepsáním metody `DatatableRestControllerV2.addSpecSort(Map<String, String> params, Pageable pageable)` (#57685).
- Přidána možnost v anotaci `@DataTableColumn` nastavit atribut `orderProperty` který určí[sloupce pro uspořádání](developer/datatables/restcontroller.md#uspořádání) Např. `orderProperty = "contactLastName,deliverySurName"`. Výhodné pro `EditorFields` třídy, které mohou agregovat data z více sloupců (#57685).
- Pro pole typu `dt-tree-dir-simple` s nastaveným `data-dt-field-root` doplněna stromová struktura rodičovských složek pro lepší[zobrazení stromové struktury](developer/datatables-editor/field-json.md) (předtím se složky zobrazovaly až od zadané kořenové složky). Přidána možnost definovat seznam složek, které se ve stromové struktuře nezobrazí pomocí konfigurační proměnné nastavené do `data-dt-field-skipFolders`.
- Výběrové [pole s možností editace](developer/datatables-editor/field-select-editable.md) upraveno tak, aby po přidání nového záznamu byl tento záznam automaticky v poli zvolen (#57757).
- Předělaná aplikace Elektronický obchod na `BE` části. Jelikož se využívají již nové třídy, pro správné fungování musíte:
  - využít aktualizační skript `/admin/update/update-2023-18.jsp` pro základní aktualizaci vašich JSP souborů
  - nakolik se nyní využívá typ `BigDecimnal` místo `float`, musíte navíc upravit všechna srovnání těchto hodnot. Typ `BigDecimal` se nesrovnává klasicky pomocí `<, =, >` ale pomocí `BigDecimal.compareTo( BigDecimal )`
  - musíte odstranit volání souborů, nebo zpětně přidat všechny soubory, které byly odstraněny, protože nebyly využívány

### Testování

- Média - doplněný test vkládání médií ve web stránce pokud uživatel nemá právo na všechna média (#57625).
- Web stránky - doplněný test vytvoření nové stránky s publikováním v budoucnosti (#57625).
- Galerie - doplněn test vodoznaku s porovnáním obrázku, doplněn test kontroly práv (#57625).
- Web stránky - doplněný test volitelných polí při vytváření web stránky (#57625).
- Allure - doplněné výsledky jUnit testů do společného Allure reportu (#57801).

![meme](_media/meme/2025-18.jpg ":no-zoom")
