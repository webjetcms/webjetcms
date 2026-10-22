# WebJET CMS skills pre AI agentov

WebJET CMS skills obsahujú postupy pre tvorbu aplikácií, DataTables, PageBuilder blokov, testov, dokumentácie a kontrolu kódu. Môžete ich pridať aj do staršieho klientského projektu založeného na basecms bez aktualizácie jeho šablóny alebo samotného CMS.

Podporované sú **Codex, GitHub Copilot a Claude Code**. Distribučným zdrojom je [webjetcms/skills](https://github.com/webjetcms/skills). Obsah sa udržiava v hlavnom repozitári WebJET CMS a do distribučného repozitára sa exportuje automaticky.

## Inštalácia

Inštaláciu zabezpečuje existujúci nástroj [skills CLI](https://github.com/vercel-labs/skills). Potrebujete Node.js aspoň 22.20, npm a Git. Overená verzia inštalátora je `skills@1.7.2`; novšia verzia môže zmeniť požiadavky na Node.js.

V koreňovom priečinku klientského projektu spustite:

```sh
npx skills@latest add webjetcms/skills --skill '*'
```

Vyberte agentov, ktorých používate. Pre osobnú inštaláciu dostupnú vo viacerých projektoch pridajte `--global`:

```sh
npx skills@latest add webjetcms/skills --skill '*' --global
```

Pre inštaláciu bez interaktívneho výberu uveďte agentov priamo:

```sh
npx --yes skills@latest add webjetcms/skills --skill '*' --agent codex github-copilot claude-code --yes
```

Parameter `--copy` vytvorí samostatné kópie namiesto symbolických odkazov. Hodí sa napríklad na Windows bez podpory symlinkov. Inštalácia nevyžaduje pridanie závislosti do projektového `package.json`.

Ak potrebujete iba konkrétny postup, vyberte skill aj jeho súvisiace skills. Napríklad pre DataTable aplikácie:

```sh
npx skills@latest add webjetcms/skills --skill wj-datatable wj-codeceptjs wj-project-context wj-setup
```

Inštalátor závislosti medzi skills automaticky nedopĺňa. Existujúce súbory môže prepísať; pri prechode z ručných kópií najprv postupujte podľa časti nižšie.

## Napojenie na pravidlá projektu

Po inštalácii zadajte agentovi:

> Použi wj-setup a nastav WebJET CMS pravidlá pre tento projekt. Zachovaj jeho existujúce pokyny.

Skill `wj-setup` pridá označené bloky do `AGENTS.md`, `.github/copilot-instructions.md` a `CLAUDE.md`. Zohľadní aj existujúci koreňový `AGENTS.override.md`. Vlastné pokyny mimo označených blokov zostanú zachované. Opakované spustenie nevytvorí duplicity.

Pri projektovej inštalácii môžete spustiť pomocný skript aj ručne:

```sh
node .agents/skills/wj-setup/scripts/setup-project.mjs --project . --scope project
```

Pre náhľad zmien pridajte `--dry-run`. Pri globálnej inštalácii agent nájde skript v nainštalovanom skille a použije `--scope global`; upravuje sa stále iba zadaný projekt. Do zdieľaných pokynov sa nezapisuje osobná absolútna cesta a globálne súbory pokynov sa nemenia.

Vlastné pravidlá píšte mimo značiek `<!-- webjet-skills:start -->` a `<!-- webjet-skills:end -->`. Ak sú značky poškodené alebo sú súbory pokynov symbolickými odkazmi, skript zmenu odmietne a oznámi dôvod. Také súbory skontrolujte a napojte ručne.

`npx skills add` samotné nastavenie projektu nespúšťa. Na inštaláciu aj nastavenie môžete agentovi poslať jednu požiadavku:

> Nainštaluj do tohto projektu všetky oficiálne WebJET CMS skills z https://github.com/webjetcms/skills cez npx skills pre agenta, ktorého práve používam. Pred nahradením existujúcich skills zachovaj a porovnaj prípadné ručné úpravy. Potom použi wj-setup a pripoj wj-project-context k pravidlám projektu bez prepisovania vlastných pokynov. Neaktualizuj WebJET CMS ani basecms.

Pre globálnu inštaláciu nahraďte „do tohto projektu“ textom „globálne pre môjho používateľa a potom nastav pravidlá tohto projektu“. Agent potrebuje prístup k terminálu, sieti a zápis do cieľových priečinkov. Odkaz na zdroj je spoľahlivejší než samotné zadanie názvu „WebJET skills“.

## Používanie a staršie projekty

Zadajte napríklad „Vytvor WebJET DataTable aplikáciu“ alebo výslovne pomenujte `wj-datatable`. Agent môže vybrať skill podľa jeho popisu. Ak nenačítal zmenené projektové pravidlá, začnite novú reláciu.

Skill `wj-project-context` sprístupňuje architektúru a konvencie WebJET. Agent má najprv overiť verziu CMS, dostupné API, názvy balíkov, prekladové súbory, SQL aktualizácie a testovacie nástroje daného projektu. Projektové pokyny majú prednosť pred spoločnými odporúčaniami.

Jedna aktuálna sada skills neznamená, že každá historická verzia CMS podporuje všetky opísané funkcie. Ak funkcia alebo potrebné nástroje chýbajú, agent má oznámiť obmedzenie. Inštalácia skills nepridáva video infraštruktúru, CodeceptJS helpery, externé pluginy ani prihlasovacie údaje.

## Aktualizácia a pripnutie verzie

Spustite interaktívnu aktualizáciu a vyberte požadovaný rozsah:

```sh
npx skills@latest update
```

Konkrétne WebJET skills aktualizujete bez zásahu do ostatných takto:

```sh
npx skills@latest update wj-project-context wj-setup wj-datatable wj-codeceptjs --project
```

Pre globálne skills použite `--global`. Po aktualizácii `wj-setup` ho znovu spustite. Lokálne zmeny v nainštalovaných skills sa automaticky nezlučujú; zákaznícke úpravy udržiavajte v projektových pravidlách alebo v samostatne pomenovaných skills.

Ak potrebujete konkrétnu revíziu, nahraďte `DISTRIBUTION_COMMIT_SHA` úplným SHA commitu z repozitára **webjetcms/skills**:

```sh
npx skills@1.7.2 add https://github.com/webjetcms/skills/tree/DISTRIBUTION_COMMIT_SHA/skills --skill '*'
```

Nejde o SHA zdrojového CMS uvedené v `source.json`. Pri pripnutej inštalácii meňte revíziu výslovne; všeobecný príkaz na aktualizáciu nepoužívajte ako náhradu riadeného vydania. Lock súbor inštalátora a nainštalované súbory uchovávajte podľa pravidiel tímu.

## Prechod z ručne kopírovaných skills

1. Skontrolujte priečinky `wj-*` v projekte aj globálnych priečinkoch agenta a zálohujte vlastné úpravy mimo cieľa inštalácie.
2. Porovnajte ich s distribúciou. Zákaznícke pravidlá presuňte do projektových pokynov alebo samostatne pomenovaného skillu.
3. Nainštalujte zvolenú sadu a overte katalóg skills. Staré duplicity odstráňte až po porovnaní; projektová aj globálna kópia sa môžu zobrazovať súčasne.
4. Spustite `wj-setup` a skontrolujte diff. Staré ručne zapísané odkazy na `.github/agents` skript nemaže; prípadné konflikty upravte pri kontrole.

Na odinštalovanie použite `npx skills remove` pre zvolené WebJET skills a odstráňte iba označené WebJET bloky z projektových pokynov.

## Údržba distribúcie

Skills upravujte v `.agents/skills` hlavného repozitára WebJET CMS, agentové podklady v `.github/agents`. Distribučné kópie sa neupravujú ručne.

Skills určené iba pre vývoj jadra vylúčte v súbore `.github/skills-distribution/excluded-skills.txt`. Na každý riadok zadajte presný názov adresára z `.agents/skills`, napríklad `wj-cke-stylescombo`, ktorý je už v zozname. Prázdne riadky a riadky začínajúce `#` sa ignorujú; vzory ako `wj-*` nie sú podporované. Export vynechá celý adresár skillu vrátane referencií, skriptov a ďalších súborov aj jeho položku v manifeste. Zdrojový skill zostáva v hlavnom repozitári. Neznámy názov alebo vylúčenie povinných skills `wj-project-context` a `wj-setup` zastaví export s chybou.

Zmena zoznamu výnimiek automaticky spustí existujúci workflow. Pri najbližšom publikovaní odstráni synchronizácia vylúčený skill aj z distribučného repozitára, ak tam už bol publikovaný.

Export spustíte z hlavného repozitára do nového priečinka:

```sh
node .github/scripts/export-skills.mjs --output /tmp/webjet-skills-export
node --test .github/scripts/skills.test.mjs
```

Export číta pracovný obsah súborov evidovaných v Git indexe. Nové súbory preto musia byť najprv zaradené do Gitu; nesledované lokálne skills sa neexportujú. Už existujúci cieľový priečinok skript neprepisuje. Lokálny export s necommitnutými úpravami slúži na kontrolu; publikovaný export vzniká z čistého CI checkoutu. Manifest obsahuje zdrojový commit a hash distribučného obsahu, bez časovej pečiatky.

Pred prvým publikovaním správca:

1. Vytvorí verejný repozitár `webjetcms/skills` s inicializovanou vetvou `main`.
2. V zdrojovom repozitári nastaví Actions secret `WEBJET_SKILLS_TOKEN`, ktorého token má právo `Contents: write` iba pre distribučný repozitár.
3. Umožní tomuto tokenu zapisovať do distribučnej vetvy podľa jej pravidiel ochrany.
4. Po zlúčení implementácie spustí workflow **WebJET skills** na vetve `main` a overí prvú inštaláciu z verejného repozitára.

Workflow kontroluje PR bez publikačného tokenu. Po relevantných zmenách na `main` alebo manuálnom spustení exportuje obsah a po úspešných testoch aktualizuje distribúciu. Zhodný obsah nevytvorí nový distribučný commit. Distribučný repozitár je určený výhradne pre generovaný výstup; synchronizácia odstraňuje súbory, ktoré už do exportu nepatria.

## Overenie distribúcie

Automatické testy kontrolujú export sledovaných súborov, metadáta, pribalené odkazy a zachovanie vlastných projektových pokynov. Integračný test používa skutočný `skills@1.7.2` v dočasných priečinkoch: inštaluje celý balík pre troch agentov projektovo aj globálne, so symlinkmi aj cez `--copy`, potom nahradí skills novším obsahom a zopakuje nastavenie projektu. GitHub Actions spúšťa tieto kontroly na Ubuntu aj Windows.

Pred prvým vydaním overte aj správanie agenta v konkrétnych projektoch:

| Úloha | Očakávané správanie |
| --- | --- |
| Vytvorenie aplikácie v jadre a aktuálnom basecms | Agent vyberie existujúci aplikačný balík, správne importy a projektové preklady. V klientskom projekte nevytvára súbory určené iba pre jadro. |
| Kontrola kódu v staršom projekte bez interného changelogu | Agent posúdi zmeny voči správnej vetve a nehlási chýbajúci `docs/sk/CHANGELOG-<year>.md`, ak ho projekt nevyžaduje. |
| Požiadavka na novšie API alebo testovací helper | Agent preverí dostupnosť v konkrétnej závislosti a konfigurácii. Ak API alebo helper chýba, pomenuje obmedzenie bez automatickej aktualizácie CMS. |

Inštalačný test nenahrádza kompiláciu vytvorenej aplikácie ani testy proti bežiacemu CMS. Prvé stiahnutie a aktualizáciu cez verejný zdroj `webjetcms/skills` overte po jeho publikovaní.
