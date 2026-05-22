# Informácie pre obchodníka - rok 2026

Tento súbor obsahuje opisy vlastností WebJET CMS dodaných v roku 2026 z pohľadu predaja. Nové záznamy sa pridávajú na vrch (pod tento úvod), takže najnovšie vlastnosti sú vždy hore.

---

## Automatizované testovanie prístupnosti webových stránok

WebJET CMS zavádza **automatizované testovanie prístupnosti (accessibility)**, ktoré overuje, či sú webové stránky a administračné rozhranie prístupné pre **všetkých používateľov** — vrátane tých so zrakovým, sluchovým, motorickým alebo kognitívnym obmedzením. Systém automaticky kontroluje súlad s medzinárodným štandardom **WCAG 2.2** (Web Content Accessibility Guidelines) na úrovniach A a AA, čo je požiadavka legislatívy EÚ aj Slovenska pre weby verejného sektora a čoraz viac aj pre komerčné subjekty.

Pre zákazníka to v praxi znamená, že **každá zmena na webe môže byť automaticky skontrolovaná** z hľadiska prístupnosti ešte pred nasadením do prevádzky. Vývojár tak nemusí manuálne kontrolovať desiatky pravidiel, pretože systém to môže robiť za neho automaticky a opakovane pri každej zmene.

Testovanie prístupnosti môže byť **zabudované priamo do vývojového procesu**, nie je to externý audit vykonaný raz ročne. To znamená, že problémy sa zachytávajú priebežne a opravujú sa v momente vzniku, čo je **výrazne lacnejšie a rýchlejšie** než dodatočná oprava po externom audite. Systém generuje **prehľadné HTML reporty** s detailným popisom každého porušenia, čo uľahčuje komunikáciu medzi vývojovým tímom a zodpovednými osobami za prístupnosť.

**Hlavné benefity:**

- **Súlad s legislatívou**: Automatická kontrola zabezpečuje, že web spĺňa požiadavky európskej smernice o prístupnosti webových sídiel (EAA) a slovenskej legislatívy, čím zákazník predchádza právnym rizikám a pokutám.
- **Inkluzívny web pre všetkých**: Web je prístupný aj pre ľudí so zdravotnými obmedzeniami, čo rozširuje potenciálnu cieľovú skupinu a zlepšuje reputáciu organizácie.
- **Priebežná kontrola namiesto jednorazového auditu**: Každá zmena je automaticky overená, čím sa problémy zachytávajú okamžite — oprava v momente vzniku je rádovo lacnejšia než dodatočný audit.
- **Nižšie náklady na opravu**: Včasná detekcia porušení znižuje náklady na opravu prístupnosti až o 80 % v porovnaní s opravami po nasadení do produkcie.
- **Prehľadné reporty**: Automaticky generované HTML reporty s popisom porušení a ich závažnosťou zjednodušujú prioritu opráv a komunikáciu v tíme.
- **Podpora štandardu WCAG 2.2**: Kontrola pokrýva najnovšiu verziu štandardu vrátane úrovní A a AA, čo zabezpečuje aktuálnosť aj voči budúcim legislatívnym požiadavkám.

Podrobná dokumentácia: [Testovanie prístupnosti](../../developer/testing/a11y.md)

## AI Skills — inteligentné zručnosti pre rýchlejší vývoj a správu CMS

WebJET CMS integruje sadu **AI Skills** — špecializovaných zručností pre umelú inteligenciu, ktoré výrazne **zrýchľujú vývoj, údržbu a rozširovanie** webových projektov. AI Skills fungujú priamo vo vývojovom prostredí (VS Code s GitHub Copilot) a dokážu na základe jednoduchej požiadavky **automaticky generovať hotový kód, testy, dokumentáciu aj celé nové moduly** v súlade s konvenciami a štruktúrou WebJET CMS. Vývojár tak nemusí ručne vytvárať desiatky súborov a pamätať si všetky technické detaily — stačí opísať, čo potrebuje, a AI Skills dodajú funkčný výsledok.

Pre zákazníka to znamená predovšetkým **výrazne rýchlejšiu dodávku nových funkcií a úprav**. Zmeny, ktoré predtým trvali hodiny alebo dni, je možné dodať v priebehu minút. Rovnako dôležitá je možnosť **rýchleho prototypovania** — zákazník si môže nechať pripraviť prototyp nového modulu, formulára alebo administračnej stránky takmer okamžite a rozhodnúť sa, či je smer správny, ešte pred investíciou do plného vývoja. Ak zákazník disponuje vlastným vývojovým tímom a upravuje si projekt samostatne, môže AI Skills **využívať priamo** — systém ho prevedie celým procesom a zabezpečí, že výsledok je kompatibilný s architektúrou WebJET CMS.

Nasadenie AI Skills zároveň zvyšuje **kvalitu a konzistenciu** dodávaného kódu. Každá zručnosť vynucuje osvedčené postupy, automaticky pridáva testy a dodržiava projektové konvencie, čím sa znižuje riziko chýb a zjednodušuje budúca údržba.

**Hlavné benefity:**

- **Rýchlejšia dodávka**: Nové funkcie a úpravy sú k dispozícii v zlomku pôvodného času, čo skracuje čas uvedenia na trh.
- **Rýchle prototypovanie**: Zákazník získa funkčný prototyp nového modulu takmer okamžite a môže ho vyhodnotiť pred schválením plného vývoja.
- **Nižšie náklady na vývoj**: Automatizácia rutinných úloh znižuje počet potrebných vývojárskych hodín.
- **Vyššia kvalita kódu**: AI Skills dodržiavajú osvedčené postupy, generujú testy a kontrolujú konzistenciu, čím sa znižuje počet chýb.
- **Nezávislosť zákazníka**: Zákazníci s vlastným vývojovým tímom môžu AI Skills využívať sami na rozšírenie a prispôsobenie svojho projektu.
- **Jednoduchosť použitia**: Stačí opísať požiadavku bežným jazykom — AI Skills prevedú zámer na hotový, funkčný kód.

### Dostupné AI Skills

| Zručnosť | Popis |
| ----------- | ------- |
| **Vytvorenie aplikácie (AppStore)** | Vygeneruje kompletnú aplikáciu pre editor stránok — Java triedu, šablónu, konfiguráciu a registráciu do zoznamu aplikácií. |
| **Vytvorenie administračnej stránky (DataTable)** | Pripraví celý CRUD modul pre administráciu — databázovú entitu, REST rozhranie, HTML stránku a automatizované testy. |
| **Automatizované E2E testy (CodeceptJS)** | Napíše end-to-end testy pre prehliadač, ktoré overia funkčnosť stránok, formulárov a oprávnení. |
| **Revízia kódu (Code Review)** | Skontroluje zmeny v kóde z hľadiska správnosti, bezpečnosti, spätnej kompatibility a dodržiavania konvencií projektu. |
| **Audit prístupnosti (Accessibility)** | Vykoná audit webovej prístupnosti podľa štandardu WCAG 2.2 a navrhne opravy pre klávesovú navigáciu, kontrast a čítačky obrazovky. |
| **Aktualizácia dokumentácie** | Automaticky doplní technickú dokumentáciu na základe zmien v kóde, čím udržiava dokumentáciu vždy aktuálnu. |
| **Preklad komentárov** | Preloží komentáre v zdrojovom kóde zo slovenčiny do angličtiny bez zmeny funkčnosti, čím zlepšuje čitateľnosť pre medzinárodné tímy. |
| **Marketingový obsah** | Na základe dodaných zmien vygeneruje podklady pre blog, sociálne siete alebo changelog — ušetrí čas marketingovému tímu. |
| **Opis vlastností pre predaj** | Analyzuje technické zmeny a vytvorí zrozumiteľný opis z pohľadu zákazníka a obchodných výhod. |

## Prihlasovanie cez OAuth2/Keycloak

WebJET CMS teraz podporuje **prihlasovanie používateľov prostredníctvom externých poskytovateľov identity** ako sú Google, Facebook, GitHub, Okta alebo podnikový Keycloak server. Technicky ide o štandard **OAuth2/OpenID Connect** — v praxi to znamená, že používatelia sa môžu prihlásiť **jedným kliknutím cez účet, ktorý už majú** (napríklad firemný Google účet alebo **podnikový SSO** systém), bez nutnosti pamätať si ďalšie heslo. Administrátor webu si jednoducho nakonfiguruje, ktorých poskytovateľov chce povoliť, a systém automaticky zobrazí príslušné prihlasovacie tlačidlá.

Kľúčovou výhodou je **automatická synchronizácia skupín a práv**. Ak organizácia používa podnikový identity server (napr. Keycloak), WebJET CMS dokáže pri každom prihlásení automaticky prevziať skupiny a role, v ktorých je používateľ zaradený, a **priradiť mu zodpovedajúce práva** v CMS. To eliminuje potrebu manuálnej správy oprávnení — keď sa zmení rola zamestnanca v podnikovom systéme, **zmena sa automaticky prenesie aj do WebJET CMS**. Administrátori sú nastavovaní automaticky na základe členstva v definovanej skupine, čo zjednodušuje správu prístupov aj vo veľkých organizáciách.

Riešenie je **flexibilné a rozšíriteľné** — zákazník môže nakonfigurovať ľubovoľného OAuth2 poskytovateľa, nielen preddefinovaných (Google, Facebook, GitHub, Okta). Podporované je aj **súčasné použitie viacerých poskytovateľov** (napr. Keycloak pre administrátorov a Google pre zákaznícku zónu) a konfigurácia sa dá úplne prispôsobiť potrebám organizácie vrátane vlastných atribútov pre prihlasovacie meno. Pre zákaznícku zónu aj pre administráciu je možné nastaviť rôznych poskytovateľov s rôznymi úrovňami synchronizácie práv.

**Hlavné benefity:**

- **Jednotné prihlásenie (SSO)**: Používatelia sa prihlasujú účtom, ktorý už poznajú — žiadne ďalšie heslá na zapamätanie, čo zvyšuje bezpečnosť aj pohodlie.
- **Automatická synchronizácia práv**: Skupiny a role sa preberajú z podnikového identity servera pri každom prihlásení — odpadá manuálna správa oprávnení v CMS.
- **Podpora ľubovoľného OAuth2 poskytovateľa**: Okrem preddefinovaných (Google, Facebook, GitHub, Okta) je možné nakonfigurovať akýkoľvek vlastný OAuth2/OpenID Connect server.
- **Bezpečnosť na podnikovej úrovni**: Autentifikácia prebieha na strane overeného poskytovateľa — WebJET CMS nikdy neukladá heslá externých služieb, čo znižuje bezpečnostné riziká.
- **Oddelená konfigurácia pre admin a zákaznícku zónu**: Rôzni poskytovatelia pre rôzne časti systému umožňujú presné riadenie prístupov podľa typu používateľa.
- **Nižšie prevádzkové náklady**: Centrálna správa používateľov v jednom systéme (napr. Keycloak) znižuje administratívnu záťaž a eliminuje duplicitnú správu účtov.
- **Jednoduchá inštalácia**: Pre populárnych poskytovateľov (Google, Facebook) stačí nastaviť dva konfiguračné parametre; pre podnikový Keycloak je k dispozícii pripravená Docker konfigurácia.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/q8xs3qDq-G4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

Podrobná dokumentácia: [OAuth2 Autentifikácia](../../install/oauth2/oauth2.md) | [Keycloak - Inštalácia a konfigurácia](../../install/oauth2/keycloak.md)

## Viackrokové formuláre

WebJET CMS prináša viackrokové formuláre, ktoré **rozdeľujú dlhé formuláre na menšie a pre používateľa zrozumiteľnejšie časti**. Namiesto jedného preplneného formulára dostane návštevník **jasne vedený proces po jednotlivých krokoch**, čo znižuje pocit zahltenia a pomáha zvýšiť počet úspešne dokončených odoslaní. Táto funkcionalita je vhodná napríklad pre registrácie, dopytové formuláre, náborové formuláre, prihlášky či interné zberové procesy.

Pre zákazníka je dôležité aj to, že formulár nemusí zostať iba v základnom nastavení. Jednotlivé kroky je možné pomenovať, doplniť o úvodné texty a prispôsobiť texty tlačidiel podľa konkrétnej kampane alebo procesu. Riešenie tak spája **lepší používateľský zážitok** s vysokou mierou prispôsobenia bez potreby pripravovať každý formulár nanovo od začiatku.

**Hlavné benefity:**

- **Vyššia úspešnosť odoslania**: Rozdelenie formulára do krokov znižuje bariéru pri vypĺňaní a pomáha návštevníkov doviesť až k odoslaniu.
- **Lepší používateľský zážitok**: Formulár pôsobí prehľadne, menej stresujúco a lepšie sa používa aj pri väčšom množstve údajov.
- **Vhodné pre rôzne scenáre**: Riešenie sa dá využiť pre obchod, marketing, HR aj zákaznícke služby bez zmeny základného princípu.
- **Jednoduché prispôsobenie komunikácie**: Texty krokov a tlačidiel možno upraviť podľa konkrétneho cieľa kampane alebo firemného štýlu.

![Viackrokový formulár](../../redactor/apps/multistep-form/real-form.png)

Podrobná dokumentácia: [Viackrokové formuláre](../../redactor/apps/multistep-form/README.md)

### Flexibilný editor formulárov bez závislosti od programátora

Súčasťou riešenia je editor, v ktorom môže administrátor **formulár priebežne upravovať podľa aktuálnych potrieb**. Kroky aj jednotlivé položky sa dajú pridávať, duplikovať, presúvať, meniť ich poradie a priebežne kontrolovať v náhľade. To výrazne skracuje čas potrebný na prípravu nových formulárov a umožňuje rýchlo reagovať na nové obchodné alebo prevádzkové požiadavky.

Veľkou výhodou je aj vysoká miera variability. Pri jednotlivých poliach je možné nastaviť **povinnosť vyplnenia, validačné pravidlá, predvyplnené hodnoty**, pomocné texty či informačné bubliny. Formuláre je navyše možné **personalizovať údajmi** o prihlásenom **používateľovi** a prispôsobiť aj špecifickým scenárom zobrazenia. Pre zákazníka to znamená nižšiu závislosť od dodávateľa a väčšiu schopnosť upravovať procesy vlastnými silami.

**Hlavné benefity:**

- **Rýchle nasadenie zmien**: Marketing alebo administrátor vie upraviť formulár bez zdĺhavého vývoja a čakania na technický zásah.
- **Presnejší zber dát**: Povinné polia, pravidlá validácie a pomocné texty znižujú chybovosť a zvyšujú kvalitu získaných údajov.
- **Personalizácia pre vyšší komfort**: Predvyplnenie údajov o prihlásenom používateľovi zrýchľuje vyplnenie a znižuje počet opustených formulárov.
- **Rozšíriteľnosť do budúcna**: Typy polí a dostupné nastavenia sa dajú prispôsobiť podľa potrieb konkrétneho projektu alebo segmentu.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/XRnwipQ-mH4" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
</div>

Podrobná dokumentácia: [Editor viackrokových formulárov](../../redactor/apps/multistep-form/README.md)

### Štatistiky formulárov pre rýchle rozhodovanie

WebJET CMS dopĺňa viackrokové formuláre o **prehľadnú štatistickú sekciu**, ktorá ukazuje nielen počet odoslaných odpovedí, ale aj **priemerný čas vypĺňania**, počet dní od vytvorenia formulára a čas poslednej odpovede. Zákazník tak získa **okamžitý obraz o tom, či formulár funguje**, či je pre používateľov zrozumiteľný a či sa na ňom oplatí ďalej pracovať.

Ešte väčšiu hodnotu prinášajú **grafy odpovedí pri jednotlivých otázkach**. Organizácia si môže sama určiť, ktoré polia chce sledovať, aký typ grafu sa použije, koľko odpovedí sa zobrazí a či sa majú spojiť menej časté alebo nevyplnené odpovede. V praxi to znamená, že marketing, obchod alebo HR tím dostanú **vizuálne a rýchlo čitateľné podklady** bez potreby exportovať dáta do externých nástrojov. Riešenie zároveň ostáva flexibilné, pretože nastavenie štatistík je možné meniť priamo pri položkách formulára.

**Hlavné benefity:**

- **Okamžitý prehľad o výkonnosti formulára**: Základné metriky pomáhajú rýchlo vyhodnotiť, či formulár plní svoj cieľ.
- **Lepšie rozhodovanie bez ďalších nástrojov**: Grafy odpovedí umožňujú robiť operatívne rozhodnutia priamo v administrácii systému.
- **Vyššia kvalita interpretácie dát**: Možnosť zoskupovať odpovede, zobraziť nezodpovedané položky alebo filtrovať top hodnoty spresňuje pohľad na správanie používateľov.
- **Prispôsobenie podľa potrieb**: Typ grafu, farebnú schému aj spôsob zobrazovania možno nastaviť podľa toho, čo potrebuje konkrétny tím sledovať.
![Štatistiky formulára](../../redactor/apps/multistep-form/stat-section.png)

Podrobná dokumentácia: [Štatistiky viackrokových formulárov](../../redactor/apps/multistep-form/stat.md)

