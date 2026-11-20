# Publikačné balíky (POC)

Publikačný balík spojí prípravu viacerých stránok, priečinkov, médií a súborov. Tím môže upravovať kampaň a preklikávať jej náhľad, kým návštevníci vidia pôvodný web. Funkciu musí najprv zapnúť správca na overenom prostredí. Predvolene je vypnutá.

## Príprava kampane

1. V časti **Webové stránky → Pracovné verzie** vyberte doménu v ľavom menu. Tabuľka zobrazuje jej pracovné verzie vrátane publikovaných, ich farbu, stav a poslednú úpravu. Termín publikovania a ďalšie údaje si môžete zapnúť vo výbere stĺpcov. Zoznam môžete filtrovať a zoradiť.
2. Tlačidlom **Pridať** vytvorte novú kampaň. Zadajte názov, poznámku a farbu. Ukážka zobrazí jej označenie v hlavičke. Keď chcete začať upravovať obsah, označte rozpracovanú verziu a použite akciu **Prepnúť na úpravy webu**. Nad hlavičkou sa zobrazí pás s jej názvom. Z neho môžete prepnúť verziu alebo sa odkazom **Spravovať pracovné verzie** vrátiť do tabuľky.
3. Stránky a priečinky upravujte obvyklým editorom. Uložená zmena sa pridá do balíka. Nové položky dostanú konečné ID, ktoré sa pri publikovaní nemení. Kolegovia vyberú ten istý balík vo vlastnom prihlásení. Potrebujú svoje bežné práva na dotknutý obsah.
4. Pridajte médiá a obrázky/prílohy mimo galérie. Možno vytvoriť prázdny súborový adresár a pracovať s jednotlivými súbormi. Súbor spoločný viacerým doménam po publikovaní ovplyvní všetky jeho použitia.
5. Pred zmenou verzie alebo domény uložte rozpracovaný formulár. Ak kolega či druhá záložka zmení kontext alebo obsah, staré uloženie server odmietne. Zachovajte si text a otvorte čerstvý editor.

Zvolenú pracovnú verziu ukazuje prepínač v hlavičke administrácie. Editor zobrazí upozornenie, ak konkrétnu stránku nemožno upravovať, napríklad pri zdieľanej System stránke alebo multigroup kópii. Individuálne plány publikovania, história, atribúty a multigroup väzby sa v pracovnej verzii nemenia; spravujú sa mimo balíka.

Pri vytvorení priečinka WebJET pridá do pracovnej verzie aj jeho hlavnú stránku s názvom priečinka, ak je zapnuté automatické vytváranie stránok a máte právo pridávať stránky.

Obrázky môžete vyberať priamo cez dialóg CKEditora. Zobrazia sa z pracovnej verzie, pričom v poli **URL adresa** aj v HTML zostáva bežná cesta `/images/...`. Nový obrázok pridaný iba do balíka bude na verejnej adrese dostupný až po publikovaní.

## Náhľad a konflikt

Tlačidlo **Náhľad** v pätičke editora zobrazí aj neuložené úpravy stránky bez ich uloženia do balíka. Funguje aj bez zmeny formulára. Obrázky a odkazy sa načítajú v kontexte zvolenej pracovnej verzie.

V páse pracovnej verzie kliknite na **Náhľad**. V novej karte môžete prejsť menu, Novinky, detaily, obrázky a prílohy. Náhľad vyžaduje prihlásenie do administrácie. Lišta uvádza logickú doménu a čas použitý Novinkami.

Odkaz **Pracovné zmeny** v hlavičke otvorí priamo kartu **Zmeny** aktívnej pracovnej verzie. V zozname pracovných verzií môžete označiť ľubovoľný riadok a použiť rovnakú akciu. Širší dialóg poskytuje viac miesta pre zoznam zmien. Stĺpec **Akcia** je hneď za stĺpcom **Typ**, aby boli tlačidlá dostupné aj na užšej obrazovke. Zoznam zúžite filtrom v stĺpci **Typ** alebo prepínačom **Len problémy** v lište. Po úpravách v inej karte prehliadača použite obnovenie tabuľky.

Akciou **Porovnať** otvoríte hlavnú a pracovnú verziu položky. Ak sa hlavná verzia líši od východiskového stavu, zobrazí sa aj tretí stĺpec **Východiskový stav**, a to aj po vyriešení konfliktu. Pri zhode sú viditeľné iba dva širšie stĺpce; nová položka vytvorená v pracovnej verzii zobrazuje iba pracovnú verziu. Na karte **Polia** má každý údaj samostatný riadok. Vľavo je názov poľa, vedľa neho hodnoty porovnávaných verzií. Na mobile sú verzie pod sebou a názov poľa je pri hodnote. Pri webových stránkach sú dostupné aj karty **HTML kód** s očíslovanými riadkami zdroja a rozbaliteľným textovým náhľadom a **Obsah stránky** s vykreslenou stránkou vrátane šablóny. Pri priečinkoch, médiách a súboroch sa tieto dve karty nezobrazujú. Prepínač **Zobraziť aj nezmenené polia** rozšíri porovnanie o ostatné údaje. Ikonou oka v hlavičke verzie otvoríte príslušnú stránku. Ikonou ceruzky pri pracovnej verzii ju môžete upraviť. Akcie sú dostupné na kartách porovnania aj pri zobrazení verzií pod sebou na mobile. Ikonou v hlavičke môžete okno maximalizovať a vrátiť na pôvodnú veľkosť.

**Východiskový stav** zachytáva hlavnú verziu položky pri prvom uložení jej úpravy v pracovnej verzii. Pri ďalších úpravách sa nemení. **Hlavná verzia** zobrazuje aktuálny stav, ktorý sa mohol medzitým zmeniť. Tento rozdiel vysvetľuje aj tooltip pri nadpise **Východiskový stav**, dostupný myšou aj klávesnicou.

Na karte **Obsah stránky** je vľavo hlavná a vpravo pracovná verzia. Posúvanie stránok je synchronizované v oboch smeroch. Prepínač **Zvýrazniť rozdiely** označí pridaný a odstránený text v pravej strane farbami známymi z porovnania v histórii stránok. Zmeny HTML značiek a ich atribútov skontrolujete na karte **HTML kód**. Pri novej stránke sa zobrazí iba pracovná verzia; pri odstránenej verzii informácia o jej stave. Obsah sa načíta až po otvorení karty. Porovnanie zobrazuje skutočný pracovný obsah aj vtedy, keď je na publikovanie vybraná hlavná verzia. Náhľady slúžia na čítanie; skripty a odosielanie formulárov sú vypnuté.

Zvýrazňovanie a skrývanie nezmenených polí vychádza z porovnania **Hlavnej verzie** s **Pracovnou verziou**. Rovnaké hodnoty sa nezvýrazňujú, aj keď sa líšia od východiskového stavu. Ten slúži iba ako kontext. Nevyplnená hodnota, prázdny text, text obsahujúci iba medzery a prázdny zoznam sa považujú za rovnakú prázdnu hodnotu a zobrazujú sa ako **—**. Hodnoty **0** a **false** sa od prázdnej hodnoty odlišujú. Pravidlá platia na kartách **Polia** aj **HTML kód**; prepínač **Zobraziť aj nezmenené polia** sprístupní aj zhodné údaje bez zvýraznenia.

Tlačidlom **Upraviť** v bloku **Pracovná verzia** otvoríte pracovný obsah v editore. Po uložení sa vráťte do porovnania a znova skontrolujte zmeny. Uloženie obsahu zruší predchádzajúcu voľbu výslednej verzie. Pokyny k úprave sú dostupné aj v tooltipe tlačidla; vysvetlenie voľby verzie nájdete pri **Uložiť riešenie**.

Pri konflikte kliknite na **Vyriešiť**. V pätičke porovnania vyberte **Použiť hlavnú verziu** alebo **Použiť pracovnú verziu** a kliknite na **Uložiť riešenie**. Po úspešnom uložení sa porovnanie zatvorí a zoznam zmien sa aktualizuje. Pri chybe zostane porovnanie otvorené s hlásením a zvolenou verziou. Voľba platí pre celú položku a zostáva zachovaná pri prepínaní kariet. Pätička je dostupná aj pri posúvaní dlhého obsahu; tlačidlom **Zatvoriť** sa vrátite do zoznamu zmien.

Ak chcete odstrániť pracovnú zmenu stránky alebo priečinka, označte položku a použite **Vrátiť zmenu** v lište tabuľky. Pri zmazanom priečinku sa obnovia aj položky zmazané spolu s ním.

Voľba výsledku hneď ovplyvní náhľad vrátane menu, Noviniek, médií a príloh. Editor ďalej uchováva pracovný obsah. Pri nevyriešenom konflikte lišta upozorní na pracovný predpoklad. Neskoršia zmena produkcie alebo WORK zneplatní starú voľbu a vyžiada nové porovnanie.

## Publikovanie a termín

Po kontrole zmien a náhľadu použite **Publikovať do hlavnej verzie** v pätičke editora alebo v lište označeného riadka. Potvrdením odošlete zmeny na zverejnenie. Tlačidlo **Uložiť** na karte **Základné** ukladá názov, farbu a poznámku pracovnej verzie. Požiadavku na publikovanie spracuje najbližší beh úlohy na pozadí, predvolene do piatich minút. Ak pred ňou beží iný balík, počká na jeho dokončenie. Publikujúci musí mať práva na priamu publikáciu celého balíka. Balík neobchádza schvaľovanie a nemení existujúce individuálne plány.

Na karte **Plán publikovania** zadajte dátum a čas v zobrazenom časovom pásme. Kliknite na **Naplánovať publikovanie**. **Zmeniť termín** posunie celý balík. Ak potrebujete znova upravovať obsah, použite **Zrušiť plánovanie**. Pracovné zmeny aj uložené riešenia konfliktov zostanú k dispozícii. Náhľad Noviniek používa budúci termín, po zrušení aktuálny čas. Menu nemá nový časový filter.

Spoločná úloha kontroluje termíny predvolene každých päť minút aj po odhlásení redaktora. Presný interval nastavuje správca. Znovu preverí používateľa, práva, konflikty a individuálne plány. Nový problém zastaví balík a prehľad uvedie dôvod. Po oprave použite **Obnoviť prípravu** a nové porovnanie.

Publikovanie prebieha po položkách. Pri chybe už dokončené položky môžu byť verejné. Skontrolujte priebeh a dôvod; po odstránení príčiny overte **Pokračovať**. Balík ani jeho hotové položky neprepisujte ručne bez posúdenia správcu. Úspešný balík je uzavretý; pre ďalšiu kampaň vytvorte nový. Na návrat do bežnej editácie vyberte v prepínači **Hlavná verzia**.

## Zmazanie pracovnej verzie

V tabuľke **Pracovné verzie** označte jednu alebo viac verzií a kliknite na ikonu **Zmazať**. Po potvrdení sa trvalo odstránia aj ich pracovné stránky, priečinky, médiá, súbory a náhľadové obrázky. Už publikovaný obsah hlavnej verzie zostáva zachovaný.

Zmazať môžete verziu v ktoromkoľvek stave, vrátane naplánovanej a publikovanej, bez čakania na bežiace publikovanie. Po zmazaní aktívnej verzie sa administrácia prepne na **Hlavnú verziu**.

Export a import nie sú dostupné, pretože samotné metadáta verzie neobsahujú jej pracovný obsah a súbory.

## Čo v POC pripraviť mimo balíka

Vo vybranej pracovnej verzii môžete otvoriť každú časť administrácie, na ktorú máte oprávnenie. V tabuľke sú adresy, ktoré podporujú prácu s balíkom.

| Obrazovka | URL |
| --- | --- |
| Prehľad administrácie a výber balíka | `/admin/v9/` |
| Správa pracovných verzií | `/admin/v9/webpages/branches/` |
| Webové stránky a ich editor | `/admin/v9/webpages/web-pages-list/` |
| Médiá | `/admin/v9/webpages/media/` |
| Editor komponentu stránky | `/admin/v9/webpages/component/` |
| Novinky | `/apps/news/admin/` |
| Súbory a výber súboru, odkazu alebo obrázka | `/admin/v9/files/index/`, `/admin/v9/files/dialog/`, `/admin/v9/files/wj_link/`, `/admin/v9/files/wj_image/` |

Pri otvorení inej obrazovky, napríklad šablón stránok alebo konfigurácie, sa zobrazí dialóg s názvom pracovnej verzie. Tlačidlom **Pokračovať v hlavnej verzii** otvoríte obrazovku s údajmi hlavnej verzie. Uložené zmeny sa v nej prejavia bez publikovania balíka. Po návrate na Webové stránky pokračujete v pôvodnom balíku.

Definície kategórií, atribúty, multigroup väzby, galériu a jej varianty, archív súborov, nastavenia adresárov, šablóny, CSS/JS a zdroje aplikácií pripravuje správca mimo pracovnej verzie. Spoločné System stránky sú iba na čítanie. Jazykové stránky tím pripraví explicitne; balík nevytvára skryté preklady ani zrkadlené kópie. Zmena URL nevytvorí automatické presmerovanie.

Overený rozsah šablón/aplikácií, konfigurácia a obnova sú v [návode pre správcu](../../admin/setup/publishing-packages.md). Pred použitím na ďalšom webe treba overiť jeho vlastné komponenty.
