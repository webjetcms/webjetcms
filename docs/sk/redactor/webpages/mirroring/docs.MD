# Zrkadlenie - Stránky

Sekcia obsahuje prehľad previazaných stránok pod spoločným synchronizačným identifikátorom `syncId`.

![](docs_datatable.png)

## Štruktúra tabuľky

Aby sme vedeli v tabuľke čítať, musíme pochopiť štruktúru tabuľky, kde:

- **Riadky**, každý riadok obsahujú všetky stránky (presnejšie cesty k stránkam), ktoré sú navzájom previazané rovnakou hodnotou parametera `syncId` (minimálne jedna stránka)
- **Stĺpce** sa delia nasledovne:
  - **SyncID**, hodnota synchronizačného identifikátora, ktorým sú stránky v riadku previazané
  - **Stav**, ikony, ktoré upozorňujú na špeciálne stavy (viac v sekcii [stav previazania](./docs#stav-previazania))
  - **sk, en, ...**, sú automaticky generované stĺpce, kde každý stĺpec obsahuje stránky pre danú jazykovú mutáciu. Tento jazyk sa získa z rodičovského priečinka stránky alebo jeho šablóny. Počet stĺpcov v tabuľke sa dynamický mení a závisí na tom, v koľkých jazykových mutáciách sú stránky previazané. Ak hodnota v stĺpci chýba, tak neexistuje pre daný `syncId` previazaná stránka v danej jazykovej mutácií.

!>**Upozornenie:** v prípade existencie viacerých previazaných stránok s rovnakou hodnotou `syncId` a v rovnakej jazykovej mutácií, ich hodnoty sa v stĺpci pre danú jazykovú mutáciu spoja, takže hodnota v stĺpci bude obsahovať cesty k viacerým stránkam.

## Stav previazania

Stĺpec **Stav** ponúka pomocou ikon rýchly prehľad o stave previazania. Podporuje nasledujúce stavy:

- <i class="ti ti-exclamation-circle" style="color: #ff4b58;"></i>, ikona zodpovedná stavu **Zlé mapovanie**. Previazanie stránok nadobudne tento stav v prípade objavenia viacerých previazaných stránok v rovnakej jazykovej mutácií.
- <i class="ti ti-alert-triangle" style="color: #fabd00;"></i>, ikona zodpovedná stavu **Nerovnomerné vnorenie**. Previazanie stránok nadobudne tento stav v prípade rozdielnej hĺbky previazaných stránok od koreňového priečinka.
- **nič**, žiadna ikona sa nezobrazuje v prípade ak previazanie je korektné (nespadá do predchazdajúcich stavov)

### Zlé mapovanie

Nakoľko previazané by mali byť iba stránky s rovnakým obsahom len v inej jazykovej mutácií, nedáva zmysel mať previazaných viac stránok v rovnakej jazykovej mutácií. Preto sa takéto previazanie vyhodnocujú ako **zlé mapovanie**.

### Nerovnomerné vnorenie

Nakoľko previazané by mali byť rovnaké štruktúry, rozdielne hĺbky rodičovských adresárov previazaných stránok indikujú chybu medzi štruktúrami. Oproti **zlému mapovaniu** nemusí ísť hneď o chybu, takéto previazania sú označené pre lepšie hľadanie prípadných chýb.

## Vymazanie/zrušenie previazania

Pri vymazaní/zrušení celého previazania zaniká existujúci synchronizačný parameter `syncId`, nakoľko už nemá čo previazať.

## Editácia previazania

Pri editácií previazania sa zobrazí každá previazaná stránka ako výberové pole stránok aj so skratkou jazyka ako štítkom.

Na nasledujúcom obrázku môžeme vidieť príklad **zlého mapovania**, kde sú previazané viaceré stránky v rovnakej jazykovej mutácií, konkrétne v prípade `sk` jazyka.

![](doc_editor_A.png)

### Zmena stránok

Pri editácií je možné previazané stránky zmeniť. V takomto prípade bude nahradenej stránke odstránený synchronizačný parameter `syncId` a novo-zvolenej stránke sa pridá parameter `syncId`.

Pre zvolené stránky nie je povolené:

- duplicitné zvolenie tej istej stránky
- zvolenie stránky s nastaveným `syncId` (samozrejme iným ako práve upravované). Ak stále budete trvať na previazaní danej stránky, najprv musíte zrušiť jej aktuálne previazanie (odstraníť `syncId`) a až následne ju môžete previazať s inými stránkami (pridať nové `syncId`).
- výber viacerých stránok v tej istej jazykovej mutácií (chyba [zlé mapovanie](./docs#zlé-mapovanie))
- výber stránok v rôznej hĺbke (chyba [nerovnomerné vnorenie](./docs#nerovnomerné-vnorenie))

Každá z týchto chýb sa kontroluje. Ak sa takáto chyba objaví pri pokuse o zmenu, akcia bude zablokovaná.

!>**Upozornenie:** editor poskytuje možnosť **Ignorovať problémy/upozornenia**. Zvolením tejto možnosti viete uložiť aj záznamy, ktoré obsahujú chyby **zlé mapovanie** a **nerovnomerné vnorenie**. Túto ochranu viete vypnúť na vlastnú zodpovednosť, ak to situácia vyžaduje.

### Pridanie stránky

Tabuľka neumožňuje vytvorenie úplne nového previazania (nového `syncId`) ale umožňuje pridanie (previazanie) nových stránok k už existujúcim. V editore pri editácií záznamu sa nachádza tlačidlo

<button id="add-sync-btn" class="btn btn-outline-secondary" onclick="showNewSelector(groupsMirroringTable)">
    <i class="ti ti-plus"></i>
    <span> Pripojiť priečinok </span>
</button>

pomocou ktorého viete pridať nové polia pre výber stránok. Keď zobrazíte maximálny povolený počet polí (povolených previazaní) tlačidlo sa skryje.

![](groups_editor_B.png)

### Odstránenie previazania

Akciou zmeny stránky viete prakticky odstrániť celé previazanie. Táto situácia nastane, keď odstránite všetky previazané stránky.