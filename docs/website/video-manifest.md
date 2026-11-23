# Videá z dokumentácie

V slovenskej verzii sú preferované tematicky zodpovedajúce videá pred statickými snímkami. Šesť videí má spolu jedenásť umiestnení v šiestich HTML fragmentoch v `sk/`. Osem pôvodných snímok v obsahu nahrádzajú videá; na stránke Funkcie pribudli navyše ukážky publikovania a PageBuildera a v Benefitoch ukážka viackrokových formulárov. Anglické fragmenty v `en/` používajú namiesto týchto videí screenshoty.

| Video | Použitie | Podklad v dokumentácii |
| --- | --- | --- |
| [Spoznajte WebJET CMS za 90 sekúnd](https://www.youtube.com/watch?v=il3FqLyqy4Y) | Úvodná ukážka na stránke Domov | [Scenár videa](../../src/test/webapp/video/406-website-content.js) |
| [Page Builder – nová verzia](https://www.youtube.com/watch?v=B_m_vPPel80) | Benefity, Funkcie (sekcia PageBuilder), Pre agentúry | [PageBuilder](../sk/redactor/webpages/pagebuilder.md), [sales 2026](../sk/sales/2026/README.md) |
| [AI asistenti](https://www.youtube.com/watch?v=LhXo7zx7bEc) | AI sekcia na Domov a Funkciách | [AI asistent](../sk/redactor/ai/README.md) |
| [Plánované publikovanie textu, obrázkov aj súborov](https://www.youtube.com/watch?v=z3QGUdDG2pk) | Benefity a Funkcie | [Plánované publikovanie](../sk/redactor/webpages/planned-publishing.md) |
| [Viackrokové formuláre](https://www.youtube.com/watch?v=XRnwipQ-mH4) | Benefity – Viackrokové formuláre; Aplikácie – Dopyty a formuláre | [Vytvorenie formulára](../sk/redactor/apps/form/README.md#vytvorenie-formuláru) |
| [AppStore](https://www.youtube.com/watch?v=2r6-0zk5ZNQ) | Pre vývojárov – vlastné aplikácie | [Zobrazenie aplikácie v zozname](../sk/custom-apps/appstore/README.md) |

YouTube oEmbed pri kontrole 15. 11. 2026 vrátil pre pôvodných päť videí HTTP 200, názov a autora **WebJET od InterWay**. Úvodné video toho istého kanála bolo overené cez oEmbed 23. 11. 2026. Obsahová zhoda pôvodných videí sa posudzovala podľa dokumentácie a metadát; nejde o prepis celých videí. Caption AI videa preto opisuje asistentov všeobecne a nesľubuje ukážku všetkých najnovších funkcií četu.

Video `X2GNFn8IpCI` zo zadania je o [úvodnom prehľade administrácie](../sk/redactor/admin/welcome.md). Pre správu oprávnení, Manažér dokumentov a headless režim sa v relevantnej dokumentácii nenašlo vhodné samostatné video; ich snímky zostávajú.

## Vloženie v SK verzii

Všetky video značky v SK fragmentoch používajú používateľom zadaný formát, mení sa iba ID videa:

```text
!INCLUDE(/components/video/video_player.jsp, file=https://www.youtube.com/watch?v=B_m_vPPel80, width=425, height=355, percentageWidth=100, align=left, autoplay=0, controls=1, fullscreen=1, branding=0, rel=1, showinfo=0)!
```

Značka je vo `figure.wj-media.wj-video`, po nej nasleduje `figcaption` s popisom a priamym odkazom na YouTube. Nie je obalená odsekom, ďalším responzívnym rámom ani odkazom. Aplikácia vytvára vlastný responzívny obal; súčasný kód má predvolený pomer 16 : 9 a triedy kompatibilné s Bootstrapom 4. Existujúce `percentageWidth=100` zabezpečuje šírku obsahového stĺpca. Nepotrebuje nový CSS ani prehrávací skript v dodávanom HTML.

`autoplay=0` vypína automatické prehrávanie, ovládanie a celá obrazovka zostávajú zapnuté. Ostatné parametre sú zachované presne podľa zadania; výsledné zobrazenie značky a súvisiacich videí riadi aj samotný YouTube.

[Lokálny náhľad](README.md#lokálny-náhľad) rozpoznáva len tento konkrétny formát video aplikácie a vykreslí iframe v obale Bootstrap 4. Do výsledných HTML súborov iframe nezapisuje. Náhľad nie je testom JSP starej produkčnej inštalácie; po vložení treba skontrolovať spracovanie značiek, prehrávanie a prípadné nastavenie súhlasov/CSP existujúceho webu.

## Anglická verzia: snímky namiesto videí

EN produktové sekcie nevyžadujú, aby si návštevník zapínal preklad slovenského záznamu. Všetkých jedenásť video umiestnení má statickú náhradu z anglickej dokumentácie:

| Video v SK | EN obrázok | EN stránky | Počet |
| --- | --- | --- | --- |
| `il3FqLyqy4Y` – úvodná ukážka WebJET CMS | [pagebuilder.png](en/images/pagebuilder.png) | Home | 1 |
| `B_m_vPPel80` – PageBuilder | [pagebuilder.png](en/images/pagebuilder.png) | Benefits, Functions, For agencies | 3 |
| `LhXo7zx7bEc` – AI asistenti | [ai-chat.png](en/images/ai-chat.png) | Home, Functions | 2 |
| `z3QGUdDG2pk` – plánované publikovanie | [publication-history.png](en/images/publication-history.png) | Benefits, Functions | 2 |
| `XRnwipQ-mH4` – viackrokové formuláre | [multi-step-form.png](en/images/multi-step-form.png) | Benefits, Applications | 2 |
| `2r6-0zk5ZNQ` – AppStore | [custom-application.png](en/images/custom-application.png) | For developers | 1 |

Popisy zodpovedajú snímkam: AI obrázok ukazuje čet v editore a publikovanie je ilustrované históriou verzií. Snímky nie sú zábermi z videa ani automatickým fallbackom. Pôvod, rozmery, adresáre `/images/webjetcms-2026/en/` a zostávajúci slovenský ukážkový obsah sú uvedené v [manifeste obrázkov](image-manifest.md).

Živé novinky na úvode zobrazujú položky JSON ako karty s odkazmi. Aj EN novinka môže odkazovať na pôvodné slovenské video; náhrada screenshotmi sa týka vložených ukážok v produktových sekciách.

## Možnosti anglických titulkov

Podľa dokumentácie YouTube overenej 16. 11. 2026 nastavuje `cc_lang_pref=en` preferovaný jazyk titulkov a `cc_load_policy=1` ich zapína pri načítaní. Tieto parametre nie sú zdokumentovaným príkazom na vytvorenie anglického prekladu. Samotné doplnenie parametrov preto nestačí ako záruka anglických titulkov pri slovenských záznamoch. Pozri [parametre prehrávača YouTube](https://developers.google.com/youtube/player_parameters#cc_lang_pref).

Pri ručnom prehrávaní možno skúsiť **Settings → Subtitles/CC → Auto-translate → English**, ak prehrávač pri danom videu tieto možnosti ponúkne. Dostupnosť závisí od titulkov videa; [pomocník YouTube](https://support.google.com/youtube/answer/100078) opisuje zapínanie titulkov pre videá, ktoré ich majú. Dostupnosť anglickej stopy ani automatického prekladu nebola overená samostatne pri všetkých piatich videách.

Aktuálna metóda `getYoutubeEmbedUrl` v [VideoApp.java](../../src/main/java/sk/iway/iwcm/components/video/VideoApp.java) zachováva parametre zdrojovej YouTube URL pri zostavení embed URL. Produkčný web môže používať staršiu verziu. Ak sa EN verzia neskôr vráti k videám, treba na nej overiť výslednú URL prehrávača aj skutočnú dostupnosť a jazyk titulkov. Pripravené EN fragmenty od tejto podpory nezávisia.
