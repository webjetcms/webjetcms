# Používanie AI asistentov

Logika využívania AI asistentov je jednotná naprieč celým CMS, aby mal používateľ konzistentný zážitok bez ohľadu na kontext. V tejto kapitole nájdete prehľad správania asistenta v dátových tabuľkách, editore webových stránok, v nástroji PageBuilder, pri označení textu aj v editore obrázkov.

## Textové polia

Asistent pri textovom poli (jednoriadkové pole aj `textarea`) slúži na vygenerovanie nového obsahu alebo úpravu existujúceho. Spúšťa sa cez tlačidlo <button class="btn btn-outline-secondary btn-ai" type="button"><i class="ti ti-sparkles"></i></button> na konci poľa.

![](textarea-focus.png)

Po kliknutí sa otvorí okno so všetkými dostupnými asistentmi pre dané pole. Sú prehľadne rozdelení do skupín (napr. **Generovanie**, **Vylepšenie**). Pri každom je názov, ikona a poskytovateľ.

![](textarea-assistants.png)

Kliknutím na asistenta sa spustí jeho vykonanie. Ak má povolené postupné načítavanie, výsledok sa bude zobrazovať po častiach; inak sa zobrazí až po dokončení.

![](textarea-waiting.png)

Po úspešnom dokončení sa výsledný text automaticky vloží do poľa. V okne sa zobrazí potvrdenie a informácia o spotrebe tokenov. Ak výsledok nevyhovuje, pomocou tlačidla <button class="btn btn-outline-secondary btn-ai-undo" type="button"><i class="ti ti-arrow-back"></i> Zrušiť zmenu</button> vrátite pôvodnú hodnotu.

![](textarea-done.png)

### Vstup od používateľa

Ak asistent vyžaduje dodatočné pokyny, po jeho zvolení sa najprv zobrazí formulár na zadanie vstupu namiesto okamžitého spustenia.

Asistenti, ktorí potrebujú vstup používateľa, majú pri názve ikonu <i class="ti ti-blockquote has-user-prompt"></i> na jednoduché rozlíšenie.

![](textarea-prompt.png)

Pole môže mať zástupný text (`placeholder`) s návodom ako správne zadať požiadavku. Ak je pole prázdne a 2x do neho kliknete (alebo stlačíte klávesu `TAB`), tak sa tento zástupný text prenesie ako editovateľný text do poľa. Môžete tak ľahko upraviť potrebné časti a použiť ho ako základ vašej požiadavky.

Po zadaní pokynov stlačte **Generovať**. Ďalší priebeh (načítanie, vloženie výsledku, možnosť vrátenia) je rovnaký ako pri asistentovi bez používateľského vstupu.

![](textarea-prompt-loading.png)

### Quill

Nakoľko aj pole typu `Quill` patrí medzi textové polia, AI asistenti preň fungujú rovnako ako pre klasické textové polia. Jediný rozdiel je v umiestnení ikony, ktorá nie je na konci poľa, ale v hlavičke medzi nástrojmi.

![](quill.png)

## Obrázkové polia

Asistent pri obrázkovom poli slúži na vygenerovanie alebo úpravu obrázka. Spúšťa sa cez tlačidlo <button class="btn btn-outline-secondary btn-ai" type="button"><i class="ti ti-sparkles"></i></button> na konci poľa.

![](image-focus.png)

Po kliknutí sa otvorí okno so všetkými dostupnými asistentmi pre dané pole. Sú prehľadne rozdelení do skupín (napr. **Generovanie**, **Úprava**). Pri každom je názov, ikona a poskytovateľ.

![](image-assistants.png)

Kliknutím na asistenta sa spustí jeho vykonanie. Ak si vyberiete asistenta, ktorý má upraviť už existujúci obrázok (napr. **Odstrániť pozadie**), ale obrázkové pole nemá nastavený žiadny obrázok, vráti sa chybová hláška.

![](image-error-1.png)

Ak pole obsahuje obrázok, spustí sa spracovanie a zobrazí sa indikácia priebehu.

![](image-loading.png)

Po dokončení sa zobrazí potvrdenie, informácia o spotrebe tokenov a náhľad výsledku. Pomocou tlačidla <a target="_blank" class="zoom-in"><i class="ti ti-zoom-in"></i></a> si môžete obrázok otvoriť v plnej veľkosti na novej karte.

Obrázok sa neukladá automaticky. Najprv vyplňte alebo upravte polia **Názov obrázka** a **Umiestnenie**. Hodnoty sa vyplnia podľa pôvodného obrázka, ale môžete ich zmeniť. Pole **Umiestnenie** umožňuje výber priečinka cez stromovú štruktúru.

Na uloženie stlačte **Uložiť obrázok**.

![](image-done.png)

Ak ste názov ani umiestnenie nezmenili alebo kombinácia už existuje, zobrazí sa dialóg s možnosťami:

- **Prepísať súbor** – pôvodný súbor sa nahradí novým
- **Premenovať súbor** – systém ponúkne odporúčaný (AI generovaný) nový názov
- **Zrušiť uloženie** – návrat späť bez uloženia

Pri voľbe **Prepísať súbor** alebo **Premenovať súbor** sa obrázok v poli okamžite aktualizuje.

![](image-name-select.png)

### Vstup od používateľa

Ak asistent vyžaduje dodatočné pokyny, zobrazí sa formulár. Doplňujúce polia sa určujú podľa možností konkrétneho **poskytovateľa, modelu a operácie**, preto sa môžu meniť. Zobraziť sa môžu napríklad:

- **Počet obrázkov**,
- **Rozmer**,
- **Kvalita**,
- **Pomer strán**.

CMS zobrazí iba polia a hodnoty, ktoré vybraný model podporuje. Ak model pre danú operáciu nepublikuje žiadne doplnkové možnosti, formulár bude obsahovať iba používateľský pokyn.

Asistenti, ktorí potrebujú vstup používateľa, majú pri názve ikonu <i class="ti ti-blockquote has-user-prompt"></i>.

![](image-prompt.png)

Ak vybraný model podporuje pole **Počet obrázkov**, môže v jednej požiadavke vygenerovať viac výsledkov. Na príklade nižšie bolo nastavené číslo 3.

Vybrať a uložiť môžete len jeden výsledok – zvolíte ho kliknutím. Všetky náhľady je možné otvoriť cez <a target="_blank" class="zoom-in"><i class="ti ti-zoom-in"></i></a>.

![](image-select.png)

### Problémy s obsahom

Poskytovateľ môže odmietnuť požiadavku, ak pokyny obsahujú chránenú alebo citlivú entitu (známa osoba, postava, licencovaná značka a pod.). Konkrétne pravidlá sa líšia podľa **poskytovateľa**. V takom prípade sa môže vrátiť chybová hláška `PROHIBITED_CONTENT`.

![](image-error-2.png)

## Editor webových stránok

Asistenti fungujú aj v editore webových stránok a slúžia na vygenerovanie nového obsahu alebo úpravu existujúceho obsahu stránky. Spúšťajú sa cez ikonu <button class="btn-ai" type="button"><i class="ti ti-sparkles"></i></button> umiestnenú v paneli nástrojov webového editora.

![](ckeditor.png)

Po kliknutí sa otvorí okno so všetkými dostupnými asistentmi pre dané pole. Sú prehľadne rozdelení do skupín (napr. **Generovanie**, **Copywriting**, **Vylepšenie**). Pri každom je názov, ikona a poskytovateľ.

![](ckeditor-assistants.png)

Kliknutím na asistenta sa spustí jeho vykonanie. Ak má povolené postupné načítavanie, výsledok sa bude zobrazovať po častiach; inak sa zobrazí až po dokončení. Ak sa bude obsah načítavať po častiach, môže to vzbudiť dojem, že štruktúra stránky sa pokazila, ale všetko sa vráti do normálu, keď sa načíta celý obsah.

![](ckeditor-loading.png)

Po úspešnom dokončení sa výsledný obsah automaticky vloží do stránky. V okne sa zobrazí potvrdenie a informácia o spotrebe tokenov. Ak výsledok nevyhovuje, pomocou tlačidla <button class="btn btn-outline-secondary btn-ai-undo" type="button"><i class="ti ti-arrow-back"></i> Zrušiť zmenu</button> vrátite pôvodnú hodnotu.

V tomto prípade je asistent nastavený tak (a to aj odporúčame pri úprave obsahu stránky), aby v okne ešte opísal všetky zmeny, ktoré vykonal na obsahu stránky, čím ponúka spätnú väzbu, čo bolo potrebné upraviť.

![](ckeditor-done.png)

### Iba časť textu

Nemusíte upravovať celý obsah stránky. Asistent pracujúci s `CKEditor` podporuje aj úpravy iba vybranej časti. Ak označíte iba časť obsahu a následne zavoláte asistenta, upraví iba danú vybranú časť.

![](ckeditor-text-selection.png)

## PageBuilder

Asistenti fungujú aj v `PageBuilder` a slúžia na vygenerovanie nového obsahu alebo úpravu existujúceho obsahu stránky vloženého v `PageBuilder`. Spúšťajú sa cez ikonu <button class="btn-ai" type="button"><i class="ti ti-sparkles"></i></button> umiestnenú v paneli nástrojov.

![](page_builder.png)

Po kliknutí sa otvorí okno so všetkými dostupnými asistentmi pre dané pole. Sú prehľadne rozdelení do skupín (napr. **Generovanie**, **Copywriting**, **Vylepšenie**). Pri každom je názov, ikona a poskytovateľ.

![](page_builder-assistants.png)

Kliknutím na asistenta sa spustí jeho vykonanie. Každý blok sa považuje za samostatný text/obsah a spracováva sa samostatne. Okno bude postupne aktualizovať stav, koľko blokov z celkového počtu už bolo spracovaných.

![](page_builder-loading.png)

Po úspešnom dokončení sa výsledný obsah automaticky vloží do stránky. V okne sa zobrazí potvrdenie a informácia o spotrebe tokenov. Ak výsledok nevyhovuje, pomocou tlačidla <button class="btn btn-outline-secondary btn-ai-undo" type="button"><i class="ti ti-arrow-back"></i> Zrušiť zmenu</button> vrátite pôvodnú hodnotu.

V tomto prípade je asistent nastavený tak (a to aj odporúčame pri úprave obsahu stránky), aby v okne ešte opísal všetky zmeny, ktoré vykonal na obsahu stránky, čím ponúka spätnú väzbu, čo bolo potrebné upraviť.

![](page_builder-done.png)

### Iba časť textu

Nemusíte upravovať celý obsah stránky. Asistent pracujúci s `PageBuilder` podporuje aj úpravy iba vybranej časti. Ak označíte iba časť obsahu a následne zavoláte asistenta, upraví iba danú vybranú časť.

![](page_builder-selection.png)

## Režim chat

Asistent s typom požiadavky **Chat** otvorí rozhovor, v ktorom môžete zadanie postupne spresňovať. Dostupný je pri textových poliach, v editore stránky aj v PageBuilderi podľa nastavenia asistenta. Odpoveď sa zobrazí v chate a obsah editora zmeníte až vlastným potvrdením.

Pod dokončenou odpoveďou nájdete ikony **Pridať**, **Nahradiť obsah** a **Kopírovať**. Pri označenom texte sa namiesto nahradenia celého obsahu ponúkne **Nahradiť označený text**. Názov tlačidla sa zobrazí v bubline po prejdení myšou alebo zameraní klávesnicou. Po vložení môžete použiť **Zrušiť vloženie**, pokiaľ ste obsah medzitým ďalej neupravili.

![Chat pri textovom poli s tlačidlami na vloženie odpovede](../chat/chat-editor.png)

V PageBuilderi môže asistent pripraviť aj HTML bloky. Vložíte ich rovnakými tlačidlami pod odpoveďou a ďalšie úpravy zadáte novou správou. Zatvorenie okna AI zachová rozhovor, zatvorenie samotného editora ho ukončí. Chat pri poli sa neukladá do histórie rýchleho vyhľadávania.

Podrobnosti práce s výberom, kontextom a ukladaním sú v kapitole [Chat pri poli a v editore stránky](../chat/README.md#chat-pri-poli-a-v-editore-stránky). Na samostatný rozhovor s prílohami použite kartu [AI Chat v rýchlom vyhľadávaní](../chat/README.md#začatie-rozhovoru).

## Obrázky v stránke

AI asistenta môžete využiť aj pri vkladaní obrázkov do webovej stránky. Tlačidlo asistenta sa nachádza v pravom dolnom rohu na konci poľa s adresou obrázka.

![](page-image.png)

Po kliknutí sa otvorí okno so všetkými dostupnými asistentmi, s ktorými sa pracuje rovnako, ako je to opísané v časti [Obrázkové polia](./README.md#obrázkové-polia)

![](page-image-assistants.png)

## Prázdne pole

Asistenti používajú často ako vstupnú hodnotu obsah daného poľa, ak je pole prázdne, nezobrazia sa. Napríklad Preložiť do angličtiny nemá zmysel zobraziť, ak je pole prázdne. Môže nastať situácia, že pre prázdne pole nie je dostupný žiaden asistent, vtedy sa zobrazí chybová správa. Do poľa je potrebné najskôr zadať text, až potom zobraziť asistentov.

Nie vždy sa ale používa rovnaké pole pre vstupnú hodnotu. Napríklad v editácii webovej stránky je asistent na generovanie Názvu webovej stránky z jej textu. Ak text web stránky je zatiaľ prázdny nemusí sa táto možnosť zobraziť.

![](no-assistants-available.png)
