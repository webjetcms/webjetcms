# Konfigurace úvodní obrazovky a označení prostředí

Pozadí uvítacího panelu a označení prostředí nastavíte přes **Nastavení → Konfigurace**. Po změně obnovte stránku. Označení se zobrazuje před názvem stránky ve společné hlavičce administrace v9 a na přihlašovací stránce. Na mobilu je za tlačítkem menu. Postup úpravy hodnot popisuje [Konfigurace](README.md).

## Konfigurační proměnné

| Proměnná | Výchozí hodnota | Význam |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | Obrázek pozadí uvítacího panelu. Prázdná nebo neplatná hodnota obrázek skryje. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Text označení. Podporuje makra i vlastní text. Zobrazuje nejvýše 8 znaků velkými písmeny; celý název zůstane v tooltipu. Prázdná hodnota označení skryje iz titulku karty. |
| `dashboardEnvironmentIcon` | `auto` | Ikona podle prostředí nebo název ikony Tabler. `rocket` nebo `ti-database`. Hodnota `none` nebo prázdná hodnota ikonu skryje. Při neexistujícím názvu se ikona nezobrazí. |
| `dashboardEnvironmentColor` | `auto` | Barva z palety dle prostředí a stylu. Vlastní barva `#RGB` či `#RRGGBB` automaticky obdrží černý nebo bílý text a ikonu s kontrastem alespoň 4,5:1. |
| `dashboardEnvironmentStyle` | `auto` | `subtle` = jemný, `strong` = výrazný. Hodnota `auto` použije výrazný styl pro PROD, jemný pro ostatní prostředí. |
| `dashboardEnvironmentDescription` | prázdná | Doplňující popis za celým názvem v tooltipu. Podporuje makra. Tooltip funguje při ukázání myší i při fokusu klávesnicí; Escape ho skryje. |

Název se přidává také do titulku karty prohlížeče a přístupného názvu hlavičky, například `[TEST] Webové stránky | WebJET CMS`. Ikona je volitelná, text zůstává povinen. Na produkci můžete označení vypnout prázdnou hodnotou `dashboardEnvironmentName`.

Obrázek pozadí může používat lokální cestu začínající `/` nebo úplnou HTTP(S) URL bez přihlašovacích údajů. Adresy začínající `//`, adresy se zpětnými lomítky nebo řídicími znaky a jiné protokoly nejsou povoleny. Maximální délka adresy je 1024 znaků.

## Automatické označení prostředí

Makro `{ENVIRONMENT_NAME}` určuje prostředí podle názvu serveru, přes který přistupujete do administrace. Změna zvolené obsahové domény označení nemění.

Detekce nerozlišuje velikost písmen. Hledá celé části názvu oddělené tečkou, pomlčkou nebo podtržítkem, také s číslem uzlu, například `uat01` nebo `web-prod-02`. Při více shodách platí pořadí **PROD → UAT → CIT → INT → TEST → DEMO → LOCAL → DEV**.

| Prostředí | Rozpoznané části názvu serveru | Automatická ikona | Jemná barva / text | Výrazná barva / text |
| --- | --- | --- | --- | --- |
| PROD | `prod`, `prd`, `production`, `live` | `ti-alert-triangle` | `#FFD6D7` / `#790011` | `#C4001F` / `#FFFFFF` |
| TEST | `test`, `testing`, `qa` | `ti-test-pipe` | `#FFE0CC` / `#7A2E00` | `#C24E00` / `#FFFFFF` |
| CIT | `cit` | `ti-user-check` | `#EADFFF` / `#5200A3` | `#6E00DC` / `#FFFFFF` |
| INT | `int`, `integration`, `sit` | `ti-plug-connected` | `#DCE4FF` / `#0037A6` | `#0049BE` / `#FFFFFF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `preprod`, `preproduction` | bez ikony | `#C8F0F4` / `#004F59` | `#00717F` / `#FFFFFF` |
| DEMO | `demo` | `ti-eye` | `#FFF0B3` / `#5C4300` | `#F6BE3F` / `#13151B` |
| LOCAL | `local`, `localhost`, zpětná smyčka IPv4 `127.*.*.*` nebo IPv6 `::1` | `ti-device-laptop` | `#E6E8EE` / `#353944` | `#353944` / `#FFFFFF` |
| DEV | Ostatní názvy, například `iwcm.interway.sk` | `ti-code` | `#CFF5E4` / `#00533D` | `#007E69` / `#FFFFFF` |

Předprodukce zahrnuje i zápisy `pre-prod`, `pre-production` a jejich varianty s tečkou či podtržítkem. Tyto části názvu se nepovažují za PROD. Část slova, například `int` v `interway`, se neshoduje s prostředím INT. TEST je samostatné prostředí; názvy `test`, `testing` a `qa` již neoznačují UAT.

Automatický vzhled přednostně použije úvodní označení prostředí v nakonfigurovaném textu, za kterým následuje lomítko, mezera, pomlčka nebo konec textu. Při jiném vlastním textu vychází z detekce serveru. Ručně nastavená ikona nebo barva má přednost. Při neplatné barvě se použije automatický výběr.

## Název uzlu a vlastní označení

Například `dashboardEnvironmentName={ENVIRONMENT_NAME}/{CLUSTER_NAME}` doplní název aktuálního uzlu z `clusterMyNodeName`. Celý název, například `UAT/node-1`, je v tooltipu; štítek zobrazí prvních 8 znaků velkými písmeny. Při prázdném názvu uzlu se koncový lomítko odstraní.

Pro krátký štítek a delší popis nastavte `dashboardEnvironmentName=TEST` a `dashboardEnvironmentDescription=Testovacie prostredie, uzol {CLUSTER_NAME}`. Nastavení prostředí používá existující konfiguraci; samostatný dialog na úvodní obrazovce není k dispozici.
