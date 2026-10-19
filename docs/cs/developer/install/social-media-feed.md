# Centrální seznam novinek

Generátor na `docs.webjetcms.sk` vytváří seznam posledních videí z YouTube a příspěvků z Facebooku. Zákaznické instalace čtou společné soubory `wjnews.sk.json`, `wjnews.cs.json` a `wjnews.en.json` ; na sociální sítě nepotřebují vlastní přístup ani token. JSON zachovává původní formát pro starší verze WebJET CMS. Novinky nainstalované verze pocházejí z lokálních překladových klíčů a dashboard je přidává při zobrazení.

## Nasazení a nastavení úlohy

1. Vytvořte archiv příkazem `ant -f ant/build.xml finalwar` a nasaďte `build/updatezip/webjetcms.war` na docs server. Cíl `finalwar` zahrnuje také třídy generátoru z balíku `sk.iway.aceintegration.socialmedia` ; zákaznické JAR a aktualizační ZIP archivy je neobsahují. Po nasazení restartujte aplikaci.
2. Procesu aplikačního serveru povolte zápis do `/json/` a `/WEB-INF/social-media/` v kořenové složce webové aplikace. Soukromá složka nesmí být přístupná přes webový server. Vstupní JSON soubory není třeba připravovat ani kopírovat při [deploymentu](deployment.md#seznam-novinek-na-docs-serveru).
3. V **Nastavení → Konfigurace** vytvořte potřebné proměnné z tabulky níže. Výchozí hodnoty jsou definovány přímo v balíčku generátoru, proto nemusí být v seznamu konfigurace před prvním uložením.
4. V **Nastavení → Automatizované úlohy** vytvořte úlohu `sk.iway.aceintegration.socialmedia.SocialMediaFeedGenerator`. **Parametry úlohy** nechte prázdné, nastavte požadované časy a v poli **Běží na uzlu** vyberte jeden konkrétní uzel. Úloha se při instalaci neregistruje automaticky.
5. Spusťte ji manuálně na zvoleném uzlu a zkontrolujte výstupy podle části [Ověření](#ověření). Ovládání časování a manuálního spuštění popisují [Automatizované úlohy](../../admin/settings/cronjob/README.md).

Silnice v konfiguraci jsou relativně ke kořenu webové aplikace. Soukromá cache dat a překladů je v `/WEB-INF/social-media/feed-cache.json` ; náhledy obrázků se ukládají do `social-media/images/` pod výstupní složkou.

| Proměnná | Výchozí hodnota | Význam |
| --- | --- | --- |
| `socialMediaFeedOutputDirectory` | `/json/` | Veřejná výstupní složka. |
| `socialMediaYoutubeChannelId` | `UCtS48g2dGJ5WVeh89PAOeLA` | YouTube kanál, ze kterého se čte veřejný Atom feed. Nevyžaduje API klíč. |
| `socialMediaFacebookPageId` | `237316516345904` | ID stránky WebJET CMS pro Graph API, ověřené přes `GET /me?fields=id,name` s Page tokenem. |
| `socialMediaFacebookAccessToken` | prázdná | Page access token. Při ukládání zapněte **Šifrovat**. Token se zadává pouze na docs serveru. |
| `socialMediaFacebookApiVersion` | `v26.0` | Verze Graph API použita při čtení příspěvků. |

Tyto proměnné řídí pouze centrální generátor. Interval obnovování a vypnutí načítání v zákaznické administraci řídí [konfigurace dashboardu](../../admin/setup/configuration/dashboard.md#novinky-ze-sociálních-sítí).

## Přístup k Facebooku a překlady

Pro vlastní stránku připravte Meta aplikaci a **Page access token** s oprávněním `pages_read_engagement`. Získání tokenu a výběr spravované stránky vyžaduje také `pages_show_list` ; zkontrolujte dostupnost oprávnění pro konkrétní aplikaci a účet správce podle [reference oprávnění Meta](https://developers.facebook.com/docs/permissions/). Generátor nic nepublikuje a nevyžaduje oprávnění k zápisu.

1. V [Meta for Developers](https://developers.facebook.com/apps/) vytvořte nebo vyberte aplikaci určenou pro přístup k vlastní Facebook stránce. Účet, kterým přístup připravujete, musí mít přístup k aplikaci i oprávnění spravovat danou stránku.
2. Podle [postupu získání Page access tokenu](https://developers.facebook.com/docs/pages/access-tokens/) povolte čtení stránky a vyberte stránku WebJET CMS. Použijte token stránky; samotný App access token nebo User access token do generátoru nepatří. Rozsah oprávnění a požadované schválení aplikace závisí na způsobu nasazení a rolích účtu.
3. S vybraným Page tokenem ověřte v Graph API Exploreru `GET /me?fields=id,name`. Odpověď musí označovat správnou stránku. Její `id` nastavte do `socialMediaFacebookPageId` ; ID z veřejné adresy `profile.php` nepovažujte automaticky za ověřené ID pro Graph API. Potom ověřte čtení `/{page-id}/published_posts`. Při přípravě trvalého provozu zkontrolujte platnost tokenu a postup obnovy podle dokumentace Meta. Změna oprávnění účtu nebo aplikace může platnost přístupu zrušit.
4. Token uložte do šifrované konfigurace docs serveru podle následujícího odstavce a spusťte generátor manuálně. Ověřte, že veřejný JSON obsahuje položky se `source: "facebook"` ; samotné úspěšné spuštění úlohy neznamená úspěch Facebooku, neboť při jeho výpadku může úloha aktualizovat ostatní zdroje.

Příspěvky čte přes `GET /v26.0/{page-id}/published_posts` podle [oficiální reference](https://developers.facebook.com/docs/graph-api/reference/page/published_posts/). Token posílá v autorizační hlavičce. Nevkládejte jej do parametrů úlohy, URL, zdrojových souborů ani veřejného JSON. Uložte jej do `socialMediaFacebookAccessToken` se zapnutou volbou **Šifrovat** ; nepoužívejte dočasné nastavení. Generátor token uložený bez šifrování nepoužije. Po získání nebo obnovení tokenu ověřte skutečné načtení vlastní stránky.

Bez tokenu se aktualizuje YouTube. Pokud již existují úspěšně načtené Facebook příspěvky, zůstanou zachovány i při chybějícím nebo neplatném tokenu. Verzi API měňte až po ověření kompatibility podle [přehledu změn Graph API](https://developers.facebook.com/docs/graph-api/changelog/).

Texty se považují za české. Překlady do češtiny a angličtiny používá `TranslationService` ; na docs serveru nastavte [překladač](../../admin/setup/translation.md). Cache odlišuje ID příspěvku, obsah a cílový jazyk, proto nezměněné texty nespotřebovávají další překlady. Při chybě se použije poslední dostupný překlad nebo český text a další běh překlad zopakuje. Identita příspěvku, odkaz, datum a obrázek zůstávají ve všech jazycích stejné.

## Formát a průběh generování

Kořen JSON zůstává objekt s polem `news`. Každá položka má textová pole `title`, `perex` a `link`. Nová volitelná pole jsou `id`, `source`, `publishedAt`, `image` a `perexLong`. Ukázka jedné položky:

```json
{
  "title": "Nové možnosti štýlov v editore",
  "perex": "Pozrite si ukážku práce so štýlmi.",
  "perexLong": "Pozrite si ukážku práce so štýlmi. Vo videu nastavíme vzhľad nadpisov a odsekov priamo v editore stránky.",
  "link": "https://www.youtube.com/watch?v=VIDEO_ID",
  "id": "youtube:VIDEO_ID",
  "source": "youtube",
  "publishedAt": "2026-10-18T09:00:00Z",
  "image": "social-media/images/1d26cf12a6e34c59a10f82d9139af543.jpg"
}
```

Generátor zařadí nejvýše tři poslední položky z každé sítě, společně seřazené od nejnovější. Z dvanácti načtených Facebook příspěvků nejprve vyloučí videa, Reels a sdílené odkazy na YouTube videa, teprve poté vybere nejvýše tři položky. Další stránky výsledků nenačítá. Pokud po filtrování nezůstane žádný Facebook příspěvek, zařadí čtyři YouTube videa, pokud je kanál nabízí. Veřejný soubor obsahuje pouze sociální příspěvky; novinky vydání ani obsah starého veřejného JSON se do něj nepřidávají.

Videa rozpoznává podle [údajů o přílohách z Graph API](https://developers.facebook.com/docs/graph-api/reference/story-attachment/) včetně vnořených příloh a podle známých adres videí. Textové a fotografické příspěvky zůstávají i tehdy, když je text zmiňuje video; podobnost názvů či obrázků se neposuzuje. Z cache lze vyřadit video s rozpoznatelným odkazem i bez spojení. Starší uložený příspěvek bez údajů o přílohách as běžným odkazem `/posts/id` lze odfiltrovat až po úspěšném obnovení Facebooku. Při úplném výpadku bez použitelných položek zůstane veřejný soubor nezměněn až do úspěšného obnovení.

`perex` je krátký textový výňatek s nejvýše 180 zobrazenými znaky, aby se vešel i do starého zobrazení novinek. Generátor použije první úplnou větu, pokud se do limitu vejde; jinak text zkrátí na hranici slova a přidá výpustku.

`perexLong` poskytuje aktuálnímu dashboardu delší výňatek s maximálně 1 000 znaky (Unicode code points), bez omezení na první větu. Při zkrácení se ukončí na hranici slova s ​​výpustkou. Karty upřednostní tento text; pokud chybí nebo je prázdný, použijí `perex`. Počet zobrazených řádků se přizpůsobuje dostupné výšce karty: kratší název uvolní místo dalšímu textu a odkaz zůstane zarovnán dolů. Karta rezervuje prostor alespoň na tři řádky výňatku s obrázkem a osm bez něj; při selhání obrázku se prostor pro text zvětší. Samotný text zkracuje CSS s výpustkou, počet řádků se přepočítá při změně rozměrů. Starší klient nové pole ignoruje.

Z obou výňatků generátor odstraní URL, hashtagy a značky odrážek či jiných bloků, řádky spojí do souvislého textu. Obsah seznamu může zůstat jako obyčejný text. Pokud text začíná zopakovaným názvem příspěvku, odstraní jej. Kontrola proběhne i po překladu; stejné délkové limity platí pro všechny jazyky. Příspěvek obsahující pouze název má prázdný výňatek, aby se stejný text nezobrazil dvakrát. Bezpečné kódování znaků může prodloužit zápis v JSON.

`image` obsahuje cestu relativní ke složce s JSON, například `social-media/images/1d26cf12a6e34c59a10f82d9139af543.jpg`. Dashboard ji doplní podle adresy načteného souboru vytvořené z `overviewJsonUrl`. Při hodnotě `https://docs.webjetcms.sk/json/new/` se tedy obrázek načte z `/json/new/social-media/images/`. Při přesunu výstupní složky přeneste i její podsložku s obrázky. Stejnou cestu používá i malý náhled ve sbalené rotaci, který se načte až při zobrazení příslušné položky. Při chybě náhledu zůstane text bez obrázku. Samostatná proměnná `socialMediaFeedPublicBaseUrl` se již nepoužívá. Generátor převede i vlastní uložené absolutní odkazy z cache na relativní cesty; dashboard nadále podporuje absolutní HTTPS odkazy ve starších seznamech.

Starší klient zobrazí seznam až při **alespoň čtyřech položkách**. Pokud zdroje poskytují méně, generátor uloží skutečný počet bez doplňování náhradních záznamů. Aktuální dashboard zobrazí také kratší seznam; ve starém klientovi se v takovém případě externí novinky nezobrazí. Soukromá cache uchovává také čtvrté YouTube video pro běh bez Facebook příspěvků.

Starý klient nové atributy ignoruje a použije text i pořadí pole. Sociální texty generátor očistí tak, aby přes historický Markdown renderer nevložily HTML, obrázek ani spustitelný odkaz. Náhled patří pouze do `image`. Nový dashboard podporuje také původní položky bez nových atributů a nezobrazí podruhé odkaz na changelog aktuálně nainstalované verze. Sociální příspěvky o téže verzi zůstávají.

Při výpadku jedné sítě se zachovají její poslední úspěšné údaje a druhá se může aktualizovat. Při úplném výpadku bez uložených sociálních položek zůstanou stávající veřejné soubory nezměněny. Úspěšná odpověď s prázdným seznamem však může vytvořit i prázdný výstup. Při chybě náhledu může být karta textová. Generátor připraví a ověří všechny jazykové výstupy před publikováním; jednotlivé soubory nahrazuje atomově. Poškozený vstup nebo neúplný zápis nesmí nahradit platný veřejný JSON. Souběžnému běhu brání zámek; přesto plánujte úlohu na jednom uzlu.

## Ověření

- Otevřete veřejné `https://docs.webjetcms.sk/json/wjnews.sk.json`, `wjnews.cs.json` a `wjnews.en.json`. Ověřte platný JSON, skutečný počet sociálních položek, pořadí a nezměněná pole `title`, `perex`, `link`. Ověřte stejné ID a odkazy ve všech jazycích a při běhu bez Facebooku čtyři YouTube videa, pokud jsou dostupná.
- Ověřte, že relativní cesty `image` směřují do složky s načteným JSON a obrázky jsou dostupné přes HTTPS z administrace na jiné doméně. Zkontrolujte také vlastní podsložku nastavenou v `overviewJsonUrl` a zablokování veřejného přístupu k `/WEB-INF/social-media/`.
- Pro veřejné JSON odpovědi i obrázky pod `/json/social-media/images/` nastavte CORS tak, aby je mohla číst administrace zákazníka. U veřejných údajů bez přihlašovacích údajů lze použít `Access-Control-Allow-Origin: *`. Obrázky se načítají v režimu `crossorigin="anonymous"`, proto potřebují tuto hlavičku také. Zkontrolujte také povolení zdroje v CSP zákaznické administrace (`connect-src` pro JSON a `img-src` pro náhledy).
- V aktuálním WebJET CMS rozbalte **Co je nového** a ověřte, že lokální novinky verze jsou první a pod nimi jsou externí karty. Na starší verzi ověřte textový seznam ze stejné URL.
- Po nastavení Facebook tokenu ověřte živé načtení příspěvků. Při opakovaném běhu zkontrolujte, že se příspěvky nezdvojují a nezměněné texty se znovu nepřekládají.
- Zkontrolujte audit provedení automatizované úlohy a log při chybě zdroje nebo překladu. Veřejné výstupy nesmí obsahovat token, soukromou cache ani neúplný JSON.

## Diagnostika načítání

Záznam auditu o spuštění úlohy potvrzuje pouze její vyvolání. Výsledek hledejte v logu třídy `SocialMediaFeedGenerator`. Při odmítnutí požadavku log obsahuje HTTP stav a při chybě Facebook Graph API také číselné `code` a `error_subcode`, pokud jejich odpověď poskytuje. Token, autorizační hlavičky ani text odpovědi se do diagnostiky nezapisují. Kódy ověřte podle [zpracování chyb Graph API](https://developers.facebook.com/docs/graph-api/guides/error-handling/).

Při chybě neexistujícího pole log doplní jeho název, pokud jde o pole požadované generátorem. Pokud odmítá již `published_posts`, ověřte Page token a ID stránky podle předchozího postupu. Případnou neshodu opravte v konfiguraci; generátor automaticky nepřepíná na jinou stránku.

Hlášení o chybějícím Facebook tokenu znamená, že je třeba zkontrolovat konfiguraci `socialMediaFacebookAccessToken` a její šifrování. Při chybě jednoho zdroje úloha pokračuje s druhým a použije uložené příspěvky z posledního úspěšného běhu. Pokud selžou oba zdroje a cache je prázdná, veřejné soubory nevytvoří ani nepřepíše.
