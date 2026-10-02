# Konfigurace úvodní obrazovky a označení prostředí

Pozadí uvítacího panelu a označení prostředí nastavíte přes **Nastavení → Konfigurace**, poté obnovte stránku. Označení se zobrazuje před názvem stránky v celé administraci v9 a na přihlašovací stránce. Na mobilu následuje za tlačítkem menu. Postup úpravy hodnot popisuje [Konfigurace](README.md).

## Konfigurační proměnné

| Proměnná | Výchozí hodnota | Význam |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | Obrázek pozadí uvítacího panelu. Prázdná nebo neplatná hodnota obrázek skryje. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Text s podporou maker. Zobrazí nejvýše 8 velkých písmen; celý název zůstane v tooltipu. Prázdná hodnota skryje označení i předponu titulku karty, také na produkci. |
| `dashboardEnvironmentIcon` | `auto` | Automatická ikona, název Tabler jako `rocket` nebo `ti-database`, případně `none` či prázdná hodnota pro skrytí ikony. Při neexistujícím názvu se ikona nezobrazí. |
| `dashboardEnvironmentColor` | `auto` | Paleta prostředí nebo vlastní pozadí `#RGB`/`#RRGGBB`. Text a ikona pro vlastní barvu budou černé nebo bílé s kontrastem alespoň 4,5 : 1. |
| `dashboardEnvironmentStyle` | `auto` | `subtle` = jemný, `strong` = výrazný, `auto` = výrazný pro PROD a jemný pro ostatní prostředí. |
| `dashboardEnvironmentDescription` | prázdná | Popis za celým názvem v tooltipu při ukázání myší i fokusu klávesnicí. Podporuje makra. Escape tooltip skryje. |

Obrázek může používat místní cestu začínající `/` nebo úplnou HTTP(S) URL bez přihlašovacích údajů. Adresy začínající `//`, adresy se zpětnými lomítky nebo řídicími znaky a jiné protokoly nejsou povoleny. Maximální délka adresy je 1024 znaků.

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

Předprodukce zahrnuje také `pre-prod`, `pre-production` a varianty s tečkou či podtržítkem; tyto části se nepovažují za PROD. Část slova, například `int` v `interway`, se neshoduje s INT. TEST je samostatné prostředí: `test`, `testing` a `qa` již neoznačují UAT.

Automatický vzhled použije rozpoznané prostředí na začátku nakonfigurovaného názvu, za kterým následuje lomítko, mezera, pomlčka nebo konec textu. Jiné vlastní názvy použijí detekci serveru. Ručně nastavená ikona nebo barva má přednost. Při neplatné barvě se použije automatický výběr.

## Název uzlu a vlastní označení

`dashboardEnvironmentName={ENVIRONMENT_NAME}/{CLUSTER_NAME}` doplní `clusterMyNodeName` aktuálního uzlu. Celý název, například `UAT/node-1`, zůstane v tooltipu; štítek zobrazí prvních 8 znaků velkými písmeny. Při prázdném názvu uzlu se koncové lomítko odstraní.

Pro krátký štítek a delší popis nastavte `dashboardEnvironmentName=TEST` a `dashboardEnvironmentDescription=Testovací prostředí, uzel {CLUSTER_NAME}`. Text se přidá také do titulku karty a přístupného názvu hlavičky, například `[TEST] Webové stránky | WebJET CMS`. Nastavení používá stávající konfiguraci; samostatný dialog na úvodní obrazovce není k dispozici.
