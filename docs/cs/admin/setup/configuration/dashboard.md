# Konfigurace úvodní obrazovky

Pozadí uvítacího panelu a označení prostředí na [úvodní obrazovce](../../../redactor/admin/welcome.md) nastavíte přes **Nastavení → Konfigurace**. Vyhledejte název proměnné, nastavte hodnotu a obnovte úvodní obrazovku. Postup úpravy hodnot popisuje [Konfigurace](README.md).

## Konfigurační proměnné

| Proměnná | Výchozí hodnota | Význam |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | Obrázek pozadí uvítacího panelu. Prázdná nebo neplatná hodnota obrázek skryje. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Text označení prostředí. Podporuje makra i vlastní text. Prázdná hodnota označení skryje. |
| `dashboardEnvironmentIcon` | `auto` | Ikona podle prostředí nebo vlastní třída Tabler `ti-*`, například `ti-database`. |
| `dashboardEnvironmentColor` | `auto` | Barva dle prostředí nebo vlastní barva ve formátu `#RGB` či `#RRGGBB`. Barva textu se vybere automaticky podle kontrastu. |

Obrázek může používat místní cestu začínající `/` nebo úplnou HTTP(S) URL bez přihlašovacích údajů. Adresy začínající `//`, adresy se zpětnými lomítky nebo řídicími znaky a jiné protokoly nejsou povoleny. Maximální délka adresy je 1024 znaků.

## Automatické označení prostředí

Makro `{ENVIRONMENT_NAME}` určuje prostředí podle názvu serveru, přes který přistupujete do administrace. Změna zvolené obsahové domény označení nemění.

Detekce nerozlišuje velikost písmen. Hledá celé části názvu oddělené tečkou, pomlčkou nebo podtržítkem, také s číslem uzlu, například `uat01` nebo `web-prod-02`. Při více shodách platí pořadí **PROD → UAT → INT → DEV**.

| Prostředí | Rozpoznané části názvu serveru | Automatická ikona | Automatická barva |
| --- | --- | --- | --- |
| PROD | `prod`, `prd`, `production`, `live` | `ti-server` | zelená `#D6F5EF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `test`, `testing`, `qa`, `preprod`, `preproduction` | `ti-clipboard-check` | žlutá `#FFF2C9` |
| INT | `int`, `integration`, `sit` | `ti-git-merge` | oranžová `#FFE0B2` |
| DEV | Ostatní názvy, například `localhost`, IP adresa nebo `iwcm.interway.sk` | `ti-code` | červená `#FFD9DE` |

Před produkce zahrnuje i zápisy `pre-prod`, `pre-production` a jejich varianty s tečkou či podtržítkem. Tyto části názvu se nepovažují za PROD. Část slova, například `int` v `interway`, se neshoduje s prostředím INT.

Automatická ikona a barva přednostně použijí úvodní označení `PROD`, `UAT`, `INT` nebo `DEV` v textu štítku, za kterým následuje lomítko, mezera, pomlčka nebo konec textu. U jiného vlastního textu vycházejí z prostředí zjištěného podle serveru. Platná ručně nastavená ikona nebo barva má přednost, neplatná hodnota použije automatický výběr.

## Název uzlu a vlastní označení

Chcete-li také zobrazit název uzlu clusteru, nastavte `dashboardEnvironmentName` na `{ENVIRONMENT_NAME}/{CLUSTER_NAME}`. Makro `{CLUSTER_NAME}` používá hodnotu `clusterMyNodeName` aktuálního uzlu. Výsledkem může být například `UAT/node-1`. Při prázdném názvu uzlu se koncový lomítko ve štítku odstraní a zůstane `UAT`.

Můžete použít i pevný text, například `UAT/Školenie`, nebo vlastní označení `Školiaci server`. Pro skrytí štítku nastavte prázdnou hodnotu `dashboardEnvironmentName`, pro skrytí obrázku pozadí nastavte prázdnou hodnotu `dashboardHeroBackgroundImage`.
