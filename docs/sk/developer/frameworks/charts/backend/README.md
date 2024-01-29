# BackEnd pre prácu s grafmi

Práca s grafmi je výhradne práca FrontEnd-u a BackEnd zabezpečuje iba dáta pre tieto grafy. Nakoľko každý graf filtruje svoje dáta pomocou externého filtra, boli pridané rôzne triedy, ktoré uľahčujú prácu s parametrami z týchto externých filtrov. Získanie a prípravu dát pre grafy zabezpečujú jednotlivé RestController-i, ktoré obsluhujú aj danú stránku na ktorej sú tieto grafy.

## Typ grafu

```ChartType``` je trieda typu ```Enum```, ktorá slúži na rýchlu a bezpečnú identifikáciu typu grafu.

Keďže rôzne grafy mávajú rovnaký zdroj dát, ktorý je potrebné iba špecificky upraviť danému typu je takáto rýchla identifikácia vhodná.

Trieda ```ChartType``` obsahuje tieto hodnoty:
- ```NOT_CHART```, táto hodnota býva primárne prednastavená. Pridanie tohto typu bolo potrebné keďže v niektorých prípadoch datatabuľka aj graf zdieľali rovnaký zdroj dát a touto hodnotou sa dá povedať že nejde o graf ale napríklad o datatabuľku.
- ```PIE```, hodnota reprezentuje graf "koláčového" typu
- ```LINE```, hodnota reprezentuje graf "čiarového" typu
- ```BAR```, hodnota reprezentuje graf "stĺpcového" typu

## Dáta pre grafy

Formát vrátených dát z BackEnd-u pre grafy závisí od typu grafu, ktorému budú tieto dáta pridelené :
-   `Bar`, vrátený typ dát je jednoduchý list objektov, ```java List<T> ```, kde objekt musí obsahovať textovú premennú(osa Y, kategória) a číselnú premennú(osa X, hodnota k danej kategórií)
-   `Pie`, vrátený typ dát je jednoduchý list objektov, ```java List<T> ```, kde objekt musí obsahovať textovú premennú(kategória) a číselnú premennú(hodnota k danej kategórií)
-   `Line`, vrátený typ dát je mapa obsahujúca listy objektov, ```java Map<String, List<T>> ```, kde kľúč mapy predstavuje názov `datasetu`, objekt v liste objektov(hodnota prvku mapy) musí obsahovať číselnú premennú(osa Y, hodnota) a dátumovú premennú(osa X, kedy bola hodnota získaná)

Bližšie informácie o procese získania/spracovanie dát pre grafy sú špecificky okomentované v jednotlivých ```RestControlleroch```, ktoré tieto dáta zabezpečujú.

### Stat sekcia

Hlavným užívateľom grafov sú aplikácie v sekcií Štatistiky(stat). Spoločnou vlastnosťou pre tieto aplikácie je, že dáta pre grafy získavame špecificky pomocou tried StatTableDB alebo StatNewDB a vrátené dáta sú Listy s typom Column. Keďže premenná Column slúži dynamicky pre rôzne kombinácie dát, každá trieda musí získať potrebné hodnoty z tejto premennej špecificky. Napríklad ```BrowserRestController``` získava hodnotu ```visit``` ako ```Column.getIntColumn3()``` ale ```CountryRestController``` získava tú istú hodnotu ako ```Column.getIntColumn2()```.

## Filtrovanie

Trieda ```FilterHeaderDto``` predstavuje najpoužívanejšie hodnoty externých filtrov, ktoré sa používajú na filtrovanie hodnôt pre grafy. Keďže takmer každá stránka využívajúca grafy má externý filter, najpoužívanejšie parametre týchto filtrov sa zhrnuli do tejto triedy, aby sa zvýšila prehľadnosť a zabránilo sa duplicite kódov.

Okrem konštruktora, ktorý nastaví základné hodnoty, trieda obsahuje aj metódu ```groupIdToQuery```. Jej úlohou je vytvoriť a nastaviť ```query``` potrebnú k spätnej kompatibilite zo zadaného ```rootGroupId```. Vytvorená ```rootGroupIdQuery``` predstavuje sql príkaz pre výber tých záznamov, ktorých ```group_id``` spadá pod zadané ```rootGroupId``` ako potomok na ktorejkoľvek úrovni (výber celého pod-stromu web stránok).

## StatService

Táto trieda bola primárne vytvorená pre zníženie duplicitného kódu a zjednodušenie spracovania hodnôt z externých filtrov. Keďže každý graf na stránke využíva nejaký externý filter, boli do tejto triedy pridané rôzne metódy, ktoré uľahčujú spracovanie hodnôt z externých filtrov alebo prácu s nimi. Bližší popis jednotlivých funkcií/metód, ich vstupy a možné výstupy je priamo v ```StatService``` triede vo forme komentárov.






