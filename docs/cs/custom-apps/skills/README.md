# WebJET CMS skills pro AI agenty

WebJET CMS skills obsahují postupy pro tvorbu aplikací, DataTables, PageBuilder bloků, testů, dokumentace a kontrolu kódu. Můžete je přidat i do staršího klientského projektu založeného na basecms bez aktualizace jeho šablony nebo samotného CMS.

Podporovány jsou **Codex, GitHub Copilot a Claude Code**. Distribučním zdrojem je [webjetcms/skills](https://github.com/webjetcms/skills). Obsah se udržuje v hlavním repozitáři WebJET CMS a do distribučního repozitáře se exportuje automaticky.

## Instalace

Instalaci zajišťuje stávající nástroj [skills CLI](https://github.com/vercel-labs/skills). Potřebujete Node.js alespoň 22.20, npm a Git. Ověřená verze instalátoru je `skills@1.7.2` ; novější verze může změnit požadavky na Node.js.

V kořenové složce klientského projektu spusťte:

```sh
npx skills@latest add webjetcms/skills --skill '*'
```

Vyberte agenty, které používáte. Pro osobní instalaci dostupnou ve více projektech přidejte `--global`:

```sh
npx skills@latest add webjetcms/skills --skill '*' --global
```

Pro instalaci bez interaktivního výběru uveďte agenty přímo:

```sh
npx --yes skills@latest add webjetcms/skills --skill '*' --agent codex github-copilot claude-code --yes
```

Parametr `--copy` vytvoří samostatné kopie namísto symbolických odkazů. Hodí se například na Windows bez podpory symlinků. Instalace nevyžaduje přidání závislosti do projektového `package.json`.

Pokud potřebujete pouze konkrétní postup, vyberte skill i jeho související skills. Například pro DataTable aplikace:

```sh
npx skills@latest add webjetcms/skills --skill wj-datatable wj-codeceptjs wj-project-context wj-setup
```

Instalátor závislosti mezi skills automaticky nedoplňuje. Stávající soubory může přepsat; při přechodu z ručních kopií nejprve postupujte podle části níže.

## Napojení na pravidla projektu

Po instalaci zadejte agentovi:

> Použij wj-setup a nastav WebJET CMS pravidla pro tento projekt. Zachovej jeho stávající pokyny.

Skill `wj-setup` přidá označené bloky do `AGENTS.md`, `.github/copilot-instructions.md` a `CLAUDE.md`. Zohlední také existující kořenový `AGENTS.override.md`. Vlastní pokyny mimo označené bloky zůstanou zachovány. Opakované spuštění nevytvoří duplicity.

Při projektové instalaci můžete spustit pomocný skript i ručně:

```sh
node .agents/skills/wj-setup/scripts/setup-project.mjs --project . --scope project
```

Pro náhled změn přidejte `--dry-run`. Při globální instalaci agent najde skript v nainstalovaném skille a použije `--scope global` ; upravuje se stále pouze zadaný projekt. Do sdílených pokynů se nezapisuje osobní absolutní cesta a globální soubory pokynů se nemění.

Vlastní pravidla pište mimo značky `<!-- webjet-skills:start -->` a `<!-- webjet-skills:end -->`. Pokud jsou značky poškozeny nebo jsou soubory pokynů symbolickými odkazy, skript změnu odmítne a oznámí důvod. Takové soubory zkontrolujte a napojte ručně.

`npx skills add` samotné nastavení projektu nespouští. Na instalaci i nastavení můžete agentovi poslat jeden požadavek:

> Nainstaluj do tohoto projektu všechny oficiální WebJET CMS skills z https://github.com/webjetcms/skills přes npx skills pro agenta, kterého právě používám. Před nahrazením stávajících skills zachovej a porovnej případné ruční úpravy. Potom použij wj-setup a připoj wj-project-context k pravidlům projektu bez přepisování vlastních pokynů. Neaktualizuj WebJET CMS ani basecms.

Pro globální instalaci nahraďte „do tohoto projektu“ textem „globálně pro mého uživatele a potom nastav pravidla tohoto projektu“. Agent potřebuje přístup k terminálu, síti a zápis do cílových složek. Odkaz na zdroj je spolehlivější než samotné zadání názvu „WebJET skills“.

## Používání a starší projekty

Zadejte například „Vytvoř WebJET DataTable aplikaci“ nebo výslovně pojmenujte `wj-datatable`. Agent může vybrat skill podle jeho popisu. Pokud nenačetl změněná projektová pravidla, začněte novou relaci.

Skill `wj-project-context` zpřístupňuje architekturu a konvence WebJET. Agent má nejprve ověřit verzi CMS, dostupné API, názvy balíků, překladové soubory, SQL aktualizace a testovací nástroje daného projektu. Projektové pokyny mají přednost před společnými doporučeními.

Jedna aktuální sada skills neznamená, že každá historická verze CMS podporuje všechny popsané funkce. Pokud funkce nebo potřebné nástroje chybí, agent má oznámit omezení. Instalace skills nepřidává video infrastrukturu, CodeceptJS helpery, externí pluginy ani přihlašovací údaje.

## Aktualizace a připnutí verze

Spusťte interaktivní aktualizaci a vyberte požadovaný rozsah:

```sh
npx skills@latest update
```

Konkrétně WebJET skills aktualizujete bez zásahu do ostatních takto:

```sh
npx skills@latest update wj-project-context wj-setup wj-datatable wj-codeceptjs --project
```

Pro globální skills použijte `--global`. Po aktualizaci `wj-setup` jej znovu spusťte. Lokální změny v nainstalovaných skills se automaticky neslučují; zákaznické úpravy udržujte v projektových pravidlech nebo v samostatně pojmenovaných skills.

Pokud potřebujete konkrétní revizi, nahraďte `DISTRIBUTION_COMMIT_SHA` úplným SHA commitu z repozitáře **webjetcms/skills**:

```sh
npx skills@1.7.2 add https://github.com/webjetcms/skills/tree/DISTRIBUTION_COMMIT_SHA/skills --skill '*'
```

Nejedná se o SHA zdrojového CMS uvedené v `source.json`. Při připnuté instalaci měňte revizi výslovně; obecný příkaz k aktualizaci nepoužívejte jako náhradu řízeného vydání. Lock soubor instalátoru a nainstalované soubory uchovávejte podle pravidel týmu.

## Přechod z ručně kopírovaných skills

1. Zkontrolujte složky `wj-*` v projektu i globálních složkách agenta a zálohujte vlastní úpravy mimo cíl instalace.
2. Porovnejte je s distribucí. Zákaznická pravidla přesuňte do projektových pokynů nebo samostatně pojmenovaného skillu.
3. Nainstalujte zvolenou sadu a ověřte katalog skills. Staré duplicity odstraňte až po srovnání; projektová i globální kopie se mohou zobrazovat současně.
4. Spusťte `wj-setup` a zkontrolujte diff. Staré ručně zapsané odkazy na `.github/agents` skript nemaže; případné konflikty upravte při kontrole.

K odinstalování použijte `npx skills remove` pro zvolené WebJET skills a odstraňte pouze označené WebJET bloky z projektových pokynů.

## Údržba distribuce

Skills upravujte v `.agents/skills` hlavního repozitáře WebJET CMS, agentové podklady v `.github/agents`. Distribuční kopie se neupravují ručně.

Skills určené pouze pro vývoj jádra vylučte v souboru `.github/skills-distribution/excluded-skills.txt`. Na každý řádek zadejte přesný název adresáře z `.agents/skills`, například `wj-cke-stylescombo`, který je již v seznamu. Prázdné řádky a řádky začínající `#` se ignorují; vzory jako `wj-*` nejsou podporovány. Export vynechá celý adresář skillu včetně referencí, skriptů a dalších souborů i jeho položku v manifestu. Zdrojový skill zůstává v hlavním repozitáři. Neznámý název nebo vyloučení povinných skills `wj-project-context` a `wj-setup` zastaví export s chybou.

Změna seznamu výjimek automaticky spustí stávající workflow. Při nejbližším publikování odstraní synchronizace vyloučený skill iz distribučního repozitáře, pokud tam již byl publikován.

Export spustíte z hlavního repozitáře do nové složky:

```sh
node .github/scripts/export-skills.mjs --output /tmp/webjet-skills-export
node --test .github/scripts/skills.test.mjs
```

Export čte pracovní obsah souborů evidovaných v Git indexu. Nové soubory proto musí být nejprve zařazeny do Gitu; nesledované lokální skills se neexportují. Již existující cílovou složku skript nepřepisuje. Lokální export s necommitnutými úpravami slouží ke kontrole; publikovaný export vzniká z čistého CI checkoutu. Manifest obsahuje zdrojový commit a hash distribučního obsahu, bez časového razítka.

Před prvním publikováním správce:

1. Vytvoří veřejný repozitář `webjetcms/skills` s inicializovanou větví `main`.
2. Ve zdrojovém repozitáři nastaví Actions secret `WEBJET_SKILLS_TOKEN`, jehož token má právo `Contents: write` pouze pro distribuční repozitář.
3. Umožní tomuto tokenu zapisovat do distribuční větve podle jejích pravidel ochrany.
4. Po sloučení implementace spustí workflow **WebJET skills** na větvi `main` a ověří první instalaci z veřejného repozitáře.

Workflow kontroluje PR bez publikačního tokenu. Po relevantních změnách na `main` nebo manuálním spuštění exportuje obsah a po úspěšných testech aktualizuje distribuci. Shodný obsah nevytvoří nový distribuční commit. Distribuční repozitář je určen výhradně pro generovaný výstup; synchronizace odstraňuje soubory, které již do exportu nepatří.

## Ověření distribuce

Automatické testy kontrolují export sledovaných souborů, metadata, přibalené odkazy a zachování vlastních projektových pokynů. Integrační test používá skutečný `skills@1.7.2` v dočasných složkách: instaluje celý balík pro tři agenty projektově i globálně, se symlinky i přes `--copy`, poté nahradí skills novějším obsahem a zopakuje nastavení projektu. GitHub Actions spouští tyto kontroly na Ubuntu i Windows.

Před prvním vydáním ověřte také chování agenta v konkrétních projektech:

| Role | Očekávané chování |
| --- | --- |
| Vytvoření aplikace v jádru a aktuálním basecms | Agent vybere existující aplikační balíček, správné importy a projektové překlady. V klientském projektu nevytváří soubory určené pouze pro jádro. |
| Kontrola kódu ve starším projektu bez interního changelogu | Agent posoudí změny vůči správné větvi a nehlásí chybějící `docs/cs/CHANGELOG-<year> .md`, pokud jej projekt nevyžaduje. |
| Požadavek na novější API nebo testovací helper | Agent prověří dostupnost v konkrétní závislosti a konfiguraci. Pokud API nebo helper chybí, pojmenuje omezení bez automatické aktualizace CMS. |

Instalační test nenahrazuje kompilaci vytvořené aplikace ani testy proti běžícímu CMS. První stažení a aktualizaci přes veřejný zdroj `webjetcms/skills` ověřte po jeho publikování.
