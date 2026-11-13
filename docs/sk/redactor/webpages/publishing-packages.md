# Publikačné balíky (POC)

Publikačný balík spojí prípravu viacerých stránok, priečinkov, médií a súborov. Tím môže upravovať kampaň a preklikávať jej náhľad, kým návštevníci vidia pôvodný web. Funkciu musí najprv zapnúť správca na overenom prostredí. Predvolene je vypnutá.

## Príprava kampane

1. V časti **Webové stránky → Pracovné verzie** vyberte doménu v ľavom menu. Tabuľka zobrazuje jej pracovné verzie vrátane publikovaných, ich farbu, stav a poslednú úpravu. Termín publikovania a ďalšie údaje si môžete zapnúť vo výbere stĺpcov. Zoznam môžete filtrovať a zoradiť.
2. Tlačidlom **Pridať** vytvorte novú kampaň. Zadajte názov, poznámku a farbu; ukážka zobrazí jej označenie v hlavičke. Uloženie vytvorí verziu bez prepnutia obsahu. Názov, poznámku a farbu môžete neskôr upraviť kliknutím na názov riadka. Označte rozpracovanú verziu a kliknite **Upravovať web**. Nad hlavičkou sa zobrazí pás s jej názvom. Prepínač v hlavičke slúži na výber verzie; odkaz **Spravovať pracovné verzie** vedie späť do tabuľky.
3. Stránky a priečinky upravujte obvyklým editorom. Uložená zmena sa pridá do balíka. Nové položky dostanú konečné ID, ktoré sa pri publikovaní nemení. Kolegovia vyberú ten istý balík vo vlastnom prihlásení. Potrebujú svoje bežné práva na dotknutý obsah.
4. Pridajte médiá a obrázky/prílohy mimo galérie. Možno vytvoriť prázdny súborový adresár a pracovať s jednotlivými súbormi. Súbor spoločný viacerým doménam po publikovaní ovplyvní všetky jeho použitia.
5. Pred zmenou verzie alebo domény uložte rozpracovaný formulár. Ak kolega či druhá záložka zmení kontext alebo obsah, staré uloženie server odmietne. Zachovajte si text a otvorte čerstvý editor.

## Náhľad a konflikt

V páse pracovnej verzie kliknite na **Náhľad**. V novej karte môžete prejsť menu, Novinky, detaily, obrázky a prílohy. Náhľad vyžaduje prihlásenie do administrácie. Lišta uvádza logickú doménu a čas použitý Novinkami.

V tabuľke pracovných verzií označte riadok a kliknite **Skontrolovať zmeny**. Porovnanie je dostupné aj tlačidlom **Pracovné zmeny** v páse pracovnej verzie. Otvoríte porovnanie pôvodného základu, súčasnej hlavnej verzie a pracovnej verzie. Ak niekto zmenil tú istú položku na oboch stranách, vyberte verziu položky, ktorú chcete ponechať, a uložte voľbu. Vlastnú kombináciu polí pripravte v pracovnom editore a znovu porovnajte.

Voľba výsledku hneď ovplyvní náhľad vrátane menu, Noviniek, médií a príloh. Editor ďalej uchováva pracovný obsah. Pri nevyriešenom konflikte lišta upozorní na pracovný predpoklad. Neskoršia zmena produkcie alebo WORK zneplatní starú voľbu a vyžiada nové porovnanie.

## Publikovanie a termín

Po kontrole zoznamu a náhľadu kliknite **Publikovať** a potvrďte konkrétne zmeny. Požiadavku spracuje najbližší beh úlohy na pozadí, predvolene do piatich minút. Ak pred ňou beží iný balík, počká na jeho dokončenie. Publikujúci musí mať práva na priamu publikáciu celého balíka. Balík neobchádza schvaľovanie a nemení existujúce individuálne plány.

Pre spoločný termín použite **Naplánovať publikovanie**. Skontrolujte dátum, čas a zobrazené časové pásmo. Naplánovaný balík sa neupravuje. **Zmeniť termín** posunie celý balík jednou operáciou; **Zrušiť plánovanie** zachová obsah aj rozhodnutia a vráti prípravu. Náhľad Noviniek používa budúci termín, po zrušení aktuálny čas. Menu nemá nový časový filter.

Spoločná úloha kontroluje termíny predvolene každých päť minút aj po odhlásení redaktora. Presný interval nastavuje správca. Znovu preverí používateľa, práva, konflikty a individuálne plány. Nový problém zastaví balík a prehľad uvedie dôvod. Po oprave použite **Obnoviť prípravu** a nové porovnanie.

Publikovanie prebieha po položkách. Pri chybe už dokončené položky môžu byť verejné. Skontrolujte priebeh a dôvod; po odstránení príčiny overte **Pokračovať**. Balík ani jeho hotové položky neprepisujte ručne bez posúdenia správcu. Úspešný balík je uzavretý; pre ďalšiu kampaň vytvorte nový. Na návrat do bežnej editácie vyberte v prepínači **Hlavná verzia**.

## Zmazanie pracovnej verzie

V tabuľke **Pracovné verzie** označte jednu alebo viac verzií a kliknite na ikonu **Zmazať**. Po potvrdení sa trvalo odstránia aj ich pracovné stránky, priečinky, médiá, súbory a náhľadové obrázky. Už publikovaný obsah hlavnej verzie zostáva zachovaný.

Zmazať môžete verziu v ktoromkoľvek stave, vrátane naplánovanej a publikovanej, bez čakania na bežiace publikovanie. Po zmazaní aktívnej verzie sa administrácia prepne na **Hlavnú verziu**.

Akcie **Skontrolovať zmeny** a **Upravovať web** majú v lište ikonové tlačidlá s tooltipmi. Export a import nie sú dostupné, pretože samotné metadáta verzie neobsahujú jej pracovný obsah a súbory.

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
