# Konfigurácia úvodnej obrazovky a označenia prostredia

Pozadie uvítacieho panelu a označenie prostredia nastavíte cez **Nastavenia → Konfigurácia**. Po zmene obnovte stránku. Označenie sa zobrazuje pred názvom stránky v spoločnej hlavičke administrácie v9 a na prihlasovacej stránke. Na mobile je za tlačidlom menu. Postup úpravy hodnôt opisuje [Konfigurácia](README.md).

## Konfiguračné premenné

| Premenná | Predvolená hodnota | Význam |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | Obrázok pozadia uvítacieho panelu. Prázdna alebo neplatná hodnota obrázok skryje. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Text označenia. Podporuje makrá aj vlastný text. Zobrazuje najviac 8 znakov veľkými písmenami; celý názov zostane v tooltipe. Prázdna hodnota označenie skryje aj z titulku karty. |
| `dashboardEnvironmentIcon` | `auto` | Ikona podľa prostredia alebo názov ikony Tabler, napr. `rocket` alebo `ti-database`. Hodnota `none` alebo prázdna hodnota ikonu skryje. Pri neexistujúcom názve sa ikona nezobrazí. |
| `dashboardEnvironmentColor` | `auto` | Farba z palety podľa prostredia a štýlu. Vlastná farba `#RGB` či `#RRGGBB` automaticky dostane čierny alebo biely text a ikonu s kontrastom aspoň 4,5 : 1. |
| `dashboardEnvironmentStyle` | `auto` | `subtle` = jemný, `strong` = výrazný. Hodnota `auto` použije výrazný štýl pre PROD, jemný pre ostatné prostredia. |
| `dashboardEnvironmentDescription` | prázdna | Doplňujúci popis za celým názvom v tooltipe. Podporuje makrá. Tooltip funguje pri ukázaní myšou aj pri fokuse klávesnicou; Escape ho skryje. |

Názov sa pridáva aj do titulku karty prehliadača a prístupného názvu hlavičky, napríklad `[TEST] Webové stránky | WebJET CMS`. Ikona je voliteľná, text zostáva povinný. Na produkcii môžete označenie vypnúť prázdnou hodnotou `dashboardEnvironmentName`.

Obrázok pozadia môže používať lokálnu cestu začínajúcu `/` alebo úplnú HTTP(S) URL bez prihlasovacích údajov. Adresy začínajúce `//`, adresy so spätnými lomkami alebo riadiacimi znakmi a iné protokoly nie sú povolené. Maximálna dĺžka adresy je 1024 znakov.

## Automatické označenie prostredia

Makro `{ENVIRONMENT_NAME}` určuje prostredie podľa názvu servera, cez ktorý pristupujete do administrácie. Zmena zvolenej obsahovej domény označenie nemení.

Detekcia nerozlišuje veľkosť písmen. Hľadá celé časti názvu oddelené bodkou, pomlčkou alebo podčiarkovníkom, aj s číslom uzla, napríklad `uat01` alebo `web-prod-02`. Pri viacerých zhodách platí poradie **PROD → UAT → CIT → INT → TEST → DEMO → LOCAL → DEV**.

| Prostredie | Rozpoznané časti názvu servera | Automatická ikona | Jemná farba / text | Výrazná farba / text |
| --- | --- | --- | --- | --- |
| PROD | `prod`, `prd`, `production`, `live` | `ti-alert-triangle` | `#FFD6D7` / `#790011` | `#C4001F` / `#FFFFFF` |
| TEST | `test`, `testing`, `qa` | `ti-test-pipe` | `#FFE0CC` / `#7A2E00` | `#C24E00` / `#FFFFFF` |
| CIT | `cit` | `ti-user-check` | `#EADFFF` / `#5200A3` | `#6E00DC` / `#FFFFFF` |
| INT | `int`, `integration`, `sit` | `ti-plug-connected` | `#DCE4FF` / `#0037A6` | `#0049BE` / `#FFFFFF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `preprod`, `preproduction` | bez ikony | `#C8F0F4` / `#004F59` | `#00717F` / `#FFFFFF` |
| DEMO | `demo` | `ti-eye` | `#FFF0B3` / `#5C4300` | `#F6BE3F` / `#13151B` |
| LOCAL | `local`, `localhost`, spätná slučka IPv4 `127.*.*.*` alebo IPv6 `::1` | `ti-device-laptop` | `#E6E8EE` / `#353944` | `#353944` / `#FFFFFF` |
| DEV | Ostatné názvy, napríklad `iwcm.interway.sk` | `ti-code` | `#CFF5E4` / `#00533D` | `#007E69` / `#FFFFFF` |

Predprodukcia zahŕňa aj zápisy `pre-prod`, `pre-production` a ich varianty s bodkou či podčiarkovníkom. Tieto časti názvu sa nepovažujú za PROD. Časť slova, napríklad `int` v `interway`, sa nezhoduje s prostredím INT. TEST je samostatné prostredie; názvy `test`, `testing` a `qa` už neoznačujú UAT.

Automatický vzhľad prednostne použije úvodné označenie prostredia v nakonfigurovanom texte, za ktorým nasleduje lomka, medzera, pomlčka alebo koniec textu. Pri inom vlastnom texte vychádza z detekcie servera. Ručne nastavená ikona alebo farba má prednosť. Pri neplatnej farbe sa použije automatický výber.

## Názov uzla a vlastné označenie

Napríklad `dashboardEnvironmentName={ENVIRONMENT_NAME}/{CLUSTER_NAME}` doplní názov aktuálneho uzla z `clusterMyNodeName`. Celý názov, napríklad `UAT/node-1`, je v tooltipe; štítok zobrazí prvých 8 znakov veľkými písmenami. Pri prázdnom názve uzla sa koncová lomka odstráni.

Pre krátky štítok a dlhší popis nastavte `dashboardEnvironmentName=TEST` a `dashboardEnvironmentDescription=Testovacie prostredie, uzol {CLUSTER_NAME}`. Nastavenie prostredia používa existujúcu konfiguráciu; samostatný dialóg na úvodnej obrazovke nie je k dispozícii.
