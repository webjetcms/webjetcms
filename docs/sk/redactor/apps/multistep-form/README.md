# Viackrokové formuláre

Novým druhom formulárov v systéme WebJet CMS sú viackrokové formuláre. Tieto formuláre umožňujú rozdelenie dlhých formulárov na viacero krokov, čo zlepšuje používateľskú skúsenosť a zvyšuje prehľadnosť.

## Vytvorenie viackrokového formulára

Fomulár môžete vytvoriť v sekcii **Aplikácie > Formuláre**, presnejšie v karte [Zoznam formulárov](../form/README.md). Zoznam obsahuje formuláre všetkých typov, vrátane viackrokových, ale pri vytvorení nového sa vždy vytvorí viackrokový formulár.

![](forms-list.png)

## Karta - Obsah formuláru

Po vytvorení formulára musíte prejsť najpr do detailu formulára, kde je zoznam už vyplnených a odoslaných formulárov. V tomto detaile si môžeme všimnúť, že sa objaví nová karta **Obsah formuláru**.

![](form-detail.png)

Kliknutím na túto kartu sa dostaneme do sekcie, kde môžeme vyskladať obsah formuláru. Po vytvorení nového fmulára sa automatický vytvorí prvý krok formulára s názvom `DEFAULT`.

![](default-form.png)

## Zloženie karty - Obsah formuláru

Karta alebo sekcia **Obsah formuláru** je rozdelená na tri časti, a to (z ľava doprava):

- **Kroky formuláru**, tabuľka obshujúca zoznam krokov formuláru
- **Položky formuláru**,  tabuľka obsahujúca zoznam položiek formuláru patriacich do práve vybraného kroku
- **Náhľad formuláru**,  zobrazenie náhľadu práve zvoleného kroku formuláru

![](real-form.png)

## Kroky formuláru

Prvá tabuľka obsahuje list krokov formuláru. V tejto tabuľke môžeme pridávať, duplikovať, mazať, upravovať a meniť poradie krokov formuláru. Každý krok má svoj nadpis, sekundárny nadpis a poradie. Každý vytvorený krok teda predstavuje reaálný krok vo formulári, ktorý sa zobrazí používateľovi. Na tieto vytvorené kroky môžeme následne pridávať položky formuláru, ktoré sa zobrazia v danom kroku.

![](form-step-editor.png)

!>**Upozornenie:** tabuľka umožňuje zvoliť súčasne len jeden krok formuláru, nakoľko od kroku zavisia položky formuláru, ktoré sa zobrazia v druhej tabuľke ako aj náhľad formuláru.

### Duplikovanie

Pri duplikovaní kroku sa skopírujú všetky položky, ktoré sa v danom kroku nachádzajú, do nového kroku. Týmto spôsobom môžeme rýchlo vytvoriť podobné kroky bez nutnosti pridávať všetky položky odznova.

### Mazanie

Pri mazaní kroku sa odstránia všetky položky, ktoré sa v danom kroku nachádzajú. Pred mazaním je vhodné skontrolovať, či v danom kroku nie sú žiadne dôležité položky, ktoré by sme chceli zachovať.

## Položky formuláru

Druhá tabuľka obsahuje položky formuláru (alebo položky kroku formuláru), ktoré sa zobrazia až po zvolení nejakého kroku v prvej tabuľke. V tejto tabuľke môžeme pridávať, duplikovať, upravovať, mazať a meniť poradie položiek formuláru. Položky sa pridávajú do práve zvoleného kroku (takže jednotlivé kroky formulár sa nebudú navzájom ovplyvňovať).

![](form-item-editor.png)

### Pridávanie a úprava položiek

Editor pre pridávanie a úpravu položiek je špeciálny tým, že mení svoj obsah podľa zvoleného typu poľa (položky). To znamená, že pre každý typ položky (napr. Meno, Priezvisko atď.) sa zobrazia rôzne nastavenia a možnosti konfigurácie. Podľa zvoleného typu položky môžete nastaviť parametre ako:

- **Povinné pole**, či je položka povinná na vyplnenie.
- **Názov poľa**, názov, ktorý sa zobrazí používateľovi. Ak nie je zadaný použije sa názov zhodný s typom poľa.
- **Predvyplnená hodnota**, hodnota, ktorá sa zobrazí v poli pred vyplnením používateľom.
- **Zástupný text**, text, ktorý sa zobrazí v poli ako nápoveda pre používateľa ak nie je pole vyplnené.
- **Tooltip**, ak zadáte hodnotu tooltipu, zobrazí sa pri názve poľa informačná bublina.
- **Regulárne výrazy**, pre pokročilú validáciu vstupu používateľa viete zvoliť ľubovoľný počet regulárnych výrazov, ktoré musia byť splnené, aby bol vstup platný. Viac sa o nich dozviete v sekcii [Regulárne výrazy](../form/regexps.md).

Ak chcete definovať vlastné položky formulárov, alebo chcete zmeniť existujúce, či zmeniť aké nastavenia sú dostupné pre jednotlivé typy položiek, pozrite si dokumentáciu v sekcii [Položky formulárov](../formsimple/README.md#informácia-pre-web-dizajnéra).

### Riadkové zobrazenie

Môžete sa stretnúť so situáciou, kedy Vám editor nedovolí pridať zvolenú položku. Môže to nastať ak ide o špeciálnu položku určenú pre riadkové zobrazenie formulára (vo všeubecnosti maju tieto položky v názve "riadkové zobrazenie"). Kým formulár nebude v režime riadkového zobrazenia, tieto položky síce vo výbere uvidíte ale nebudete ich môcť použiť.

### Značky

Ak chete v položke formuláru použíť informácie o aktuálnom používateľovi (napr. jeho meno, email, atď.), môžete použiť špeciálne značky. Tieto značky sa automatický nahradia príslušnými hodnotami pri zobrazení formuláru používateľovi. Pre neprihlasených používateľov budú tieto značky nahradené prázdnou hodnotou.

Dostpné značky sú:

- ```!LOGGED_USER_NAME!``` - meno a priezvisko (ak je konf. premenná ```fullNameIncludeTitle``` nastavená na true obsahuje aj titul)
- ```!LOGGED_USER_FIRSTNAME!``` - meno
- ```!LOGGED_USER_LASTNAME!``` - priezvisko
- ```!LOGGED_USER_TITLE!``` - titul
- ```!LOGGED_USER_LOGIN!``` - prihlasovacie meno
- ```!LOGGED_USER_EMAIL!``` - emailová adresa
- ```!LOGGED_USER_COMPANY!``` - firma
- ```!LOGGED_USER_CITY!``` - mesto
- ```!LOGGED_USER_ADDRESS!``` - adresa (ulica)
- ```!LOGGED_USER_COUNTRY!``` - štát
- ```!LOGGED_USER_PHONE!``` - telefón
- ```!LOGGED_USER_ZIP!``` - PSČ
- ```!LOGGED_USER_ID!``` - ID používateľa
- ```!LOGGED_USER_BIRTH_DATE!``` - dátum narodenia
- ```!LOGGED_USER_FIELDA!``` - voľné pole A
- ```!LOGGED_USER_FIELDB!``` - voľné pole B
- ```!LOGGED_USER_FIELDC!``` - voľné pole C
- ```!LOGGED_USER_FIELDD!``` - voľné pole D
- ```!LOGGED_USER_FIELDE!``` - voľné pole E
- `!LOGGED_USER_GROUPS!` - zoznam skupín používateľov

## Náhľad formuláru

Posledná časť sekcie **Obsah formuláru** je náhľad formuláru. Tento náhľad zobrazuje aktuálny krok formuláru tak, ako ho uvidí používateľ. Náhľad sa aktualizuje vždy, keď nastane nejaká zmena v tabuľke kroky alebo položky formuláru. Týmto spôsobom môžete okamžite vidieť, ako bude formulár vyzerať po vykonaní zmien. Môžeme vidieť že nahľad absabuej okrem samotných položiek aj nadpis kroku a sekundárny nadpis kroku.

Na konci každého kroku sa automatický vygeneruje tlačidlo, ktorého text sa mení podľa toho, či je krok posledný alebo nie. Ak nie je posledný, tlačidlo bude mať text **Prejsť na ďalší krok**, inak **Odoslať formulár**.

!>**Upozornenie:** Náhľad formuláru sa vo výskedku môže graficky líšiť od skutočného zobrazenia vo webovej aplikácii, nakoľko záleží na použitom šablónovaní a štýloch danej stránky. Náhľad slúži hlavne na predstavu o rozložení a obsahu formuláru.X

## Konfiguračné premenné

Dostupné konfiguračné premenné pre viackrokové formuláre:

- `multistepform_nameFields` - zoznam názvov polí, ktoré budú považované za polia pre meno. Medzi týmito poľami sa bude hľadať meno, ktoré by sa použilo ako oslovenie v emailoch. Použije sa iba **prvé** nájdene neprázdne meno.
- `multistepform_emailFields` - zoznam názvov polí, ktoré budú považované za polia pre emailovú adresu. Medzi týmito poľami sa bude hľadať emailová adresa, na ktorú sa odošle potvrdenie o prijatí formuláru. Použije sa  **všetky** nájdene a validné emailové adresy.
- `multistepform_attachmentDefaultName` - prednastavený názov prílohy v emailoch, ktorý sa použije ak sa nepodarí získať skutočný názov súboru prílohy.
- `multistepform_subjectDefaultValue` - prednastavený predmet emailu, ktorý sa použije ak nie je zadaný predmet v nastaveniach/atribútoch formuláru.