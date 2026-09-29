# Konfigurácia úvodnej obrazovky

Pozadie uvítacieho panelu a označenie prostredia na [úvodnej obrazovke](../../../redactor/admin/welcome.md) nastavíte cez **Nastavenia → Konfigurácia**. Vyhľadajte názov premennej, nastavte hodnotu a obnovte úvodnú obrazovku. Postup úpravy hodnôt opisuje [Konfigurácia](README.md).

## Konfiguračné premenné

| Premenná | Predvolená hodnota | Význam |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `/admin/skins/webjet8/assets/global/img/wj/wj9_bg.jpg` | Obrázok pozadia uvítacieho panelu. Prázdna alebo neplatná hodnota obrázok skryje. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Text označenia prostredia. Podporuje makrá aj vlastný text. Prázdna hodnota označenie skryje. |
| `dashboardEnvironmentIcon` | `auto` | Ikona podľa prostredia alebo vlastná trieda Tabler `ti-*`, napríklad `ti-database`. |
| `dashboardEnvironmentColor` | `auto` | Farba podľa prostredia alebo vlastná farba vo formáte `#RGB` či `#RRGGBB`. Farba textu sa vyberie automaticky podľa kontrastu. |

Obrázok môže používať lokálnu cestu začínajúcu `/` alebo úplnú HTTP(S) URL bez prihlasovacích údajov. Adresy začínajúce `//`, adresy so spätnými lomkami alebo riadiacimi znakmi a iné protokoly nie sú povolené. Maximálna dĺžka adresy je 1024 znakov.

## Automatické označenie prostredia

Makro `{ENVIRONMENT_NAME}` určuje prostredie podľa názvu servera, cez ktorý pristupujete do administrácie. Zmena zvolenej obsahovej domény označenie nemení.

Detekcia nerozlišuje veľkosť písmen. Hľadá celé časti názvu oddelené bodkou, pomlčkou alebo podčiarkovníkom, aj s číslom uzla, napríklad `uat01` alebo `web-prod-02`. Pri viacerých zhodách platí poradie **PROD → UAT → INT → DEV**.

| Prostredie | Rozpoznané časti názvu servera | Automatická ikona | Automatická farba |
| --- | --- | --- | --- |
| PROD | `prod`, `prd`, `production`, `live` | `ti-server` | zelená `#D6F5EF` |
| UAT | `uat`, `aut`, `acc`, `acceptance`, `stage`, `staging`, `test`, `testing`, `qa`, `preprod`, `preproduction` | `ti-clipboard-check` | žltá `#FFF2C9` |
| INT | `int`, `integration`, `sit` | `ti-git-merge` | oranžová `#FFE0B2` |
| DEV | Ostatné názvy, napríklad `localhost`, IP adresa alebo `iwcm.interway.sk` | `ti-code` | červená `#FFD9DE` |

Predprodukcia zahŕňa aj zápisy `pre-prod`, `pre-production` a ich varianty s bodkou či podčiarkovníkom; tieto časti názvu sa nepovažujú za PROD. Časť slova, napríklad `int` v `interway`, sa nezhoduje s prostredím INT.

Automatická ikona a farba prednostne použijú úvodné označenie `PROD`, `UAT`, `INT` alebo `DEV` v texte štítku, za ktorým nasleduje lomka, medzera, pomlčka alebo koniec textu. Pri inom vlastnom texte vychádzajú z prostredia zisteného podľa servera. Platná ručne nastavená ikona alebo farba má prednosť; neplatná hodnota použije automatický výber.

## Názov uzla a vlastné označenie

Ak chcete zobraziť aj názov uzla klastra, nastavte `dashboardEnvironmentName` na `{ENVIRONMENT_NAME}/{CLUSTER_NAME}`. Makro `{CLUSTER_NAME}` používa hodnotu `clusterMyNodeName` aktuálneho uzla. Výsledkom môže byť napríklad `UAT/node-1`. Pri prázdnom názve uzla sa koncová lomka v štítku odstráni a zostane `UAT`.

Môžete použiť aj pevný text, napríklad `UAT/Školenie`, alebo vlastné označenie `Školiaci server`. Na skrytie štítku nastavte prázdnu hodnotu `dashboardEnvironmentName`; na skrytie obrázka pozadia nastavte prázdnu hodnotu `dashboardHeroBackgroundImage`.
