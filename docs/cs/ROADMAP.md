# ROADMAP

Seznam plánovaných verzí WebJET CMS. Verze jsou číslovány jako `rok.týždeň` což je také očekávaný čas dostupnosti dané verze. Úkoly označené číslem tiketu jsou již v řešení, úkoly označené značkou `[x]` jsou již implementovány, úkoly označené značkou `+` byly přidány po přípravě `roadmapy` a ovlivňují dodání ostatních úkolů.

Upozornění: jedná se o plán, podle potřeb a kapacit mohou být do verze přidány nové úkoly a některé naopak pro plánované do pozdější verze.

Vysvětlení použitých piktogramů:
- [ ] úloha je v plánu
- [ ] má-li na konci ID přiřazeného tiketu už jsme na úloze začali pracovat (#id-tiketu)
- [x] úkol je hotový
- [ ] +pokud začíná na znak `+` byla úloha přidána do seznamu až po prvotním vytvoření plánu, takové úkoly jsou typicky požadovány zákazníky a pokud budou realizovány ovlivní časový plán. To znamená, že některé jiné úkoly nemusí být v dané verzi realizovány z nedostatku času.
- [ ] ~~přeškrtnuto~~ úloha přesunuta do jiné verze, nebo řešena jiným způsobem, nebo zrušena. V popisu je vždy číslo verze kam se přesunula nebo důvod zrušení.

## 2026

- Sémantické vyhledávání - využít AI ke zlepšení vyhledávání pomocí `RAG`.
- Headless CMS - připravit REST rozhraní pro použití WebJET CMS v headless módu.
- Migrace na Spring Boot projekt.
- Testování - přidat testování přístupnosti pomocí rozšíření [codeceptjs-a11y-helper](https://github.com/kobenguyent/codeceptjs-a11y-helper).
- Práva - přidat možnost nastavit práva jen na čtení a případně nastavit jen povolené ID pro editaci.
- Formuláře - přidat možnost nastavit celkovou velikost příloh pro formulář, nyní lze nastavit jedině per soubor.
- Statistika - upravit zápis do `seo_bots` přes `StatWriteBuffer` pro méně konfliktů při vysokém zatížení a cluster databázi.
- Skripty - přidat možnost nastavit, zda se má skript vkládat také v editoru stránek, nebo ne.
- Formuláře - přidat skupinu výběrových a zaškrtávacích polí `radio/checkbox` napojenou na číselník, podobně jako máme pro `select` pole.

## 2025

- [x] Přechod na `Jakarta EE` - změna Java packages z `javax.servlet` na `jakarta.servlet`, připravit migrační skript (#57793).
- [x] Přechod na aplikační server Tomcat 11+ (#57793).
- [x] Přechod na `Spring` verze 7 (#57793).
- [ ] Zavést do projektu povinnost použití `SonarLint` a formátování kódu přes `.editorconfig` nebo `Spotless` - příklad https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/build.gradle.
- [x] Primární používání GitHub repozitáře pro vývoj.
- [x] Zrušení generování artifaktů na starý `iwmsp.sk` repozitář, artefakty budou dostupné už jen přes [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms).
- [x] Aktualizace knihovny `pd4ml` na novou verzi, jednotlivé weby si budou muset pořídit licenci samostatně, nebude již poskytována WebJET CMS.
- [ ] Přechod nejpoužívanějších aplikací do Spring verze s využitím Thymeleaf šablon.
- [x] Zrušení `Apache Struts` framework, nahrazení `logic:present,logic:iterate,bean:write` buď za `JSTL` variantu, nebo implementace podobné funkcionality do `iwcm:present,iwcm:iterate,iwcm:beanrwite` (#57789).
- [ ] Přesun JSP souborů, Java tříd a JavaScript knihoven staré verze 8 do `obsolete` jaro archivu, který nebude standardní součástí WebJET CMS. Může být použit na starých projektech, kde zatím není provedena aktualizace všech zákaznických aplikací na `Spring` verze, ale bez podpory a aktualizací ze strany WebJET CMS.
- [ ] Statistika - mapa kliknutí - obnovení funkcionality, vyřešení problému responzivnosti (samostatná evidence dle šířky okna).
- [x] Statistika - možnost filtrovat botů pro statistiku vadných stránek (#58053).
- [ ] Log soubory - filtrovat podle jména instalace.
- [ ] `quill` - přidat možnost nastavit položky menu včetně barev.
- [ ] Aplikace - možnost nákupu aplikace pro OpenSource verzi (#55825).
- [ ] Možnost provést Thymeleaf kód v hlavičce/patičce a možná i v těle web stránky.
- [ ] Bezpečnost - přidat podporu generování `nonce` pro `Content-Security-Policy` hlavičku, viz. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.
- [x] Formuláře - přidat možnost volat Java třídu pro validaci formuláře (#58161).
- [x] Značky - filtrovat podle aktuální domény aby to bylo stejné jako v jiných částech (#57837).
- [x] Import uživatelů - pokud není zadané heslo, tak vygenerovat (pro nové uživatele), pokud není posílán stav `authorized` nastavit na `true` (#58253).
- [ ] V testech nějak automatizovaně kontrolovat výskyt `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` což jsou nesprávná čekání bez definovaného času, způsobí zaseknutí automatizovaných testů.
- [ ] Doplnit aplikaci pro přesměrování hlavní stránky na `/sk/` nebo `/en/` podle jazyka prohlížeče.
- [x] Upravit vymazání konfigurace tak, že při vymazání se jí nastaví původní hodnota definovaná v `Constants` (#57849).
- [x] Galerie - při duplikování obrázku umožnit změnu "Složka", abychom uměli duplikovat obrázky do jiné než aktuální složky (#57885).
- [x] Hromadný email - auditovat změny ve skupinách uživatelů (#58249).
- [x] Archiv souborů - předělat do datových tabulek (#57317).
- [ ] Volitelná pole - přidat možnost výběru více položek pro napojení na číselník.
- [x] Elektronický obchod - integrace na platební bránu `GoPay` (#56609).
- [x] Přidat možnost autorizace přes `OAuth2`, možnost použít `mock` server https://github.com/navikt/mock-oauth2-server nebo https://github.com/patientsknowbest/fake-oauth2-server (#56665).
- [ ] Autorizace přes `SAML` - integrovat knihovnu [Spring SAML](https://spring.io/projects/spring-security-saml) pro možnost autentifikace vůči `ADFS/SAML` serveru.
- [x] Rezervace - nová aplikace pro celo denní rezervaci (#57389).
- [x] Aplikace - předělat dialog nastavení aplikací v editoru web stránek ze starého JSP na datovou tabulku (#57409).
- [x] Hromadný email - optimalizace tvorby seznamu příjemců (#57537).
- [ ] +Úkoly na pozadí - možnost ručně spustit úlohu na `node`, který má úloha nastaven, nyní se spustí na `node` kde je uživatel přihlášen.
- [ ] +Formuláře - zakázat `GET` volání na `FormMail/FormMailAjax`.
- [ ] +Elektronický obchod - do emailu přidat `JSON-LD` data https://schema.seznam.cz/objednávky/dokumentace/.
- [ ] +Číselník, Blog, Novinky - upravit tak, aby výběr typu číselníku nebo složky pro novinky byl vlevo podobně jako v galerii/webových stránkách. Nemusí být pak karty ale vše najednou zobrazeno.
- [ ] +Překladové klíče - zobrazovat stromovou strukturu překladových klíčů pro lepší orientaci.
- [ ] +Přidat podporu přihlašování se do administrace přes [PassKeys](https://passkeys.dev/docs/tools-libraries/libraries/) (#56665).
- [ ] +Fotobanka - přidat možnost nastavit název souboru před stažením z fotobanky, automaticky nastavit podle hledaného výrazu.
- [ ] +Galerie - pokud nastavuji perex obrázek na obrázek v galerii, stáhnu z fotobanky, a obrázek přejmenuji na existující v databázi (je-li soubor smazán z disku) tak vznikne v `gallery` tabulce duplicitní záznam. Navíc se nepřejmenují ostatní obrázky `o_,s_`. Třeba pamatovat na to, že teoreticky mohu přejmenovat libovolný, mělo by to detekovat, že jsem v galerii a přejmenovat všechny verze.
- [x] +Konfigurace - doplnit možnost nastavení `Hikari` přes `poolman.xml/ENV` jako například `spring.datasource.hikari.idle-timeout=30000, spring.datasource.hikari.max-lifetime=1800000, spring.datasource.hikari.connection-timeout=30000` (#54273-61).
- [ ] Datatable - opravit počítání označených řádků po jejich vymazání.
- [ ] +Průzkumník - nastavit vlastnosti složky (indexování, práva) podle rodičovské (rekurzivní) při prvním otevření, pokud složka nemá nastavení v databázi.
- [ ] +Pro pole typu `DataTableColumnType.JSON`, konkrétně `className = "dt-tree-page-null"` přidat možnost definovat kořenovou složku.
- [ ] +Přesunout všechny `Converter` třídy jako `DocDetailsNotNullConverter` do samostatného `package` kde nebudou jiné entity aby bylo možné tento `Converter` použít iv projektech kde je použit samostatný `JPA`.
- [x] +Přidat možnost nastavit jméno HTTP hlavičky pro `x-forwarded-for` a určit, která z IP adres se použije (první VS poslední) (#58237).
- [ ] +Datatabulky - přidat možnost uspořádání podle více sloupců kliknutím s klávesou `SHIFT`.
- [ ] +Datatabulky - přidat možnost `hideOnDuplicate` pro karty v editoru, nezapomenout i na třídu`DataTableTab` aby to šlo nastavit iv anotaci.
- [ ] +Průzkumník - do Média této stránky přidat možnost přesunout se do rodičovské složky.
- [ ] +Konfigurace - v případě `cluster` instalace doplnit možnost nastavit proměnnou pouze pro aktuální uzel (neuložit ji do databáze). Původně to řešilo výběrové pole `applyToAllClusterNodes`.
- [ ] +Číselníky - přidat možnost definovat typ pole pro řetězec jako máme ve volitelných polích.
- [x] +Statistika - do sekce návštěvnost přidat sumární počet Vidění a Návštěv pro snadný přehled celkové návštěvnosti za zvolené období (součet čísel v tabulce). Mohlo by jít doplnit `footerCallback` - https://datatables.net/examples/advanced\_init/footer\_callback.html (#57929).
- [x] +Zrcadlení struktury - přidat možnost vymazat `sync_id` hodnoty pro zvolenou složku (rekurzivní). Aby bylo snadno možné zrušit/resetovat synchronizaci. Také existuje problém, že naklonuju `SK,DE,EN` pak vypnu `DE` a chci mít jen `EN` ale zrcadlení stále aplikuje změny i na `DE` složka (#57561).
- [ ] +Do testů v GitHube přidat verifikaci `autoupdate` pro všechny podporované databázové servery - tedy inicializovat prázdnou databázi a ověřit všechny `autoupdate` a ověřit, že projdou bez chyby. Udělat jako samostatnou pipeline.
- [ ] Galerie - umožnit změnu cesty galerie (složky) a nastavit vše s tím spojené.
- [ ] +Konfigurace - doplnit sloupec skupina s hodnotou `modules` konfigurační proměnné (výběrové pole, může mít více hodnot). Doplnit možnost zobrazit i nenastavené proměnné (neboli kompletní seznam). Vytvořit nástroj pro vygenerování všech proměnných podle skupin/modulů do `md` souboru v dokumentaci pro přehled všech proměnných.
- [ ] +Pokud mám neuloženou stránku s titulkem Úklid a už existuje jiná stránka s názvem Úklid, tak nově nahrané obrázky se před jejím uložením nahrají do složky `upratovanie`. Ale když se stránka uloží, získá URL adresu úklid-2.html a další obrázky se již nahrají do složky `upratovanie-2`. Je třeba upravit kód v `getPageUploadSubDir` tak, aby místo přímého použití titulku stránky zkusil získat URL adresu pro novou stránku a to pak použil.
- [x] +HTTP hlavičky - rozšířit maximální velikost hodnoty HTTP hlavičky na více než 255 znaků, pro `Content-Security-Policy` je to nedostatečná velikost (#PR83, #58129).
- [x] +Integrace AI nástrojů, pomocníků, asistentů (#57997).
- [ ] +V editaci profilu se nezobrazí API klíč po jeho vygenerování, notifikace se nepřenesou do rodičovského okna.
- [ ] +Formuláře - upravit ochranu formulářů tak, aby se nepoužíval `document.write`.
- [ ] +Přidat možnost nastavit typ `textarea` jako je v AI asistentech is čísly řádků, například. do skriptů nebo jinde, kde se předpokládá psaní kódu.
- [x] +Novinky - přesunout pole `contextClasses` z aplikace novinky do šablony novinek. Pole nastavit jako `hidden` aby zůstalo funkční (někde může být nastaveno), pokud je prázdné použít hodnotu ze šablony. Musí tedy fungovat obě možnosti, lze spojit obě hodnoty do jednoho seznamu (#58245).
- [ ] +Vnořené datatabulky - nastavit počet záznamů v režimu `auto` podle velikosti oblasti vnořené datatabulky.
- [ ] +Funkce odeslat email později používá `sendMailSaveEmailPath`, které neumí uložit soubor podle aktuální domény, zamyslet se nad řešením. Možná je tomu tak kvůli tomu, že se emaily posílají na pozadí kde doména nemusí být známa.

## 2024

- [x] Přechod na Java 17 - WebJET od verze 2024.0 bude vyžadovat Java verze 17. (#54425)
- [x] Výměna sady ikon z `FontAwesome` za sadu `Tabler Icons` (#56397).
- [x] Publikování JAR souborů do `Maven Central` (#43144).
- [x] Nová verze aplikace Nákupní košík/`eshop`, ukázková verze web stránky, integrace přes API na online fakturační systémy, aktualizované integrace na platební brány (#56329,56385,56325).
- [x] Překladové klíče - možnost importovat pouze neexistující klíče (#56061).
- [ ] ~~`quill` - přidat možnost nastavit položky menu včetně barev.~~
- [ ] ~~Aplikace - možnost nákupu aplikace pro OpenSource verzi (#55825).~~
- [x] Web stránky - je-li zapnuta konf. proměnná `syncGroupAndWebpageTitle` a jedna stránka je nastavena jako hlavní více složek vypnout přejmenování názvu složky podle hlavní stránky. Plus když je hlavní stránka v jiné složce, také nepřejmenovávat (#56477).
- [x] Audit - předělat Změněné stránky a Čeká na publikování do datatabulek (#56165).
- [x] Blog - administrace `bloggerov` předělat do datatabulek (#56169).
- [x] Blog - komentáře - integrovat na sekci Diskuse, přidat `bloggerom` práva i na sekci Diskuse (#56173).
- [x] AB testování - předělání do DT, využít možnosti z `news` aplikace, provést nastavení konf. proměnných podle zadaného prefixu (#56177).
- [x] Kalendář událostí - neschválené a doporučení události - předělat do DT, využít kód pro seznam událostí (#56181).
- [x] Editor - při vytvoření odkazu na email to v okně automaticky přidává http, i když odkaz nakonec vloží správně s `mailto:` prefixem (#56189).
- [x] Dotazníky - předělat aplikaci do datatabulek (#55949).
- [x] +PostgreSQL - přidat podporu databáze (#56305).
- [x] Formulář snadno - přidat možnosti jako mají standardní formuláře (hlavně nastavení přesměrování po odeslání), možnost odeslat formulář i na uživatele (pro kontrolu) (#56481).
- [x] +Formulář snadno - přidat podporu pro wysiwyg pole (#56481).
- [x] +Smazat nepoužívané a vyměnit málo používané knihovny (#56265).
- [x] +MultiWeb - ověřit funkčnost, doplnit potřebná práva (#56421, #56405, #56425).
- [x] +Hromadný email - přidat podporu odhlášení jedním klikem (#56409).
- [ ] ~~+Možnost provést Thymeleaf kód v hlavičce/patičce a možná i v těle web stránky.~~
- [x] +Vyměnit databázový pool za `HikariCP` (#56821).
- [x] +Úrovně logování - předělat na datatabulku (#56833).
- [x] +DBPool - přechod z `Apache DBCP` na `HikariCP` (#56821).
- [x] Dokumentace - překlad do Angličtiny (#56237,#56249,#56773).
- [x] +Dokumentace - překlad do Češtiny (#57033).
- [x] +A/B Testování - vypnout pro vyhledávače/boty, indikovat verzi pro Ninja objekt (#56677).
- [x] +Datatabulky - možnost přeskočit řádek při importu když je vadný (#56465).
- [x] +Přesměrování - optimalizovat získání přesměrování bez druhotného získávání přesměrovacího kódu z databáze (#53469).
- [ ] ~~+Bezpečnost - přidat podporu generování `nonce` pro `Content-Security-Policy` hlavičku, viz. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.~~
- [x] +Galerie - přidat možnost změnit velikost obrázku.
- [ ] ~~+Formuláře - přidat možnost volat Java třídu pro validaci formuláře.~~
- [x] +Indexování souborů - doplnit do Perex-Začátek publikování datum poslední změny souboru (#57177).
- [x] +Překladové klíče - do REST služby `/rest/properties/` přidat možnost filtrovat klíče podle konf. proměnné, aby nebylo možné veřejně získat všechny klíče z WebJET CMS (#57202).
- [x] +Web stránky - auditovat úspěšné časové publikování web stránky, možnost poslat notifikaci autorovi web stránky (#57173).
- [x] +Zobrazovat informaci o platnosti licence 2 měsíce před jejím expirací na úvodní obrazovce (#57169).
- [ ] ~~+Značky - filtrovat podle aktuální domény aby to bylo stejné jako v jiných částech.~~
- [x] +Audit - doplnit HTTP hlavičku `Referrer` aby bylo možné identifikovat původní stránku ze které bylo volání provedeno (#57565).
- [ ] ~~+Import uživatelů - pokud není zadané heslo, tak vygenerovat (pro nové uživatele), není-li posílán stav `available` nastavit na `true`.~~
- [ ] ~~+V testech nějak automatizovaně kontrolovat výskyt `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` což jsou nesprávná čekání bez definovaného času, způsobí zaseknutí automatizovaných testů.~~
- [ ] ~~+Doplnit aplikaci pro přesměrování hlavní stránky na `/sk/` nebo `/en/` podle jazyka prohlížeče.~~
- [ ] ~~+Upravit vymazání konfigurace tak, že při vymazání se jí nastaví původní hodnota definovaná v `Constants`.~~
- [x] +Galerie - přidat pole URL adresa zdroje obrázku s možností zadat adresu, ze které jsme obrázek získali, automaticky nastavit při použití foto banky.
- [ ] ~~+Galerie - při duplikování obrázku umožnit změnu "Složka", abychom uměli duplikovat obrázky do jiné než aktuální složky.~~
- [ ] ~~+Hromadný email - auditovat změny ve skupinách uživatelů.~~
- [ ] ~~+Archiv souborů - předělat do datových tabulek (#57317).~~
- [ ] ~~+Volitelná pole - přidat možnost výběru více položek pro napojení na číselník.~~
- [x] +Vyhledávání v administraci - předělání do datové tabulky (#57333).
- [x] +Anketa - doplnění původní funkcionality zobrazení statistiky (#57337).
- [x] +Značky - doplnit možnost definovat název ve více jazycích, doplnit volitelná pole (#57273,#57449)
- [x] +Aplikace - předělat dialog nastavení aplikací v editoru web stránek ze starého JSP na datovou tabulku (#57157,#57161,#57409).
- [x] +Web stránky, galerie - doplnit možnost vyhledávat ve stromové struktuře (#57265,#57437)

## 2023.52 / 2024.0

> Verze 2023.52 / 2024.0 je zaměřena na zlepšení bezpečnosti, přechod z knihovny `Struts` na `Spring`.

- [x] Aktualizace - předělat do nového designu, doplnit možnost aktualizace s použitím jaro packages (#55797).
- [x] Klonování struktury - předělat na používání Zrcadlení struktury s možností překladu (#55733).
- [x] Rating - předělat na Spring (#55729).
- [x] Fórum - předělat do Spring (#55649).
- [x] Uživatelé - přidat sloupec skupina práv do datatabulky (#55601).
- [ ] Galerie - nový typ pro zobrazení malého počtu fotek s expanzí, možnost přechodu mezi všemi fotkami v článku (#55349).
- [ ] ~~Přidat možnost autorizace přes `OAuth2`, možnost použít `mock` server https://github.com/navikt/mock-oauth2-server nebo https://github.com/patientsknowbest/fake-oauth2-server (#56665).~~
- [ ] ~~Autorizace přes `SAML` - integrovat knihovnu [Spring SAML](https://spring.io/projects/spring-security-saml) pro možnost autentifikace vůči `ADFS/SAML` serveru.~~
- [x] Web stránky - přidat možnost obnovení smazané složky tak, aby se korektně nastavil web stránkám atribut `available` (#55937).
- [ ] Průzkumník - upravit do nového designu, aktualizovat JS kód elfinder (#55849).
- [x] Web stránky - při vytváření odkazů v dialogovém okně zapamatovat poslední adresář a při přidání dalšího odkazu jej rovnou použít (#54953-29).
- [x] Kalendář událostí - pokud se zadá událost v říjnu na listopad kdy dochází k posunu času zobrazuje se s o hodinu posunutým časem (#56857).
- [ ] upravit posílání zpráv mezi administrátory z vyskakovacího okna na lepší uživatelské rozhraní.
- [x] Aplikace - zlepšit popis každé aplikace, aktualizovat obrázky aplikace (#55293).
- [x] Foto galerie - optimalizovat množství načtených dat (#56093).
- [x] Foto galerie - image editor se inicializuje při každém otevření okna, upravit, aby se použil již existující editor (#55853).
- [x] Datatabulky - do automatizovaného testu doplnit testování funkce Duplikovat (#56849).
- [ ] Banner - přidat možnost používání šablon pro zobrazení banneru. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.
- [ ] Ověřit jak fungují SEO rozšíření pro WordPress (např. https://yoast.com/beginners-guide-yoast-seo/) a navrhnout podobné řešení pro WebJET CMS. Integrovat něco jako https://github.com/thinhlam/seo-defect-checker do editoru (#56853).
- [ ] Přidat blokem v PageBuilder-i nějaký data atribut se jménem bloku a ten pak zobrazovat ve vlastnostech bloku, aby se dalo zpětně identifikovat jaký to je blok.
- [ ] Na úvodní obrazovce implementovat mini aplikaci todo.
- [ ] DT - po přidání záznamu přestránkovat na stranu, kde se záznam nachází (typicky poslední strana)
- [x] Editor - přidat možnost zobrazit pro vybrané webové stránky editor typu HTML, který nebude možné přepnout do WYSIWYG režimu. Použity pro speciální stránky integrující různé JavaScript aplikace nebo speciální komponenty. Vhodný editor je [Ace-code](https://www.npmjs.com/package/ace-code) (#56129).
- [x] Fulltext - složka `files` ve web stránkách s indexem přesunout do karty System (54953-30).
- [x] +Uživatelé - zamezit smazání sebe sama (#55917).
- [x] +Novinky - doplnit kontrolu práv na složku a zobrazit jen takové, na které má uživatel práva (#56661).
- [x] +Aplikace - možnost podmíněně zobrazit aplikace podle typu zařízení, implementovat genericky, nastavitelné přes UI pro Banner (#55921).
- [x] +Statistika - omezení zobrazených statistik pouze na složky, na které má uživatel právo (#55941).
- [x] +Restaurační menu - předělat aplikaci do datatabulek (#55945).
- [x] +Přesměrování - oddělit přesměrování podle zvolené domény (#55957).
- [x] +Perex skupiny - oddělit administraci perex skupin podle práv uživatele na web stránky (#55961).
- [x] +Banner - podmíněné zobrazení podle zařízení mobil/tablet/desktop (#55997).
- [x] +Page Builder - generování kotev/horního menu na základě vložených sekcí (#56017).
- [x] +Volitelná pole - přidat pole typu `UUID` pro generování identifikátorů (#55953).
- [x] +Import web stránek - předělat do Spring (#55905).
- [x] +Monitorování serveru - CPU - `oshi-core` nefunguje na Windows 11 (#55865).
- [x] +Hlavičky - možnost definovat příponu souboru pro které se hlavička nastaví (#56109).

## 2023.40

> Verze 2023.40 upravuje uživatelské rozhraní na základě nových UX návrhů, zlepšuje použitelnost datatabulek.

- [x] Migrace na vyšší verzi Javy s `LTS` podporou (Java 11 nebo ideální až Java 17) (#54425).
- [x] Banner - přidat možnost nastavit adresářovou strukturu stránek ve které se bude banner zobrazovat a možnost definovat výjimky kde se nebude (#55285).
- [x] Hlavičky - přidat novou aplikaci, která umožňuje definovat libovolnou HTTP hlavičku podle URL adresy (#55165).
- [ ] ~~(přesun do 2024.0)upravit posílání zpráv mezi administrátory z vyskakovacího okna na lepší uživatelské rozhraní~~
- [ ] ~~(přesun do 2024.0)Aplikace - zlepšit popis každé aplikace, aktualizovat obrázky aplikace (#55293).~~
- [ ] ~~(přesun do 2024.0)Foto galerie - optimalizovat množství načtených dat~~
- [ ] ~~(přesun do 2024.0)Foto galerie - image editor se inicializuje při každém otevření okna, upravit, aby se použil již existující editor (#55853)~~
- [ ] ~~(přesun do 2024.0)Datatabulky - do automatizovaného testu doplnit testování funkce Duplikovat.~~
- [ ] ~~(přesun do 2024.0)Banner - přidat možnost používání šablon pro zobrazení banneru. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.~~
- [ ] ~~(přesun do 2024.0)Ověřit jak fungují SEO rozšíření pro WordPress (např. https://yoast.com/beginners-guide-yoast-seo/) a navrhnout podobné řešení pro WebJET CMS. Integrovat něco jako https://github.com/thinhlam/seo-defect-checker do editoru.~~
- [ ] ~~(přesun do 2024.0)Přidat blokům v PageBuilder-i nějaký data atribut se jménem bloku a ten pak zobrazovat ve vlastnostech bloku, aby se dalo zpětně identifikovat jaký to je blok.~~
- [x] Číselníky - v exportu se nenachází atribut, že je záznam smazán a poté po importu se záznam aktivuje (#55541).
- [ ] ~~(přesun do 2024.0)Na úvodní obrazovce implementovat mini aplikaci todo.~~
- [ ] ~~(přesun do 2024.0)DT - po přidání záznamu přestránkovat na stranu, kde se záznam nachází (typicky poslední strana)~~
- [ ] ~~(přesun do 2024.0)Editor - přidat možnost zobrazit pro vybrané web stránky editor typu HTML, který nebude možné přepnout do WYSIWYG režimu. Použity pro speciální stránky integrující různé JavaScript aplikace nebo speciální komponenty. Vhodný editor je [Ace-code](https://www.npmjs.com/package/ace-code).~~
- [x] Publikování OpenSource/Community verze WebJET CMS na `github.com` (#55789,#55773,#55829,#55749).
- [x] +Datatabulky - ve web stránkách/galerii při nastavení jiného poměru sloupců se špatně zobrazují tlačítka (#54953-22).
- [x] +Překlady - doplnit do Zrcadlení struktury možnost překládat i text stránky přes [DeppL](https://www.deepl.com/docs-api/html/) (#55709).
- [ ] ~~(přesun do 2024.0)+Autorizace přes `SAML` - integrovat knihovnu [Spring SAML](https://spring.io/projects/spring-security-saml) pro možnost autentifikace vůči `ADFS/SAML` serveru.~~
- [ ] ~~(přesun do 2024.0)+Přechod ze Struts framework na Spring - postupně přepsat kód všech tříd do Spring kontrolorů (#55389,#55469,#55489,#55493,#55469,#55501,#55609,#55613
- [x] +Předělat administraci seznamu novinek do nového designu a datatabulky, využít co nejvíce kódu ze stávajícího seznamu web stránek (#55441).
- [x] +Aktualizovat knihovnu `amcharts` pro grafy na novější verzi 5 (#55405).
- [x] +Předělat aplikaci Novinky do nového designu (#55441).
- [x] +Přidat podporu vývoje přes DevContainers (#55345).
- [x] +Monitorování serveru - předělat sekce Aplikace, Web Stránky a SQL dotazy do nových datatabulek (#55497).
- [x] +Web stránky - schvalování - předělat ze `Struts` na `Spring`, doplnit testy (#55493).
- [x] +SEO - předělat na repozitáře a datatables (#55537).
- [x] +Autorizace uživatelů - v administraci chybí původní možnost z v8 (#55545).
- [x] +DT - vyhledávání záznamu podle ID, lepší řešení pro hledání web stránky podle docid když je v seznamu mnoho stran (#55581).
- [x] +Diskuse - předělat administraci do datatables (#55501).
- [x] +Monitorování serveru - předělat monitorování Aplikace/Web stránky/SQL dotazy do datatables/nových grafů (#55497).
- [x] +Proxy - přidat podporu pro volání REST služeb bez vkládání výstupu do web stránky (#55689).
- [x] +Banner - přidat podporu pro video soubory včetně obsahového banneru (#55817).
- [x] +Překladové klíče - možnost importu pouze jednoho jazyka bez dopadu na překlady v jiných jazycích (import pouze zadaného sloupce z Excelu) (#55697).
- [x] +Média - přidat podporu volitelných polí (#55685).

## 2023.18

> Verze 2023.18 mění požadavek na provoz řešení na Java verze 17. Toto je zásadní změna a vyžaduje i změnu na serverech. Z toho důvodu je provedena hned na začátku roku.

- [ ] ~~(přesun do 2023.40)Migrace na vyšší verzi Javy s `LTS` podporou (Java 11 nebo ideální až Java 17) (#54425)~~.
- [x] Integrace kódu verze 8 do projektu, odstranění duplicitních stránek, zrušení možnosti přepnutí do verze 8, ponechání pouze nových verzí stránek (#54993).
- [x] Aktualizace CK Editor na novější verzi (#55093).
- [x] Formuláře - doplnit funkci archivovat formulář (#54993).
- [ ] ~~(přesun do 2023.40)Banner - přidat možnost nastavit adresářovou strukturu stránek ve které se bude banner zobrazovat a možnost definovat výjimky kde se nebude.~~
- [x] Vyhledávání - `Lucene` - přidat indexování jazyka na základě jazyka stránky, čili do indexu by se dostaly jen stránky v zadaném jazyce šablony (#54273-34).
- [ ] ~~(přesun do 2023.40)Hlavičky - přidat novou aplikaci, která umožňuje definovat libovolnou HTTP hlavičku podle URL adresy (#55165).~~
- [x] Atributy stránky - doplněna možnost nastavení atributů stránky v editoru (#55145).
- [x] Šablony - přidat možnost sloučení šablon (#55021).
- [ ] +Audit - přidat anotaci na primární klíč pokud je jiný než ID, konkrétně pro editaci překladových klíčů se neaudituje hodnota `key` ale `id` které je nepodstatné.
- [x] +Galerie - Na multidomain se používala struktura `/images/DOMENA/gallery` co teď není vidět v galerii. Je třeba to tam nějak doplnit (#54953-4).
- [x] +Překlady - možnost dynamické definice podporovaných jazyků v šablonách, překladových klíčích atd. (#MR332).
- [x] +Bezpečnost - logy serveru jsou dostupné vrámci audit práva, je třeba to oddělit do samostatného práva (#54953-5).
- [x] +Zlepšit podporu DB serveru Oracle, otestovat funkčnost entit (#54953-6).
- [x] +Web stránky - při duplikování web stránky se mají duplikovat i připojená média (#54953-6).
- [x] +Web stránky/galerie - přidat možnost nastavit poměr stran pro zobrazení stromové struktury a tabulky (#54953-7).
- [x] +Překladové klíče - upravit editaci na tabulkové zobrazení kde ve sloupcích jsou jednotlivé jazyky pro možnost najednou editovat překlady ve všech jazycích (#55001).
- [x] +Banner - předělat statistiku zobrazení bannerů do nového designu (#54989).
- [x] +Překladové klíče - doplnit kontrolu počtu překladových jazyků, aby to nepadalo na `fieldK` (#MR344).
- [x] +Číselník - předělat do nového designu aby bylo možné odstranit závislost na staré verze datatables.net (#55009).
- [ ] +Opravit chybu v editoru Např. v aplikaci galerie - při posunu okna chycením za titulek Aplikace se špatně posune a následně neustále sleduje kurzor. Je třeba potom kliknout dolů u tlačítka OK pro vypnutí posouvání okna.
- [x] +Proxy - předělat do nového designu (#55025).

## 2022.52 / 2023.0

> Verze 2022.52 představuje dlouhodobý seznam úkolů, na které nevyšel čas v podrobnějším plánu na rok 2022.

- [ ] ~~zatím zrušeno: nahrát videa používání WebJET CMS automatizovaným způsobem - https://dev.to/yannbertrand/automated-screen-recording-with-javascript-18he nebo https://www.macrorecorder.com~~
- [x] přidat statistiku testů přes modul `Allure` - https://codecept.io/plugins/#allure (#54437).
- [x] přidat `code coverage` report, například. přes JaCoCo - https://docs.gradle.org/current/userguide/jacoco\_plugin.html (#54909).
- [ ] ~~(přesun do 2023.40) upravit posílání zpráv mezi administrátory z vyskakovacího okna na lepší uživatelské rozhraní~~
- [ ] ~~(přesun do 2023.40) Aplikace - zlepšit popis každé aplikace, aktualizovat obrázky aplikace.~~
- [x] Galerie - přidat fyzickou změnu jména souboru (včetně vygenerovaných `s_` a `o_` obrázků) po změně atributu Jméno souboru v editoru (#39751-52).
- [x] +Datatabulky - zapamatovat si počet záznamů na stranu pro každou datatabulku (#39751-50).
- [ ] +`Command Palette` - přidat příkazovou paletu s integrovaným vyhledáváním podobně jako má VS Code - https://trevorsullivan.net/2019/09/18/frictionless-user-experiences-add-a-command-palette-to-your-react-application/
- [ ] ~~(přesun do 2023.40) +foto galerie - optimalizovat množství načtených dat~~
- [ ] ~~(přesun do 2023.40) +foto galerie - image editor se inicializuje při každém otevření okna, upravit, aby se použil již existující editor~~
- [ ] ~~(přesun do 2023.40) +Datatabulky - do automatizovaného testu doplnit testování funkce Duplikovat.~~
- [ ] ~~(přesun do 2023.40) +Banner - přidat možnost používání šablon pro zobrazení banneru. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.~~
- [ ] ~~(přesun do 2023.40) +Ověřit jak fungují SEO rozšíření pro WordPress (např. https://yoast.com/beginners-guide-yoast-seo/) a navrhnout podobné řešení pro WebJET CMS.~~
- [ ] ~~(přesun do 2023.40) +Přidat blokům v PageBuilder-i nějaký data atribut se jménem bloku a ten pak zobrazovat ve vlastnostech bloku, aby se dalo zpětně identifikovat jaký to je blok.~~
- [x] Zvážit možnost použití [HotSwapAgent](http://hotswapagent.org/) pro rychlejší vývoj bez potřeby restartů (vyřešeno přestavením `build.gradle` pro podporu standardního HotSwap).
- [ ] ~~(přesun do 2023.40)+Číselníky - v exportu se nenachází atribut, že je záznam smazán a poté po importu se záznam aktivuje.~~
- [ ] ~~(přesun do 2023.40)Na úvodní obrazovce implementovat mini aplikaci todo.~~
- [ ] ~~(přesun do 2023.40)+DT - po přidání záznamu pro stránkovat na stranu, kde se záznam nachází (typicky poslední strana)~~
- [x] +Doplnit překlady textů z pug souborů (např. gallery.pug), hledat například. písmeno á nebo podobné, které by se v nepřeložených textech mohlo nacházet.
- [x] +Přidat možnost ponechat okno editoru otevřené po uložení (viz. https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open, closeOnComplete, https://editor.datatables.net/reference/api/submit()). Realizováno pomocí klávesové zkratky `CTRL+s/CMD+s` (#54273-26).
- [x] `Combine` / mazání dat - při smazání všech dat v cache přenést korektně i na `Cluster` a tam také smazat stejně, aktuální `ClusterRefresher` smaže jen cache, ne `DocDB/GroupsDB` a nenastaví nový čas pro `combime` (#54673).
- [x] +REST - přidat možnost definování API klíčů (tokenů) používaných namísto generovaných CSRF tokenů pro volání REST externími službami (#54941).
- [ ] ~~(přesun do 2023.18) +Hlavičky - přidat novou aplikaci, která umožňuje definovat libovolnou HTTP hlavičku podle URL adresy.~~
- [x] +Skripty - přidat `autocomplete` na pole Umístění skriptu v šabloně (#54941).
- [x] +Kontrola odkazů - upravit původní verzi Kontrola odkazů na datatabulku a propojit na seznam web stránek (#54697).
- [x] +Rezervace a rezervační objekty - předělat administraci na datatabulku a Spring (#54701).
- [x] +Anketa - předělat administraci na datatabulku a Spring (#54705).
- [x] +Atributy stránky - předělat administraci na datatabulku a Spring (#54709).
- [x] +Šablony - přidat kartu se seznamem web stránek používajících šablonu (#54693).
- [x] +Statistika - předělat do nového designu a grafů (#54497, #54585, #54897).
- [x] +Vyhledávání - `Lucene` - přidat podporu vyhledávání v českém a anglickém jazyce (#54273-34).
- [x] +Vyřešit problém s verzemi FontAwesome v administraci a web stránkách (#39751-51).
- [x] +Bannerový systém - doplnit možnost zobrazení kampaňového banneru podle URL parametru (#MR296).
- [x] +Galerie - rozšířit možnosti editoru, přidat horní/dolní index, možnost vložit HTML kód, nastavení barev. Zlepšit podporu vůči starému editoru/html kódu (#54857-4).
- [x] +Galerie - přizpůsobit počet fotek velikosti okna, pamatovat si nastavenou velikost obrázků v tabulce (#54857-4, #market-245).
- [x] +Web stránky - zobrazení perex skupin jako multi výběrové pole s vyhledáváním namísto standardních `checkbox` polí (pokud existuje mnoho perex skupin) (#market-245).
- [x] +Formulář snadno - přidat typ pole `select` - standardní výběrové pole, přidat možnost zadat jinou hodnotu a zobrazený text (#MR298, #39751-45).
- [x] +Web stránky - přidat zobrazení složek jako datové tabulky s možností hromadných operací (#54797).
- [x] +Bezpečnost - přidat možnost nastavit HTTP hlavičku `Access-Control-Allow-Origin` (#39751-44).
- [x] +Bannerový systém - doplnit možnost nastavit klienta, kontrolovat práva při zobrazení (#38751-52).
- [x] +Bannerový systém - doplnit možnost nastavit volitelná pole, refaktorovat `BannerEntity` a `BannerBean` do jednoho objektu, aby se dala použít nová pole iv JSP komponentě (#39751-52).

## 2022.40

> Verze 2022.40 je zaměřena na zlepšení bezpečnosti.

- [x] Uživatelé - přidat kontrolu hesla z historie při ukládání uživatele, neboť nyní to dovolí, ale přihlásit se s ním nelze (#54293).
- [x] Skupiny uživatelů - přidat kartu se seznamem uživatelů ve skupině práv (#54493).
- [x] Aktualizovat NPM moduly - https://www.npmjs.com/package/npm-check-updates nebo https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version (#54721).
- [x] Změnit `hash` funkci hesel na `bcrypt` (respektive něco modernějšího - https://cheatsheetseries.owasp.org/cheatsheets/Password\_Storage\_Cheat\_Sheet.html) (#54573).
- [x] Doplnit/ověřit kontrolu práv pro editaci konfiguračních proměnných (funkce editace všech proměnných) (#54749).
- [x] Validace - doplnit lepší validace na pole (např. url adresa, doména) s popisnými chybovými hlášeními a upravit `DatatableExceptionHandlerV2` aby uměl použít překladové klíče přímo z WebJET CMS is dodatečnými údaji (jako min/max hodnota a jiná makra v překladovém klíči) (#54597).
- [x] Logování - přechod z `log4j` na standard `slf4j` - vyřešeno přechodem na `logback` ().
- [x] Logování - přidat systém pro dočasný zápis logů do RAM paměti pro možnost shlédnutí posledních logů přes administraci, pokud není přístup k logům na souborovém systému. Možnost získat i logy z nodů clusteru. přes dočasný zápis do dat `conf` tabulky (#54269).
- [x] Na úvodní stránku doplnit kontroly aktualizace a databáze jako ve WebJET 8 + ~~otevření nápovědy u nové verze~~ (v úvodu je odkaz na seznam změn) (#54457).
- [x] Upravit vizuál nevyhovujícího hesla a změny hesla na WebJET 9 (minimálně vyměnit obrázek pozadí) (#54573).
- [x] Bezpečnost - přidat kontrolu přístupu k administraci - aktuálně se kontroluje pouze volání REST služby, ne volání `Thymeleaf` stránky (#54649)
- [x] Datatabulka - přidat do dialogového okna smazání jméno položky, třeba je myslet na situaci kdy je označeno více řádků (#54753).
- [ ] ~~(přesun do 2023.18) +Migrace na vyšší verzi Javy s `LTS` podporou (Java 11 nebo ideální až Java 17) (#54425).~~
- [ ] ~~(přesun do 2023.18) +Formuláře - doplnit funkci archivovat formulář.~~
- [x] +Web stránky - doplnit funkci náhledu stránky před uložením (#54557).
- [x] +Web stránky - přidat možnost `drag&drop` stránek do adresářové struktury (#54513).
- [x] +Připravit ukázkovou aplikaci pro programátora do administrace kde bude možné vybrat nějaké možnosti ve formuláři a nahrát soubor, který se serverově zpracuje. Následně se zobrazí výsledek (#54449).
- [x] +Připravit ukázkovou šablonu s použitím Thymeleaf a případně PUG souborů s integrací na PageBuilder (#54345).
- [x] +Předělat aplikaci Hromadný email-Odeslané emaily do nového designu (#54377).
- [x] +Doplnit CZ a EN překlady (#53805).
- [x] +Doplnit k editaci skupiny uživatelů kartu se seznamem uživatelů v dané skupině, upravit seznam web stránek na serverové stránkování (#54993).
- [ ] ~~(přesun do 2023.18) +Banner - přidat možnost nastavit adresářovou strukturu stránek ve které se bude banner zobrazovat a možnost definovat výjimky kde se nebude.~~
- [x] +Doplnit pamatování způsobu uspořádání pro každou datatabulku (#54513-22).~~
- [x] +Doplnit X pro zavření okna ik ikoně maximalizace.
- [ ] ~~(přesun do 2022.52) +Šablony - přidat kartu se seznamem web stránek používajících šablonu (#54693).~~
- [x] +Web stránky - složky lze označit více s CTRL, ale nejdou se takto všechny smazat (smaže jen první) (#54797).
- [x] +Datatabulky - dynamicky určit počet sloupců tabulky, aby nezůstávalo prázdné místo, což evokuje, že tam již nejsou další záznamy (uživatel si nevšimne, že tam je stránkování) (#54273-26).
- [x] +Datatabulky - přidat možnost použití multiselect v datatabulce a v aplikaci v editoru (#54273-25).
- [x] +Kalendář událostí - předělat aplikaci do nového designu (#54473).
- [x] +Web stránky - přidat možnost zobrazit ID složky a pořadí uspořádání ve stromové struktuře stránek (#54513-1).
- [x] +Web stránky - přidat možnost uložit stránku jako A/B test (#54513-2).
- [x] +Formuláře - přidat možnost exportu od posledního exportu, vybrané záznamy, podle aktuálního filtru (#54513-3)
- [x] +Generovat `POM` soubor se závislostmi/knihovnami přímo z gradle verze, namísto úprav v původní WJ8 verzi (#54673).
- [ ] ~~(přesun do 2022.52) +Skripty - přidat `autocomplete` na pole Umístění skriptu v šabloně.~~
- [ ] ~~(přesun do 2022.52) +Kontrola odkazů - upravit původní verzi Kontrola odkazů na datatabulku a propojit na seznam web stránek (#54697).~~
- [ ] ~~(přesun do 2022.52) +Rezervace a rezervační objekty - předělat administraci na datatabulku a Spring (#54701).~~
- [ ] ~~(přesun do 2022.52) +Anketa - předělat administraci na datatabulku a Spring (#54705).~~
- [ ] ~~(přesun do 2022.52) +Atributy stránky - předělat administraci na datatabulku a Spring (#54709).~~
- [x] +Možnost posouvat okno editoru (#54513-21).
- [x] +Zapamatovat pořadí sloupců v tabulce pro každou tabulku (#54513-22)
- [x] +Zapamatovat si naposledy otevřenou složku v seznamu web stránek (#39751-45)

## 2022.18

> Verze 2022.18 se soustřeďuje na zlepšení použitelnosti.

- [x] editor - zobrazení stránky v inline editaci (je-li nastaven inline editor na Page Builder) (#54349).
- [x] DT při klientském stránkování označení všech řádků se tváří jako označení na jedné straně, ale smaže vše i na ostatních stranách (#54281).
- [x] Přidat tlačítko do oznámení že existuje rozpracovaná verze pro otevření (#54357).
- [x] Přidat sloupec s ikonami pro zobrazení webové stránky (#54257).
- [x] Předělat DocID/web stránka na ID ve web stránkách (#53805).
- [x] Úvod - přidat aplikaci do pravého sloupce se seznamem Oblíbených URL adres (#54177)
- [x] Úvod - přidat jednoduchý formulář pro poslání připomínky k WJ (#54181)
- [x] Úvod - zfunkčnit kliknutí na ikony v prvním bloku (návštěvy, formuláře...) (#53805)
- [x] Přidat možnost nastavení dvou faktorové autorizace administrátorem (#54429)
- [x] Pro WJ Spring komponenty mapovat `editor_component`. Momentálně to hledá jsp v `INCLUDE` a pro tyto komponenty je tam například. `sk.iway.xxx.component.SiteBrowserComponent` a tak to nenajde a vrátí `appstore` (#54333).
- [x] Web stránky - doplnit možnost změnit doménu po zadání `docid` pokud se stránka nachází v jiné doméně a automaticky přepnout záložku na Systém nebo Koš pokud se nachází v této větvi (#54397).
- [x] Překladové klíče - přidat tlačítko k obnovení klíčů z `properties` souboru, obnovit klíče po smazání záznamu a nezobrazit chybu mazání pokud klíč neexistuje (neboť je z properties souboru - id větší než 1000000) (#54401).
- [x] DT Editor - přidat notifikaci při odchodu ze stránky (#54413).
- [x] Volitelná pole - doplnit možnost vybrat adresář (#54433).
- [ ] ~~+(přesun do 2022.52) DT - po přidání záznamu přestránkovat na stranu, kde se záznam nachází (typicky poslední strana)~~
- [x] +Doladit verzi pro Firefox, přidat automatizované testování i pro Firefox (#54437).
- [x] +Po přepnutí do WJ8 verze chybí linky pro přidání přesměrování (a možná i jiné) (#53805).
- [x] +Do WJ8 rozhraní doplnit ikonu k přepnutí na V9 verzi (#53805).
- [ ] ~~+(přesun do 2022.52) Doplnit překlady textů z pug souborů (např. gallery.pug), hledat nap. písmeno á nebo podobné, které by se v nepřeložených textech mohlo nacházet.~~
- [x] +Při importu konfigurace s aktualizací podle názvu to padne na chybě, vhodné by bylo vyměnit to za původní import is náhledem změn.
- [ ] ~~+ (přesun do 2022.36) Připravit ukázkovou aplikaci pro programátora do administrace kde bude možné vybrat nějaké možnosti ve formuláři a nahrát soubor, který se serverově zpracuje. Následně se zobrazí výsledek (#54449).~~
- [x] +Aktualizovat Java knihovny na aktuální verzi bez zranitelností.
- [x] +Přidat možnost použít `Thymeleaf` šablony pro `@WebjetComponent` (#54273).
- [x] +Zřídit instanci s veřejným git repozitářem (#54285).
- [ ] ~~+(přesun do 2022.52) Přidat možnost ponechat okno editoru otevřené po uložení (viz. https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open).~~
- [x] +V menu části šablony zobrazit položky editace šablon přímo na první úrovni (#54345)
- [x] +Doplnit možnost zadat `placeholder` v aplikaci Formulář snadno (#54381).
- [x] +Ověřit spuštění WebJET CMS nad Oracle a Microsoft SQL (#54457).

## 2021.52

> Verze 2021.52 je zaměřena na zlepšení práce s web stránkami a editorem.

### Web stránky

- [x] editor - doplněné info ikony k jednotlivým polím s vysvětlením jejich významu (#54253)
- [x] editor - dokončení TODO položek v editaci web stránky v záložce Přístup - nastavení zobrazení navigační lišty a mapy stránek (rozšíření datového modelu) (#54205)
- [x] editor - upravené pole URL adresa podle původního UI návrhu (zobrazena pouze koncová část s možností přepnutí zobrazení celé URL) (#54237)
- [x] editor - zobrazení informace o schvalování stránky (po uložení, když má být schválena) (#54129)
- [x] editor - zobrazení rozpracované verze stránky (#54129)
- [x] ~~editor - přidat možnost Uložit a Uložit jako~~ - nahrazeno vlastností Duplikovat datatabulky
- [x] web stránky - zobrazení web stránek iz pod adresářů (#52435)
- [x] Web stránky - adresáře - zobrazovat pole pro zadání domény pouze pro kořenové složky, automaticky nastavit doménu při uložení kořenové složky (#54249)
- [x] Web stránky - přidat informaci o editaci aktuální web stránky jiným redaktorem (#54193)
- [x] Web stránky - ověřit/doplnit kontrolu práv na přidávání/editaci/mazání, zkrácené menu a skryté adresáře v administraci (#54257).
- [ ] ~~stromová struktura - podpora klávesových zkratek pro vytvoření/editaci nové položky ve stromové struktuře~~ - nebude se realizovat, po UX konzultaci se budou všude používat jen tlačítka
- [ ] ~~stromová struktura - kontextové menu - přidáno kontextové menu ve stromové struktuře (po zvážení vhodnosti z pohledu UI/UX kontextové menu vs ikony v nástrojové liště)~~ - nebude se realizovat, po UX konzultaci se budou všude používat pouze tlačítka
- [x] +úprava buňky - před úpravou načíst data ze serveru, aby se získala `editorFields` data. V seznamu uživatelů se jinak nedá upravovat například. jméno (nebo chybí heslo). Zobrazují se chybně i některé nadpisy (Vstup do admin sekce) (#54109).
- [x] +generické řešení problému s `domain_id` (#53913)
- [x] +doplnění pole ID do každé datatabulky jak se používá pro správu web stránek (#53913)
- [x] +editovat aplikaci Otázky a odpovědi do designu WebJET 2021 (#53913)
- [x] +zlepšení použitelnosti na mobilních zařízeních (#53573, #54133)
- [x] +přidat možnost web stránku uložit jako rozpracovanou verzi pomocí zaškrtávacího pole (#54161)
- [x] +Změnit písmo na `Open Sans` (#53689).
- [x] +Integrovat automatický překlad textů přes https://www.deepl.com/translator (např. pro zrcadlení struktury, překladové texty atd.) (MR146).
- [x] +Web stránky - pokud je stránka ve více adresářích nelze editovat stránku v sekundárním adresáři (vrátí se údaje původní stránky a nespárují se data podle docid/ID řádku).
- [x] +Upravit design verze 8, aby se podobal verzi 2021 (minimální chování menu) (#54233).
- [x] +Doplnit deployment ve formátu pro aktualizaci přes Nastavení->Aktualizace WebJETu (#54225).
- [x] +Doplnit mazání starých konf. proměnných `statDistinctUsers-` a `statSessions-` v režimu `auto` clustra, doplnit zobrazení nodů do Monitorování->Aktuální hodnoty (#54453).

### Aplikace v novém designu

- [x] +Otázky a odpovědi - upravit na nový design (#53913).
- [x] +Bannerový systém - upravit na nový design (#53917).
- [x] +Hromadný e-mail - Doménové limity - upravit na nový design (#54153).
- [x] +Export dat - upravit na nový design (#54149).
- [x] +Tooltip - upravit na nový design (#53909).

### Datatabulky

- [x] +Přidat možnost zadat ID záznamu v libovolné tabulce, podobně jako je to pro web stránky.

### Export-import
- [x] +Export-import - opravit pořadí sloupců vůči hlavičce (viditelné v seznamu uživatelů, ale může být i jinde) (#54097).
- [x] +Export-import - vyřešit sloupce se stejným jménem ale jinými daty (např. v seznamu uživatelů jsou 2 adresy, což není reflektováno v excel názvech sloupců) (#54097).

### Bezpečnost

- [x] +Aktualizovat NPM moduly - https://www.npmjs.com/package/npm-check-updates nebo https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version.
- [x] +Datatabulky - do REST Controlleru přidat automatické nastavování podmínky sloupce `domain_id` (#53913).
- [x] +Upravit způsob kompilace Java tříd tak, aby se kompletně před deploymentem vše sestavilo nově a najednou. U upravených tříd tedy nemůže nastat problém v kompatibilitě.
- [x] +Doplnit kontrolu bezpečnosti v administraci v polích, ve kterých lze vkládat HTML kód (využít `org.owasp.html.PolicyFactory`) (#53931).

### Jiné úpravy

- [x] +Verze pro mobilní zařízení - optimalizovat rozhraní, datatabulky a editor pro použití v mobilních zařízeních.

## 2021.40

> Cílem verze 2021.40 je zmigrovat editaci uživatelů a modul GDPR do Datatables Editoru. Nadále budeme pracovat i na zlepšení použitelnosti práce s web stránkami – možnost Drag & Drop a kontextové menu ve stromové struktuře.

### Nové vlastnosti

- [x] uživatelé - předělané do Datatables Editor (#46873)
- [x] aplikace GDPR - předěláno do Datatables Editor (#53881, #53905)

### Web stránky

- [x] editor - volná pole - možnost nastavení typů polí `selectbox`, `autocomplete`, výběr URL obrázku/stránky (#53757)
- [x] stromová struktura - podpora drag & drop web stránek a adresářů (#52396)
- [x] +automaticky vytvářet `System` složku pro doménu a zobrazovat z něj stránky včetně pod složek (#53685)
- [x] +automatické smazání titulku Nová web stránka při kliknutí do pole a přenos do navigační lišty (#53693)
- [x] +editovat načítání seznamu složek pro skupiny šablon - zobrazit všechny složky nejen podle jména instalace pro snadnější použití šablon (#53693)
- [x] +přidat editaci perex skupin (#53701)
- [x] +doplnit kontrolu práv pro editaci překladových klíčů (funkce editace všech klíčů) (#53757)
- [x] +při přejmenování domény změnit také prefix domény konfiguračních proměnných a překladových klíčů (#market-107)
- [x] +pokud stromová struktura obsahuje max. 2 adresáře zobrazit je rozbaleno (#53805)
- [x] +při nastavování rodičovského adresáře automaticky rozbalit kořenový adresář (#53805)
- [x] +při vytvoření doménového `System` adresáře automaticky vytvořit také pod adresáře pro hlavičky, patičky a menu (#53805)
- [x] +web stránky - doplnit ikonu pro zobrazení stránky (#53753)

### Datatables

- [x] +Úprava buňky pro editor obsahující více listů (#53585).

### Obecné

- [x] aplikace galerie - upravit editaci položky stromové struktury z VUE komponenty na datatables editor (jako je u webových stránek) (#53561)
- [x] přepsat definici datatables z PUG souborů do Java anotací (#48430)
- [x] +navrhnout a zdokumentovat způsob vytváření zákaznických aplikací v novém designu WebJET 2021 (#54045)

## 2021.13

> Cílem verze 2021.13 je zlepšit použitelnost práce s web stránkami a možnost použití v projektech zákazníků. Zároveň bude ukončen vývoj nových vlastností WebJET 8 a sepsání sekce, což je nové ve verzi 8.8.

### Web stránky

- [x] editor - zlepšení viditelnosti pole Poznámka redaktora (#53131)
- [x] editor - zobrazení a správa médií (záložka Média) (#52351, #52462)
- [x] editor - zobrazení historie stránky (#53385)
- [x] web stránky - zfunkčnění tabu Naposledy upravené a Čekající ke schválení (#53493)
- [x] web stránky - import/export - doplnění původního importu a exportu stránek v XML zip do nástrojové lišty web stránek (#53497)
- [x] stromová struktura - někdy kliknutí na ikonu editovat není provedeno (po předchozím zavření dialogu přes zrušit) (#51487)
- [x] stromová struktura - dokončení TODO položek v editaci adresáře v záložce Šablona a Přístup (#51487)
- [x] stromová struktura - úprava zobrazených ikon ve stromové struktuře na základě UI/UX připomínek (#52396)
- [x] stromová struktura - zobrazení plánovaných verzí a historie adresáře (#53489)
- [x] stromová struktura - přidána ikona pro obnovení stromové struktury (#52396)
- [x] stromová struktura - přidané zobrazení `docid/groupid` a pořadí zobrazování (dle nastavení) (#53429, #53565)
- [x] stromová struktura - zfunkčnění karet Systém a Koš (#53137)
- [x] +optimalizovat rychlost zobrazení seznamu web stránek (#53513)

### Datatables editor

- [x] po zavření dialogu a otevření jiného zůstanou zobrazena chybová hlášení (#52639)
- [x] po zavření dialogu a otevření zůstane obsah posunutý dolů/v původním místě (#53119)
- [x] přidán field type datatable pro možnost vložení vnořené datatabulky do editoru (např. pro média, historii stránek a podobně) (#52351)
- [x] doplnění zobrazení error hlášení při chybě tokenu/odhlášení uživatele (#53119)
- [x] +doplnění možnosti autocomplete pro textová pole (#52999)
- [x] +zobrazení pole poznámka pro redaktora přes notifikace (#53131)
- [x] +inicializace vnořených datatabulek až po přechodu na daný list (#53425)
- [x] +integrace výběru souboru přes elfinder (#52609)

### Jiné

- [x] +Konfigurace - zobrazení plánovaných verzí a historie změn (#53089)
- [x] +doplnit možnost překládat texty v JavaScript souborů (#53128)
- [x] +Načíst data vnořené datatabulky až po kliknutí na list kde se nachází (#53425)
- [x] +kontrolu spojení se serverem a zobrazení chybového hlášení při chybě spojení nebo chybě bezpečnostního (CSRF) tokenu (#53119)
- [x] +aktualizovat testovací framework codeceptjs na aktuální verzi (#52444)
- [x] +rozdělit dokumentaci na programátorskou, uživatelskou, instalační atp. (#52384)
- [x] +zapamatování nastavení zobrazených sloupců v datatabulce pro každého uživatele zvlášť (#53545)
- [x] +nový vizuál přihlašovací obrazovky (#53617)

### Obecné

- [x] zlepšení integrace do zákaznických projektů na základě prvních testů (#52996, #53589)
- [x] udržování session se serverem, zobrazení progress baru odhlášení jako ve verzi 8 (#53119)
- [ ] sepsání sekce Co je nového pro WebJET 8.8
- [x] +úprava dokumentace na formát `docsify` a přesun na server http://docs.webjetcms.sk/ (#52384)
- [x] +aktualizace testovacího frameworku codeceptjs na verzi 3.0.4 (#52444)

<!-- deepmark-ignore-start -->
<script type="text/javascript">
setTimeout(function() {
    for (var node of document.querySelectorAll("input[type=checkbox]")) {
        node.removeAttribute("disabled");
        // Prevent click events
        node.addEventListener('click', function(event) {
            event.preventDefault();
        });
    }
}, 100);
</script>
<!-- deepmark-ignore-end -->
