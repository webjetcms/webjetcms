# BackEnd pre prácu s grafmi

Práca s grafmi je výhradne práca FrontEnd-u a BackEnd zabezpečuje iba dáta pre tieto grafy. Nakoľko každý graf filtruje svoje dáta pomocou externého filtra, boli pridané rôzne triedy, ktoré uľahčujú prácu s parametrami z týchto externých filtrov. Získanie a prípravu dát pre grafy zabezpečujú jednotlivé `RestController` súbory, ktoré obsluhujú aj danú stránku na ktorej sú tieto grafy.

## Typ grafu

`ChartType` je trieda typu `Enum`, ktorá slúži na rýchlu a bezpečnú identifikáciu typu grafu.

Keďže rôzne grafy mávajú rovnaký zdroj dát, ktorý je potrebné iba špecificky upraviť danému typu je takáto rýchla identifikácia vhodná.

Trieda `ChartType` obsahuje tieto hodnoty:

- `NOT_CHART`, táto hodnota býva primárne prednastavená. Pridanie tohto typu bolo potrebné keďže v niektorých prípadoch datatabuľka aj graf zdieľali rovnaký zdroj dát a touto hodnotou sa dá povedať že nejde o graf ale napríklad o datatabuľku.
- `LINE`, hodnota reprezentuje graf **čiarového** typu
- `BAR_VERTICAL`, hodnota reprezentuje graf **stĺpcového** typu, s vertikálnymi stĺpcami
- `BAR_HORIZONTAL`, hodnota reprezentuje graf **stĺpcového** typu, s horizontálnymi stĺpcami
- `PIE_CLASSIC`, hodnota reprezentuje graf **koláčového** typu
- `PIE_DONUT`, hodnota reprezentuje variáciu grafu **koláčového** typu, ktorý má v strede prázdny kruh (nie je vyplnený)
- `DOUBLE_PIE`,  hodnota reprezentuje graf **dvojitého koláčového** typu (vonkajší a vnútorný graf)
- `WORD_CLOUD`, hodnota reprezentuje graf **oblak slov**, ktorý zobrazuje slová, kde veľkosť slova reprezentuje jeho frekvenciu výskytu (alebo inú hodnotu) v danom datasete
- `TABLE`, hodnota reprezentuje jednoduchú **tabuľku** s dátami (bez pokročilých funkcionalít ako je stránkovanie, filtrovanie, atď.)

## Dáta pre grafy

Formát vrátených dát z BackEnd-u pre grafy závisí od typu grafu, ktorému budú tieto dáta pridelené:

- `LINE`, vrátený typ dát je mapa obsahujúca listy objektov, `Map<String, List<T>>`, kde kľúč mapy predstavuje názov `datasetu`, objekt v liste objektov (hodnota prvku mapy) musí obsahovať číselnú premennú (osa Y, hodnota) a dátumovú premennú (osa X, kedy bola hodnota získaná)
- `BAR_VERTICAL` / `BAR_HORIZONTAL`, vrátený typ dát je jednoduchý list objektov, `List<T>`, kde objekt musí obsahovať textovú premennú (osa Y, kategória) a číselnú premennú (osa X, hodnota k danej kategórií)
- `PIE_CLASSIC` / `PIE_DONUT`, vrátený typ dát je jednoduchý list objektov, `List<T>`, kde objekt musí obsahovať textovú premennú (kategória) a číselnú premennú (hodnota k danej kategórií)
- `DOUBLE_PIE`, vrátený typ dát je jednoduchý list objektov, `List<T>`, kde objekt musí obsahovať textovú premennú (kategória) a dve číselné premenné (každá hodnota pre jednu kategóriu)
- `WORD_CLOUD`, v prípade tohto grafu sa dáta vracajú v 2 rôznych formátoch:
  - jednoduchý list objektov, `List<T>`, kde objekt obsahuje textovú premennú (slovo / frázu) a číselnú premennú (frekvencia výskytu slova)
  - môže byť jednoduchý textový reťazec, `String`, sám graf si vyhľadá jednotlivé slová a ich frekvenciu výskytu v tomto reťazci
- `TABLE`, vrátený typ dát je jednoduchý list objektov, `List<T>`, kde objekt obsahuje rôzne textové či číselné hodnoty, ktoré chcete zobraziť

Bližšie informácie o procese získania/spracovania dát pre grafy sú špecificky okomentované v jednotlivých `RestControlleroch`, ktoré tieto dáta zabezpečujú.

### Sekcia štatistiky

Hlavným užívateľom grafov sú aplikácie v sekcií Štatistiky. Spoločnou vlastnosťou pre tieto aplikácie je, že dáta pre grafy získavame špecificky pomocou tried `StatTableDB` alebo `StatNewDB` a vrátené dáta sú `listy` s typom `Column`. Keďže premenná `Column` slúži dynamicky pre rôzne kombinácie dát, každá trieda musí získať potrebné hodnoty z tejto premennej špecificky. Napríklad `BrowserRestController` získava hodnotu `visit` ako `Column.getIntColumn3()` ale `CountryRestController` získava tú istú hodnotu ako `Column.getIntColumn2()`.

## Filtrovanie

Trieda `FilterHeaderDto` predstavuje najpoužívanejšie hodnoty externých filtrov, ktoré sa používajú na filtrovanie hodnôt pre grafy. Keďže takmer každá stránka využívajúca grafy má externý filter, najpoužívanejšie parametre týchto filtrov sa zhrnuli do tejto triedy, aby sa zvýšila prehľadnosť a zabránilo sa duplicite kódov.

Okrem konštruktora, ktorý nastaví základné hodnoty, trieda obsahuje aj metódu `groupIdToQuery`. Jej úlohou je vytvoriť a nastaviť `query` potrebnú k spätnej kompatibilite zo zadaného `rootGroupId`. Vytvorená `rootGroupIdQuery` predstavuje SQL príkaz pre výber tých záznamov, ktorých `group_id` spadá pod zadané `rootGroupId` ako potomok na ktorejkoľvek úrovni (výber celého pod-stromu web stránok).

## `StatService`

Táto trieda bola primárne vytvorená pre zníženie duplicitného kódu a zjednodušenie spracovania hodnôt z externých filtrov. Keďže každý graf na stránke využíva nejaký externý filter, boli do tejto triedy pridané rôzne metódy, ktoré uľahčujú spracovanie hodnôt z externých filtrov alebo prácu s nimi. Bližší popis jednotlivých funkcií/metód, ich vstupy a možné výstupy je priamo v `StatService` triede vo forme komentárov.

## Farebná schéma grafov

Grafy podporujú rozdielne farebné schémy na úpravu vzhľadu grafu. Pre výber farebnej schémy odporúčame použiť pole typu `IMAGE_RADIO`, ktoré musí mať triedu `image-radio-chart-colorset` pre správne nastavenie (viď príklad nižšie)

```java
@Column(name = "color_scheme")
@DataTableColumn(inputType = DataTableColumnType.IMAGE_RADIO, title = "&nbsp;", tab = "stat", className = "image-radio-horizontal image-radio-chart-colorset")
private String colorScheme;
```

možnosti pre toto pole nastavíte ako

```java
page.addOptions("colorScheme", StatService.getColorSchemeOptions(), "label", "value", true);
```

dôležité je práve volanie statickej metódy `getColorSchemeOptions` z pomocnej triedy `StatService`. Táto metóda vráti zoznam dostupných farebných schém aj s cestou k obrázku, ktorý dané farby v schéme zobrazuje

```java
public static final String DEFAULT_COLORSET_NAME = "set3";
public static final List<OptionDto> getColorSchemeOptions() {
    List<OptionDto> optionsMap = new ArrayList<>();
    optionsMap.add(new OptionDto("set1", "set1", "/apps/_common/charts/images/overlapping_circles_set1.png"));
    optionsMap.add(new OptionDto("set2", "set2", "/apps/_common/charts/images/overlapping_circles_set2.png"));
    optionsMap.add(new OptionDto("set3", "set3", "/apps/_common/charts/images/overlapping_circles_set3.png"));
    optionsMap.add(new OptionDto("set4", "set4", "/apps/_common/charts/images/overlapping_circles_set4.png"));
    optionsMap.add(new OptionDto("set5", "set5", "/apps/_common/charts/images/overlapping_circles_set5.png"));
    optionsMap.add(new OptionDto("set_blue", "set_blue", "/apps/_common/charts/images/overlapping_circles_set_blue.png"));
    optionsMap.add(new OptionDto("set_green", "set_green", "/apps/_common/charts/images/overlapping_circles_set_green.png"));
    optionsMap.add(new OptionDto("set_red", "set_red", "/apps/_common/charts/images/overlapping_circles_set_red.png"));
    optionsMap.add(new OptionDto("set_yellow", "set_yellow", "/apps/_common/charts/images/overlapping_circles_set_yellow.png"));
    return optionsMap;
}
```