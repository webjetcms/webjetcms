# Konfigurace úvodní obrazovky a označení prostředí

Pozadí uvítacího panelu a označení prostředí nastavíte přes **Nastavení → Konfigurace**. Po změně obnovte stránku. Označení se zobrazuje před názvem stránky ve společné hlavičce administrace v9 a na přihlašovací stránce. Na mobilu je za tlačítkem menu. Postup úpravy hodnot popisuje [Konfigurace](README.md).

## Konfigurační proměnné

| Proměnná | Výchozí hodnota | Význam |
| --- | --- | --- |
| `dashboardHeroBackgroundImage` | `linear-gradient(111.7deg, #031D46 0%, #073780 76.92%)` | Pozadí uvítacího panelu: CSS gradient nebo cesta k obrázku. Prázdná nebo neplatná hodnota použije výchozí modrý přechod. |
| `dashboardSocialRefreshHours` | `8` | Počet hodin platnosti uloženého seznamu externích novinek. Hodnota `0` nebo záporná hodnota vypne jeho načítání i zobrazování uložených položek. Rozbalení panelu při zapnuté funkci vždy ověří aktuální seznam. |
| `overviewJsonUrl` | `https://docs.webjetcms.sk/json/` | Základní URL souborů `wjnews.sk.json`, `wjnews.cs.json` a `wjnews.en.json`. Lze nastavit vlastní HTTP(S) server nebo lokální cestu ke složce s těmito soubory. |
| `dashboardEnvironmentName` | `{ENVIRONMENT_NAME}` | Text označení. Podporuje makra i vlastní text. Zobrazuje nejvýše 8 znaků velkými písmeny; celý název zůstane v tooltipu. Prázdná hodnota označení skryje iz titulku karty. |
| `dashboardEnvironmentIcon` | `auto` | Ikona podle prostředí nebo název ikony Tabler. `rocket` nebo `ti-database`. Hodnota `none` nebo prázdná hodnota ikonu skryje. Při neexistujícím názvu se ikona nezobrazí. |
| `dashboardEnvironmentColor` | `auto` | Barva z palety dle prostředí a stylu. Vlastní barva `#RGB` či `#RRGGBB` automaticky obdrží černý nebo bílý text a ikonu s kontrastem alespoň 4,5:1. |
| `dashboardEnvironmentStyle` | `auto` | `subtle` = jemný, `strong` = výrazný. Hodnota `auto` použije výrazný styl pro PROD, jemný pro ostatní prostředí. |
| `dashboardEnvironmentDescription` | prázdná | Doplňující popis za celým názvem v tooltipu. Podporuje makra. Tooltip funguje při ukázání myší i při fokusu klávesnicí; Escape ho skryje. |
| `adminLoginLocationApiKey` | prázdná | Zjišťování orientační polohy přihlášení podle IP adresy: prázdná hodnota použije bezplatné API IPWHOIS, `DISABLED` zjišťování vypne, API klíč použije placené API. Klíč je viditelný návštěvníkem přihlašovací stránky. Podrobnosti popisuje [Orientační poloha přihlášení](#orientační-poloha-přihlášení). |
| `adminNewDeviceDetectionEnabled` | `true` | Zapne rozpoznávání a blokování prohlížečů administrátora, upozornění na přehledu a emaily o novém zařízení. Hodnota `false` vypne i kontrolu zablokovaných zařízení. Zjišťování polohy řídí samostatně `adminLoginLocationApiKey`. |
| `adminNewDeviceMaxAgeDays` | `90` | Počet dní od posledního úspěšného přihlášení, během kterých se prohlížeč považuje za známý pro daný účet. Určuje také platnost cookie a lhůtu čištění nepoužívaných nezablokovaných zařízení. Neplatná nebo nekladná hodnota použije 90 dní. |

Název se přidává také do titulku karty prohlížeče a přístupného názvu hlavičky, například `[TEST] Webové stránky | WebJET CMS`. Ikona je volitelná, text zůstává povinen. Na produkci můžete označení vypnout prázdnou hodnotou `dashboardEnvironmentName`.

Pozadí změníte přímo hodnotou `dashboardHeroBackgroundImage`, například:

- `linear-gradient(135deg, #102A43 0%, #486581 100%)` – vlastní barvy a směr přechodu. Podporovány jsou i radiální, kuželové a opakované CSS gradienty.
- `/images/company/header.jpg` – obrázek z webového sídla.
- `https://cdn.example.com/header.jpg` – obrázek z úplné HTTP(S) adresy.

Obrázek nahradí gradient a pokryje celý panel se zachováním poměru stran (`cover`), zarovnaný na střed a spodní okraj. Cestu zadávejte přímo, bez obalu `url(...)`. HTTP(S) adresa nesmí obsahovat přihlašovací údaje. Adresy začínající `//`, adresy se zpětnými lomítky nebo řídicími znaky a jiné protokoly nejsou povoleny. Maximální délka adresy je 1024 znaků.

## Novinky ze sociálních sítí

Panel **Co je nového** spojuje novinky nainstalované verze s příspěvky z YouTube a Facebooku WebJET CMS. Prohlížeč čte společný JSON ze serveru nastaveného v `overviewJsonUrl` ; zákaznický server sociální sítě nekontaktuje. Generování dat na docs serveru popisuje [Centrální seznam novinek](../../../developer/install/social-media-feed.md).

JSON musí být veřejně dostupný bez přihlášení, i při vlastní lokální cestě: požadavek neposílá přihlašovací cookies ani hlavičku `Referer`.

Relativní cesta obrázku v poli `image` se doplní podle složky s JSON. U `overviewJsonUrl=https://docs.webjetcms.sk/json/new/` se například `social-media/images/obrazok.jpg` načte z `https://docs.webjetcms.sk/json/new/social-media/images/obrazok.jpg`. Při přesunu seznamu proto přeneste i podsložku s obrázky. Absolutní HTTPS adresy obrázků ze starších seznamů zůstávají podporovány.

Při otevření úvodní obrazovky se použije seznam uložený v `localStorage`. Pokud chybí nebo dosáhl platnosti nastavené v `dashboardSocialRefreshHours` (výchozí 8 hodin), obnoví se na pozadí. Při každém rozbalení se seznam ověří i před uplynutím této lhůty. Cache je uložena pod klíčem `webjet.externalNews.v1` a obsahuje také URL načteného seznamu. Při změně URL nebo jazyka se načte příslušný seznam a přepíše tuto položku; česká administrace používá soubor `wjnews.cs.json`. Ve sbalené rotaci se načte jen náhled právě zobrazené novinky; ostatní obrázky se předem nenačítají. Obrázky karet se začnou načítat po rozbalení panelu. Náhled v rotaci nemění stav přečtení.

Štítek **Nové** zůstává zobrazen, dokud uživatel nerozbalí nepřečtený obsah. Rozbalení potvrdí lokální novinky i právě zobrazené sociální položky. Úspěšné uložení do nastavení účtu obsahuje datum přečtení `readAt` ; pro sociální příspěvky se uchovávají i otisky jejich ID pod klíčem `dashboard.externalNews.<odtlačok základnej URL>`. Přepnutí jazyka ani úprava překladu téhož příspěvku nevytvoří nové upozornění. Nové ID jej vytvoří bez ohledu na datum publikování. Data přijatá až po sbalení nebo ve skryté kartě prohlížeče se neoznačí za přečtená; pokud je panel stále rozbalen, označí se při návratu do karty.

Při chybě načítání se externí karty, položky rotace a jejich upozornění skryjí i tehdy, když prohlížeč má uložená starší data. Panel nezobrazuje hlášení o načítání ani chybě; zůstanou lokální novinky verze a odkazy na sociální sítě. Uložená cache se chybou nevymaže.

Hodnota `dashboardSocialRefreshHours=0` vypne požadavky na JSON a obrázky, externí karty, jejich rotaci i upozornění na nepřečtené položky. Platí to i pro data, která již prohlížeč uložil. Novinky nainstalované verze a odkazy na oba sociální kanály zůstanou dostupné. Po změně konfigurace obnovte stránku administrace.

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

## Detekce nových zařízení

**Detekci vypnete nastavením `adminNewDeviceDetectionEnabled=false`** v části **Nastavení → Konfigurace**. Vypne se rozpoznávání nových prohlížečů, upozornění na přehledu, emaily o novém zařízení i kontrola již zablokovaných zařízení. Výchozí hodnota je `true`. Zjišťování polohy má [samostatné nastavení](#orientační-poloha-přihlášení).

Při zapnuté detekci WebJET CMS upozorňuje administrátora na úspěšné přihlášení z neznámého prohlížeče. Upozornění obdrží i při prvním přihlášení po zapnutí funkce. Postup potvrzení nebo zablokování zařízení popisují [Systémová upozornění](../../../redactor/admin/welcome.md#systémové-upozornění). Detekce se nevztahuje na běžné návštěvníky, API tokeny ani HTTP Basic.

Prohlížeč se rozpoznává pomocí cookie `wjdevice`. Proměnná `adminNewDeviceMaxAgeDays` určuje dobu zapamatování od posledního úspěšného přihlášení, ve výchozím nastavení **90 dní**. Nastavujte ji rovněž pro všechny domény. Po uplynutí této lhůty se nepoužívaná nezablokovaná zařízení is upozorněními automaticky vymažou; čištění pokračuje i při vypnuté detekci. Zablokovaná zařízení se nemažou.

**Ověřte doručování emailů a správnost emailových adres administrátorů.** Odesílatel se nastavuje stejně jako při obnově hesla: `passwordResetDefaultSenderName` a `passwordResetDefaultSenderEmail`, případně obecné `defaultSenderName` a `defaultSenderEmail`. Odblokování prohlížeče vyžaduje kód doručený na email účtu, i když uživatel používá 2FA. Při problémech s přístupem použijte v daném prohlížeči formulář `/admin/logon/` a dokončete emailové ověření.

Po vymazání nebo expiraci cookie se prohlížeč považuje za nový a jeho předchozí blokování se neuplatní. Při podezření na zneužití účtu je proto třeba změnit heslo a zapnout 2FA.

### Orientační poloha přihlášení

Zjišťování polohy funguje nezávisle na detekci zařízení. **Externí volání vypnete nastavením `adminLoginLocationApiKey=DISABLED`** ; již uložené polohy zůstanou zachovány.

Proměnná `adminLoginLocationApiKey` má tyto možnosti:

- Prázdná hodnota (výchozí): bezplatné API služby [IPWHOIS](https://ipwhois.io/documentation).
- `DISABLED`: vypnuté zjišťování polohy.
- API klíč: placené API. **Klíč je viditelný návštěvníkům přihlašovací stránky**, protože službu volá jejich prohlížeč.

Poloha je orientační a může odpovídat sídlu firmy nebo VPN bráně. Nepoužívá se k ověření ani blokování zařízení. Její nedostupnost nebrání přihlášení.

Poskytovatel služby vidí veřejnou IP adresu a doménu CMS. Pokud pravidla organizace taková externí volání neumožňují, zjišťování vypněte; podmínky zpracování údajů uvádí poskytovatel v [zásadách soukromí](https://ipwhois.io/privacy). Při omezené CSP povolte v `connect-src` používaný endpoint `https://ipwho.is` nebo `https://ipwhois.pro`.

### Novinky vydání

Text `admin.overview.changelog` zapisujte jako Markdown nadpis s verzí a seznam bodů. Každý bod začíná tučným názvem, za nímž následuje pomlčka a popis. V souboru `.properties` používejte oddělovač `\\n` ; editor překladových klíčů může obsahovat běžné konce řádků.

```markdown
## WebJET CMS 2026.18

- **Schvaľovanie zmien priečinkov** – Zmeny priečinkov môžu prejsť viacúrovňovým schvaľovaním.
- **Testovanie prístupnosti** – Kontroly sú súčasťou automatizovaných testov.
```

Počet při odkazu **Co je nového** se odvodí ze seznamu. Starší text v odstavcích zůstává čitelný. V `user_settings_admin` se pro každého uživatele a jazyk uchovává záznam `dashboard.news.<jazyk>` s otiskem přečteného textu (`fingerprint`) a datem přečtení (`readAt`). Ukládá se po rozbalení viditelného panelu. Změna textu obnoví štítek **Nové** i ve stejné verzi; upozornění nevyprší po uplynutí času. Menu Nápověda čte stejný stav lokálních novinek, ale jeho otevření jej nemění. Uložení ani reset rozložení dashboardu tento záznam nemění.

Starší záznam s `firstSeen` znamenal pouze první zobrazení a neměl informaci o přečtení. Po aktualizaci se proto lokální novinky zobrazí jako nepřečtené, dokud je uživatel jednou nerozbalí. Stávající uložené ID přečtených sociálních příspěvků zůstávají platné.
