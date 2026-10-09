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
| `adminLoginLocationApiKey` | prázdná | Zjišťování orientační polohy přihlášení podle IP adresy: prázdná hodnota použije bezplatné API IPWHOIS, `DISABLED` zjišťování vypne, API klíč použije placené API. Klíč je viditelný návštěvníkem přihlašovací stránky. Podrobnosti popisuje [Orientační poloha přihlášení](#orientační-poloha-přihlášení). |
| `adminNewDeviceDetectionEnabled` | `true` | Zapne rozpoznávání a blokování prohlížečů administrátora, upozornění na přehledu a emaily o novém zařízení. Hodnota `false` vypne i kontrolu zablokovaných zařízení. Zjišťování polohy řídí samostatně `adminLoginLocationApiKey`. |
| `adminNewDeviceMaxAgeDays` | `90` | Počet dní od posledního úspěšného přihlášení, během kterých se prohlížeč považuje za známý pro daný účet. Určuje také platnost cookie a lhůtu čištění nepoužívaných nezablokovaných zařízení. Neplatná nebo nekladná hodnota použije 90 dní. |

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

## Detekce nových zařízení

Funkce upozorní administrátora na úspěšné přihlášení z prohlížeče, který jeho účet nepoužil v nastavené lhůtě. Při prvním přihlášení po zapnutí funkce obdrží upozornění i email. Uživatelský postup popisují [Systémová upozornění](../../../redactor/admin/welcome.md#systémové-upozornění).

Cookie `wjdevice` obsahuje náhodný identifikátor; tabulka `user_login_devices` uchovává jeho hash a čas posledního přihlášení pro uživatele. Záznam má automaticky generováno `device_id` ; prohlížeč se vyhledává podle `user_id` a hashu cookie. Samostatné `domain_id` se nepoužívá, protože každý uživatel má vlastní `user_id`. Cookie má cestu `/`, atributy `HttpOnly`, `SameSite=Lax` au HTTPS i `Secure`. Každé dokončené přihlášení obnoví cookie a zapamatování účtu na dalších `adminNewDeviceMaxAgeDays` dnů. Běžné požadavky během otevřeného pořadu ani neúspěšné přihlášení lhůtu neprodlužují. Odhlášení cookie zachová. Neplatná nebo nekladná konfigurace použije 90 dní; horní hranice platnosti cookie je 24 855 dní.

Email se zařadí do existující fronty přes `SendMail.sendLater`. Jméno a adresa odesílatele se určí stejně jako u zapomenutého hesla: nejprve `passwordResetDefaultSenderName` a `passwordResetDefaultSenderEmail`, poté `defaultSenderName` a `defaultSenderEmail`, nakonec náhradní údaje uživatele. Email obsahuje prohlížeč, systém, orientační polohu, IP adresu, čas posledního přihlášení, název serveru a označení z `dashboardEnvironmentName`. Chyba odeslání se zaznamená bez zrušení přihlášení nebo upozornění na přehledu.

Zařízení uchovává i poslední upozornění: čas `create_date`, prohlížeč, systém a stav potvrzení nebo nahlášení. Upozornění zanikne po potvrzení nebo po 7 dnech od `create_date`. Každé úspěšné přihlášení aktualizuje `last_seen`, IP adresu a orientační polohu. Pokud nová poloha není dostupná, uloží se `NULL`. Při přihlášení po expiraci se upozornění ve stejném nezablokovaném záznamu obnoví; samostatná historie událostí se neukládá. Odkaz **Nebyl jsem to já** v emailu používá `device_id` a zobrazí aktuální detail zařízení. Odkaz **Byl jsem to já** navíc obsahuje náhodný 256bitový token s platností 24 hodin; potvrzení provede přihlášená administrace přes POST s CSRF ochranou. Samotné otevření GET adresy stav nemění. Úloha `sk.iway.iwcm.users.devices.DeviceCleanup`, přidaná databázovou aktualizací, každý den ve 03:41 odstraní nezablokovaná zařízení nepoužitá během `adminNewDeviceMaxAgeDays` dnů spolu s jejich upozorněními. Potom už detail z emailu není dostupný. Hodnotu `adminNewDeviceMaxAgeDays` nastavujte rovněž pro všechny domény; čištění pokračuje i při vypnuté detekci.

Evidenci zařízení a jejich upozornění spravuje `DeviceService` přes entitu `DeviceEntity` a Spring Data repozitář `DeviceRepository`. Služba vrací přímo entitu; anotace `@JsonIgnore` skrývá interní údaje při odeslání na frontend. Časy se posílají v milisekundách a `expiresAt` se vypočítá z `createDate`, bez dalšího databázového sloupce. Tato vrstva není vázána na administrátora. `AdminDeviceService` zabezpečuje zapojení do administrátorského přihlášení, cookie a emailová upozornění; `AdminDeviceRestController` poskytuje akce dostupné přihlášenému administrátorovi.

Potvrzení z upozornění i ze seznamu aktivních přihlášení vyžaduje šestimístný kód doručený na email účtu. Platí 10 minut a umožňuje 5 pokusů; opětovné odeslání je omezeno na jeden kód za minutu pro zařízení. Databáze uchovává pouze SHA-256 otisky kódu a tokenu, oddělené podle účtu, zařízení a účelu. Ověřovací údaje se neposílají ve veřejném JSON. Potvrzení, nahlášení i obnovení upozornění zneplatní oba ověřovací údaje. Opětovné odeslání kódu neobnoví počet pokusů, dokud předchozí platnost neuplyne. Po úspěšném dvoufaktorovém ověření ve WebJET CMS se nezablokované zařízení potvrdí automaticky. U nového zařízení se odešle informační email s možností nahlášení.

Klepnutí na **Zablokovat zařízení** nastaví existující pole `reported_at`, zneplatní ověřovací údaje a odhlásí známé relace daného zařízení přes běžný mechanismus odhlášení. Zablokované záznamy se při čištění neodstraňují. Odkaz **Nebyl jsem to já** v emailu otevře detail po přihlášení; samotné otevření odkazu zařízení nezablokuje.

Při dalším interaktivním přihlášení zablokovaného prohlížeče se nejprve ověří přihlašovací údaje a případné 2FA. Potom stránka `/admin/logon/device/` vyžádá šestimístný kód z emailu. Čekající identita je v samostatném session atributu `adminUser_waitingForDevice`, mimo `USER_KEY` a Spring Security kontextu. Výzva platí 15 minut, kód 10 minut, platí limit 5 pokusů a minutový odstup odesílání. Kód je vázán také na tuto výzvu a cookie. Správný kód zařízení odblokuje a potvrdí. Starý potvrzovací odkaz jej odblokovat nemůže. Při nedoručení kódu zůstává přístup uzavřen.

Kontrola platí pro administrátorské přihlášení formulářem, přístupovým klíčem, OAuth2 a NTLM, včetně přihlášení administrátora přes uživatelskou zónu. Nevztahuje se na API tokeny, HTTP Basic ani běžné návštěvníky. Nastavení `adminNewDeviceDetectionEnabled=false` vypne i kontrolu blokování. Výjimky testovacích prohlížečů neobcházejí existující blok.

Specifická přihlášení přes `doFilterLogon=true`, `doFilterLogon=redir` nebo hlavičku `wjlogontoken` kontrolují stávající blokování ještě před vytvořením přihlášené session. Zablokovaný prohlížeč obdrží HTTP 403 bez emailové výzvy; při chybě načítání stavu dostane HTTP 503. Pro odblokování je třeba se ve stejném prohlížeči přihlásit přes běžný formulář `/admin/logon/` a dokončit emailové ověření. Tato specifická přihlášení nová zařízení neevidují ani neobnovují jejich cookie. Chybějící, prázdná nebo neplatná cookie proto tuto kontrolu neaktivuje. Hlavička `wjlogontoken` vytváří přihlášenou session platnou i pro další požadavky a odlišuje se od API autentifikace přes `x-auth-token`.

Blokování se váže na cookie `wjdevice`, ne na fyzické zařízení. Po jejím vymazání nebo expiraci se prohlížeč chová jako nový; blokování proto nenahrazuje změnu prozrazeného hesla ani 2FA.

### Výjimky pro automatizované testy

E2E testy často začínají s novým profilem prohlížeče bez cookie `wjdevice`. Každé takové přihlášení proto vytvoří nové zařízení. Výjimky pro testovací prohlížeče jsou uloženy přímo v `AdminDeviceService` v neměnných množinách `IGNORED_DEVICE_DOMAINS` a `IGNORED_DEVICE_USER_AGENTS`. Nelze je měnit přes konfiguraci ani za běhu aplikace, úprava vyžaduje změnu kódu a nové nasazení.

### Orientační poloha přihlášení

Proměnná `adminLoginLocationApiKey` určuje zjišťování polohy pomocí [IPWHOIS](https://ipwhois.io/documentation):

- Prázdná hodnota (výchozí): bezplatné API, aktuálně 1 000 požadavků denně; při volání z prohlížeče se limit sdílí za doménu.
- `DISABLED`: vypne externí volání i přijímání nových výsledků. Okolní mezery a velikost písmen se ignorují. Historické údaje zůstanou uloženy.
- API klíč: placené API. **Klíč je dostupný návštěvníkům přihlašovací stránky**, protože požadavek odesílá jejich prohlížeč.

Požadavek se spustí při zobrazení přihlašovací stránky, s časovým limitem 3 sekundy. Přihlášení na ni nečeká. Dočasný výsledek v HTTP session platí 10 minut a při dokončení přihlášení se uloží do relace a zařízení. Pokud chybí, první stránka administrace provede jeden náhradní pokus. Email použije údaj dostupný při odeslání nebo text **Neznámá** ; po doplnění se znovu neposílá.

Tabulka `user_login_devices` uchovává ve sloupci `location` pouze text města a země, například `Bratislava, SK`. Neúspěšné zjišťování ani jeho vypnutí nebrání přihlášení. Poloha je údaj od prohlížeče, který lze změnit; nepoužívá se k autentifikaci, potvrzení či blokování zařízení.

Prohlížeč volá API přímo, aby služba viděla veřejnou IP jeho připojení i při přístupu do CMS přes LAN. Město může patřit firemní centrále, proxy nebo VPN bráně. IP zobrazená pod městem nadále pochází z požadavku přijatého CMS a může být interní.

Volání používá HTTPS, `referrerPolicy: "no-referrer"` a `credentials: "omit"`. Neposílá přihlašovací údaje, identifikátory uživatele či relace ani interní IP. Poskytovatel však vidí veřejnou IP, s doménou a portem CMS a běžné hlavičky prohlížeče. Jeho [zásady soukromí](https://ipwhois.io/privacy) připouštějí logování provozních údajů, využití Cloudflare a zpracování v různých zemích; neuvádějí přesnou dobu uchovávání API logů ani záruku zpracování pouze v EU. Pokud to pravidla organizace neumožňují, nastavte `DISABLED`. Při omezené CSP povolte v `connect-src` pouze používaný endpoint `https://ipwho.is` nebo `https://ipwhois.pro`.
