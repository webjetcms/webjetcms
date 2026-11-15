# Videá z dokumentácie

Preferované sú tematicky zodpovedajúce videá pred statickými snímkami. Päť videí má spolu desať umiestnení v šiestich HTML fragmentoch. Osem pôvodných snímok v obsahu nahrádzajú videá; na stránke Funkcie pribudli navyše ukážky publikovania a PageBuildera.

| Video | Použitie | Podklad v dokumentácii |
| --- | --- | --- |
| [Page Builder – nová verzia](https://www.youtube.com/watch?v=B_m_vPPel80) | Úvodná ukážka na Domov; Benefity; Funkcie; Pre agentúry | [PageBuilder](../sk/redactor/webpages/pagebuilder.md), [sales 2026](../sk/sales/2026/README.md) |
| [AI asistenti](https://www.youtube.com/watch?v=LhXo7zx7bEc) | AI sekcia na Domov a Funkciách | [AI asistent](../sk/redactor/ai/README.md) |
| [Plánované publikovanie textu, obrázkov aj súborov](https://www.youtube.com/watch?v=z3QGUdDG2pk) | Benefity a Funkcie | [Plánované publikovanie](../sk/redactor/webpages/planned-publishing.md) |
| [Viackrokové formuláre](https://www.youtube.com/watch?v=XRnwipQ-mH4) | Aplikácie – Dopyty a formuláre | [Vytvorenie formulára](../sk/redactor/apps/form/README.md#vytvorenie-formuláru) |
| [AppStore](https://www.youtube.com/watch?v=2r6-0zk5ZNQ) | Pre vývojárov – vlastné aplikácie | [Zobrazenie aplikácie v zozname](../sk/custom-apps/appstore/README.md) |

YouTube oEmbed pri kontrole 15. 11. 2026 vrátil pre všetkých päť videí HTTP 200, názov a autora **WebJET od InterWay**. Obsahová zhoda sa posudzovala podľa dokumentácie a metadát; nejde o prepis celých videí. Caption AI videa preto opisuje asistentov všeobecne a nesľubuje ukážku všetkých najnovších funkcií četu.

Video `X2GNFn8IpCI` zo zadania je o [úvodnom prehľade administrácie](../sk/redactor/admin/welcome.md). Pre hlavnú ukážku webu bol vybraný PageBuilder, ktorý priamo ukazuje tvorbu stránky. Pre správu oprávnení, Manažér dokumentov a headless režim sa v relevantnej dokumentácii nenašlo vhodné video; ich snímky zostávajú.

## Vloženie

Všetky produkčné fragmenty používajú používateľom zadaný formát, mení sa iba ID videa:

```text
!INCLUDE(/components/video/video_player.jsp, file=https://www.youtube.com/watch?v=B_m_vPPel80, width=425, height=355, percentageWidth=100, align=left, autoplay=0, controls=1, fullscreen=1, branding=0, rel=1, showinfo=0)!
```

Značka je vo `figure.wj-media.wj-video`, po nej nasleduje `figcaption` s popisom a priamym odkazom na YouTube. Nie je obalená odsekom, ďalším responzívnym rámom ani odkazom. Aplikácia vytvára vlastný responzívny obal; súčasný kód má predvolený pomer 16 : 9 a triedy kompatibilné s Bootstrapom 4. Existujúce `percentageWidth=100` zabezpečuje šírku obsahového stĺpca. Nepotrebuje nový CSS ani prehrávací skript v dodávanom HTML.

`autoplay=0` vypína automatické prehrávanie, ovládanie a celá obrazovka zostávajú zapnuté. Ostatné parametre sú zachované presne podľa zadania; výsledné zobrazenie značky a súvisiacich videí riadi aj samotný YouTube.

[Lokálny náhľad](README.md#lokálny-náhľad) rozpoznáva len tento konkrétny formát video aplikácie a vykreslí iframe v obale Bootstrap 4. Do výsledných HTML súborov iframe nezapisuje. Náhľad nie je testom JSP starej produkčnej inštalácie; po vložení treba skontrolovať spracovanie značiek, prehrávanie a prípadné nastavenie súhlasov/CSP existujúceho webu.

Živé novinky na úvode naďalej zobrazujú položky JSON ako karty s odkazmi. Táto úprava sa týka videoukážok v produktových sekciách.
