# Centrálny zoznam noviniek

Generátor na `docs.webjetcms.sk` vytvára zoznam posledných videí z YouTube a príspevkov z Facebooku. Zákaznícke inštalácie čítajú spoločné súbory `wjnews.sk.json`, `wjnews.cs.json` a `wjnews.en.json`; na sociálne siete nepotrebujú vlastný prístup ani token. JSON zachováva pôvodný formát pre staršie verzie WebJET CMS. Novinky nainštalovanej verzie pochádzajú z lokálnych prekladových kľúčov a dashboard ich pridáva pri zobrazení.

## Nasadenie a nastavenie úlohy

1. Skompilujte projekt a nasaďte triedy z `build/classes/java/main/sk/iway/aceintegration/socialmedia/` do rovnakého balíka v `WEB-INF/classes/` na docs serveri. Balík nie je súčasťou zákazníckeho WAR, JAR ani aktualizačných archívov. Pri aktualizácii tried reštartujte aplikáciu.
2. Procesu aplikačného servera povoľte zápis do `/json/` a `/WEB-INF/social-media/` v koreňovom priečinku webovej aplikácie. Súkromný priečinok nesmie byť prístupný cez webový server. Vstupné JSON súbory netreba pripravovať ani kopírovať pri [deploymente](deployment.md#zoznam-noviniek-na-docs-serveri).
3. V **Nastavenia → Konfigurácia** vytvorte potrebné premenné z tabuľky nižšie. Predvolené hodnoty sú definované priamo v balíku generátora, preto nemusia byť v zozname konfigurácie pred prvým uložením.
4. V **Nastavenia → Automatizované úlohy** vytvorte úlohu `sk.iway.aceintegration.socialmedia.SocialMediaFeedGenerator`. **Parametre úlohy** nechajte prázdne, nastavte požadované časy a v poli **Beží na uzle** vyberte jeden konkrétny uzol. Úloha sa pri inštalácii neregistruje automaticky.
5. Spustite ju manuálne na zvolenom uzle a skontrolujte výstupy podľa časti [Overenie](#overenie). Ovládanie časovania a manuálneho spustenia opisujú [Automatizované úlohy](../../admin/settings/cronjob/README.md).

Cesty v konfigurácii sú relatívne ku koreňu webovej aplikácie. Súkromná cache dát a prekladov je v `/WEB-INF/social-media/feed-cache.json`; náhľady obrázkov sa ukladajú do `social-media/images/` pod výstupným priečinkom.

| Premenná | Predvolená hodnota | Význam |
| --- | --- | --- |
| `socialMediaFeedOutputDirectory` | `/json/` | Verejný výstupný priečinok. |
| `socialMediaFeedPublicBaseUrl` | `https://docs.webjetcms.sk` | Verejná HTTPS adresa servera pre odkazy na uložené obrázky. |
| `socialMediaYoutubeChannelId` | `UCtS48g2dGJ5WVeh89PAOeLA` | YouTube kanál, z ktorého sa číta verejný Atom feed. Nevyžaduje API kľúč. |
| `socialMediaFacebookPageId` | `100066653358802` | Facebook stránka spravovaná vydavateľom WebJET CMS. |
| `socialMediaFacebookAccessToken` | prázdna | Page access token. Pri ukladaní zapnite **Šifrovať**. Token sa zadáva iba na docs serveri. |
| `socialMediaFacebookApiVersion` | `v26.0` | Verzia Graph API použitá pri čítaní príspevkov. |

Tieto premenné riadia iba centrálny generátor. Interval obnovovania a vypnutie načítavania v zákazníckej administrácii riadi [konfigurácia dashboardu](../../admin/setup/configuration/dashboard.md#novinky-zo-sociálnych-sietí).

## Prístup k Facebooku a preklady

Pre vlastnú stránku pripravte Meta aplikáciu a **Page access token** s oprávnením `pages_read_engagement`. Získanie tokenu a výber spravovanej stránky vyžaduje aj `pages_show_list`; skontrolujte dostupnosť oprávnení pre konkrétnu aplikáciu a účet správcu podľa [referencie oprávnení Meta](https://developers.facebook.com/docs/permissions/). Generátor nič nepublikuje a nevyžaduje oprávnenie na zápis.

1. V [Meta for Developers](https://developers.facebook.com/apps/) vytvorte alebo vyberte aplikáciu určenú na prístup k vlastnej Facebook stránke. Účet, ktorým prístup pripravujete, musí mať prístup k aplikácii aj oprávnenie spravovať danú stránku.
2. Podľa [postupu získania Page access tokenu](https://developers.facebook.com/docs/pages/access-tokens/) povoľte čítanie stránky a vyberte stránku `100066653358802`. Použite token stránky; samotný App access token alebo User access token do generátora nepatrí. Rozsah oprávnení a požadované schválenie aplikácie závisia od spôsobu nasadenia a rolí účtu.
3. Overte, že získaný token dokáže čítať `/{page-id}/published_posts`. Pri príprave trvalej prevádzky skontrolujte jeho platnosť a postup obnovy podľa dokumentácie Meta. Zmena oprávnení účtu alebo aplikácie môže platnosť prístupu zrušiť.
4. Token uložte do šifrovanej konfigurácie docs servera podľa nasledujúceho odseku a spustite generátor manuálne. Overte, že verejný JSON obsahuje položky so `source: "facebook"`; samotné úspešné spustenie úlohy neznamená úspech Facebooku, pretože pri jeho výpadku môže úloha aktualizovať ostatné zdroje.

Príspevky číta cez `GET /v26.0/{page-id}/published_posts` podľa [oficiálnej referencie](https://developers.facebook.com/docs/graph-api/reference/page/published_posts/). Token posiela v autorizačnej hlavičke. Nevkladajte ho do parametrov úlohy, URL, zdrojových súborov ani verejného JSON. Uložte ho do `socialMediaFacebookAccessToken` so zapnutou voľbou **Šifrovať**; nepoužívajte dočasné nastavenie. Generátor token uložený bez šifrovania nepoužije. Po získaní alebo obnovení tokenu overte skutočné načítanie vlastnej stránky.

Bez tokenu sa aktualizuje YouTube. Ak už existujú úspešne načítané Facebook príspevky, zostanú zachované aj pri chýbajúcom alebo neplatnom tokene. Verziu API meňte až po overení kompatibility podľa [prehľadu zmien Graph API](https://developers.facebook.com/docs/graph-api/changelog/).

Texty sa považujú za slovenské. Preklady do češtiny a angličtiny používa `TranslationService`; na docs serveri nastavte [prekladač](../../admin/setup/translation.md). Cache odlišuje ID príspevku, obsah a cieľový jazyk, preto nezmenené texty nespotrebúvajú ďalšie preklady. Pri chybe sa použije posledný dostupný preklad alebo slovenský text a ďalší beh preklad zopakuje. Identita príspevku, odkaz, dátum a obrázok zostávajú vo všetkých jazykoch rovnaké.

## Formát a priebeh generovania

Koreň JSON zostáva objekt s poľom `news`. Každá položka má textové polia `title`, `perex` a `link`. Nové voliteľné polia sú `id`, `source`, `publishedAt` a `image`. Ukážka jednej položky:

```json
{
  "title": "Nové možnosti štýlov v editore",
  "perex": "Pozrite si ukážku práce so štýlmi.",
  "link": "https://www.youtube.com/watch?v=VIDEO_ID",
  "id": "youtube:VIDEO_ID",
  "source": "youtube",
  "publishedAt": "2026-10-18T09:00:00Z",
  "image": "https://docs.webjetcms.sk/json/social-media/images/VIDEO_ID.jpg"
}
```

Generátor zaradí najviac tri posledné položky z každej siete, spoločne zoradené od najnovšej. Ak nie je dostupný žiadny Facebook príspevok, zaradí štyri YouTube videá, pokiaľ ich kanál ponúka. Verejný súbor obsahuje iba sociálne príspevky; novinky vydaní ani obsah starého verejného JSON sa doň nepridávajú.

Starší klient zobrazí zoznam až pri **aspoň štyroch položkách**. Ak zdroje poskytujú menej, generátor uloží skutočný počet bez dopĺňania náhradných záznamov. Aktuálny dashboard zobrazí aj kratší zoznam; v starom klientovi sa v takom prípade externé novinky nezobrazia. Súkromná cache uchováva aj štvrté YouTube video pre beh bez Facebook príspevkov.

Starý klient nové atribúty ignoruje a použije text aj poradie poľa. Sociálne texty generátor očistí tak, aby cez historický Markdown renderer nevložili HTML, obrázok ani spustiteľný odkaz. Náhľad patrí iba do `image`. Nový dashboard podporuje aj pôvodné položky bez nových atribútov a nezobrazí druhýkrát odkaz na changelog aktuálne nainštalovanej verzie. Sociálne príspevky o tej istej verzii zostávajú.

Pri výpadku jednej siete sa zachovajú jej posledné úspešné údaje a druhá sa môže aktualizovať. Pri úplnom výpadku bez uložených sociálnych položiek zostanú existujúce verejné súbory nezmenené. Úspešná odpoveď s prázdnym zoznamom však môže vytvoriť aj prázdny výstup. Pri chybe náhľadu môže byť karta textová. Generátor pripraví a overí všetky jazykové výstupy pred publikovaním; jednotlivé súbory nahrádza atómovo. Poškodený vstup alebo neúplný zápis nesmie nahradiť platný verejný JSON. Súbežnému behu bráni zámok; aj tak plánujte úlohu na jednom uzle.

## Overenie

- Otvorte verejné `https://docs.webjetcms.sk/json/wjnews.sk.json`, `wjnews.cs.json` a `wjnews.en.json`. Overte platný JSON, skutočný počet sociálnych položiek, poradie a nezmenené polia `title`, `perex`, `link`. Overte rovnaké ID a odkazy vo všetkých jazykoch a pri behu bez Facebooku štyri YouTube videá, ak sú dostupné.
- Overte dostupnosť odkazov `image` cez HTTPS z administrácie na inej doméne a zablokovanie verejného prístupu k `/WEB-INF/social-media/`.
- Pre verejné JSON odpovede aj obrázky pod `/json/social-media/images/` nastavte CORS tak, aby ich mohla čítať administrácia zákazníka. Pri verejných údajoch bez prihlasovacích údajov možno použiť `Access-Control-Allow-Origin: *`. Obrázky sa načítavajú v režime `crossorigin="anonymous"`, preto potrebujú túto hlavičku tiež. Skontrolujte aj povolenie zdroja v CSP zákazníckej administrácie (`connect-src` pre JSON a `img-src` pre náhľady).
- V aktuálnom WebJET CMS rozbaľte **Čo je nové** a overte, že lokálne novinky verzie sú prvé a pod nimi sú externé karty. Na staršej verzii overte textový zoznam z rovnakej URL.
- Po nastavení Facebook tokenu overte živé načítanie príspevkov. Pri opakovanom behu skontrolujte, že sa príspevky nezdvojujú a nezmenené texty sa znova neprekladajú.
- Skontrolujte audit vykonania automatizovanej úlohy a log pri chybe zdroja alebo prekladu. Verejné výstupy nesmú obsahovať token, súkromnú cache ani neúplný JSON.
