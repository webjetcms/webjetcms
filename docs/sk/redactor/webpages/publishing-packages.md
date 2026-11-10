# Publikačné balíky (POC)

Publikačný balík spojí prípravu viacerých stránok, priečinkov, médií a súborov. Tím môže upravovať kampaň a preklikávať jej náhľad, kým návštevníci vidia pôvodný web. Funkciu musí najprv zapnúť správca na overenom prostredí. Predvolene je vypnutá.

## Príprava kampane

1. V **Webové stránky** vyberte doménu. Pod výberom domény otvorte prepínač **Pracovná verzia** a zvoľte **+ Nová pracovná verzia**. Zadajte názov a opis kampane.
2. Vytvorenie balíka samo neprepne editor. Kliknite **Upravovať web**. Pred každou úpravou skontrolujte aktívny názov verzie; voľba **Produkcia** upravuje verejný obsah.
3. Stránky a priečinky upravujte obvyklým editorom. Uložená zmena sa pridá do balíka. Nové položky dostanú konečné ID, ktoré sa pri publikovaní nemení. Kolegovia vyberú ten istý balík vo vlastnom prihlásení. Potrebujú svoje bežné práva na dotknutý obsah.
4. Pridajte médiá a obrázky/prílohy mimo galérie. Možno vytvoriť prázdny súborový adresár a pracovať s jednotlivými súbormi. Súbor spoločný viacerým doménam po publikovaní ovplyvní všetky jeho použitia.
5. Pred zmenou verzie alebo domény uložte rozpracovaný formulár. Ak kolega či druhá záložka zmení kontext alebo obsah, staré uloženie server odmietne. Zachovajte si text a otvorte čerstvý editor.

## Náhľad a konflikt

Otvorte **Náhľad** a prejdite skutočné menu, Novinky, detaily, obrázky a prílohy. Náhľad zostáva na hostiteľovi administrácie a vyžaduje prihlásenie; odkaz nie je určený verejnosti. Lišta uvádza logickú doménu a čas použitý Novinkami.

V prehľade **Zmeny / Skontrolovať zmeny** vidíte pôvodný základ, súčasnú produkciu a pracovnú verziu. Ak niekto zmenil tú istú položku na oboch stranách, vyberte celú **Produkciu** alebo **Pracovnú verziu** a uložte voľbu. Automatické skladanie jednotlivých polí sa nevykonáva. Vlastnú kombináciu pripravte v bežnom pracovnom editore a znovu porovnajte.

Voľba výsledku hneď ovplyvní náhľad vrátane menu, Noviniek, médií a príloh. Editor ďalej uchováva pracovný obsah. Pri nevyriešenom konflikte lišta upozorní na pracovný predpoklad. Neskoršia zmena produkcie alebo WORK zneplatní starú voľbu a vyžiada nové porovnanie.

## Publikovanie a termín

Po kontrole zoznamu a náhľadu kliknite **Publikovať** a potvrďte konkrétne zmeny. Publikujúci musí mať práva na priamu publikáciu celého balíka. Balík neobchádza schvaľovanie a nemení existujúce individuálne plány.

Pre spoločný termín použite **Naplánovať publikovanie**. Skontrolujte dátum, čas a zobrazené časové pásmo. Naplánovaný balík sa neupravuje. **Zmeniť termín** posunie celý balík jednou operáciou; **Zrušiť plánovanie** zachová obsah aj rozhodnutia a vráti prípravu. Náhľad Noviniek používa budúci termín, po zrušení aktuálny čas. Menu nemá nový časový filter.

Cron kontroluje termíny každú minútu aj po odhlásení redaktora. Znovu preverí používateľa, práva, konflikty a individuálne plány. Nový problém zastaví balík a prehľad uvedie dôvod. Po oprave použite **Obnoviť prípravu** a nové porovnanie.

Publikovanie prebieha po položkách. Pri chybe už dokončené položky môžu byť verejné. Skontrolujte priebeh a dôvod; po odstránení príčiny overte **Pokračovať**. Balík ani jeho hotové položky neprepisujte ručne bez posúdenia správcu. Úspešný balík je uzavretý; pre ďalšiu kampaň vytvorte nový. Na návrat do bežnej editácie výslovne vyberte **Produkcia**.

## Čo v POC pripraviť mimo balíka

Definície kategórií, atribúty, multigroup väzby, galériu a jej varianty, archív súborov, nastavenia adresárov, šablóny, CSS/JS a zdroje aplikácií pripravuje správca mimo pracovnej verzie. Spoločné System stránky sú iba na čítanie. Jazykové stránky tím pripraví explicitne; balík nevytvára skryté preklady ani zrkadlené kópie. Zmena URL nevytvorí automatické presmerovanie.

Overený rozsah šablón/aplikácií, konfigurácia a obnova sú v [návode pre správcu](../../admin/setup/publishing-packages.md). Pred použitím na ďalšom webe treba overiť jeho vlastné komponenty.
