# Seznam změn verze 2026

## 2026.0-SNAPSHOT

> Vývojová verze aktualizovaná z main repozitáře.

### Průlomové změny

- Z administrace byla odstraněna závislost na knihovně [Vue.js](https://vuejs.org). Před aktualizací doporučujeme ověřit kompatibilitu vlastních aplikací. Velikost JavaScript souborů se zmenšila o cca 170kB, což má dopad také na rychlost inicializace administrace. Více v [sekci pro programátora](#pre-programátora).
- AspectJ - z distribuce byla odstraněna podpora `load-time weavingu` (`aspectjweaver` a `META-INF/aop-ajc.xml`); vestavěné aspekty se zpracují již při kompilaci, více v [sekci pro programátora](#pre-programátora). Při použití v MultiWeb instalaci můžete odstranit `-javaagent:/www/tomcat/.../aspectjweaver.jar` nastavení z `JAVA_OPTS` v aplikačním serveru (#290).
- Export obsahu pro Flash - odstraněna byla historická funkce generování XML souborů `/flash_xml/{docId}.xml` při publikování stránky. Konfigurační proměnná `exportFlash` již není podporována a její definování v `SpringConfig` funkci neobnoví (#293).
- Microsoft SQL Server - ukončena byla podpora verzí starších než 2012 a odstraněna konfigurační proměnná `mssqlUseOldTopQuery`. WebJET CMS vyžaduje Microsoft SQL Server 2012 nebo novější, starý způsob stránkování pomocí `TOP` již není podporován (#293).
- Formulář snadno a vícekrokové formuláře - upravené zobrazení `tooltip` z původního `i` elementu na standardní `button`. Je tak splněn požadavek na přístupnost - tooltip je dostupný myší i klávesnicí (#306).

### Webové stránky

- Koš webových stránek - přidáno [automatické mazání starých stránek a složek](redactor/apps/gdpr/data-deleting.md) z koše podle nastaveného retenčního období. Přidána možnost mazání stránek a složek v koši i v sekci [Mazání dat](sysadmin/data-deleting/README.md) podle zvoleného rozsahu dat. Sjednocená logika výpočtu počtu a mazání, opraveno trvalé odstranění složky koše a prázdných složek (#271).

![](sysadmin/data-deleting/database-delete.png)

- Přesměrování - přesměrování vytvořená uživatelem jsou odlišena od automatických (označeno šedým pozadím) a lze je samostatně [filtrovat v seznamu přesměrování](redactor/webpages/redirects/README.md#automatické-a-uživatelem-vytvořené-přesměrování) (#58625).

![](redactor/webpages/redirects/redirect-path.png)

- SEO - přidáno samostatné nastavení **Následujení odkazů vyhledávači** s možnostmi **Podle nastavení Procházet**, **Povolit následování odkazů** (`follow`) a **Zakázat následování odkazů** (`nofollow`). HTTP hlavička `X-Robots-Tag` a Ninja `${ninja.page.robots}` používají stejnou logiku: při indexování bez omezení vrátí `all`, jinak kombinaci direktiv `noindex` a `nofollow` podle nastavení stránky. Více v [dokumentaci Ninja](frontend/ninja-starter-kit/ninja-jv/page/README.md#nastavení-indexování-string) (#OSK563).
- SEO - v [skupinách šablon](frontend/templates/template-groups.md#karta-seo) lze nastavit výchozí SEO popis, obrázek a alternativní text obrázku. Objekt Ninja `Page` je použije, pokud stránka nemá zadané vlastní hodnoty; přibyla také hodnota `${ninja.page.seoImageAlt}` pro `og:image:alt` (#OSK593).

![](frontend/templates/temps-groups-edit-seo.png)

- Ninja - doplněno [generování rozměrů](frontend/ninja-starter-kit/ninja-bp/README.md) SEO obrázku `og:image:width` a `og:image:height` (#OSK563).
- Šablony - přidána možnost nastavit přesun `<style>` a `<link rel="stylesheet">` značek z těla stránky do `<head>` přes [volbu v šabloně](frontend/templates/templates.md) s podporou globální konfigurační proměnné `showDocMoveStyleToHead`. Bloky v IE podmínkách, `noscript` a `script` zůstávají na místě (#231).

![](frontend/templates/templates-edit-advanced.png)

- V dialogu vkládání obrázků přidána karta **Miniatura** pro nastavení parametrů [generování zmenšených obrázků](redactor/webpages/working-in-editor/README.md#karta-miniatura) `thumbnail` (#58317).

![](redactor/webpages/working-in-editor/image_dialog-thumb.png)

- V dialogu vkládání odkazu přidána karta [Manažer dokumentů](redactor/files/file-archive/README.md) pro snadné vkládání odkazů na soubory v manažerovi dokumentů, nahrávání nových souborů a jejich správu. Více se dozvíte v části [Odkazy na soubory a nahrávání souborů](redactor/webpages/working-in-editor/README.md#odkazy-na-soubory-a-nahrávání-souborů) (#58593).

![](redactor/webpages/working-in-editor/link_dialog-file-archive.png)

- Soubory Manažera dokumentů ve složce `/files/archiv` jsou v dialozích vkládání odkazu a obrázku dostupné pouze pro zobrazení a výběr. Nahrávání, přejmenování, mazání a ostatní úpravy lze provést pouze přes [Manažer dokumentů](redactor/files/file-archive/README.md).

- [Fotobanka](redactor/webpages/working-in-editor/README.md#karta-fotobanka) - při stahování obrázku z fotobanky lze nastavit název souboru. Název se automaticky předvyplní a očistí, přípona se určí podle zdrojového obrázku a stávající soubor se nepřepíše. Přidána také podpora výběru typu a kategorie obrázku a možnost hledat video soubory (#58645).

![](redactor/webpages/working-in-editor/image_dialog-pixabay.png)

### Headless režim

Přidána [podpora headless režimu](frontend/headless/README.md), ve kterém WebJET CMS slouží čistě jako `backend` CMS. Obsah, navigace, vyhledávání a formuláře jsou dostupné přes REST API. Frontend aplikace (např. Astro, Next.js, Vue, React nebo jakýkoli HTTP klient) si data stáhne a zobrazuje je podle vlastních šablon (#258).

![](frontend/headless/home.png)

V jednom WebJET CMS můžete mít více (desítky) domén a následně mít menší web stránky vytvořené v různých technologiích, které konzumují a zobrazují obsah z CMS systému. Podporováno je také vkládání standardní aplikací jako foto galerie, formuláře, GDPR cookies a podobně.

![](frontend/headless/gallery.png)

- Přesměrování - přidáno [čištění a optimalizace přesměrování](redactor/webpages/redirects/README.md#čištění-přesměrování) s náhledem změn před provedením. Čištění odstraňuje staré, duplicitní a cyklické přesměrování a zkracuje řetězce přesměrování (#58629).

![](redactor/webpages/redirects/redirect-cleaning-analyzed.png)

### Formuláře

- [Statistiky vícekrokových formulářů](redactor/apps/multistep-form/stat.md) byly rozšířeny o datový filtr a pokročilé metriky zobrazení/pokusů/jazyků etc. (#58509).

![](redactor/apps/multistep-form/stat-section-advanced.png)

- Do vícekrokových formulářů přidána možnost jednoduše nastavovat výběrová pole a skupiny zaškrtávacích / výběrových polí (#58517).

![](redactor/apps/multistep-form/form-item-editor-advanced.png)

- Do vícekrokových formulářů přidána možnost propojení výběrového pole a skupiny zaškrtávacích / výběrových polí na číselník (#58517).

![](redactor/apps/multistep-form/form-item-editor-advanced-enum.png)

- Přidána možnost ve formuláři nastavit maximální kombinovaná velikost souboru, původně se dala nastavovat jen velikost pro soubor, pokud formulář obsahuje více souborů lze nastavit maximální velikost pro všechny soubory společně (#58517).
- [Vícekrokové formuláře](redactor/apps/multistep-form/README.md) - přidáno duplikování celého formuláře včetně nastavení, kroků a položek. Editor položek zobrazuje jejich automaticky vytvořený identifikátor a podporuje vlastní chybovou zprávu, oříznutí mezer, prázdnou možnost ve výběrovém seznamu a nový typ pole automatické doplňování s vyhledáváním bez rozlišení diakritiky (#osk573).
- [Vícekrokové formuláře](redactor/apps/multistep-form/README.md) - do úvodního textu kroku a [stránky s verzí pro email](redactor/apps/form/README.md#karta---nastavení) lze vložit hodnoty položek pomocí značek. Opraveno bylo rozpoznávání polí jména a emailu podle začátku identifikátoru, zpracování jazyka, reCAPTCHA v3, nezávislé vložení více instancí formuláře na jednu stránku a vymazání všech odpovědí bez odstranění definice formuláře (#osk573).
- [Vícekrokové formuláře](redactor/apps/multistep-form/README.md) - přidána možnost zadat vlastní chybovou zprávu a oříznout mezery na začátku a konci textu zadaného návštěvníkem (#osk573).
- Formulář snadno a vícekrokové formuláře - [vlastní HTML kód tooltipu](redactor/apps/formsimple/README.md#vlastní-html-kód-tooltipu) v překladovém klíči `components.formsimple.tooltipCode` podporuje značky s identifikátory pole, položky a kroku včetně unikátního `${tooltipId}`. Ovládací prvek tak lze pomocí `aria-describedby` jednoznačně propojit s obsahem tooltipu i při opakovaném vložení formuláře na stránku. Vícekrokový formulář po každém zobrazení kroku publikuje na objektu `window` [událost `WJ.multistepForm.stepShown`](redactor/apps/multistep-form/README.md#javascript-událost-po-zobrazení-kroku) s odkazy na jeho HTML elementy a identifikátory formuláře a kroku (#306).
- Formulář snadno a vícekrokové formuláře - výchozí tooltip je nově dostupný myší i klávesnicí, poskytuje stabilní kontextový název a popis, zůstává zobrazen při přesunu kurzoru na jeho obsah a lze jej zavřít klávesou `Escape` (#306).

### Sémantické vyhledávání

- Přidána podpora [sémantického vyhledávání](redactor/apps/semantic-search/README.md) postaveného na technologii vektorové databáze `pgvector` a `OpenAI embeddings`. Umožňuje návštěvníkům najít relevantní stránky na základě **významu otázky**, nejen shody klíčových slov (#211).

- Doplněný hybridní režim sémantického vyhledávání a volitelná RAG odpověď z indexovaného obsahu. Aplikace **Vyhledávání** má nová nastavení pro typ vyhledávání, hybridní chování, výběr AI asistenta a limity kontextu odpovědi (#58521).

![](redactor/apps/semantic-search/rag-result.png)

- Embedding indexování a vyhledávání používá poskytovatele a model nastavený v systémovém AI asistentovi. Indexy různých poskytovatelů a modelů mohou existovat současně; stránka **Sémantický index** zobrazuje aktuální nastavení a při opětovném indexování zachová ostatní kombinace. Jádro embedding požadavků, odpovědí a komunikace s poskytovateli bylo vyčleněno do knihovny `webjet-ai` ; WebJET CMS nadále zajišťuje výběr asistenta, indexování a uložení vektorů (#58694).

### Aplikace

- Číselníky - pro pojmenovaná řetězcová pole lze v nové kartě [Typy řetězcových polí](redactor/apps/enumeration/README.md#karta-typy-řetězcových-pole) nastavit typ pole, možnosti výběru, povinnost, pomocný text a omezení délky stejně jako u volitelných polí. Nabídka a názvy konfigurací vycházejí z poslední uložené verze typu číselníku. Nepojmenovaná pole zůstávají skrytá, nevyhodnocují se jako povinná a pole bez specifické konfigurace se zobrazí jako běžný text. Starší vlastní Excel šablony a integrace REST API je třeba upravit z atributů `string1` až `string12` na `fieldA` až `fieldL` (#58641).

![](redactor/apps/enumeration/editor_stringFieldTypes.png)

- Elektronický obchod - přidaná aplikace [Statistiky](redactor/apps/eshop/stats/README.md) se souhrnnými ukazateli, filtrováním podle stavu, měny a období a grafy tržeb, produktů, kategorií, způsobů doručení a platebních metod (#58065).

![](redactor/apps/eshop/stats/stats.png)

- Přidána nová aplikace [Přesměrování podle jazyka](redactor/apps/language-redirect/README.md) pro automatické přesměrování návštěvníků na jazykovou verzi stránky podle detekce jazyka z HTTP hlavičky `Accept-Language`. Podporuje až 8 přiřazení jazyků na URL adresy, respektování jazykového cookie a možnost přesměrování pouze na kořenové URL (#58497).

![](redactor/apps/language-redirect/editor-basic.png)

- Rezervace - aplikace **Rezervace času** a **Rezervace dní** mají sjednocený vizuální styl podle kalendáře `Vanilla Calendar`, upravené kontrastní barvy buněk podle `WCAG`, oddělené vizuální CSS styly do samostatných souborů a **Rezervace času** zobrazuje v hodinových buňkách skutečnou cenu dle ceníku.

- Rezervace - přidána nová aplikace [Moje rezervace](redactor/apps/reservation/my-reservations-app/README.md), která přihlášenému uživateli zobrazí přehled jeho rezervací, stavem rezervace a možností smazání povolených budoucích rezervací (#58565).

![](redactor/apps/reservation/my-reservations-app/app-page.png)

- Manažer dokumentů - přidáno zobrazení stromové struktury složek (#58593).

![](redactor/files/file-archive/datatable.png)

- Manažer dokumentů - přidána možnost nahrát více souborů najednou přes `drag&drop` (#58593).

![](redactor/files/file-archive/drag-drop-upload-dialog.png)

### Galerie

- Přidána možnost nastavit samostatný způsob [změny velikosti pro velký obrázek](redactor/apps/gallery/structure.md#karta-rozměry) nezávisle na malém obrázku. Ve výchozím nastavení se použije způsob změny velikosti stejně jako je nastaveno pro malý obrázek. Nastavení lze volitelně překopírovat i do podsložek (#58633).

![](redactor/apps/gallery/dir-sizes-tab.png)

### Volitelná pole

- Kompletně implementovaná funkčnost [nastavení volitelných polí](frontend/webpages/customfields/custom-fields-settings.md). Umožňuje centrálně nastavit vlastnosti polí bez editace překladových klíčů. Podporovány jsou všechny typy polí (text, textarea, select, multiselect, autocomplete, enumeration, obrázek, odkaz, JSON a další) s typově specifickými nastaveními jako maximální délka textu, možnosti výběru, propojení na číselníky nebo závislost na jiných polích. Uživatelské rozhraní nabízí také jednoduchý způsob nastavení možných hodnot pro výběrová/autocomplete pole (#58529).

![](frontend/webpages/customfields/custom-fields-settings-editor.png)

- Přidána možnost nastavit volitelné pole jako povinné (#58413).
- Přidány nové typy volitelných polí [přepínač a zaškrtávací pole](frontend/webpages/customfields/custom-fields-settings.md#rozdíl-mezi-selectmultiselect-a-radiocheckbox) s podporou statických možností i propojení na číselník. Typ `multiselect` nyní také podporuje [propojení na číselník](frontend/webpages/customfields/custom-fields-settings.md#zdroj-možností). Původní typ `enumeration` byl nahrazen přepínačem zdroje možností u typů `select`, `multiselect`, `radio` a `checkbox` kde se pro všechny tyto typy polí načtou možnosti z propojeného číselníku (#58637).

### Přístupnost

- Administrace - rozšířené ovládání klávesnicí a podpora čteček obrazovky pro datové tabulky, modální okna a notifikace, bublinová nápověda, výběr data a barvy a HTML editor. Upraveno bylo přesouvání a zvracení zaměření, výběr řádků a buněk, přístupné názvy a ARIA stavy, označení povinných polí, kontrasty ovládacích prvků a odkaz na přeskočení na hlavní obsah. Byly doplněny automatizované regresní testy `a11y` pro tyto scénáře (#235).

### Multiweb

- Přidána možnost [vytvořit novou doménu](install/multiweb/config.md) z řídicí domény, vytvoří také uživatele, skupinu šablon, šablonu a systémové stránky (#58525).
- Přidáno zobrazení seznamu skupin šablon (#58525).
- V řídící doméně je možné upravovat všechna přesměrování domén.
- V řídicí doméně přidána možnost zobrazit všechny soubory.

### Jiné menší změny

- Monitorování serveru - v aktuálních hodnotách byla přidána sekce [Kódování znaků](sysadmin/monitoring/README.md#kódování-znaků), která zobrazuje kódování HTTP odpovědí, JVM a locale prostředí běžícího procesu aplikačního serveru (#305).
- Konfigurace - přidán hierarchický strom konfiguračních proměnných s pohledy **Změněno**, **Zákaznické**, **Všechny** a modulovými větvemi (#293).
- Konfigurace - přidána možnost **Nastavit dočasně**, která nastaví hodnotu konfigurační proměnné pouze na aktuálním uzlu bez uložení do databáze. Po restartu se obnoví hodnota uložená v databázi (#291).

![](admin/setup/configuration/page.png)

- Automatizované úlohy - přidána možnost [manuálně spustit úlohu](admin/settings/cronjob/README.md) na uzlu nebo skupině uzlů nastavené v poli **Běží na uzlu**. Původní lokální spuštění na aktuálním uzlu zůstává dostupné samostatným tlačítkem (#58718).

- Překladové klíče - přidána stromová struktura prefixů překladových klíčů s filtrováním seznamu podle zvoleného prefixu (#58714).

![](admin/settings/translation-keys/dataTable.png)

- Průzkumník - ve vlastnostech složky přidána karta [Nepoužívané soubory](redactor/files/fbrowser/folder-settings/README.md#nepoužívané-soubory) pro vyhledání a smazání nepoužívaných souborů (#58621).

![](redactor/files/fbrowser/folder-settings/folder_settings_unused_files_result.png)

- Průzkumník - přidáno právo **Povolit nahrávání souborů s diakritikou**, které umožňuje zachovat diakritiku při nahrávání, vytváření a přejmenování souborů a složek ve složkách `/files`, `/images` a `/shared`. Bez tohoto práva se názvy nadále automaticky upraví bez diakritiky (#58589).
- Přihlášení - zrychlené načtení úvodní stránky v administraci - přidána vyrovnávací paměť pro seznam posledních stránek, změněných stránek a auditních záznamů (#58589).
- Export/import souborů - upravený design dialogového okna a responzivní zobrazení formulářových polí podle aktuálního designu administrace (#58581).
- Grafy - přidán nový stromový graf [`TreeChartForm`](developer/frameworks/charts/frontend/statjs.md#graf-typu-tree) pro vizualizaci hierarchických dat s přibližováním, rozbalováním a maximalizováním (#58065).
- Google reCaptcha - doplněna podpora vkládání více více krokových formulářů do stránky, podporován je režim `invisible/reCaptcha/reCaptchaV3`, upravené chybové hlášení na srozumitelnější text (#osk573).
- Webové stránky - doplněné zvýraznění elementu nad kterým je vyvoláno kontextové menu. Důležité pokud chcete provést akci Smazat element, abyste přesně viděli který element je označen (#OSK675).
- Multiweb - doplněna možnost přejmenovat existující doménu + přesměrování po přejmenování (#58317-15).
- Multiweb - upraveno [zobrazení skupin šablon](install/multiweb/README.md) podle dostupných šablon a aliasu aktuální domény (#58317-17).
- Statistika - nastavené datum/rozsah od-do se ukládá v prohlížeči a je zapamatován i po odhlášení/restartu prohlížeče (#58065).
- Vícekrokové formuláře - doplněné přesunutí (`scroll`) na začátek formuláře po přechodu na další krok (#osk573).

### Oprava chyb

- Formuláře - opraveno archivování formulářů (#305).
- Průzkumník - upravené porovnávání souborů s diakritikou při kontrole existence souboru při jeho přepsání - formát `utf-8 NFC vs NFD` (#58317-12, #58698).
- Webové stránky - opraveno přidávání prázdného `P` elementu na konec stránky (#58317-13).
- Webové stránky - opraveno načtení hodnoty `ckeditor_button_sizes` pro tlačítko typu `A` (#OSK674).
- Monitorování SQL - opravena správa životního cyklu měření `PreparedStatement`. Záznam se odstraní i při zavření před spuštěním měření a jednotlivé objekty `PreparedStatement` se rozlišují podle identity bez volání JDBC `hashCode()` a `equals()`. Souběžný přístup používá `ConcurrentHashMap` a atomický stav bez globálního `synchronized` bloku, takže vlákna nečekají na společný zámek a měření se nespojí ani při kolizi identitních hashů.

### Bezpečnost

- Přidána podpora generování `nonce` pro [Content-Security-Policy](sysadmin/pentests/README.md#content-security-policy-csp) hlavičku (#58533).
- AI asistenti - přidána ochrana před `prompt injection` útoky s oddělením systémových instrukcí od uživatelského obsahu a detekcí kódovaných vstupů (#58549).

### Dokumentace

- Vytvořena nová sekce [Přehled nových vlastností](sales/README.md) která obsahuje popisy nových vlastností a **funkcionalit WebJET CMS srozumitelným jazykem**, bez zbytečně technických formulací (#58505).
- Vytvořená sekce [Řešení problémů](sysadmin/troubleshooting/README.md) v manuálu pro provoz.

### Pro programátora

- Administrace - odstraněná závislost na [Vue.js](https://vuejs.org). Stromová pole, úvodní stránka, výběr oblasti obrázku a monitorování serveru používají nativní [web komponenty](developer/frameworks/web-components.md). Globální objekt `window.VueTools` ani balíky pro Vue již nejsou součástí administrace. Vlastní rozšíření je musí nahradit web komponenty nebo si Vue sestavit samostatně (#58722).
- AI asistenti - klientská logika nezávislá na poskytovateli pro OpenAI, Gemini a OpenRouter, zpracování streamů, typy požadavků/odpovědí a ochrana promptů byly vyčleněny do samostatného artefaktu `com.webjetcms:webjet-ai` a externího [repozitáře webjet-ai](https://github.com/webjetcms/webjetcmi/webjetcmi). WebJET CMS předává konfiguraci přes typovaný adaptér a nadále zajišťuje auditování, perzistenci a integraci uživatelského rozhraní. Jedná se o nekompatibilní změnu: původní CMS SPI pro vlastní poskytovatele a jeho transportní a streamovací podpůrné třídy byly odstraněny. Vlastní poskytovatelé je nutné migrovat na rozhraní `AiProvider` knihovny a CMS adaptér `LibrarySupportLogic` (#58670).
- AI poskytovatelé - vlastní implementaci lze [přidat do projektu](custom-apps/apps/ai/assistants/README.md) jako Spring bean `AiProvider` ; CMS ji automaticky spojí s vestavěnými poskytovateli. Konfigurace a pole editoru jsou soustředěny v jednom adaptéru `LibrarySupportLogic` /`AiAssitantsInterface`. Možnosti generování obrázků se načítají podle poskytovatele, modelu a operace z knihovny `webjet-ai`, takže se dynamicky zobrazí pouze podporovaný počet, rozměr, kvalita a poměr stran (#58694).
- Datové tabulky - přidán nový typ pole `OPTIONS` pro [dynamický seznam hodnot](developer/datatables-editor/standard-fields.md#options) v editoru. Každý řádek obsahuje dvě textová pole (klíč a hodnota), podporuje přidávání, odebírání a změnu pořadí pomocí `drag & drop` (#58517).

![](redactor/apps/multistep-form/form-item-editor-advanced.png)

- Datové tabulky - rozšířená funkcionalita pole typu [`OPTIONS`](developer/datatables-editor/standard-fields.md#options) o možnost přidat prázdnou hodnotu pomocí `allowEmptyOption` (#osk573).
- Datové tabulky - HTML editor [`QUILL`](developer/datatables-editor/standard-fields.md#quill) při úpravě zdrojového kódu odstraňuje nadbytečné prázdné odstavce (#osk573).
- Datové tabulky - přidán nový typ pole `ENUMERATION` pro [napojení na číselníky](developer/datatables-editor/standard-fields.md#enumeration) v editoru. Pole ukládá konfiguraci ve formátu `enumeration-options|ID_CISELNIKA|MENO_STLPCA_TEXTU|MENO_STLPCA_HODNOTY` a umožňuje nastavit zdroj hodnot (#58517).
- Datové tabulky - [výběr složky](developer/datatables-editor/field-json.md#možnosti-classname) přes pole `dt-tree-dir-simple` při omezených právech správně zobrazuje nastavenou kořenovou složku i neaktivní rodiče povolených složek a respektuje konfigurační proměnnou `fbrowserShowOnlyWritableFolders`. Přidaný atribut `data-dt-field-writableOnly` umožňuje omezit výběr pouze na složky s právem na zápis (#58317-17).

![](redactor/apps/multistep-form/form-item-editor-advanced-enum.png)

- AspectJ - historické aspekty `CloudFilter`, `SqlPerformance` a `AspectException` ve formátu `.aj` byly migrovány na Java třídy s anotací `@Aspect`. Gradle nejprve zkompiluje zdrojové kódy přes `javac`, který spustí anotační procesory Lombok/MapStruct, a plugin `io.freefair.aspectj.post-compile-weaving` následně zpracuje bajtový kód pomocí `AspectJ weaving`. Odstraněna byla duplicitní Ant/AJC kompilace i lokální kopie build knihoven; Ant balení používá výstup z Gradle a úlohy `Delombok`. `CloudFilter` používá `execution pointcut` a příslušný `advice` se při kompilaci vloží do `GroupsDB` a `DocDB`, takže se filtrování aplikuje i při volání z JSP zkompilovaných bez LTW. Byly doplněny regresní testy a aktualizována [dokumentace nasazení](developer/install/deployment.md#kompilace-java-a-aspectj) (#290).
- Logování - do [Logback MDC](https://logback.qos.ch/manual/mdc.html) doplněn atribut `sessionId` a přihlašovacího jména uživatele `userLogin` (#OSK526).
- Aktualizovaná knihovna [Tabler Icons](https://tabler.io/icons) na verzi 3.44.0, vyřešen problém se současným používáním `Outline` a `Filled` sad (#58509).
- Web stránky - pokud potřebujete mít prázdný první řádek v konfigurační proměnné `imageMagickCustomParams*` pro [nastavení vlastních parametrů](redactor/apps/gallery/README.md#vlastní-parametry-imagemagick) `ImageMagick` zadejte hodnotu `---`.
- Překladové klíče - upravené auditování chybějících překladových klíčů - vyloučené auditování pokud se později testuje, zda klíč skutečně existuje (#261).
- Testování - přidáno [automatizované nahrávání prezentačních videí](developer/testing/video.md) pomocí CodeceptJS a Playwright. Video scénáře vytvářejí kvalitní WebM výstup se syntetickým kurzorem, přirozenou dráhou pohybu a reprodukovatelným průběhem kliknutí. Mluvené slovo lze ze scénáře automaticky vygenerovat přes ElevenLabs API jako MP3 soubor s nastavitelným modelem a hlasem (#299).
- Konfigurace - ze tříd `Constants` a `ConstantsV9` byly odstraněny zastaralé konfigurační proměnné. Pokud je ve svém projektu používáte můžete si do vašeho `SpringConfig` přidat potřebnou definici:

```java
		setString("smsSendCmd", "", MOD_SMS,
				"Format pre odosielanie SMS, pre Orange:email e2sms@e2sms.orange.sk smtp.iway.sk tomas.kosec@interway.sk, pre T-Mobile:email sms.eurotel.sk obsidian.interway.sk");
		setString("smsSendInternational", "", MOD_SMS, "Format cisla pre SMS, moze mat hodnoty ++42, +42, 42");
		setInt("smsSendMaxlength", 140, MOD_SMS, "maximalna dlzka SMS spravy");
		setString("syncRemoteServer", "", "sync", "Adresa servera, s ktorym sa tento synchronizuje");
		setString("multilang", "", MOD_CONFIG, "overwrite string na ziskanie moznych hodnot MultilangDB");
		setBoolean("editorEnableXHTML", true, MOD_OBSOLETE, "xhtml mode for Struts framework");
		setBoolean("usrLogonRequireSMS", false, "user;security",
				"ak je true, tak prihlasenie na stranke vyzaduje SMS autorizaciu, vyzaduje nastavenie modulu SMS");
		setString("fbrowserFileEditor", "editarea", MOD_OBSOLETE, "typ editora suborov - normal, alebo editarea");
		setString("chartColors", "#5C5CF7,#F75C5C,#5CF75C,#FFC165,#E463E4,#A13600,#55FFFF,#FFAFAF,#B10505,#065BD8",
				"stat;config", "farby ciar a textov pre grafy statistiky (Open Flash Chart)");
		setString("mapGoogleLicense", "", "map", "predvoleny licencny kluc pre google mapy");
		setBoolean("magzillaChangeEmailRecipients", true, MOD_MAGZILLA,
				"flag ci sa v magzille pri commente k bugu zobrazi moznost zmenit komu sa posle email, alebo sa nezobrazi a maily sa poslu podla default nastaveni");
		setString("magzilla_mail_sufix", "interway.sk", MOD_MAGZILLA, "suffix emailu pre komunikaciu s magzillou");
		setString("magzilla_web", "", MOD_MAGZILLA,
				"base URL adresa pre magzilla stranky (napr. http://www.interway.sk/helpdesk)");
		setString("magzilla_attachments_dir", "/files/helpdesk/", MOD_MAGZILLA, "adresar pre prilohy magzilly");
		setString("mgz_mailbox", "INBOX", MOD_MAGZILLA, "udaje pre mail parser helpdesku - priecinok s inboxom");
		setString("mgz_user_name", "", MOD_MAGZILLA, "udaje pre mail parser helpdesku - prihlasovacie meno");
		setString("mgz_host", "", MOD_MAGZILLA, "udaje pre mail parser helpdesku - adresa POP3 servera");
		setInt("mgz_port", -1, MOD_MAGZILLA, "udaje pre mail parser helpdesku - port POP3 servera");
		setBoolean("mgz_use_ssl", false, MOD_MAGZILLA,
				"udaje pre mail parser helpdesku - pouzitie SSL pri pristupe k POP3 serveru");
		setString("mgz_password", "", MOD_MAGZILLA, "udaje pre mail parser helpdesku - heslo");
		setString("mgz_client_type", "pop3", MOD_MAGZILLA, "udaje pre mail parser helpdesku - typ pripojenia");
		setString("mgz_strings",
				"__________ Informacia od ESET NOD32 Antivirus|--\nS pozdravom|<br>--\n<br>|<pre class=\"moz-signature\"|<p class=MsoAutoSig>",
				MOD_MAGZILLA, "zoznam retazcov oddelenych znakom | za ktorymi sa povazuje sprava za ukoncenu");
		setString("mgz_subjects", "Re:|Fwd:|FW:", MOD_MAGZILLA, "prefixy, ktore sa odstranuje z predmetu spravy");
		setString("mgzManagerGroupName", "Prava-manazeri", MOD_MAGZILLA, "Nazov hlavnej skupiny manazerov");
		setString("mgzClientGroupName", "HD Klienti", MOD_MAGZILLA, "Nazov hlavnej skupiny klientov");
		setString("mgzSolverGroupName", "Prava-solveri", MOD_MAGZILLA, "Nazov hlavnej skupiny solverov");
		setString("mgzTesterGroupName", "Prava-testeri", MOD_MAGZILLA, "Nazov hlavnej skupiny testerov");
		setString("mgzManagerBugViews", "managerAllBugs,managerWaitingBugs,allNewBugs,allBugs,notSolvedBugs,solvedBugs",
				MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia - pre manazera");
		setString("mgzClientBugViews", "clientAllBugs,clientWaitingBugs,clientSolvedBugs", MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia -  pre klienta");
		setString("mgzSolverBugViews", "assignedSolverBugs,allNewBugs,notSolvedBugs,solvedBugs", MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia - pre solvera");
		setString("mgzTesterBugViews", "assignedTesterBugs,newTesterBugs,allTesterBugs,notSolvedBugs,solvedBugs",
				MOD_MAGZILLA,
				"predvolene pohlady bugov generovane do selectBoxu v MagZille pri parametri allowBugs=all, generuju sa do selectBoxu podla poradia - pre testera");
		setString("microsite_web", "", MOD_MICROSITE,
				"base URL adresa pre microsite stranky (napr. http://www.interway.sk/microsite)");
		setInt("KurzyDBCacheInMinutes", 60, "kurzy;performance", "default nastavenie poctu minut, pre KurzyDB");
		setBoolean("magmaSaveAttendance", false, "magma",
				"Nastavenim na true sa bude zaznamenavat prihlasenie pouzivatela do prace");
		setString("magma_defaultApproverEmail", "", "magma",
				"email adresa pre schvalovanie dovoleniek ak nie je nikto nastaveny");
		setString("magma_holidaysApproveCC", "", "magma",
				"CC email so spravou o schvaleni dovolenky (napr. email ekonomky)");
		setString("magmaCalendarLoginEnabled", "", "magma",
				"ciarkou oddeleny zoznam IP adries z ktorych sa zaznamena prichod a odchod do prace (aby sa nezaznamenal prichod do prace napr. z domu");
		setString("usersPositionListReport", "", MOD_MAGZILLA,
				"Pre helpdesk definuje zoznam pozicii zamestnanca, pre ktore sa budu generovat reporty. Jednotlive pozicie sa oddeluju znakom |.");
		setString("magma_meetingUserGroup", "", "magma",
				"Názov skupiny z ktorej sa budú zobrazovat pouzivatelia pri vytvarani schodzky");
		setString("magzilla_private_web", "", MOD_MAGZILLA,
				"interna base URL adresa pre magzilla stranky(napr. http://intra.iway.sk/helpdesk)");
		setString("magzillaNotifyOnTicket", "", MOD_MAGZILLA,
				"Zoznam bodkociarkou oddelenych zaznamov v tvare id ticketu:email napr. (5555:helpdesk@iway.sk;). Nasledne ak si pouzivatel prida ticket 5555 do kalendara, odosle sa mail na helpdesk@iway.sk");
		setBoolean("webdavActive", false, "webdav", "Určuje či je prístup cez WebDav aktívny");
		setString("webdavUrlPrefix", "webdav", "webdav", "Určuje prefix cez ktorý je WebDav pristupny napr 'webdav' ");
		setString("webdavSharedDirectories", "/css/;/jscripts/;/files/;/images/", "webdav",
				"Urcuje, ktore adresare su pristupne cez WebDav. Priklad : /images/;/files/;");
		setString("mgzAutoreplyWords", "Out Of Office|Automatická odpoveď|AutoReply", MOD_MAGZILLA,
				"Slovička, ktorými sa definujú autoreply maily v predmete správy. Takéto správy sa odstraňujú. Pokiaľ je hodnota prázdna, tak sa funkcia odstraňovania takýchto e-mailov vypne.");
		setString("magzillaAutomaticChangeStatus", "RIESI SA,In progress", MOD_MAGZILLA,
				"Automatická zmena statusu ticketu pri vytvorení nového záznamu do kalendára z prostredia helpdesk v intranete");
		setBoolean("dragDropUploadEnabled", true, MOD_OBSOLETE,
				"Povolenia Drag & Drop nahrávania súborov z desktopu do WebJETu vo FireFoxe");
		setBoolean("googleDocsEnabled", false, "Googel Docs",
				"Ak je nastavene na true, spristupni sa upload a otvorenie prilohy v ticketoch cez Google Docs.");
		setString("googleDocsConsumerKey", "", "Googel Docs",
				"Kluc pre domenu, cez ktoru sa bude ku Google Docs pristupovat. Ak je prazdne, Google Docs v tiketoch nie je pristupny.");
		setString("googleDocsConsumerSecret", "", "Googel Docs",
				"Secret pre domenu, cez ktoru sa bude ku Google Docs pristupovat. Ak je prazdne, Google Docs v tiketoch nie je pristupny.");
		setString("mgzBugErrorStrings", "bug,chyba", MOD_MAGZILLA, " Vyhladavane texty pri nefakturovanom case");
		setBoolean("zmluvyApprovEditChanges", false, "zmluvy",
				"Pokial je nastavena hodnota na true, tak editovanu zmluvu bude treba opatovne schvalit na publikaciu. E-mail o zmene sa posle schvalovatelom ci je hodnota true alebo false.");
		setBoolean("portalStartup", false, "portal", "Spustanie portalu pri starte WJ");
		setString("AngularCDNVersion", "2.0.0-beta.0");
		setBoolean("sassCompilerEnabled", false, MOD_CONFIG,
				"nastavenim na true, sa zapne kompilacia sass a scss suborov");
		setString("AngularCDNVersion", "", "angular", "Verzia angular.io ktora sa ma vlozit do stranky");
		setString("emailAttachmentsPublisher.pop3.host", "", "emailAttachmentsPublisher", "adresa na pop3");
		setString("emailAttachmentsPublisher.pop3.user", "", "emailAttachmentsPublisher", "pouzivatel na pop3");
		setString("emailAttachmentsPublisher.pop3.password", "", "emailAttachmentsPublisher", "heslo na pop3");
		setBoolean("zmluvyEnableVo", false, "zmluvy",
				"Ak je nastavena na true, tak sa budu zobrazovat aj skupiny pre verejne obstaravanie.");
```

## 2026.18

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

- [Anketa snadno](redactor/apps/inquiry/inquiry-simple.md)

![](redactor/apps/inquiry/inquiry-simple-tab-basic.png)

- [Mapa](redactor/apps/map/README.md)

![](redactor/apps/map/map-editor.png)

- [Doporučení](redactor/apps/app-testimonials/README.md)

![](redactor/apps/app-testimonials/editor-style.png)

- [Předpřipravené bloky (HTMLBox)](redactor/apps/htmlbox/README.md)

![](redactor/apps/htmlbox/editor-block.png)

- Video - přidána možnost nastavit [CSS třídy pro poměr stran videa](redactor/apps/video/README.md#konfigurace), možnosti zobrazené v aplikaci se nastavují přes konfigurační proměnné `videoClasses`, `videoWrapperClass` a `videoItemClass` (#osk496).

### Formuláře

- Přidána karta [Statistika](redactor/apps/multistep-form/stat.md) pro zobrazení odpovědí formulářů ve formě grafů (#58333).

![](redactor/apps/multistep-form/stat-section.png)

- Pro vícekrokové formuláře přidán sloupec **Trvání vyplnění**, který zobrazuje jak dlouho trvalo vyplnění formuláře uživatelem (čas od jeho zobrazení po odeslání) (#58333).
- Vícekrokové formuláře - přidána podpora pro podmíněné zobrazení/validování položky formuláře na základě vytvořených podmínek. Více v části [Podmíněné zobrazení/validování položky](redactor/apps/multistep-form/README.md#podmíněné-zobrazenívalidování-položky) (#58477).

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

## 2026.0.x

> Opravná verze původní verze 2026.0.

- Webové stránky - opraveno ukládání web stránky s mezerou na konci URL adresy (provede se odstranění prázdných znaků) (#OSK650).
- Webové stránky - opravené zacyklení nepublikované stránky pokud URL nekončí na znak `/` - ​​konfigurační proměnná `virtualPathLastSlash=false` (#OSK684).
- Manažer dokumentů - přidáno smazání cache paměti po publikování nové verze souboru (#TB2754).
- Multiweb - opravena možnost smazat nebo upravit doménové přesměrování, které obsahuje `http/s` prefix (#58317-15).
- Galerie - v editoru aplikace se mezi vizuálními styly zobrazují pouze JSP soubory ze složek `/components/{INSTALL_NAME}/gallery` a `/components/gallery`, bez duplicitních položek (#58317-16).
- Vložení HTML kódu - v náhledu aplikace v editoru webových stránek se pro obsah tvořený pouze elementy `script` zobrazí zdrojový kód namísto prázdného obsahu (#OSK625).
- Bezpečnost - zpřísněné ověřování odkazu na obnovu zapomenutého hesla. Ověřovací záznam se kontroluje pro vybraný uživatelský účet i při vlastním způsobu odesílání, respektuje časovou platnost a po použití se zneplatní pro všechny účty zahrnuté v žádosti (#292).
- Bezpečnost - zpřísněné ověřování oprávnění při práci se záznamy v administraci (#295).
- Bezpečnost - zpřísněná kontrola práv na složku při nahrávání souboru do administrace a její přepsání pokud soubor existuje.
- Bezpečnost - zpřísněná kontrola oprávnění při obnově historické verze souboru (#295).
- Bezpečnost - zpřísněná kontrola oprávnění při správě blogerů (#295).
- Bezpečnost - zpřísněná validace názvů databázových sloupců při dynamickém uspořádání a filtrování. **Upozornění:** veřejné API již v parametrech uspořádání nepodporují vlastní SQL výrazy, používají se pouze bezpečné názvy sloupců nebo dostupné pojmenované konstanty (#294).
- Bezpečnost - [zabezpečený koncový bod `row-reorder`](developer/datatables/README.md#pořadí-uspořádání-řádků) datových tabulek. Povoleno je měnit pouze numerické pole označené `DataTableColumnType.ROW_REORDER`, přičemž se kontrolují oprávnění pro každý záznam i dodatečný rozsah celé dávky pomocí `checkRowReorderScope`. U formulářů se ověřuje příslušnost k formuláři a kroku i přístup uživatele; neplatný požadavek se neuloží (#295).
- CKEditor - doplněna možnost [konfigurovat pravidla čištění obsahu](frontend/setup/ckeditor.md#čištění-html-kódu-při-vložení-z-wordexcel) při vložení z Word/Excel. **Upozornění:** výchozí čištění nově odstraňuje i atribut `nowrap` z buněk `TD` a CSS třídy s atributy `align` a `valign` z buněk `TH` (#300).

## 2026.0.28

> Opravná verze původní verze 2026.0.

!> Upozornění: po aktualizaci zkontrolujte funkčnost generování `/thumb` obrázků a [nastavení povolených rozměrů](frontend/thumb-servlet/README.md#omezení).

- Web stránky - zrušeny [plánované verze](redactor/webpages/history.md) jsou v historii zobrazeny přeškrtnutým písmem a nelze je smazat (#58573).
- Archiv souborů - upravená úloha na pozadí pro publikování souborů - z důvodu práv se neprovádí na veřejném uzlu (#246).
- Úlohy na pozadí - přidána možnost spustit [úlohu na pozadí](admin/settings/cronjob/README.md) pouze na uzlech v plné konfiguraci nebo na veřejných uzlech (#246).
- Generátor primárních klíčů - doplněna automatická oprava jmen tabulek a názvů sloupce s primární hodnotou (#246).
- Bezpečnost - opravené chyby Local File Inclusion, kontrola nahrávaných souborů a RCE. Děkujeme Josef Korbel (Citadelo) za nahlášení těchto zranitelností (#252).
- Bezpečnost - přidána [kontrola povolených rozměrů](frontend/thumb-servlet/README.md#omezení) generování `/thumb` obrázků, první měsíc v režimu učení a následně po restartu WebJETu v režimu kontroly (#259).
- Bezpečnost - aktualizované knihovny `jackson-annotations` (2.22), `jackson-databind` (2.22.0), `logback-core/classic` (1.5.37) a `tink` (1.22.0). Z důvodu zpětné kompatibility v opravné verzi nebyla realizována migrace `Apache HttpClient` z 4.x na 5.x, ale WebJET se typicky připojuje na bezpečné/vnitřní API/domény (#264).
- Aktualizované přemosťovací knihovny `slf4j-api`, `jcl-over-slf4j` a `log4j-over-slf4j` na verzi `2.0.18`. Zároveň jsou v `dependency-check` potlačeny falešně hlášené zranitelnosti `CVE-2020-9493` a `CVE-2022-23307`, které se týkají původního `Apache log4j 1.2.x`, ne přemostění `log4j-over-slf4j` (#264).
- Aktualizovaná knihovna `swagger-ui` na verzi 5.32.8 (#264).
- JPA - opraveno vícenásobné ukončení databázové transakce při importu přesměrování (#256).
- `/thumb` - ​​opraveno generování obrázku u `ip=1/2` a nulové velikosti `w/h` parametru (#58317-10).
- Hromadné úpravy - upravené hledání/nahrazení více řádkového HTML kódu v `replaceall` skriptu (#262).
- Cluster - upravené nastavení CSRF tokenů v `session` tak, aby se změna distribuovala i v `redis` (#265).

## 2026.0.25

> Opravná verze původní verze 2026.0.

!> Upozornění: po aktualizaci zkontrolujte funkčnost všech formulářů. Pokud některý nelze odeslat, uložte znovu jeho nastavení.

- AI asistent - upravené získání odpovědi při použití `reasoning` v OpenAI (#244).
- AI asistent - doplněná propagace smazání cache do uzlů clusteru při úpravě asistenta.
- Apache Tomcat - ve verzi `9.0.118/11.0.22` bylo změněno chování získání seznamu Java tříd což má za následek nefunkčnost `Stripes Framework`. Upravená verze filtruje nesprávné verze souborů aby start proběhl korektně. Do staršího projektu můžete přenést přímo třídu [VFS.java](https://github.com/webjetcms/webjetcms/blob/main/src/main/java/net/sourceforge/stripes/vfs/VFS.java), zkompilovat ji ve vašem projektu a použít bez potřeby aktualizace.
- Bezpečnost - opravena možnost nastavit [jméno HTTP hlavičky pro získání IP adresy](sysadmin/pentests/README.md#konfigurace) přes proměnnou `xForwardedForHeader`.
- Bezpečnost - opravené chyby Local File Inclusion, kontrola nahrávaných souborů a RCE. Děkujeme Josef Korbel (Citadelo) za nahlášení těchto zranitelností (#252). Možné dočasné řešení bez aktualizace celého WebJET CMS je:
  - smazat nebo [aktualizovat](https://github.com/webjetcms/webjetcms/blob/main/src/main/webapp/components/grideditor/phantom/phantom_sablona_ajax.jsp) soubor `/components/grideditor/phantom/phantom_sablona_ajax.jsp`
  - pokud máte WebJET CMS novější než `2025.52` do konfigurační proměnné `pathFilterBlockedPaths` přidat hodnotu `,/components/grideditor/phantom/`
  - pokud máte WebJET CMS novější než `2025.52` můžete [globálně pro celý server](install/external-configuration.md) do `JAVA_OPTS` přidat systémovou proměnnou:

```txt
-Dwebjet.pathFilterBlockedPaths=.DS_Store,debug.,config.properties,Thumbs.db,.git,.svn,/WEB-INF/,./,/components/grideditor/phantom/
```

- Bezpečnost - přidán CSRF token pro formulář při vypnuté SPAM ochraně (#245).
- Bezpečnost - každé odeslání formuláře kontroluje nastavení formuláře v databázi, pokud se nenajde formulář nelze odeslat. Původní verze toto kontrolovala pouze na veřejných uzlech clusteru, nyní se to kontroluje bez ohledu na cluster. V případě potřeby vypnete funkčnost nastavením konfigurační proměnné `formAllowOnlyExistingFormsOnPublicNode` na hodnotu `false` (#245).
- Bezpečnost - opravena možnost získání administrátorského účtu při generování offline verze (#245).
- Formulář snadno - upravená pole pro zadání názvu pole a tooltipu na jednořádkový WYSIWYG editor, aby výsledek neobsahoval `P` element (#244).
- Galerie - opraveno vytvoření záznamu v databázi při kopírování souborů v průzkumníku. Záznam se nevytvoří pro `o_` a `s_` obrázky (#58317-9).
- Manažer dokumentů - doplněné filtrování souborů podle dat platnosti (není-li platný rozsah dat, nezobrazí se) a souborů které nemají nastavený atribut zobrazovat (#233).
- Video - aktualizovaná knihovna `videojs` pro přehrávání lokálních audio/video souborů z verze 6.2.0 na verzi 8.23.6 (#233).
- Video - opraveno nastavení odkazu na lokální audio/video soubor při editaci již vložené aplikace (#233).
- Výkon - přidán index v tabulce `emails` podle `click_hash` pro lepší výkon v Oracle databázi (#244).
- Web stránky - doplněné zobrazení `mp3` souborů ve výběru Média všech stránek/Videa (#233).
- Web stránky - povoleno vkládání souborů typu `svg,webp,mp3` pokud uživatel nemá povoleno právo Kompletní menu v editoru, hodnota nastavená v konfigurační proměnné `FCKConfig.UploadFileTypes[Basic][image]` (#233).

## 2026.0.18

> Opravná verze původní verze 2026.0.

- Administrace - doplněné přesměrování po přihlášení pokud původní cesta začíná na `/components` a obsahuje `/admin` (#58317-4).
- Aktualizace WebJETu - opravené odkazy na dokumentaci.
- Bannerový Systém - opraveno načtení seznamu skupin v Microsoft SQL.
- Bezpečnost - přidána možnost [deaktivovat aplikaci](sysadmin/pentests/README.md#deaktivace-aplikace), aby nebyla dostupná. Umíte tak vypnout aplikace jako Zálohování systému, Restartovat a podobně, pokud nasazujete přes `CI-CD` službu a dané aplikace nemají využití, nebo nejsou žádoucí z bezpečnostních důvodů.
- Bezpečnost - aktualizován `Swagger UI` a výjimky pro `dependencyCheckAnalyze` (#58317-6).
- Bezpečnost - aktualizované knihovny `log4j,pdfbox,thymeleaf,postgresql` (#58317-6,#226).
- Bezpečnost - přidána ochrana před brute force útoky na 2FA tokeny. Při neúspěšných pokusech se IP adresa dočasně zablokuje - stejně jako při přihlašování heslem (#222).
- Bezpečnost - generované PDF soubory více neobsahují v meta datech `Creator/Producer` informace o generátoru `PD4ML`. Hodnota se stahuje z konfigurační proměnné `pdfAuthorName`. Čištění metadat je řízeno proměnnou `metadataCleanFiles` (výchozí hodnota `pdf-gen`) (#222).
- Bezpečnost - opraveno zpracování chyby prázdného hesla (#222).
- Datové tabulky - opravena možnost zavření editoru ve vnořeném modálním okně (#OSK303).
- Dotazníky - opraveno ukládání dotazníku při použití databáze Oracle nebo Microsoft SQL (#217).
- Galerie - opraveno uložení nastavení galerie pro složku na disku (bez záznamu v databázi) v Oracle DB.
- Galerie - přidána konfigurační proměnná `metadataRemoveMinFileSize` pro nastavení minimální velikosti souboru v bajtech, pod kterou se přeskočí odstraňování metadat (#osk378).
- GDPR - mazání dat - opraveno mazání formulářů v Oracle/PostgreSQL databázi (#224).
- Hromadný email - opraven přenos příjemců při duplikování kampaně v Oracle DB (#54273-82).
- Hromadný email - opraveno nahrazení externích odkazů, které obsahují více URL parametry v emailu (#54273-83).
- Média - opravena kontrola oprávnění při přidávání médií do neuložené web stránky uživatelům bez práva na všechna média (#58317-6).
- Manažer dokumentů - opraveno smazání indexu souboru při uložení, pokud soubor již není platný. Opraveno iniciální SQL pro nastavení indexování souborů pokud se soubor nemá zobrazovat (#227).
- Multiweb - opraveno nastavení domény po přihlášení (#58317-03).
- Multiweb - opraveno používání doménových aliasů při použití externích složek - nastavena konfigurační proměnná `cloudStaticFilesDir` (#58317-4).
- Překladové klíče - opraveno nastavení prázdných hodnot při vytvoření nového záznamu a odsazení polí s původní hodnotou (#56845).
- Průzkumník - opraveno nahrávání více souborů přes drag & drop (#58317-2).
- Volitelná pole - opraveno zobrazení hodnot (namísto ID) a filtru (výběrové pole namísto textového pole) pro výběrová pole, číselníky a výběr web stránky (#58421-0).
- Webové stránky - opraveno odstranění diakritiky při nahrání obrázku do editoru přes drag&drop (#58361).
- Webové stránky - Page Builder - při vytvoření nové stránky se použije hodnota nastavená v poli HTML kód nové stránky složky. Stránka tedy nemusí být prázdná, ale může obsahovat připravené bloky (#osk378).
- Webové stránky - Nastavení bloku - opraveno nastavení pozadí a pokročilých nastavení (#TB2456).
- Webové stránky - opraveno smazání složky z koše při multiweb instalaci (#58317-03).
- Webové stránky - sladěné získání seznamu šablon mezi web stránkami a složkami (#58317-03).
- Webové stránky - opraven přenos dat publikování při náhledu web stránky a přesměrování při vlastnostech bloku (#osk412).
- Webové stránky - opravena chyba získání [šablony pro mobilní zařízení](frontend/templates/templates.md#zobrazení-pro-specifické-zařízení) v MultiWEB instalaci při shodě jmen šablony v různých doménách (#58317-5).
- Webové stránky - opraveno ukládání stránky, která má kopie ve více složkách a zároveň je použito zrcadlení stránek (#58317-7).
- Webové stránky - Ninja - doplněný atribut `${ninja.temp.lngIsoUnderscore}` s kódem jazykové mutace ve formátu `sk_SK` místo `sk-SK` (#217).
- Webové stránky - opravená uzavírací značka `</link>`, správně nahrazená za `/>`, protože `link` je prázdný element (#osk498).
- Webové stránky - opravena chyba odstranění časové složky při nastavení data konání (#54273-89).
- Webové stránky - přidána možnost [nastavit JavaScript funkci](frontend/setup/config.md) pro `target="_blank"` odkazy, ve výchozím nastavení na `return openTargetBlank(this, event)`. Hodnota se nastavuje v konfigurační proměnné `editorTargetBlankFunction`, pokud je nastaveno na prázdnou hodnotu `onclick` funkce se nenastaví (#225).
- Hromadný email - opraveno vytváření kampaní a emailů při duplikování kampaně (#58649).

Jakarta verze:

- Aktualizovaná knihovna `Spring Security` z verze 6 na verzi 7 (#43144).

## 2026.0

> **WebJET CMS 2026.0** přináší vylepšenou verzi nástroje **Page Builder** pro tvorbu **komplexních web stránek**. V blocích lze **vyhledávat a filtrovat** na základě značek, snadno tak najdete vhodný blok pro vložení do stránky. Přidány byly nové funkce jako **rozdělení sloupce**, **vkládání více sekcí najednou** a **stále zobrazené tlačítko pro přidání nové sekce** pro rychlé rozšíření obsahu stránky.
>
> Podpora **PICTURE elementu** umožňuje zobrazovat **různé obrázky podle rozlišení obrazovky** návštěvníka, čímž se zlepšuje vizuální zážitek na různých zařízeních. Navíc lze vkládat **vlastní ikony** definované ve společném SVG souboru, což přináší větší flexibilitu v designu.
>
> Nový nástroj pro **tvorbu formulářů** umožňuje snadno vytvářet **víceleté formuláře** s možností programové validace jednotlivých kroků a možností **potvrzení platnosti emailové adresy** pomocí zaslaného kódu. Vyhnete se tak vyplnění formulářů různými roboty.

!> Upozornění: verze **2026.0** je poslední verze pro aplikační server Tomcat 9 s využitím `javax namespace`. Všechny novější verze od `2026.18` budou určeny **už jen pro** aplikační server **Tomcat 10/11** s využitím `Jakarta namespace`. Změna z `javax` na `jakarta namespace` je důsledkem předání `Java EE` specifikací z Oracle do `Open Source/Eclipse Foundation`, kde projekt pokračuje pod názvem `Jakarta EE`. Tato změna vyžaduje aktualizaci aplikačního serveru, protože verze Tomcat 10 a novější již používají výhradně `jakarta namespace` pro všechny `Java EE` API. Aktuální seznam dostupných verzí naleznete v [sekci instalace](install/versions.md).

### Průlomové změny

- Aktualizované knihovny `commons-lang,displaytag`, více v [sekci pro programátora](#pro-programátora) (#58153).
- Změněné chování ikony Bloky v režimu Page Builder - [textové bloky integrované](frontend/page-builder/blocks.md) do složky `content` podobně jako jsou bloky pro `section, container, column` (#58165).
- Upravené zpracování **nahrávání souborů** `multipart/form-data`, více v [sekci pro programátora](#pro-programátora) (#57793-3).
- Doporučujeme **zkontrolovat funkčnost všech formulářů** z důvodu úprav jejich zpracování, více informací v sekci [pro programátora](#pre-programátora) (#58161).

### Webové stránky

- Přidána možnost vkládat `PICTURE` element, který zobrazuje [obrázek podle rozlišení obrazovky](frontend/setup/ckeditor.md#picture-element) návštěvníka. Můžete tedy zobrazit rozdílné obrázky na mobilním telefonu, tabletu nebo počítači (#58141).

![](frontend/setup/picture-element.png)

- Přidána možnost vkládat [vlastní ikony](frontend/setup/ckeditor.md#svg-ikony) definované ve společném SVG souboru (#58181).

![](frontend/setup/svgicon.png)

- Přidán přenos aktuálního HTML kódu při přepnutí režimu editoru Standardní/HTML/Page Builder. Můžete tak jednoduše upravit Page Builder stránku v HTML kódu a znovu zobrazit úpravy v režimu Page Builder (#58145).
- Přidáno kontextové menu Smazat element, pomocí kterého můžete snadno smazat tlačítko, odkaz, odstavec, formulář, sekci a podobně. Stačí když na element kliknete pravým tlačítkem pro zobrazení kontextového menu (#osk233).
- Page Builder - upravené generování stylů při použití nástroje tužka. Do CSS stylu se generují jen změněné hodnoty, ty jsou v dialogovém okně zvýrazněny modrým orámováním vstupního pole (#58145).
- Page Builder - přidána možnost volání [vlastního JavaScript souboru](frontend/page-builder/blocks.md#podporný-javascript-kód) s podpůrnými funkcemi pro úpravu kódu. Přidána možnost upravit nastavení jako selektory pro elementy, barvy a podobně (#58141).
- Page Builder - upravené generování kotev u kart tak, aby název kotvy byl generován podle názvu karty - původně nebyl generován sémanticky jako `autotabs-x-y` (#112).
- Page Builder - doplněna možnost nastavit šířku sloupce na `auto` pro automatické přizpůsobení obsahu (#114).
- Page Builder - doplněna možnost připravit [textové bloky](frontend/page-builder/blocks.md) přímo do složky `content`, vkládají se namísto původních bloků čtených z web stránek ze složky Šablony. Web designér je připraví spolu s ostatními typy Page Builder bloků. Umožňuje rychlé vložení často používaných textových částí, tlačítek a podobně (#58165).
- Page Builder - při vkládání nového bloku je výchozí karta Knihovna namísto Základní, aby se zjednodušil výběr bloku z připraveného seznamu (#58165).
- Page Builder - doplněna možnost rozdělit sloupec na dvě části pomocí nové funkce Rozdělit sloupec. Vyvoláte ji pomocí kliknutí na + ve žluté liště, zvolením možnosti Blok a následně v kartě Základní zvolíte možnost Rozdělit sloupec. Funkce umožňuje rychlé rozdělení sloupce bez nutnosti vkládat nový sloupec a přesouvat obsah (#58165).
- Page Builder - doplněna možnost vložit blok, který obsahuje více sekcí nebo jiných elementů - označí se po vložení všechny sekce (#58173).
- Page Builder - doplněno [ID bloku](frontend/page-builder/blocks.md#id-bloku) do atributu `data-pb-id` pro možnost vyhledání použití bloku ve web stránkách přes vyhledávání v administraci (#58193).
- Page Builder - seznam oblíbených bloků je ukládán pro každého uživatele zvlášť, aby si každý mohl spravovat vlastní seznam oblíbených bloků (#58193).
- Page Builder - přidána stále zobrazená ikona pro přidání nové sekce na konci stránky, což zjednodušuje přidávání nových sekcí do stránky (#58173).

![](redactor/webpages/pagebuilder-plusbutton.png)

- Page Builder - upravený design nástrojové lišty pro lepší viditelnost na různých pozadích (#58165).

![](redactor/webpages/pagebuilder.png)

- Page Builder - doplněna možnost [filtrovat bloky](frontend/page-builder/blocks.md#název-a-značky-bloku) podle názvu a štítků (#58173).

![](redactor/webpages/pagebuilder-library.png)

- Doplněna [detekce změny obsahu](redactor/webpages/working-in-editor/README.md#detekce-změny-obsahu-stránky) a upozornění na neuložené změny při zavírání okna prohlížeče. Změny se začnou detekovat 5 vteřin po otevření web stránky. (#112).
- Doplněna možnost nastavit výchozí hodnoty pro tabulky v CKEditoru přes konfigurační proměnné, více v [sekci nastavení CKEditoru](frontend/setup/ckeditor.md#konfigurační-proměnné) (#58189).
- Doplněna možnost vkládat [tlačítko](frontend/setup/ckeditor.md#tlačítko) - element `button`. Umíte tak snadno vkládat různá akční `call to action` tlačítka (#58201).
- Styl - [výběr stylu](frontend/examples/template-bare/README.md#seznam-stylů-pre-editor) definovaného pro element Např. `p.paragraph-green,p.paragraph-red-border,p.paragraph-yellow-background` nebo `section.test-section,section.test-section-green` umožňuje nastavit více stylů současně. Opakovaným zvolením již nastaveného stylu se tento styl odstraní (#OSK140).
- Upravený text pro publikování stránky do budoucna na **Naplánovat změnu stránky po tomto datu**, při zvolení této možnosti se také změní tlačítko pro uložení na text **Naplánovat** pro jasnější informaci pro uživatele (#58253).
- Do žádosti o schválení web stránky doplněn seznam změněných polí (#58077).

![](redactor/webpages/approve/approve-form.png)

### Aplikace

Předěláno nastavení vlastností aplikací v editoru ze starého kódu v `JSP` na `Spring` aplikace. Aplikace automaticky získávají také možnost nastavit [zobrazení na zařízeních](custom-apps/appstore/README.md#podmíněné-zobrazení-aplikace). Design je ve shodě se zbytkem WebJET CMS a datových tabulek (#58073).

- [Novinky](redactor/apps/news/README.md)

![](redactor/apps/news/editor-dialog.png)

- [Formulář snadno](redactor/apps/formsimple/README.md)

![](redactor/apps/formsimple/editor-dialog-items.png)

### Formuláře

- Nový způsob vytváření formulářů, které mohou obsahovat [více kroků](redactor/apps/multistep-form/README.md) s pokročilými funkcemi. V seznamu formulářů umíte vytvořit nový formulář, kterému následně přidáte jednotlivé položky a případně několik kroků. Karta položky formuláře je viditelná v detailu formuláře typu Vícekrokový formulář (#58161).

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/XRnwipQ-mH4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

- Seznam formulářů - celá sekce byla předělána z technologie `Vue.js` na standardní `Html + JavaScript` pro lepší integraci do WebJET CMS a zjednodušení úprav (#58161).
- Seznam formulářů - umožněno vytváření formuláře, který je automaticky typu [víceletý formulář](redactor/apps/multistep-form/README.md) (#58161).
- Seznam formulářů - umožněno nastavování parametrů/atributů všech typů formulářů přímo v editoru formuláře (#58161).
- Seznam formulářů - pole poznámka umožňuje vkládat formátovaný text, umíte tak lépe evidovat doplňkové informace k formuláři (#58161).
- Detail formuláře - přidána možnost zobrazení všech údajů přihlášeného uživatele, data se také exportují do Excelu (#58161).
- Ověřovací kód - přidána možnost odeslat formulář až po zadání [ověřovacího kódu](redactor/apps/form/README.md#nastavení-potvrzení-zaslaným-kódem) zaslaného na email adresu. Umíte tak lépe chránit formuláře před SPAM-em (#58161).

![](redactor/apps/form/form-step-email-verification-2.png)

### Přesměrování

- Přidáno možnost ukončit platnost přesměrování ve stanoveném čase a možnost zadat poznámku s informací k čemu přesměrování slouží. Přesměrování, která již nejsou časově platná, se zobrazí červeně (#58105).

![](redactor/webpages/redirects/path-editor.png)

### Elektronický obchod

- Nová sekce [Způsoby doručení](redactor/apps/eshop/delivery-methods/README.md), jako samostatná tabulka nahrazuje původní konfiguraci dostupných způsob doručení, která se nacházela přímo v nastavení aplikace **elektronického obchodu**. Pro každý způsob doručení lze nastavit také cenu, která při zvolení možnosti bude automaticky přičtena k objednávce. Nastavené způsoby doručení se také automaticky promítnou do možností při vytváření objednávky zákazníkem. Připraveno je doručení poštou a osobní vyzvednutí, do budoucna plánujeme doplnit integraci na doručovací společnosti (#58061).

![](redactor/apps/eshop/delivery-methods/datatable.png)

### Bezpečnost

- Přidána podpora pro povolení pouze **jednoho aktivního přihlášení** na jednoho uživatele. Režim zapnete nastavením konfigurační proměnné `sessionSingleLogon` na hodnotu `true`. Při novém přihlášení se zruší předchozí aktivní `session` (#58121).
- Odstraněna nepodporovaná knihovna [commons-lang](https://mvnrepository.com/artifact/commons-lang/commons-lang), nahrazená novou knihovnou [commons-lang3](https://mvnrepository.com/artifact/org.apache.commons/common skript pro úpravu zdrojových kódů (#58153).
- Přidán seznam [Moje aktivní přihlášení](redactor/admin/welcome.md#moje-aktivní-přihlášení) na úvodní obrazovce administrace, která zobrazuje všechna aktivní přihlášení do administrace pod vaším uživatelským účtem a možnost jejich ukončení. Přidána i možnost odeslat email přihlášenému administrátorovi (#58125).

![](redactor/admin/sessions.png)

- Captcha - nastavením konfigurační proměnné `captchaType` na hodnotu `none` lze Captcha zcela vypnout. Nezobrazí se iv případě, pokud má šablona zobrazené web stránky vypnutou SPAM ochranu. V takovém případě je ale třeba korektně kontrolovat vypnutí SPAM ochrany šablony i v případném kódu zpracování/verifikace Captcha odpovědi, pro formuláře je tato kontrola zajištěna. Můžete použít volání `Captcha.isRequired(component, request)` pro ověření režimu a vypnutí spam ochrany (#54273-78).
- Aktualizovaná knihovna pro [odesílání emailů](install/config/README.md#odesílání-emailů) z `com.sun.mail:javax.mail:1.6.2` na `com.sun.mail:jakarta.mail:1.6.8` z důvodu podpory nových autentifikačních mechanismů SMTP serverů jako například `NTLMv2` a přidána konfigurační proměnná `smtpAuthMechanism` pro vy. na hodnotu `NTLM` pro vynucení `NTLM` autorizace namísto použití `BASIC` autorizace (#58153).
- Upraveno logování výjimek při přerušení HTTP spojení (např. při zavření prohlížeče, odchodu na jinou web stránku a podobně). Takové výjimky se nezapíší do logu, aby nenastala chyba obsazení místa. Týká se výjimek typu `IOExceptio` a názvů výjimek definovaných přes konfigurační proměnnou `clientAbortMessages`, ve výchozím nastavení `response already,connection reset by peer,broken pipe,socket write error` (#58153).

### Jiné menší změny

- Vyhledávání - upravené načtení seznamu šablon při hledání web stránek. Načtou se všechny šablony bez ohledu na jejich dostupnost ve složkách, aby se nestalo, že při editaci web stránky šablona není dostupná (#58073).
- HTTP hlavičky - přidána možnost nastavit hlavičku delší než 255 znaků, například pro nastavení `Content-Security-Policy` ([#82](https://github.com/webjetcms/webjetcms/issues/82))

![](admin/settings/response-header/editor.png)

- Konfigurace - upravený způsob smazání konfigurační proměnné. Po vymazání se automaticky nastaví původní hodnota z `Constants`, aby byla stejná jako bude po restartu serveru. V původním řešení se proměnná jen smazala, ale její hodnota zůstala interně nastavena do restartu serveru (#57849).
- Konfigurace - přidána možnost nastavit [jméno HTTP hlavičky](sysadmin/pentests/README.md#konfigurace) pro získání IP adresy návštěvníka přes konfigurační proměnnou `xForwardedForHeader` (#58237).
- Bezpečnost - přidána možnost konfigurace blokovaných cest souborů/adresářů přes proměnnou `pathFilterBlockedPaths`. Standardně jsou blokovány URL adresy, které v názvu obsahují výraz `.DS_Store,debug.,config.properties,Thumbs.db,.git,.svn`. Lze přidat další podle potřeby (#PR103).
- Značky - upravené zobrazené značek, v případě duplicity hodnot. Porovnání je bez vlivu diakritiky a velkých/malých písmen [#115](https://github.com/webjetcms/webjetcms/issues/115).

![](redactor/webpages/perex-duplicity-values.png)

- Zrcadlení - přidána možnost zobrazit obrázek vlajky namísto textu v [přepínači jazyka stránky](redactor/apps/docmirroring/README.md#vytvoření-odkazu-na-jazykové-mutace-v-hlavičce-stránky) (#54273-79).
- Změna hesla - přidána možnost nastavit jméno a email adresu ze které je odeslán email s odkazem na změnu hesla přes konfigurační proměnné `passwordResetDefaultSenderEmail` a `passwordResetDefaultSenderName` (#58125).
- Statistika - doplněné sumární počty vidění a návštěv v TOP stránkách (#PR136).
- Novinky - přejmenovaná hodnota uspořádat podle priority na uspořádat podle Pořadí uspořádání (priority) pro sladění s hodnotou v editoru (#57667-16).
- Formulář snadno - přidána možnost nastavit hodnotu `useFormDocId` pro vložení formuláře např. do patičky stránky (#57667-16).
- Novinky / Šablony novinek - přesunuté pole `contextClasses` z aplikace Novinky do Šablony novinek, aby se vlastnost nastavovala přímo v šabloně novinek. Původní hodnota `contextClasses` z Novinek bude nadále fungovat, ale nelze jej již nastavovat v uživatelském rozhraní (#58245).
- Manažer dokumentů - přidána možnost [editovat metadata historické verze dokumentu](redactor/files/file-archive/README.md#úprava-historické-verze-dokumentu-v-manažeru) v manažeru dokumentů (#58241).
- Hromadný email - upraveno auditování změn v kampani. Pokud se přidá skupina neaudituje se celý seznam příjemců (bylo to zbytečně mnoho záznamů v auditu), zapíše se pouze seznam změněných skupin. Při manuálním přidání emailů se nadále audituje jméno i emailová adresa (#58249).
- Uživatelé - při importu pokud sloupec v Excelu neobsahuje pole heslo, tak se pro nové uživatele vygeneruje náhodné heslo. Pokud není v Excelu zadán stav Schválený uživatel, tak se nastaví na hodnotu `true` (#58253).
- MultiWeb - doplněno zobrazení domény v boční liště (#58317-0).
- MultiWeb - doplněna možnost nastavit doménu přesměrování aby bylo možné zadat `https://` prefix (#58317-0).
- MultiWeb - doplněna kontrola práv pro skupiny médií a značky (#58317-0).
- Seznam formulářů - nastavení [zpracovatele formulářů](custom-apps/apps/multistep-forms/README.md), pomocí autocomplete pole, který nabízí třídy implementující `FormProcessorInterface` (#58313).
- Číselníky - doplněno odstranění mezer na začátku a konci pole typu řetězec v datech číselníku (#OSK233).

### Oprava chyb

- Značky - opravené duplikování složky v Zobrazit pro při uložení značky, odstraněn výběr složky z ostatních domén, protože značky jsou již odděleny podle domén (#58121).
- Web stránky - opraveno vkládání tvrdé mezery za spojky tak, aby se aplikovalo pouze na text stránky a nikoli na atributy nebo HTML značky (#OSK235).
- Datatables - opravené zpracování události `Enter` u vybraných vstupních polí filtrů tabulky (#58313).
- Datatables - opravené filtrování kdy se více `serverSide:false` tabulek na stránce navzájem ovlivňovalo při filtrování (#58313).
- Elektronický obchod - opraveno odesílání email notifikace, při změně stavu objednávky (#58313).
- Elektronický obchod - opraveno automatické nastavení stavu objednávky po změně plateb (#58313).

### Dokumentace

- Aktualizovány všechny fotky obrazovky v české verzi dokumentace (#58113).

### Pro programátora

- Volná pole - přidána možnost specifikovat vlastní sloupce pro label a hodnotu při [propojení na číselník](frontend/webpages/customfields/README.md#číselník). Umožňuje flexibilnější nastavení, která vlastnost z číselníku se použije jako zobrazený text a která jako uložená hodnota (#PR108).
- Smazané nepoužívané soubory `/admin/spec/gallery_editor_perex_group.jsp,/admin/spec/perex_group.jsp`, pokud je ve vašem projektu používáte vezměte je ze [starší verze](https://github.com/webjetcms/webjetcms/tree/release/2025.40/src/main/webapp/admin/spec) WebJET CMS (#58073
- Mírně upravené API v [NewsActionBean](../../src/main/java/sk/iway/iwcm/components/news/NewsActionBean.java), hlavně nastavení `groupIds` které jsou nyní typu `List<GroupDetails>`. Můžete použít `setGroupIds(int[] groupIds)` pro nastavení s polem ID hodnot (#58073).
- Opravena možnost vkládání uvozovek do parametrů aplikací (#58117).
- Připravené kontejnery pro všechny podporované databázové servery ve WebJET CMS pro snadné spuštění ve VS Code. Nacházejí se ve složce `.devcontainer/db` (#58137).
- Elektronický obchod - kvůli změnám při procesu implementace **způsobů doručení** je třeba provést úpravu souboru pomocí aktualizačního skriptu `update-2023-18.jsp` a to nad sekcí `basket` (#58061).
- Elektronický obchod - přejmenovaná anotace `@PaymentMethod` na `@FieldsConfig` a `@PaymentFieldMapAttr` na `@FieldMapAttr` pro sjednocení anotací mezi platbami a způsoby doručení (#58061).
- Elektronický obchod - při procesu implementace **způsobů doručení** do souboru `order_form.jsp` přibylo několik změn, které si musíte implementovat manuálně. Tyto změny jsou příliš komplexní, aby se daly doplnit pomocí aktualizačního skriptu `update-2023-18.jsp` (#58061).
- Navigační lišta - přidána možnost použít vlastní implementaci generátoru [navigační lišty](redactor/apps/navbar/README.md). Přes konfigurační proměnnou `navbarDefaultType` lze nastavit jméno třídy implementující `NavbarInterface` (#PR101).
- Odstraněna nepodporovaná knihovna [commons-lang](https://mvnrepository.com/artifact/commons-lang/commons-lang), nahrazená novou knihovnou [commons-lang3](https://mvnrepository.com/artifact/org.apache.commons/common skript pro úpravu zdrojových kódů (#58153).
- Aktualizovaná knihovna [displaytag](https://mvnrepository.com/artifact/com.github.hazendaz/displaytag) na verzi `2.9.0` (#58153).
- Upraveno zpracování nahrávání souborů `multipart/form-data`. Ve Spring aplikacích pro souborové pole použijte místo `org.apache.commons.fileupload.FileItem` přímo `org.springframework.web.multipart.MultipartFile`, které bude automaticky nastaveno. Není již potřeba používat volání typu `entity.setDocument(MultipartWrapper.getFileStoredInRequest("document", request))` pro získání souboru. **Upozornění:** je třeba nahradit všechny výskyty `CommonsMultipartFile` za `MultipartFile` ve vašem kódu, také zrušit URL parametry ve Spring aplikaci pro vynucené zpracování. Výraz `data-th-action="@{${request.getAttribute('ninja').page.urlPath}(\_\_forceParse=1,\_\_setf=1)}"` nahraďte za `data-th-action="${request.getAttribute('ninja').page.urlPath}"`. Můžete použít `/admin/update/update-2023-18.jsp` k aktualizaci souborů (#57793-3).
- Doplněna možnost vytvoření [projektových kopií souborů](frontend/customize-apps/README.md) Spring aplikaci. Stačí vytvořit vlastní verzi souboru ve složce `/apps/INSTALL_NAME/` podobně jako se používá pro JSP soubory. WebJET CMS nejprve hledá soubor v projektové složce a pokud není nalezen použije standardní soubor z `/apps/` složky (#58073).
- Doplněna možnost nastavit [jméno pro CSS styl](frontend/examples/template-bare/README.md) v CSS souboru přes komentář `/* editor title: Style Name */`. Jméno se zobrazí v seznamu stylů v editoru (#58209).
- Editor - upravený dialog pro nastavení `a.btn` - ​​zrušeno nastavení barev a velikostí, [používají se už jen CSS třídy](frontend/setup/ckeditor.md#tlačítko) stejně jako pro `button` (#57657-16).
- Datové tabulky - možnost zobrazení pouze ikony bez pořadí pro `rowReorder` ak danému sloupci přidáme třídu `icon-only` (#58161).
- Datové tabulky - nové možnosti pro výběr řádků v tabulce `toggleSelector` a `toggleStyle`, více v [sekci datových tabulek](developer/datatables/README.md#možnosti-konfigurace) (#58161).
- Datové tabulky - nová možnost vlastní [render](developer/datatables-editor/datatable-columns.md) funkce pomocí anotace `@DataTableColumn(...renderFunction = "renderStepName")`. Umožní vám zobrazit ve sloupci složené hodnoty z více polí a podobně (#58161).
- Datové tabulky - přidána možnost [přesměrovat uživatele](developer/datatables/restcontroller.md#přesměrování-po-uložení) na jinou stránku po uložení záznamu voláním metody `setRedirect(String redirect)` (#58161).
- Formuláře - Upravené zobrazení seznamu formulářů, zrušená třída `FormAttributesDB`, nahrazená třídou `FormService`. Nastavení formulářů změněno z tabulky `form_attributes` na tabulku `form_settings`. Doporučujeme po aktualizaci ověřit funkčnost všech formulářů na web stránce (#58161).
- Formuláře - vytvoření nové tabulky `form_settings` jako náhradu za tabulku `form_attributes`, kde se ukládají vlastnosti formulářů. Jednotlivé atributy (nastavení) jsou nyní uloženy v samostatných sloupcích jako jeden záznam na řádek. Data byla do nové tabulky konvertována pomocí `UpdateDatabase.java` (#58161).
- Přechod na novou tabulku `form_settings` pro vlastnosti formulářů v `.jsp` souborech. Je třeba spustit aktualizační skript `update-2025-0.jsp`, který upraví potřebné `.jsp` (#58161).
- Seznam formulářů - nastavování parametrů/atributů všech typů formulářů přesměrováno z tabulky `form_attributes` do nové tabulky `form_settings` (#58161).
- Datové tabulky - přidána BE podpora pro `row-reorder`, kdy je možné měnit pořadí záznamů přímo v datové tabulce pomocí drag&drop (#58161).
- Události - přidána událost [Aktualizace kódů v textu](developer/backend/events.md#aktualizace-kódů-v-textu) pro možnost úprav kódů v textu stránky typu `!CUSTOM_CODE!` a podobně (#54273-63).
- Datové tabulky - přidáno [Spring události](developer/backend/events-datatable.md) pro možnost úprav dat v zákaznických instalacích (#54273-63).

### Testování

- Doplněný skript [rm-same-images.sh](../../src/test/webapp/rm-same-images.sh) pro odstranění stejných obrázků při pořízení nových snímků obrazovky (#58113).

![meme](_media/meme/2026-0.jpg ":no-zoom")
