# Číselníky

Aplikácia Číselníky umožňuje vytvárať, upravovať, mazať a duplikovať pomenované typy číselníkov a ukladať do nich dáta. Typy číselníkov aj ich dáta je tiež možné exportovať a importovať pomocou súboru.

Typy číselníkov sa vyberajú v stromovom zozname v ľavom paneli. Pravý panel zobrazuje datatabuľku s dátami vybraného typu číselníka.

![](dataTable_enumType.png)

## Typy číselníkov

Samostatnú kartu **Typy číselníkov** nahrádza stromová štruktúra v ľavej časti. Tlačidlami nad stromom môžete typ vytvoriť, upraviť, duplikovať, zmazať, importovať alebo exportovať. Pri úprave sa otvorí rovnaký editor s nastaveniami polí a prepojení. Vyhľadávanie pod tlačidlami filtruje názvy typov.

Tlačidlom <button class="btn btn-sm btn-outline-secondary" type="button"><span><i class="ti ti-adjustments-horizontal"></i></span></button> nad stromom môžete zmeniť pomer šírky stromu a tabuľky alebo zobraziť **vymazané typy**. Nastavenie sa uloží pre prihláseného používateľa samostatne pre túto aplikáciu.

!> **Vymazané typy** sa štandardne v strome nezobrazujú. V nastaveniach stromu môžete zapnúť voľbu **Zobraziť vymazané typy**; zobrazia sa červenou farbou s ikonou koša a môžete ich vybrať a upraviť. Pri výbere vymazaného typu sú akcie zmazania a duplikovania blokované, kým typ neobnovíte.

Strom zohľadňuje pole **Podradený typ číselníka** v nastavení typu: ak typ **A** odkazuje na typ **B**, typ **B** sa zobrazí pod typom **A**. Typ prepojený s viacerými rodičmi sa zobrazí pod každým z nich; všetky jeho výskyty otvárajú rovnaké dáta a nastavenia. Vyhľadávanie ponechá viditeľnú aj cestu cez nadradené typy a pri obnovení výberu sa táto cesta rozbalí. Prepojenia jednotlivých dátových záznamov hierarchiu typov nemenia.

Výber typu sa ukladá do adresy stránky, takže odkaz môžete uložiť alebo zdieľať. Ak vybraný typ už neexistuje, zobrazí sa prvý dostupný typ. Ak nie je dostupný žiadny typ, vytvorte ho tlačidlom **+** nad stromom; pridávanie dát je dovtedy vypnuté.

Pri vytváraní nového typu číselníka musíte zadať jedinečný názov. Ostatné polia sú voliteľné. Karty **Reťazce**, **Čísla**, **Boolovské** a **Dátumy** obsahujú niekoľko očíslovaných polí, ktorými určíte štruktúru dát daného číselníka. Ak poľu zadáte názov, v dátach číselníka sa vytvorí pole so zadaným názvom a dátovým typom zodpovedajúcim danej karte.

![](editor_enumType.png)

Príklad: ak vyplníte dve polia na karte **Reťazce**

![](editor_stringTab.png)

a jedno pole na karte **Boolovské**,

![](editor_booleanTab.png)

v tabuľke dát daného číselníka sa zobrazia dva stĺpce typu reťazec a jeden stĺpec typu boolean so zadanými názvami. V editore sa zobrazia zodpovedajúce polia (pozri obrázky v sekcii [Zoznam dát číselníkov](#zoznam-dát-číselníkov)).

Štruktúru dát si tak môžete definovať pre každý číselník zvlášť. Môžete kombinovať textové, číselné, boolovské a dátumové polia. Počet polí každého dátového typu je obmedzený počtom polí na príslušnej karte.

### Karta Typy reťazcových polí

Po prvom uložení typu číselníka sa zobrazí karta **Typy reťazcových polí**. Umožňuje rozšíriť pomenované polia z karty **Reťazce** o nastavenia známe z [voliteľných polí](../../../frontend/webpages/customfields/custom-fields-settings.md), napríklad o výberové pole, výber viacerých možností, automatické dopĺňanie, prepojenie na iný číselník alebo výber obrázka, odkazu, priečinka či webovej stránky.

![](editor_stringFieldTypes.png)

V tabuľke sa zobrazujú iba reťazcové polia, pre ktoré je vytvorená konfigurácia. Pri jej pridaní sú v poli **Voliteľné pole** dostupné iba pomenované reťazcové polia vo formáte **Reťazec N – názov**. Ponuka polí, ich názvy a popisy vychádzajú z poslednej uloženej verzie typu číselníka. Po pomenovaní alebo premenovaní reťazcového poľa preto najskôr uložte typ číselníka; konfigurácia sa následne automaticky aktualizuje. Ak názov poľa odstránite, pole sa skryje a jeho nastavenie povinnosti sa zruší.

Pre každé pole je možné nastaviť:

- typ poľa a vlastnosti špecifické pre daný typ, napríklad možnosti výberového poľa,
- povinnosť vyplnenia,
- pomocný text zobrazený ako `tooltip`.

![](editor_stringFieldType.png)

Bez špecifickej konfigurácie sa pomenované reťazcové pole zobrazí ako bežné textové pole s maximálnou dĺžkou 1024 znakov. Nepomenované reťazcové polia sa v dátach číselníka ani v možnostiach konfigurácie nezobrazia.

!> **Upozornenie:** číselníky zatiaľ nie sú rozdelené podľa zvolenej domény, preto sa nastavenia voliteľných polí (typy reťazcových polí) vždy ukladajú do hlavnej domény. V sekcii [Voliteľné polia](../../../frontend/webpages/customfields/custom-fields-settings.md) sa tieto nastavenia zobrazia len v hlavnej doméne. Odporúčame ich nastavovať a upravovať vždy na karte **Typy reťazcových polí**.

!> **Upozornenie na spätnú kompatibilitu:** dátové atribúty reťazcových polí sa zmenili z `string1` až `string12` na `fieldA` až `fieldL`. Vo vlastných alebo starších šablónach programu Excel na import dát číselníka musíte kódové názvy v hlavičke ručne upraviť, napríklad `Mesto|string1` na `Mesto|fieldA`. Rovnaké názvy `fieldA` až `fieldL` používajte aj v integráciách REST API, ktoré spracúvajú dáta číselníkov. Databázové stĺpce `string1` až `string12` zostávajú nezmenené.

### Karta Základné

Na karte **Základné** sa nastavujú tieto vlastnosti:

- Názov typu - jedinečný názov pre typ číselníka, nesmie byť prázdny.
- Vymazaný - označuje vyradený typ. Vypnutím tejto voľby a uložením obnovíte typ aj všetky jeho dátové záznamy.
- Podradený typ číselníka - vybraný typ sa zobrazí v strome pod aktuálnym typom. Toto nastavenie automaticky neprepája ich dátové záznamy.
- Povoliť prepojenie dátových záznamov na číselník - v editore jednotlivých záznamov sprístupní výber prepojeného číselníka. Nemení nastavenie podradeného typu.
- Povoliť výber rodičovského záznamu - jednotlivým záznamom umožní vybrať rodiča spomedzi ostatných záznamov toho istého číselníka.

Voľby **Povoliť prepojenie dátových záznamov na číselník** a **Povoliť výber rodičovského záznamu** nemožno povoliť súčasne. Nastavenie podradeného typu je od nich nezávislé. Pri každom z týchto troch polí je dostupný vysvetľujúci popis.

Pri výbere **podradeného typu číselníka** platia určité obmedzenia. Niektoré možnosti sa preto nedajú zvoliť (sú označené sivou farbou), pri iných sa pri pokuse o uloženie zobrazí chybová správa.

1. Prepojenie číselníka na seba samého je zakázané. Ak pre číselník **B** vyberáte prepojenie na iný číselník, v zozname bude aj on sám, ale túto možnosť nebude možné zvoliť.

![](editor_select_1.png)

2. Kruhové prepojenie číselníkov je zakázané. Ak číselník **A** odkazuje na číselník **B**, číselník **B** sa nemôže prepojiť späť na číselník **A**. V nastaveniach číselníka **B** síce môžete vybrať číselník **A**, ale pri pokuse o uloženie sa zobrazí chybová správa.

![](editor_select_2.png)

3. Nové prepojenie na vymazaný číselník nie je možné vytvoriť. Ak číselník **C** odkazuje na číselník **D**, ktorý bol následne zmazaný, číselník **D** sa v možnostiach zobrazí s prefixom **`(!deleted)_`** a nebude ho možné vybrať. Existujúce prepojenie číselníka **C** na číselník **D** zostane zachované. Môžete ho zmeniť, ale po zmene už nebude možné znovu vybrať vymazaný číselník **D**.

![](editor_select_3.png)

Ak je zapnutá voľba **Povoliť prepojenie dátových záznamov na číselník**, jednotlivé záznamy môžete prepájať na iné číselníky. Platia tieto obmedzenia:

1. Prepojenie na číselník, do ktorého záznam patrí, je zakázané. Ak vytvárate záznam v číselníku **X**, číselník **X** sa v možnostiach prepojenia nezobrazí.
2. Pre prepojenie na vymazaný číselník platia rovnaké podmienky ako pri poli **Podradený typ číselníka**.

Ak je zapnutá voľba **Povoliť výber rodičovského záznamu**, jednotlivým záznamom môžete vybrať rodiča spomedzi ostatných záznamov toho istého číselníka. Platia tieto obmedzenia:

1. Typ číselníka musí mať pomenované pole **Reťazec 1**. Jeho hodnota sa používa na identifikáciu záznamu pri výbere rodiča.
2. Prepojenie záznamu na seba samého je zakázané. Aktuálny záznam sa preto v možnostiach výberu rodiča nezobrazí.

!> **Upozornenie:** ak vypnete voľbu **Povoliť prepojenie dátových záznamov na číselník** alebo **Povoliť výber rodičovského záznamu** a zmenu uložíte, všetky zodpovedajúce prepojenia dátových záznamov tohto číselníka sa odstránia. Opätovné zapnutie voľby ich neobnoví.

Napríklad záznam typu **X** odkazuje na číselník **Z**. Ak v type **X** vypnete voľbu **Povoliť prepojenie dátových záznamov na číselník** a uložíte zmenu, záznam príde o prepojenie na **Z**. Po opätovnom zapnutí voľby bude možné prepojenie znovu vybrať, ale pôvodná hodnota sa neobnoví.

## Zoznam dát číselníkov

V tabuľke dát môžete upravovať záznamy vytvorených typov číselníkov. V strome v ľavom paneli vyberte číselník, ktorý chcete spravovať. Po jeho výbere sa zobrazia príslušné dáta. Nové záznamy vytvoríte tlačidlom **+** nad tabuľkou dát. Ak má typ číselníka niektoré stĺpce nepomenované, tieto stĺpce ani ich dáta sa nezobrazia.

!> **Upozornenie:** vymazané typy sú dostupné iba po zapnutí voľby **Zobraziť vymazané typy**. Pred pridávaním alebo importovaním dát vymazaný typ obnovte.

![](dataTable_enumData.png)

Príklad:

Pri vytváraní číselníka **A** sme pomenovali polia **Reťazec 1**, **Reťazec 2** a **Boolean 1**. Tabuľka obsahuje práve tieto stĺpce. Pri vytváraní nového záznamu sa v editore zobrazia dve polia typu reťazec a jedno pole typu boolean s názvami zadanými pri vytváraní číselníka. Ak sme to v nastaveniach číselníka povolili, v editore bude dostupné aj **rodičovské prepojenie** alebo **prepojenie na číselník**.

![](editor_enumData.png)

Pri zmene vybraného typu číselníka sa môžu zmeniť stĺpce tabuľky aj polia v editore dát podľa nastavení vybraného typu.

## Mazanie dát

Typy číselníkov ani ich dátové záznamy sa pri zmazaní štandardne fyzicky neodstránia z databázy, iba sa označia ako zmazané. Vďaka tomu zostanú údaje dostupné pre existujúce záznamy, ktoré na ne odkazujú. Napríklad v číselníku **Farba auta** môžete zmazať farbu, ktorú už nechcete ponúkať pri vytváraní nových záznamov, ale v starších záznamoch ju stále potrebujete zobraziť.

Typ číselníka môžete obnoviť v používateľskom rozhraní: v nastaveniach stromu zapnite **Zobraziť vymazané typy**, vyberte typ s ikonou koša, otvorte jeho editor a vypnite voľbu **Vymazaný**. Uložením sa obnoví typ aj všetky jeho dátové záznamy vrátane záznamov zmazaných samostatne pred zmazaním typu. Bežná úprava aktívneho typu zmazané dáta neobnovuje.

Pri zmazaní typu sa označia ako zmazané aj jeho dátové záznamy. Pri vypnutej voľbe **Zobraziť vymazané typy** typ zmizne zo všetkých vetiev stromu, ale existujúce prepojenia z ostatných typov zostanú uložené; v ich editore sa zobrazujú s prefixom **`(!deleted)_`**. Ak zmažete rodičovský typ, jeho podradený typ sa nezmaže: zostane pod ďalšími aktívnymi rodičmi alebo sa zobrazí na najvyššej úrovni stromu, ak už žiadneho aktívneho rodiča nemá.

Pri zobrazení vymazaných typov strom zachová aj ich prepojenia na rodičov a potomkov. Po obnovení typu sa opäť zobrazí v bežnom strome na základe zachovaných prepojení.
