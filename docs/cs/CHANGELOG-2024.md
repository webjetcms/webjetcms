# Seznam změn verze 2024

## 2024.0.SNAPSHOT

### Průlomové změny

- AB Testování - zamezené volání URL adres B verze (obsahujících výraz `abtestvariant`) pokud není přihlášen administrátor. Povolit přímé volání takových URL lze nastavením konf. proměnné `ABTestingAllowVariantUrl` na hodnotu `true` (#56677).
- Databázové připojení - změněná knihovna pro management databázových spojení z `Apache DBCP` na [HikariCP](https://github.com/brettwooldridge/HikariCP) (#56821).
- Inicializace - upravená inicializace WebJETu použitím `Spring.onStartup` místo `InitServlet`. Zabezpečeno je správné pořadí načítání konfiguračních proměnných a jejich použití v `SpringBean` objektech (#56913).
- Kódování znaků - vzhledem ke změně v inicializaci je kódování znaků čteno z konf. proměnné `defaultEncoding` s výchozí hodnotou `utf-8`. Pokud historicky používáte kódování `windows-1250` je třeba hodnotu v konfiguraci upravit. Už se nepoužívá hodnota ve `web.xml` pro `SetCharacterEncodingFilter` ale hodnota v konfiguraci WebJETu (#56913).

### Web stránky

- Klonování struktury - doplněna možnost [ponechat URL adresy při klonování](redactor/apps/clone-structure/README.md). Z URL adres se odstraní prefix podle zdrojové složky a doplní se prefix podle cílové. Pokud tedy klonujete např. novou jazykovou mutaci, přidá se jen např. `/en/` prefix, ale ostatní URL adresy zůstanou beze změny (#56673).

### Audit

- Předělaná sekce **Úrovně logování** na sekce Audit->[Úrovně logování](sysadmin/audit/audit-log-levels.md) a Audit->[Log soubory](sysadmin/audit/audit-log-files. md) do nového designu (#56833).

![](sysadmin/audit/audit-log-levels-datatable.png)

### AB Testování

- Pro vyhledávací boty (např. Google) se vždy zobrazí A varianta, aby text stránek byl konzistentní. Bot se detekuje stejně jako pro statistiku podle `User-Agent` hlaviček nastavených v konf. proměnné `statDisableUserAgent` (#56677).
- Do Ninja třídy přidána [identifikace zobrazené varianty](frontend/ninja-starter-kit/ninja-bp/README.md) pomocí `data-ab-variant="${ninja.abVariant}` (#56677).
- Zamezeno volání URL adres B verze (obsahujících výraz `abtestvariant`) pokud není přihlášen administrátor. Povolit přímé volání takových URL lze nastavením konf. proměnné `ABTestingAllowVariantUrl` na hodnotu `true` (#56677).

### Novinky

- Doplněna kontrola práv na složky - výběrové pole složky pro zobrazení novinek je filtrováno podle práv na složky web stránek (#56661).

### Průzkumník

- Nová verze knihovny [elfinder](https://github.com/webjetcms/libs-elFinder/tree/feature/webjetcms-integration) pro [správu souborů](redactor/files/fbrowser/README.md). Upravený design dle vzhledu datatabulek pro krásnější integraci.

![](redactor/files/fbrowser/page.png)

- Výchozí kódování souborů pro editor je nastaveno podle konf. proměnné `defaultEncoding`. Pro JSP soubory je kódování `utf-8/windows-1250` detekováno podle atributu `pageEncoding`, pokud soubor na začátku obsahuje výraz `#encoding=` použije se podle této hodnoty (#55849).

### Rezervace

- Přidána podpora pro automatické vypočítání ceny rezervace při jejím vytváření (#56841).
- Přidána nová MVC [Aplikace Rezervace času](redactor/apps/reservation/time-book-app/README.md), pro rezervaci zvolených objektů v hodinových intervalech (#56841).

![](redactor/apps/reservation/time-book-app/app-page.png)

### Bezpečnost

- Upravené dialogy pro hesla, jejich změnu a multi faktorovou autorizaci pro podporu hesel délky 64 znaků, doplněné testy změny hesla (#56657).

### Dokumentace

- Doplněna dokumentace k chybějícím aplikacím do sekce [Pro redaktora](redactor/README.md) (#56649).
- Doplněna anglická verze dokumentace (#56773).

### Testování

- Vytvořený objekt `TempMail` pro jednodušší práci s email schránkou [tempmail.plus](https://tempmail.plus) pro testování odeslaných emailů (#56929).
- Všechny základní testy (používající `DataTables.baseTest`) doplněné o testování funkce Duplikovat (#56849).

### Systémové změny

- Inicializace - přidána možnost [inicializovat hodnoty](install/external-configuration.md) pro cluster (např. `clusterMyNodeName,clusterMyNodeType,useSMTPServer,pkeyGenOffset`) i nastavením environmentálních proměnných s prefixem `webjet_` nebo systémových s prefixem `webjet. (#56877).
- Inicializace - upravená inicializace WebJETu použitím `Spring.onStartup` místo `InitServlet`. Zabezpečeno je správné pořadí načítání konfiguračních proměnných a jejich použití v `SpringBean` objektech (#56913).
- Kódování znaků - vzhledem ke změně v inicializaci je kódování znaků čteno z konf. proměnné `defaultEncoding` s výchozí hodnotou `utf-8`. Pokud historicky používáte kódování `windows-1250` je třeba hodnotu v konfiguraci upravit. Už se nepoužívá hodnota ve `web.xml` pro `SetCharacterEncodingFilter` (#56913).

### Pro programátora

- Datatabulky - přidána možnost nastavit tlačítka i pro [vytvoření nového záznamu](developer/datatables-editor/README.md#speciální-tlačítka) nastavením `createButtons` (#55849).
- Dialogové okno - přidána funkce `WJ.openIframeModalDatatable` pro otevření [modálního okna](developer/frameworks/webjetjs.md#iframe-dialog) obsahujícího editor datatabulky (editace záznamu). Automaticky nastaví možnosti pro uložení a zavření okna po uložení záznamu datatabulky (#55849).
- Zrušena podpora knihoven `Apache Commons DBCP, Commons Pool a Oracle UCP`. Databázová připojení jsou spravována pomocí [HikariCP](https://github.com/brettwooldridge/HikariCP). Zrušeno API `ConfigurableDataSource.printStackTraces` (#56821).
- Databázové připojení - doplněné auditování nezavřených databázových spojení (spojení, která jsou otevřena déle než 5 minut). Do auditu se zaznamenají jako typ `SQLERROR` s textem `Connection leak detection triggered` a výpisem zásobníku pro dohledání místa, kde se spojení nezavírá (#56821).
- Zrušena podpora knihoven `Apache Commons DBCP, Commons Pool, Oracle UCP`. Databázová připojení jsou spravována pomocí [HikariCP](https://github.com/brettwooldridge/HikariCP). Zrušeno API `ConfigurableDataSource.printStackTraces` (#56821).
- Databázové připojení - doplněné auditování nezavřených databázových spojení (spojení, která jsou otevřena déle než 5 minut). Do auditu se zaznamenají jako typ `SQLERROR` s textem `Connection leak detection triggered` a výpisem zásobníku pro dohledání místa, kde se spojení nezavírá (#56821).
- Anotace [@WebjetAppStore](custom-apps/appstore/README.md) umožňuje nastavit atribut `custom` pro určení zákaznické aplikace, která je v seznamu na začátku před standardními aplikacemi (#56841).
- Inicializace - upravená inicializace WebJETu použitím `Spring.onStartup` místo `InitServlet`. Zabezpečeno je správné pořadí načítání konfiguračních proměnných a jejich použití v `SpringBean` objektech (#56913).
- `SpringSecurity` - anotace `@EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled=true)` nahrazena `@EnableMethodSecurity(securedEnabled = true, prePostEnabled=true)` (#56913)

## 2024.18

> Verze 2024.18 obsahuje **novou sadu ikon**, Formulář snadno doplněn o **pokročilé nastavení** (příjemci, přesměrování...), do nového designu jsou předělané aplikace AB Testování, Audit (čeká na publikování, změněné stránky), Blog, Dotazníky, Kalendář událostí (schvalování). Přidává **podporu instalace typu MultiWeb** (oddělení údajů domén) v Šablonách, Hromadném e-mailu a dalších aplikacích. Nová knihovna na **detekci prohlížečů**, ​​ve Statistika-Prohlížeče dojde k drobným rozdílům, ale údaje o prohlížeči anonymizovaně zaznamenáváme i bez Cookies souhlasu.

**Upozornění:** ke spuštění verze 2024 je třeba mít na serveru instalovanou Java verze 17.

### Průlomové změny

Tato verze přináší několik změn, které nemusí být zpětně kompatibilní:

- Hromadný email - upravená podpora odesílání emailů přes službu Amazon SES z použití speciálního API na [standardní SMTP protokol](install/config/README.md#nastavení-amazon-ses).
- [Odstraněné knihovny](install/README.md#změny-při-přechodu-na-20240-snapshot) `bsf,c3p0,cryptix,datetime,jericho-html,jsass,opencloud,spring-messaging,uadetector,joda-time ,aws-java-sdk-core,aws-java-sdk-ses,jackson-dataformat-cbor,jmespath-java` (#56265).
- Odstraněna značka `iwcm:forEach`, je třeba ji nahradit za `c:forEach`. Rozdíl je v tom, že Java objekt není přímo dostupný, je třeba jej získat pomocí `pageContext.getAttribute("name")`. Použijte volání `/admin/update/update-2023-18.jsp` pro aktualizaci vašich JSP souborů (#56265).
- Hromadný email - oddělené kampaně, příjemci a odhlášené emaily podle domén, starší záznamy jsou do domén zařazeny podle URL adresy web stránky pro odeslání. Výhoda v oddělení odhlášených emailů je v případě provozování více web sídel a rozdílných seznamů příjemců, kdy se odhlašuje odděleně pro jednotlivé domény. UPOZORNĚNÍ: aktuálně odhlášené emaily se nastaví pro doménu s ID 1, pokud používáte primárně hromadný email na jiné než první doméně aktualizujte sloupec `domain_id` v databázové tabulce `emails_unsubscribe` (#56425).
- Hromadný email - smazané nepoužívané metody z Java třídy `cs.iway.iwcm.dmail.EmailDB`, pokud je ve vašem projektu používáte přesuňte si je z [původního zdrojového kódu](https://github.com/webjetcms/webjetcms/ blob/ef495c96da14e09617b4dc642b173dd029856092/src/webjet8/java/cs/iway/iwcm/dmail/EmailDB.java) do vaší vlastní třídy (#56425).
- Ikony - z důvodu přechodu na Open Source řešení jsme změnili sadu ikon z původní FontAwesome na novou sadu [Tabler Icons](https://tabler.io/icons). Pokud ve vašich vlastních aplikacích používáte ikony ze sady FontAwesome je třeba upravit kód a nahradit je ikonami ze sady `Tabler Icons`. Můžete použít skript ```/admin/update/update-2023-18.jsp``` pro úpravu nejčastěji používaných ikon v administraci (upraví pouze soubory, které vyžadují přihlášení).

### Přechod na Java 17

WebJET CMS verzie 2024 přešel na Java verze 17. Obsahuje následující změny:

- Aktualizováno několik knihovn, například. `AspectJ 1.9.19, lombok 1.18.28`.
- Aktualizovaná knihovna Eclipselink na standardní verzi, použití WebJET CMS `PkeyGenerator` nastaveno pomocí třídy `JpaSessionCustomizer` a `WJGenSequence`.
- Aktualizován `gradle` na verzi 8.1.
- Odstraněna stará knihovna ```ch.lambdaj```, použijte standardní Java Lambda výrazy (#54425).
- Odstraněna značka `<iwcm:forEach`, použití nahrazeno standardním `<c:forEach` (#56265).
- Pro zjednodušení aktualizace můžete použít skript ```/admin/update/update-2023-18.jsp``` pro kontrolu a opravu JSP souborů. Zákaznické Java třídy je třeba nově zkompilovat a opravit chyby z důvodu změny API.

### Nová sada ikon

Z důvodu přechodu na Open Source řešení jsme změnili sadu ikon z původní FontAwesome na novou sadu [Tabler Icons](https://tabler.io/icons). Některé ikony byly upraveny, aby lépe vystihovaly funkci tlačítka.

![](_media/changelog/2024q1/ti-layout.png)

U datatabulek jsou ikony pro nastavení tabulky, znovu načtení údajů, import a export přesunuty napravo, aby lépe oddělily standardní funkce od nastavení a pokročilých operací. Na obrázcích je vidět srovnání nové (nahoře) a staré verze (dole).

![](_media/changelog/2024q1/ti-dt.png)
![](_media/changelog/2024q1/fa-dt.png)

### Vylepšení uživatelského rozhraní

- Menu - menu položky/ikony sekce (Přehled, Web stránky, Příspěvky...) se zobrazí pouze pokud má uživatel přístup k některé položce v dané sekci (#56169).
- Novinky - upravené přidání novinky - přepnutí na kartu Základní pro jednodušší nastavení titulku novinky a nastavení zařazení ve stromové struktuře dle zvolené sekce v hlavičce stránky (#56169).
- Úvod - sekce Přihlášení admini, Moje poslední stránky, Změněné stránky a Audit se zobrazují pouze pokud má uživatel potřebná práva (#56169).
- Úvod - doplněna informace o složce v seznamu posledních stránek, doplněna možnost otevřít auditní záznam (#56397).
- Web stránky - zlepšená editace na mobilních zařízeních - nástrojová lišta editoru je posouvatelná, dostupné jsou všechny ikony (#56249-5).
- Datatabulky - zlepšené uspořádání nástrojové lišty editoru při malých rozlišeních - ikony se korektně posunou na druhý řádek, možnost zadat ID zůstává vpravo nahoře (#56249-5)
- Datatabulky - ikona pro označení/odznačení všech záznamů mění stav podle toho, zda jsou označeny řádky, nebo ne (#56397).
- Datatabulky - zmenšené mezery mezi sloupci, snížená výška názvu stránky, nástrojové lišty a patičky pro zobrazení více sloutlců na obrazovce/zhuštění informací. Na stejné obrazovce by se měl v tabulce zobrazit minimálně jeden řádek navíc. (#56397).

### Web stránky

- Standardní [synchronizace titulku](redactor/webpages/group.md#synchronizace-názvu-složka-a-web-stránky) složky a hlavní web stránky se nepoužije, pokud je jedna web stránka nastavena jako hlavní více složkám, nebo když je hlavní stránka z jiné složky (#56477).

### MultiWeb

Přidána podpora [provozy v režimu MultiWeb](install/multiweb/README.md) - multi tenantní správa více samostatných domén v jednom WebJETu. Domény jsou navenek samostatné a každá se tváří jako samostatná instalace WebJET CMS.

- Seznam uživatelů - oddělený podle ID domény (#56421).
- Úvod - Online admini - oddělen podle domén (#56421).
- Práva na Doménové limity, HTTP hlavičky, Úrovně logování, Poslední logy, Skupiny uživatelů, Skupiny práv jsou dostupné pouze v první/správcovské doméně (#56421).
- Web stránky - přidána možnost vytvořit více kořenových složek (#56421).
- Statistika - Chybné stránky - přidán sloupec `domain_id` do databáze pro oddělení chybných URL adres podle domén (#56421).
- Média - skupiny médií - seznam rozdělený podle aktuální zobrazení domény a práv stromové struktury web stránek (#56421).

### AB testování

- Seznam stránek v AB testu předělaný do [nového designu](redactor/apps/abtesting/abtesting.md), přidaná sekce pro nastavení konfigurace AB testování (#56177).

![](redactor/apps/abtesting/stat-percent.png)

### Audit

- Doplněna podpora filtrování uživatele i podle zadané email adresy.
- Předělaná sekce Audit->[Čeká na publikování](sysadmin/audit/audit-awaiting-publish-webpages.md) do nového designu. Přehledně zobrazuje seznam stránek, které budou v pozměněné v budoucnosti (#56165).
- Předělaná sekce Audit->[Změněné stránky](sysadmin/audit/audit-changed-webpages.md) do nového designu. Zobrazuje kompletní seznam změněných stránek za zvolené období (#56165).

![](sysadmin/audit/audit-changed-webpages.png)

### Blog

- Blog předělaný do nové administrace. Sekce seznam článků používá standardní možnosti jak jsou použity v seznamu web stránek/novinek (#56169, #56173).
- Původní seznam diskusních příspěvků je přesunut přímo do sekce Diskuse, uživatelé/blogeři získávají právo i na tuto část (#56169).
- Pro zobrazení seznamu článků je používána standardná aplikace pro novinky.
- Správa blogerů (administrace uživatelů) předělaná na datatabulku, umožňuje zjednodušeně vytvořit blogera a korektně mu nastavit práva.

![](redactor/apps/blog/blogger-blog.png)

### Dotazníky

Aplikace [dotazníky](redactor/apps/quiz/README.md) předělána do nového designu. Umožňuje vytvořit dotazníky s vyhodnocením správné odpovědi. Dotazník může být s jednou správnou odpovědí nebo s bodovanými odpověďmi. Aplikace obsahuje také statistické vyhodnocení (#55949).

![](redactor/apps/quiz/quizStat.png)

### Formulář snadno

- Přidána karta rozšířena s pokročilými možnostmi nastavení zpracování formuláře podobně jako mají standardní HTML formuláře. Přidány možnosti nastavení příjemců, přesměrování, ID stránky s verzí do emailu atp. Upravený seznam položek formuláře pro lepší využití prostoru (#56481).

![](redactor/apps/formsimple/editor-dialog-advanced.png)

- Přidaný typ pole Formátované textové pole pro zadávání textů s formátováním jako je tučné písmo, odrážky, číslovaný seznam a podobně (#56481).

![](redactor/apps/formsimple/formsimple-wysiwyg.png)

### GDPR Cookies

- Integrace s [Google Tag Manager](redactor/apps/gdpr/gtm.md) doplněna o nové typy souhlasů `ad_user_data` a `ad_personalization`, které jsou napojeny na souhlas s marketingovými cookies. Upraveno generování JSON objektu z hodnot `true/false` na správné hodnoty `granted/denied` (#56629).

### Hromadný e-mail

- Upravena podpora odesílání emailů přes službu Amazon SES z použití speciálního API na [standardní SMTP protokol](install/config/README.md#nastavení-amazon-ses) (#56265).
- Nastavení doménových limitů přidáno jako samostatné právo, ve výchozím nastavení je nepovoleno, je třeba jej přidat vhodným uživatelům (#56421).
- Oddělené kampaně, příjemci a odhlášené emaily podle domén, starší záznamy jsou do domén zařazeny podle URL adresy web stránky pro odeslání. Výhoda v oddělení odhlášených emailů je v případě provozování více web sídel a rozdílných seznamů příjemců, kdy se odhlašuje odděleně pro jednotlivé domény. UPOZORNĚNÍ: aktuálně odhlášené emaily se nastaví pro doménu s ID 1, pokud používáte primárně hromadný email na jiné než první doméně aktualizujte sloupec `domain_id` v databázové tabulce `emails_unsubscribe` (#56425).
- Přidána možnost přímého [odhlášení se z hromadného emailu](redactor/apps/dmail/form/README.md#odhlášení) kliknutím na odkaz zobrazený v emailovém klientovi/Gmail nastavením hlavičky emailu `List-Unsubscribe` a `List-Unsubscribe=One -Click` (#56409).

### Kalendář událostí

- Neschválené a doporučené události - proces [schvalování nových událostí](redactor/apps/calendar/non-approved-events/README.md) a [doporučených událostí](redactor/apps/calendar/suggest-events/README.md) předěláno do nového designu (#56181).

### Novinky

- Přidána možnost Vyloučit hlavní stránky složek v [seznamu novinek](redactor/apps/news/README.md#nastavení-aplikace-ve-web-stránce) pro vyloučení hlavních stránek z pod složek v seznamu novinek. Předpokládá se, že podadresáře obsahují hlavní stránku se seznamem novinek v této složce, tyto stránky se vyloučí a nepoužijí se v seznamu novinek (#56169).

### Otázky a odpovědi

- Přidáno samostatné ukládání odpovědi do emailu i do databáze pro pozdější ověření odpovědi (#56533).
- Opraveno zobrazení sloupce Otázka již byla zodpovězena (#56533).
- Při zvolení možnosti Zobrazovat na web stránce se zkopíruje odpověď do emailu do odpovědi na web stránku (je-li již zadána) (#56533).

### Šablony

- Oddělený seznam šablon podle domén - zobrazují se pouze šablony, které nemají omezení zobrazení podle složek nebo obsahují omezení na složku aktuálně zobrazené domény (#56509).

### Statistika

- Upravené získání čísla týdne podle ISO 8601, hodnoty ve statistikách podle týdne mohou být rozdílné vůči předchozí verzi (#56305).
- Chybné stránky - přidán sloupec `domain_id` do databáze pro oddělení chybných URL adres podle domén. Historické údaje nejsou odděleny (zobrazí se ve všech doménách), ale od momentu aktualizace se budou zobrazovat již chybné URL oddělené podle domén (#56421).
- Upravena [detekce prohlížeče](redactor/apps/stat/README.md#prohlížeče) s využitím knihovny [UAP-java](https://github.com/ua-parser/uap-java). Některé údaje jsou detekovány jinak než původně - rozlišuje se Safari a Mobile Safari na iOS, operační systém pro Android telefony je namísto Unix nastaven na Android, pro některé případy je detekován Linux namísto Unix, macOS jako Mac OS X. Přidána podpora detekce Instagram a Facebook interního prohlížeče. Po aktualizaci na tuto verzi mohou tedy nastat rozdíly při zobrazení období před a po aktualizaci. Je možné aktualizovat soubor s definicí prohlížečů nastavením cesty k [YAML](https://github.com/ua-parser/uap-core/blob/master/regexes.yaml) souboru v konf. proměnné `uaParserYamlPath` (#56221).
- Typ prohlížeče a operační systém je zapsán do statistiky i bez souhlasus ukládáním cookies, jelikož tento údaj cookies nepoužívá. Údaj je anonymizován a zapsán se zaokrouhleným časem na 15 minut (#56221).

### Bezpečnost

- 404 - přidána možnost vypnout ochranu volání 404 stránky (počet požadavků) podobně jako jiné spam ochrany nastavením IP adresy do konf. proměnné `spamProtectionDisabledIPs`. Pro danou IP adresu se vypnou i další SPAM ochrany (pro opakovaná volání).
- Přidána kontrola licencí použitých knihoven při `deployment` nové verze (#56265).
- Aktualizováno více knihovny na novější verze, majoritní verze změněny pro `mariadb-java-client` ze 2 na 3, `pdfbox` ze 2 na 3 (#56265).

### Testování

- Přidali/upravili jsme automatizované testování front-end části (ne admin části) aplikací Mapa, Carousel, Archiv souborů, Content Block, Datum, Google Vyhledávání, Odkazy na sociální sítě, Doporučení, Poslat stránku emailem, Počasí, Příbuzné stránky, Působivá prezentace , Slider, Slit Slider, Video, Carousel Slider, Vložení HTML kódu, Vložení dokumentu, Vyhledávání, Podmíněné zobrazení, Bloky (#56413).
- Přidána podpora pro automatické otestování/kontrolu mezi-doménového oddělení záznamů do [DataTables.baseTest](developer/testing/datatable.md#možnosti-nastavení) (#56509).

### Pro programátora

- Hromadný email - smazané nepoužívané metody z Java třídy `cs.iway.iwcm.dmail.EmailDB`, pokud je ve vašem projektu používáte přesuňte si je z [původního zdrojového kódu](https://github.com/webjetcms/webjetcms/ blob/ef495c96da14e09617b4dc642b173dd029856092/src/webjet8/java/cs/iway/iwcm/dmail/EmailDB.java) do vaší vlastní třídy (#56425).
- `MailHelper` - přidána možnost nastavit hlavičky emailu voláním `addHeader(String name, String value)`, API SendMail upravené pro použití `MailHelper`, který doporučujeme primárně používat pro odesílání emailů (#56409).
- Přidána možnost zobrazit ve vaší aplikaci seznam konf. proměnných podle [zadaného prefixu](./custom-apps/config/README.md) (#56177).
- Přidána možnost kontrolovat práva při vytvoření, editaci, mazání, provedení akce ale i při získání záznamu implementací metody `public boolean checkItemPerms(T entity, Long id)` (#56421).
- Přidána třída `DatatableRestControllerAvailableGroups` pro snadnou implementaci kontroly práv uživatele i na základě práv na stromovou strukturu web stránek (#56421).

### Systémové změny

- WebJET CMS je dostupný přímo v [repozitáři maven central](https://repo1.maven.org/maven2/com/webjetcms/webjetcms/), GitHub projekty [basecms](https://github.com/webjetcms/basecms ) a [democms](https://github.com/webjetcms/democms) upravené pro použití přímo tohoto repozitáře. Sestavení je mírně odlišné od původního sestavení, knihovny `wj*.jar` jsou spojeny do `webjet-VERZE-libs.jar`. Použitá knihovna [pd4ml](https://pd4ml.com/support-topics/maven/) je ve verzi 4, pro generování PDF souborů vyžaduje zadání licence do souboru `pd4ml.lic` v [pracovní složce](https:// pd4ml.com/support-topics/pd4ml-v4-programmers-manual/) serveru nebo složce kde se nachází `pd4ml.jar`. Později bude doplněna možnost zadat licenční číslo přes konfigurační proměnnou (#43144).
- Zrušená podpora plně textového indexování `rar` archivů (#43144).
- NTLM - přidána konf. proměnná `ntlmLogonAction.charsetEncoding` s názvem kódování znaků pro LDAP přihlášení. Pokud je prázdné, nepoužije se a znaky se ponechají v kódování jak je vrátí LDAP server.
- PostgreSQL - přidána podpora [databáze PostgreSQL](install/setup/README.md#vytvoření-db-schémata) (#56305).

Odstranili jsme několik nepoužívané knihovny, málo používané jsme nahradili alternativami:

- Odstraněna značka `<iwcm:forEach`, použití nahrazeno standardním `<c:forEach`. Změnu proveďte z `<iwcm:forEach items="${iii}" var="vvv" type="cs.iway.ttt">` na `<c:forEach items="${iii}" var="vvv "><%cs.iway.ttt vvv = (cs.iway.ttt)pageContext.getAttribute("vvv");%>`.
- Odstraněna JSP knihovna `datetime`, pokud používáte JSP značky `<datetime:xxx>` můžete si ji přidat do `build.gradle` jako `implementation("taglibs:datetime:1.0.1")`.
- Odstraněna knihovna `c3p0` a podpora použití tohoto databázového `pool`.
- Odstraněny staré JS funkce `saveEditor` a `historyPageClick` včetně staré REST služby `/admin/rest/document/`.
- Hromadný email - upravená podpora odesílání emailů přes službu Amazon SES z použití speciálního API/knihovny na [standardní SMTP protokol](install/config/README.md#nastavení-amazon-ses).

V případě potřeby některé z uvedených knihoven ve vašem projektu si ji přidejte do vašeho `build.gradle`:

```gradle
dependencies {
 implementation("com.amazonaws:aws-java-sdk-core:1.12.+")
 implementation("com.amazonaws:aws-java-sdk-ses:1.12.+")
 implementation("bsf:bsf:2.4.0")
 implementation("commons-validator:commons-validator:1.3.1")
 implementation("taglibs:datetime:1.0.1")
 implementation("net.htmlparser.jericho:jericho-html:3.1")
 implementation("jóda-time:jóda-time:2.10.13")
 implementation("io.bit3:jsass:5.1.1")
 implementation("org.jsoup:jsoup:1.15.3")
 implementation("org.mcavallo:opencloud:0.3")
 implementation("org.springframework:spring-messaging:${springVersion}")
 implementation("net.sf.uadetector:uadetector-core:0.9.22")
 implementation("net.sf.uadetector:uadetector-resources:2014.10")
 implementation("cryptix:cryptix:3.2.0")
 implementation("org.springframework:spring-messaging:${springVersion}")
 implementation("com.google.protobuf:protobuf-java:3.21.7")
 implementation("com.google.code.findbugs:jsr305:3.0.2")
 implementation("org.apache.taglibs:taglibs-standard-spec:1.2.5")
 implementation("org.apache.taglibs:taglibs-standard-impl:1.2.5")
 implementation('com.mchange:c3p0:0.9.5.5')
}
```

### Oprava chyb

2024.0.X

- Bezpečnost - při chybě nahrávání souboru nebude zobrazena chyba ze serveru ale generická chybová zpráva (#56277-13).
- GDPR/Cookies - opraveno nastavení cookies v jedno doménovém WebJETu (duplikování nastavených cookies).
- Datatabulky - vypnuta možnost filtrování podle ID v tabulkách, kde ID není primární klíč, například. Konfigurace, Mazání dat, Překladové klíče (#56277-12).
- Formuláře - opraveno zobrazení sloupce Datum potvrzení souhlasu u formulářů s nastaveným [potvrzením email adresy](redactor/apps/form/README.md#nastavení-potvrzení-emailové-adresy) (#56393-7).
- Formuláře - opraveno zobrazení textu "prázdné" v tabulce (#56277-10).
- Formuláře - upravený export čísel - čísla s desetinném místem odděleným znakem čárka jsou zkonvertovány na oddělovač tečka a na číselnou reprezentaci pro správný formát v Excelu. Nepoužije se na čísla začínající znakem + nebo 0 (#56277-10).
- Formuláře - opravený duplicitní export při přechodu mezi více formuláři bez obnovení stránky (#56277-10).
- Formuláře - při vypnuté spam ochraně `spamProtection=false` se již nebude kontrolovat CSRF token při odeslání formuláře (#56277-13).
- Galerie - opraveno smazání složky galerie vytvořené přes web stránku při vkládání obrázku (#56393-8).
- Galerie - opraveno nastavení parametrů složky galerie pokud rodičovská složka nemá uložená nastavení (je bílá). Hledá se uložené nastavení složky směrem ke kořeni (#56393-10).
- Galerie/Editor obrázků - doplněna chybějící funkce pro změnu velikosti obrázku.
- Hromadný email - opravena chyba vložení příjemce ze skupiny uživatelů, který nemá povolené přihlášení (je deaktivován, nebo nemá platné data přihlášení od-do) (#56701).
- Klonování struktury - opraveno nastavení propojení složek při klonování (mohlo docházet k neúplnému klonování složek) (#56277-7).
- Úrovně logování - opraveno nastavení úrovní do `Logger` objektu (#56277-12).
- Skripty, Bannerový systém, Skupiny práv - opravena funkce duplikovat záznam (#56849).
- Statistika - přidána možnost [nastavit licenční číslo](install/config/README.md#licence) pro knihovnu amcharts pro zobrazení grafů (#56277-7).
- Statistika - upravené zaznamenávání chybných URL adres - odstraněn identifikátor session `jsessionid`, který mohou přidávat do URL adresy některé roboty (#56277-11).
- Web stránky - opraveno otevření složky zadáním jeho ID, pokud se složka nachází v jiné doméně (#56277-7).
- Web stránky - PageBuilder - opraveno vkládání odkazu (duplikování okna souborů), vkládání formulářových polí a upravený vizuál podle aktuální verze (#56277-9).
- Web stránky - v okně vložení obrázku přidána podpora zobrazení cesty ve stromové struktuře ke stávajícímu obrázku s prefixem `/thumb` (#56277-9).
- Web stránky - opraveno zobrazení překladových klíčů na základě prefixu ID šablony (#56393-7).
- Web stránky - opraveno smazání stránky, která má nastaveno i publikování do budoucna/notifikaci (a před smazáním byla zobrazena v editoru stránek) (#56393-8).

2024.0.21

UPOZORNĚNÍ: upravené čtení a ukládání hesel uživatelů, po nasazení ověřte práci s uživatelským účtem, zejména změnu hesla, zapomenuté heslo atp. Použijte skript `/admin/update/update-2023-18.jsp` pro základní úpravu souborů.

- Bezpečnost - opravena kontrola přístupu k souborům ve složce `/files/protected/` při použití externích souborů - nastavena konf. proměnná `cloudStaticFilesDir` (#56277-6).
- Bezpečnost - opravena kontrola typů souborů při nahrávání ve formulářích a použití `/XhrFileUpload` (#56633).
- Elektronický obchod - opravený import ceníku
- Hromadný email - vrácená třída `EMailAction` pro použití v úkolech na pozadí pro odesílání hromadného emailu.
- Instalace - upravená detekce `JarPackaging` při startu pokud neexistuje soubor `poolman.xml`.
- Klonování struktury - opraveno klonování v jedno doménové instalaci.
- Klonování struktury - při klonování složky doplněné kopírování všech atributů původní složky (html kód do hlavičky, jméno instalace, přístupová práva, zobrazení v mapě stránek a navigační liště) (#56633).
- Plně textové vyhledávání - doplněna kontrola nastavení zaškrtávacího pole Indexovat soubory pro vyhledávání v nastavení přiečinka. Pokud pole není zaškrtnuté, soubory ve složce se nebudou indexovat. Původní verze kontrolovala pouze existenci složky `/files` v kartě System ve web stránkách (#56277-6).
- PostgreSQL - opravené chyby získání údajů z databáze (boolean hodnota) - Kalendář událostí, Rezervace, Nepoužívané soubory, Posílání zpráv, Seznam administrátorů (#56277-6).
- Uživatelé - opraveno uložení hesla bez šifrování při použití API `UsersDB.getUser/UsersDB.saveUser` při přechodu přes GUI. Předpokládalo se, že hesla budou při API volání předem zašifrována, což se neudálo. Kód doplněný o detekci `hash`, při čtení z databáze se hesla, salt a API klíč nečte a nastaví se hodnota "Heslo nezměněno". Při změně hesla dojde k odhlášení ostatních relací téhož uživatele. (#56277-6).
- Vyhledávání - vyloučeno indexování souborů ze složky začínající na `/files/protected/`, pro `Lucene` vyhledávání doplněna kontrola na tuto cestu, odkaz nebude do vyhledání zařazen (standardní databázové vyhledávání podmínku již obsahovalo) (#56277-6).
- Zrcadlení struktury/Klonování - doplněno kopírování volných polí složky (#56637).
- Web stránky - upravené načítání stránek z podadresářů - filtrován je seznam stránek plně textového vyhledávání, pokud se nachází v hlavní složce domény (#56277-6).

2024.0.17

- Bezpečnost - opraveny zranitelnosti z penetračních testů (#55193-5).
- Bezpečnost - upravené vkládání objektů pomocí zápisu `!REQUEST` tak, aby byly [filtrovány speciální HTML znaky](frontend/thymeleaf/text-replaces.md#parametry-a-atributy) (#55193-6).
- Bezpečnost - třída `BrowserDetector` vrátí hodnoty s filtrovanými speciálními HTML znaky (#55193-6).
- Bezpečnost - opraveno generování QR kódu pro dvou faktorovou autorizaci, opraveno uložení autorizačního tokenu při vynucené dvou faktorové autorizaci po přihlášení (když je nastavena konf. proměnná `isGoogleAuthRequiredForAdmin` na hodnotu true) (#56593).
- Datatabulky - přidána možnost přeskočit chybné záznamy při importu z xlsx, chybové zprávy jsou kumulovány do jedné společné notifikace (#56465).
- Datatabulky - opraven import pouze nových záznamů (#56393-4).
- Formuláře - opraveno přepínání karet kliknutím na šipky na klávesnici při zadávání textu v kartách Rozšířené nastavení nebo Limity na soubory (#56393-3).
- Formuláře - doplněna možnost vytisknout formulář v seznamu formulářů (#56393-3).
- Formuláře - opraveno zobrazení náhledu formuláře odeslaného jako email bez formátování (#55193-6).
- Záhlaví HTTP `Strict-Transport-Security` je ve výchozím nastavení nastaveno na `max-age=31536000` (#55193-5).
- Hromadný email - opraveno získání web stránky z URL adresy (#56393-3).
- Hromadný email - opraveno nastavení příjemců pro nově vytvářený email (#56409).
- Hromadný email - přidána možnost manuálně zadat více příjemců emailu oddělených i znakem mezera (podporováno je oddělení znakem čárka, středník, mezera nebo nový řádek) a přeskočit vadné emaily (#56465).
- Hromadný email - při znovu uložení kampaně/spuštění a zastavení odesílání jsou z kampaně smazáni aktuálně odhlášení příjemci (aby nedošlo k opětovnému odeslání emailu po odhlášení), zlepšená kontrola duplicit při manuálním přidání a importu z xlsx (#56465).
- Hromadný email - upravena kontrola email adresy, povoleno i jedno písmenové domény a email adresy (#56465).
- Mazání dat - cache objekty - upravená dostupnost tlačítek smazat vše jen pro případ, kdy není označen ani jeden řádek.
- Média - výběr média skupiny, která má omezená práva.
- Notifikace - přidána možnost posouvání seznamu notifikací - pro případ zobrazení dlouhé notifikace, nebo velkého množství notifikací (#56465).
- PDF - opravené generování PDF souborů s vloženým obrázkem přes httpS protokol - knihovna `pd4ml` chybně detekuje verzi Javy z druhého čísla podle původního číslování `1.8`, přičemž aktuálně se používá `17.0`. Upraveno dočasnou změnou druhého čísla na hodnotu 8 (#56393-2).
- Uživatelé - zlepšený import uživatelů - automatické generování přihlašovacího jména, hesla a doplněné číslo řádku při chybném záznamu (#56465).
- Statistika - opravený jazyk a formát dat v grafech statistiky podle zvoleného jazyka přihlášení (#56381).
- Otázky a odpovědi - opraveno zobrazení sloupce Otázka již byla zodpovězena, při uložení odpovědi se zkopíruje odpověď do emailu do odpovědi na web stránku jako bylo ve verzi 8 (#56533).
- Vložení dokumentu - doplněno opakované získání náhledu dokumentu, pokud se ho nepodaří načíst na první pokus (#56393-3).
- Web stránky - zrušená klávesová zkratka `ctrl+shift+v` pro vložení jako čistý text, jelikož tuto možnost již standardně poskytuje přímo prohlížeč (#56393-3).

2024.0.9

- Datatabulky - opravený export dat ve statistice (u tabulky se `serverSide=false`) (#56277-3).
- Galerie - opraveno načtení seznamu fotografií při zobrazení galerie se zadané složky (např. ve web stránce) (#56277-1).
- Uživatelé - zobrazení práv na web stranky a složky upravené pro zobrazení každého záznamu na novém řádku pro lepší přehled (#56269).
- Uživatelé - upravený export a import pro podporu doménových jmen při nastavení práv na web stránky a složky (#56269).
- Web stránky - opraveno nastavení složky podle titulku stránky u ještě neuložené webové stránky a přetažení obrázku přímo do editoru (#56277-1)
- Web stránky - přidána možnost zadat do odkazu telefonní číslo ve formě `tel:0903xxxyyy` (#56277-4)
- SEO - oprava zaznamenání přístupu robota bez odsouhlaseného GDPR (statistika robota se zaznamená bez ohledu na souhlas) (#56277-5).

### Testování

- Datatabulky - základní test - u povinných polí, která mají předem nastavenou hodnotu se přeskočí test povinnosti pole (#56265).


## 2024.0

> Verze 2024.0 obsahuje novou verzi **aktualizace s popisem změn**, **klonování struktury** integrované s funkcí zrcadlení (včetně možnosti překladů), přidává možnost **obnovit** web stránku, nebo **celá složka z koše** , přidává **editor typu HTML** a možnost nastavení typu editoru přímo pro šablonu, **aplikacím** lze **zapnout zobrazení pouze pro zvolené typy zařízení** mobil, tablet, PC a samozřejmě zlepšuje bezpečnost a komfort práce.

<div class="video-container">
 <iframe width="560" height="315" src="https://www.youtube.com/embed/YGvWne70czo" title="YouTube video player" frameborder="0" allow="accelerometr; autoplay; clipboard- write; encrypted-media;
</div>

Seznam změn je shodný s verzí [2023.53-java17](CHANGELOG-2023.md).