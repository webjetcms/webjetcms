# WebJET CMS 2026

Vítejte v dokumentaci k WebJET CMS verze 2026. Doporučujeme přečíst si [seznam změn](CHANGELOG-2026.md) a [roadmap](ROADMAP.md).

## Seznam změn ve verzi 2026.18

> WebJET CMS 2026.18 přináší **schvalování změn složek** s podporou víceúrovňového schvalování a **testování přístupnosti** integrované přímo do automatizovaných testů.
>
> V editoru stránek můžete nyní **maximalizovat dialogová okna** při nastavování aplikací, vkládání obrázků či odkazů. Nastavení aplikace zároveň získalo nové možnosti pro atributy přístupnosti (aria) a **vlastní CSS styly** zobrazení.
>
> V oblasti bezpečnosti přibyla **podpora přihlašování přes OAuth2/Keycloak/Google/Facebook** a **Přístupové klíče** (PassKey/WebAuthn), spolu s vylepšeními pro stabilnější provoz v klastrovém prostředí.

!>**Upozornění:** Verze určená pro `jakarta namespace`, vyžaduje aplikační server Tomcat 11, používá Spring verze 7. Před aktualizací [zkontrolujte požadavky](install/versions.md#změny-při-přechodu-na-jakarta-verzi).

### Průlomové změny

- Vyžadován je aplikační server Tomcat 11, verze 10 již není podporována (#58385).
- Odstraněné historické aplikace `/components/adresar/editor_component.jsp` a `/components/sharing_icons/editor_component.jsp`, které se již nepoužívaly. Chcete-li je nadále používat, stáhněte si z platformy [GitHub](https://github.com/webjetcms/webjetcms/tree/hotfix/2026.0-main/src/main/webapp/components) (#57409).

### Webové stránky

- Schvalování - přidáno [schvalování změn složek](redactor/webpages/approve/README.md) včetně schvalování vytvoření, editace a smazání složky. Schvalovatel obdrží email s přehledem změn a možností schválit nebo zamítnout změnu. Podporováno je i víceúrovňové schvalování (#58405).

![](redactor/webpages/approve/approve-group-page.png)

- Schvalování složek - v kartě `Na schválenie` přidány pod-karty `Dokumenty` a `Priečinky` pro oddělené zobrazení stránek a složek čekajících na schválení (#58405).
- Schvalování složek - přidáno zobrazení sloupce s datem schválení/zamítnutí a jménem schvalovatele v historii změn složek (#58405).
- Aplikace - přidána možnost nastavit [styly zobrazení aplikace](redactor/webpages/working-in-editor/README.md#karta-zobrazení). Můžete tak nastavit například odsazení aplikace ve stránce, šířku nebo různé styly zobrazení, ale také informace pro čtečku pro slabozraké návštěvníky (#osk418).

![](custom-apps/appstore/common-settings-tab.png)

- Editor stránek - do dialogového okna [Odkaz a Tlačítko](redactor/webpages/working-in-editor/README.md#tlačítka) přidána karta s rozšířenými nastaveními jako ID, titulek, popis pro čtečky (aria-label) a podobně (#osk115).
- Ninja - přidané pole `canonical` jako [volitelné pole Q](frontend/ninja-starter-kit/ninja-jv/page/README.md#informace-o-stránce) pro nastavení kanonické URL adresy stránky. V případě, že je pole prázdné, použije se URL adresa stránky. Hodnotu v šabloně získáte jako `${ninja.page.canonical}`. Přidá do URL parametr `page`, pokud existuje, pro zobrazení správné strany v seznamu novinek (#OSK149, #54273-88).
- Page Builder - opraveno přesouvání okna pro vložení bloků, opraveno překrývání výběru režimu editoru, zapracovány UX připomínky (#58353).
- Sjednocené používání nástroje `ImageMagick` pro změnu velikostí obrázků mezi galerií a `/thumb servlet` (#osk396).
- Přidána podpora vkládání obrázků ve formátu `webp` včetně zápisu při změně velikosti pomocí nativní knihovny `libwebp` přes `ImageIO` (#osk396).
- Přidáno konfigurační proměnné `imageMagickCustomParams*` pro [nastavení vlastních parametrů](redactor/apps/gallery/README.md#vlastní-parametry-imagemagick) `ImageMagick` operací podle typu operace a formátu obrázku (#osk396).
- Aplikace - doplněná možnost **maximalizovat a minimalizovat** okno pro **vkládání aplikací, obrázku, odkazů** atp. na stránku (#57409).
- Přidána podpora pro automatické obnovení všech provázaných (zrcadlených) stránek a složek z koše, když jedna z nich byla obnovena (#osk423).
- Složka - přidána možnost nastavit HTML kód nové stránky z lokálního Systém/Šablony složky, původně se seznam stránek četl podle konfigurační proměnné `tempGroupId` (#57409).
- Upravené názvosloví [volitelných polí](frontend/ninja-starter-kit/ninja-jv/page/README.md) pro SEO hodnoty (#228).
- Přidána ikona pro přesun [kurzoru na těžko dostupné místo](redactor/webpages/working-in-editor/README.md#vkládání-textu-na-těžko-dostupná-místa), jako například za poslední SVG ikonu v řádku a podobně (#osk105).

![](redactor/webpages/working-in-editor/wjmagicline-append.png)

### Aplikace

Předěláno nastavení vlastností aplikací v editoru ze starého kódu v `JSP` na `Spring` aplikace. Aplikace automaticky získávají také možnost nastavit [zobrazení na zařízeních](custom-apps/appstore/README.md#podmíněné-zobrazení-aplikace). Design je ve shodě se zbytkem WebJET CMS a datových tabulek (#57409).

- [Předpřipravené bloky (HTMLBox)](redactor/apps/htmlbox/README.md)

![](redactor/apps/htmlbox/editor-block.png)

- [Anketa snadno](redactor/apps/inquiry/inquiry-simple.md)

![](redactor/apps/inquiry/inquiry-simple-tab-basic.png)

- [Mapa](redactor/apps/map/README.md)

![](redactor/apps/map/map-editor.png)

- [Doporučení](redactor/apps/app-testimonials/README.md)

![](redactor/apps/app-testimonials/editor-style.png)

- [Předpřipravené bloky](redactor/apps/htmlbox/README.md)

![](redactor/apps/htmlbox/editor-block.png)

- Video - přidána možnost nastavit [CSS třídy pro poměr stran videa](redactor/apps/video/README.md#konfigurace), možnosti zobrazené v aplikaci se nastavují přes konfigurační proměnné `videoClasses`, `videoWrapperClass` a `videoItemClass` (#osk496).

### Formuláře

- Přidána karta [Statistika](redactor/apps/multistep-form/stat.md) pro zobrazení odpovědí formulářů ve formě grafů (#58333).

![](redactor/apps/multistep-form/stat-section.png)

- Pro vícekrokové formuláře přidán sloupec **Trvání vyplnění**, který zobrazuje jak dlouho trvalo vyplnění formuláře uživatelem (čas od jeho zobrazení po odeslání) (#58333).

### Galerie

- Přidána možnost přesouvat složky galerie přímo v editoru změnou nadřazené složky. Podrobnosti naleznete v části [Správa struktury](redactor/apps/gallery/structure.md) (#58433).
- Vylepšené zpracování přesunu složek galerie s cílem zabránit vzniku neplatných odkazů. Více informací v části [přemístění složky Galerie](redactor/apps/gallery/structure.md#přemístění-složky-galerie) (#58433).
- Přidána podpora pro filtrování složek galerií podle aktuálního ale i původního/souborového názvu složky galerie ve stromové struktuře (#58445).
- Přidána podpora pro [zobrazení původního/souborového názvu složky](redactor/apps/gallery/structure.md#nastavení-zobrazení-stromové-struktury) galerie ve stromové struktuře. Jméno složky na disku může být jiné než hezké jméno zadané ve vlastnostech galerie (#58445).

![](redactor/apps/gallery/jstree-settings.png)

### Manažer dokumentů

- Přidáno zobrazení hodnoty **Globální Id** pro dokumenty (#58357).
- Přidána možnost zvolit dokumenty k zobrazení v aplikaci pomocí jejich `globalId` hodnoty ve vnořené tabulce, více v části [Karta - Vybrané dokumenty](redactor/files/file-archive/file-archive-app.md#karta---vybrané-dokumenty) (#58357).

### Přístupnost

- Přidáno [automatizované testování přístupnosti](developer/testing/a11y.md) (a11y / WCAG) pomocí `axe-core` integrovaných do CodeceptJS. Testy pokrývají úrovně WCAG 2.0/2.1/2.2 AA (#58389).
- Zlepšená přístupnost / opraveno WCAG chyby na stránkách:
  - Přihlášení do administrace (#58389-2).
  - Úvod / menu položky / hlavička (#58389-3).
  - Zlepšené kontrasty barev v chybových zprávách a hlášeních (#58389-4).
  - Datové tabulky, editor (#58389-4).
- Rozšířené a11y testy o nové scénáře pro správce souborů, sledování, statistiky, nahrávání souborů, správu uživatelů a webové stránky. Metoda `a11y.check()` podporuje parametr `context` pro omezení kontroly na konkrétní část stránky včetně vnořených `iframe` elementů (#58389-5).

### Jiné menší změny

- Galerie - přidána podpora průsvitnosti v `png/webp/gif` obrázcích při změně jejich velikosti, pokud se nepoužívá [ImageMagick](redactor/apps/gallery/README.md#možné-konfigurační-proměnné) (#osk396).
- GitHub pipeline - po `merge pull request` se automaticky vygeneruje příspěvek na sociální sítě pomocí LLM (GitHub Copilot / Google Gemini) a přidá se jako komentář k danému `pull request` spolu s případnými fotkami obrazovky z dokumentace (#177).
- Skripty - přidána možnost nastavit, zda se má skript vkládat v editoru stránek v režimu PageBuilder (#58349).
- Skripty - přidán atribut [Pořadí vkládání](redactor/apps/insert-script/README.md) pro nastavení pořadí vkládání skriptů v rámci stejné pozice. Výchozí hodnota pro stávající skripty je 10, u nového skriptu se automaticky nastaví na nejvyšší hodnotu + 10 (#osk387).
- Editor stránek - opraveno určení složky pro nahrávání obrázků/souborů u nové/ještě neuložené webové stránky s duplicitním názvem: složka nyní odpovídá skutečné URL adrese stránky včetně přípony `-2`, `-3` atd. (#58361).
- Zrcadlení struktury - přidána možnost generovat [odkazy na jazykové mutace](redactor/apps/docmirroring/README.md#nastavení-atributu-hreflang) v hlavičce stránky pomocí aplikace `hreflang.jsp`, odkazy obsahují atribut `hreflang` pro lepší SEO optimalizaci jazykových verzí.
- Stromová struktura - přidáno automatické posunutí na aktuálně zvolený prvek ve stromové struktuře (#58433).
- Aplikace - přidána možnost skrýt pole/karty v editoru aplikace pomocí konfigurační proměnné `appHideFields`. Více se dočtete v části [Skrývání polí/karet](custom-apps/appstore/README.md#skrývání-polikaret) (#58433).
- `imageradio` - ​​přidána možnost nastavit pole typu `imageradio` jako `disabled` (#58333).
- Design všech grafů v celém projektu byl předělán z tmavého na světlý režim pro lepší sladění designu (#58333).

![](developer/frameworks/charts/frontend/line-chart.png)

- Aplikace `Tooltip` přejmenovaná na Nápověda (#205).
- Statistika návštěvnosti - upravené filtrování podle složky. Uživatel může zvolit libovolnou složku iz jiných domén, pokud nemá omezená práva na složky nebo má-li právo **Zobrazit statistiku pro všechny složky** (#58453).
- Uživatelé - upravená možnost vidět všechny složky v nastavení **Práva na adresáře a stránky**, pokud má administrátor právo **Správa administrátorů**. Složky jsou zobrazeny iv případě, že administrátor má sám omezená práva na složky (#58453).

### Oprava chyb

- `imageradio` - ​​opraveno zobrazení pole typu `imageradio` v editoru datatabulky (#58333).
- Webové stránky - opraveno obnovení stránky, která byla vytvořena přes zrcadlení a neměla historický záznam, který se dal použít k obnovení (#208).
- Webové stránky - při obnovení stránky z koše se použije historická verze aby se zachoval její původní stav před vymazáním (#208).
- Tlačítko - opravena možnost nastavit vlastnosti tlačítka které je zakázáno (atribut `disabled=disabled`) (#209).
- Fotogalerie - opraveno vyhledávání ve složkách (#58433).
- Soubory - doplněné zachování `.min.js` a `.min.css` v názvu souboru při nahrání do `/files,/images` složek (#213).

### Výkon

- Upraven `PkeyGenerator` pro režim cluster `auto`. Metoda `allocate` obalená do transakce (`setAutoCommit(false)`) s opakováním při `deadlock`. Zlepšené získání bloku iv režimu cluster `auto`, čímž se snižuje počet přístupů do databáze a riziko `deadlock`. Navýšení a čtení hodnoty `pkey_generator` probíhá v jednom atomickém SQL dotazu, což zabraňuje přidělení stejného bloku více uzlům clusteru. Používají se databázově specifické SQL příkazy (#213):

| Databáze | SQL příkaz | Minimální verze |
| --- | --- | --- |
| MySQL | `LAST_INSERT_ID(expr)` | 3.23+ |
| MariaDB | `LAST_INSERT_ID(expr)` | 5.1+ (všechny GA) |
| Microsoft SQL Server | `UPDATE ... OUTPUT inserted` | 2005+ |
| PostgreSQL | `UPDATE ... RETURNING` | 8.2+ |
| Oracle | `RETURNING value INTO` | 10g+ |

- `SeoManager` a `ClusterRefresher` tolerují databázový `deadlock` při `UPDATE/DELETE` operacích bez chybového logu (#213).
- Režim cluster `auto` - ​​přidána konfigurační proměnná `clusterAutoRandomDelay` pro [náhodné zpoždění startu](install/config/README.md#režim-auto) některých úloh, aby se snížilo riziko souběžných databázových konfliktů mezi uzly clusteru (#213).

### Bezpečnost

- Přidán soubor `.github/SECURITY.md` se [odpovědným oznamováním zranitelností](sysadmin/responsible-disclosure/README.md) (#187).
- Aktualizované knihovny `AspectJ, Eclipselink, slf4j, GoPay` (#57793).
- Verze `SpringSecurity` zvýšena na verzi 7 (#56665).
- Přidána možnost přihlašování prostřednictvím [OAuth2/Keycloak/Google/Facebook...](install/oauth2/oauth2.md) (#56665).

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/q8xs3qDq-G4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

- Odstraněny nepoužívané knihovny `lodash,pdfmake`, aktualizovaný seznam `dependency-check-suppressions`, opravena prvotní instalace (#204).
- Opraveno zpracování neplatného hesla při API/BASIC autentifikaci po přechodu na `Spring Security 7`, aby se chyba `password encoder` nepřeměnila na interní výjimku a požadavek byl korektně zamítnut (#58369).
- Přidána podpora přihlašování do administrace přes [Přístupové klíče](redactor/admin/logon.md#použít-přístupový-klíč) `PassKey/WebAuthn` (#58369).

![](redactor/admin/passkey-logon.png)

- Uživatelé - pokud vytváření nového uživatele nebo jeho editaci do pole heslo zadáte znak `*` vygeneruje se nové bezpečné heslo a zobrazí se vám v notifikaci (#58369).
- [Skupiny práv](admin/users/perm-groups.md) - přidána možnost nastavit příznak **Přístup ke všem adresářům web stránek** a **Přístup ke všem složkám souborového systému**, které při přihlášení přepíší sečtená práva z ostatních skupin a poskytnou uživateli neomezený přístup k web stránkám nebo souborům2 (#osk
- Aktualizovaná knihovna `Datatables.net/Editor` z verze `2.2.2/2.3.2` na `2.3.7/2.5.2` (#206).

### Dokumentace

- Upravený automatický překladač pro použití Google Translator API v3 (#58301).
- Anglická verze dokumentace nově přeložena (#58301).

### Pro programátora

- AI - nový `AI skill` pro opravu A11Y/WCAG chyb, stačí použít nástroj `/wj-accessibility`.
- Aktualizované závislosti na minimální požadavky pro Tomcat 11 (Tomcat 10 již není podporován). `Stripes` validace - upravené provádění EL výrazů z odstraněného `jakarta.servlet.jsp.el` na `jakarta.el` kvůli kompatibilitě s `jakarta.servlet.jsp-api:4.0.0` (#58385).
- Aktualizovaný způsob zobrazení API dokumentace na standard [OpenAPI 3.0](https://www.openapis.org/). Dokumentace je dostupná na adrese `/admin/swagger-ui/index.html` pro uživatele, kteří mají právo na editaci administrátorů (#57793).
- Administrace - změněn `build` souborů administrace z `webpack` na `rspack`, který je výrazně rychlejší (#206).
- Administrace - sjednocené generování `PUG` šablon pro `watch` a `prod`, odstraněné nepoužívané `npm` build závislosti a historické `webpack` skripty (#206).
- Administrace - doplněno automatické obnovení otevřené stránky při `npm run watch` po změně `JS/CSS/PUG` souborů (#206).
- Administrace - přidán skript `npm run analyze` s HTML reportem velikosti použitých knihoven (#206).
- Datatabulky - přidán nový typ události `DatatableColumnsEvent`, na který lze poslouchat a dynamicky upravit definici sloupců před inicializací tabulky. Více se dozvíte v části [Událost DatatableColumnsEvent](developer/backend/events-datatable.md#událost-DatatableColumnsEvent) (#58433).
- Doplněna knihovna `Jackson v3`, některé JSON objekty nemusí serializovat správně pokud nemají správné `Java Bean` jméno (např. `setcookieId` bez velkého `C`, nebo `set__rowNum__`). Nejlepší řešení je správně nastavit jméno proměnné, případně použít anotaci typu `@JsonProperty("__rowNum__")` i na `getter/setter` (#58369).
- Galerie - upravené volání knihovny ImageMagick, změněné API pro jeho volání na `ImageTools.executeImageMagick(...)` (#osk396).
- Grafy - nástroj/knihovna [chart-tool.js](../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js) pro práci s `amcharts` grafy byl aktualizován, přály se nové funkcionality, nové grafy a vylepšila.
- Grafy - přidána nová třída/knihovna [stats-by-charts.js](../../src/main/webapp/apps/_common/charts/stats-by-charts.js) pro rychlé vytváření celých sekcí statistik s využitím [chart-tool.js](../../src/main/webapp/admin/v9/src/js/libs/chart/chart-tools.js) pro vytváření grafů (#58333).
- Hlavičkové záložky - přidána podpora pod-karet přes funkci `WJ.headerSubTabs()` pro vnořené karty v seznamu Neschváleno ve webových stránkách (#58405).
- Odstraněna anotace `@Temporal` na datových sloupcích v databázových entitách, doporučené řešení je pro nové entity používat `java.time.*` typy. Zdá se, že Eclipselink/JPA správně detekuje typ `Date` jako datum a čas a anotace není nutná. Doporučujeme po aktualizaci zkontrolovat chování datových polí (#57793).
- Třída `PageListHolder/MutableSortDefinition` je ve Spring 7 `Deprecated`, jako přímou náhradu můžete použít naši implementaci `PagedListHolder/SortDefinition` z package `sk.iway.iwcm.system.datatable` (#57793).
- Webové stránky - obnovení z koše - doplněno [publikování událostí](developer/backend/events.md) `ON_RECOVER` a `AFTER_RECOVER` pro obnovu stránek a složek z koše (#161).
- Webové stránky - doplněna možnost upravit karty okně [Styl při použití PageBuilder](frontend/page-builder/blocks.md#podporný-javascript-kód) voláním funkce `window.pbBuildTabMenu`. Umíte tak pro zákazníka zobrazit jen relevantní karty a nastavení bloku (#58345).
- Webové stránky - doplněna možnost volat [vlastní funkci pro čištění HTML kódu](frontend/page-builder/blocks.md#podporný-javascript-kód) po vložení z `Microsoft Office` nebo při získání HTML kódu (#OSK49).
- `WebjetEvent` – přidána možnost nastavit uživatele typu `Identity` přímo do události. Vhodné při zpracování událostí, kde je potřebný uživatel, ale není dostupný `context` nebo `request` (#OSK423).
- `WJ.openIframeModal` - ​​přidána možnost přesouvat dialogové okno uchopením za hlavičku (drag & drop), maximalizovat/minimalizovat okno a definovat vlastní tlačítka v patičce přes parametr `buttons` (#58405).

![meme](_media/meme/2026-18.jpg ":no-zoom")