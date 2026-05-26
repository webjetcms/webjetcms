# Seznam změn verze 2025

## 2025.52/SNAPSHOT

> **WebJET CMS 2025.52/SNAPSHOT** přináší vylepšenou verzi nástroje **Page Builder** pro tvorbu **komplexních web stránek**. V blocích lze **vyhledávat a filtrovat** na základě značek, snadno tak najdete vhodný blok pro vložení do stránky. Přidány byly nové funkce jako **rozdělení sloupce**, **vkládání více sekcí najednou** a **stále zobrazené tlačítko pro přidání nové sekce** pro rychlé rozšíření obsahu stránky.
>
> Podpora **PICTURE elementu** umožňuje zobrazovat **různé obrázky podle rozlišení obrazovky** návštěvníka, čímž se zlepšuje vizuální zážitek na různých zařízeních. Navíc lze vkládat **vlastní ikony** definované ve společném SVG souboru, což přináší větší flexibilitu v designu.
>
> Nový nástroj pro **tvorbu formulářů** umožňuje snadno vytvářet **víceleté formuláře** s možností programové validace jednotlivých kroků a možností **potvrzení platnosti emailové adresy** pomocí zaslaného kódu. Vyhnete se tak vyplnění formulářů různými roboty.

!> Upozornění: verze `2025.52` je technicky shodná s verzí `2026.0` a může být plnohodnotně zaměněna.

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
- Page Builder - upravené generování kotev u karet tak, aby název kotvy byl generován podle názvu karty - původně byl generován nesémanticky jako `autotabs-x-y` (#112).
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

![](redactor/apps/multistep-form/real-form.png)

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
- Editor - upravený dialog pro nastavení `a.btn` - ​​zrušeno nastavení barev a velikostí, [používají se už jen CSS třídy](frontend/setup/ckeditor.md#tlačítko) stejně jako pr `button` (#57657-16).
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

## 2025.40

> **WebJET CMS 2025.40** přináší integrovaného **AI Asistenta**, který zásadně zjednodušuje práci s obsahem. Umožňuje automaticky **opravovat gramatiku**, **překládat** texty, navrhovat titulky, sumarizovat články a generovat **ilustrační obrázky** přímo v editoru. Díky tomu je tvorba obsahu **rychlejší, přesnější a kreativnější** než kdykoli předtím.
>
> Významné změny se týkají i **značek** a **šablon novinek**, které byly přepracovány do **samostatných databázových tabulek** s podporou oddělení podle domén. To přináší vyšší **přehlednost, jednodušší správu** a možnost efektivního přizpůsobení obsahu pro více webů. Uživatelské prostředí bylo **optimalizováno pro menší obrazovky** – systém automaticky přizpůsobí zobrazení oken a maximalizuje využitelný prostor.
>
> Na technické úrovni byl odstraněn zastaralý Struts Framework. Díky tomu je WebJET CMS výkonnější, stabilnější, bezpečnější a připraven k dalšímu rozvoji moderních webových řešení.

### Průlomové změny

- Odstraněn `Struts Framework`, je třeba provést aktualizaci `JSP` souborů, Java tříd a upravit soubor `web.xml`, více v [sekci pro programátora](#pre-programátora) (#57789).
- Pokud používáte aplikační server Tomcat ve verzi 9.0.104 a více je třeba [aktualizovat nastavení](install/versions.md#změny-při-přechodu-na-tomcat-90104) parametru `maxPartCount` na `<Connector` elementu (#54273-70).
- Značky - rozdělené podle domén - při startu se vytvoří kopie značek pro každou doménu (je-li používáno rozdělení údajů podle domén - nastavena konfigurační proměnná `enableStaticFilesExternalDir=true`). Aktualizují se ID značek pro web stránky a galerii. Je třeba manuálně zkontrolovat ID značek pro všechny aplikace novinky a jiné aplikace, které obsahují ID značky – aktualizace se je pokusí opravit, ale doporučujeme ID zkontrolovat. Více informací v sekci pro programátora. (#57837).
- Novinky - [šablony novinek](frontend/templates/news/README.md) předělané z definice přes překladové klíče na vlastní databázovou tabulku. Při startu WebJETu se zkonvertují záznamy z původního formátu. Jsou odděleny podle domén, pokud obsahují doménový alias vytvoří se pouze v příslušné doméně (#57937).
- Bezpečnost - přísnější kontrola URL adres administrace - je třeba, aby URL adresa v administraci měla na konci znak `/`, nesprávná adresa je `/admin/v9/webpages/web-pages-list` nebo `/apps/quiz/admin`, správná `/admin/v9/webpages/web-pages-list/` nebo `/apps/quiz/admin/`. Je nutné, aby programátor zkontroloval definice URL adres v souborech `modinfo.properties` (#57793).

### AI Asistent

V dnešním světě je umělá inteligence všude kolem nás a samozřejmě WebJET jako moderní redakční systém nechce zůstat pozadu. Proto s hrdostí představujeme novou verzi WebJET CMS, kde jsme integrovali [pokročilé AI nástroje](redactor/ai/README.md).

![](redactor/ai/datatables/ckeditor-assistants.png)

Tyto funkce vám usnadní tvorbu a úpravu obsahu – od opravy gramatiky, přes překlady textů, návrhy titulků, až po generování ilustračních obrázků.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/LhXo7zx7bEc" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

### Web stránky

- AB Testování - přidána možnost [zobrazovat AB verzi](redactor/apps/abtesting/README.md) podle stavu přihlášeného uživatele - nepřihlášenému uživateli se zobrazí A verze a přihlášenému B verze. Režim aktivujete nastavením konfigurační proměnné `ABTestingForLoggedUser` na hodnotu `true` (#57893).
- [Page Builder](redactor/webpages/pagebuilder.md) - upravený vizuál, aby lépe zapadal do aktuálního designu WebJET CMS (#57893).

![](redactor/webpages/pagebuilder-style.png)

- Povoleno zobrazení stránek obsahujících `404.html` v URL adrese ze systémových složek, aby vám taková technická stránka nepřekážela mezi standardními web stránkami (#57657-8).
- Značky - rozdělené zobrazení značek podle aktuálně zvolené domény, abyste mohli mít značky zvlášť pro každou doménu ve WebJETu (#57837).
- Klonování struktury - přidána informace o nakonfigurovaném překladači a kolik volných znaků k překladu zbývá (#57881).
- Zrcadlení struktury - přidána možnost vymazat `sync_id` hodnoty pro zvolenou složku (rekurzivní). Aby bylo snadno možné zrušit/resetovat zrcadlení stránek (#57881).

![](redactor/apps/clone-structure/clone_structure_set_translator.png)

- Zrcadlení - přidání nové sekce [zrcadlení](redactor/webpages/mirroring/README.md) pro sledování a manageování provázaných složek a stránek po akci zrcadlení (#57941).

![](redactor/webpages/mirroring/groups_datatable.png)

- Při výběru obrázku nebo video souboru, v editoru stránek jsou v průzkumníku zobrazeny jen vhodné typy souborů, ostatní jsou filtrovány (#57921).

### Šablony

- Přidána nová sekce [Šablony novinek](frontend/templates/news/README.md) pro správu a správu šablon novinek (#57937).

![](frontend/templates/news/news-temps-datatable.png)

### Uživatelské rozhraní

- Při použití malého monitoru (výška okna méně než 760 bodů) se zobrazí okno automaticky na celou plochu a zmenší se hlavička a patička (titulek okna je menším písmem). Zvýší se tak zobrazené množství informací, což je zapotřebí hlavně v sekci web stránky. Používá se u oken používajících CSS třídu `modal-xl`, což jsou aktuální web stránky, foto galerie, editor obrázků a uživatelé (#57893).

![](redactor/webpages/pagebuilder.png)

- V editoru přidána možnost kliknout na ikonu obrázku na začátku pole, pro jeho zobrazení v nové kartě.

![](developer/datatables-editor/field-type-elfinder.png)

### Aplikace

- Přidána možnost zobrazit aplikaci pouze přihlášenému/nepřihlášenému uživateli. Režim se nastavuje v kartě [Zobrazení nastavení aplikace](redactor/webpages/working-in-editor/README.md#karta-zobrazení) v editoru stránek (#57893).

![](custom-apps/appstore/common-settings-tab.png)

Předěláno nastavení vlastností aplikací v editoru ze starého kódu v `JSP` na `Spring` aplikace. Aplikace automaticky získávají také možnost nastavit [zobrazení na zařízeních](custom-apps/appstore/README.md#podmíněné-zobrazení-aplikace). Design je ve shodě se zbytkem WebJET CMS a datových tabulek (#57409).

- [Carousel Slider](redactor/apps/carousel_slider/README.md)
- [Emotikony](redactor/apps/emoticon/README.md)
- [Fórum/Diskuse](redactor/apps/forum/README.md)
- [Otázky a odpovědi](redactor/apps/qa/README.md)
- [Uživatelé](redactor/apps/user/README.md)
- [Působivá prezentace](redactor/apps/app-impress_slideshow/README.md)
- [Restaurační menu](redactor/apps/restaurant-menu/README.md)
- [Slider](redactor/apps/slider/README.md)
- [Slit slider](redactor/apps/app-slit_slider/README.md)
- [Sociální ikony](redactor/apps/app-social_icon/README.md)
- [Video](redactor/apps/video/README.md)

![](redactor/apps/app-slit_slider/editor-items.png)

### Menu

- Pokud [menu webové stránky](redactor/apps/menu/README.md) nemá zadanou kořenovou složku (hodnota je nastavena na 0), automaticky se použije kořenová složka pro aktuálně zobrazenou web stránku. Je to výhodné pokud se zobrazuje menu ve více jazykových mutacích kde každá je kořenová složka - nemusíte mít menu/hlavičky pro každý jazyk samostatně, stačí jedna společná (#57893).

### Statistika

- V sekci [návštěvnost](redactor/apps/stat/README.md#návštěvnost) přidán sumární počet Vidění, Návštěv a Počet různých uživatelů pro snadný přehled celkové návštěvnosti za zvolené období (#57929).

![](redactor/apps/stat/stats-page.png)

- V sekci [chybné stránky](redactor/apps/stat/README.md#chybné-stránky) přidáno filtrování podle botů (aplikuje se pouze na nově zaznamenané údaje) a sumární počet v patičce. Je nutné upravit stránku `404.jsp` ve vašem projektu přidáním objektu `request` do volání `StatDB.addError(statPath, referer, request);` (#58053).

![](redactor/apps/stat/error-page.png)

### Volitelná pole

- Přidána podpora pro nové typy [volitelných polí](frontend/webpages/customfields/README.md):
  - [Výběr složky webových stránek](frontend/webpages/customfields/README.md#výběr-složky-webových-stránek) (#57941).
  - [Výběr webové stránky](frontend/webpages/customfields/README.md#výběr-webové-stránky) (#57941).

![](frontend/webpages/customfields/webpages-doc-null.png)

![](frontend/webpages/customfields/webpages-group-null.png)

### Bezpečnost

- Opravena možná zranitelnost v Safari při speciální URL adrese směřující na archiv souborů v kombinaci s pěknou 404 stránkou (#57657-8).

### Jiné menší změny

- Audit změn - vyhledávání - pole Typ je uspořádáno podle abecedy (#58093).
- Elektronický obchod - přidána možnost nastavit [kořenový adresář](redactor/apps/eshop/product-list/README.md) se seznamem produktů pomocí konfigurační proměnné `basketAdminGroupIds`, pokud nevyhovuje automatické hledání podle vložené aplikace seznam produktů (#58057).
- Elektronický obchod - aplikace pro nastavení platebních metod přesunuta ze složky `/apps/eshop/admin/payment-methods/` do standardního `/apps/basket/admin/payment-methods/` (#58057).
- Elektronický obchod - po smazání objednávky jsou smazány z databáze i její položky a platby (#58070).
- Monitorování serveru - aktuální hodnoty - přidaný typ databázového serveru (MariaDB, Microsoft SQL, Oracle, PostgreSQL) (#58101).
- Překladač - u překladače `DeepL` se zlepšilo zpracování vrácených chybových hlášek, pro přesnější identifikování problému (#57881).
- Překladač - přidána podpora pro implementaci více překladačů a jejich automatické zpracování/využití (#57881).
- Překladač - přidáno automatické [auditování počtu spotřebovaných znaků](admin/setup/translation.md) při každém překladu. Do audit záznamu typu `TRANSLATION` se do sloupce `EntityID` zapíše spotřebované množství kreditů při překladu. Audituje se i počet dostupných znaků, výsledek je uložen do cache a aktualizuje se znovu nejdříve o 5 minut (#57965).
- Průzkumník - optimalizované načítání, opravené duplicitní čtení knihovny `jQuery UI` (#57997).

### Oprava chyb

- Datové tabulky - opraveno nastavení možností do výběrového menu externího filtru (#57657-8).
- Klonování struktury - opravena validace zadaných id složek a přidán výpis chybové zprávy (#57941).
- Galerie - přidána podpora pro výběr složky galerie, v aplikaci Galerie ve web stránce, při použití doménových aliasů a editace záznamu v galerii s doménovým aliasem (#57657-11).
- Webové stránky - opraveno zobrazení seznamu stránek při zobrazení složek jako tabulky (#57657-12).
- Grafy - opraveno zobrazení velkého množství legend v grafech, automaticky se využije posouvání v legendách (#58093).

### Dokumentace

- Doplněna dokumentace pro nastavení a používání [dvoustupňového ověřování/autorizace](redactor/admin/logon.md#dvoustupňové-ověřování) (#57889).

### Pro programátora

- Zrušená třída `ImportXLSForm`, která se používala v importech z `XLS` formátu v [spec/import_xls.jsp](../../src/main/webapp/admin/spec/import_xls.jsp). Technicky třída není nutná, stačí smazat referenci v JSP a upravit formulář na standardní HTML formulář (#57789).
- Zlepšený aktualizační skript `/admin/update/update-2023-18.jsp` pro Archiv souborů - umí aktualizovat standardní změny a doplnit potřebné změny do vaší verze `FileArchivatorBean` a pomocných tříd (#57789).
- Třída `org.apache.struts.action.ActionMessage` nahrazená objektem `String`, třída `ActionMessages` nahrazená `List<String>` (#57789).
- Zrušený framework `Struts`, tagy `<logic:present/iterate/...` nahrazeny za odpovídající `<iwcm:present/iterate/...`, pozor `<bean:write` za `<iwcm:beanWrite`.
- V Java kódu jsou z důvodu odstranění `Struts` následující změny:
  - `ActionMessage` nahrazen `String`
  - `ActionMessages` nahrazen `List<String>`
  - `BasicLdapLogon.logon` vrátí `List<String>` místo `ActionMessages`
  - `org.apache.struts.util.ResponseUtils.filter` nahrazen `sk.iway.iwcm.tags.support.ResponseUtils.filter`
- Amcharts - přidána podpora pro zadání funkce pro transformaci textu ve štítcích kategorií u grafu typu `PIE` (#58093).
- Amcharts - přidána podpora pro zadání funkce pro transformaci textu v legendě grafu typu `LINE` (#58093).
- Amcharts - přidána možnost skrýt tooltip když hodnota je `null` nebo `0` v grafu typu `LINE` (#58093).

Pro konverzi JSP i Java souborů můžete použít skript `/admin/update/update-2023-18.jsp`. Zadáte-li jako cestu hodnotu `java` provede se nahrazení iv `../java/*.java` souborech. Problémem je spuštění projektu, pokud obsahuje chyby. Můžete ale složku `src/main/java` přejmenovat na `src/main/java-update`, aby šel spustit čistý WebJET. Následně můžete použít aktualizační skript. Ten prohledává a aktualizuje složku `../java/*.java` i `../java-update/*.java`.

V souboru `WEB-INF/web.xml` již není nutná inicializace `Apache Struts`, smažte celou `<servlet>` sekci obsahující `<servlet-class>org.apache.struts.action.ActionServlet</servlet-class>` a `<servlet-mapping>` obsahující `<servlet-name>action</servlet-name>`.

- Rozdělené značky podle domén (je-li nastavena konfigurační proměnná `enableStaticFilesExternalDir=true`), aby bylo možné jednoduše mít samostatné značky pro každou doménu. Při spuštění WebJET nakopíruje stávající značky pro všechny definované domény. Přeskočí značky, které mají nastavené zobrazení pouze ve specifické složce, kde podle první složky nastaví doménu pro značku. Aktualizuje značky pro Novinky, tedy pro aplikaci `/components/news/news-velocity.jsp` kde vyhledá výraz `perexGroup` a `perexGroupNot` u kterých se pokusí ID značek aktualizovat podle domény dané web stránky. Informace se zapíše do historie a v Auditu vznikne záznam s podrobností jak se `INCLUDE` nahradil, příklad:

```txt
UPDATE:
id: 76897

news-velocity.jsp - update perexGroups+perexGroupsNot for domainId, old code::
INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="625", perexGroupNot="626")
new code:
INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="", perexGroupNot="")

INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="3+645", perexGroupNot="794")
new code:
INCLUDE(/components/news/news-velocity.jsp, groupIds="24", alsoSubGroups="false", publishType="new", order="date", ascending="false", paging="false", pageSize="1", offset="0", perexNotRequired="false", loadData="false", checkDuplicity="true", contextClasses="", cacheMinutes="0", template="news.template.dlazdica-3", perexGroup="1438+1439", perexGroupNot="1440")
```

Pro první `INCLUDE` byly odstraněny značky s ID 625 a 626, protože ty se nezobrazují v dané složce/doméně - měly nastavené zobrazení pouze pro určitou složku. Ve druhém `INCLUDE` byly změněny značky `3+645` na nově vzniklé `1438+1439` a `794` za `1440`.

| perex_group_id | perex_group_name      | domain_id | available_groups |
|----------------|-----------------------|-----------|------------------|
| 3              | další perex skupina  | 1         | NULL             |
| 645            | deletedPerexGroup     | 1         | NULL             |
| 794            | kalendář-událostí     | 1         | NULL             |
| 1438           | další perex skupina  | 83        | NULL             |
| 1439           | deletedPerexGroup     | 83        | NULL             |
| 1440           | kalendář-událostí     | 83        | NULL             |

Před spuštěním aktualizace existovaly v databázi pouze záznamy `3, 645 a 794`, kterým se nastavilo `domain_id=1`. Záznamy `1438, 1439 a 1440` vznikly při aktualizaci pro `domain_id=83`.

- Datové tabulky - přidána podpora pro úpravu [lokálních JSON dat](developer/datatables-editor/field-datatable.md#lokální-json-data) (#57409).
- Datové tabulky - přidáno rozšíření [Row Reorder](https://datatables.net/extensions/rowreorder/) pro možnost uspořádání seznamu pomocí funkce `Drag&Drop` (#57409).
- Datatabulky - Přidána možnost nastavení [Patičky pro součet hodnot](developer/datatables/README.md#patička-pro-součet-hodnot) (#57929).
- Aplikace - doplněna možnost použít lokální JSON data pro nastavení položek aplikace, například položek pro [působivou prezentaci](redactor/apps/app-impress_slideshow/README.md) (#57409).

![](redactor/apps/app-impress_slideshow/editor-items.png)

## 2025.18

> Verze **2025.18** přináší kompletně předělaný modul **Elektronického obchodu** s podporou **platební brány GoPay** a vylepšeným seznamem objednávek. Aplikace **Kalendář novinek** byla oddělena jako **samostatná aplikace** a zároveň jsme předělali nastavení více aplikací v editoru stránek do nového designu. **Manažer dokumentů** (původně Archiv souborů) prošel **vizuálním i funkčním restartem** včetně nových nástrojů pro správu, export a import dokumentů.
>
> Vylepšen byl i systém **Hromadného e-mailu** s novými možnostmi pro odesílatele a pohodlnějším výběrem příjemců. **Rezervace** získaly nové možnosti jako **nadměrné rezervace**, vytváření rezervací zpětně do minulosti a zasílání notifikací na specifické emaily pro každý rezervační objekt.
>
> Optimalizovali jsme počet souborů v **Průzkumníku**, což vede k **rychlejšímu načítání** a přidali nové informace do **Monitorování serveru**.

### Průlomové změny

- Aplikace Kalendář novinek oddělená do samostatné aplikace, pokud kalendář novinek používáte je třeba upravit cestu `/components/calendar/news_calendar.jsp` na `/components/news-calendar/news_calendar.jsp` (#57409).
- Upravená inicializace Spring a JPA, více informací v sekci pro programátora (#43144).
- Předělaná backend část aplikace elektronický obchod, více v sekci pro programátora (#57685).

### Datové tabulky

- Při nastavení filtru číselné hodnoty od-do se pole zvětší pro lepší zobrazení zadané hodnoty podobně jako to dělá datové pole (#57685).
- Aplikace Archiv souborů byla předělána na Spring aplikaci. Bližší informace naleznete v sekci pro programátora (#57317).
- Aplikace Elektronický obchod byla na `BE` části předělána. Bližší informace naleznete v sekci pro programátora (#56609).

### Manažer dokumentů (Archiv souborů)

- **Seznam souborů** předělaný do nového designu s přidáním nové logiky oproti staré verzi. Více se dočtete v části [Archiv souborů](redactor/files/file-archive/README.md) (#57317).

![](redactor/files/file-archive/datatable_allFiles.png)

- **Manažer kategorií** opraven a předělán do nového designu. Více se dočtete v části [Manažer kategorií](redactor/files/file-archive/category-manager.md) (#57317).
- **Manažer produktů** byl přidán jako nová sekce. Více se dočtete v části [Manažer produktů](redactor/files/file-archive/product-manager.md) (#57317).
- **Export hlavních souborů** byl upraven tak, aby nabízel širší možnosti exportu souborů a zlepšil přehlednost výpisů. Více se dočtete v části [Export hlavních souborů](redactor/files/file-archive/export-files.md) (#57317).

![](redactor/files/file-archive/export_all.png)

- **Import hlavních souborů** byl opraven a upraven, aby dokázal pracovat s rozšířenými možnostmi exportu. Více se dočtete v části [Import hlavních souborů](redactor/files/file-archive/import-files.md) (#57317).
- **Indexování** dokumentů ve vyhledávačích typu `Google` upravené tak, aby se neindexovaly staré/historické verze dokumentů a dokumenty mimo datum platnosti (nastavená HTTP hlavička `X-Robots-Tag=noindex, nofollow`). Indexování těchto dokumentů lze povolit v editoru v manažerovi dokumentů (#57805).

### Aplikace

Předěláno nastavení vlastností aplikací v editoru ze starého kódu v `JSP` na `Spring` aplikace. Aplikace automaticky získávají také možnost nastavit [zobrazení na zařízeních](custom-apps/appstore/README.md#podmíněné-zobrazení-aplikace). Design je ve shodě se zbytkem WebJET CMS a datových tabulek (#57409).

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

- Zrychlené načtení údajů aplikace v editoru - data jsou vložena přímo ze serveru, není třeba provést volání REST služby (#57673).
- Upravený vizuál - název aplikace při vkládání do stránky přesunut do hlavního okna (namísto původního nadpisu Aplikace) pro zvětšení velikosti plochy pro nastavení aplikaci (#57673).

![](redactor/apps/menu/editor-dialog.png)

- Doplněné fotky obrazovky aplikací v české jazykové mutaci pro většinu aplikací (#57785).

### Hromadný e-mail

- **Přesunuté pole Web stránka** – nyní se nachází před polem **Předmět**, aby se po výběru stránky předmět automaticky vyplnil podle názvu zvolené web stránky (#57541).
- **Úprava pořadí v kartě Skupiny** – e-mailové skupiny jsou nyní zobrazeny před skupinami uživatelů (#57541).
- **Nové možnosti pro jméno a e-mail odesílatele** – jsou-li konfigurační proměnné `dmailDefaultSenderName` a `dmailDefaultSenderEmail` nastaveny, použijí se tyto hodnoty. Pokud jsou prázdné, systém automaticky vyplní jméno a e-mail aktuálně přihlášeného uživatele. (#57541)
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
- Doplněna informace o verzi knihoven `Spring (Core, Data, Security)` do sekce Monitorování serveru-Aktuální hodnoty (#57793).

### Rezervace

- **Podpora pro nadměrnou rezervaci** – umožňuje administrátorům vytvořit více rezervací `overbooking` na tentýž termín (#57405).
- **Vylepšená validace při importu** – nyní lze importovat [rezervace](redactor/apps/reservation/reservations/README.md) i do minulosti, nebo vytvořit `overbooking` rezervace při importu údajů (#57405).
- **Podpora pro přidání rezervace do minulosti** – umožňuje administrátorům vytvořit rezervace v minulosti (#57389).
- Do [rezervačních objektů](redactor/apps/reservation/reservation-objects/README.md) byl přidán sloupec **Emaily pro notifikace**, který pro každý zadaný platný email (oddělený čárkou) odešle email pokud byla rezervace přidána a schválena (#57389).
- Notifikacím pro potvrzení rezervace a dalším systémovým notifikacím je možné nastavit jméno a email odesílatele pomocí konfiguračních proměnných `reservationDefaultSenderName,reservationDefaultSenderEmail` (#57389).
- Přidána nová aplikace [Rezervace dnů](redactor/apps/reservation/day-book-app/README.md), pro rezervaci celodenních objektů na určitý interval pomocí integrovaného kalendáře (#57389).

![](redactor/apps/reservation/day-book-app/app-table_B.png)

### Galerie

- Přidána podpora pro **změnu složky** obrázku, která umožňuje [přesunout obrázek](redactor/apps/gallery/README.md#) při úpravě nebo duplikování do jiné složky. Užitečné to je právě při duplikování, kdy můžete rovnou nastavit novou složku, kam chcete obrázek duplikovat. Pokud složku zadáte ručně a neexistuje, automaticky se vytvoří a nastaví se mu vlastnosti podle nejbližší existující rodičovské složky (#57885).

### Elektronický obchod

!> **Upozornění:** z důvodu aktualizace databáze může první start serveru trvat déle - do databáze se vypočítají hodnoty pro počet položek a cenu pro rychlejší načtení seznamu objednávek.

- Přidána karta **Osobní informace** do seznamu objednávek - obsahuje podrobné informace o **adrese doručení** jakož i **kontaktní informace** vše na jednom místě (#57685).
- Přidána karta **Volitelná pole** do seznamu objednávek - [volitelná pole](frontend/webpages/customfields/README.md) podle potřeby implementace (#57685).
- Export seznamu objednávek - doplněné sloupce celková cena s DPH a počet položek (#57685).
- Formulář pro objednání - doplněna možnost definovat dostupný seznam zemí přes konfigurační proměnnou `basketInvoiceSupportedCountries` (#57685).
- Upravené zobrazení údajů z karty **Osobní údaje** v seznamu objednávek, jejich logické rozdělení do částí pro lepší přehled (#57685).
- V seznamu objednávek byly přidány sloupce **Počet položek**, **Cena bez DPH** a **Cena s DPH**. Hodnoty se automaticky přepočítají při změně položek objednávky (#57685).
- Do seznamu položek doplněna možnost zobrazení web stránky produktu kliknutím na ikonu, produkt se zobrazí také v kartě Náhled při otevření editoru položky (#57685).
- V seznamu objednávek proveden výběr země přes výběrové pole, který nabízí pouze země definované konstantou `basketInvoiceSupportedCountries` (#57685).

![](redactor/apps/eshop/invoice/editor_personal-info.png)

- Nová verze [konfigurace způsobů platby](redactor/apps/eshop/payment-methods/README.md) a integrace na platební brány. Údaje jsou odděleny podle domén. Přidali jsme podporu [platební brány GoPay](https://www.gopay.com), což znamená i akceptaci platebních karet, podporu `Apple/Google Pay`, platby přes internet banking, `PayPal`, `Premium SMS` atp. Kromě toho jsou podporovány platby převodem a dobírka. Pro každý typ platby lze nastavit také cenu, která při zvolení možnosti bude automaticky přičtena k objednávce. Nastavené způsoby platby se také automaticky promítnou do možností při vytváření objednávky zákazníkem.

![](redactor/apps/eshop/payment-methods/datatable.png)

- Nová aplikace Seznam objednávek se seznamem objednávek aktuálně přihlášeného uživatele. Klepnutím na objednávku lze zobrazit detail objednávky a stáhnout ji v PDF formátu (#56609).

### Jiné menší změny

- Vyhledávání v administraci - upravené rozhraní na vlastní `RestController` a `Service` (#57561).
- Průzkumník - rychlejší načítání a nižší zatížení serveru snížením počtu souborů/požadavek na server (#56953).
- `dt-tree-dir-simple` - ​​přidána podpora pro [skrytí rodičovské složky](developer/datatables-editor/field-json.md#možnosti-classname) ve zobrazené stromové struktuře atributem `data-dt-field-hideRootParents` (#57885).

### Oprava chyb

- Hromadný email - při duplikování kampaně doplněno duplikování seznamu příjemců (#57533).
- Datové tabulky - import - upravená logiky **Přeskočit chybné záznamy** při importu tak, aby se při této možnosti zpracovaly i generické chyby `Runtime` a zajistilo se dokončení importu bez přerušení. Tyto chyby se následně zobrazí uživateli pomocí notifikace v průběhu importování (#57405).
- Soubory - opraven výpočet velikosti souborů/složek v patičce průzkumníka a při zobrazení detailu složky (#57669).
- Navigace - opravena navigace pomocí karet v mobilním zobrazení (#57673).
- Autocomplete - opravena chyba u pole typu `Autocomplete`, kde první získaná hodnota v případě `jstree` nebyla korektní (#57317).

### Pro programátora

!> **Upozornění:** upravená inicializace Spring a JPA, postupujte podle [návodu v sekci instalace](install/versions.md#změny-při-přechodu-na-20250-snapshot).

Jiné změny:

- Přidána možnost provést [doplňkový HTML/JavaScript kód](custom-apps/appstore/README.md#doplňkový-html-kód) ve Spring aplikaci s anotací `@WebjetAppStore` nastavením atributu `customHtml = "/apps/calendar/admin/editor-component.html"` (#57409).
- V datatable editoru přidán typ pole [IMAGE_RADIO](developer/datatables-editor/standard-fields.md#image_radio) pro výběr jedné z možnosti pomocí obrázku (#57409).
- Přidán typ pole `UPLOAD` pro [nahrání souboru](developer/datatables-editor/field-file-upload.md) v editoru datatabulky (#57317).
- Při inicializaci [vnořené datatabulky](developer/datatables-editor/field-datatable.md) přidána možnost upravit `columns` objekt zadáním JavaScript funkce do atributu `data-dt-field-dt-columns-customize` anotace (#57317).
- Přidána podpora pro získání jména a emailu odesílatele pro různé emailové notifikace použitím `SendMail.getDefaultSenderName(String module, String fallbackName), getDefaultSenderEmail(String module, String fallbackEmail)` (#57389).
- Přidána možnost nastavit kořenovou složku pro [pole typu JSON](developer/datatables-editor/field-json.md) ve formátu ID i cesty: `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "/Aplikácie/Atribúty stránky")` nebo `@DataTableColumnEditorAttr(key = "data-dt-field-root", value = "26")`.
- Spuštění úloh na pozadí se provede až po kompletní inicializaci včetně `Spring` (#43144).
- Doplněna možnost nastavit [všechny vlastnosti HikariCP](install/setup/README.md#vytvoření-db-schémy) (#54273-61).
- Doplněna kontrola, zda databázový ovladač podporuje nastavení sekvencí (#54273-61).
- Upravená funkce `WJ.headerTabs`, pokud posloucháte na změnu karty doporučujeme použít událost typu `$('#pills-tabsFilter a[data-wj-toggle="tab"]').on('click', function (e) {`, kde v `e` získáte kartu, na kterou se kliklo (#56845-20250325).
- Předělaná aplikace Manažer dokumentů (Archiv souborů) na Spring aplikaci. Pokud používáte původní verzi a chcete ji zachovat, musíte přidat zpět soubory `/components/file_archiv/file_archiv.jsp` a `components/file_archiv/editor_component.jsp` a potřebné třídy ze [starší verze WebJET CMS](https://github.com/webjetcms/webjetcms/tree/release/2025.0/src/main/java/cs/iway
- Manažer dokumentů (Archiv souborů) - upravené API `FileArchivatorBean.getId()/getReferenceId()/saveAndReturnId()` vrátí `Long`, můžete použít `getFileArchiveId()` pro včetně `int` hodnoty. Smazané nepoužívané metody, v případě jejich potřeby je přeneste do vašich tříd. Nedoporučujeme modifikovat WebJET třídy, vytvořte si nové třídy typu `FileArchivatorProjectDB` ve vašem projektu kde metody přidáte. Pokud jsme smazali celou třídu, kterou používáte (např. `FileArchivatorAction`), můžete si ji přímo přidat do vašeho projektu (#57317).
- Přidáno automatické nastavení filtrování sloupce na hodnotu `false`, v případě že hodnota je `null` (nenastavena) a jde o sloupec, který je vnořený, jako např. `editorFields` sloupce (#57685).
- Přidána možnost [speciálního uspořádání](developer/datatables/restcontroller.md#uspořádání) přepsáním metody `DatatableRestControllerV2.addSpecSort(Map<String, String> params, Pageable pageable)` (#57685).
- Přidána možnost v anotaci `@DataTableColumn` nastavit atribut `orderProperty` který určí [sloupce pro uspořádání](developer/datatables/restcontroller.md#uspořádání). `orderProperty = "contactLastName,deliverySurName"`. Výhodné pro `EditorFields` třídy, které mohou agregovat data z více sloupců (#57685).
- Pro pole typu `dt-tree-dir-simple` s nastaveným `data-dt-field-root` doplněna stromová struktura rodičovských složek pro lepší [zobrazení stromové struktury](developer/datatables-editor/field-json.md) (předtím se složky zobrazovaly až od zadané kořenové složky). Přidána možnost definovat seznam složek, které se ve stromové struktuře nezobrazí pomocí konfigurační proměnné nastavené do `data-dt-field-skipFolders`.
- Výběrové [pole s možností editace](developer/datatables-editor/field-select-editable.md) upraveno tak, aby po přidání nového záznamu byl tento záznam automaticky v poli zvolen (#57757).
- Předělaná aplikace Elektronický obchod na `BE` části. Jelikož se využívají již nové třídy, pro správné fungování musíte:
  - využít aktualizační skript `/admin/update/update-2023-18.jsp` pro základní aktualizaci vašich JSP souborů
  - jelikož se nyní využívá typ `BigDecimnal` místo `float`, musíte navíc upravit všechna srovnání těchto hodnot. Typ `BigDecimal` se nesrovnává klasicky pomocí `<, =, >` ale pomocí `BigDecimal.compareTo( BigDecimal )`
  - musíte odstranit volání souborů, nebo zpětně přidat všechny soubory, které byly odstraněny, protože nebyly využívány

### Testování

- Média - doplněný test vkládání médií ve web stránce pokud uživatel nemá právo na všechna média (#57625).
- Web stránky - doplněn test vytvoření nové stránky s publikováním v budoucnosti (#57625).
- Galerie - doplněn test vodoznaku s porovnáním obrázku, doplněn test kontroly práv (#57625).
- Web stránky - doplněn test volitelných polí při vytváření web stránky (#57625).
- Allure - doplněné výsledky jUnit testů do společného Allure reportu (#57801).

![meme](_media/meme/2025-18.jpg ":no-zoom")

## 2025.0.52

> Opravná verze původní verze 2025.0.

- Bezpečnost - zakázané přesměrování na externí URL adresy po přihlášení přes přihlašovací formulář.
- Datové tabulky - opravena chyba zobrazení výběrových polí při zapnutí režimu Upravit v zobrazení mřížky (#57657-16).
- Datové tabulky - opraveno uložení nového záznamu přes klávesovou zkratku `CTRL+S` - ​​po uložení se nastaví vrácené hodnoty zpět do editoru aby se správně nastavilo ID záznamu pro další úpravy (#57657-16).
- Formuláře - upravené generování názvu pole tak, aby neobsahovalo tečku (#57657-16).
- Hromadný email - upravená tlačítka pro spuštění/zastavení odesílání hromadného emailu na lépe srozumitelné `play` a `stop` (#54273-81).
- Hromadný email - opraveno ukládání v Oracle databázi, pole předmět nastaveno jako povinné (#54273-81).
- Hromadný email - Doménové limity - opraveno načtení limitů pro domény z databáze (#54273-81).
- Hromadný e-mail - opraveno nastavení ID uživatele při přidání skupiny, pokud existuje více uživatelů se stejným emailem (#58217).
- Spam ochrana - opravena chyba kontroly časového rozmezí mezi odesláními formuláře/vyhledávání (#57657-16).
- Webové stránky - opravené uspořádání stromové struktury při přesunu položek přes `Drag&Drop` v případě nastavení sestupného uspořádání (#MF-1199).
- Webové stránky - opraveno vyhledávání složky v kartě Koš (#58081).
- Webové stránky - opraveno posílání notifikace o schválení/neschválení stránky pokud neexistuje žádný schvalovatel s notifikací, doplněn seznam změn v různých polích web stránky (#58007).

## 2025.0.50

> Opravná verze původní verze 2025.0.

- Bezpečnost - opravena možnost přihlášení, pokud heslo obsahuje diakritiku.
- Bezpečnost - opravena možná zranitelnost v synchronizaci stránek (#55193-7).
- Bezpečnost - přidána možnost konfigurace blokovaných cest souborů/adresářů přes proměnnou `pathFilterBlockedPaths`. Standardně jsou blokovány URL adresy, které v názvu obsahují výraz `.DS_Store,debug.,config.properties,Thumbs.db,.git,.svn,/WEB-INF/,./`. Lze přidat další podle potřeby (#PR103).
- Bannerový systém - opraveno zobrazení YouTube video banneru (#55193-7).
- Datové tabulky - opraveno zobrazení pokročilých možností exportu (#58113).
- Kalendář událostí - opraveno ukládání pole popis, na kterém nebylo povoleno ukládání HTML kódu (#58113).
- Novinky - opraveno vyloučení hlavních stránek pro zadanou kořenovou složku (#57657-15).
- Webové stránky - PageBuilder - opravený jazyk uživatelského rozhraní na jazyk přihlášeného uživatele (ne jazyk web stránky) (#58133).
- Webové stránky - upravena možnost vkládání HTML kódu do názvu složky a web stránky. Do názvu web stránky povoleno vkládání bezpečného HTML kódu (`AllowSafeHtmlAttributeConverter`) bez jeho úpravy, funguje tak v navigační liště, novinkách a podobně. Pro volání `${ninja.page.seoTitle}` je vrácena hodnota s odstraněným HTML kódem, jelikož se předpokládá vložení do `title` značky. Pokud potřebujete získat titulek is HTML kódem můžete použít volání `${ninja.page.seoTitleHtml}`. Při složce je nahrazen znak `/` za entitu `&#47;`, protože znak `/` se používá k oddělení jednotlivých složek (#54273-75).
- Webové stránky - opraveno zobrazení režimu PageBuilder nastaveného přímo v šabloně stránky (#57657-15).
- PDF - opraveno duplikování absolutní cesty do `fonts` složky s písmy při zadané URL adrese v proměnné `pdfBaseUrl` (#58185).
- Oracle/Microsoft SQL - opravené SQL chyby a datové typy v základních testech/tabulkách, použité čisté databáze se [základními daty](developer/testing/README.md#smazání-databáze) (#58185).

## 2025.0.40

> Opravná verze původní verze 2025.0.

!> **Upozornění:** možná změna chování polí typu `quill` pro odrážkový seznam v datových tabulkách (#54273-72).

- PDF - opraveno nastavení cesty do `fonts` složky s písmy. Je třeba zadat plnou cestu na disku serveru (#57657).
- Aktualizovaná knihovna `Apache Commons BeanUtils` na verzi 1.11.0.
- Inicializace - doplněná kontrola existence souboru `autoupdate.xml`, aby na veřejných uzlech clusteru nepsalo chybu při startu WebJETu (#54273-68).
- Bezpečnost - doplněna kontrola výrazu `onwebkit` u URL parametrů pro zamezení provedení XSS útoku (#54273-68).
- Pole typu `QUILL` (malý HTML editor používaný v Bannerech, Galerii...) - opraveno duplikování `P` elementu pokud obsahuje CSS třídu nebo jiný atribut (#54273-69).
- Bezpečnost - v anotaci `@AllowSafeHtmlAttributeConverter` povoleno vkládání atributů `alt,title` pro `img` a `class` pro elementy `a,img,div,span,p,h1,h2,h3,h4,h5,h6,i,b,strong,em` (#54273-69).
- Bezpečnost - aktualizovaná knihovna `hibernate-validator` na verzi `6.2.5.Final` (#54273-69).
- Bezpečnost - opravena možná zranitelnost v AB testování.
- Bezpečnost - opraveno zbytečné čtení `dataAsc` v JSON objektu `DocBasic`.
- Bezpečnost - snížené množství textu při logování chyby `Session has already been invalidated` při zahlcení/útoku na web server (#BVSSLA-34).
- Administrace - přidána možnost vkládat [doplňkový CSS/JavaScript](custom-apps/apps/customize-admin.md) soubor do administrační části, například. pro vlastní CSS styly pro [pole typu Quill](developer/datatables-editor/standard-fields.md#quill) (#54273-69).
- Datové tabulky - pro Oracle a Microsoft SQL vypnuta možnost uspořádání podle sloupců obsahujících dlouhý text (`ntext/clob`) - tyto databázové systémy nepodporují uspořádání v případě použití tohoto datového typu. Atribut musí v `Entite` mít anotaci `@Lob`, která pro uvedené databáze vypne možnost uspořádání pro daný sloupec. Pro MariaDB a PostgreSQL je uspořádání stále podporováno (#54273-70).
- Datové tabulky - opravené vyhledávání pokud v jednom poli zvolíte možnost "Začíná na" a v jiném poli např. "Končí na" (#54273-70).
- Datové tabulky / vyhledávání v administraci - povoleny speciální znaky (např. uvozovky) pro vyhledávání v datových tabulkách (#54273-70).
- Formuláře - schované zbytečné tlačítko pro vytvoření nového záznamu v seznamu vyplněných formulářů (#54273-70).
- Formulář snadno - opraveno nastavení povinných polí v položkách formuláře (#57657-12).
- Webové stránky - doplněna možnost vkládat HTML kód do názvů složek jako například `WebJET<sup>TM</sup>` - ​​v seznamu webových stránek se z důvodu bezpečnosti HTML kód neprovede, ale v aplikacích jako Menu a navigační lišta se HTML kód zobrazí korektně a provede se. Důležitá podmínka je, aby kód obsahoval uzavírací značku `</...>`. HTML kód je odstraněn iz automaticky generované URL adresy. Povolen je pouze bezpečný HTML kód povolený ve třídě `AllowSafeHtmlAttributeConverter` (#54273-70).
- Webové stránky - opraveno zobrazení karty média u starých uživatelů, kteří neměli právo na správu médií (#57657-10).
- Datové tabulky - pro pole typu malý HTML editor (`quill`) **upravené chování pro odrážkový seznam** (HTML značka `ul`). Původní editor nastavoval pro tento případ na `li` elementu atribut `data-list="bullet"` a nedokázal použít přímo `ul` element místo `ol` elementu. Nové chování používá korektní HTML značku `ul` a odstraňuje nepotřebný atribut `data-list="bullet"` (#54273-72).
- Galerie - opraveno zobrazení perex skupin pokud je jich více než 30 v galerii a editoru obrázků - zobrazeno jako výběrové pole. Opraveno načítání a uložení skupin v editoru obrázků (#57657-9).
- Průzkumník - opraveno nahrávání celé složky s pod složkami do `/images/gallery` přes Průzkumník - korektní vytvoření `o_` a `s_` obrázků (#57657-11).
- Galerie - opraveno zobrazení ikony sdílení v galerii typu `PrettyPhoto` (#57657-11).
- Galerie - opraveno zobrazení seznamu složek při použití doménových aliasů (zobrazení pouze složek z aktuálně zvolené domény) (#57657-11).
- Galerie - opraveno získání vodoznaku pro galerie používající doménový alias (#57657-11).
- Nahrávání souborů - upravené zpracování souborů nahrávaných přes `/XhrFileUpload`. Upravená verze umožní restartovat server a následně po obnovení `session` soubor korektně zpracovat. Doplněno zobrazení varování pokud soubor je nepovoleného typu (#PR75).
- Galerie - zrušeno nastavení URL adresy při zobrazení fotografií v galerii typu `PrettyPhoto` pro snadnější použití tlačítka zpět v prohlížeči (#57657-12).
- Novinky - opraveno nastavení zobrazení hlavních stránek ze složek (#57657-12).
- PDF - při generování PDF opravena chyba odhlášení uživatele, pokud PDF obsahuje obrázky vložené přes `/thumb` prefix (#57657-13).

## 2025.0.23

> Opravná verze původní verze 2025.0.

### Oprava chyb

- Datové tabulky - opraveno chybné zobrazení karet, které se nemají zobrazovat při vytváření nového záznamu (např. v šablonách) (#57533).
- Datové tabulky - doplněný limit počtu záznamů při zobrazení všechny. Hodnota je shodná s maximálním počtem řádků pro exportu, nastavuje se v konfigurační proměnné `datatablesExportMaxRows` (#57657-2).
- Datové tabulky - opravený počet záznamů na straně když stránka obsahuje navigační karty (#57725-1).
- Datové tabulky - opravený nadpis Duplikovat místo Upravit při duplikování záznamu, upravena ikona tlačítka pro duplikování (#57725-3).
- Datové tabulky - sjednocený název `ID` sloupce z původních `ID, Id, id` na sjednocený `ID`. Pro `DataTableColumnType.ID` není nutné nastavit `title` atribut, automaticky se použije klíč `datatables.id.js`. Některé překladové klíče smazány, protože nejsou nutné (#49144)
- Editor obrázků - při editaci obrázku ze vzdáleného serveru doplněna notifikace o potřebě stažení obrázku na lokální server (#57657-2).
- Web stránky - opraveno vložení bloku obsahující aplikaci (#57657-2).
- Web stránky - doplněn `ninja` objekt při vkládání aplikace do nové web stránky (#57389).
- Web stránky - stránky v koši se již nebudou zobrazovat v kartě Neschváleno, pokud schvalovatel klikne na odkaz v emailu zobrazí se chyba Stránka je v koši, aby se náhodou neschválila stránka, která byla mezi tím smazána (#54273-62).
- Web stránky - schvalování - opraveno načtení seznamu v kartě Neschváleno při použití databázového serveru `Oracle` (#54273-62).
- Web stránky - opravena aktualizace nodů clusteru při změně značek (#57717).
- Web stránky - opraveno zobrazení seznamu stránek pokud má uživatel právo pouze na vybrané webové stránky (#57725-4).
- Web stránky - doplněný přepínač domén i když není nastavena konfigurační proměnná `enableStaticFilesExternalDir` ale jen `multiDomainEnabled` (#57833).
- Aplikace - opraveno zobrazení karty překladové klíče při použití komponenty `editor_component_universal.jsp` (#54273-57).
- Aplikace - přidána podpora vkládání nového řádku přes klávesovou zkratku `SHIFT+ENTER` do jednoduchého textového editoru používaného např. v Otázky a odpovědi (#57725-1).
- Číselníky - přesunutý výběr číselníku přímo do nástrojové lišty datové tabulky (#49144).
- Novinky - přesunutý výběr sekce přímo do nástrojové lišty datové tabulky (#49144).
- Přihlášení - opravena chyba přihlášení při exspirování časové platnosti hesla (#54273-57).
- Přihlášení - opravené přihlášení v multiweb instalaci (#54273-57).
- GDPR - opraveno zobrazení karty Čištění databáze při použití `Oracle/PostgreSQL` databáze (#54273-57).
- Archiv souborů - opraveno zobrazení ikon v dialogu data a času (#54273-57).
- Bezpečnost - aktualizovaná knihovna `Swagger UI` na verzi `5.20.0`, doplněné výjimky v `dependency-check-suppressions.xml`.
- Aktualizace - doplněno mazání nepotřebných souborů při aktualizaci rozbalené verze (#57657-4).
- Multiweb - doplněna kontrola `ID` domény při registraci návštěvníka web sídla (#57657-4).
- Uživatelé - přidána možnost vybrat i Kořenovou složku v právech uživatele v sekci Nahrávání souborů do adresářů (54273-60).
- Uživatelé - upravené nastavení práv - zjednodušené nastavení práv administrátorů a registrovaných uživatelů (již není třeba zvolit i právo Uživatelé), opravené duplicitní položky, upravené seskupení v sekci Šablony (#57725-4).
- Průzkumník - doplněna lepší hlášení při chybě vytvoření ZIP archivu (#56058).
- Statistika - opraveno vytvoření tabulky pro statistiku kliknutí v teplotní mapě.
- Překladač - implementace inteligentního zpoždění pro překladač `DeepL` jako ochrana proti chybě `HTTP 429: too many requests`, která způsobovala výpadek překladů (#57833).
- Klonování struktury - opraveno nechtěné překládání implementace aplikací `!INCLUDE(...)!`, při automatickém překladu těla stránky (#57833).
- Klonování struktury - přidán překlad perex anotace automatickém překladu stránek (#57833).
- Průzkumník - opravena práva nastavení vlastností složky a souboru (#57833).
- Monitorování serveru - opraveno hlášení o nastavení konfigurační proměnné pro Aplikace, WEB stránky a SQL dotazy (#57833).
- Úvod - opraveno zobrazení požadavku na dvoustupňové ověřování při integraci přes `IIS` (#57833).
- Klonování/zrcadlení struktury - opraveno nastavení URL adresy složky (odstranění diakritiky a mezer) (#57657-7).
- Galerie - doplněno chybějící značky (#57837).
- Značky - opraveno nastavení složek existující značky v sekci Zobrazit pro (#57837).

### Bezpečnost

- Aktualizovaná knihovna `Apache POI` na verzi 5.4.1 pro opravu zranitelností `CVE-2025-31672`.

## 2025.0

> Ve verzi **2025.0** jsme přinesli **nový design administrace** pro ještě lepší přehlednost a uživatelský komfort.
>
> Jednou z hlavních změn je přesunutí **druhé úrovně menu** do **karet v hlavičce stránky**, čímž se zjednodušila navigace. Ve webových stránkách jsme také **sloučili karty složek a webových stránek**, abyste měli vše přehledně na jednom místě. Pokud hlavička neobsahuje karty, tabulky se automaticky přizpůsobí a zobrazí **řádek navíc**.
>
> Prosíme vás o zpětnou vazbu prostřednictvím **formuláře Zpětná vazba**, pokud při používání nové verze identifikujete **jakýkoliv problém se zobrazením**. Připomínku můžete doplnit io **fotku obrazovky**, což nám pomůže rychleji identifikovat a vyřešit případné nedostatky.
>
> Děkujeme za spolupráci a pomoc při vylepšování WebJET CMS!

### Průlomové změny

- Web stránky - zrušena inline editace. Možnost přímé editace stránky v režimu jejího zobrazení byla odstraněna, jelikož využívala starší verzi editoru, která již není podporována. Jako alternativu lze aktivovat [nástrojový panel](redactor/webpages/editor/README.md#nástrojový-panel) zobrazovaný v pravém horním rohu webové stránky. Tento panel umožňuje rychlý přístup k editoru web stránky, složky nebo šablony. Můžete vypnout nebo zapnout pomocí konfigurační proměnné `disableWebJETToolbar`. Po aktivaci se začne zobrazovat na webové stránce po vstupu do sekce Webové stránky v administraci (#57629).

![](redactor/webpages/webjet-toolbar.png)

- Přihlášení - pro administrátory nastavený [požadavek na změnu hesla](sysadmin/pentests/README.md#pravidla-hesel) jednou za rok. Hodnotu lze upravit v konfigurační proměnné `passwordAdminExpiryDays`, nastavením na hodnotu 0 se kontrola vypne (#57629).
- Úvod - přidán požadavek na aktivaci dvoustupňového ověřování pro zvýšení bezpečnosti přihlašovacích údajů. Výzva se nezobrazuje, pokud je ověřování řešeno přes `LDAP` nebo je-li překladový klíč `overview.2fa.warning` nastaven na prázdnou hodnotu (#57629).

### Design

Ve verzi **2025.0** jsme přinesli vylepšený **design administrace**, který je přehlednější a efektivnější.

**Upravený přihlašovací dialog** – nové pozadí a přesunutí přihlašovacího dialogu na pravou stranu. Na **přihlášení** lze použít nejen přihlašovací jméno ale **už i email adresu**.

![](redactor/admin/logon.png)

**Přehlednější hlavička** – název aktuální stránky nebo sekce se nyní zobrazuje přímo v hlavičce.

**Nová navigace v levém menu** – pod položky již nejsou součástí levého menu, ale zobrazují se **jako karty v horní části** stránky.

![](redactor/admin/welcome.png)

**Sloučené karty v sekci Webové stránky** – přepínání typů složky a typů webových stránek se nyní zobrazují ve společné části, čímž se zjednodušila navigace. **Výběr domény** byl přesunut na spodní část levého menu.
  ![](redactor/webpages/domain-select.png)

**Přeorganizované menu položky**:

- **SEO** přesunuty do sekce **Přehledy**.
- **GDPR a Skripty** přesunuty do sekce **Šablony**.
- **Galerie** je nyní v sekci **Soubory**.
- Některé názvy položek byly upraveny, aby lépe vystihovaly jejich funkci.

### Web stránky

- Přidána možnost nastavit inkrement pořadí uspořádání pro složky v konfigurační proměnné `sortPriorityIncrementGroup` a webové stránky v konfigurační proměnné `sortPriorityIncrementDoc`. Výchozí hodnoty jsou 10 (#57667-0).

### Testování

- Standardní heslo pro `e2e` testy se získá z `ENV` proměnné `CODECEPT_DEFAULT_PASSWORD` (#57629).

### Oprava chyb

- Webové stránky - vkládání odkazů na soubor v PageBuilder (#57649).
- Webové stránky - doplněné informace o odkazu (typ souboru, velikost) do atributu Pomocný titulek `alt` (#57649).
- Webové stránky - opraveno nastavení pořadí uspořádání web stránek při použití `Drag&Drop` ve stromové struktuře (#57657-1).
- Webové stránky - při duplikování webové stránky/složky se nastaví hodnota `-1` do pole Pořadí uspořádání pro zařazení na konec seznamu. Hodnotu `-1` můžete také zadat ručně pro získání nové hodnoty pořadí uspořádání (#57657-1).
- Webové stránky - import webových stránek - opraveno nastavení média skupin při importu stránek obsahujících média. Při importu se automaticky vytvoří všechna Média skupiny (i nepoužívaná) z důvodu, že se při importu stránek překládá i média skupina nastavená pro média aplikaci `/components/media/media.jsp` ve stránce (ta může obsahovat i ID média skupiny mimo importované stránky) (#57657-1).
- Firefox - snížená verze sady `Tabler Icons` na `3.0.1`, protože Firefox při použití novějších verzí výrazně zatěžuje procesor. Optimalizované čtení CSS stylu `vendor-inline.style.css` (#56393-19).

Zbývající seznam změn změn je shodný s verzí [2024.52](CHANGELOG-2024.md).

![meme](_media/meme/2025-0.jpg ":no-zoom")
