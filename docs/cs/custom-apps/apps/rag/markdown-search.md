# Vyhledávání v Markdown dokumentaci

Sémantické vyhledávání a RAG mohou pracovat i se soubory Markdown nasazenými ve webové aplikaci nebo uloženými v samostatné složce na serveru. Dokumentace WebJET CMS a klientská dokumentace mají samostatné kořeny. Markdown index je společný pro všechny domény. Vyhledávání respektuje přístupová práva aktuálního uživatele a vybraný jazyk. Docsify ve výchozím nastavení hledá v aktuálním adresáři a všech jeho podadresářích napříč přístupnými nakonfigurovanými kořeny. V dialogu lze omezení na adresář vypnout.

## Nasazení Docsify

Do rozbalené WAR aplikace zkopírujte Docsify shell ze složky `docs`: `index.jsp`, `index.html`, `style.css` a `assets/`. Shell používá původní history směrování; vnořené umístění vyžaduje úpravu konfigurace popsanou níže. Do stejného kořene přidejte jazykové složky s obsahem, například:

```text
/admin/docs/webjetcms/
    index.jsp
    index.html
    style.css
    assets/
    sk/
        README.md
        _sidebar.md
        _navbar.md
        redactor/...
    en/...
/admin/docs/orange/
    index.jsp
    index.html
    style.css
    assets/
    sk/...
```

Původní shell je nastaven pro veřejnou dokumentaci pod `/latest/`. V tomto nasazení otevírejte například `/latest/sk/` ; přechod z kořene dokumentace vybírá jazyk prohlížeče (`sk`, `cs`, jinak `en`). Vyhledávací pole má slovenské, české a anglické texty; při jiném jazykovém kódu použije anglický překlad.

Při vnořeném nasazení, například `/admin/docs/webjetcms/`, nestačí soubory pouze zkopírovat. V kopii `index.html` nastavte `basePath`, `<base href>` a cesty ke statickým souborům podle umístění dokumentace. Zachovejte `routerMode: 'history'` a na serveru nastavte přepis dokumentových adres, například `/admin/docs/webjetcms/sk/redactor/`, na příslušný `index.html`. Samotný welcome-file `index.jsp` tuto obsluhu nenahrazuje. Stávající veřejné adresy `/latest/...` a verzované adresy nadále obsluhuje původní `404.jsp`. U samostatné dokumentační domény musí být REST služba dostupná na stejném originu; samotný statický server nebo Tomcat bez WebJET backendu vyhledávání neposkytuje.

Původní konfigurace používá kořen `/` pro `127.0.0.1`, `localhost`, `iwcm.interway.sk` a `docs.interway.sk`. Na těchto adresách zůstávají odkazy `/sk/...`, `/en/...` a `/cs/...`. Server musí při otevření dokumentové adresy vrátit `index.html`, například pomocí fallbacku v `docsify serve`. Stávající `404.jsp` tuto obsluhu nezajišťuje pro adresy bez verze. U jiného hostitele nebo umístění upravte konfiguraci shellu; cesta se neurčuje automaticky z otevřené adresy.

Shell nadále načítá knihovny Docsify a jejich doplňky ze stávajících CDN. Do WAR nemusíte kopírovat `node_modules`, testy ani nástroje pro generování dokumentace.

## Konfigurace

Použijte stejnou [vektorovou databázi a AI poskytovatele](semantic-search/README.md) jako při vyhledávání webových stránek. Kořeny dokumentace a mapování souborových cest nastavte globálně, bez doménového omezení:

| Proměnná | Příklad | Význam |
| --- | --- | --- |
| `ragSemanticSearchEnabled` | `true` | Zapne sémantické vyhledávání. |
| `restAllowedIpAddresses-MarkdownSearchRestController` | Povolí volání Markdown REST služby ze všech IP adres. Lze zadat i IP adresy nebo jejich prefixy oddělené čárkou. Při výchozí prázdné hodnotě se použije globální `restAllowedIpAddresses` (výchozí `127.0.0.1`). | `ragMarkdownFolders` |
| `/admin/docs/client,file:/docs/cs/admin/users` | Globální seznam kořenů dokumentace pro všechny domény, oddělených čárkou nebo novým řádkem. Ve výchozím nastavení je prázdný. Prefix `file:` označuje složku na serveru; `file:/docs` je alias dokumentace nastaven přes `symlinkTranslate`. | `ragMarkdownSearchRequireLogin` |
| `true` | Ve výchozím nastavení vyžaduje přihlášeného uživatele; nemusí být administrátor. Hodnota `false` umožní anonymní vyhledávání iv nakonfigurovaných kořenech `file:` a `/admin/`. | `symlinkTranslate` |
| `/docs/\|/srv/documentation/webjetcms/` | Globální mapování aliasu `/docs/` na absolutní složku dokumentace. Pro `file:/docs` a jeho podsložky je povinné. | `ragAnswerAllowed` |
| `true` | Zapne automatické generování RAG odpovědi při vyhledávání. Při hodnotě `false` se zobrazí pouze nalezené dokumenty. Vyžaduje nakonfigurovaného asistenta pro odpovědi. | Zapne automatické generovanie RAG odpovede pri vyhľadávaní. Pri hodnote `false` sa zobrazia iba nájdené dokumenty. Vyžaduje nakonfigurovaného asistenta pre odpovede. |

Kořeny bez prefixu `file:` jsou cesty v rozbalené webové aplikaci **bez kontextové cesty aplikace**: pro URL `/cms/admin/docs/webjetcms` nastavte `/admin/docs/webjetcms`. Lze indexovat pouze podsložku, například `/admin/docs/webjetcms/sk/admin/users`. Backend pro odkazy najde nejbližší nadřazený Docsify `index.html` nebo `index.jsp` ; pokud chybí, použije indexovaný kořen. Cesty `..`, zakódované webové cesty a `WEB-INF` /`META-INF` nejsou povoleny.

Pro serverové soubory použijte absolutní cestu s prefixem `file:`, například `file:/srv/manuals`. Forma `file:///srv/manuals` se normalizuje na `file:/srv/manuals` ; relativní cesty a `file://server/...` nejsou podporovány. Samotná cesta `/Users/.../docs` označuje webovou cestu. Proces WebJETu musí mít právo číst složku; symbolické odkazy v jeho obsahu se nezpracovávají.

Alias ​​`file:/docs` a jeho podsložky vyžadují globální mapování `/docs/` v `symlinkTranslate`. Použijte mapování pro dané prostředí a zachovejte ostatní mapování na samostatných řádcích:

```text
/docs/|/srv/documentation/webjetcms/
```

Lokálně může cíl směřovat na složku `docs` vašeho projektu, například `/home/developer/webjetcms/docs/`. Musí to být existující absolutní složka. Alias ​​`file:/docs/sk/admin/users` pak označuje jeho podsložku `sk/admin/users` ; cesta zůstává omezena na mapovaný kořen. Bez mapování skončí alias chybou. Umístění se neodvozuje z pracovního adresáře ani struktury projektu. V konfiguraci, frontě i chuncích zůstává logická cesta, například `file:/docs/sk/admin/users/README.md`. Změna fyzického umístění téže dokumentace nevyžaduje nové indexy. Samostatný `sourceRoot` se neukládá.

**Dokumentaci rozdělte do jazykových složek**, například `sk`, `en` nebo `cs`. Jazyk se určuje pro každý soubor z nejbližší nadřazené složky s rozpoznaným názvem v celé cestě včetně nakonfigurovaného kořene. Jazyky určuje globální proměnná `languages` ; `sk`, `en` a `cs` jsou podporovány vždy. Název `sk-manual` se za jazyk nepovažuje. Soubory bez jazykové složky se při indexování přeskočí.

U kořene `file:/docs/sk/admin/users` dostane přímý soubor `README.md` jazyk `sk`. U kořene `file:/docs` se zpracují všechny jazykové podsložky. Indexují se i přímé `.md` soubory; sousední části dokumentace se neprohledávají. Jazyk se nevybírá v dialogu a neukládá se ve frontě, pouze v chuncích.

REST služba ve výchozím nastavení povoluje IP adresu `127.0.0.1` podle globálního `restAllowedIpAddresses`. V **Nastavení → Konfigurace** nastavte `restAllowedIpAddresses-MarkdownSearchRestController` na `*` pro všechny IP adresy nebo například na `192.168.,203.0.113.7` pro vybrané adresy. Používá stejnou kontrolu IP jako [veřejné REST služby](../../spring/public-services.md): neprázdné nastavení controlleru má přednost, při prázdném se použije globálně `restAllowedIpAddresses`. Globální hodnota `*` tak povolí i tuto službu, pokud nemá vlastní omezení. Nepovolené volání vrátí HTTP `401` ještě před vyhledáváním a voláním AI.

Po povolení IP adresy `ragMarkdownSearchRequireLogin` aktuální domény ve výchozím nastavení vyžaduje libovolného přihlášeného uživatele; hodnota `false` umožní také anonymní vyhledávání ve všech nakonfigurovaných kořenech včetně `/admin/` a `file:`. Blokované cesty a ochrana jednotlivých webových souborů uživatelskými skupinami se nadále respektují. Kontrola přihlášení proběhne před voláním embeddingů a generováním odpovědi AI.

API `/rest/rag/markdown/search` je mimo administrátorskou cestu. Nastavení přihlášení nemění ochranu samotného prohlížeče Docsify, souborů ani zprávy indexu. Pro běžné uživatele nebo anonymní přístup nasazujte obsah na přístupnou webovou cestu, například `/docs/webjetcms/`, nebo na samostatnou dokumentační doménu s WebJET backendem na stejném originu. Soubory pod `/admin/` nadále podléhají ochraně administrace.

Indexování `file:` samo nezpřístupní obsah přes HTTP. Výsledky obsahují název, úryvek a úplnou cestu `sourcePath`, ale `url` je `null`. Docsify pro `file:/docs/...` vytvoří odkaz do aktuálního prohlížeče, který musí poskytovat obsah mapované složky `/docs/`. Ostatní souborové kořeny zobrazují název bez odkazu.

Markdown používá stávající nastavení rozdělení textu, hybridního vyhledávání, podobnosti výsledků a RAG odpovědí. Chunky a vektory typu `MARKDOWN` se ukládají s `domainId = 0` ; identita zdroje se také počítá s doménou `0`. Webové stránky zůstávají typu `DOCUMENT` s ID své domény.

Fronta si uchová kladné ID domény, která požadavek vytvořila. Její asistent `RAG-EMB-INDEX`, konfigurace poskytovatele a statistiky spotřeby se použijí při zpracování. Vyhledávání používá asistenta `RAG-EMB-SEARCH` a spotřebu aktuální domény, stejně jako RAG odpověď. Asistenty upravíte v běžném seznamu AI asistentů příslušné domény.

Vyhledávací asistent musí používat stejného poskytovatele a model jako uložené embeddingy. Domény se stejnou kombinací sdílejí stávající vektory. Při jiné kombinaci je třeba vytvořit odpovídající index; dimenze musí odpovídat společné vektorové databázi.

Po nastavení shellu pro vnořené nasazení se kontext vyhledávání odvodí z jeho `basePath`: při `/cms/admin/docs/...` se použije `/cms`. Při jiném umístění, například `/cms/manual/`, doplňte do `window.$docsify` nastavení:

```javascript
markdownSearch: {
    contextPath: '/cms'
}
```

Plugin umožňuje nastavit i `endpoint` ; při správném `basePath` pod `/admin/docs/` jej není třeba zadávat. Toto nastavení vyhledávání nenahrazuje konfiguraci shellu a serverového přepisu adres. Kořeny vyhledávání určuje výhradně backend podle `ragMarkdownFolders` a přístupových práv. Prohlížeč odesílá jazyk aktuální stránky Docsify a volitelné omezení na aktuální adresář podle zvoleného odkazu v dialogu.

### Lokální náhled Docsify

Ve složce `docs` spusťte statický server:

```sh
npm run docs
```

Náhled na `http://127.0.0.1:3000` neposkytuje REST službu WebJETu. Na požadavek vyhledávání může vrátit HTML místo JSON; plugin tehdy zobrazí zprávu o nedostupném vyhledávání.

Pro vyhledávání i odpovědi AI nastavte globální mapování `/docs/` v `symlinkTranslate` na absolutní cestu ke složce `docs` poskytované statickým serverem. Nechte statický server běžet a ve druhém terminálu ve složce `docs` spusťte lokální proxy server. Pokud je WebJET dostupný na `http://iwcm.interway.sk`, použijte:

```sh
npm run docs:rag -- --backend http://iwcm.interway.sk
```

Otevřete `http://iwcm.interway.sk:3001/sk/`. Pokud je `ragMarkdownSearchRequireLogin=true`, nejprve se přihlaste přes přihlašovací stránku webu na `http://iwcm.interway.sk` ve stejném prohlížeči i profilu; postačuje běžný uživatelský účet. Administrátoři mohou použít také `/admin/`. Stejný název hostitele umožní použít existující přihlašovací cookie i na jiném portu. Při hodnotě `false` přihlášení není třeba. Vestavěný prohlížeč VS Code má samostatný pořad a přihlášení z běžného prohlížeče nepřevezme.

Pro výchozí produkční REST službu `https://docs.webjetcms.sk` stačí:

```sh
npm run docs:rag
```

Otevřete `http://127.0.0.1:3001/sk/`. Dokumentace se načte z lokálního statického serveru a vyhledávání použije `https://docs.webjetcms.sk/rest/rag/markdown/search`. Lokální náhled nesdílí přihlašovací cookies produkční domény, proto tento způsob vyžaduje povolené anonymní vyhledávání na backendu (`ragMarkdownSearchRequireLogin=false`).

Náhled můžete otevřít také prostřednictvím vlastního názvu hostitele směřujícího na `127.0.0.1`, například `http://docs.interway.sk:3001/sk/`. Cíl volání REST určuje parametr `--backend` s výchozí hodnotou `https://docs.webjetcms.sk`. Parametr `--docs` má výchozí hodnotu `http://127.0.0.1:3000` a `--port` hodnotu `3001`.

Proxy poslouchá pouze na lokálním rozhraní, zobrazuje náhled ze statického serveru a požadavky na vyhledávání posílá do WebJETu. V HTML náhledu nastaví kořen dokumentace na `/`, takže i při jiném názvu backendového hostitele používáte history adresy `/sk/...`. Kořen dokumentace se při spuštění nezadává. Nové vyhledávání v podadresáři se omezí na adresář aktuální stránky a jeho podadresáře. Například z adresy `http://iwcm.interway.sk:3001/sk/admin/README` se odešle `directory=/sk/admin/`. Odkaz **vyhledat ve všech adresářích** v dialogu zopakuje otázku bez tohoto filtru. V kořenovém adresáři jazyka, například `/sk/`, se filtr neposílá a odkaz se nezobrazuje. Soubory dokumentace, nastavení CORS ani ověřování přihlášení se nemění.

## Plánovaná indexace

V **Nastavení → Automatizované úlohy** přidejte úlohu:

```text
sk.iway.iwcm.rag.service.MarkdownIndexCronTask
```

Nastavte požadovaný čas, například denně ve 02:00: minuta `0`, hodina `2`, ostatní časová pole `*`. První naplnění indexu můžete provést ručním spuštěním tohoto úkolu. Tato úloha prohledává soubory přímo. Stávající `RagIndexCronTask` obsluhuje frontu webových stránek i ruční požadavky na indexaci a odstranění Markdown dokumentů.

Prvním volitelným parametrem úlohy je nakonfigurován kořen dokumentace, druhým název domény, jejíž asistent a statistiky se použijí. Parametry oddělte znakem `|`. Prázdný první parametr znamená všechny globálně nakonfigurované kořeny. Jazyk se u každého souboru určí z cesty, takže jedna úloha může zpracovat více jazyků. Pro lokální složku a konkrétní doménu použijte:

```text
file:/docs/sk/admin/users|www.example.sk
```

Ke zpracování všech kořenů se stejnou doménou použijte:

```text
|www.example.sk
```

V MultiWeb/cloud režimu je název domény povinen a musí patřit stávající doméně. Úloha sama nemá doménový kontext, proto se spotřeba nemůže odvodit z uživatele, který ji vytvořil. V režimu jedné domény lze druhý parametr vynechat; použije se doména `1`. Samotný kořen `file:/docs/sk/admin/users` nebo prázdné parametry tak zůstávají platné pouze v režimu jedné domény. Složka musí být přesně uvedena v globálním `ragMarkdownFolders`.

Při existujících úlohách odstraňte starší parametr jazyka a nastavte parametry v pořadí `koreň|doména`. Například `sk|file:/docs/sk/admin/users` nebo `sk||file:/docs/sk/admin/users` změňte na `file:/docs/sk/admin/users|www.example.sk`. Pokud úloha měla pouze parametr `sk`, nastavte pro zpracování všech kořenů `|www.example.sk`. Nahraďte příklad názvem skutečné domény, které má být zaznamenána spotřeba.

Při každém spuštění:

1. Prohledá vybraný kořen nebo nakonfigurované kořeny včetně jejich podsložek. Jazyk odvodí z cesty každého souboru. Zpracuje `.md`, vynechá soubory bez jazykové složky, navigační soubory (`_sidebar.md`, `_navbar.md`, `_coverpage.md`, `_footer.md`, `_404.md`, `404.md`), skryté složky a `node_modules`. Nenásleduje symbolické odkazy v obsahu.
2. Porovná SHA‑256 obsahu a aktuální části textu s uloženým indexem. Při nezměněném dokončeném dokumentu nevytváří nové embeddingy. Změna poskytovatele, modelu, dimenze nebo rozdělení textu vyvolá nové zpracování.
3. Při změně souboru znovu použije nezměněné embeddingy podle hashů jednotlivých částí. Zachová nadpisy, tabulky, příklady kódu a všechny HTML komentáře včetně komentářů mimo ukázek kódu. Vynechá YAML front matter.
4. Po úspěšném přečtení celého kořene odstraní index již neexistujících souborů i starší indexy souborů bez rozpoznané jazykové složky. Přejmenování se zpracuje jako nový soubor a odstranění původního. Prázdný soubor se z indexu odstraní.

Při chybě poskytovatele před uložením zůstává původní index. Nedokončené nebo vadné části se při dalším běhu zkoušejí znovu. Pokud kořen chybí nebo není možné dokončit čtení, jeho původní záznamy se hromadně nemažou. Odebrání kořene z konfigurace okamžitě zakáže jeho vyhledávání; uložená data samo neodstraní. Po změně dimenze a vymazání embeddingů spusťte úlohu znovu.

U více aplikačních uzlů naplánujte úlohu na jednom uzlu. Souběžná spuštění v rámci jednoho procesu jsou blokována. Chyby naleznete v serverovém logu pod třídami `MarkdownIndexCronTask` a `MarkdownIndexService`.

## Správa indexu v administraci

V **Nastavení → Sémantický index → ​​Markdown dokumenty** vyberte nakonfigurovaný kořen nebo podsložku a nastavte zahrnutí podsložek. Dialog převezme tento rozsah, zobrazí počty a zařadí indexaci nebo odstranění do fronty zpracované úlohou `RagIndexCronTask`. Indexování počítá pouze soubory s rozpoznaným jazykem; opětovné indexování aktualizuje i jazyk chunků a může znovu použít nezměněné embeddingy. Odstranění zahrnuje všechny indexy ve vybraném rozsahu bez ohledu na jazyk i záznamy již odstraněných souborů. Podrobnosti naleznete v [návodu pro správu sémantického indexu](../../../redactor/apps/semantic-search/embedding-chunks.md).

Při nasazení musí proběhnout aktualizace databáze `autoupdate-webjet9.xml`, které rozšíří identifikátory ve frontě na `BIGINT` a doplní údaje o zdrojovém souboru. Jazyk se ve frontě neukládá a zůstává součástí chunků. Přístup k nové kartě i jejím REST službám řídí stávající právo `embeddingChunks`.

Fronta i chunky používají pouze úplnou logickou cestu `sourcePath`, například `file:/docs/sk/admin/users/README.md` ; samostatný `sourceRoot` neukládají. Při zpracování fronty se kořen ověří vůči `ragMarkdownFolders` a identifikátoru dokumentu. Pole `sourcePath` umožňuje 1537 znaků. Stávající Markdown požadavky a chunky s relativní cestou se automaticky nekonvertují; před novým indexováním je odstraňte.

Požadavky ve frontě se pro vybraný společný zdroj nahrazují bez ohledu na doménu, která je vytvořila. Nový požadavek si uloží doménu aktuálního uživatele. Odstranění tak zruší i čekající indexaci stejného zdroje z jiné domény.

Při přechodu ze starší verze:

- Znovu zařaďte čekající Markdown požadavky s `domainId = 0` nebo s identitou zdroje vypočítanou s nenulovou doménou. Doména ve frontě musí označovat vlastníka spotřeby, identita zdroje se počítá s doménou `0`.
- Společné chunky a vektory s `domainId = 0` zůstávají platné. Markdown chunky s nenulovou doménou se nepoužívají; dokumentaci indexujte znovu a staré chunky pak odstraňte. Samotná změna `domainId` přes SQL nestačí.
- Ověřte jazykové složky. Starší požadavky na indexování bez rozpoznaného jazyka zůstanou ve frontě s chybou; upravte cestu a zařaďte je znovu nebo požadavky odstraňte. Úspěšná plánovaná indexace odstraní i starší chunky bez jazykové složky.

## REST vyhledávání

Po IP kontrole se jako první ověří dostupnost sémantického vyhledávání. Pokud není dostupné, API vrátí HTTP `503` před validací parametrů, načtením kořenů, kontrolou cache a voláním AI. Kontrola platí i pro požadavky, jejichž výsledky jsou uloženy v cache.

```http
GET /rest/rag/markdown/search?query=How%20to%20edit%20a%20page&language=en
```

Parametry `query` a `language` jsou povinné. Prohledávají se všechny přístupné kořeny nakonfigurované přes `ragMarkdownFolders` ve vybraném jazyce, případně omezené parametrem `directory`. Kořen dokumentace se v požadavku neposílá. `query` může mít 1 až 2000 znaků. Generování odpovědi AI řídí automaticky konfigurační proměnná `ragAnswerAllowed` ; parametr požadavku `answer` se nepoužívá. API vrátí nejvýše 10 dokumentů:

```json
{
  "results": [
    {
      "title": "Page editor",
      "url": "/admin/docs/webjetcms/#/en/redactor/webpages/",
      "sourcePath": "/admin/docs/webjetcms/en/redactor/webpages/README.md",
      "snippet": "The page editor allows...",
      "score": 0.82
    }
  ],
  "answer": "Open the page editor..."
}
```

Volitelný parametr `directory` omezuje vyhledávání na adresář aktuální stránky včetně všech podadresářů. Obsahuje jazyk a úvodní i koncový lomítko, například `/sk/admin/`:

```http
GET /rest/rag/markdown/search?query=users&language=sk&directory=%2Fsk%2Fadmin%2F
```

Backend přeloží adresář na cestu v každém přístupném kořeni. U kořene `file:/docs` se hledá pod `file:/docs/sk/admin/` ; u kořene `/admin/docs/webjetcms` pod `/admin/docs/webjetcms/sk/admin/`. Je-li nakonfigurován pouze hlubší kořen, například `file:/docs/sk/admin/users`, prohledává se pouze tento kořen. Sousední adresáře se nezahrnou. Filtr se aplikuje před seřazením a limitováním výsledků ve vektorovém i fulltextovém vyhledávání a omezuje i kontext odpovědi AI. Bez `directory` se adresář neomezuje. Jazyk v cestě musí odpovídat parametru `language` ; relativní cesty a segmenty `..` nejsou povoleny.

`answer` je obyčejný text nebo `null`, pokud je `ragAnswerAllowed` vypnuto nebo se odpověď nepodařilo sestavit z dostupného kontextu. Pole `answerHtml` zachovává bezpečné formátování odpovědi: odstavce, zvýraznění, seznamy, tabulky, ukázky kódu a rozbalovací detaily. Neobsahuje atributy, odkazy, obrázky ani spustitelný obsah. RAG obdrží pouze přístupné části ze společného indexu pro přístupné kořeny a vybraný jazyk, včetně názvu a odkazu na zdroj. Generování odpovědi používá asistenta a nastavení aktuální domény. Výsledky nejsou cachovány v prohlížeči (`Cache-Control: no-store`). Neplatné parametry vracejí HTTP 400. Pokud je přihlášení povinné a uživatel není přihlášen, API vrátí HTTP 401. Zablokovaný přístup ke všem nakonfigurovaným kořenům vrací HTTP 403.

Vyhledávání používá společnou [ochranu před SPAMem](../../../install/config/spam-protection.md) aplikace vyhledávání. Proměnná `spamProtectionTimeout-search` určuje minimální odstup mezi požadavky (výchozí 10 sekund) a `spamProtectionHourlyLimit-search` počet požadavků za hodinu (výchozí 200). Limity se počítají podle IP adresy v rámci jedné instance aplikace a sdílejí se s běžným vyhledáváním. Platí i pro přihlášené uživatele. Při překročení limitu API vrátí HTTP 429 ještě před voláním embeddingů nebo generováním odpovědi AI. Docsify zobrazí zprávu o limitu; požadavek automaticky neopakuje.

Docsify namísto Google Custom Search zobrazuje vlastní vyhledávací pole bez přepínače odpovědi AI. Výsledky a odpověď AI povolená přes `ragAnswerAllowed` se zobrazí v dialogu; kliknutím na název výsledku se otevře příslušná stránka v nové kartě a dialog zůstane otevřený. Pro zdroje `file:/docs/...` se úplná cesta `sourcePath` převede na odkaz do aktuálního prohlížeče dokumentace, i když je indexována pouze podsložka. Ostatní souborové kořeny bez veřejné adresy se zobrazují jako text. Dialog lze zavřít také klávesou Escape.

Náhled nalezené části dokumentu zachovává Markdown formátování: nadpisy, tučné písmo, kurzívu, seznamy, citace a ukázky kódu. Pole `snippet` obsahuje nejvýše 350 znaků původního Markdown textu se zachovanými zalomeními řádků a při zkrácení znak `…`. Prohlížeč jej vykreslí stávajícím parserem Docsify; nadpisy mají v náhledu kompaktní velikost. Opakovaný název na začátku náhledu se vynechá. Pokud náhled začíná názvem a cestou k sekci, cesta se zobrazí samostatně pod odkazem a shodné úvodní nadpisy se neopakují. Odkaz na příslušnou sekci zůstává zachován. HTML ze zdroje se zobrazí jako text, při odkazech a obrázcích se zachová pouze jejich textový popis. Změna zobrazení nevyžaduje opětovné indexování.

V záhlaví dialogu je otázka v upravitelném poli. Po úpravě spustíte nové vyhledávání klávesou Enter nebo ikonou lupy na konci pole, bez zavření dialogu. Zachová se aktuální rozsah i jazyk a otázka se aktualizuje iv postranním panelu. Pod polem je rozsah vyhledávání a jazyk. Odpověď AI je v jemně fialové kartě nad seznamem **Nalezené dokumenty** s počtem zobrazených výsledků. Zachovává formátování asistenta: postupy mají zvýrazněná čísla kroků, pole mohou být v kompaktní tabulce a ukázky kódu jsou v samostatných panelech. Delší volitelné příklady může asistent vložit do rozbalovacích detailů. Pokud chybí `answerHtml`, zobrazí se původní text. Nový výchozí asistent používá pro postupy číslované kroky, zvýrazněné názvy polí, tabulky s popisem polí a krátké ukázky kódu. Stávající asistenti si zachovají uložené instrukce; stejný styl lze nastavit jejich úpravou. Dokumenty jsou v samostatných kartách s ikonou, náhledem a cestou k sekci nebo zdrojovému adresáři. Pokud odpověď není dostupná, karta AI se nezobrazuje. Otázka zůstává viditelná i během načítání, při prázdných výsledcích a při chybě. Při posouvání výsledků zůstává záhlaví s otázkou a tlačítkem pro zavření na místě.

Nové vyhledávání v podadresáři se omezí na adresář aktuální stránky a všechny jeho podadresáře: `/latest/sk/admin/README` i `/latest/sk/admin/` odešlou `/sk/admin/`. Odkaz **vyhledat ve všech adresářích** zopakuje stejnou otázku bez `directory` ; opačný odkaz obnoví adresářový filtr. V kořenovém adresáři jazyka, například `/latest/sk/` nebo `/latest/sk/README`, se zobrazí **Celá dokumentace · Čeština**, filtr `directory` se neposílá a odkaz na rozšíření rozsahu se nezobrazuje. Vyhledávání zůstává omezeno na vybraný jazyk a přístupné kořeny. Přepnutí rozsahu zachová otázku i jazyk, nahradí výsledky i odpověď AI a nemění otevřenou stránku. Odkazy fungují i ​​při prázdných výsledcích.

### Serverová cache vyhledávání

Backend uchovává vyhledané chunky a odpověď AI v `Cache` po dobu 30 minut. Pro klíč cache převede otázku na malá písmena a odstraní diakritiku, takže například `Ďalšia úloha` a `dalsia uloha` použijí stejný záznam. Vyhledávání a generování odpovědi dostanou původní otázku. Záznamy jsou odděleny podle domény, jazyka, výsledných prohledávaných cest a kontextové cesty aplikace. Opakované použití záznamu neprodlužuje jeho platnost.

Pokud vyhledávání nevrátí žádné chunky, výsledek se neukládá do cache. Další požadavek tak zopakuje vyhledávání i po dočasném výpadku poskytovatele embeddingů nebo vektorové databáze.

Pokud některý uložený chunk po opětovném indexování nebo smazání již neexistuje v databázi, backend vyřadí uložené výsledky i odpověď a zopakuje vyhledávání. Aktualizované dokumenty jsou tak dostupné i při opakování stejné otázky před uplynutím 30 minut.

Při každém požadavku se znovu ověří přístup ke zdrojům a aktuální hodnota `ragAnswerAllowed`. Po vypnutí odpovědí AI se nevrátí ani uložená odpověď. Po zapnutí se chybějící odpověď vygeneruje z uložených přístupných chunků bez opakování vyhledávání. Pokud se změní seznam přístupných chunků, předchozí odpověď se nepoužije. Neúspěšné generování odpovědi se neukládá a další požadavek jej může zopakovat. Ochrana před SPAMem platí i při použití cache; prohlížeč odpovědi nadále neukládá.

## Ověření změn

Backendové regresní testy:

```sh
./gradlew test --tests 'sk.iway.iwcm.rag.*'
```

Test rozhraní se skutečným Docsify a simulovanou REST odpovědí i test lokálního proxy serveru:

```sh
node --test docs/tests/*.test.cjs
```

Testy potřebují Node.js 20 nebo novější a nainstalované závislosti `docs/` a `src/test/webapp/` včetně prohlížeče Playwright. Při nasazení ověřte i skutečnou indexaci, poskytovatele embeddingů a RAG odpovědí na cílové databázi.

Test nové karty a dialogů ve frontě spusťte proti lokálnímu WebJETu po načtení aktuálních Java tříd a překladů:

```sh
cd src/test/webapp
npm run all tests/rag/markdown-embedding-chunks.js
```

Test používá simulované odpovědi Markdown fronty a tabulky, takže neposílá požadavky AI poskytovateli. Metadata sloupců a kontrolu přístupu ověřuje ve spuštěné aplikaci.
