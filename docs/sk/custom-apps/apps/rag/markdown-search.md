# Vyhľadávanie v Markdown dokumentácii

Sémantické vyhľadávanie a RAG môžu pracovať aj so súbormi Markdown nasadenými vo webovej aplikácii alebo uloženými v samostatnom priečinku na serveri. Dokumentácia WebJET CMS a klientská dokumentácia majú samostatné korene. Markdown index je spoločný pre všetky domény. Vyhľadávanie rešpektuje prístupové práva aktuálneho používateľa a vybraný jazyk. Docsify predvolene hľadá v aktuálnom adresári a všetkých jeho podadresároch naprieč prístupnými nakonfigurovanými koreňmi. V dialógu možno obmedzenie na adresár vypnúť.

## Nasadenie Docsify

Do rozbalenej WAR aplikácie skopírujte Docsify shell z priečinka `docs`: `index.jsp`, `index.html`, `style.css` a `assets/`. Do rovnakého koreňa pridajte jazykové priečinky s obsahom, napríklad:

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

Po otvorení `/admin/docs/webjetcms` Tomcat použije existujúci welcome-file `index.jsp`. Docsify prejde na jazyk prehliadača (`sk`, `cs`, inak `en`). Ak nasadzujete iba slovenský obsah, používajte priamy odkaz `/admin/docs/webjetcms/#/sk/`. Ďalšie jazyky sú dostupné priamym odkazom, napríklad `#/de/`; texty vyhľadávania majú v takom prípade anglický náhradný preklad.

Vnorené nasadenie používa odkazy s `#/`, napríklad `/admin/docs/webjetcms/#/sk/redactor/`. Nevyžaduje vlastný 404 handler. Existujúce verejné adresy `/latest/...` a `/v2026/...` používajú pôvodné history smerovanie a existujúci `404.jsp`. Pri samostatnej dokumentačnej doméne musí byť REST služba dostupná na rovnakom origine; samotný statický server alebo Tomcat bez WebJET backendu vyhľadávanie neposkytuje.

Pri nasadení dokumentácie v koreňovom priečinku zostávajú podporované aj pôvodné adresy `/sk/...`, `/en/...` a `/cs/...` bez `#/`. Používajú history smerovanie a načítavajú súbory prehliadača z koreňa `/`. Server musí pri otvorení takejto adresy vrátiť `index.html`, napríklad pomocou fallbacku v `docsify serve`. Existujúci `404.jsp` túto obsluhu nezabezpečuje pre adresy bez verzie.

Shell naďalej načítava knižnice Docsify a ich doplnky z existujúcich CDN. Do WAR nemusíte kopírovať `node_modules`, testy ani nástroje na generovanie dokumentácie.

## Konfigurácia

Použite rovnakú [vektorovú databázu a AI poskytovateľa](semantic-search/README.md) ako pri vyhľadávaní webových stránok. Korene dokumentácie a mapovanie súborových ciest nastavte globálne, bez doménového obmedzenia. Markdown index je spoločný, ale embeddingoví asistenti a štatistiky spotreby patria konkrétnej doméne. Indexovanie používa asistenta `RAG-EMB-INDEX` domény, ktorá požiadavku zaradila do fronty alebo je nastavená v plánovanej úlohe. Vyhľadávanie používa asistenta `RAG-EMB-SEARCH` aktuálnej domény. Asistentov upravíte v bežnom zozname AI asistentov príslušnej domény. Nastavte:

| Premenná | Príklad | Význam |
| --- | --- | --- |
| `ragSemanticSearchEnabled` | `true` | Zapne sémantické vyhľadávanie. |
| `ragMarkdownFolders` | `/admin/docs/client,file:/docs/sk/admin/users` | Globálny zoznam koreňov dokumentácie pre všetky domény, oddelených čiarkou alebo novým riadkom. Predvolene je prázdny. Prefix `file:` označuje priečinok na serveri; `file:/docs` je alias dokumentácie nastavený cez `symlinkTranslate`. |
| `ragMarkdownSearchRequireLogin` | `true` | Predvolene vyžaduje prihláseného používateľa; nemusí byť administrátor. Hodnota `false` umožní anonymné vyhľadávanie aj v nakonfigurovaných koreňoch `file:` a `/admin/`. |
| `symlinkTranslate` | `/docs/\|/srv/documentation/webjetcms/` | Globálne mapovanie aliasu `/docs/` na absolútny priečinok dokumentácie. Pre `file:/docs` a jeho podpriečinky je povinné. |
| `ragAnswerAllowed` | `true` | Zapne automatické generovanie RAG odpovede pri vyhľadávaní. Pri hodnote `false` sa zobrazia iba nájdené dokumenty. Vyžaduje nakonfigurovaného asistenta pre odpovede. |

Korene bez prefixu `file:` sú cesty vo webovej aplikácii, **bez kontextovej cesty aplikácie**. Môžu smerovať priamo na požadovanú časť dokumentácie aj hlbšie pod jazykovým priečinkom. Napríklad pri URL `/cms/admin/docs/webjetcms` sa konfiguruje `/admin/docs/webjetcms`. Dokumentácia v tomto prípade musí byť dostupná ako súbory v rozbalenej webovej aplikácii. Cesty `..`, zakódované webové cesty a `WEB-INF`/`META-INF` nie sú povolené.

Indexovaný koreň nemusí obsahovať samotný prehliadač Docsify. Ak je v `ragMarkdownFolders` iba `/admin/docs/webjetcms/sk/admin/users`, ale Docsify je nasadený v `/admin/docs/webjetcms/`, ďalšie nastavenie koreňa v prehliadači nie je potrebné.

Vyhľadávanie zostane obmedzené na tento podpriečinok. Výsledok `perm-groups.md` sa otvorí cez `/admin/docs/webjetcms/#/sk/admin/users/perm-groups`. Backend hľadá najbližší nadradený `index.html` alebo `index.jsp` s konfiguráciou Docsify; ak ho v nasadených súboroch nenájde, zachová pôvodnú adresu odvodenú od indexovaného koreňa. Plugin skladá odkazy z úplnej cesty zdroja vzhľadom na aktuálny prehliadač a rešpektuje jeho hash alebo history smerovanie. Kontextová cesta aplikácie, napríklad `/cms`, sa doplní automaticky.

Pre dokumentáciu uloženú na rôznych miestach v jednotlivých prostrediach použite rovnaký alias:

```text
file:/docs/sk/admin/users
```

Do globálnej premennej `symlinkTranslate` pridajte mapovanie `/docs/` na fyzické umiestnenie dokumentácie. Napríklad lokálne:

```text
/docs/|/home/developer/webjetcms/docs/
```

Na serveri môže ten istý alias smerovať na iný priečinok:

```text
/docs/|/srv/documentation/webjetcms/
```

Použite mapovanie pre dané prostredie; ďalšie existujúce mapovania v `symlinkTranslate` zachovajte na samostatných riadkoch. Cieľ musí byť absolútny existujúci priečinok. WebJET preloží základ `/docs/` pomocou `FileTools.symlinkReplaceToRootPath` a až potom pripojí zvolený podpriečinok, napríklad `sk/admin/users`. Výsledná cesta musí zostať v mapovanom priečinku dokumentácie. Mapovanie sa vždy číta globálne, nezávisle od aktuálnej domény.

Bez mapovania alias skončí chybou; cesta sa už neodvodzuje z umiestnenia projektu ani z pracovného adresára servera. Možno použiť aj samotné `file:/docs` alebo iný jeho podpriečinok. V konfigurácii, fronte aj chunkoch zostáva logická cesta `file:/docs/...`. Ak mapovanie smeruje na tú istú dokumentáciu ako predtým, existujúce indexy netreba vytvárať znova. Ostatné cesty s prefixom `file:` sa naďalej interpretujú ako absolútne cesty na serveri, napríklad `file:/srv/manuals/sk/admin/users`.

Indexujú sa súbory `.md` priamo v nastavenom priečinku aj v jeho podpriečinkoch. V uvedenom príklade sa súbor `docs/sk/admin/users/README.md` vo fronte aj v chunkoch uloží ako `sourcePath = file:/docs/sk/admin/users/README.md`. Samostatný `sourceRoot` sa neukladá. Susedné časti dokumentácie sa neprehľadávajú.

**Dokumentáciu rozdeľte do jazykových priečinkov**, napríklad `sk`, `en` alebo `cs`. Jazyk sa odvodzuje pre každý súbor z celej cesty tvorenej nakonfigurovaným koreňom a relatívnou cestou súboru. Rozhoduje najbližší nadradený priečinok, ktorého celý názov zodpovedá rozpoznanému jazyku. Rozpoznané jazyky určuje globálna konfiguračná premenná `languages`; kódy `sk`, `en` a `cs` sú podporované aj bez uvedenia v tejto premennej. Kód `cs` sa uloží ako `cs`. Názvy ako `sk-manual` sa za jazyk nepovažujú. Súbory bez rozpoznaného jazykového priečinka sa pri indexovaní preskočia.

Pri koreni `file:/docs/sk/admin/users` má aj priamy súbor `README.md` jazyk `sk`. Pri koreni `file:/docs` možno jednou akciou indexovať `sk/admin/users/README.md`, `en/admin/users/README.md` aj `cs/admin/users/README.md`; každý súbor dostane jazyk zo svojej cesty. Jazyk sa v dialógu nevyberá a vo fronte sa neukladá. Odvodená hodnota zostáva uložená v chunkoch, aby bolo možné vyhľadávať podľa jazyka.

Proces WebJETu musí mať právo čítať nastavený priečinok; symbolické odkazy v jeho obsahu sa nespracúvajú. Priečinky `file:` sa zobrazia aj na karte **Markdown dokumenty** a používajú rovnakú frontu aj plánovanú indexáciu.

Prefix rozlišuje cestu servera od webovej cesty: samotná hodnota `/Users/.../docs` sa stále interpretuje ako cesta vo webovej aplikácii. Akceptovaná je aj lokálna forma `file:///Users/.../docs`; normalizuje sa na `file:/Users/.../docs`. Relatívne cesty a sieťové adresy `file://server/...` nie sú podporované.

Prihlásenie pre API riadi konfiguračná premenná `ragMarkdownSearchRequireLogin` aktuálnej domény. Predvolená hodnota `true` vyžaduje ľubovoľného prihláseného používateľa bez požiadavky na administrátorské oprávnenia. Pri hodnote `false` môžu vyhľadávať aj neprihlásení návštevníci. Nastavenie platí pre všetky korene vrátane `/admin/` a `file:`. Blokované cesty a explicitná ochrana jednotlivých webových súborov pomocou používateľských skupín sa naďalej rešpektujú. Kontrola prihlásenia prebehne pred volaním embeddingov a generovaním odpovede AI.

Vyhľadávacia služba je dostupná na `/rest/rag/markdown/search`, mimo administrátorskej cesty. Konstanta riadi iba vyhľadávanie, nemení ochranu samotného prehliadača Docsify, Markdown súborov ani správy indexu. Ak má byť dokumentácia dostupná aj bežným používateľom alebo anonymne, nasadzujte prehliadač a obsah na prístupnú webovú cestu, napríklad `/docs/webjetcms/`, alebo použite samostatnú dokumentačnú doménu. Priame otvorenie súborov pod `/admin/` naďalej podlieha ochrane administrácie.

Indexovanie lokálneho priečinka samo nesprístupní súbory cez HTTP. REST výsledky pre `file:` obsahujú názov, úryvok a úplnú cestu `sourcePath`; hodnota `url` zostáva `null`. Pre alias `file:/docs/...` Docsify vytvorí z `sourcePath` odkaz na súbor v aktuálnom prehliadači dokumentácie, ktorý musí poskytovať obsah mapovaného priečinka `/docs/`. Kliknutím na názov sa dokument otvorí v novej karte. Ostatné súborové korene bez verejnej adresy zobrazujú názov bez odkazu.

Používajú sa existujúce nastavenia embeddingov, veľkosti a prekrytia častí textu, hybridného vyhľadávania, podobnosti výsledkov a RAG odpovedí. Markdown má samostatný typ `MARKDOWN`; iba jeho chunky a vektory sa ukladajú s `domainId = 0`, čo označuje spoločné dáta pre všetky domény. Požiadavka vo fronte si uchová kladné ID domény, ktorá ju vytvorila. Jej asistent, konfigurácia AI poskytovateľa a štatistiky spotreby sa použijú aj pri neskoršom spracovaní fronty. Spotreba vyhľadávania sa zaznamená aktuálnej doméne. Identita spoločného Markdown zdroja sa naďalej počíta s doménou `0`, nezávisle od domény vykonávanej operácie. Indexovanie webových stránok ostáva typu `DOCUMENT` s kladným ID príslušnej domény. Asistent pre RAG odpovede a kontrola prístupových práv naďalej používajú kontext domény aktuálnej požiadavky.

Asistent vyhľadávania musí používať rovnakého poskytovateľa a model ako uložené embeddingy; vyhľadávanie nimi filtruje výsledky. Ak viaceré domény používajú tú istú kombináciu, zdieľajú existujúce vektory a samotné prepnutie domény nevyžaduje ďalšiu indexáciu. Pri inej kombinácii poskytovateľa a modelu treba vytvoriť zodpovedajúce embeddingy; dimenzia musí zodpovedať konfigurácii spoločnej vektorovej databázy.

Pri nasadení pod kontextovou cestou, napríklad `/cms`, sa kontext pre `/cms/admin/docs/...` odvodí automaticky. Pri inom umiestnení, napríklad `/cms/manual/`, doplňte do `window.$docsify` nastavenie:

```javascript
markdownSearch: {
    contextPath: '/cms'
}
```

Plugin umožňuje nastaviť aj `endpoint`; štandardné nasadenie pod `/admin/docs/` ho nepotrebuje. Korene vyhľadávania určuje výhradne backend podľa `ragMarkdownFolders` a prístupových práv. Prehliadač odosiela jazyk aktuálnej stránky Docsify a voliteľné obmedzenie na aktuálny adresár podľa zvoleného odkazu v dialógu.

### Lokálny náhľad Docsify

V priečinku `docs` spustite statický server:

```sh
npm run docs
```

Náhľad na `http://127.0.0.1:3000` neposkytuje REST službu WebJETu. Na požiadavku vyhľadávania môže vrátiť HTML namiesto JSON; plugin vtedy zobrazí správu o nedostupnom vyhľadávaní.

Pre vyhľadávanie aj odpovede AI nastavte globálne mapovanie `/docs/` v `symlinkTranslate` na absolútnu cestu k priečinku `docs` poskytovanému statickým serverom. Nechajte statický server bežať a v druhom termináli v priečinku `docs` spustite lokálny proxy server. Ak je WebJET dostupný na `http://iwcm.interway.sk`, použite:

```sh
npm run docs:rag -- --backend http://iwcm.interway.sk
```

Otvorte `http://iwcm.interway.sk:3001/#/sk/`. Ak je `ragMarkdownSearchRequireLogin=true`, najprv sa prihláste cez prihlasovaciu stránku webu na `http://iwcm.interway.sk` v rovnakom prehliadači aj profile; postačuje bežný používateľský účet. Administrátori môžu použiť aj `/admin/`. Rovnaký názov hostiteľa umožní použiť existujúcu prihlasovaciu cookie aj na inom porte. Pri hodnote `false` prihlásenie netreba. Vstavaný prehliadač VS Code má samostatnú reláciu a prihlásenie z bežného prehliadača neprevezme.

Proxy počúva iba na lokálnom rozhraní, zobrazuje náhľad zo statického servera a požiadavky na vyhľadávanie posiela do WebJETu. Koreň dokumentácie sa pri spustení nezadáva. Nové vyhľadávanie sa obmedzí na adresár aktuálnej stránky a jeho podadresáre. Napríklad z adresy `http://iwcm.interway.sk:3001/#/sk/admin/README` sa odošle `directory=/sk/admin/`. Odkaz **vyhľadať vo všetkých adresároch** v dialógu zopakuje otázku bez tohto filtra. Súbory dokumentácie, nastavenia CORS ani overovanie prihlásenia sa nemenia.

## Plánovaná indexácia

V **Nastavenia → Automatizované úlohy** pridajte úlohu:

```text
sk.iway.iwcm.rag.service.MarkdownIndexCronTask
```

Nastavte požadovaný čas, napríklad denne o 02:00: minúta `0`, hodina `2`, ostatné časové polia `*`. Prvé naplnenie indexu môžete vykonať ručným spustením tejto úlohy. Táto úloha prehľadáva súbory priamo. Existujúca `RagIndexCronTask` obsluhuje frontu webových stránok aj ručné požiadavky na indexáciu a odstránenie Markdown dokumentov.

Prvým voliteľným parametrom úlohy je nakonfigurovaný koreň dokumentácie, druhým názov domény, ktorej asistent a štatistiky sa použijú. Parametre oddeľte znakom `|`. Prázdny prvý parameter znamená všetky globálne nakonfigurované korene. Jazyk sa pri každom súbore určí z cesty, takže jedna úloha môže spracovať viac jazykov. Pre lokálny priečinok a konkrétnu doménu použite:

```text
file:/docs/sk/admin/users|www.example.sk
```

Na spracovanie všetkých koreňov s rovnakou doménou použite:

```text
|www.example.sk
```

V MultiWeb/cloud režime je názov domény povinný a musí patriť existujúcej doméne. Úloha sama nemá doménový kontext, preto sa spotreba nemôže odvodiť z používateľa, ktorý ju vytvoril. V režime jednej domény možno druhý parameter vynechať; použije sa doména `1`. Samotný koreň `file:/docs/sk/admin/users` alebo prázdne parametre tak zostávajú platné iba v režime jednej domény. Priečinok musí byť presne uvedený v globálnom `ragMarkdownFolders`.

Pri existujúcich úlohách odstráňte starší parameter jazyka a nastavte parametre v poradí `koreň|doména`. Napríklad `sk|file:/docs/sk/admin/users` alebo `sk||file:/docs/sk/admin/users` zmeňte na `file:/docs/sk/admin/users|www.example.sk`. Ak mala úloha iba parameter `sk`, na spracovanie všetkých koreňov nastavte `|www.example.sk`. Nahraďte príklad názvom skutočnej domény, ktorej sa má zaznamenať spotreba.

Pri každom spustení:

1. Prehľadá vybraný koreň alebo nakonfigurované korene vrátane ich podpriečinkov. Jazyk odvodí z cesty každého súboru. Spracuje `.md`, vynechá súbory bez jazykového priečinka, navigačné súbory (`_sidebar.md`, `_navbar.md`, `_coverpage.md`, `_footer.md`, `_404.md`, `404.md`), skryté priečinky a `node_modules`. Nenasleduje symbolické odkazy v obsahu.
2. Porovná SHA‑256 obsahu a aktuálne časti textu s uloženým indexom. Pri nezmenenom dokončenom dokumente nevytvára nové embeddingy. Zmena poskytovateľa, modelu, dimenzie alebo rozdelenia textu vyvolá nové spracovanie.
3. Pri zmene súboru znovu použije nezmenené embeddingy podľa hashov jednotlivých častí. Zachová nadpisy, tabuľky, príklady kódu a všetky HTML komentáre vrátane komentárov mimo ukážok kódu. Vynechá YAML front matter.
4. Po úspešnom prečítaní celého koreňa odstráni index už neexistujúcich súborov aj staršie indexy súborov bez rozpoznaného jazykového priečinka. Premenovanie sa spracuje ako nový súbor a odstránenie pôvodného. Prázdny súbor sa z indexu odstráni.

Pri chybe poskytovateľa pred uložením zostáva pôvodný index. Nedokončené alebo chybné časti sa pri ďalšom behu skúšajú znovu. Ak koreň chýba alebo nie je možné dokončiť čítanie, jeho pôvodné záznamy sa hromadne nemažú. Odobratie koreňa z konfigurácie okamžite zakáže jeho vyhľadávanie; uložené dáta samo neodstráni. Po zmene dimenzie a vymazaní embeddingov spustite úlohu znova.

Pri viacerých aplikačných uzloch naplánujte úlohu na jednom uzle. Súbežné spustenia v rámci jedného procesu sú blokované. Chyby nájdete v serverovom logu pod triedami `MarkdownIndexCronTask` a `MarkdownIndexService`.

## Správa indexu v administrácii

V **Nastavenia → Sémantický index → Markdown dokumenty** môžete v strome vybrať jeden z koreňov v `ragMarkdownFolders` alebo jeho podpriečinok, prezerať časti dokumentov a zaradiť indexáciu alebo odstránenie do fronty. Prepínač **Zahrnúť aj podpriečinky** nastavuje rozsah tabuľky aj dialógu; po vypnutí sa použijú iba súbory priamo vo vybranom priečinku. Dialóg prevezme oba výbery a umožní ich upraviť. Jazyk sa pri indexovaní odvodí z cesty súboru a dialóg zobrazuje pravidlo rozdelenia dokumentácie do jazykových priečinkov. Súbory bez rozpoznaného jazykového priečinka sa nezaradia na indexovanie ani do počtov pre túto akciu. Opätovné indexovanie aktualizuje odvodený jazyk chunkov aj pri nezmenenom texte; existujúce embeddingy sa môžu použiť znova. Odstránenie odstráni všetky indexy súborov iba vo vybranom rozsahu bez ohľadu na jazyk, vrátane už odstránených súborov. Výber podpriečinka nemení identitu zdroja ani úplnú cestu uloženú v `sourcePath`. Tlačidlá zobrazia počty dokumentov a požiadaviek vo fronte; samotnú operáciu vykoná `RagIndexCronTask`. Podrobnosti nájdete v [návode na správu sémantického indexu](../../../redactor/apps/semantic-search/embedding-chunks.md).

Pri nasadení musia prebehnúť aktualizácie databázy `autoupdate-webjet9.xml`, ktoré rozšíria identifikátory vo fronte na `BIGINT` a doplnia údaje o zdrojovom súbore. Jazyk sa vo fronte neukladá a zostáva súčasťou chunkov. Prístup k novej karte aj jej REST službám riadi existujúce právo `embeddingChunks`.

Fronta aj chunky používajú iba úplnú logickú cestu `sourcePath`, napríklad `file:/docs/sk/admin/users/README.md`; samostatný `sourceRoot` neukladajú. Pri spracovaní fronty sa koreň overí voči `ragMarkdownFolders` a identifikátoru dokumentu. Pole `sourcePath` umožňuje 1537 znakov. Existujúce Markdown požiadavky a chunky s relatívnou cestou sa automaticky nekonvertujú; pred novým indexovaním ich odstráňte.

Požiadavky vo fronte sa pre vybraný spoločný zdroj nahrádzajú bez ohľadu na doménu, ktorá ich vytvorila. Nová požiadavka si uloží doménu aktuálneho používateľa. Odstránenie tak zruší aj čakajúcu indexáciu rovnakého zdroja z inej domény.

Pri prechode zo staršej verzie odstráňte čakajúce požiadavky typu `MARKDOWN` s `domainId = 0` a zaraďte ich znova v požadovanej doméne. Nemajú uloženého vlastníka spotreby a nespracujú sa. Znova zaraďte aj staršie požiadavky, ktorých identita zdroja bola vypočítaná s nenulovou doménou. Existujúce spoločné chunky a vektory s `domainId = 0` zostávajú platné; ich identifikátory nemeňte. Markdown chunky s nenulovým `domainId` sa v spoločnom indexe nepoužívajú; dokumentáciu indexujte znova a pôvodné chunky možno následne odstrániť. Nemeňte iba `domainId` pomocou SQL: identita spoločného zdroja musí byť vypočítaná s doménou `0`, zatiaľ čo doména vo fronte určuje vlastníka spotreby. Pred opätovným indexovaním overte jazykové priečinky; predchádzajúci ručne vybraný jazyk sa už nepoužíva. Čakajúce požiadavky spoločného indexu odvodia jazyk pri spracovaní z uloženej cesty. Staršie požiadavky na indexovanie bez rozpoznaného jazykového priečinka skončia chybou a zostanú vo fronte na ďalší pokus; upravte štruktúru dokumentácie a zaraďte správnu cestu alebo požiadavku odstráňte. Úspešná plánovaná indexácia odstráni aj staršie chunky súborov, ktorých cesty nespĺňajú pravidlo jazykových priečinkov.

## REST vyhľadávanie

```http
GET /rest/rag/markdown/search?query=How%20to%20edit%20a%20page&language=en
```

Parametre `query` a `language` sú povinné. Prehľadávajú sa všetky prístupné korene nakonfigurované cez `ragMarkdownFolders` vo vybranom jazyku, prípadne obmedzené parametrom `directory`. Koreň dokumentácie sa v požiadavke neposiela. `query` môže mať 1 až 2000 znakov. Generovanie odpovede AI riadi automaticky konfiguračná premenná `ragAnswerAllowed`; parameter požiadavky `answer` sa nepoužíva. API vráti najviac 10 dokumentov:

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

Voliteľný parameter `directory` obmedzuje vyhľadávanie na adresár aktuálnej stránky vrátane všetkých podadresárov. Obsahuje jazyk a úvodnú aj koncovú lomku, napríklad `/sk/admin/`:

```http
GET /rest/rag/markdown/search?query=users&language=sk&directory=%2Fsk%2Fadmin%2F
```

Backend preloží adresár na cestu v každom prístupnom koreni. Pri koreni `file:/docs` sa hľadá pod `file:/docs/sk/admin/`; pri koreni `/admin/docs/webjetcms` pod `/admin/docs/webjetcms/sk/admin/`. Ak je nakonfigurovaný iba hlbší koreň, napríklad `file:/docs/sk/admin/users`, prehľadáva sa iba tento koreň. Susedné adresáre sa nezahrnú. Filter sa aplikuje pred zoradením a limitovaním výsledkov vo vektorovom aj fulltextovom vyhľadávaní a obmedzuje aj kontext odpovede AI. Bez `directory` sa adresár neobmedzuje. Jazyk v ceste musí zodpovedať parametru `language`; relatívne cesty a segmenty `..` nie sú povolené.

`answer` je obyčajný text alebo `null`, ak je `ragAnswerAllowed` vypnuté alebo sa odpoveď nepodarilo zostaviť z dostupného kontextu. RAG dostane iba prístupné časti zo spoločného indexu pre prístupné korene a vybraný jazyk, vrátane názvu a odkazu na zdroj. Generovanie odpovede používa asistenta a nastavenia aktuálnej domény. Výsledky nie sú cachované v prehliadači (`Cache-Control: no-store`). Neplatné parametre vracajú HTTP 400. Ak je prihlásenie povinné a používateľ nie je prihlásený, API vráti HTTP 401. Zablokovaný prístup ku všetkým nakonfigurovaným koreňom vracia HTTP 403.

Docsify namiesto Google Custom Search zobrazuje vlastné vyhľadávacie pole bez prepínača odpovede AI. Výsledky a odpoveď AI povolená cez `ragAnswerAllowed` sa zobrazia v dialógu; kliknutím na názov výsledku sa otvorí príslušná stránka v novej karte a dialóg zostane otvorený. Pre zdroje `file:/docs/...` sa úplná cesta `sourcePath` prevedie na odkaz do aktuálneho prehliadača dokumentácie, aj keď je indexovaný iba podpriečinok. Ostatné súborové korene bez verejnej adresy sa zobrazujú ako text. Dialóg možno zavrieť aj klávesom Escape.

Náhľad nájdenej časti dokumentu zachováva Markdown formátovanie: nadpisy, tučné písmo, kurzívu, zoznamy, citácie a ukážky kódu. Pole `snippet` obsahuje najviac 350 znakov pôvodného Markdown textu so zachovanými zalomeniami riadkov a pri skrátení znak `…`. Prehliadač ho vykreslí existujúcim parserom Docsify; nadpisy majú v náhľade kompaktnú veľkosť. HTML zo zdroja sa zobrazí ako text, pri odkazoch a obrázkoch sa zachová iba ich textový popis. Zmena zobrazenia nevyžaduje opätovné indexovanie.

Každé nové vyhľadávanie z bočného panela otvorí dialóg s nadpisom **Výsledky vyhľadávania** a menším podnadpisom **V adresári /sk/admin/ a podadresároch**, kde sa cesta odvodí z aktuálnej URL bez názvu súboru a parametrov; `#/sk/admin/README` aj `#/sk/admin/` odošlú `/sk/admin/`. Podadresáre sú zahrnuté vždy.

V podnadpise za čiarkou nasleduje odkaz **vyhľadať vo všetkých adresároch**. Zopakuje rovnakú otázku bez parametra `directory` a zobrazí nové výsledky s rovnakým hlavným nadpisom a podnadpisom **Vo všetkých adresároch**. V tomto zobrazení za čiarkou nasleduje odkaz **vyhľadať v aktuálnom adresári /sk/admin/**, ktorý opäť vyhľadá s adresárovým filtrom. Prepnutie zachová jazyk a otázku, nahradí predchádzajúce výsledky aj odpoveď AI a nemení otvorenú stránku dokumentácie. Odkazy zostávajú dostupné aj pri prázdnych výsledkoch.

## Overenie zmien

Backendové regresné testy:

```sh
./gradlew test --tests 'sk.iway.iwcm.rag.*'
```

Test rozhrania so skutočným Docsify a simulovanou REST odpoveďou:

```sh
node docs/tests/semantic-search.test.cjs
```

Test potrebuje nainštalované závislosti `docs/` a `src/test/webapp/` vrátane prehliadača Playwright. Pri nasadení overte aj skutočnú indexáciu, poskytovateľa embeddingov a RAG odpovedí na cieľovej databáze.

Test novej karty a dialógov vo fronte spustite proti lokálnemu WebJETu po načítaní aktuálnych Java tried a prekladov:

```sh
cd src/test/webapp
npm run all tests/rag/markdown-embedding-chunks.js
```

Test používa simulované odpovede Markdown fronty a tabuľky, takže neposiela požiadavky AI poskytovateľovi. Metadáta stĺpcov a kontrolu prístupu overuje v spustenej aplikácii.
