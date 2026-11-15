# Podklady k funkciám WebJET CMS

Prieskum lokálnej dokumentácie a vybraných integračných bodov k 15. 11. 2026, vetva `feature/website-content`. Tento súbor je interný podklad k webovým textom, nie text určený na publikovanie. Opisuje aktuálnu vetvu produktu; sám osebe nepotvrdzuje dostupnosť v každej staršej zákazníckej inštalácii ani zahrnutie do konkrétnej licencie.

## Najsilnejšie doložené témy

| Téma | Čo možno vecne komunikovať | Podklad | Hranica tvrdenia |
| --- | --- | --- | --- |
| AI pri práci s obsahom | Tvorba a úprava textu priamo v poliach, editore stránok a PageBuilderi; práca aj s vybranou časťou textu. | [Používanie AI asistentov](../sk/redactor/ai/datatables/README.md) | Asistenti sa zobrazujú podľa konfigurácie poľa, vstupu, oprávnení a poskytovateľa. Netvrdiť bez výhrady „v každom poli“. |
| AI obrázky | Generovanie a úpravy obrázkov vrátane odstránenia pozadia; redaktor vyberá výsledok a miesto uloženia. | [Obrázkové polia](../sk/redactor/ai/datatables/README.md#obrázkové-polia) | Operácie, rozmery a počet výsledkov závisia od zvoleného modelu. Dostupné funkcie nemusia mať všetci poskytovatelia. |
| AI čet nad podkladmi | V PageBuilderi možno z PDF s textovou vrstvou, DOCX alebo PPTX pripraviť návrh blokov stránky, skontrolovať náhľad a pokračovať v úpravách v rovnakom rozhovore. | [Režim čet](../sk/redactor/ai/datatables/README.md#režim-čet), [AI Čet](../sk/redactor/ai/chat/README.md) | Vloženie návrhu a uloženie stránky sú kroky redaktora. História s prílohami vyžaduje podporovaný OpenAI/Gemini model, uložený záznam a vypnutý dočasný čet. Neopisovať ako automatické publikovanie. |
| Voľba AI | Správca môže konfigurovať vlastných asistentov, pokyny, modely a priradenie k poliam; dokumentované sú OpenAI, Gemini, OpenRouter, lokálne serverové modely a AI v Chrome. | [Nastavenie asistentov](../sk/redactor/ai/settings/README.md) | Externé služby vyžadujú príslušné účty/API kľúče a internet. Neuvádzať konkrétne aktuálne ceny ani dostupnosť bezplatných modelov bez samostatného overenia. |
| Riadenie AI spotreby | Štatistiky použitia a tokenov podľa asistentov, dní a používateľov; nastavenie limitu asistenta v aktuálnej doméne, upozornenia a periodické nulovanie. | [Limity tokenov](../sk/redactor/ai/settings/README.md#karta-limity-tokenov), [Štatistika](../sk/redactor/ai/stat/README.md) | Tokenový limit nie je presný finančný strop. Už spustená požiadavka ho môže prekročiť; účtovanie závisí od poskytovateľa a modelu. |
| Lokálna AI na serveri | Text, preklad čistého textu a sémantické indexovanie/vyhľadávanie možno spracovať na vlastnom serveri bez odosielania týchto požiadaviek externej AI službe. | [Lokálne modely](../sk/redactor/ai/settings/README.md#lokálne-modely), [Sales 2026](../sk/sales/2026/README.md#lokálna-umelá-inteligencia-s-dátami-pod-vlastnou-kontrolou) | Vyžaduje doplnkovú knižnicu a pripravené modelové balíky, dostatok CPU/RAM a overenie na vlastnom obsahu. Lokálny preklad nie je preklad celých HTML stránok. Generovanie odpovedí môže byť pomalšie a menej kvalitné. |
| Sémantické a hybridné vyhľadávanie | Návštevník môže hľadať podľa významu otázky; hybridný režim kombinuje význam s fulltextovou zhodou. | [Sémantické vyhľadávanie](../sk/redactor/apps/semantic-search/README.md) | Treba poskytovateľa embeddingov, indexovanie a podporovanú vektorovú databázu PostgreSQL/pgvector alebo MariaDB Vector. Je možné použiť samostatnú databázu. |
| AI odpoveď vo vyhľadávaní | Voliteľný prehľad AI nad výsledkami vychádza z nájdeného indexovaného obsahu webu. | [Sémantické vyhľadávanie](../sk/redactor/apps/semantic-search/README.md) | Nepotvrdené citácie vložené priamo do odpovede. Nezamieňať výsledky pod odpoveďou za citácie každej vety; nesľubovať bezchybnú odpoveď alebo úplnú elimináciu halucinácií. |
| PageBuilder | Skladanie stránok z pripravených blokov, vizuálna knižnica, strom štruktúry, presun/duplikovanie blokov a nastavenia rozloženia pre zariadenia. | [PageBuilder](../sk/redactor/webpages/pagebuilder.md), [Sales 2026](../sk/sales/2026/README.md#prehľadnejšia-a-rýchlejšia-tvorba-stránok-v-pagebuilderi) | Podoba blokov, knižnice a povolených nastavení sa pripravuje pre konkrétny projekt. Nesľubovať ľubovoľný dizajn bez prípravy šablóny. |
| Redakčné schvaľovanie | Zmeny stránok aj priečinkov možno podmieniť schválením; schvaľovateľ porovná publikovanú a navrhnutú verziu a pridá komentár. | [Schvaľovanie zmien](../sk/redactor/webpages/approve/README.md) | Závisí od nastavenia schvaľovateľov; autor so schvaľovacím právom môže zmenu schváliť automaticky. Nie je to univerzálne vynútené pravidlo štyroch očí. |
| Práva a roly | Skupinové aj individuálne práva na stránky, priečinky, funkcie a aplikácie. | [Skupiny práv](../sk/admin/users/perm-groups.md), [Používatelia](../sk/admin/users/README.md) | Práva zo skupín sa kombinujú. Neprisudzovať produktu certifikáciu bezpečnosti alebo absolútnu ochranu len na základe týchto funkcií. |
| Audit a história | História publikovaných verzií, pracovné verzie, evidovanie schválenia/zamietnutia; audit udalostí vrátane uloženia/publikovania a používateľských operácií. | [Editor](../sk/redactor/webpages/editor/README.md), [História](../sk/redactor/webpages/history.md), [Audit](../sk/sysadmin/audit/README.md) | Audit sa konfiguruje; netvrdiť nemenný audit každej akcie. Pracovné verzie sa pri publikovaní čistia, preto nepísať „nikdy nestratíte žiadnu verziu“. |
| Plánované publikovanie | Príprava novej verzie na určený termín, pri zapnutej rozšírenej funkcii aj s chránenými obrázkami a dokumentmi. | [Plánované publikovanie](../sk/redactor/webpages/planned-publishing.md) | Chránené plánované súbory sú predvolene vypnuté; podporovaný je jeden plán na stránku. Netvrdiť všeobecné embargo na všetky súvisiace údaje. |
| Viac webov | Správa viacerých domén; MultiWeb poskytuje samostatné domény/tenantov v jednej inštalácii s riadiacou doménou. | [Správa domén](../sk/redactor/webpages/editor/README.md#správa-viacerých-domén), [MultiWeb](../sk/install/multiweb/README.md) | Bežná správa viacerých domén a MultiWeb nie sú totožné. Globálne časti ostávajú riadiacej doméne; konkrétne oddelenie sa riadi konfiguráciou. |
| Viac jazykov | Prepojenie jazykových verzií, zrkadlenie štruktúry, automatický preklad názvu, URL a obsahu pri nastavenom prekladači. | [Zrkadlenie štruktúry](../sk/redactor/apps/docmirroring/README.md), [Prekladač](../sk/admin/setup/translation.md) | Ochrana ručne upravených prekladov závisí od správne nastavenej evidencie autora automatického prekladu. Pri zlyhaní prekladača môže zostať pôvodný jazyk. |
| Firemná identita | OAuth2/OpenID Connect a Keycloak; podľa nastavenia preberanie skupín a práv pri prihlásení. | [OAuth2](../sk/install/oauth2/oauth2.md), [Keycloak](../sk/install/oauth2/keycloak.md) | Ide o integráciu a konfiguráciu; nesľubovať automatické pripojenie k ľubovoľnému firemnému systému bez práce. |
| Headless | Obsah, navigácia, novinky a vyhľadávanie cez API; ukážkový frontend, náhľady a použitie existujúcich aplikácií. | [Headless](../sk/frontend/headless/README.md), [Služby](../sk/frontend/headless/services.md), [Ukážka](../sk/frontend/headless/example.md) | Doložená možnosť pre vývojárov. Z API samotného nevyplýva hotové prepojenie na každý CRM/ERP systém. |

## Aplikácie vhodné do prehľadu

Odporúčané redakčné členenie (pracovná hypotéza, čaká na rozhodnutia z rozhovoru): najprv aplikácie pre obsah, dokumenty a zber dopytov; potom marketing a špecifické procesy. Neuvádzať celkový počet aplikácií, kým nie je jasné, či sa počítajú moduly, verejné komponenty alebo varianty.

| Skupina | Aplikácia a doložené použitie | Dokumentácia |
| --- | --- | --- |
| Obsah a dokumenty | Novinky/tlačové správy a výpisy obsahu; blog; galéria; manažér dokumentov s verziami a časovaným zverejnením. | [Novinky](../sk/redactor/apps/news/README.md), [Blog](../sk/redactor/apps/blog/README.md), [Galéria](../sk/redactor/apps/gallery/README.md), [Manažér dokumentov](../sk/redactor/files/file-archive/README.md) |
| Dopyty a formuláre | Jednoduché aj viackrokové formuláre, podmienené polia, validácia, uložené odpovede, export a štatistiky; voliteľné šifrovanie uložených hodnôt. | [Formuláre](../sk/redactor/apps/form/README.md), [Editor a podmienky](../sk/redactor/apps/multistep-form/README.md), [Štatistiky](../sk/redactor/apps/multistep-form/stat.md), [Šifrovanie](../sk/redactor/apps/form/encryption.md) |
| Vyhľadávanie a navigácia | Klasické, sémantické a hybridné vyhľadávanie; voliteľná AI odpoveď; prepojenie jazykových verzií. | [Vyhľadávanie](../sk/redactor/apps/search/README.md), [Sémantické vyhľadávanie](../sk/redactor/apps/semantic-search/README.md), [Jazykové verzie](../sk/redactor/apps/docmirroring/README.md) |
| Marketing a meranie | Personalizované hromadné e-maily, bannery, A/B testy, štatistika návštevnosti a nástroje SEO. | [Hromadný e-mail](../sk/redactor/apps/dmail/campaings/README.md), [Bannery](../sk/redactor/apps/banner/README.md), [A/B testy](../sk/redactor/apps/abtesting/README.md), [Štatistiky](../sk/redactor/apps/stat/README.md), [SEO](../sk/redactor/apps/seo/README.md) |
| Súhlasy a prístupy | Správa kategórií cookies nadviazaná na načítanie skriptov; zaheslované sekcie a používateľské skupiny. | [Cookies súhlas](../sk/redactor/apps/gdpr/README.md), [Zaheslované stránky](../sk/redactor/zaheslovana-zona/README.md) |
| Udalosti a spätná väzba | Kalendár udalostí, ankety, dotazníky a rezervácie s administráciou. | [Kalendár](../sk/redactor/apps/calendar/README.md), [Ankety](../sk/redactor/apps/inquiry/README.md), [Dotazníky](../sk/redactor/apps/quiz/README.md), [Rezervácie](../sk/redactor/apps/reservation/reservations/README.md) |
| Predaj | Elektronický obchod: produkty, objednávky, platby/doručenie a prehľady predaja. | [Produkty](../sk/redactor/apps/eshop/product-list/README.md), [Objednávky](../sk/redactor/apps/eshop/invoice/README.md), [Štatistiky](../sk/redactor/apps/eshop/stats/README.md) |

Zoznam opisuje produktové možnosti, nie obsah každej licencie. Aplikácia Cookies súhlas s kategóriami nie je totožná s jednoduchou informačnou Cookie lištou. Ani jedna aplikácia sama o sebe nedokazuje právny súlad konkrétneho webu.

## Doloženie implementácie a rozlíšenie roadmapy

- AI volania a kontrola limitov: `src/main/java/sk/iway/iwcm/components/ai/rest/AiService.java` (volania `checkTokenLimit` a integrácia poskytovateľov), `AiChatService.java`, `AiChatAttachments.java`, `AiTokenUsageReset.java`.
- Voliteľnosť lokálnej knižnice: `src/main/java/sk/iway/iwcm/components/ai/providers/local/LocalAiAvailableCondition.java` výslovne podmieňuje lokálne AI prítomnosťou `webjet-ai-local`. Závislosť a distribučné pravidlá sú v `build.gradle`.
- Sémantické vyhľadávanie: `src/main/java/sk/iway/iwcm/rag/search/RagService.java`, indexovacia úloha `rag/service/RagIndexCronTask.java`.
- Headless API: `src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java`, `HeadlessActionsRestController.java`, `HeadlessNewsRestController.java` a služby v `headless/service/`.
- Schvaľovanie a publikovanie má samostatný workflow: `src/main/java/sk/iway/iwcm/editor/approve/ApproveService.java`, `editor/service/EditorService.java`, `doc/DocPublishService.java`; prehľad v [agentovej dokumentácii editora](../../.github/agents/AGENTS-editor.md).
- **Java/Spring sú súčasný základ. Spring Boot nie je v aktuálnej konfigurácii core potvrdený ako dodaná verzia.** Používateľ ho označil ako pripravovaný; na verejnom webe ho teda nemožno vydávať za súčasnú vlastnosť ani sľúbiť termín. `build.gradle` a [index projektu](../../.github/agents/AGENTS.md) uvádzajú servletovú aplikáciu inicializovanú cez `SpringAppInitializer`, bez Spring Boot.
- Kontrolované boli dokumenty a vybrané integračné body, nie celý runtime, produkčná dostupnosť či licenčný nárok. Testy aplikácie sa pri tomto prieskume nespúšťali.

## Pripravované publikačné balíky a pracovné verzie: PR #388

Používateľ pri vyjasnení zadania potvrdil primárne CTA **Dohodnúť si ukážku**, pozicioning firemného CMS s konkrétnou AI pomocou a dôraz na schvaľovanie a plánovanie zmien. Ako pripravované rozšírenie uviedol spoločné plánovanie viacerých stránok a branching v [PR #388](https://github.com/webjetcms/webjetcms/pull/388).

### Overený stav

Read-only kontrola cez GitHub CLI k 15. 11. 2026: názov `feature/58858-publishing-packages`, stav **OPEN**, **draft**, `mergedAt=null`, cieľ `main`. Zdrojová vetva je `feature/58858-publishing-packages`, kontrolovaný head `6198aa7f9a165cbf156c35d3258aa94adbbc7c12`. Nebol prepnutý checkout ani zmenený PR.

Aktuálny [návod pre správcu v tomto head](https://github.com/webjetcms/webjetcms/blob/6198aa7f9a165cbf156c35d3258aa94adbbc7c12/docs/sk/admin/setup/publishing-packages.md) označuje funkciu ako **experimentálny POC**, predvolene vypnutý (`branchesEnabled=false`). Uvádza konkrétny overený profil MariaDB 12.2.2/InnoDB, MariaDB Connector/J 3.5.10, Java 17.0.18, jednu inštanciu, jednu zapisovateľnú databázu a lokálny filesystem. Testovacie overenie podľa dokumentácie samo osebe nepovoľuje produkčné zapnutie. Toto nie je všeobecne vydaná funkcia ani sľub kompatibility so všetkými nasadeniami WebJET CMS.

### Doložené pripravované pracovné postupy

Podľa [redaktorskej dokumentácie v kontrolovanom head](https://github.com/webjetcms/webjetcms/blob/6198aa7f9a165cbf156c35d3258aa94adbbc7c12/docs/sk/redactor/webpages/publishing-packages.md):

- **Spoločná príprava kampane:** pracovná verzia združuje viacero stránok, priečinkov, médií a súborov; redaktori vyberajú rovnakú verziu vo vlastnom prihlásení a návštevníci naďalej vidia hlavný web.
- **Preklikávateľný náhľad:** prihlásený administrátor môže prejsť menu, Novinky, detaily, obrázky a prílohy v podporovanom rozsahu; pri naplánovaní Novinky používajú budúci termín.
- **Kontrola zmien a konfliktov:** porovnanie pôvodného základu, aktuálnej hlavnej verzie a pracovnej verzie; výber verzie kolidujúcej položky, kontrola, či sa podklad medzičasom nezmenil.
- **Spoločný termín:** plánovanie publikovania celého balíka, posun jeho termínu jednou operáciou alebo zrušenie plánovania so zachovaním prípravy. Naplánovaný balík sa neupravuje, kým sa neobnoví príprava.
- **Kontrola pri publikovaní:** opätovné overenie používateľa, práv, konfliktov a individuálnych plánov. Publikujúci potrebuje práva na priamu publikáciu celého balíka; balík neobchádza schvaľovanie.
- **Prehľad výsledku:** publikovanie po položkách, viditeľný priebeh, dôvod zastavenia a možnosť kontrolovaného pokračovania po odstránení príčiny.

Opis PR je pôvodný návrh s užším rozsahom; napríklad vylučuje zmeny stromu. Dokumentácia na aktuálnom head už opisuje pracovné priečinky a preklikávanie menu. Marketingové podklady preto vychádzajú z aktuálnych používateľských dokumentov a ich výslovných obmedzení, nie iba zo staršieho opisu PR.

### Hranice pre webové tvrdenia

- Do finálnych textov zatiaľ iba samostatne a zreteľne **Pripravujeme**. Nesmie byť nenápadne zaradené medzi dnešné štandardné funkcie ani používané ako dôkaz súčasného rozdielu oproti konkurencii.
- „Spoločný termín kampane“ je presnejšie než „všetky stránky naraz“. Publikovanie je postupné; počas neho môžu byť verejné staré aj nové položky. Pri chybe už dokončené položky zostávajú verejné.
- Nesľubovať publikovanie presne na sekundu. Predvolený scheduler kontroluje požiadavky každých päť minút; ručné spustenie tiež čaká na jeho beh a prípadný predchádzajúci balík.
- Neopisovať všeobecný Git pre celý web, ľubovoľné paralelné prostredia ani úplný rollback kampane. Celý balík nemá automatický návrat všetkých zmien a súborov.
- Pracovná verzia nepokrýva všetky moduly. Galéria, archív súborov, atribúty, multigroup väzby, šablóny, CSS/JS a zdroje aplikácií sú mimo opísaného POC. Jazykové stránky treba pripraviť explicitne; nevznikajú automaticky zrkadlené preklady.
- [Vývojársky návod](https://github.com/webjetcms/webjetcms/blob/6198aa7f9a165cbf156c35d3258aa94adbbc7c12/docs/sk/developer/apps/webpages/publishing-packages.md) potvrdzuje overené Menu a Novinky; priame SQL/JPA a vlastné komponenty sa musia prispôsobiť. Plný PageBuilder layout, ďalšie databázy, viac uzlov a externé úložiská nie sú plošne potvrdené.

Verejnú zmienku možno po potvrdení rozsahu formulovať napríklad ako: **„Pripravujeme spoločnú prípravu a plánovanie kampaní. Viaceré stránky a súvisiace podklady spojíte do pracovnej verzie, skontrolujete náhľad a nastavíte spoločný termín publikovania.“** Ide o pracovný námet, nie schválený finálny text ani prísľub termínu dostupnosti.

## Obrázky z dokumentácie

Rozmery a veľkosť boli zistené zo súborov. Súbory zatiaľ neboli upravované ani kopírované. Pri použití zachovať pravdivú podobu UI; nevyrábať do snímok nové funkcie alebo výsledky.

| Súbor v `docs/sk/` | Rozmery | Veľkosť | Použitie a poznámka |
| --- | --- | --- | --- |
| `redactor/webpages/pagebuilder-structure.png` | 1280 × 760 | 303 KB | Nové ovládanie PageBuildera a strom štruktúry. Vizuálne overené; obsahuje lorem ipsum, vhodnejšie k vysvetleniu ovládania než ako hlavný obrázok webu. |
| `redactor/ai/datatables/ckeditor-assistants.png` | 1280 × 850 | 226 KB | Výber asistentov v editore. |
| `redactor/ai/datatables/pb-chat-preview.png` | 1440 × 1000 | 234 KB | AI čet s náhľadom vytvorenej stránky. Vizuálne overené; zrozumiteľná ukážka, obsah je demo kaderníckeho salónu. |
| `redactor/ai/datatables/pb-chat-success.png` | 1440 × 1000 | 301 KB | Výsledok vloženia AI návrhu do PageBuildera. |
| `redactor/ai/datatables/image-done.png` | 1000 × 600 | 97 KB | AI práca s obrázkom a ukladanie výsledku. |
| `redactor/ai/chat/chat-editor.png` | 1440 × 1000 | 158 KB | Čet pri práci s obsahom. |
| `redactor/ai/stat/all.png` | 1920 × 1080 | 182 KB | Prehľad AI spotreby. Skôr detail funkcií než hero. |
| `redactor/ai/settings/datatable-token-limits-tab.png` | 900 × 876 | 57 KB | Tokenové limity; vhodné k správe AI pre organizácie. |
| `sales/2026/local-ai.png` | 1672 × 941 | 1930 KB | Ilustrácia lokálnej AI; pred použitím posúdiť vizuálne a zvážiť úsporný formát. |
| `redactor/files/file-archive/datatable.png` | 1400 × 850 | 144 KB | Manažér dokumentov; vhodné pre enterprise obsah. |
| `redactor/apps/multistep-form/real-form.png` | 1360 × 720 | 112 KB | Formulár z pohľadu návštevníka. |
| `redactor/apps/multistep-form/stat-section.png` | 1280 × 1800 | 141 KB | Dlhá snímka štatistík formulára; nevkladať do nízkeho horizontálneho boxu s orezaním podstaty. |
| `redactor/apps/docmirroring/doc-en.png` | 1280 × 760 | 96 KB | Preložená jazyková verzia. |
| `frontend/headless/home.png` | 1280 × 720 | 98 KB | Ukážka headless frontendu, nie vizuálny dôkaz API. |
| `redactor/admin/welcome.png` | 1360 × 745 | 97 KB | Snímka štatistiky návštevnosti s grafom z mája 2022; názov súboru nezodpovedá úvodu administrácie. Root ju vizuálne overil a do nového obsahu nepoužije. |
| `redactor/webpages/approve/approve-form.png` | 1280 × 760 | 83 KB | Vizuálne overené; stará snímka z roku 2021 a testovacie nadpisy. Skôr sekundárna ukážka, menej vhodná na prezentáciu moderného UI. |
| `redactor/apps/semantic-search/rag-result.png` | 1280 × 760 | 115 KB | Vizuálne overené; otázka o McGregorovi/whisky a testovací útržok „bbbfff“. Pre hlavný web neodporúčané bez novej pravdivej snímky s vhodnejším demo obsahom. |

## Formulácie zo sales dokumentácie, ktoré nepreberať doslova

- „Náklady na opravy prístupnosti nižšie až o 80 %“: v prieskume nebola doložená štúdia ani vlastné meranie. Vynechať.
- „Automatická kontrola zabezpečuje súlad s legislatívou/WCAG“: [technický podklad](../sk/developer/testing/a11y.md) opisuje vývojárske testovanie cez axe-core a CodeceptJS. Nepotvrdzuje certifikáciu ani úplnú automatickú kontrolu každého publikovaného obsahu. Možno komunikovať podporu automatizovaných kontrol vo vývojovom procese.
- „Nulové náklady“, „maximálna ochrana údajov“: lokálne spracovanie nemá poplatok za volanie externej AI, ale spotrebúva infraštruktúru a má podmienky nasadenia. AI v prehliadači navyše závisí od podporovaného Chrome/API a hardvéru.
- „Redaktor nikdy nestratí prácu“, „automatizácia eliminuje chyby“, „vždy presná odpoveď“: absolútne sľuby nie sú doložené; existujú podmienky, konflikty a zlyhania externých služieb.
- „AI je natívna, väčšina konkurencie ju nemá“: samotná lokálna dokumentácia nepreukazuje stav trhu. Odlišnosť formulovať cez konkrétny pracovný postup a kontrolu poskytovateľa.
- „Vyššia konverzia“, „nižší odchod návštevníkov“, „úspora hodín“: z funkcie možno odvodiť zjednodušenie konkrétneho kroku; merateľný obchodný efekt uvádzať až s dátami zákazníka.
- „hreflang zlepší pozície“: doložená je podpora označenia jazykových verzií, nie záruka pozície vo vyhľadávaní.
- „Málo CMS na Jave“, „jediné/unikátne CMS“: bez vymedzeného a overeného porovnania nepoužívať. Vecný prínos je rozšíriteľnosť v Java/Spring prostredí a dostupná dokumentácia/API.

## Otázky, ktoré lokálna dokumentácia nerozhodne

1. Aký záväzok sa ponúka agentúram: iba možnosť používať produkt, alebo už konkrétny partnerský program, podpora a obchodné podmienky?
2. Ktoré zákaznícke mená/logá a prípadové štúdie chce majiteľ produktu zvýrazniť? Mená dodal používateľ; prieskum im nepriraďuje konkrétne implementované moduly ani dosiahnuté výsledky.
3. Má byť pripravovaný Spring Boot verejne komunikovaný už teraz? Ak áno, oddeliť ho od dnešných možností a nepísať termín bez potvrdenia.

Hlavný obchodný ďalší krok už používateľ potvrdil: **Dohodnúť si ukážku**.
