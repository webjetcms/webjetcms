# Seznam změn verze 2025

## 2025-SNAPSHOT

> Vývojová verze

### Průlomové změny

- Aplikace Kalendář novinek oddělena do samostatné aplikace, pokud kalendář novinek používáte je třeba upravit cestu `/components/calendar/news_calendar.jsp` na `/components/news-calendar/news_calendar.jsp` (#57409).
- Upravená inicializace Spring a JPA, více informací v sekci pro programátora (#43144).

### Aplikace

Předěláno nastavení vlastností aplikací v editoru ze starého kódu v `JSP` na `Spring` aplikace. Aplikace automaticky získávají i možnost nastavit [zobrazení na zařízeních](custom-apps/appstore/README.md#podmíněné-zobrazení-aplikace). Design je ve shodě se zbytkem WebJET CMS a datových tabulek (#57409).
- [Anketa](redactor/apps/inquiry/README.md)
- [Bannerový systém](redactor/apps/banner/README.md)
- [Datum a čas, Datum a svátek](redactor/apps/app-date/README.md) - sloučeno do jedné společné aplikace
- [Dotazníky](redactor/apps/quiz/README.md)
- [Hromadný e-mail](redactor/apps/dmail/form/README.md)
- [Kalendář událostí](redactor/apps/calendar/README.md)
- [Kalendář novinek](redactor/apps/news-calendar/README.md)

![](redactor/apps/dmail/form/editor.png)

### Hromadný e-mail
- **Přesunuté pole Web stránka** – nyní se nachází před polem **Předmět**, aby se po výběru stránky předmět automaticky vyplnil podle názvu zvolené web stránky (#57541).
- **Úprava pořadí v kartě Skupiny** – e-mailové skupiny jsou nyní zobrazeny před skupinami uživatelů (#57541).
- **Nové možnosti pro jméno a e-mail odesílatele** – jsou-li konfigurační proměnné `dmailDefaultSenderName` a `dmailDefaultSenderEmail` nastaveno, použijí se tyto hodnoty. Pokud jsou prázdné, systém automaticky vyplní jméno a e-mail aktuálně přihlášeného uživatele. (#57541)
  - Pomocí těchto proměnných lze nastavit **fixní hodnoty** (např. název společnosti) pro všechny [kampaně](redactor/apps/dmail/campaings/README.md), bez ohledu na to, kdo je přihlášen.

![](redactor/apps/dmail/campaings/editor.png)

### Kalendář novinek

- Kalendář novinek oddělen jako samostatná aplikace, původně to byla možnost v aplikaci Kalendář (#57409).
- Zobrazuje kalendář napojený na seznam novinek s možností filtrovat novinky podle zvoleného data v kalendáři.

![](redactor/apps/news-calendar/news-calendar.png)

### Rezervace

- **Podpora pro nadměrnou rezervaci** – umožňuje administrátorům vytvořit více rezervací `overbooking` na tentýž termín (#57405).
- **Vylepšená validace při importu** – nyní lze importovat [rezervace](redactor/apps/reservation/reservations/README.md) i do minulosti, nebo vytvořit `overbooking` rezervace při importu údajů (#57405).
- **Podpora pro přidání rezervace do minulosti** – umožňuje administrátorům vytvořit rezervace v minulosti (#57389).
- Do [rezervačních objektů](redactor/apps/reservation/reservation-objects/README.md) byl přidán sloupec **Emaily pro notifikace**, který pro každý zadaný platný email (oddělený čárkou) odešle email pokud byla rezervace přidána a schválena (#57389).
- Notifikacím pro potvrzení rezervace a dalším systémovým notifikacím lze nastavit jméno a email odesílatele pomocí konfiguračních proměnných `reservationDefaultSenderName,reservationDefaultSenderEmail` (#57389).
- Přidána nová aplikace [Rezervace dní](redactor/apps/reservation/day-book-app/README.md), pro rezervaci celodenních objektů na určitý interval pomocí integrovaného kalendáře (#57389).

![](redactor/apps/reservation/day-book-app/app-table_B.png)

### Oprava chyb

- Hromadný email - při duplikování kampaně doplněno duplikování seznamu příjemců (#57533).
- Datové tabulky - import - upravená logiky **Přeskočit vadné záznamy** při importu tak, aby se při této možnosti zpracovaly i generické chyby `Runtime` a bylo zajištěno dokončení importu bez přerušení. Tyto chyby se následně zobrazí uživateli pomocí notifikace v průběhu importování (#57405).
- Soubory - opraven výpočet velikosti souborů/složek v patičce průzkumníka a při zobrazení detailu složky (#57669).

### Pro programátora

!> **Upozornění:** upravená inicializace Spring a JPA, postupujte podle [návodu v sekci instalace](install/versions.md#změny-při-přechodu-na-20250-snapshot).

Jiné změny:
- Přidána možnost provést [doplňkový HTML/JavaScript kód](custom-apps/appstore/README.md#doplňkový-html-kód) ve Spring aplikaci s anotací `@WebjetAppStore` nastavením atributu `customHtml = "/apps/calendar/admin/editor-component.html"` (#57409).
- V datatable editoru přidán typ pole [IMAGE\_RADIO](developer/datatables-editor/standard-fields.md#image_radio) pro výběr jedné z možnosti pomocí obrázku (#57409).
- Přidána podpora pro získání jména a emailu odesílatele pro různé emailové notifikace použitím `SendMail.getDefaultSenderName(String module, String fallbackName), getDefaultSenderEmail(String module, String fallbackEmail)` (#57389).
- Přidána možnost nastavit kořenovou složku pro [pole typu JSON](developer/datatables-editor/field-json.md) ve formátu ID i cesty: `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")` nebo `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "26")`.
- Spuštění úloh na pozadí se provede až po kompletní inicializaci včetně `Spring` (#43144).

### Testování

- Média - doplněný test vkládání médií ve web stránce pokud uživatel nemá právo na všechna média (#57625).
- Web stránky - doplněný test vytvoření nové stránky s publikováním v budoucnosti (#57625).
- Galerie - doplněn test vodoznaku s porovnáním obrázku, doplněn test kontroly práv (#57625).
- Web stránky - doplněný test volitelných polí při vytváření web stránky (#57625).

## 2025.0.x

> Opravná verze původní verze 2025.0.

### Oprava chyb

- Datové tabulky - opraveno chybné zobrazení karet, které se nemají zobrazovat při vytváření nového záznamu (např. v šablonách) (#57533).
- Datové tabulky - doplněný limit počtu záznamů při zobrazení všechny. Hodnota je shodná s maximálním počtem řádků pro exportu, nastavuje se v konfigurační proměnné `datatablesExportMaxRows` (#57657-2).
- Editor obrázků - při editaci obrázku ze vzdáleného serveru doplněna notifikace o potřebě stažení obrázku na lokální server (#57657-2).
- Web stránky - opraveno vložení bloku obsahující aplikaci (#57657-2).
- Web stránky - doplněn `ninja` objekt při vkládání aplikace do nové webové stránky (#57389).
- Aplikace - opraveno zobrazení karty překladové klíče při použití komponenty `editor_component_universal.jsp` (#54273-57).
- Přihlášení - opravena chyba přihlášení při exspirování časové platnosti hesla (#54273-57).
- Přihlášení - opravené přihlášení v multiweb instalaci (#54273-57).
- GDPR - opravené zobrazení karty Čištění databáze při použití `Oracle/PostgreSQL` databáze (#54273-57).
- Archiv souborů - opraveno zobrazení ikon v dialogu data a času (#54273-57).
- Bezpečnost - aktualizovaná knihovna `Swagger UI` na verzi `5.20.0`, doplněné výjimky v `dependency-check-suppressions.xml`.
- Aktualizace - doplněno mazání nepotřebných souborů při aktualizaci rozbalené verze (#57657-4).
- Multiweb - doplněná kontrola `ID` domény při registraci návštěvníka web sídla (#57657-4).

## 2025.0

> Ve verzi **2025.0** jsme přinesli **nový design administrace** pro ještě lepší přehlednost a uživatelský komfort.
>
> Jednou z hlavních změn je přesunutí **druhé úrovně menu** do **karet v hlavičce stránky**, čímž se zjednodušila navigace. Ve webových stránkách jsme také **sloučily karty složek a webových stránek**, abyste měli vše přehledně na jednom místě. Pokud hlavička neobsahuje karty, tabulky se automaticky přizpůsobí a zobrazí **řádek navíc**.
>
> Prosíme vás o zpětnou vazbu prostřednictvím **formuláře Zpětná vazba**, pokud při používání nové verze identifikujete **jakýkoli problém se zobrazením**. Připomínku můžete doplnit io **fotku obrazovky**, což nám pomůže rychleji identifikovat a vyřešit případné nedostatky.
>
> Děkujeme za spolupráci a pomoc při vylepšování WebJET CMS!

### Průlomové změny

- Web stránky - zrušena inline editace. Možnost přímé editace stránky v režimu jejího zobrazení byla odstraněna, jelikož využívala starší verzi editoru, která již není podporována. Jako alternativu lze aktivovat nástrojový panel zobrazovaný v pravém horním rohu webové stránky. Tento panel umožňuje rychlý přístup k editoru web stránky, složky nebo šablony. Můžete vypnout nebo zapnout pomocí konfigurační proměnné `disableWebJETToolbar`. Po aktivaci se začne zobrazovat na webové stránce po vstupu do sekce Webové stránky v administraci (#57629).
- Přihlášení - pro administrátory nastavena [požadavek na změnu hesla](sysadmin/pentests/README.md#pravidla-hesel) jednou za rok. Hodnotu lze upravit v konfigurační proměnné `passwordAdminExpiryDays`, nastavením na hodnotu 0 se kontrola vypne (#57629).
- Úvod - přidán požadavek na aktivaci dvoustupňového ověřování pro zvýšení bezpečnosti přihlašovacích údajů. Výzva se nezobrazuje, pokud je ověřování řešeno přes `LDAP` nebo je-li překladový klíč `overview.2fa.warning` nastaven na prázdnou hodnotu (#57629).

### Design

Ve verzi **2025.0** jsme přinesli vylepšený **design administrace**, který je přehlednější a efektivnější.

**Upravený přihlašovací dialog** – nové pozadí a přesunutí přihlašovacího dialogu na pravou stranu. Na **přihlášení** je možné použít nejen přihlašovací jméno ale **už i email adresu**. ![](redactor/admin/logon.png)

**Přehlednější hlavička** – název aktuální stránky nebo sekce se nyní zobrazuje přímo v hlavičce.

**Nová navigace v levém menu** – pod položky již nejsou součástí levého menu, ale zobrazují se **jako karty v horní části** stránky. ![](redactor/admin/welcome.png)

**Sloučené karty v sekci Webové stránky** – přepínání typů složky a typů webových stránek se nyní zobrazují ve společné části, čímž se zjednodušila navigace. **Výběr domény** byl přesunut na spodní část levého menu. ![](redactor/webpages/domain-select.png)

**Přeorganizované menu položky**:
- **SEO** přesunuty do sekce **Přehledy**.
- **GDPR a Skripty** přesunuty do sekce **Šablony**.
- **Galerie** je nyní v sekci **Soubory**.
- Některé názvy položek byly upraveny, aby lépe vystihovaly jejich funkci.

### Web stránky

- Přidána možnost nastavit inkrement pořadí uspořádání pro složky v konfigurační proměnné `sortPriorityIncrementGroup` a web stránky v konfigurační proměnné `sortPriorityIncrementDoc`. Výchozí hodnoty jsou 10 (#57667-0).

### Testování

- Standardní heslo pro `e2e` testy se získá z `ENV` proměnné `CODECEPT_DEFAULT_PASSWORD` (#57629).

### Oprava chyb

- Webové stránky - vkládání odkazů na soubor v PageBuilder (#57649).
- Webové stránky - doplněné informace o odkazu (typ souboru, velikost) do atributu Pomocný titulek `alt` (#57649).
- Webové stránky - opravené nastavení pořadí uspořádání web stránek při použití `Drag&Drop` ve stromové struktuře (#57657-1).
- Webové stránky - při duplikování webové stránky/složka se nastaví hodnota `-1` do pole Pořadí uspořádání pro zařazení na konec seznamu. Hodnotu `-1` můžete také zadat ručně pro získání nové hodnoty pořadí uspořádání (#57657-1).
- Webové stránky - import webových stránek - opraveno nastavení média skupin při importu stránek obsahujících média. Při importu se automaticky vytvoří všechna Média skupiny (i nepoužívaná) z důvodu, že se při importu stránek překládá i média skupina nastavená pro média aplikaci `/components/media/media.jsp` ve stránce (ta může obsahovat i ID média skupiny mimo importované stránky) (#57657-1).
- Firefox - snížená verze sady `Tabler Icons` na `3.0.1`, protože Firefox při použití novějších verzí výrazně zatěžuje procesor. Optimalizované čtení CSS stylu `vendor-inline.style.css` (#56393-19).

Zbývající seznam změn změn je shodný s verzí [2024.52](CHANGELOG-2024.md).

![meme](_media/meme/2025-0.jpg ":no-zoom")
