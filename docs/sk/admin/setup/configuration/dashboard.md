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
| `adminNewDeviceDetectionEnabled` | `true` | Zapne rozpoznávanie prehliadačov pri dokončenom prihlásení administrátora, upozornenia na prehľade a emaily o novom zariadení. |
| `adminNewDeviceMaxAgeDays` | `90` | Počet dní od posledného úspešného prihlásenia, počas ktorých sa prehliadač považuje za známy pre daný účet. Určuje aj platnosť cookie. |

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

## Detekcia nových zariadení

Funkcia upozorní administrátora na úspešné prihlásenie z prehliadača, ktorý jeho účet nepoužil v nastavenej lehote. Pri prvom prihlásení po zapnutí funkcie dostane upozornenie aj email. Používateľský postup opisujú [Systémové upozornenia](../../../redactor/admin/welcome.md#systémové-upozornenia).

Cookie `wjAdminDevice` obsahuje náhodný identifikátor; tabuľka `user_login_devices` uchováva jeho hash a čas posledného prihlásenia pre používateľa. Záznam má automaticky generované `device_id`; prehliadač sa vyhľadáva podľa `user_id` a hashu cookie. Samostatné `domain_id` sa nepoužíva, pretože každý používateľ má vlastné `user_id`. Cookie má cestu `/`, atribúty `HttpOnly`, `SameSite=Lax` a pri HTTPS aj `Secure`. Každé dokončené prihlásenie obnoví cookie a zapamätanie účtu na ďalších `adminNewDeviceMaxAgeDays` dní. Bežné požiadavky počas otvorenej relácie ani neúspešné prihlásenie lehotu nepredlžujú. Odhlásenie cookie zachová. Neplatná alebo nekladná konfigurácia použije 90 dní; horná hranica platnosti cookie je 24 855 dní.

Email sa zaradí do existujúcej fronty cez `SendMail.sendLater`. Meno a adresa odosielateľa sa určia rovnako ako pri zabudnutom hesle: najprv `passwordResetDefaultSenderName` a `passwordResetDefaultSenderEmail`, potom `defaultSenderName` a `defaultSenderEmail`, nakoniec náhradné údaje používateľa. Email obsahuje prehliadač, systém, IP adresu, čas, názov servera a označenie z `dashboardEnvironmentName`. Chyba odoslania sa zaznamená bez zrušenia prihlásenia alebo upozornenia na prehľade.

Zariadenie uchováva aj posledné upozornenie: čas `create_date`, prehliadač, systém, IP adresu a stav potvrdenia alebo nahlásenia. Upozornenie zanikne po potvrdení alebo po 7 dňoch od `create_date`. Bežné prihlásenie aktualizuje iba `last_seen`. Pri prihlásení po expirácii alebo nahlásení sa upozornenie v rovnakom zázname obnoví; samostatná história udalostí sa neukladá. Odkaz v emaile používa `device_id` a zobrazí aktuálny detail zariadenia. Úloha `sk.iway.iwcm.users.devices.DeviceCleanup`, pridaná databázovou aktualizáciou, každý deň o 03:41 odstráni zariadenia nepoužité počas `adminNewDeviceMaxAgeDays` dní spolu s ich upozorneniami. Potom už detail z emailu nie je dostupný. Hodnotu `adminNewDeviceMaxAgeDays` nastavujte rovnako pre všetky domény; čistenie pokračuje aj pri vypnutej detekcii.

Evidenciu zariadení a ich upozornení spravuje `DeviceService` cez entitu `DeviceEntity` a Spring Data repozitár `DeviceRepository`. Služba vracia priamo entitu; anotácia `@JsonIgnore` skrýva interné údaje pri odoslaní na frontend. Časy sa posielajú v milisekundách a `expiresAt` sa vypočíta z `createDate`, bez ďalšieho databázového stĺpca. Táto vrstva nie je viazaná na administrátora. `AdminDeviceService` zabezpečuje zapojenie do administrátorského prihlásenia, cookie a emailové upozornenia; `AdminDeviceRestController` poskytuje akcie dostupné prihlásenému administrátorovi.
