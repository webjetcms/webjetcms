# ROADMAP

Seznam plánovaných verzí systému WebJET CMS. Verze jsou číslovány takto `rok.týždeň` což je také očekávaná doba dostupnosti této verze. Úkoly označené číslem tiketu již probíhají, úkoly označené číslem `[x]` jsou již implementovány, úlohy označené `+` byly přidány po přípravě `roadmapy` a ovlivnit plnění dalších úkolů.

Upozornění: jedná se o plán, nové úkoly mohou být do verze přidány v závislosti na potřebách a kapacitě a některé mohou být přidány do pozdější verze.

Vysvětlení použitých piktogramů:
- [ ] úkol probíhá podle plánu
- [ ] pokud již začal pracovat na úkolu (#ticket-id)
- [x] práce je hotová
- [ ] +pokud začíná na znaku `+` úkol byl do seznamu přidán po počátečním vytvoření plánu, takové úkoly jsou obvykle požadovány zákazníky a v případě realizace ovlivní plán. To znamená, že některé jiné úkoly nemusí být v dané verzi realizovány z důvodu nedostatku času.
- [ ] ~~vyškrtnuto~~ úkol přesunut do jiné verze, nebo řešen jiným způsobem, nebo zrušen. V popisu je vždy uvedeno číslo verze, kam byla úloha přesunuta, nebo důvod zrušení.

## 2025

- [ ] Přepnout na `Jakarta EE` - změnit balíčky Java z `javax.servlet` na adrese `jakarta.servlet`, připravte migrační skript.
- [ ] Přepněte na aplikační server Tomcat 11+.
- [ ] Přepnout na `Spring` Verze 7.
- [ ] Zavést do projektu povinnost používat `SonarLint` a formátování kódu pomocí `.editorconfig` nebo `Spotless` - Příklad https://github.com/gothinkster/spring-boot-realworld-example-app/blob/master/build.gradle.
- [ ] Primární využití úložiště GitHub pro vývoj.
- [ ] Zrušit generování artefaktů na starých `iwmsp.sk` artefakty budou k dispozici pouze prostřednictvím [Maven Central](https://mvnrepository.com/artifact/com.webjetcms/webjetcms).
- [ ] Aktualizace knihovny `pd4ml` na novou verzi si budou muset jednotlivé weby zakoupit licenci samostatně, nebude již poskytována systémem WebJET CMS.
- [ ] Migrace nejpoužívanějších aplikací na verzi Spring pomocí šablon Thymeleaf.
- [ ] Zrušení `Apache Struts` rámec, náhrada `logic:present,logic:iterate,bean:write` buď pro `JSTL` nebo implementace podobné funkce do `iwcm:present,iwcm:iterate,iwcm:beanrwite`.
- [ ] Přesunutí souborů JSP, tříd Java a knihoven JavaScript staré verze 8 na `obsolete` jarní archiv, který nebude standardní součástí systému WebJET CMS. Lze jej použít na starých projektech, kde ještě nebyly aktualizovány všechny zákaznické aplikace. `Spring` verze, ale bez podpory a aktualizací ze strany WebJET CMS.
- [ ] Statistiky - kliknutí na mapu - obnovení funkčnosti, vyřešení problému s odezvou (oddělit registraci podle šířky okna).
- [ ] Statistiky - možnost filtrovat roboty pro statistiky neúspěšných stránek.
- [ ] Soubory protokolu - filtrování podle názvu instalace.
- [ ] `quill` - přidat možnost nastavení položek menu včetně barev.
- [ ] Aplikace - možnost zakoupit aplikaci pro verzi OpenSource (#55825).
- [ ] Možnost spustit kód Thymeleaf v záhlaví/zápatí a možná i v těle webové stránky.
- [ ] Zabezpečení - přidat podporu generování `nonce` Pro `Content-Security-Policy` hlavičky, viz např. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.
- [ ] Formuláře - přidání možnosti volat třídu jazyka Java pro validaci formuláře.
- [ ] Štítky - filtrujte podle aktuální domény, aby byly stejné jako v ostatních sekcích.
- [ ] Import uživatelů - pokud není zadáno heslo, vygeneruje se (pro nové uživatele), pokud není odeslán žádný stav `available` nastavit na `true`.
- [ ] V testech nějakým způsobem automatizovaně kontrolujte výskyt `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` což jsou nesprávná čekání bez definovaného času, způsobí, že se automatické testy zaseknou.
- [ ] Přidání aplikace, která přesměruje hlavní stránku na `/sk/` nebo `/en/` podle jazyka prohlížeče.
- [ ] Upravte smazání konfigurace tak, aby se při smazání nastavila na původní hodnotu definovanou v položce `Constants`.
- [ ] Galerie - při duplikování obrázku povolte změnu "Složka", která nám umožní duplikovat obrázky do jiné složky, než je aktuální složka.
- [ ] Hromadný e-mail - audit změn v uživatelských skupinách.
- [ ] Archivní soubory - přebudování na datové tabulky (#57317).
- [ ] Nepovinná pole - přidejte možnost vybrat více položek, které chcete propojit s číselníkem.
- [ ] eCommerce - integrace s platební bránou `GoPay` (#56609).
- [ ] Přidat možnost autorizace prostřednictvím `OAuth2`, možnost používat `mock` server https://github.com/navikt/mock-oauth2-server nebo https://github.com/patientsknowbest/fake-oauth2-server (#56665).
- [ ] Autorizace prostřednictvím `SAML` - integrovat knihovnu [Spring SAML](https://spring.io/projects/spring-security-saml) pro možnost ověření proti `ADFS/SAML` serveru.
- [ ] Rezervace - nová aplikace pro celodenní rezervace (#57389).
- [ ] Aplikace - převedení dialogu nastavení aplikace v editoru webové stránky ze starého JSP na datovou tabulku (#57409).
- [ ] Hromadný e-mail - optimalizace vytváření seznamu příjemců (#57537).
- [ ] +Úlohy na pozadí - možnost ručního spuštění úlohy na `node` který má nastavenou úlohu, nyní se spustí na `node` kde je uživatel přihlášen.
- [ ] +Formuláře - zakázat `GET` volání na `FormMail/FormMailAjax`.
- [ ] +Ecommerce - přidat do e-mailu `JSON-LD` Data https://schema.seznam.cz/objednavky/dokumentace/.
- [ ] +Dial, Blog, Novinky - upravit tak, aby výběr typu číselníku nebo složky pro novinky byl vlevo, podobně jako u galerie/webových stránek. Nemusí to pak být záložky, ale všechny se zobrazí najednou.
- [ ] +Překladové klíče - zobrazí stromovou strukturu překladových klíčů pro lepší orientaci.
- [ ] +Přidání podpory pro přihlášení do administrace prostřednictvím [PassKeys](https://passkeys.dev/docs/tools-libraries/libraries/)

## 2024

- [x] Přechod na Javu 17 - WebJET od verze 2024.0 bude vyžadovat Javu verze 17 (#54425)
- [x] Nahrazení sady ikon ze souboru `FontAwesome` pro sad `Tabler Icons` (#56397).
- [x] Publikování souborů JAR do `Maven Central` (#43144).
- [x] Nová verze aplikace Nákupní košík/`eshop`, ukázková verze webových stránek, integrace API do online fakturačních systémů, aktualizované integrace do platebních bran (#56329,56385,56325).
- [x] Překladové klíče - možnost importovat pouze neexistující klíče (#56061).
- [ ] ~~`quill` - přidat možnost nastavení položek menu včetně barev.~~~
- [ ] ~~Aplikace - možnost zakoupit aplikaci pro verzi OpenSource (#55825).~~
- [x] Webové stránky - pokud je povolena konfigurační proměnná `syncGroupAndWebpageTitle` a jedna stránka je nastavena jako hlavní stránka pro více složek, zakažte přejmenování názvu složky podle hlavní stránky. Navíc pokud je hlavní stránka v jiné složce, také nepřejmenovávejte (#56477).
- [x] Audit - opakování změněných stránek a čekání na publikování do datových tabulek (#56165).
- [x] Blog - správa `bloggerov` Přestavba na datové tabulky (#56169).
- [x] Blog - komentáře - zařadit do sekce Diskuze, přidat `bloggerom` práva také v sekci Diskuze (#56173).
- [x] AB testování - přepracování na DT, použití možností z `news` aplikace, proveďte nastavení konfiguračních proměnných podle předpony (#56177).
- [x] Kalendář akcí - neschválené a doporučené akce - předělat na DT, použít kód pro seznam akcí (#56181).
- [x] Editor - při vytváření odkazu na e-mail se do okna automaticky přidá http, ačkoli se odkaz nakonec vloží správně s pomocí `mailto:` předpona (#56189).
- [x] Dotazníky - přebudování aplikace na datové tabulky (#55949).
- [x] +PostgreSQL - přidání podpory databáze (#56305).
- [x] Formulář easy - přidat možnosti jako u standardních formulářů (zejména nastavení přesměrování po odeslání), možnost odeslání formuláře uživateli (pro kontrolu) (#56481).
- [x]+Form easy - přidání podpory pro wysiwyg pole (#56481).
- [x] +Odstranění nepoužívaných a nahrazení málo používaných knihoven (#56265).
- [x] +MultiWeb - ověření funkčnosti, přidání potřebných práv (#56421, #56405, #56425).
- [x] +Hromadný e-mail - přidání podpory odhlášení jedním kliknutím (#56409).
- [ ] ~~+Možnost spustit kód Thymeleaf v záhlaví/zápatí a možná také v těle webové stránky.~~
- [x] +Nahraďte databázový fond pomocí `HikariCP` (#56821).
- [x] +Úrovně záznamů - převod na datovou tabulku (#56833).
- [x] +DBPool - přechod z `Apache DBCP` na adrese `HikariCP` (#56821).
- [x] Dokumentace - český překlad (#56237,#56249,#56773).
- [x] +Dokumentace - český překlad (#57033).
- [x] +A/B testování - zakázat pro prohlížeče/boty, uvádět verzi pro objekt Ninja (#56677).
- [x] +Datové tabulky - možnost přeskočit řádek při importu, pokud je nesprávný (#56465).
- [x] +Přesměrování - optimalizace získávání přesměrování bez nutnosti získávat kód přesměrování z databáze podruhé (#53469).
- [ ] ~~+Zabezpečení - přidat podporu generování `nonce` Pro `Content-Security-Policy` záhlaví, viz např. https://medium.com/@ooutofmind/enhancing-web-security-implementing-csp-nonce-mechanism-with-spring-cloud-gateway-a5f206d69aee.~~.
- [x] +Galerie - přidání možnosti změny velikosti obrázku.
- [ ] ~~+Forms - přidání možnosti volat třídu Java pro validaci formulářů.~~
- [x] +Indexování souborů - přidání data poslední změny souboru (#57177) do Perex-Start of publishing.
- [x] +Překladové klíče - do služby REST `/rest/properties/` přidat možnost filtrování klíčů podle konfigurační proměnné, aby nebylo možné veřejně načíst všechny klíče z WebJET CMS (#57202).
- [x] +Webové stránky - audit úspěšného publikování webové stránky, možnost zaslání oznámení autorovi webové stránky (#57173).
- [x] +Zobrazení informací o vypršení platnosti licence 2 měsíce před jejím vypršením na úvodní obrazovce (#57169).
- [ ] ~~+Tags - filtr podle aktuální domény, aby byl stejný jako v ostatních sekcích.~~
- [x] +Audit - přidat hlavičku HTTP `Referrer` k identifikaci původní stránky, ze které bylo volání provedeno (#57565).
- [ ] ~~+Import uživatelů - pokud není zadáno heslo, vygeneruje se (pro nové uživatele), pokud není odeslán žádný stav `available` nastavit na `true`.~~
- [ ] ~~+V testech nějak automaticky kontrolujte výskyt parametru `I\.waitForText\('.*?', '.*?'\);` a `I\.waitForText\(".*?", ".*?"\);` což jsou nesprávná čekání bez definovaného času, způsobí pád automatických testů.~~~
- [ ] ~~+Přidat aplikaci, která přesměruje hlavní stránku na `/sk/` nebo `/en/` podle jazyka prohlížeče.~~~
- [ ] ~~+Změnit odstranění konfigurace tak, aby se po jejím odstranění nastavila na původní hodnotu definovanou v položce `Constants`.~~
- [x] +Galerie - přidání pole URL pro zdroj obrázku s možností zadání adresy, ze které jsme obrázek získali, automaticky nastavené při použití fotobanky.
- [ ] ~~+Galerie - při duplikování obrázku povolte změnu "Složka", abyste mohli duplikovat obrázky do jiné složky, než je aktuální složka.~~
- [ ] ~~+Hromadný e-mail - audit změn v uživatelských skupinách.~~
- [ ] ~~+Archivní soubory - přebudování na datové tabulky (#57317).~~
- [ ] ~~+Volitelná pole - přidejte možnost výběru více položek pro propojení s číselníkem.~~
- [x] +Vyhledávání v administraci - přepracování do datové tabulky (#57333).
- [x] +Anotace - doplnění původní funkce zobrazení statistik (#57337).
- [x] +Tags - přidání možnosti definovat název ve více jazycích, přidání volitelných polí (#57273,#57449)
- [x] +Aplikace - převod dialogu nastavení aplikace v editoru webové stránky ze starého JSP na datovou tabulku (#57157,#57161,#57409).
- [x] +Webové stránky, galerie - přidání možnosti vyhledávání ve stromové struktuře (#57265,#57437)

## 2023.52 / 2024.0

> Verze 2023.52/2024.0 je zaměřena na zlepšení zabezpečení, přechod od knihovny `Struts` na adrese `Spring`.

- [x] Aktualizace - redesign, přidání možnosti aktualizovat pomocí jarních balíčků (#55797).
- [x] Klonovací struktura - předělat na zrcadlovou strukturu s možností překladu (#55733).
- [x] Hodnocení - Přestavba na jaro (#55729).
- [x] Fórum - předělání na jaro (#55649).
- [x] Uživatelé - přidání skupiny práv sloupce do datové tabulky (#55601).
- [ ] Galerie - nový typ pro zobrazení malého počtu fotografií s rozbalením, možnost přepínat mezi všemi fotografiemi v článku (#55349).
- [ ] ~~~Přidání možnosti autorizace prostřednictvím `OAuth2`, možnost používat `mock` server https://github.com/navikt/mock-oauth2-server nebo https://github.com/patientsknowbest/fake-oauth2-server (#56665).~~~
- [ ] ~~Autorizace prostřednictvím `SAML` - integrovat knihovnu [Spring SAML](https://spring.io/projects/spring-security-saml) pro možnost ověření proti `ADFS/SAML` server.~~
- [x] Webové stránky - přidání možnosti obnovit smazanou složku tak, aby byl atribut správně nastaven pro webové stránky `available` (#55937).
- [ ] Průzkumník - úprava do nového designu, aktualizace JS kódu elfinderu (#55849).
- [x] Webové stránky - pamatujte si poslední adresář při vytváření odkazů v dialogu a použijte jej přímo při přidávání dalšího odkazu (#54953-29).
- [x] Kalendář událostí - pokud je událost zadána v říjnu na listopad, kdy je čas posunut, zobrazí se s časem posunutým o jednu hodinu (#56857).
- [ ] upravit zasílání zpráv mezi správci z vyskakovacího okna na lepší uživatelské rozhraní.
- [x] Aplikace - vylepšete popis jednotlivých aplikací, aktualizujte obrázky aplikací (#55293).
- [x] Fotogalerie - optimalizace množství načítaných dat (#56093).
- [x] Fotogalerie - editor obrázků se inicializuje při každém otevření okna, upravit pro použití existujícího editoru (#55853).
- [x] Datové tabulky - přidání testování duplicit (#56849) do automatického testu.
- [ ] Banner - přidejte možnost použít šablony pro zobrazení banneru, např. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.
- [ ] Podívejte se, jak fungují SEO rozšíření pro WordPress (např. https://yoast.com/beginners-guide-yoast-seo/) a navrhněte podobné řešení pro WebJET CMS. Integrovat do editoru něco jako https://github.com/thinhlam/seo-defect-checker (#56853).
- [ ] Přidejte blokům v PageBuilderu nějaký datový atribut s názvem bloku a poté jej zobrazte ve vlastnostech bloku, aby bylo možné identifikovat, o který blok se jedná.
- [ ] Na úvodní obrazovce implementujte aplikaci todo mini.
- [ ] DT - po přidání záznamu přejděte na stránku, kde se záznam nachází (obvykle poslední stránka).
- [x] Editor - přidání možnosti zobrazit editor HTML pro vybrané webové stránky, které nebude možné přepnout do režimu WYSIWYG. Používá se na speciálních stránkách integrujících různé aplikace JavaScript nebo speciální komponenty. Vhodný editor je [Kód esa](https://www.npmjs.com/package/ace-code) (#56129).
- [x] Fulltext - složka `files` na indexových stránkách přesunout na kartu Systém (54953-30).
- [x] +Uživatelé - zabránit samovolnému mazání (#55917).
- [x] +News - přidat kontrolu práv ke složkám a zobrazovat pouze složky, ke kterým má uživatel práva (#56661).
- [x] +Aplikace - možnost podmíněného zobrazení aplikací podle typu zařízení, implementováno obecně, konfigurovatelné přes uživatelské rozhraní pro Banner (#55921).
- [x] +Statistiky - omezení zobrazených statistik na složky, ke kterým má uživatel právo (#55941).
- [x] +Nabídka Obnovit - přebudování aplikace na datové tabulky (#55945).
- [x] +Přesměrování - oddělené přesměrování podle vybrané domény (#55957).
- [x] +Skupiny perexu - oddělená správa skupin perexu podle uživatelských práv na webu (#55961).
- [x] +Banner - podmíněné zobrazení podle mobilního/tabletového/desktopového zařízení (#55997).
- [x] +Sestavitel stránek - generování kotev/horního menu na základě vložených sekcí (#56017).
- [x] +Volitelná pole - přidat typ pole `UUID` pro generování identifikátorů (#55953).
- [x] +Import webových stránek - přestavba na Spring (#55905).
- [x] +Monitorování serveru - CPU - `oshi-core` nefunguje ve Windows 11 (#55865).
- [x] +Headers - možnost definovat příponu souboru, pro který bude nastavena hlavička (#56109).

## 2023.40

> Verze 2023.40 upravuje uživatelské rozhraní na základě nových návrhů UX, zlepšuje použitelnost datových tabulek.

- [x] Migrace na vyšší verzi Javy s `LTS` (Java 11 nebo ideálně až Java 17) (#54425).
- [x] Banner - přidání možnosti nastavit adresářovou strukturu stránek, na kterých se bude banner zobrazovat, a možnosti definovat výjimky, kdy se zobrazovat nebude (#55285).
- [x] Hlavičky - přidání nové aplikace, která umožňuje definovat libovolnou hlavičku HTTP podle URL (#55165).
- [ ] ~~(přesunout do 2024.0)upravit zasílání zpráv mezi administrátory z vyskakovacího okna na lepší uživatelské rozhraní~~
- [ ] ~~(přesunout do 2024.0)Aplikace - zlepšit popis jednotlivých aplikací, aktualizovat obrázky aplikací (#55293).~~
- [ ] ~~(přesun do 2024.0)Fotogalerie - optimalizace množství načítaných dat~~
- [ ] ~~(přesunout do 2024.0)Fotogalerie - editor obrázků se inicializuje při každém otevření okna, upravit pro použití stávajícího editoru (#55853)~~
- [ ] ~~(přesunout do 2024.0)Datové tabulky - přidat Duplicitní testování do automatizovaného testu.~~
- [ ] ~~~(přesunout do 2024.0)Banner - přidat možnost použití šablon pro zobrazení banneru, např. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.~~
- [ ] ~~~(přesun do 2024.0)Zkontrolujte, jak fungují SEO rozšíření pro WordPress (např. https://yoast.com/beginners-guide-yoast-seo/) a navrhněte podobné řešení pro WebJET CMS. Integrujte do editoru něco jako https://github.com/thinhlam/seo-defect-checker.~~
- [ ] ~~(přesunout do 2024.0)Přidat do bloků v PageBuilderu nějaký datový atribut s názvem bloku a pak ho zobrazit ve vlastnostech bloku, aby bylo možné identifikovat, o který blok se jedná.~~
- [x] Slovníky - v exportu není příznak, že je záznam smazán a po importu je záznam aktivován (#55541).
- [ ] ~~(přesun na 2024.0)Na úvodní obrazovce implementujte aplikaci todo mini.~~
- [ ] ~~(přesun na 2024.0)DT - po přidání záznamu přejděte na stránku, kde se záznam nachází (obvykle poslední stránka)~~
- [ ] ~~~(přesun do verze 2024.0)Editor - přidání možnosti zobrazit editor HTML pro vybrané webové stránky, které nebude možné přepnout do režimu WYSIWYG. Používá se na speciálních stránkách integrujících různé aplikace JavaScriptu nebo speciální komponenty. Vhodný editor je [Kód esa](https://www.npmjs.com/package/ace-code).~~
- [x] Zveřejnění OpenSource/komunitní verze WebJET CMS na webu `github.com` (#55789,#55773,#55829,#55749).
- [x] +Databáze - na webových stránkách/galerii se při nastavení jiného poměru sloupců špatně zobrazují tlačítka (#54953-22).
- [x] +Překlady - přidejte do zrcadlení struktury možnost překládat také text stránky prostřednictvím [DeppL](https://www.deepl.com/docs-api/html/) (#55709).
- [ ] ~~~(přesun do 2024.0)+Autorizace prostřednictvím `SAML` - integrovat knihovnu [Spring SAML](https://spring.io/projects/spring-security-saml) pro možnost ověření proti `ADFS/SAML` server.~~
- [ ] ~~(přesun do 2024.0)+Přechod z frameworku Struts na Spring - postupné přepsání kódu všech tříd na Spring controllery (#55389,#55469,#55489,#55493,#55469,#55501,#55609,#55613,#55617,#55701).~~
- [x] +Přepracujte administraci seznamu novinek do nového designu a datové tabulky s využitím co nejvíce kódu ze stávajícího seznamu webových stránek (#55441).
- [x] +Aktualizace knihovny `amcharts` pro grafy v novější verzi 5 (#55405).
- [x] +Přepracování aplikace Zprávy (#55441).
- [x] +Přidání podpory pro vývoj pomocí DevContainers (#55345).
- [x] +Monitorování serveru - přebudování sekcí Aplikace, Webové stránky a SQL dotazy na nové datové tabulky (#55497).
- [x] +Webové stránky - schválení - přestavba z `Struts` na adrese `Spring`, pro doplnění testů (#55493).
- [x] +SEO - rebuild na úložištích a datových tabulkách (#55537).
- [x] +Outorizace uživatele - v administraci chybí původní volba z v8 (#55545).
- [x] +DT - vyhledávání záznamů podle ID, lepší řešení pro vyhledání webové stránky podle docid, když je v seznamu mnoho stránek (#55581).
- [x] +Diskuse - opakovaná správa datových tabulek (#55501).
- [x] +Monitorování serveru - přepracování monitorování dotazů Aplikace/Webové stránky/SQL na datové tabulky/nové grafy (#55497).
- [x] +Proxy - přidání podpory pro volání služeb REST bez vložení výstupu do webové stránky (#55689).
- [x] +Banner - přidání podpory pro video soubory včetně banneru s obsahem (#55817).
- [x] +Klíče pro překlad - možnost importovat pouze jeden jazyk bez ovlivnění překladů v ostatních jazycích (importovat pouze zadaný sloupec z Excelu) (#55697).
- [x] +Média - přidání podpory pro volitelná pole (#55685).

## 2023.18

> Verze 2023.18 mění požadavek na spuštění řešení na verzi Java 17. Jedná se o zásadní změnu, která vyžaduje i změnu serverů. Z tohoto důvodu je implementována hned na začátku roku.

- [ ] ~~~(přesun na 2023.40)Migrace na vyšší verzi Javy s `LTS` podpora (Java 11 nebo ideálně až Java 17) (#54425)~~~.
- [x] Integrace kódu verze 8 do projektu, odstranění duplicitních stránek, odstranění možnosti přepnutí na verzi 8, zachování pouze nových verzí stránek (#54993).
- [x] Aktualizace editoru CK na novější verzi (#55093).
- [x] Formuláře - přidání funkce archivního formuláře (#54993).
- [ ] ~~(přesunout do 2023.40)Banner - přidat možnost nastavení adresářové struktury stránek, na kterých se bude zobrazovat banner, a možnost definovat výjimky, na kterých se zobrazovat nebude.~~
- [x] Vyhledávání - `Lucene` - přidat indexování podle jazyka stránky, aby byly do indexu zahrnuty pouze stránky v zadaném jazyce šablony (#54273-34).
- [ ] ~~(přesunout do 2023.40)Hlavičky - přidat novou aplikaci, která umožňuje definovat libovolnou hlavičku HTTP podle URL (#55165).~~
- [x] Atributy stránky - přidána možnost nastavení atributů stránky v editoru (#55145).
- [x] Šablony - přidání možnosti sloučení šablon (#55021).
- [ ] +Audit - přidá anotaci k primárnímu klíči, pokud se liší od ID, konkrétně pro editaci překladových klíčů se hodnota neaudituje. `key` Ale `id` což je irelevantní.
- [x] +Galerie - Byla použita multidoménová struktura `/images/DOMENA/gallery` která nyní není v galerii viditelná. Je třeba ji tam nějak přidat (#54953-4).
- [x] +Překlady - možnost dynamicky definovat podporované jazyky v šablonách, překladových klíčích atd. (#MR332).
- [x] +Zabezpečení - protokoly serveru jsou dostupné v rámci oprávnění k auditu, je třeba je oddělit do samostatného oprávnění (#54953-5).
- [x] +Zlepšení podpory serveru Oracle DB, testování funkčnosti entit (#54953-6).
- [x] +Webové stránky - při duplikování webové stránky by se měla duplikovat i připojená média (#54953-6).
- [x] +Webové stránky/galerie - přidání možnosti nastavení poměru stran pro zobrazení stromové struktury a tabulky (#54953-7).
- [x] +Klíče pro překlad - změna editace na tabulkové zobrazení, kde sloupce obsahují jednotlivé jazyky pro možnost editace překladů ve všech jazycích najednou (#55001).
- [x] +Banner - přepracování statistik zobrazení banneru do nového designu (#54989).
- [x] +Klíče pro překlad - přidat kontrolu počtu jazyků překladu, aby nespadl na `fieldK` (#MR344).
- [x] +Digitizer - přepracování pro odstranění závislosti na staré verzi datatables.net (#55009).
- [ ] +Oprava chyby v editoru, např. v aplikaci galerie - při posouvání okna uchopením titulkového pruhu Aplikace se špatně posouvá a pak stále sleduje kurzor. Pak je třeba kliknout dolů tlačítkem OK, aby se rolování okna vypnulo.
- [x] +Proxy - redesign (#55025).

## 2022.52 / 2023.0

> Verze 2022.52 je dlouhodobým seznamem úkolů, na které v podrobnějším plánu na rok 2022 nezbyl čas.

- [ ] ~~~~zatím zrušeno: automatické nahrávání videí pomocí WebJET CMS - https://dev.to/yannbertrand/automated-screen-recording-with-javascript-18he nebo https://www.macrorecorder.com~~
- [x] přidání testovacích statistik prostřednictvím modulu `Allure` - https://codecept.io/plugins/#allure (#54437).
- [x] přidat `code coverage` hlášení, např. prostřednictvím JaCoCo - https://docs.gradle.org/current/userguide/jacoco\_plugin.html (#54909).
- [ ] ~~(přesunout do 2023.40) upravit zasílání zpráv mezi administrátory z vyskakovacího okna na lepší uživatelské rozhraní~~
- [ ] ~~(přesunout do 2023.40) Aplikace - zlepšit popis jednotlivých aplikací, aktualizovat obrázky aplikací.~~
- [x] Galerie - přidání změny fyzického názvu souboru (včetně generovaných obrázků s\* a o\*) po změně atributu Název souboru v editoru (#39751-52).
- [x] +Databáze - pamatujte si počet záznamů na stránku pro každou datovou tabulku (#39751-50).
- [ ] +`Command Palette` - přidání palety příkazů s integrovaným vyhledáváním podobně jako ve VS Code - https://trevorsullivan.net/2019/09/18/frictionless-user-experiences-add-a-command-palette-to-your-react-application/
- [ ] ~~(přesun na 2023.40) +fotogalerie - optimalizace množství načítaných dat~~
- [ ] ~~(přesun do 2023.40) +fotogalerie - editor obrázků se inicializuje při každém otevření okna, upravit pro použití existujícího editoru~~
- [ ] ~~(přesun do 2023.40) +Datové tabulky - přidat testování funkce Duplicate do automatizovaného testu.~~
- [ ] ~~~(přesun do 2023.40) +Banner - přidání možnosti použít šablony pro zobrazení banneru, např. https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template\_literals.~~
- [ ] ~~(přesunout do 2023.40) +Ověřit, jak fungují SEO rozšíření pro WordPress (např. https://yoast.com/beginners-guide-yoast-seo/) a navrhnout podobné řešení pro WebJET CMS.~~
- [ ] ~~(přesunout do 2023.40) +Přidat blokům v PageBuilderu nějaký datový atribut s názvem bloku a pak ho zobrazit ve vlastnostech bloku, aby bylo možné identifikovat, o který blok se jedná.~~
- [x] Zvažte použití [HotSwapAgent](http://hotswapagent.org/) pro rychlejší vývoj bez nutnosti restartů (řešeno přestavbou `build.gradle` pro standardní podporu HotSwap).
- [ ] ~~(přesun do 2023.40)+Číselníky - v exportu není žádný atribut, že je záznam smazán a po importu je záznam aktivován.~~
- [ ] ~~(přesun na 2023.40)Na úvodní obrazovce implementujte aplikaci todo mini.~~
- [ ] ~~(přesunout na 2023.40)+DT - po přidání záznamu stránkovat na stránku, kde se záznam nachází (obvykle poslední stránka)~~
- [x] +Kompletní překlady textů ze souborů pug (např. gallery.pug), hledání např. písmene á nebo podobných, které se mohou vyskytovat v nepřeložených textech.
- [x]+Přidat možnost ponechat okno editoru otevřené i po uložení (viz https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open, closeOnComplete, https://editor.datatables.net/reference/api/submit()). Implementováno pomocí klávesové zkratky `CTRL+s/CMD+s` (#54273-26).
- [x] `Combine` / smazat data - při mazání všech dat v mezipaměti se správně přenese i do `Cluster` a tam také smazat, skutečné `ClusterRefresher` odstraní pouze mezipaměť, nikoli `DocDB/GroupsDB` a nenastavuje nový čas pro `combime` (#54673).
- [x] +REST - přidání možnosti definovat API klíče (tokeny), které se používají místo generovaných CSRF tokenů pro REST volání externích služeb (#54941).
- [ ] ~~(přesun do 2023.18) +Headers - přidání nové aplikace, která umožňuje definovat libovolnou hlavičku HTTP podle URL.~~
- [x] +Skripty - přidat `autocomplete` o umístění skriptu v poli šablony (#54941).
- [x] +Kontrola odkazů - upravte původní verzi kontroly odkazů na datovou a odkazujte na seznam webových stránek (#54697).
- [x] +Objekty rezervací a rezervací - opakovaná správa datové tabulky a jara (#54701).
- [x] +Anneta - přepracování správy datatable a Spring (#54705).
- [x] +Page Attributes - redo administration on datatable and Spring (#54709).
- [x] +Šablony - přidejte záložku se seznamem webových stránek, které používají šablonu (#54693).
- [x] +Statistika - přepracování na nový design a grafy (#54497, #54585, #54897).
- [x] +Hledat - `Lucene` - přidání podpory vyhledávání v češtině a angličtině (#54273-34).
- [x] +Vyřešení problému s verzemi FontAwesome v administraci a na webových stránkách (#39751-51).
- [x] +Bannerový systém - přidejte možnost zobrazení banneru kampaně podle parametru URL (#MR296).
- [x] +Galerie - rozšíření možností editoru, přidání horního/poddílného indexu, možnost vložení kódu HTML, nastavení barev. Zlepšení podpory starého editoru/html kódu (#54857-4).
- [x] +Galerie - přizpůsobte počet fotografií velikosti okna, pamatujte na nastavenou velikost obrázků v tabulce (#54857-4, #market-245).
- [x] +Webové stránky - zobrazování skupin perexů jako pole pro více výběrů s vyhledáváním místo standardního pole `checkbox` polí (pokud existuje mnoho skupin perexů) (#market-245).
- [x] +Form easy - přidat typ pole `select` - standardní výběrové pole, přidejte možnost zadat jinou hodnotu a zobrazit text (#MR298, #39751-45).
- [x] +Webové stránky - přidání zobrazení složek jako datové tabulky s hromadnými operacemi (#54797).
- [x] +Zabezpečení - přidání možnosti nastavení hlavičky HTTP `Access-Control-Allow-Origin` (#39751-44).
- [x] +Systém bannerů - přidání možnosti nastavit klienta, ovládání práv zobrazení (#38751-52).
- [x] +Systém bannerů - přidat možnost nastavení volitelných polí, refaktorizace `BannerEntity` a `BannerBean` do jednoho objektu, aby bylo možné nová pole použít v komponentě JSP (#39751-52).

## 2022.40

> Verze 2022.40 je zaměřena na zlepšení zabezpečení.

- [x] Uživatelé - přidat kontrolu historie hesel při ukládání uživatele, protože nyní to umožňuje, ale nelze se s ním přihlásit (#54293).
- [x] Skupiny uživatelů - přidání karty se seznamem uživatelů ve skupině práv (#54493).
- [x] Aktualizace modulů NPM - https://www.npmjs.com/package/npm-check-updates nebo https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version (#54721).
- [x] Změna `hash` funkce hesel na `bcrypt` (nebo něco modernějšího - https://cheatsheetseries.owasp.org/cheatsheets/Password\_Storage\_Cheat\_Sheet.html) (#54573).
- [x] Přidat/ověřit kontrolu editačních práv pro konfigurační proměnné (funkce editovat všechny proměnné) (#54749).
- [x] Validace - přidejte lepší validace polí (např. url adresa, doména) s popisnými chybovými zprávami a upravte je. `DatatableExceptionHandlerV2` možnost používat překladové klíče přímo z WebJET CMS s dalšími údaji (jako je min/max hodnota a další makra v překladovém klíči) (#54597).
- [x] Logovaní - přechod z `log4j` na standardní `slf4j` - vyřešeno přechodem na `logback` ().
- [x] Logovaníí - přidání systému pro dočasný zápis protokolů do paměti RAM pro možnost zobrazení nejnovějších protokolů prostřednictvím administrace, pokud protokoly nejsou dostupné v souborovém systému. Možnost získat také logy z uzlů clusteru, např. prostřednictvím dočasného zápisu do dat. `conf` Tabulky (#54269).
- [x] Přidání kontroly aktualizací a databáze jako ve WebJETu 8 + ~~otevření nápovědy k nové verzi~~ (v úvodu je odkaz na seznam změn) (#54457) na domovskou stránku.
- [x] Upravte vizuální podobu nekompatibilního hesla a změny hesla na WebJET 9 (alespoň vyměňte obrázek na pozadí) (#54573).
- [x] Zabezpečení - přidat řízení přístupu k administraci - v současné době se kontrolují pouze volání služby REST, nikoliv volání `Thymeleaf` stránky (#54649)
- [x] Datatable - přidání názvu položky do dialogu pro odstranění, je třeba myslet na situaci, kdy je vybráno více řádků (#54753).
- [ ] ~~~(přesun na 2023.18) +Migrace na vyšší verzi Javy s `LTS` podpora (Java 11 nebo ideálně až Java 17) (#54425).~~~
- [ ] ~~(přesun do 2023.18) +Forms - přidat funkci archivního formuláře.~~
- [x] +Webové stránky - přidání funkce náhledu před uložením (#54557).
- [x] +Webové stránky - přidat možnost `drag&drop` stránek do adresářové struktury (#54513).
- [x] +Připravte pro programátora v administraci ukázkovou aplikaci, kde bude možné vybrat některé možnosti ve formuláři a nahrát soubor, který bude serverem zpracován. Poté se zobrazí výsledek (#54449).
- [x] +Připravit vzorovou šablonu pomocí Thymeleaf a případně PUG souborů s integrací do PageBuilderu (#54345).
- [x] +Přepracujte aplikaci Hromadné e-maily na nový design (#54377).
- [x] +Dodání CZ a EN překladů (#53805).
- [x] +Přidat kartu pro úpravu skupiny uživatelů se seznamem uživatelů v této skupině, upravit seznam webových stránek pro stránkování serveru (#54993).
- [ ] ~~(přesunout do 2023.18) +Banner - přidat možnost nastavení adresářové struktury stránek, na kterých se bude zobrazovat banner, a možnost definovat výjimky, na kterých se zobrazovat nebude.~~
- [x] +Přidat zapamatování metody řazení pro každou datovou tabulku (#54513-22).~~~
- [x] +Přidat X pro zavření okna a ikonu maximalizace.
- [ ] ~~(přesunout do 2022.52) +Šablony - přidat záložku se seznamem webových stránek používajících šablonu (#54693).~~
- [x] +Webové stránky - pomocí klávesy CTRL lze vybrat více složek, ale nelze takto odstranit všechny složky (odstraní se pouze první) (#54797).
- [x] +Databáze - dynamicky určovat počet sloupců v tabulce tak, aby v ní nebylo prázdné místo, což evokuje, že už nejsou žádné další záznamy (uživatel si nevšimne, že je stránkování) (#54273-26).
- [x] +Databáze - přidání možnosti použít multiselect v datové tabulce a v aplikaci v editoru (#54273-25).
- [x] +Kalendář akcí - přepracování aplikace (#54473).
- [x] +Webové stránky - přidání možnosti zobrazit ID složky a pořadí uspořádání ve stromové struktuře stránky (#54513-1).
- [x] +Webové stránky - přidání možnosti uložit stránku jako A/B test (#54513-2).
- [x] +Vzorce - přidat možnost exportu od posledního exportu, vybrané záznamy podle aktuálního filtru (#54513-3)
- [x] +Generovat `POM` se závislostmi/knihovnami přímo z verze gradle, místo abyste upravovali původní verzi WJ8 (#54673).
- [ ] ~~~(přesunout do 2022.52) +Skripty - přidat `autocomplete` v poli Umístění skriptu v šabloně.~~~
- [ ] ~~(přesunout do 2022.52) +Kontrola odkazů - upravit původní verzi kontroly odkazů na datovou a odkazovat na seznam webových stránek (#54697).~~
- [ ] ~~(přesunout do 2022.52) +Objekty rezervací a rezervací - přepracování správy datové tabulky a jara (#54701).~~
- [ ] ~~(přesunout do 2022.52) +Anketa - přepracovat správu datatable a Spring (#54705).~~
- [ ] ~~(přesunout do 2022.52) +Stránka s atributy - přepracování administrace datatable a Spring (#54709).~~
- [x] +Rolování okna editoru (#54513-21).
- [x] +Zapamatujte si pořadí sloupců v tabulce pro každou tabulku (#54513-22)
- [x] +Pamatovat si naposledy otevřenou složku v seznamu webových stránek (#39751-45)

## 2022.18

> Verze 2022.18 se zaměřuje na vylepšení použitelnosti.

- [x] editor - zobrazení stránky v řádkovém editoru (pokud je řádkový editor nastaven na Page Builder) (#54349).
- [x] DT v klientském stránkování označuje všechny řádky, předstírá označení na jedné stránce, ale na ostatních stránkách vše smaže (#54281).
- [x] Přidání tlačítka pro upozornění, že je k dispozici rozpracovaná verze k otevření (#54357).
- [x] Přidání sloupce s ikonami pro zobrazení webové stránky (#54257).
- [x] Přepracujte DocID/web page na ID na webových stránkách (#53805).
- [x] Domů - přidání aplikace do pravého sloupce se seznamem oblíbených adres URL (#54177)
- [x] Úvod - přidání jednoduchého formuláře pro zasílání komentářů WJ (#54181)
- [x] Úvodní stránka - ikony v prvním bloku (návštěvy, formuláře...) jsou klikací (#53805)
- [x] Přidání možnosti nastavení dvoufaktorové autorizace správcem (#54429)
- [x] Pro mapu komponent WJ Spring `editor_component`. V současné době hledá jsp v `INCLUDE` a pro tyto komponenty existuje např. `sk.iway.xxx.component.SiteBrowserComponent` a tak ho nenajde a vrátí se. `appstore` (#54333).
- [x] Webové stránky - přidání možnosti změny domény po zadání `docid` pokud je stránka v jiné doméně, a automaticky přepnout záložku do systému nebo koše, pokud je v této větvi (#54397).
- [x] Překladové klíče - přidat tlačítko pro obnovení klíčů z `properties` obnovit klíče po smazání záznamu a nezobrazovat chybu smazání, pokud klíč neexistuje (protože je ze souboru vlastností - id větší než 1000000) (#54401).
- [x] Editor DT - přidání upozornění při opuštění stránky (#54413).
- [x] Volitelná pole - přidejte možnost výběru adresáře (#54433).
- [ ] ~~+(přesun na 2022.52) DT - po přidání záznamu přejděte na stránku, kde se záznam nachází (obvykle poslední stránka)~~
- [x] +Vyladění verze pro Firefox, přidání automatického testování i pro Firefox (#54437).
- [x] +Po přechodu na verzi WJ8 chybí odkazy pro přidání přesměrování (a možná i další) (#53805).
- [x] +Dodání ikony do rozhraní WJ8 pro přepnutí na verzi V9 (#53805).
- [ ] ~~+(přesunout do 2022.52) Přidat překlady textů ze souborů pug (např. gallery.pug), hledat např. písmeno á nebo podobné, které by se mohlo nacházet v nepřeložených textech.~~
- [x] +Při importu konfigurace s aktualizací podle názvu dojde k chybě, bylo by vhodné ji nahradit původním importem s náhledem změn.
- [ ] ~~+ (přesunout do 2022.36) Připravte pro programátora vzorovou aplikaci pro použití v administraci, kde bude možné vybrat některé možnosti ve formuláři a nahrát soubor, který bude zpracován serverem. Poté se zobrazí výsledek (#54449). ~~~
- [x] +Aktualizujte knihovny Java na aktuální verzi bez zranitelností.
- [x] +Přidat možnost použití `Thymeleaf` šablony pro `@WebjetComponent` (#54273).
- [x] +Založit instanci s veřejným git repozitářem (#54285).
- [ ] ~~+(přesunout do 2022.52) Přidat možnost ponechat okno editoru otevřené i po uložení (viz https://datatables.net/forums/discussion/32727/how-to-keep-editing-window-open).~~
- [x] +V nabídce sekce šablony zobrazit položky pro úpravu šablony přímo na první úrovni (#54345)
- [x] +Přidat možnost zadání `placeholder` v aplikaci Form Easy (#54381).
- [x] +Ověření běhu WebJET CMS nad Oracle a Microsoft SQL (#54457).

## 2021.52

> Verze 2021.52 je zaměřena na zlepšení práce s webovými stránkami a editorem.

### Webové stránky

- [x] editor - přidány informační ikony pro každé pole s vysvětlením jejich významu (#54253)
- [x] editor - doplnění položek TODO v editaci webové stránky na kartě Přístup - nastavení navigačního panelu a zobrazení mapy webu (rozšíření datového modelu) (#54205)
- [x] editor - upravené pole URL podle původního návrhu uživatelského rozhraní (zobrazuje se pouze koncová část, s možností přepnout zobrazení celé URL) (#54237)
- [x] editor - zobrazení informací o schválení stránky (po uložení, když má být schválena) (#54129)
- [x] editor - zobrazení rozpracované práce (#54129)
- [x] ~~editor - přidat možnost Uložit a Uložit jako~~ - nahrazeno vlastností Duplikovat datové tabulky
- [x] webové stránky - zobrazení webových stránek i z podadresářů (#52435)
- [x] Webové stránky - adresáře - zobrazení pole pro zadání domény pouze pro kořenové složky, automatické nastavení domény při ukládání kořenové složky (#54249)
- [x] Webové stránky - přidání informací o úpravách aktuální webové stránky jiným editorem (#54193)
- [x] Webové stránky - ověření/přidání kontroly oprávnění přidat/upravit/odstranit, zkrácených nabídek a skrytých adresářů v administraci (#54257).
- [ ] ~~stromová struktura - podpora klávesových zkratek pro vytvoření/editaci nové položky ve stromové struktuře~~ - nebude implementováno, po konzultaci UX budou všude použita pouze tlačítka
- [ ] ~~stromová struktura - kontextové menu - přidáno kontextové menu ve stromové struktuře (po zvážení vhodnosti z hlediska UI/UX kontextové menu vs. ikony na panelu nástrojů)~~ - nebude implementováno, po konzultaci UX budou všude použita pouze tlačítka
- [x] +editovat buňku - načtení dat ze serveru před úpravou, abyste získali `editorFields` Údaje. V seznamu uživatelů nelze jinak upravovat např. jméno (protože chybí heslo). Také některé nadpisy (sekce Enter admin) se zobrazují nesprávně (#54109).
- [x] +obecné řešení problému s `domain_id` (#53913)
- [x] +přidání pole ID ke každé datové tabulce, jak se používá pro správu webových stránek (#53913)
- [x] +upravit aplikaci otázek a odpovědí podle návrhu WebJET 2021 (#53913)
- [x] +zlepšení použitelnosti na mobilních zařízeních (#53573, #54133)
- [x] +přidat možnost uložit webovou stránku jako rozpracovanou pomocí zaškrtávacího políčka (#54161)
- [x] +Změnit písmo na `Open Sans` (#53689).
- [x] +Zapojit automatický překlad textů prostřednictvím https://www.deepl.com/translator (např. pro zrcadlení struktury, překladové texty atd.) (MR146).
- [x] +Webové stránky - pokud je stránka ve více než jednom adresáři, není možné upravovat stránku v sekundárním adresáři (vrátí se data původní stránky a data podle docid/ID řádku se nespárují).
- [x] +Úprava designu verze 8 tak, aby se podobal verzi 2021 (minimální chování menu) (#54233).
- [x] +Dodat formát nasazení pro aktualizaci přes Nastavení->WebJET Update (#54225).
- [x] +Úplné odstranění starých konfiguračních proměnných `statDistinctUsers-` a `statSessions-` v režimu `auto` clustra, přidat zobrazení uzlů do Monitoring->Aktuální hodnoty (#54453).

### Aplikace v novém designu

- [x] +Otázky a odpovědi - úprava na nový design (#53913).
- [x] +Systém bannerů - úprava na nový design (#53917).
- [x] +Hromadný e-mail - Omezení domény - úprava na nový design (#54153).
- [x] +Export dat - úprava na nový design (#54149).
- [x] +Tooltip - úprava na nový design (#53909).

### Datové tabulky

- [x] +Přidat možnost zadat ID záznamu v libovolné tabulce, podobně jako je tomu u webových stránek.

### Export-import
- [x] +Export-import - oprava pořadí sloupců vzhledem k záhlaví (viditelné v seznamu uživatelů, ale může být i jinde) (#54097).
- [x] +Export-import - řešení sloupců se stejným názvem, ale různými daty (např. v seznamu uživatelů jsou 2 adresy, což se neprojeví v názvech sloupců v Excelu) (#54097).

### Zabezpečení

- [x] +Aktualizace modulů NPM - https://www.npmjs.com/package/npm-check-updates nebo https://nodejs.dev/learn/update-all-the-nodejs-dependencies-to-their-latest-version.
- [x] +Tabulky - přidání automatického nastavení podmínek sloupců do REST Controlleru `domain_id` (#53913).
- [x] +Změnit způsob kompilace tříd jazyka Java tak, aby se před nasazením vše kompletně přebudovalo najednou. Nemůže tak nastat problém s kompatibilitou upravených tříd.
- [x] +Doplnit bezpečnostní kontrolu v administraci v polích, do kterých lze vložit kód HTML (použijte tlačítko `org.owasp.html.PolicyFactory`) (#53931).

### Další úpravy

- [x] +Verze pro mobilní zařízení - optimalizace rozhraní, datových tabulek a editoru pro použití na mobilních zařízeních.

## 2021.40

> Cílem verze 2021.40 je migrace uživatelských úprav a modulu GDPR do Editoru datových tabulek. Budeme také nadále pracovat na zlepšení použitelnosti práce s webovými stránkami - možnosti Drag & Drop a kontextové nabídky ve stromové struktuře.

### Nové funkce

- [x] uživatelé - převedeno do Editoru datových tabulek (#46873)
- [x] Aplikace GDPR - přepracováno v editoru datových tabulek (#53881, #53905)

### Webové stránky

- [x] editor - volná pole - možnost nastavení typů polí `selectbox`, `autocomplete`, vyberte adresu URL obrázku/stránky (#53757)
- [x] stromová struktura - podpora přetahování pro webové stránky a adresáře (#52396)
- [x] +automaticky vytvořit `System` složku pro doménu a zobrazení stránek z ní, včetně podsložek (#53685)
- [x] +automatické odstranění názvu Nová webová stránka po kliknutí do pole a přenesení do navigačního panelu (#53693)
- [x] +úprava načítání seznamu složek pro skupiny šablon - zobrazení všech složek nejen podle názvu instalace pro snadnější použití šablon (#53693)
- [x] +přidat úpravu skupiny perex (#53701)
- [x] +přidat kontrolu práv k úpravám pro překladové klávesy (funkce upravit všechny klávesy) (#53757)
- [x] +při přejmenování domény změňte také prefix domény konfiguračních proměnných a překladových klíčů (#market-107)
- [x] +pokud stromová struktura obsahuje maximálně 2 adresáře, zobrazí je rozbalené (#53805)
- [x] +při nastavování nadřazeného adresáře automaticky rozbalit kořenový adresář (#53805)
- [x] +při vytváření domény `System` adresáře také automaticky vytvářejí podadresáře pro záhlaví, zápatí a nabídky (#53805)
- [x] +webové stránky - přidání ikony pro zobrazení stránky (#53753)

### Datové tabulky

- [x] +Upravit buňku pro editor obsahující více listů (#53585).

### Obecné

- [x] aplikace galerie - úprava editace položek stromové struktury z komponenty VUE do editoru datových tabulek (jako u webových stránek) (#53561)
- [x] přepsání definice datových tabulek ze souborů PUG do anotací v Javě (#48430)
- [x] +navrhnout a zdokumentovat způsob vytváření zákaznických aplikací v novém designu WebJET 2021 (#54045)

## 2021.13

> Cílem verze 2021.13 je zlepšit použitelnost práce s webovými stránkami a možnost jejich využití v zákaznických projektech. Současně bude dokončen vývoj nových funkcí WebJETu 8 a bude sepsána sekce Co je nového ve verzi 8.8.

### Webové stránky

- [x] editor - zlepšení viditelnosti pole Poznámka editora (#53131)
- [x] editor - zobrazení a správa médií (karta Média) (#52351, #52462)
- [x] editor - zobrazení historie stránek (#53385)
- [x] webové stránky - funkčnost záložky Naposledy upraveno a čeká na vyřízení (#53493)
- [x] webové stránky - import/export - přidání původního importu a exportu stránek v XML zipu na panel nástrojů webových stránek (#53497)
- [x] stromová struktura - někdy se kliknutí na ikonu úprav neprovede (po předchozím zavření dialogu pomocí tlačítka zrušit) (#51487)
- [x] stromová struktura - doplnění položek TODO v úpravě adresáře na kartě Šablona a přístup (#51487)
- [x] stromová struktura - úprava ikon zobrazených ve stromové struktuře na základě připomínek UI/UX (#52396)
- [x] stromová struktura - zobrazení naplánovaných verzí a historie adresářů (#53489)
- [x] stromová struktura - přidána ikona pro obnovení stromové struktury (#52396)
- [x] stromová struktura - přidáno zobrazení `docid/groupid` a pořadí zobrazení (podle nastavení) (#53429, #53565)
- [x] stromová struktura - funkčnost záložek Systém a Košík (#53137)
- [x] +optimalizace rychlosti zobrazování seznamu webových stránek (#53513)

### Editor datových tabulek

- [x] po zavření dialogového okna a otevření jiného zůstávají zobrazeny chybové zprávy (#52639)
- [x] po zavření dialogového okna a jeho opětovném otevření zůstane obsah posunutý dolů/původní místo (#53119)
- [x] přidáno pole typu datatable, které umožňuje vložení vnořeného datatable do editoru (např. pro média, historii stránek atd.) (#52351)
- [x] přidání zobrazení chybové zprávy při chybě tokenu/odhlášení uživatele (#53119)
- [x] +přidání možnosti automatického dokončování pro textová pole (#52999)
- [x] +zobrazení pole poznámky pro editor prostřednictvím oznámení (#53131)
- [x] +inicializace vnořených datových tabulek až po přechodu na daný list (#53425)
- [x] +integrace výběru souborů přes elfinder (#52609)

### Další

- [x] +Konfigurace - zobrazení plánovaných verzí a historie změn (#53089)
- [x] +přidat možnost překládat text v souborech JavaScript (#53128)
- [x] +Čtení dat vnořené datové tabulky pouze po kliknutí na list, kde se nachází (#53425)
- [x] +kontrolovat připojení k serveru a zobrazit chybovou zprávu v případě chyby připojení nebo chyby bezpečnostního tokenu (CSRF) (#53119)
- [x] +aktualizace testovacího frameworku codeceptjs na aktuální verzi (#52444)
- [x] +rozdělit dokumentaci na programování, uživatele, instalaci atd. (#52384)
- [x] +zapamatování nastavení zobrazených sloupců v datové tabulce pro každého uživatele zvlášť (#53545)
- [x] +nový vizuál přihlašovací obrazovky (#53617)

### Obecné

- [x] zlepšení integrace do zákaznických projektů na základě prvních testů (#52996, #53589)
- [x] zachovat relaci se serverem, zobrazit ukazatel průběhu odhlášení jako ve verzi 8 (#53119)
- [ ] napsání sekce Co je nového ve WebJET 8.8
- [x] +úprava dokumentace na formát `docsify` a přesunout na http://docs.webjetcms.sk/ (#52384)
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
